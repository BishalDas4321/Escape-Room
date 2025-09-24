import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Image;
import java.awt.Point;

import javax.swing.JPanel;
import javax.swing.JFrame;

import java.io.File;
import javax.imageio.ImageIO;

import java.util.Random;
import java.util.Set;
import java.util.HashSet;

/**
 * Game board (panel-based)
 * - Visible traps (trap.png, scaled)
 * - Powerups (power.png), coins (coin.png)
 * - Ensures coins, traps, powerups NEVER overlap cells
 */
public class GameGUI extends JPanel
{
  private static final long serialVersionUID = 141L;

  private static final int WIDTH = 510;
  private static final int HEIGHT = 360;
  private static final int SPACE_SIZE = 60;
  private static final int GRID_W = 8;
  private static final int GRID_H = 5;
  private static final int START_LOC_X = 15;
  private static final int START_LOC_Y = 15;

  // player placement
  private int x = START_LOC_X;
  private int y = START_LOC_Y;

  // images
  private Image bgImage;
  private Image prizeImage;
  private Image trapImage;
  private Image powerImage;
  private Image player;

  // player info
  private Point playerLoc;
  private int playerSteps;

  // board elements
  private int totalWalls;
  private Rectangle[] walls;
  private int totalPrizes;
  private Rectangle[] prizes;
  private int totalTraps;
  private Rectangle[] traps;
  private int totalPowerups;
  private Rectangle[] powerups;

  // scoring (penalties negative)
  private int prizeVal   = 10;
  private int trapVal    = 10; // -10 when stepped on; +10 when detrap succeeds
  private int endVal     = 10;
  private int offGridVal = 5;
  private int hitWallVal = 5;

  private JFrame frame;

  // track used cells for items (to avoid overlaps)
  private final Set<Point> occupiedItemCells = new HashSet<>();
  private final Random rand = new Random();

  public int getTrapVal()  { return trapVal; }
  public int getPrizeVal() { return prizeVal; }

  public GameGUI()
  {
    try { bgImage    = ImageIO.read(new File("grid.png"));  }
    catch (Exception e) { System.err.println("Could not open file grid.png"); }
    try { prizeImage  = ImageIO.read(new File("coin.png"));  }
    catch (Exception e) { System.err.println("Could not open file coin.png"); }
    try { trapImage   = ImageIO.read(new File("trap.png"));  }     // trap.png supported
    catch (Exception e) { System.err.println("Could not open file trap.png"); }
    try { powerImage  = ImageIO.read(new File("powerup.png")); }
    catch (Exception e) { System.err.println("Could not open file powerup.png"); }
    try { player      = ImageIO.read(new File("player.png")); }
    catch (Exception e) { System.err.println("Could not open file player.png"); }

    playerLoc = new Point(x, y);

    frame = new JFrame();
    frame.setTitle("EscapeRoom");
    frame.setSize(WIDTH, HEIGHT);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.add(this);
    frame.setVisible(true);
    frame.setResizable(false);

    totalWalls    = 20;
    totalPrizes   = 3;
    totalTraps    = 5;
    totalPowerups = 2;

    setDoubleBuffered(true);
    setFocusable(false);
  }

  public void createBoard()
  {
    traps    = new Rectangle[totalTraps];
    prizes   = new Rectangle[totalPrizes];
    walls    = new Rectangle[totalWalls];
    powerups = new Rectangle[totalPowerups];

    occupiedItemCells.clear();

    createTrapsUnique();
    createPrizesUnique();
    createPowerupsUnique();
    createWalls();

    repaint();
  }

  public int movePlayer(int incrx, int incry)
  {
    int newX = x + incrx;
    int newY = y + incry;

    playerSteps++;

    if ((newX < 0 || newX > WIDTH - SPACE_SIZE) || (newY < 0 || newY > HEIGHT - SPACE_SIZE))
    {
      System.out.println("OFF THE GRID!");
      return -offGridVal;
    }

    for (Rectangle r : walls)
    {
      int startX = (int) r.getX();
      int endX   = (int) r.getX() + (int) r.getWidth();
      int startY = (int) r.getY();
      int endY   = (int) r.getY() + (int) r.getHeight();

      if ((incrx > 0) && (x <= startX) && (startX <= newX) && (y >= startY) && (y <= endY))
        return wallHit();
      else if ((incrx < 0) && (x >= startX) && (startX >= newX) && (y >= startY) && (y <= endY))
        return wallHit();
      else if ((incry > 0) && (y <= startY && startY <= newY && x >= startX && x <= endX))
        return wallHit();
      else if ((incry < 0) && (y >= startY) && (startY >= newY) && (x >= startX) && (x <= endX))
        return wallHit();
    }

    x = newX;
    y = newY;
    repaint();
    return 0;
  }

