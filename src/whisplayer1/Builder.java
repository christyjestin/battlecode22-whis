package whisplayer1;

import battlecode.common.*;

public strictfp class Builder {

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

    static final int visionRadiusSquared = RobotType.LABORATORY.visionRadiusSquared;
    static final int actionRadiusSquared = RobotType.LABORATORY.actionRadiusSquared;
    static Team ownTeam = null;
    static Team opponent = null;
    static MapLocation archonLocation = null;
    static int archonId = -1;
    static MapLocation laboratoryLocation = null;
    static int laboratoryId = -1;

    static MapLocation findArchonLocation(RobotController rc) throws GameActionException {
        for (Direction direction : directions) {
            MapLocation adjLoc = rc.adjacentLocation(direction);
            if (rc.canSenseRobotAtLocation(adjLoc) && rc.senseRobotAtLocation(adjLoc).type.equals(RobotType.ARCHON)) {
                return adjLoc;
            }
        }
        return null;
    }

    static void runBuilder(RobotController rc) throws GameActionException {
        if (ownTeam == null) ownTeam = rc.getTeam();
        if (opponent == null) opponent = ownTeam.opponent();
        RobotPlayer.updateEnemyArchons(rc, visionRadiusSquared, opponent);

        if (!rc.isActionReady()) return;

        if (archonLocation == null) {
            archonLocation = findArchonLocation(rc);
            if (archonLocation == null) rc.disintegrate();
        }

        // if the archon's dead, you die as well
        if (!rc.canSenseRobotAtLocation(archonLocation)) rc.disintegrate();

        // prioritize healing archons
        RobotInfo archonInfo = rc.senseRobotAtLocation(archonLocation);
        if (archonInfo.getHealth() < RobotType.ARCHON.getMaxHealth(archonInfo.getLevel())) {
            if (rc.canRepair(archonLocation)) rc.repair(archonLocation);
        } else {
            RobotInfo[] nearbyBots = rc.senseNearbyRobots(actionRadiusSquared, ownTeam);
            for (RobotInfo bot : nearbyBots) {
                if (bot.getHealth() < bot.getType().getMaxHealth(bot.getLevel())) {
                    MapLocation botLocation = bot.getLocation();
                    if (rc.canRepair(botLocation)) rc.repair(botLocation);
                    if (!rc.isActionReady()) break;
                }
            }
        }
        // if (laboratoryId == -1 || !rc.canSenseRobot(laboratoryId)) {
        //     Direction bestDirection = RobotPlayer.findBestSpawnDirection(rc);
        //     if (rc.canBuildRobot(RobotType.LABORATORY, bestDirection)) {
        //         rc.buildRobot(RobotType.LABORATORY, bestDirection);
        //         laboratoryLocation = rc.adjacentLocation(bestDirection);
        //         laboratoryId = rc.senseRobotAtLocation(laboratoryLocation).getID();
        //     }
        // }
    }
}
