package whisplayer1;

import battlecode.common.*;
import java.util.Random;

public strictfp class RobotPlayer {

    static int turnCount = 0;

    static final Random rng = new Random();

    static final int minerCountIndex = 0;
    static final int soldierCountIndex = 1;
    static final int builderCountIndex = 2;
    static final int rubbleGridStartIndex = 3;

    static final int enemyArchonStartIndex = GameConstants.SHARED_ARRAY_LENGTH - GameConstants.MAX_STARTING_ARCHONS;
    static final int enemyArchonStopIndex = GameConstants.SHARED_ARRAY_LENGTH;
    static final int archonHealthStartIndex = enemyArchonStartIndex - GameConstants.MAX_STARTING_ARCHONS;
    static final int archonHealthStopIndex = enemyArchonStartIndex;
    static final int archonLocationStartIndex = archonHealthStartIndex - GameConstants.MAX_STARTING_ARCHONS;
    static final int archonLocationStopIndex = archonHealthStartIndex;
    static final int archonSpawnStartIndex = archonLocationStartIndex - GameConstants.MAX_STARTING_ARCHONS;
    static final int archonSpawnStopIndex = archonLocationStartIndex;
    static final int archonCounterIndex = archonSpawnStartIndex - 1;

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
        Direction currentMove = null;
        boolean redirected = false;
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
                redirected = true;
            }
            currentMove = bestMove;
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
                    redirected = true;
                }
                currentMove = bestBackupMove;
                rc.move(bestBackupMove);
            }
        }
        Direction[] ret = { lastThreeMoves[1], lastThreeMoves[2], currentMove, redirected ? currentMove : null };
        return ret;
    }

    // utilizes rubble grid to make better macro decisions
    public static Direction[] rubblePathfinder(
        RubbleGrid rubbleGrid,
        MapLocation rcLocation,
        MapLocation targetLocation,
        RobotController rc,
        Direction[] lastThreeMoves
    )
        throws GameActionException {
        // if you're already in the right grid square, just use the regular pathfinding algorithm
        if (rubbleGrid.sameGridSquare(rcLocation, targetLocation)) {
            return pathfinder(targetLocation, rc, lastThreeMoves);
        }
        // otherwise find the best grid square to go to, and use pathfinder algorithm to go there
        int[] rcGridIndices = rubbleGrid.gridIndexFromLocation(rcLocation);
        Direction direction = rcLocation.directionTo(targetLocation);

        int[] straightGridSquare = rubbleGrid.gridSquareInDirection(rcGridIndices, direction);
        int[] rightGridSquare = rubbleGrid.gridSquareInDirection(rcGridIndices, direction.rotateRight());
        int[] leftGridSquare = rubbleGrid.gridSquareInDirection(rcGridIndices, direction.rotateLeft());

        int straightScore = rubbleGrid.rubbleScoreAtGridSquare(straightGridSquare);
        int rightScore = rubbleGrid.rubbleScoreAtGridSquare(rightGridSquare) + 2;
        int leftScore = rubbleGrid.rubbleScoreAtGridSquare(leftGridSquare) + 2;

        MapLocation intermediateTarget = null;
        if (straightScore <= rightScore && straightScore <= leftScore) {
            intermediateTarget = rubbleGrid.centerLocation(straightGridSquare);
        } else if (rightScore <= straightScore && rightScore <= leftScore) {
            intermediateTarget = rubbleGrid.centerLocation(rightGridSquare);
        } else {
            intermediateTarget = rubbleGrid.centerLocation(leftGridSquare);
        }
        return pathfinder(intermediateTarget, rc, lastThreeMoves);
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

    public static void updateEnemyArchons(RobotController rc, int visionRadiusSquared, Team opponent)
        throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(visionRadiusSquared, opponent);
        for (RobotInfo enemy : enemies) {
            if (enemy.getType().equals(RobotType.ARCHON)) addEnemyArchon(rc, enemy.getLocation());
        }
        checkEnemyArchons(rc);
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
