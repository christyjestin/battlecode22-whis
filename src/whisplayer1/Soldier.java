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
    static MapLocation targetLocation = null;
    static int visionRadiusSquared = RobotType.SOLDIER.visionRadiusSquared;
    static int actionRadiusSquared = RobotType.SOLDIER.actionRadiusSquared;
    static int mapWidth = -1;
    static int mapHeight = -1;
    static Team opponent = null;
    static boolean reportedDeath = false;
    static Boolean defenseMode = null;
    static MapLocation defendingLocation = null;

    static boolean isArchonAtLocation(RobotController rc, MapLocation loc) throws GameActionException {
        return rc.canSenseRobotAtLocation(loc) && rc.senseRobotAtLocation(defendingLocation).type == RobotType.ARCHON;
    }

    static int weakestArchonHealth(RobotController rc) throws GameActionException {
        int minHealth = RobotType.ARCHON.getMaxHealth(3);
        for (int i = RobotPlayer.archonHealthStartIndex; i < RobotPlayer.archonHealthStopIndex; i++) {
            int val = rc.readSharedArray(i);
            if (val != 0 && val < minHealth) minHealth = val;
        }
        System.out.println("minHealth:" + minHealth);
        return minHealth;
    }

    static MapLocation weakestArchonLocation(RobotController rc) throws GameActionException {
        int minHealth = RobotType.ARCHON.getMaxHealth(3);
        int index = 0;
        for (int i = RobotPlayer.archonHealthStartIndex; i < RobotPlayer.archonHealthStopIndex; i++) {
            int val = rc.readSharedArray(i);
            if (val != 0 && val < minHealth) {
                minHealth = val;
                index = i - RobotPlayer.archonHealthStartIndex;
            }
        }
        // retrieve encoding of weakest archon's location from shared array
        return RobotPlayer.retrieveLocationfromArray(rc, RobotPlayer.archonLocationStartIndex + index);
    }

    /**
     * Run a single turn for a Soldier.
     * This code is wrapped inside the infinite loop in run(), so it is called once
     * per turn.
     */
    public static void runSoldier(RobotController rc) throws GameActionException {
        // report likely soldier death
        if (rc.getHealth() < 6 && !reportedDeath) {
            RobotPlayer.decrementArray(rc, RobotPlayer.soldierCountIndex);
            reportedDeath = true;
        }

        if (!rc.isActionReady() && !rc.isMovementReady()) return;

        // init code
        if (mapWidth == -1) mapWidth = rc.getMapWidth();
        if (mapHeight == -1) mapHeight = rc.getMapHeight();
        if (opponent == null) opponent = rc.getTeam().opponent();
        if (exploreDest == null) exploreDest = new MapLocation(rng.nextInt(mapWidth), rng.nextInt(mapHeight));

        // Try to attack someone
        RobotInfo[] enemies = rc.senseNearbyRobots(actionRadiusSquared, opponent);
        MapLocation target = null;
        for (RobotInfo enemy : enemies) {
            MapLocation loc = enemy.getLocation();
            if (rc.canAttack(loc)) {
                if (enemy.getType().equals(RobotType.ARCHON)) {
                    RobotPlayer.addEnemyArchon(rc, loc);
                    rc.attack(loc);
                    target = null;
                    break;
                } else {
                    target = loc;
                }
            }
        }

        RobotPlayer.checkEnemyArchons(rc);

        if (target != null) rc.attack(target);

        if (!rc.isMovementReady()) return;

        targetLocation = exploreDest;
        // randomly generate a new target location if you get close enough to it
        if (rc.getLocation().distanceSquaredTo(targetLocation) < actionRadiusSquared / 2) {
            exploreDest = new MapLocation(rng.nextInt(rc.getMapWidth()), rng.nextInt(rc.getMapHeight()));
            targetLocation = exploreDest;
        }

        // note that currently, once defenseMode is set to false, it won't ever change
        if (defenseMode != null && defenseMode) {
            // if the archon isn't there anymore, then stop defending
            if (rc.canSenseLocation(defendingLocation) && !isArchonAtLocation(rc, defendingLocation)) {
                defenseMode = null;
                // if there are too many robots nearby, have half of the robots stop defending
            } else if (rc.senseNearbyRobots(visionRadiusSquared, rc.getTeam()).length > actionRadiusSquared) {
                defenseMode = rng.nextBoolean();
            }
            // if this robot is still a defender, go towards the archon being attacked
            if (defenseMode != null && defenseMode) targetLocation = defendingLocation;
            // if an archon is being attacked, have half of the archons go defend it
        } else if (defenseMode == null && weakestArchonHealth(rc) < RobotType.ARCHON.getMaxHealth(1)) {
            defenseMode = rng.nextBoolean();
            if (defenseMode) {
                defendingLocation = weakestArchonLocation(rc);
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
        RobotPlayer.pathfinder(targetLocation, rc);
    }
}
