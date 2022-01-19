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
    static MapLocation rcLocation = null;
    static final int visionRadiusSquared = RobotType.ARCHON.visionRadiusSquared;
    static Team ownTeam = null;
    static Team opponent = null;
    static int archonIndex = -1;
    static int mapWidth = -1;
    static int mapHeight = -1;
    static Direction towardsCenter = null;
    static Direction towardsRight = null;
    static Direction towardsLeft = null;
    static Direction[] centerDirections = null;
    static final int previousHealthDropLength = 20;
    // note that a positive value in this array means health has DROPPED
    // while a negative value means health has been gained
    static int[] previousHealthDrops = new int[previousHealthDropLength];
    static int previousHealth = RobotType.ARCHON.getMaxHealth(1);

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

    static boolean noNearbyLaboratories(RobotController rc) throws GameActionException {
        RobotInfo[] nearbyBots = rc.senseNearbyRobots(visionRadiusSquared, ownTeam);
        for (RobotInfo bot : nearbyBots) {
            if (bot.getType().equals(RobotType.LABORATORY)) return false;
        }
        return true;
    }

    static int nearbyBuildersCount(RobotController rc) throws GameActionException {
        int counter = 0;
        RobotInfo[] nearbyBots = rc.senseNearbyRobots(visionRadiusSquared, ownTeam);
        for (RobotInfo bot : nearbyBots) {
            if (bot.getType().equals(RobotType.BUILDER)) counter++;
        }
        return counter;
    }

    // write the archon's health to the shared array and get the minimum health of all archons
    static boolean anyArchonHealthDrops(RobotController rc) throws GameActionException {
        int health = rc.getHealth();
        int healthDropIndex = RobotPlayer.archonHealthDropStartIndex + archonIndex;
        int spawnIndex = RobotPlayer.archonSpawnStartIndex + archonIndex;

        // shift values back and add in latest drop
        for (int i = 0; i < previousHealthDropLength - 1; i++) previousHealthDrops[i] = previousHealthDrops[i + 1];
        previousHealthDrops[previousHealthDropLength - 1] = previousHealth - health;
        previousHealth = health;

        // if you're close to dying, reset the array
        if (health < 50) {
            rc.writeSharedArray(healthDropIndex, 0);
            // also reset spawn counter in array if you're close to dying
            rc.writeSharedArray(spawnIndex, GameConstants.MAX_SHARED_ARRAY_VALUE);
            return true;
        } else {
            // write down your health drop
            int total = 0;
            for (int i = 0; i < 10; i++) if (previousHealthDrops[i] > 0) total += previousHealthDrops[i];
            rc.writeSharedArray(healthDropIndex, total);
            if (total > 0) return true;

            for (int i = RobotPlayer.archonHealthDropStartIndex; i < RobotPlayer.archonHealthDropStopIndex; i++) {
                if (rc.readSharedArray(i) > 0) return true;
            }
            return false;
        }
    }

    // write the archon's location to the shared array
    static void writeArchonLocation(RobotController rc) throws GameActionException {
        rc.writeSharedArray(RobotPlayer.archonLocationStartIndex + archonIndex, rcLocation.x * 100 + rcLocation.y);
    }

    static void runArchon(RobotController rc) throws GameActionException {
        // retrieve an index for this archon
        if (archonIndex == -1) {
            archonIndex = rc.readSharedArray(RobotPlayer.archonCounterIndex);
            RobotPlayer.incrementArray(rc, RobotPlayer.archonCounterIndex);
        }

        // write to shared array for defense
        if (rcLocation == null) {
            rcLocation = rc.getLocation();
            writeArchonLocation(rc);
        }

        // init code code
        if (mapWidth == -1) mapWidth = rc.getMapWidth();
        if (mapHeight == -1) mapHeight = rc.getMapHeight();
        if (center == null) center = new MapLocation(mapWidth / 2, mapHeight / 2);
        if (towardsCenter == null) towardsCenter = rcLocation.directionTo(center);
        // randomly choose if a direction if the archon is already at the center of the map
        if (towardsCenter.equals(Direction.CENTER)) towardsCenter = directions[rng.nextInt(directions.length)];
        if (towardsRight == null) towardsRight = towardsCenter.rotateRight();
        if (towardsLeft == null) towardsLeft = towardsCenter.rotateLeft();
        if (centerDirections == null) centerDirections = new Direction[] { towardsRight, towardsCenter, towardsLeft };
        if (ownTeam == null) ownTeam = rc.getTeam();
        if (opponent == null) opponent = ownTeam.opponent();

        // write this archon's health drop from past 10 turns and find out if any archons on your team
        // have had a health drop
        // I placed this function call up here because it needs to happen on every turn (i.e. before
        // the return statement below)
        boolean anyHealthDrops = anyArchonHealthDrops(rc);
        boolean tooManyBots = rc.getRobotCount() > (mapHeight * mapWidth / 3);
        boolean tooLittleLead = rc.getTeamLeadAmount(ownTeam) < RobotType.SOLDIER.buildCostLead;
        // stop early if you already have robots on over a third of the map, if it's not your turn,
        // if you can't spawn, or if there's too little lead
        if (tooManyBots || !myTurn(rc, archonIndex) || !rc.isActionReady() || tooLittleLead) return;

        // spawn 2 builders next to each archon
        if (nearbyBuildersCount(rc) < 2 && rc.getRobotCount() > (int) (rc.getArchonCount() * 2.5)) {
            Direction spawnDirection = RobotPlayer.findBestSpawnDirection(rc);
            if (rc.canBuildRobot(RobotType.BUILDER, spawnDirection)) {
                rc.buildRobot(RobotType.BUILDER, spawnDirection);
                RobotPlayer.incrementArray(rc, RobotPlayer.builderCountIndex + archonIndex);
                RobotPlayer.incrementArray(rc, RobotPlayer.archonSpawnStartIndex + archonIndex);
                return;
            }
        }

        // spawn both miners and soldiers in a dynamic ratio
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(visionRadiusSquared, opponent);
        if (anyHealthDrops || nearbyEnemies.length > 0) {
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

        // if we have spawned enough bots in a reasonable ratio, then don't spawn to build up lead
        // only do this if we can either mutate to level 2 or build a laboratory
        if (rc.getRobotCount() > (mapHeight * mapWidth / 40) && ratio >= 4 && ratio <= 6) {
            if (rc.getLevel() == 1 || noNearbyLaboratories(rc)) return;
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