  private int wallHit() {
    System.out.println("A WALL IS IN THE WAY");
    return -hitWallVal;
  }

  /** Check trap at offset (0,0) = current tile; or ±SPACE_SIZE in cardinal directions. */
  public boolean isTrap(int newx, int newy)
  {
    double px = playerLoc.getX() + newx;
    double py = playerLoc.getY() + newy;

    for (Rectangle r : traps)
      if (r.getWidth() > 0 && r.contains(px, py)) return true;
    return false;
  }

  /**
   * Clear a trap at player + (newx,newy). Returns 0 if success, -trapVal if none.
   * Caller decides how to score stepping on traps or detrap bonuses.
   */
  public int springTrap(int newx, int newy)
  {
    double px = playerLoc.getX() + newx;
    double py = playerLoc.getY() + newy;

    for (Rectangle r : traps)
    {
      if (r.contains(px, py))
      {
        if (r.getWidth() > 0)
        {
          r.setSize(0, 0);
          repaint();
          return 0; // success (no score change here)
        }
      }
    }
    System.out.println("THERE IS NO TRAP HERE TO SPRING");
    return -trapVal;
  }

  /**
   * Disarm a trap in range (current tile or cardinal-adjacent).
   * Returns +trapVal if disarmed, else -trapVal.
   */
  public int disarmNearbyTrap()
  {
    int s = SPACE_SIZE;
    int[][] offsets = { {0,0}, {s,0}, {-s,0}, {0,s}, {0,-s} };

    for (int[] off : offsets)
    {
      double px = playerLoc.getX() + off[0];
      double py = playerLoc.getY() + off[1];

      for (Rectangle r : traps)
      {
        if (r.getWidth() > 0 && r.contains(px, py))
        {
          r.setSize(0, 0);
          repaint();
          return +trapVal;
        }
      }
    }
    return -trapVal;
  }

  /**
   * Pick up a powerup on the player's current tile.
   * @return true if a powerup was collected
   */
  public boolean pickupPowerup()
  {
    if (powerups == null) return false;
    double px = playerLoc.getX();
    double py = playerLoc.getY();
    for (Rectangle pw : powerups)
    {
      if (pw.getWidth() > 0 && pw.contains(px, py))
      {
        pw.setSize(0,0);
        repaint();
        return true;
      }
    }
    return false;
  }

  public int pickupPrize()
  {
    double px = playerLoc.getX();
    double py = playerLoc.getY();

    for (Rectangle p : prizes)
    {
      if (p.getWidth() > 0 && p.contains(px, py))
      {
        System.out.println("YOU PICKED UP A PRIZE!");
        p.setSize(0, 0);
        repaint();
        return prizeVal;
      }
    }
    System.out.println("OOPS, NO PRIZE HERE");
    return -prizeVal;
  }

  public int getSteps() { return playerSteps; }

  public void setPrizes(int p)   { totalPrizes   = Math.max(1, p); }
  public void setTraps(int t)    { totalTraps    = Math.max(1, t); }
  public void setWalls(int w)    { totalWalls    = Math.max(1, w); }
  public void setPowerups(int n) { totalPowerups = Math.max(0, n); }

  public int replay()
  {
    int win = playerAtEnd();

    if (prizes != null)
      for (Rectangle p : prizes) p.setSize(SPACE_SIZE/3, SPACE_SIZE/3);
    if (traps != null)
      for (Rectangle t : traps) t.setSize(SPACE_SIZE/3, SPACE_SIZE/3);
    if (powerups != null)
      for (Rectangle pw : powerups) pw.setSize(SPACE_SIZE/3, SPACE_SIZE/3);

    x = START_LOC_X;
    y = START_LOC_Y;
    playerSteps = 0;
    repaint();
    return win;
  }

  public int endGame()
  {
    int win = playerAtEnd();
    setVisible(false);
    if (frame != null) frame.dispose();
    return win;
  }

