package whisplayer1;

import battlecode.common.*;
import java.util.Random;

public strictfp class Soldier {

    static final Random rng = new Random();

    static MapLocation exploreDest = null;
    static boolean goingTowardGuess = true;
    static int visionRadiusSquared = RobotType.SOLDIER.visionRadiusSquared;
    static int actionRadiusSquared = RobotType.SOLDIER.actionRadiusSquared;
    static int mapWidth = -1;
    static int mapHeight = -1;
    static Team ownTeam = null;
    static Team opponent = null;
    static boolean reportedDeath = false;
    static Boolean defenseMode = null;
    static MapLocation defendingLocation = null;
    static Boolean reserveMode = rng.nextBoolean();
    static MapLocation center = null;
    static Direction[] lastThreeMoves = { null, null, null };
    static Direction nextMove = null;
    static NoLead noLead = null;
    static MapLocation savedArchonLocation = null;
    static MapLocation meetupPoint = null;

    static MapLocation randomLocation(RobotController rc) throws GameActionException {
        return new MapLocation(rng.nextInt(mapWidth), rng.nextInt(mapHeight));
    }

    static boolean isArchonAtLocation(RobotController rc, MapLocation loc) throws GameActionException {
        return rc.canSenseRobotAtLocation(loc) && rc.senseRobotAtLocation(loc).getType().equals(RobotType.ARCHON);
    }

    static int nearbySoldiersCount(RobotController rc) throws GameActionException {
        RobotInfo[] nearbyBots = rc.senseNearbyRobots(visionRadiusSquared, ownTeam);
        int counter = 0;
        for (RobotInfo bot : nearbyBots) {
            if (bot.type.equals(RobotType.SOLDIER)) counter++;
        }
        return counter;
    }

    static boolean anyArchonHealthDrops(RobotController rc) throws GameActionException {
        for (int i = RobotPlayer.archonHealthDropStartIndex; i < RobotPlayer.archonHealthDropStopIndex; i++) {
            if (rc.readSharedArray(i) > 0) return true;
        }
        return false;
    }

    static MapLocation mostThreatenedArchonLocation(RobotController rc) throws GameActionException {
        int highestDrop = 0;
        int index = 0;
        for (int i = RobotPlayer.archonHealthDropStartIndex; i < RobotPlayer.archonHealthDropStopIndex; i++) {
            int val = rc.readSharedArray(i);
            if (val > highestDrop) {
                highestDrop = val;
                index = i - RobotPlayer.archonHealthDropStartIndex;
            }
        }
        // retrieve encoding of most threatened archon's location from shared array
        return RobotPlayer.retrieveLocationfromArray(rc, RobotPlayer.archonLocationStartIndex + index);
    }

    static MapLocation retrieveArchonLocationGuess(RobotController rc) throws GameActionException {
        int counter = 0;
        int[] guesses = new int[GameConstants.MAX_STARTING_ARCHONS * 3];
        for (int i = RobotPlayer.archonGuessStartIndex; i < RobotPlayer.archonGuessStopIndex; i++) {
            int encoding = rc.readSharedArray(i);
            if (encoding == 0) continue;
            guesses[counter] = encoding;
            counter++;
        }
        if (counter == 0) return null;
        int encoding = guesses[rng.nextInt(counter)];
        return new MapLocation(encoding / 100, encoding % 100);
    }

    static void removeArchonLocationGuess(RobotController rc, MapLocation guess, boolean correct)
        throws GameActionException {
        int encoding = guess.x * 100 + guess.y;
        for (int i = RobotPlayer.archonGuessStartIndex; i < RobotPlayer.archonGuessStopIndex; i++) {
            // find the index of the guess' encoding (if it exists - may have already been removed)
            if (encoding != rc.readSharedArray(i)) continue;

            int category = (i - RobotPlayer.archonGuessStartIndex) / GameConstants.MAX_STARTING_ARCHONS;
            for (int m = 0; m < 2; m++) {
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

    // check if the guess has been removed from the shared array by another soldier yet
    static boolean archonGuessStillValid(RobotController rc, MapLocation guess) throws GameActionException {
        int encoding = guess.x * 100 + guess.y;
        for (int i = RobotPlayer.archonGuessStartIndex; i < RobotPlayer.archonGuessStopIndex; i++) {
            if (rc.readSharedArray(i) == encoding) return true;
        }
        return false;
    }

    static boolean closeEnoughTo(MapLocation rcLocation, MapLocation loc, int distanceSquared)
        throws GameActionException {
        return rcLocation.isWithinDistanceSquared(loc, distanceSquared);
    }

    static MapLocation meetupPoint(MapLocation archonLocation) throws GameActionException {
        Direction centerDirection = archonLocation.directionTo(center);
        MapLocation meetupPoint = archonLocation;
        for (int i = 0; i < 10; i++) {
            meetupPoint = meetupPoint.add(centerDirection);
        }
        return meetupPoint;
    }

    public static void runSoldier(RobotController rc) throws GameActionException {
        // report likely soldier death
        if (rc.getHealth() < 6 && !reportedDeath) {
            RobotPlayer.decrementArray(rc, RobotPlayer.soldierCountIndex);
            reportedDeath = true;
        }

        // init code
        if (mapWidth == -1) mapWidth = rc.getMapWidth();
        if (mapHeight == -1) mapHeight = rc.getMapHeight();
        if (center == null) center = new MapLocation(mapWidth / 2, mapHeight / 2);
        if (ownTeam == null) ownTeam = rc.getTeam();
        if (opponent == null) opponent = ownTeam.opponent();
        if (exploreDest == null) exploreDest = reserveMode ? center : retrieveArchonLocationGuess(rc);
        // if we weren't able to find an archon at any of the guesses, then explore randomly
        if (exploreDest == null) {
            exploreDest = randomLocation(rc);
            goingTowardGuess = false;
        }
        if (noLead == null) noLead = new NoLead(rc, visionRadiusSquared, mapHeight, mapWidth);

        RobotPlayer.updateEnemyArchons(rc, visionRadiusSquared, opponent);
        RobotPlayer.updateGoldDeposits(rc, visionRadiusSquared);
        MapLocation rcLocation = rc.getLocation();
        // only update when it's safe since this is bytecode intensive
        if (rc.senseNearbyRobots(visionRadiusSquared, opponent).length == 0) noLead.updateGrid(rcLocation);

        // Try to attack someone
        MapLocation target = null;
        RobotInfo[] enemies = rc.senseNearbyRobots(visionRadiusSquared, opponent);
        for (RobotInfo enemy : enemies) {
            MapLocation enemyLocation = enemy.getLocation();
            if (rc.canAttack(enemyLocation)) {
                target = enemyLocation;
                if (enemy.getType().equals(RobotType.ARCHON)) break;
            }
        }
        if (target != null) rc.attack(target);

        // if you can't sense an archon at the guess location, then remove it from the shared array
        if (!reserveMode && goingTowardGuess) {
            if (closeEnoughTo(rcLocation, exploreDest, visionRadiusSquared)) {
                boolean foundArchon =
                    rc.canSenseRobotAtLocation(exploreDest) &&
                    rc.senseRobotAtLocation(exploreDest).getType().equals(RobotType.ARCHON);
                // none of the other soldiers need to search this location anymore
                removeArchonLocationGuess(rc, exploreDest, foundArchon);
                if (!foundArchon) {
                    MapLocation guess = retrieveArchonLocationGuess(rc);
                    goingTowardGuess = guess != null;
                    exploreDest = goingTowardGuess ? guess : randomLocation(rc);
                }
            } else if (!archonGuessStillValid(rc, exploreDest)) {
                MapLocation guess = retrieveArchonLocationGuess(rc);
                goingTowardGuess = guess != null;
                exploreDest = goingTowardGuess ? guess : randomLocation(rc);
            }
        }

        // randomly generate a new target location if you get close enough to it, and you're not a reserve soldier
        if (!reserveMode && closeEnoughTo(rcLocation, exploreDest, actionRadiusSquared / 2)) {
            MapLocation guess = retrieveArchonLocationGuess(rc);
            goingTowardGuess = guess != null;
            exploreDest = goingTowardGuess ? guess : randomLocation(rc);
        }
        MapLocation targetLocation = exploreDest;

        // note that currently, once defenseMode is set to false, it won't ever change
        if (defenseMode != null && defenseMode) {
            // if the archon isn't there anymore, then stop defending
            if (rc.canSenseLocation(defendingLocation) && !isArchonAtLocation(rc, defendingLocation)) {
                defenseMode = null;
                // if there are too many robots nearby, have half of the robots stop defending
            } else if (
                closeEnoughTo(rcLocation, defendingLocation, 10) && nearbySoldiersCount(rc) > actionRadiusSquared
            ) {
                defenseMode = rng.nextBoolean();
            }
            // if this robot is still a defender, go towards the archon being attacked
            if (defenseMode != null && defenseMode) targetLocation = defendingLocation;
            // if an archon is being attacked, have half of the archons go defend it
        } else if (defenseMode == null && anyArchonHealthDrops(rc)) {
            defenseMode = rng.nextBoolean();
            if (defenseMode) {
                defendingLocation = mostThreatenedArchonLocation(rc);
                targetLocation = defendingLocation;
            }
        }
        // otherwise check if we know the location of an enemy archon
        if (defenseMode == null || defenseMode == false) {
            for (int i = RobotPlayer.enemyArchonStartIndex; i < RobotPlayer.enemyArchonStopIndex; i++) {
                if (rc.readSharedArray(i) != 0) {
                    MapLocation archonLocation = RobotPlayer.retrieveLocationfromArray(rc, i);
                    if (savedArchonLocation == null || !archonLocation.equals(savedArchonLocation)) {
                        savedArchonLocation = archonLocation;
                        meetupPoint = meetupPoint(archonLocation);
                    }
                    // regroup/wait to attack if you don't have many teammates nearby
                    targetLocation = nearbySoldiersCount(rc) > 4 ? archonLocation : meetupPoint;
                    break;
                }
            }
        }

        if (!rc.isMovementReady()) return;

        rc.setIndicatorString(targetLocation.toString());
        if (nextMove == null) {
            Direction[] pathfinderReturn = RobotPlayer.pathfinder(targetLocation, rc, lastThreeMoves);
            lastThreeMoves = new Direction[] { pathfinderReturn[0], pathfinderReturn[1], pathfinderReturn[2] };
            nextMove = pathfinderReturn[3];
        } else {
            if (rc.canMove(nextMove)) {
                rc.move(nextMove);
                lastThreeMoves = new Direction[] { lastThreeMoves[1], lastThreeMoves[2], nextMove };
            }
            nextMove = null;
        }
    }
}
