/*
* Problem 1: Escape Room
*
* V2.3 – visible traps, -10 on trap, detrap (adjacent), timer, powerups (double coins for N steps),
*         and unique placement for items handled in GameGUI.
*/
public class EscapeRoom
{
  // === Timer config ===
  private static final int TIME_LIMIT_SECONDS = 90;

  // === Powerup config ===
  private static final int POWERUP_STEPS_DURATION = 20;  // lasts this many player steps

  private static void printHelp() {
    System.out.println("\nCommands:");
    System.out.println("  Movement:  right|r, left|l, up|u, down|d");
    System.out.println("  Jump:      jr (jump right), jl, ju, jd  (jumps over 1 space; cannot jump through walls)");
    System.out.println("  Prize:     pickup | p  (Collect $)  [doubles while powerup active]");
    System.out.println("  Traps:     ftr/ftl/ftu/ftd (Find Trap 1 space away), detrap | dt (disarm trap under/adjacent)");
    System.out.println("  Other:     replay   (reset same board)");
    System.out.println("             help|?   (this menu)");
    System.out.println("             quit|q   (Finish / Fail Finish)");
    System.out.println("             (Timer auto-ends at " + TIME_LIMIT_SECONDS + "s)\n");
  }

  public static void main(String[] args)
  {
    System.out.println("Welcome to EscapeRoom!");
    System.out.println("Get to the other side, avoid walls & traps, collect coins. Type 'help' for commands.\n");

    GameGUI game = new GameGUI();   // draws board, items, powerups
    game.createBoard();

    final int m = 60;                // grid step (must match board)
    final int TRAP_PEN = 10;         // -10 when you STEP on a trap

    int score = 0;

    // powerup state
    boolean doubleCoinsActive = false;
    int doubleCoinsStepsLeft = 0;

    String[] validCommands = {
      "right","left","up","down","r","l","u","d",
      "jump","jr","jumpleft","jl","jumpup","ju","jumpdown","jd",
      "pickup","p",
      "findtrapright","ftr","findtrapleft","ftl","findtrapup","ftu","findtrapdown","ftd",
      "detrap","dt",
      "replay","help","?","quit","q"
    };

    long tStart = System.currentTimeMillis();
    boolean play = true;
    boolean timedOut = false;

    printHelp();

    while (play)
    {
      // timer check before command
      int timeLeft = TIME_LIMIT_SECONDS - (int)((System.currentTimeMillis() - tStart)/1000);
      if (timeLeft <= 0) { timedOut = true; break; }

      System.out.print("Enter command (time left " + timeLeft + "s):");
      String input = UserInput.getValidInput(validCommands);

      int stepsBefore = game.getSteps();

      switch (input) {
        /* ---------------- Movement (single step) ---------------- */
        case "right": case "r": {
          int delta = game.movePlayer(m,0);
          score += delta;
          if (delta == 0 && game.isTrap(0,0)) { // landed on a trap tile
            System.out.println("TRAP VICTIM! -10");
            score -= TRAP_PEN;
            game.springTrap(0,0);              // clear the trap
          }
          break;
        }
        case "left": case "l": {
          int delta = game.movePlayer(-m,0);
          score += delta;
          if (delta == 0 && game.isTrap(0,0)) {
            System.out.println("TRAP VICTIM! -10");
            score -= TRAP_PEN;
            game.springTrap(0,0);
          }
          break;
        }
        case "up": case "u": {
          int delta = game.movePlayer(0,-m);
          score += delta;
          if (delta == 0 && game.isTrap(0,0)) {
            System.out.println("TRAP VICTIM! -10");
            score -= TRAP_PEN;
            game.springTrap(0,0);
          }
          break;
        }
        case "down": case "d": {
          int delta = game.movePlayer(0,m);
          score += delta;
          if (delta == 0 && game.isTrap(0,0)) {
            System.out.println("TRAP VICTIM! -10");
            score -= TRAP_PEN;
            game.springTrap(0,0);
          }
          break;
        }

        /* ---------------- Jump (two steps) ---------------- */
        case "jr": case "jump": {
          int d1 = game.movePlayer(m,0);  score += d1;
          if (d1 == 0) {
            int d2 = game.movePlayer(m,0); score += d2;
            if (d2 == 0 && game.isTrap(0,0)) {
              System.out.println("TRAP VICTIM! -10");
              score -= TRAP_PEN;
              game.springTrap(0,0);
            }
          }
          break;
        }
        case "jl": case "jumpleft": {
          int d1 = game.movePlayer(-m,0); score += d1;
          if (d1 == 0) {
            int d2 = game.movePlayer(-m,0); score += d2;
            if (d2 == 0 && game.isTrap(0,0)) {
              System.out.println("TRAP VICTIM! -10");
              score -= TRAP_PEN;
              game.springTrap(0,0);
            }
          }
          break;
        }
        case "ju": case "jumpup": {
          int d1 = game.movePlayer(0,-m); score += d1;
          if (d1 == 0) {
            int d2 = game.movePlayer(0,-m); score += d2;
            if (d2 == 0 && game.isTrap(0,0)) {
              System.out.println("TRAP VICTIM! -10");
              score -= TRAP_PEN;
              game.springTrap(0,0);
            }
          }
          break;
        }
        case "jd": case "jumpdown": {
          int d1 = game.movePlayer(0,m); score += d1;
          if (d1 == 0) {
            int d2 = game.movePlayer(0,m); score += d2;
            if (d2 == 0 && game.isTrap(0,0)) {
              System.out.println("TRAP VICTIM! -10");
              score -= TRAP_PEN;
              game.springTrap(0,0);
            }
          }
          break;
        }

        /* ---------------- Prizes ---------------- */
        case "pickup": case "p": {
          int prize = game.pickupPrize();
          if (prize > 0 && doubleCoinsActive) {
            // double coins -> add the same amount again
            prize += prize;
          }
          score += prize;

          // allow pickup to grab a powerup if you're on one
          if (game.pickupPowerup()) {
            doubleCoinsActive = true;
            doubleCoinsStepsLeft = POWERUP_STEPS_DURATION;
            System.out.println("POWERUP! Double coins for next " + POWERUP_STEPS_DURATION + " steps.");
          }
          break;
        }

        /* ---------------- Trap tools ---------------- */
        case "findtrapright": case "ftr": {
          boolean t = game.isTrap(m,0);
          System.out.println(t ? "Trap to the RIGHT." : "No trap to the RIGHT.");
          break;
        }
        case "findtrapleft": case "ftl": {
          boolean t = game.isTrap(-m,0);
          System.out.println(t ? "Trap to the LEFT." : "No trap to the LEFT.");
          break;
        }
        case "findtrapup": case "ftu": {
          boolean t = game.isTrap(0,-m);
          System.out.println(t ? "Trap UP." : "No trap UP.");
          break;
        }
        case "findtrapdown": case "ftd": {
          boolean t = game.isTrap(0,m);
          System.out.println(t ? "Trap DOWN." : "No trap DOWN.");
          break;
        }
        case "detrap": case "dt": {
          int delta = game.disarmNearbyTrap();
          if (delta > 0) System.out.println("Trap disarmed! +" + delta);
          else System.out.println("No trap in range. " + delta);
          score += delta;
          break;
        }

        /* ---------------- Session control ---------------- */
        case "replay": {
          score += game.replay();
          System.out.println("Board reset. Steps reset to 0.");
          // powerup resets on replay
          doubleCoinsActive = false;
          doubleCoinsStepsLeft = 0;
          // timer DOES NOT reset (comment next lines in if you want it to)
          // tStart = System.currentTimeMillis();
          // System.out.println("Timer reset to " + TIME_LIMIT_SECONDS + "s.");
          break;
        }
        case "help": case "?": {
          printHelp();
          break;
        }
        case "quit": case "q": {
          play = false;
          break;
        }
      }

      // auto-pickup powerup when you MOVE onto it (movement/jump cases)
      if (game.pickupPowerup()) {
        doubleCoinsActive = true;
        doubleCoinsStepsLeft = POWERUP_STEPS_DURATION;
        System.out.println("POWERUP! Double coins for next " + POWERUP_STEPS_DURATION + " steps.");
      }

      // burn down powerup by steps taken this loop
      int stepsAfter = game.getSteps();
      int stepDelta = Math.max(0, stepsAfter - stepsBefore);
      if (doubleCoinsActive && stepDelta > 0) {
        doubleCoinsStepsLeft -= stepDelta;
        if (doubleCoinsStepsLeft <= 0) {
          doubleCoinsActive = false;
          doubleCoinsStepsLeft = 0;
          System.out.println("Powerup expired.");
        }
      }

      // status line with timer
      timeLeft = TIME_LIMIT_SECONDS - (int)((System.currentTimeMillis() - tStart)/1000);
      System.out.println("score=" + score + " | steps=" + game.getSteps() +
                         (doubleCoinsActive ? (" | x2 coins (" + doubleCoinsStepsLeft + " steps left)") : "") +
                         " | " + Math.max(0, timeLeft) + "s left");
    }

    if (timedOut) System.out.println("\nTime's up!");
    score += game.endGame();
    System.out.println("Final score=" + score);
    System.out.println("Total steps=" + game.getSteps());
  }
}
