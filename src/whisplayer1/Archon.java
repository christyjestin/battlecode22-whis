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

    /**
     * Run a single turn for an Archon.
     * This code is wrapped inside the infinite loop in run(), so it is called once
     * per turn.
     */
    static void runArchon(RobotController rc) throws GameActionException {
            // Pick a direction to build in.
            Direction dir = directions[rng.nextInt(directions.length)];
            rc.setIndicatorString("Trying to build a miner");
            // only spawn miners for now
            if (rc.canBuildRobot(RobotType.MINER, dir)) {
                rc.buildRobot(RobotType.MINER, dir);
            }


        // code to spawn 1/2 miners and 1/2 soldiers
        // if (rng.nextBoolean()) {
        // // Let's try to build a miner.
        // rc.setIndicatorString("Trying to build a miner");
        // if (rc.canBuildRobot(RobotType.MINER, dir)) {
        // rc.buildRobot(RobotType.MINER, dir);
        // }
        // } else {
        // // Let's try to build a soldier.
        // rc.setIndicatorString("Trying to build a soldier");
        // if (rc.canBuildRobot(RobotType.SOLDIER, dir)) {
        // rc.buildRobot(RobotType.SOLDIER, dir);
        // }
        // }
    }
}
