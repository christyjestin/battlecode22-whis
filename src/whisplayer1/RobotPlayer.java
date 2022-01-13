package whisplayer1;

import battlecode.common.*;
import java.util.Random;

/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what
 * we'll call once your robot
 * is created!
 */
public strictfp class RobotPlayer {

    /**
     * We will use this variable to count the number of turns this robot has been
     * alive.
     * You can use static variables like this to save any information you want. Keep
     * in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between
     * your robots.
     */
    static int turnCount = 0;

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided
     * by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant
     * number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very
     * useful for debugging!
     */
    static final Random rng = new Random();

    static final int minerCountIndex = GameConstants.MAX_STARTING_ARCHONS;
    static final int soldierCountIndex = GameConstants.MAX_STARTING_ARCHONS + 1;
    static final int builderCountIndex = GameConstants.MAX_STARTING_ARCHONS + 2;

    /** Array containing all the possible movement directions. */
    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    /**
     * run() is the method that is called when a robot is instantiated in the
     * Battlecode world.
     * It is like the main function for your robot. If this method returns, the
     * robot dies!
     *
     * @param rc The RobotController object. You use it to perform actions from this
     *           robot, and to get
     *           information on its current status. Essentially your portal to
     *           interacting with the world.
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        // Hello world! Standard output is very useful for debugging.
        // Everything you say here will be directly viewable in your terminal when you
        // run a match!
        // System.out.println("I'm a " + rc.getType() + " and I just got created! I have
        // health " + rc.getHealth());

        // You can also use indicators to save debug notes in replays.
        // rc.setIndicatorString("Hello world!");

        while (true) {
            // This code runs during the entire lifespan of the robot, which is why it is in
            // an infinite
            // loop. If we ever leave this loop and return from run(), the robot dies! At
            // the end of the
            // loop, we call Clock.yield(), signifying that we've done everything we want to
            // do.

            turnCount += 1; // We have now been alive for one more turn!
            // System.out.println("Age: " + turnCount + "; Location: " + rc.getLocation());

            // Try/catch blocks stop unhandled exceptions, which cause your robot to
            // explode.
            try {
                // The same run() function is called for every robot on your team, even if they
                // are
                // different types. Here, we separate the control depending on the RobotType, so
                // we can
                // use different strategies on different robots. If you wish, you are free to
                // rewrite
                // this into a different control structure!
                rc.setIndicatorString(
                    rc.readSharedArray(60) +
                    " " +
                    rc.readSharedArray(61) +
                    " " +
                    rc.readSharedArray(62) +
                    " " +
                    rc.readSharedArray(63) +
                    " " +
                    rc.readSharedArray(30)
                );
                switch (rc.getType()) {
                    case ARCHON:
                        Archon.runArchon(rc);
                        break;
                    case MINER:
                        Miner.runMiner(rc);
                        break;
                    case SOLDIER:
                        Soldier.runSoldier(rc);
                        break;
                    case LABORATORY: // Examplefuncsplayer doesn't use any of these robot types below.
                    case WATCHTOWER: // You might want to give them a try!
                    case BUILDER:
                    case SAGE:
                        break;
                }
            } catch (GameActionException e) {
                // Oh no! It looks like we did something illegal in the Battlecode world. You
                // should handle GameActionExceptions judiciously, in case unexpected events
                // occur in the game world. Remember, uncaught exceptions cause your robot to
                // explode!
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop
                // again.
                Clock.yield();
            }
            // End of loop: go back to the top. Clock.yield() has ended, so it's time for
            // another turn!
        }
        // Your code should never reach here (unless it's intentional)! Self-destruction
        // imminent...
    }

    public static void pathfinder(MapLocation targetLocation, RobotController rc) throws GameActionException {
        Direction targetDirection = rc.getLocation().directionTo(targetLocation);
        if (targetDirection.equals(Direction.CENTER)) return;
        Direction[] possibleMoves = {
            targetDirection.rotateLeft().rotateLeft(),
            targetDirection.rotateLeft(),
            targetDirection,
            targetDirection.rotateRight(),
            targetDirection.rotateRight().rotateRight(),
        };
        Direction bestMove = null;
        int bestScore = -500;
        for (int i = 0; i < possibleMoves.length; i++) {
            if (rc.canMove(possibleMoves[i])) {
                // override pathfinding algorithm if destination is adjacent
                if (targetLocation != null && rc.adjacentLocation(possibleMoves[i]).equals(targetLocation)) {
                    bestMove = possibleMoves[i];
                    break;
                }
                int score = -20 * Math.abs(i - 2) - rc.senseRubble(rc.adjacentLocation(possibleMoves[i]));
                if (score > bestScore) {
                    bestMove = possibleMoves[i];
                    bestScore = score;
                }
            }
        }
        if (bestMove != null) {
            rc.move(bestMove);
        } else {
            Direction oppositeDirection = targetDirection.opposite();
            Direction[] backupMoves = {
                oppositeDirection.rotateLeft(),
                oppositeDirection,
                oppositeDirection.rotateRight(),
            };
            Direction bestBackupMove = null;
            int bestBackupScore = -500;
            for (int i = 0; i < backupMoves.length; i++) {
                if (rc.canMove(backupMoves[i])) {
                    int score = 20 * Math.abs(i - 1) - rc.senseRubble(rc.adjacentLocation(backupMoves[i]));
                    if (score > bestBackupScore) {
                        bestBackupMove = backupMoves[i];
                        bestBackupScore = score;
                    }
                }
            }
            if (bestBackupMove != null) rc.move(bestBackupMove);
        }
    }

    public static void addEnemyArchon(MapLocation loc, RobotController rc) throws GameActionException {
        int convertLocation = loc.x * 100 + loc.y;
        for (int i = 63; i >= 60; i--) {
            if (convertLocation == rc.readSharedArray(i)) {
                return;
            }
        }
        for (int i = 63; i >= 60; i--) {
            if (0 == rc.readSharedArray(i)) {
                rc.writeSharedArray(i, convertLocation);
                return;
            }
        }
    }

    public static void checkEnemyArchons(RobotController rc) throws GameActionException {
        for (int i = 63; i >= 60; i--) {
            MapLocation a = new MapLocation(rc.readSharedArray(i) / 100, rc.readSharedArray(i) % 100);
            if (rc.canSenseLocation(a)) {
                if (
                    !rc.canSenseRobotAtLocation(a) || rc.senseRobotAtLocation(a).getType() != RobotType.ARCHON
                ) rc.writeSharedArray(i, 0);
            }
        }
    }

    public static MapLocation retrieveLocationfromArray(RobotController rc, int index) throws GameActionException {
        int encoding = rc.readSharedArray(index);
        int x = encoding / 100;
        int y = encoding % 100;
        return new MapLocation(x, y);
    }

    public static void incrementArray(RobotController rc, int index) throws GameActionException {
        rc.writeSharedArray(index, rc.readSharedArray(index) + 1);
    }

    public static void decrementArray(RobotController rc, int index) throws GameActionException {
        rc.writeSharedArray(index, rc.readSharedArray(index) - 1);
    }

    public static boolean archonDetected(RobotController rc) throws GameActionException {
        for(int i = 60; i <64; i ++){
            if(rc.readSharedArray(i) > 0){
                return true;
            }

        }

        return false;
    }
}
