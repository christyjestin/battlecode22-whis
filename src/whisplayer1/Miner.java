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
    static Team opponent = null;
    static boolean reportedDeath = false;
    static Direction[] lastThreeMoves = { null, null, null };
    static Direction nextMove = null;
    static Boolean exploreMode = null;

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
        Deposit.addDeposits(rc, visionRadiusSquared, mapHeight, mapWidth);

        // return immediately if you can't move
        if (!rc.isMovementReady()) return;

        MapLocation deposit = Deposit.nearestDeposit(rc, rcLocation);
        // if you haven't been assigned, and you know about a deposit, then become an explorer w/ 50/50 chance
        if (deposit != null && exploreMode == null) {
            exploreMode = rng.nextBoolean();
            if (!exploreMode) destination = deposit;
        }
        // if you're close to your target, get a new target based on your explore mode
        int distance = rcLocation.distanceSquaredTo(destination);
        if ((exploreMode == null || exploreMode) && distance < visionRadiusSquared / 2) {
            if (deposit != null) exploreMode = rng.nextBoolean();
            destination = (exploreMode == null || exploreMode) ? randomLocation(rc) : deposit;
        } else if (exploreMode != null && !exploreMode) {
            destination = deposit;
        }
        rc.setIndicatorString(destination.toString());

        // move using pathfinder algorithm
        if (nextMove == null) {
            Direction[] pathfinderReturn = RobotPlayer.pathfinder(destination, rc, lastThreeMoves);
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
