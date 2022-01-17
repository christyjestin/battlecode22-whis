package whisplayer1;

import battlecode.common.*;
import java.util.Random;

public strictfp class Archon {

    static final Random rng = new Random();
    static int ratio = 5;

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

    static MapLocation center = null;
    static boolean wroteLocation = false;
    static final int visionRadiusSquared = RobotType.ARCHON.visionRadiusSquared;
    static Team opponent = null;
    static Team team = null;
    static int archonIndex = -1;
    static int mapWidth = -1;
    static int mapHeight = -1;
    static Direction towardsCenter = null;
    static Direction towardsRight = null;
    static Direction towardsLeft = null;
    static Direction[] centerDirections = null;
    static RubbleGrid rubbleGrid = null;

    // check if it is this archon's turn to spawn
    static boolean myTurn(RobotController rc, int index) throws GameActionException {
        int myIndex = RobotPlayer.archonSpawnStartIndex + index;
        int myCount = rc.readSharedArray(myIndex);
        int totalArchons = rc.readSharedArray(RobotPlayer.archonCounterIndex);
        for (int i = RobotPlayer.archonSpawnStartIndex; i < RobotPlayer.archonSpawnStartIndex + totalArchons; i++) {
            // lowest count gets to go first; if there's a tie, lower index gets to go first
            if (i < myIndex && rc.readSharedArray(i) <= myCount) return false;
            if (i > myIndex && rc.readSharedArray(i) < myCount) return false;
        }
        return true;
    }

    // write the archon's health to the shared array and get the minimum health of all archons
    static int minArchonHealth(RobotController rc) throws GameActionException {
        if (archonIndex == -1) {
            // retrieve an index for this archon
            archonIndex = rc.readSharedArray(RobotPlayer.archonCounterIndex);
            RobotPlayer.incrementArray(rc, RobotPlayer.archonCounterIndex);
        }
        int health = rc.getHealth();
        int healthIndex = RobotPlayer.archonHealthStartIndex + archonIndex;
        int spawnIndex = RobotPlayer.archonSpawnStartIndex + archonIndex;
        // if you're close to dying, reset the array; otherwise just write your health
        rc.writeSharedArray(healthIndex, (health < 50) ? 0 : health);
        // also reset spawn counter in array if you're close to dying
        if (health < 50) rc.writeSharedArray(spawnIndex, GameConstants.MAX_SHARED_ARRAY_VALUE);
        int minHealth = RobotType.ARCHON.getMaxHealth(3);
        for (int i = RobotPlayer.archonHealthStartIndex; i < RobotPlayer.archonHealthStopIndex; i++) {
            int val = rc.readSharedArray(i);
            if (val != 0 && val < minHealth) minHealth = val;
        }
        return minHealth;
    }

    // write the archon's location to the shared array
    static void writeArchonLocation(RobotController rc) throws GameActionException {
        if (archonIndex == -1) {
            // retrieve an index for this archon
            archonIndex = rc.readSharedArray(RobotPlayer.archonCounterIndex);
            RobotPlayer.incrementArray(rc, RobotPlayer.archonCounterIndex);
        }
        int index = RobotPlayer.archonLocationStartIndex + archonIndex;
        MapLocation loc = rc.getLocation();
        rc.writeSharedArray(index, loc.x * 100 + loc.y);
        wroteLocation = true;
    }

    static void runArchon(RobotController rc) throws GameActionException {
        // write to shared array for defense
        if (!wroteLocation) writeArchonLocation(rc);

        // init code code
        if (mapWidth == -1) mapWidth = rc.getMapWidth();
        if (mapHeight == -1) mapHeight = rc.getMapHeight();
        if (center == null) center = new MapLocation(mapWidth / 2, mapHeight / 2);
        if (towardsCenter == null) towardsCenter = rc.getLocation().directionTo(center);
        if (towardsCenter.equals(Direction.CENTER)) towardsCenter = directions[rng.nextInt(directions.length)];
        if (towardsRight == null) towardsCenter.rotateRight();
        if (towardsLeft == null) towardsCenter.rotateLeft();
        if (centerDirections == null) centerDirections = new Direction[] { towardsRight, towardsCenter, towardsLeft };
        if (team == null) team = rc.getTeam();
        if (opponent == null) opponent = team.opponent();
        if (rubbleGrid == null) rubbleGrid = new RubbleGrid(rc, visionRadiusSquared, mapHeight, mapWidth);

        MapLocation rcLocation = rc.getLocation();
        MapLocation[] nearbyLocations = rc.getAllLocationsWithinRadiusSquared(rcLocation, visionRadiusSquared);
        rubbleGrid.updateGridFromNearbyLocations(rcLocation, nearbyLocations);

        RobotPlayer.updateEnemyArchons(rc, visionRadiusSquared, opponent);

        // write this archon's health and find out the lowest health on your team
        // I placed this function call up here because it needs to happen on every turn (i.e. before
        // the return statement below)
        int lowestHealth = minArchonHealth(rc);
        boolean tooManyBots = rc.getRobotCount() > (mapHeight * mapWidth / 3);
        boolean tooLittleLead = rc.getTeamLeadAmount(team) < RobotType.SOLDIER.buildCostLead;
        // stop early if you already have robots on over a third of the map, if it's not your turn,
        // if you can't spawn, or if there's too little lead
        if (tooManyBots || !myTurn(rc, archonIndex) || !rc.isActionReady() || tooLittleLead) return;

        // spawn both miners and soldiers in a dynamic ratio
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(visionRadiusSquared, opponent);
        if (lowestHealth < RobotType.ARCHON.getMaxHealth(1) || nearbyEnemies.length > 0) {
            // spawn 10% miners, 90% soldiers
            ratio = 1;
        } else if (RobotPlayer.enemyArchonDetected(rc)) {
            // spawn 30% miners, 70% soldiers
            ratio = 3;
        } else {
            // normal state: 50% miners, 50% soldiers
            int numMiners = rc.readSharedArray(RobotPlayer.minerCountIndex);
            int numSoldiers = rc.readSharedArray(RobotPlayer.soldierCountIndex);
            int total = numMiners + numSoldiers;
            // if we don't have any robots, spawn only miners
            // otherwise spawn them such that we move closer to having 50/50
            ratio = (total > 0) ? (10 * numSoldiers / total) : 10;
        }

        int randomInt = rng.nextInt(10);
        RobotType spawnType = randomInt < ratio ? RobotType.MINER : RobotType.SOLDIER;
        int countIndex = randomInt < ratio ? RobotPlayer.minerCountIndex : RobotPlayer.soldierCountIndex;
        RobotPlayer.incrementArray(rc, RobotPlayer.archonSpawnStartIndex + archonIndex);
        Direction leftPointer = centerDirections[rng.nextInt(centerDirections.length)];
        Direction rightPointer = leftPointer.rotateRight();
        // go through the directions and try to spawn a bot
        for (int i = 0; i < 4; i++) {
            if (rc.canBuildRobot(spawnType, leftPointer)) {
                rc.buildRobot(spawnType, leftPointer);
                RobotPlayer.incrementArray(rc, countIndex);
                break;
            } else if (rc.canBuildRobot(spawnType, rightPointer)) {
                rc.buildRobot(spawnType, rightPointer);
                RobotPlayer.incrementArray(rc, countIndex);
                break;
            }
            leftPointer = leftPointer.rotateLeft();
            rightPointer = rightPointer.rotateRight();
        }
    }
}
