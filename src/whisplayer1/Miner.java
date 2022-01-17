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

    static final int visionRadiusSquared = RobotType.MINER.visionRadiusSquared;
    static final int actionRadiusSquared = RobotType.MINER.actionRadiusSquared;
    static int mapHeight = -1;
    static int mapWidth = -1;
    static Team opponent = null;
    static boolean reportedDeath = false;
    static Direction[] lastThreeMoves = { null, null, null };
    static Direction nextMove = null;
    static LeadGrid leadGrid = null;
    static boolean exploreMode = true;
    static MapLocation exploreDest = null;

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

    static MapLocation closestLeadLocation(RobotController rc, MapLocation rcLocation) throws GameActionException {
        MapLocation[] leadLocations = rc.senseNearbyLocationsWithLead(visionRadiusSquared);
        MapLocation closestLocation = null;
        int leastDistance = 7200;
        for (MapLocation location : leadLocations) {
            // ignore the location if another robot is already there
            if (rc.canSenseRobotAtLocation(location)) continue;
            int lead = rc.senseLead(location);
            if (lead > 1) {
                int distance = rcLocation.distanceSquaredTo(location);
                if (distance < leastDistance) {
                    closestLocation = location;
                    leastDistance = distance;
                }
            }
        }
        return closestLocation;
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
        if (exploreDest == null) exploreDest = randomLocation(rc);
        if (leadGrid == null) leadGrid = new LeadGrid(rc, visionRadiusSquared, mapHeight, mapWidth);

        // Try to mine on squares around us.
        MapLocation rcLocation = rc.getLocation();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(rcLocation.x + dx, rcLocation.y + dy);
                while (rc.canMineGold(mineLocation)) {
                    rc.mineGold(mineLocation);
                }
                while (rc.canMineLead(mineLocation) && rc.senseLead(mineLocation) > 1) {
                    rc.mineLead(mineLocation);
                }
            }
        }

        MapLocation[] nearbyLocations = rc.getAllLocationsWithinRadiusSquared(rcLocation, visionRadiusSquared);
        leadGrid.updateGridFromNearbyLocations(rcLocation, nearbyLocations);

        MapLocation targetLocation = null;
        int closestGold = 7200;
        MapLocation[] goldLocations = rc.senseNearbyLocationsWithGold(visionRadiusSquared);
        for (MapLocation goldLocation : goldLocations) {
            // ignore the location if another robot is already there
            if (rc.canSenseRobotAtLocation(goldLocation)) continue;
            if (rcLocation.distanceSquaredTo(goldLocation) < closestGold) {
                closestGold = rcLocation.distanceSquaredTo(goldLocation);
                targetLocation = goldLocation;
            }
        }

        RobotInfo[] enemies = rc.senseNearbyRobots(visionRadiusSquared, opponent);
        for (RobotInfo enemy : enemies) {
            if (enemy.getType().equals(RobotType.ARCHON)) RobotPlayer.addEnemyArchon(rc, enemy.getLocation());
        }

        // return immediately if you can't move
        if (!rc.isMovementReady()) return;

        // move using pathfinder algorithm
        if (targetLocation == null) {
            // randomly generate a new destination to explore if you're already close
            if (rcLocation.distanceSquaredTo(exploreDest) < visionRadiusSquared / 2) {
                exploreDest = randomLocation(rc);
            }
            MapLocation nearestLeadDeposit = leadGrid.nearestLeadDeposit(rcLocation);
            exploreMode = nearestLeadDeposit == null;
            targetLocation = exploreMode ? exploreDest : nearestLeadDeposit;
            // if you're going towards a lead deposit, and you're already in the right grid square, then go towards the nearest lead
            if (!exploreMode && leadGrid.sameGridSquare(rcLocation, targetLocation)) {
                targetLocation = closestLeadLocation(rc, rcLocation);
                // if all of the lead nearby is already being mined, keep exploring
                if (targetLocation == null) targetLocation = exploreDest;
            }
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
