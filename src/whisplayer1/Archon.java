package whisplayer1;

import battlecode.common.*;
import java.util.Random;

public strictfp class Archon {
    /** A random number generator. */
    static final Random rng = new Random(6147);

    /** Array containing all the possible movement directions. */
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
    static int minerCount = 0;

    /**
     * Run a single turn for an Archon.
     * This code is wrapped inside the infinite loop in run(), so it is called once
     * per turn.
     */
    static void runArchon(RobotController rc) throws GameActionException {
        // Try to attack someone
        int radiusSquared = rc.getType().actionRadiusSquared;
        MapLocation[] leadDeposit = rc.senseNearbyLocationsWithLead();
        for (MapLocation lead : leadDeposit) {
            if (rc.canAttack(loc)) {
                if (enemy.getType().equals(RobotType.ARCHON)) {
                    rc.attack(loc);

                    rc.writeSharedArray(30,loc.x);
                    rc.writeSharedArray(31,loc.y);
                    if(!rc.canSenseRobotAtLocation(new MapLocation(loc.x,loc.y))){
                        rc.writeSharedArray(30,0);
                        rc.writeSharedArray(31,0);
                    }
                    target = null;
                    break;
                } else {
                    target = loc;
                }
            }
        }


        if (center == null)
            center = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
        // some directions
        Direction towardsCenter = rc.getLocation().directionTo(center);
        Direction towardsRight = towardsCenter.rotateRight();
        Direction towardsLeft = towardsCenter.rotateLeft();
        while (true) {
            int random = rng.nextInt(3);
            Direction dir = random == 0 ? towardsRight : (random == 1 ? towardsCenter : towardsLeft);
            if (minerCount < 100 && rng.nextBoolean()) {
                // spawn both miners and soldiers
                if (rc.canBuildRobot(RobotType.MINER, dir)) {
                    rc.buildRobot(RobotType.MINER, dir);
                    minerCount++;
                }
            } else {
                if (rc.canBuildRobot(RobotType.SOLDIER, dir)) {
                    rc.buildRobot(RobotType.SOLDIER, dir);
                }
            }
        }
    }
}
