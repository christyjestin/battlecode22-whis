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
     * Archons will switch after they've spawned `spawnThreshold` times or after
     * they've
     * moved `moveThreshold` times
     */
    static final int spawnThreshold = 10;
    static final int moveThreshold = 20;

    /**
     * Run a single turn for an Archon.
     * This code is wrapped inside the infinite loop in run(), so it is called once
     * per turn.
     */
    static void runArchon(RobotController rc) throws GameActionException {
        /** indicates how many miners the Archons have tried spawning */
        int spawned = 0;
        while (spawned < spawnThreshold) {
            // Pick a direction to build in.
            Direction dir = directions[rng.nextInt(directions.length)];
            rc.setIndicatorString("Trying to build a miner");
            // only spawn miners for now
            if (rc.canBuildRobot(RobotType.MINER, dir)) {
                rc.buildRobot(RobotType.MINER, dir);
                spawned++;
            }
        }

        MapLocation targetLocation = null;

        /** look for other archons */
        for (RobotInfo robot : rc.senseNearbyRobots()) {
            if (robot.getType().equals(rc.getType()) && robot.getID() != rc.getID()) {
                targetLocation = robot.getLocation();
                /** stop looking if you find one on the other team */
                if (!robot.getTeam().equals(rc.getTeam()))
                    break;
            }
        }

        /** wait until it can and then transform to portable */
        while (!rc.canTransform()) {
        }
        rc.transform();

        /** Move towards another archon if it can sense one; otherwise move randomly */
        // in case this comes up, this is a ternary operator
        // "a ? b : c" is equivalent to "if a then b else c"
        Direction toMove = targetLocation != null ? rc.getLocation().directionTo(targetLocation)
                : directions[rng.nextInt(directions.length)];

        int triedMove = 0;
        while (triedMove < moveThreshold) {
            if (rc.canMove(toMove)) {
                rc.move(toMove);
                System.out.println("I moved!");
            }
            triedMove++;
        }

        /** wait until it can and then transform to turret */
        while (!rc.canTransform()) {
        }
        rc.transform();

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
