package whisplayer1;

import battlecode.common.*;
import java.util.Random;

public strictfp class Miner {

    static final Random rng = new Random();

    static MapLocation destination = null;
    static final int visionRadiusSquared = RobotType.MINER.visionRadiusSquared;
    static final int actionRadiusSquared = RobotType.MINER.actionRadiusSquared;
    static int mapHeight = -1;
    static int mapWidth = -1;
    static int id = -1;
    static Team ownTeam = null;
    static Team opponent = null;
    static boolean reportedDeath = false;
    static Direction[] lastThreeMoves = { null, null, null };
    static Direction nextMove = null;
    static NoLead noLead = null;

    static boolean hasMiner(RobotController rc, MapLocation location) throws GameActionException {
        if (!rc.canSenseRobotAtLocation(location)) return false;
        RobotInfo robot = rc.senseRobotAtLocation(location);
        return robot.getType().equals(RobotType.MINER) && robot.getTeam().equals(ownTeam) && robot.getID() != id;
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
        if (id == -1) id = rc.getID();
        if (ownTeam == null) ownTeam = rc.getTeam();
        if (opponent == null) opponent = ownTeam.opponent();
        if (noLead == null) noLead = new NoLead(rc, visionRadiusSquared, mapHeight, mapWidth);
        if (destination == null) destination = noLead.minerRandomLocation();

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

        RobotPlayer.updateEnemyArchons(rc, visionRadiusSquared, opponent);
        noLead.updateGrid(rcLocation);

        // return immediately if you can't move
        if (!rc.isMovementReady()) return;

        MapLocation[] nearbyLocations = rc.getAllLocationsWithinRadiusSquared(rcLocation, visionRadiusSquared);

        MapLocation targetLocation = null;
        int targetScore = -7200;

        for (MapLocation location : nearbyLocations) {
            // ignore the location if another robot is already there
            if (hasMiner(rc, location)) continue;

            int gold = rc.senseGold(location);
            int lead = rc.senseLead(location);
            if (gold > 0 || lead > 1) {
                int score = 100 * gold + 10 * lead - rcLocation.distanceSquaredTo(location);
                if (score > targetScore) {
                    targetLocation = location;
                    targetScore = score;
                }
            }
        }

        // move using pathfinder algorithm
        if (targetLocation == null) {
            // randomly generate a new destination if you're already there
            if (rcLocation.distanceSquaredTo(destination) < visionRadiusSquared / 2) {
                destination = noLead.minerRandomLocation();
            }
            targetLocation = destination;
        }
        rc.setIndicatorString(targetLocation.toString());

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
