package whisplayer1;

import battlecode.common.*;
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

    static MapLocation exploreDest = null;
    static int visionRadiusSquared = RobotType.SOLDIER.visionRadiusSquared;
    static int actionRadiusSquared = RobotType.SOLDIER.actionRadiusSquared;

    /**
     * Run a single turn for a Soldier.
     * This code is wrapped inside the infinite loop in run(), so it is called once
     * per turn.
     */
    public static void runSoldier(RobotController rc) throws GameActionException {
        if (!rc.isActionReady() && !rc.isMovementReady())
            return;

        if (exploreDest == null)
            exploreDest = new MapLocation(rng.nextInt(rc.getMapWidth()), rng.nextInt(rc.getMapHeight()));

        // Try to attack someone
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(actionRadiusSquared, opponent);
        MapLocation target = null;
        for (RobotInfo enemy : enemies) {
            MapLocation loc = enemy.getLocation();
            if (rc.canAttack(loc)) {
                if (enemy.getType().equals(RobotType.ARCHON)) {
                    rc.attack(loc);

                    rc.writeSharedArray(30, loc.x);
                    rc.writeSharedArray(31, loc.y);
                    if (!rc.canSenseRobotAtLocation(new MapLocation(loc.x, loc.y))) {
                        rc.writeSharedArray(30, 0);
                        rc.writeSharedArray(31, 0);
                    }
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
        MapLocation targetLocation = (rc.readSharedArray(30) != 0)
                ? new MapLocation(rc.readSharedArray(30), rc.readSharedArray(31))
                : exploreDest;

        if (targetLocation == rc.getLocation() && rc.readSharedArray(30) == 0) {
            exploreDest = new MapLocation(rng.nextInt(rc.getMapWidth()), rng.nextInt(rc.getMapHeight()));
            targetLocation = exploreDest;
        }
        System.out.println(targetLocation.toString());
        RobotPlayer.pathfinder(targetLocation, rc);
    }
}
