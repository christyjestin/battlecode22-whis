package whisplayer1;

import battlecode.common.*;
import java.util.Random;

public strictfp class Soldier {

    static final Random rng = new Random();

    static MapLocation exploreDest = null;
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

    static boolean closeEnoughTo(RobotController rc, MapLocation loc, int distanceSquared) throws GameActionException {
        return rc.getLocation().isWithinDistanceSquared(loc, distanceSquared);
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
        if (exploreDest == null) exploreDest = reserveMode ? center : randomLocation(rc);

        RobotPlayer.updateEnemyArchons(rc, visionRadiusSquared, opponent);

        if (!rc.isActionReady() && !rc.isMovementReady()) return;

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

        if (!rc.isMovementReady()) return;

        // randomly generate a new target location if you get close enough to it, and you're not a reserve soldier
        if (!reserveMode && closeEnoughTo(rc, exploreDest, actionRadiusSquared / 2)) {
            exploreDest = randomLocation(rc);
        }
        MapLocation targetLocation = exploreDest;

        // note that currently, once defenseMode is set to false, it won't ever change
        if (defenseMode != null && defenseMode) {
            // if the archon isn't there anymore, then stop defending
            if (rc.canSenseLocation(defendingLocation) && !isArchonAtLocation(rc, defendingLocation)) {
                defenseMode = null;
                // if there are too many robots nearby, have half of the robots stop defending
            } else if (closeEnoughTo(rc, defendingLocation, 10) && nearbySoldiersCount(rc) > actionRadiusSquared) {
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
                    targetLocation = RobotPlayer.retrieveLocationfromArray(rc, i);
                    break;
                }
            }
        }

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
