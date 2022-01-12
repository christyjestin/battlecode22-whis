package whisplayer1;

import battlecode.common.*;

import java.util.HashMap;
import java.util.Random;

public class Soldier {

    static final Random rng = new Random();

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

    static HashMap<Integer, MapLocation> exploreDest = new HashMap<Integer, MapLocation>();
    static MapLocation archonLocation = null;

    /**
     * Run a single turn for a Soldier.
     * This code is wrapped inside the infinite loop in run(), so it is called once
     * per turn.
     */
    public static void runSoldier(RobotController rc) throws GameActionException {
        if (!rc.isActionReady() && !rc.isMovementReady())
            return;

        final int id = rc.getID();
        if (!exploreDest.containsKey(id))
            exploreDest.put(id, new MapLocation(rng.nextInt(rc.getMapWidth()), rng.nextInt(rc.getMapHeight())));

        // Try to attack someone
        int radiusSquared = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radiusSquared, opponent);
        MapLocation target = null;
        for (RobotInfo enemy : enemies) {
            MapLocation loc = enemy.getLocation();
            if (rc.canAttack(loc)) {
                if (enemy.getType().equals(RobotType.ARCHON)) {
                    rc.attack(loc);
                    archonLocation = loc;
                    target = null;
                    break;
                } else {
                    target = loc;
                }
            }
        }

        if (target != null) {
            rc.attack(target);
        }

        if (!rc.isMovementReady())
            return;

        // move using pathfinder algorithm
        MapLocation targetLocation = (archonLocation != null) ? archonLocation : exploreDest.get(id);

        if (targetLocation == rc.getLocation() && archonLocation == null) {
            exploreDest.put(id, new MapLocation(rng.nextInt(rc.getMapWidth()), rng.nextInt(rc.getMapHeight())));
            targetLocation = exploreDest.get(id);
        }

        RobotPlayer.pathfinder(targetLocation, rc);
    }
}
