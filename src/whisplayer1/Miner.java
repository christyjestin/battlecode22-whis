package whisplayer1;

import battlecode.common.*;
import java.util.Random;

public strictfp class Miner {
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

    /**
     * Run a single turn for a Miner.
     * This code is wrapped inside the infinite loop in run(), so it is called once
     * per turn.
     */
    static void runMiner(RobotController rc) throws GameActionException {
        if (center == null)
            center = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
        // some directions
        Direction towardsCenter = rc.getLocation().directionTo(center);
        Direction towardsRight = towardsCenter.rotateRight();
        Direction towardsLeft = towardsCenter.rotateLeft();

        // Try to mine on squares around us.
        MapLocation me = rc.getLocation();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
                // Notice that the Miner's action cooldown is very low.
                // You can mine multiple times per turn!
                while (rc.canMineGold(mineLocation)) {
                    rc.mineGold(mineLocation);
                }
                while (rc.canMineLead(mineLocation) && rc.senseLead(mineLocation) > 1) {
                    rc.mineLead(mineLocation);
                }
            }
        }

        int visionRadius = rc.getType().visionRadiusSquared;
        MapLocation[] nearbyLocations = rc.getAllLocationsWithinRadiusSquared(me, visionRadius);

        MapLocation targetLocation = null;
        int targetScore = -3600;

        for (MapLocation tryLocation : nearbyLocations) {
            // ignore the location if another robot is already there
            if (rc.senseRobotAtLocation(tryLocation) != null)
                continue;

            int gold = rc.senseGold(tryLocation);
            int lead = rc.senseLead(tryLocation);
            if (gold > 0 || lead > 1) {
                int score = 100 * gold + 10 * lead - me.distanceSquaredTo(tryLocation);
                if (score > targetScore) {
                    targetLocation = tryLocation;
                    targetScore = score;
                }
            }
        }

        if (targetLocation != null) {
            Direction toMove = me.directionTo(targetLocation);
            int tryMove = 0;
            while (tryMove < 10 && !rc.getLocation().equals(targetLocation)) {
                if (rc.canMove(toMove)) {
                    rc.move(toMove);
                }
                tryMove++;
            }
        } else {
            // Also try to move randomly (but towards center)
            int random = rng.nextInt(3);
            Direction dir = random == 0 ? towardsRight : (random == 1 ? towardsCenter : towardsLeft);
            int tryMove = 0;
            while (tryMove < 10) {
                if (rc.canMove(dir)) {
                    rc.move(dir);
                }
                tryMove++;
            }
        }
    }
}
