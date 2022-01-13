package whisplayer1;

import battlecode.common.*;
import java.util.Random;

public strictfp class RobotPlayer {

    static int turnCount = 0;

    static final Random rng = new Random();

    static final int minerCountIndex = GameConstants.MAX_STARTING_ARCHONS;
    static final int soldierCountIndex = GameConstants.MAX_STARTING_ARCHONS + 1;
    static final int builderCountIndex = GameConstants.MAX_STARTING_ARCHONS + 2;

    static final int enemyArchonStartIndex = GameConstants.SHARED_ARRAY_LENGTH - GameConstants.MAX_STARTING_ARCHONS;
    static final int enemyArchonStopIndex = GameConstants.SHARED_ARRAY_LENGTH;
    static final int ownArchonStartIndex = enemyArchonStartIndex - GameConstants.MAX_STARTING_ARCHONS;
    static final int ownArchonStopIndex = enemyArchonStartIndex;
    static final int archonCounterIndex = ownArchonStartIndex - 1;

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

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        while (true) {
            turnCount += 1;

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                rc.setIndicatorString(
                    rc.readSharedArray(4) +
                    " " +
                    rc.readSharedArray(5) +
                    " " +
                    rc.readSharedArray(6) +
                    " " +
                    rc.readSharedArray(55)
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
                    case LABORATORY:
                    case WATCHTOWER:
                    case BUILDER:
                    case SAGE:
                        break;
                }
            } catch (GameActionException e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
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

    public static void addEnemyArchon(RobotController rc, MapLocation loc) throws GameActionException {
        int convertLocation = loc.x * 100 + loc.y;
        for (int i = enemyArchonStartIndex; i < enemyArchonStopIndex; i++) {
            if (rc.readSharedArray(i) == convertLocation) {
                return;
            } else if (rc.readSharedArray(i) == 0) {
                rc.writeSharedArray(i, convertLocation);
                return;
            }
        }
    }

    public static void checkEnemyArchons(RobotController rc) throws GameActionException {
        for (int i = enemyArchonStartIndex; i < enemyArchonStopIndex; i++) {
            MapLocation a = new MapLocation(rc.readSharedArray(i) / 100, rc.readSharedArray(i) % 100);
            if (rc.canSenseLocation(a)) {
                // reset array value if there's no robot at that index or if the robot there is not an archon
                if (!rc.canSenseRobotAtLocation(a) || rc.senseRobotAtLocation(a).getType() != RobotType.ARCHON) {
                    rc.writeSharedArray(i, 0);
                }
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

    public static boolean enemyArchonDetected(RobotController rc) throws GameActionException {
        for (int i = enemyArchonStartIndex; i < enemyArchonStopIndex; i++) {
            if (rc.readSharedArray(i) > 0) return true;
        }
        return false;
    }
}
