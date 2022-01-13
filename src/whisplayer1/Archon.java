package whisplayer1;

import battlecode.common.*;
import java.util.Random;

public strictfp class Archon {

    /**
     * A random number generator.
     */
    static final Random rng = new Random(6147);
    static int spawnCounter = 0;
    static int ratio = 5;
    /**
     * Array containing all the possible movement directions.
     */
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
    static boolean wroteToSharedArray = false;
    static final int visionRadiusSquared = RobotType.ARCHON.visionRadiusSquared;
    static Team opponent = null;
    static int healthIndex = -1;

    // write the archon's health to the shared array and get the minimum health of all archons
    static int minArchonHealth(RobotController rc) throws GameActionException {
        if (healthIndex == -1) {
            // retrieve an index for this archon
            healthIndex = rc.readSharedArray(RobotPlayer.archonCounterIndex);
            RobotPlayer.incrementArray(rc, RobotPlayer.archonCounterIndex);
        }
        int health = rc.getHealth();
        int index = RobotPlayer.ownArchonStartIndex + healthIndex;
        // if you're close to dying, reset the array; otherwise just write your health
        rc.writeSharedArray(index, (health < 50) ? 0 : health);
        int minHealth = RobotType.ARCHON.getMaxHealth(3);
        for (int i = RobotPlayer.ownArchonStartIndex; i < RobotPlayer.ownArchonStopIndex; i++) {
            int val = rc.readSharedArray(i);
            if (val != 0 && val < minHealth) minHealth = val;
        }
        return minHealth;
    }

    /**
     * Run a single turn for an Archon.
     * This code is wrapped inside the infinite loop in run(), so it is called once
     * per turn.
     */
    static void runArchon(RobotController rc) throws GameActionException {
        // write lead deposit to shared array
        if (!wroteToSharedArray) {
            MapLocation[] leadDeposits = rc.senseNearbyLocationsWithLead(RobotType.ARCHON.visionRadiusSquared);
            if (leadDeposits.length > 0) {
                for (int i = 0; i < rc.getArchonCount(); i++) {
                    if (rc.readSharedArray(i) == 0) {
                        rc.writeSharedArray(i, leadDeposits[0].x * 100 + leadDeposits[0].y);
                        wroteToSharedArray = true;
                        break;
                    }
                }
            }
        }

        // init code code
        if (center == null) center = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
        if (opponent == null) opponent = rc.getTeam().opponent();

        // randomly choose a spawn direction facing the center of the map
        Direction towardsCenter = rc.getLocation().directionTo(center);
        Direction towardsRight = towardsCenter.rotateRight();
        Direction towardsLeft = towardsCenter.rotateLeft();
        Direction[] centerDirections = { towardsRight, towardsCenter, towardsLeft };
        Direction dir = centerDirections[rng.nextInt(centerDirections.length)];

        // stop early if you already have robots on over a third of the map
        if (rc.getRobotCount() > (rc.getMapHeight() * rc.getMapWidth() / 3)) return;

        // spawn both miners and soldiers in a dynamic ratio
        RobotType spawnType = (spawnCounter % 10) < ratio ? RobotType.MINER : RobotType.SOLDIER;
        int countIndex = (spawnCounter % 10) < ratio ? RobotPlayer.minerCountIndex : RobotPlayer.soldierCountIndex;
        if (rc.canBuildRobot(spawnType, dir)) {
            rc.buildRobot(spawnType, dir);
            RobotPlayer.incrementArray(rc, countIndex);
            spawnCounter++;
        }

        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(visionRadiusSquared, rc.getTeam().opponent());
        if (minArchonHealth(rc) < RobotType.ARCHON.getMaxHealth(1) || nearbyEnemies.length > 0) {
            // only spawn soldiers
            ratio = 0;
        } else if (RobotPlayer.enemyArchonDetected(rc)) {
            // spawn 20% miners, 80% soldiers
            ratio = 2;
        } else {
            // normal state: 50% miners, 50% soldiers
            ratio = 5;
        }
    }
}
