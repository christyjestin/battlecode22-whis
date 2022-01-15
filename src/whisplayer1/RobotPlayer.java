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
    static final int archonHealthStartIndex = enemyArchonStartIndex - GameConstants.MAX_STARTING_ARCHONS;
    static final int archonHealthStopIndex = enemyArchonStartIndex;
    static final int archonLocationStartIndex = archonHealthStartIndex - GameConstants.MAX_STARTING_ARCHONS;
    static final int archonLocationStopIndex = archonHealthStopIndex;
    static final int archonCounterIndex = archonLocationStartIndex - 1;

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
                switch (rc.getType()) {
                    case ARCHON:
                        rc.setIndicatorString(
                            rc.readSharedArray(0) +
                            " " +
                            rc.readSharedArray(1) +
                            " " +
                            rc.readSharedArray(2) +
                            " " +
                            rc.readSharedArray(3)
                        );
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

    public static boolean isRepeatMove(Direction[] lastThreeMoves, Direction nextMove) {
        for (Direction move : lastThreeMoves) {
            if (move == null || move.equals(Direction.CENTER)) return false;
        }
        return lastThreeMoves[0].equals(lastThreeMoves[2]) && lastThreeMoves[1].equals(nextMove);
    }

    public static Direction turnLeft(Direction dir) {
        return dir.rotateLeft().rotateLeft();
    }

    public static Direction turnRight(Direction dir) {
        return dir.rotateRight().rotateRight();
    }

    public static Direction[] pathfinder(MapLocation targetLocation, RobotController rc, Direction[] lastThreeMoves)
        throws GameActionException {
        Direction targetDirection = rc.getLocation().directionTo(targetLocation);
        if (targetDirection.equals(Direction.CENTER)) {
            Direction[] ret = { lastThreeMoves[1], lastThreeMoves[2], Direction.CENTER };
            return ret;
        }
        Direction[] possibleMoves = {
            targetDirection.rotateLeft().rotateLeft(),
            targetDirection.rotateLeft(),
            targetDirection,
            targetDirection.rotateRight(),
            targetDirection.rotateRight().rotateRight(),
        };
        Direction bestMove = null;
        int bestScore = -500;
        Direction move = null;
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
            if (isRepeatMove(lastThreeMoves, bestMove)) {
                bestMove = rng.nextBoolean() ? turnLeft(bestMove) : turnRight(bestMove);
            }
            move = bestMove;
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
            if (bestBackupMove != null) {
                if (isRepeatMove(lastThreeMoves, bestBackupMove)) {
                    bestBackupMove = rng.nextBoolean() ? turnLeft(bestBackupMove) : turnRight(bestBackupMove);
                }
                move = bestBackupMove;
                rc.move(bestBackupMove);
            }
        }
        Direction[] ret = { lastThreeMoves[1], lastThreeMoves[2], move };
        return ret;
    }

    public static void addEnemyArchon(RobotController rc, MapLocation loc) throws GameActionException {
        int encoding = loc.x * 100 + loc.y;
        for (int i = enemyArchonStartIndex; i < enemyArchonStopIndex; i++) {
            if (rc.readSharedArray(i) == encoding) {
                return;
            } else if (rc.readSharedArray(i) == 0) {
                rc.writeSharedArray(i, encoding);
                return;
            }
        }
    }

    // check if shared array's locations for enemy archons are still accurate
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
        return new MapLocation(encoding / 100, encoding % 100);
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
