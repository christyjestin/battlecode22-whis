package whisplayer1;

import battlecode.common.*;
import java.util.Random;

public strictfp class Miner {
    static int randomCounter = 0;

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
     * Run a single turn for a Miner.
     * This code is wrapped inside the infinite loop in run(), so it is called once
     * per turn.
     */
    static void runMiner(RobotController rc, boolean a) throws GameActionException {
        Direction defaultDir = directions[RobotPlayer.rng.nextInt(directions.length)];
        Team opponent = rc.getTeam().opponent();
        int visionRadius = (int) (Math.sqrt(RobotType.MINER.visionRadiusSquared));
        while (true) {
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

            MapLocation[] nearbyLocations = rc.getAllLocationsWithinRadiusSquared(me, visionRadius);

            MapLocation targetLocation = null;
            int targetScore = -3600;

            for (MapLocation tryLocation : nearbyLocations) {
                // ignore the location if another robot is already there
                RobotInfo robot = rc.senseRobotAtLocation(tryLocation);
                if (robot != null && robot.team != opponent)
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
            } // a ? b : c ->  if a then b else c
            Direction toMove = targetLocation != null ? me.directionTo(targetLocation) : defaultDir;
            int moveCounter = 0;
            while (moveCounter < visionRadius) {
                if (rc.canMove(toMove)) {
                    rc.move(toMove);
                } else {
                    toMove.rotateRight();
                    if (rc.canMove(toMove)) {
                        rc.move(toMove);
                    }
                    toMove.rotateLeft();
                }
                moveCounter++;
                while(!rc.isMovementReady()) {}
            }
        }
    }
}