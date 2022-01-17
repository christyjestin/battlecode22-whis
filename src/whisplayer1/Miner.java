package whisplayer1;

import battlecode.common.*;
import java.util.Random;

public strictfp class Miner {

    static final Random rng = new Random();

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

    static MapLocation destination = null;
    static final int visionRadiusSquared = RobotType.MINER.visionRadiusSquared;
    static final int actionRadiusSquared = RobotType.MINER.actionRadiusSquared;
    static int mapHeight = -1;
    static int mapWidth = -1;
    static Team opponent = null;
    static boolean reportedDeath = false;
    static Direction[] lastThreeMoves = { null, null, null };
    static Direction nextMove = null;

    // bool indicating whether the miner is assigned to watch lead deposits near archons
    static Boolean archonDeposit = null;

    static double halfGridLength = -1;

    // algorithm to randomly generate a location on grid based on miner's vision radius
    static MapLocation generateLocation(RobotController rc) throws GameActionException {
        // one square on our grid should be the same size as the inscribed square in the vision radius circle
        if (halfGridLength == -1) halfGridLength = Math.sqrt(visionRadiusSquared / 2.0);

        double fullGridLength = 2.0 * halfGridLength;
        // number of squares to fill the map along each dimension
        int gridHeight = (int) (Math.ceil(mapHeight / fullGridLength));
        int gridWidth = (int) (Math.ceil(mapWidth / fullGridLength));
        int x = (int) (rng.nextInt(gridWidth) * fullGridLength + halfGridLength);
        int y = (int) (rng.nextInt(gridHeight) * fullGridLength + halfGridLength);

        // x, y must be less than mapWidth and mapHeight in order to be a valid location
        if (x > mapWidth - 1) x = mapWidth - 1;
        if (y > mapHeight - 1) y = mapHeight - 1;
        return new MapLocation(x, y);
    }

    static MapLocation randomLocation(RobotController rc) throws GameActionException {
        if (mapHeight == -1) mapHeight = rc.getMapHeight();
        if (mapWidth == -1) mapWidth = rc.getMapWidth();
        return new MapLocation(rng.nextInt(mapWidth), rng.nextInt(mapHeight));
    }

    static void runMiner(RobotController rc) throws GameActionException {
        // report likely miner death
        if (rc.getHealth() < 6 && !reportedDeath) {
            RobotPlayer.decrementArray(rc, RobotPlayer.minerCountIndex);
            reportedDeath = true;
        }

        // init code
        if (mapHeight == -1) mapHeight = rc.getMapHeight();
        if (mapWidth == -1) mapWidth = rc.getMapWidth();
        if (opponent == null) opponent = rc.getTeam().opponent();
        if (destination == null) destination = randomLocation(rc);

        // Try to mine on squares around us.
        MapLocation me = rc.getLocation();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
                while (rc.canMineGold(mineLocation)) {
                    rc.mineGold(mineLocation);
                }
                while (rc.canMineLead(mineLocation) && rc.senseLead(mineLocation) > 1) {
                    rc.mineLead(mineLocation);
                }
            }
        }

        RobotInfo[] enemies = rc.senseNearbyRobots(visionRadiusSquared, opponent);
        for (RobotInfo enemy : enemies) {
            if (enemy.getType().equals(RobotType.ARCHON)) RobotPlayer.addEnemyArchon(rc, enemy.getLocation());
        }

        // return immediately if you can't move
        if (!rc.isMovementReady()) return;

        MapLocation[] nearbyLocations = rc.getAllLocationsWithinRadiusSquared(me, visionRadiusSquared);

        MapLocation targetLocation = null;
        int targetScore = -3600;

        for (MapLocation tryLocation : nearbyLocations) {
            // ignore the location if another robot is already there
            if (rc.senseRobotAtLocation(tryLocation) != null) continue;

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

        // move using pathfinder algorithm
        if (targetLocation == null) {
            // randomly generate a new destination if you're already there
            if (rc.getLocation().distanceSquaredTo(destination) < visionRadiusSquared / 2) {
                destination = randomLocation(rc);
            }
            targetLocation = destination;
        }

        if (nextMove == null) {
            Direction[] pathfinderReturn = RobotPlayer.pathfinder(targetLocation, rc, lastThreeMoves);
            lastThreeMoves = new Direction[] { pathfinderReturn[0], pathfinderReturn[1], pathfinderReturn[2] };
            nextMove = pathfinderReturn[3];
        } else {
            if (rc.canMove(nextMove)) {
                rc.move(nextMove);
                lastThreeMoves = new Direction[] { lastThreeMoves[1], lastThreeMoves[2], nextMove };
            }
            nextMove = null;
        }
    }
}
