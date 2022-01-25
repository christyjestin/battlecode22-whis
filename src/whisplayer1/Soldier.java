package whisplayer1;

import battlecode.common.*;
import java.util.Random;

public strictfp class Soldier {

    static int visionRadiusSquared = RobotType.SOLDIER.visionRadiusSquared;
    static int actionRadiusSquared = RobotType.SOLDIER.actionRadiusSquared;
    static int mapWidth = -1;
    static int mapHeight = -1;
    static Team ownTeam = null;
    static Team opponent = null;
    static MapLocation center = null;

    final Random rng = new Random();
    MapLocation exploreDest = null;
    boolean goingTowardGuess = true;
    boolean reportedDeath = false;
    Boolean defenseMode = null;
    MapLocation defendingLocation = null;
    Boolean reserveMode = rng.nextBoolean();
    Direction[] lastThreeMoves = { null, null, null };
    Direction nextMove = null;
    NoLead noLead = null;
    MapLocation savedArchonLocation = null;
    MapLocation meetupPoint = null;

    MapLocation randomLocation() throws GameActionException {
        return new MapLocation(rng.nextInt(mapWidth), rng.nextInt(mapHeight));
    }

    boolean isArchonAtLocation(RobotController rc, MapLocation loc) throws GameActionException {
        return rc.canSenseRobotAtLocation(loc) && rc.senseRobotAtLocation(loc).getType().equals(RobotType.ARCHON);
    }

    int nearbySoldiersCount(RobotController rc) throws GameActionException {
        RobotInfo[] nearbyBots = rc.senseNearbyRobots(visionRadiusSquared, ownTeam);
        int counter = 0;
        for (RobotInfo bot : nearbyBots) {
            if (bot.getType().equals(RobotType.SOLDIER)) counter++;
        }
        return counter;
    }

    MapLocation mostThreatenedArchonLocation(RobotController rc) throws GameActionException {
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

    MapLocation retrieveArchonLocationGuess(RobotController rc) throws GameActionException {
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

    // check if the guess has been removed from the shared array by another soldier yet
    boolean archonGuessStillValid(RobotController rc, MapLocation guess) throws GameActionException {
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

    public void runSoldier(RobotController rc) throws GameActionException {
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
            exploreDest = randomLocation();
            goingTowardGuess = false;
        }
        if (noLead == null) noLead = new NoLead(rc, visionRadiusSquared, mapHeight, mapWidth);

        RobotPlayer.updateEnemyArchons(rc, visionRadiusSquared, opponent);
        RobotPlayer.updateGoldDeposits(rc, visionRadiusSquared);
        MapLocation rcLocation = rc.getLocation();
        noLead.updateGrid(rcLocation);

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

        // if the team has figured out that the guess is invalid, then go towards another guess
        if (goingTowardGuess && !archonGuessStillValid(rc, exploreDest)) {
            MapLocation guess = retrieveArchonLocationGuess(rc);
            goingTowardGuess = guess != null;
            exploreDest = goingTowardGuess ? guess : randomLocation();
        }

        // randomly generate a new target location if you get close enough to it, and you're not a reserve soldier
        if (!reserveMode && !goingTowardGuess && closeEnoughTo(rcLocation, exploreDest, actionRadiusSquared / 2)) {
            exploreDest = randomLocation();
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
        } else if (defenseMode == null && RobotPlayer.anyArchonHealthDrops(rc)) {
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
