package whisplayer1;

import battlecode.common.*;
import java.util.Random;

public strictfp class RobotPlayer {

    static int turnCount = 0;

    static final Random rng = new Random();

    static final int minerCountIndex = 0;
    static final int soldierCountIndex = 1;
    static final int builderCountIndex = 2;
    static final int noLeadGridStartIndex = 3;
    static final int noLeadGridStopIndex = 12;
    static final int goldDepositStartIndex = noLeadGridStopIndex;
    static final int goldDepositStopIndex = goldDepositStartIndex + 8;

    static final int enemyArchonStartIndex = GameConstants.SHARED_ARRAY_LENGTH - GameConstants.MAX_STARTING_ARCHONS;
    static final int enemyArchonStopIndex = GameConstants.SHARED_ARRAY_LENGTH;
    static final int archonHealthDropStartIndex = enemyArchonStartIndex - GameConstants.MAX_STARTING_ARCHONS;
    static final int archonHealthDropStopIndex = enemyArchonStartIndex;
    static final int archonLocationStartIndex = archonHealthDropStartIndex - GameConstants.MAX_STARTING_ARCHONS;
    static final int archonLocationStopIndex = archonHealthDropStartIndex;
    static final int archonSpawnStartIndex = archonLocationStartIndex - GameConstants.MAX_STARTING_ARCHONS;
    static final int archonSpawnStopIndex = archonLocationStartIndex;
    static final int archonGuessStartIndex = archonSpawnStartIndex - GameConstants.MAX_STARTING_ARCHONS * 3;
    static final int archonGuessStopIndex = archonSpawnStartIndex;
    static final int archonCounterIndex = archonGuessStartIndex - 1;

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
                            rc.readSharedArray(60) +
                            " " +
                            rc.readSharedArray(61) +
                            " " +
                            rc.readSharedArray(62) +
                            " " +
                            rc.readSharedArray(63) +
                            " " +
                            rc.readSharedArray(36) +
                            " " +
                            rc.readSharedArray(37) +
                            " " +
                            rc.readSharedArray(38) +
                            " " +
                            rc.readSharedArray(39) +
                            " " +
                            rc.readSharedArray(40) +
                            " " +
                            rc.readSharedArray(41) +
                            " " +
                            rc.readSharedArray(42) +
                            " " +
                            rc.readSharedArray(43) +
                            " " +
                            rc.readSharedArray(44) +
                            " " +
                            rc.readSharedArray(45) +
                            " " +
                            rc.readSharedArray(46) +
                            " " +
                            rc.readSharedArray(47)
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
                        Laboratory.runLaboratory(rc);
                        break;
                    case BUILDER:
                        Builder.runBuilder(rc);
                        break;
                    case WATCHTOWER:
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
        return (
            !lastThreeMoves[0].equals(lastThreeMoves[1]) &&
            lastThreeMoves[0].equals(lastThreeMoves[2]) &&
            lastThreeMoves[1].equals(nextMove)
        );
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
            Direction[] ret = { lastThreeMoves[1], lastThreeMoves[2], Direction.CENTER, null };
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

    public static void addEnemyArchon(RobotController rc, MapLocation loc) throws GameActionException {
        removeArchonLocationGuess(rc, loc, true);
        int encoding = loc.x * 100 + loc.y;
        for (int i = enemyArchonStartIndex; i < enemyArchonStopIndex; i++) {
            if (rc.readSharedArray(i) == encoding) return;
        }
        for (int i = enemyArchonStartIndex; i < enemyArchonStopIndex; i++) {
            if (rc.readSharedArray(i) == 0) {
                rc.writeSharedArray(i, encoding);
                return;
            }
        }
    }

    public static void removeArchonLocationGuess(RobotController rc, MapLocation guess, boolean correct)
        throws GameActionException {
        int encoding = guess.x * 100 + guess.y;
        for (int i = archonGuessStartIndex; i < archonGuessStopIndex; i++) {
            // find the index of the guess' encoding (if it exists - may have already been removed)
            if (encoding != rc.readSharedArray(i)) continue;

            rc.writeSharedArray(i, 0);
            int category = (i - RobotPlayer.archonGuessStartIndex) / GameConstants.MAX_STARTING_ARCHONS;
            for (int m = 0; m < 3; m++) {
                // if the guess is right, then remove all of the guesses except for the ones in the right category
                // (xAxis, yAxis, rotational); otherwise remove the guesses that are in the wrong category
                if (correct ? m == category : m != category) continue;
                for (int n = 0; n < GameConstants.MAX_STARTING_ARCHONS; n++) {
                    int index = RobotPlayer.archonGuessStartIndex + (m * GameConstants.MAX_STARTING_ARCHONS) + n;
                    if (rc.readSharedArray(index) != 0) rc.writeSharedArray(index, 0);
                }
            }
            return;
        }
    }

    public static boolean fixedPositionType(RobotType type) throws GameActionException {
        return type.equals(RobotType.ARCHON) || type.equals(RobotType.BUILDER) || type.equals(RobotType.LABORATORY);
    }

    // check if shared array's locations for enemy archons are still accurate
    public static void checkEnemyArchons(RobotController rc) throws GameActionException {
        for (int i = enemyArchonStartIndex; i < enemyArchonStopIndex; i++) {
            int encoding = rc.readSharedArray(i);
            if (encoding == 0) continue;
            MapLocation a = new MapLocation(encoding / 100, encoding % 100);
            if (rc.canSenseLocation(a)) {
                // reset array value if there's no robot at that index or if the robot there is not an archon
                if (!rc.canSenseRobotAtLocation(a) || !rc.senseRobotAtLocation(a).getType().equals(RobotType.ARCHON)) {
                    rc.writeSharedArray(i, 0);
                }
            }
        }
    }

    public static void checkGuesses(RobotController rc) throws GameActionException {
        for (int i = archonGuessStartIndex; i < archonGuessStopIndex; i++) {
            int encoding = rc.readSharedArray(i);
            if (encoding == 0) continue;

            MapLocation guess = new MapLocation(encoding / 100, encoding % 100);
            if (rc.canSenseLocation(guess)) {
                boolean foundArchon =
                    rc.canSenseRobotAtLocation(guess) &&
                    rc.senseRobotAtLocation(guess).getType().equals(RobotType.ARCHON);
                removeArchonLocationGuess(rc, guess, foundArchon);
            }
        }
    }

    public static void updateEnemyArchons(RobotController rc, int visionRadiusSquared, Team opponent)
        throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(visionRadiusSquared, opponent);
        for (RobotInfo enemy : enemies) {
            if (enemy.getType().equals(RobotType.ARCHON)) addEnemyArchon(rc, enemy.getLocation());
        }
        checkGuesses(rc);
        checkEnemyArchons(rc);
    }

    public static void addGoldDeposits(RobotController rc, MapLocation[] goldLocations) throws GameActionException {
        for (MapLocation goldLocation : goldLocations) {
            int encoding = goldLocation.x * 100 + goldLocation.y;
            for (int i = goldDepositStartIndex; i < goldDepositStopIndex; i++) {
                if (rc.readSharedArray(i) == encoding) return;
            }
            for (int i = goldDepositStartIndex; i < goldDepositStopIndex; i++) {
                if (rc.readSharedArray(i) == 0) {
                    rc.writeSharedArray(i, encoding);
                    return;
                }
            }
        }
    }

    public static void updateGoldDeposits(RobotController rc, int visionRadiusSquared) throws GameActionException {
        for (int i = goldDepositStartIndex; i < goldDepositStopIndex; i++) {
            int encoding = rc.readSharedArray(i);
            if (encoding == 0) continue;
            MapLocation goldLocation = new MapLocation(encoding / 100, encoding % 100);
            if (rc.canSenseLocation(goldLocation) && rc.senseGold(goldLocation) == 0) rc.writeSharedArray(i, 0);
        }
        addGoldDeposits(rc, rc.senseNearbyLocationsWithGold(visionRadiusSquared));
    }

    public static MapLocation nearestGoldDeposit(RobotController rc, MapLocation rcLocation)
        throws GameActionException {
        MapLocation deposit = null;
        int closestDistance = 7200;
        for (int i = goldDepositStartIndex; i < goldDepositStopIndex; i++) {
            int encoding = rc.readSharedArray(i);
            if (encoding == 0) continue;
            MapLocation goldLocation = new MapLocation(encoding / 100, encoding % 100);
            int distance = rcLocation.distanceSquaredTo(goldLocation);
            if (distance < closestDistance) {
                deposit = goldLocation;
                distance = closestDistance;
            }
        }
        return deposit;
    }

    static Direction findBestSpawnDirection(RobotController rc) throws GameActionException {
        int minRubble = GameConstants.MAX_RUBBLE + 1;
        Direction bestDirection = null;
        for (Direction direction : directions) {
            MapLocation loc = rc.adjacentLocation(direction);
            // skip this location if there's an archon there
            if (rc.canSenseRobotAtLocation(loc) && fixedPositionType(rc.senseRobotAtLocation(loc).getType())) continue;
            int rubble = rc.senseRubble(loc);
            if (rubble < minRubble) {
                minRubble = rubble;
                bestDirection = direction;
            }
        }
        return bestDirection;
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