  @Override
  public void paintComponent(Graphics g)
  {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;

    // grid
    if (bgImage != null) g.drawImage(bgImage, 0, 0, null);
    else { g2.setPaint(new Color(245,245,245)); g2.fillRect(0,0,WIDTH,HEIGHT); }

    // traps (visible, scaled to rect size)
    if (traps != null)
      for (Rectangle t : traps)
        if (t.getWidth() > 0) {
          int tx = (int) t.getX(), ty = (int) t.getY();
          int tw = (int) t.getWidth(), th = (int) t.getHeight(); // typically 15x15
          if (trapImage != null) g.drawImage(trapImage, tx, ty, tw, th, null);
          else { g2.setPaint(Color.RED); g2.fillRect(tx, ty, tw, th); }
        }

    // powerups (visible)
    if (powerups != null)
      for (Rectangle pw : powerups)
        if (pw.getWidth() > 0) {
          int px = (int) pw.getX(), py = (int) pw.getY();
          int pwW = (int) pw.getWidth(), pwH = (int) pw.getHeight();
          if (powerImage != null) g.drawImage(powerImage, px, py, pwW, pwH, null);
          else { g2.setPaint(new Color(120, 0, 200)); g2.fillOval(px, py, pwW, pwH); }
        }

    // prizes
    if (prizes != null)
      for (Rectangle p : prizes)
        if (p.getWidth() > 0) {
          int px = (int) p.getX(), py = (int) p.getY();
          int pw = (int) p.getWidth(), ph = (int) p.getHeight(); // 15x15
          if (prizeImage != null) g.drawImage(prizeImage, px, py, pw, ph, null);
          else { g2.setPaint(Color.YELLOW); g2.fillOval(px, py, pw, ph); }
        }

    // walls
    if (walls != null)
      for (Rectangle r : walls) { g2.setPaint(Color.BLACK); g2.fill(r); }

    // player
    if (player != null) g.drawImage(player, x, y, 40, 40, null);
    else { g2.setPaint(Color.BLUE); g2.fillOval(x, y, 40, 40); }

    playerLoc.setLocation(x, y);
  }

  /* ---------- unique placement helpers ---------- */

  private Point getUniqueFreeCell() {
    // tries until it finds a (w,h) not used by any prize/trap/powerup
    while (true) {
      int h = rand.nextInt(GRID_H);
      int w = rand.nextInt(GRID_W);
      Point cell = new Point(w, h);
      if (!occupiedItemCells.contains(cell)) {
        occupiedItemCells.add(cell);
        return cell;
      }
    }
  }

  private void createPrizesUnique()
  {
    int s = SPACE_SIZE;
    for (int i = 0; i < totalPrizes; i++)
    {
      Point cell = getUniqueFreeCell();
      int w = cell.x, h = cell.y;
      prizes[i] = new Rectangle((w*s + 15), (h*s + 15), 15, 15);
    }
  }

  private void createTrapsUnique()
  {
    int s = SPACE_SIZE;
    for (int i = 0; i < totalTraps; i++)
    {
      Point cell = getUniqueFreeCell();
      int w = cell.x, h = cell.y;
      traps[i] = new Rectangle((w*s + 15), (h*s + 15), 15, 15);
    }
  }

  private void createPowerupsUnique()
  {
    int s = SPACE_SIZE;
    for (int i = 0; i < totalPowerups; i++)
    {
      Point cell = getUniqueFreeCell();
      int w = cell.x, h = cell.y;
      powerups[i] = new Rectangle((w*s + 12), (h*s + 12), 20, 20);
    }
  }

  /* ---------- other generators ---------- */

  private void createWalls()
  {
    int s = SPACE_SIZE;
    for (int i = 0; i < totalWalls; i++)
    {
      int h = rand.nextInt(GRID_H);
      int w = rand.nextInt(GRID_W);

      Rectangle r;
      if (rand.nextInt(2) == 0)
        r = new Rectangle((w*s + s - 5), h*s, 8, s);        // vertical
      else
        r = new Rectangle(w*s, (h*s + s - 5), s, 8);        // horizontal
      walls[i] = r;
    }
  }

  private int playerAtEnd()
  {
    double px = playerLoc.getX();
    if (px > (WIDTH - 2*SPACE_SIZE))
    {
      System.out.println("YOU MADE IT!");
      return endVal;
    }
    else
    {
      System.out.println("OOPS, YOU QUIT TOO SOON!");
      return -endVal;
    }
  }
}
