package whisplayer1;

import battlecode.common.*;

strictfp class Builder {

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

    static final int visionRadiusSquared = RobotType.BUILDER.visionRadiusSquared;
    static final int actionRadiusSquared = RobotType.BUILDER.actionRadiusSquared;
    static int mapHeight = -1;
    static int mapWidth = -1;
    static Team ownTeam = null;
    static Team opponent = null;

    MapLocation archonLocation = null;
    int archonId = -1;
    MapLocation laboratoryLocation = null;
    int laboratoryId = -1;
    MapLocation rcLocation = null;
    NoLead noLead = null;

    MapLocation findArchonLocation(RobotController rc) throws GameActionException {
        for (Direction direction : directions) {
            MapLocation adjacentLocation = rc.adjacentLocation(direction);
            if (RobotPlayer.isArchonAtLocation(rc, adjacentLocation)) return adjacentLocation;
        }
        return null;
    }

    public void runBuilder(RobotController rc) throws GameActionException {
        // init code
        if (ownTeam == null) ownTeam = rc.getTeam();
        if (opponent == null) opponent = ownTeam.opponent();
        if (mapHeight == -1) mapHeight = rc.getMapHeight();
        if (mapWidth == -1) mapWidth = rc.getMapWidth();
        if (rcLocation == null) rcLocation = rc.getLocation();
        if (noLead == null) noLead = new NoLead(rc, visionRadiusSquared, mapHeight, mapWidth);

        if (archonLocation == null) {
            archonLocation = findArchonLocation(rc);
            if (archonLocation == null) rc.disintegrate();
        }

        RobotPlayer.updateGoldDeposits(rc, visionRadiusSquared);
        noLead.updateGrid(rcLocation);

        // if the archon's dead, you die as well
        if (!rc.canSenseRobotAtLocation(archonLocation)) rc.disintegrate();

        if (!rc.isActionReady()) return;

        // mutate whenever we can
        if (rc.canMutate(archonLocation)) rc.mutate(archonLocation);

        // prioritize healing archons and then laboratories
        RobotInfo archonInfo = rc.senseRobotAtLocation(archonLocation);
        if (archonInfo.getHealth() < RobotType.ARCHON.getMaxHealth(archonInfo.getLevel())) {
            if (rc.canRepair(archonLocation)) rc.repair(archonLocation);
        } else if (laboratoryLocation != null && rc.canRepair(laboratoryLocation)) {
            if (rc.canRepair(laboratoryLocation)) rc.repair(laboratoryLocation);
        } else {
            RobotInfo[] nearbyBots = rc.senseNearbyRobots(actionRadiusSquared, ownTeam);
            for (RobotInfo bot : nearbyBots) {
                if (bot.getHealth() < bot.getType().getMaxHealth(bot.getLevel())) {
                    MapLocation botLocation = bot.getLocation();
                    if (rc.canRepair(botLocation)) rc.repair(botLocation);
                }
            }
        }

        // spawn a laboratory if you've already mutated the archon to level 2 and there is no laboratory
        if (archonInfo.getLevel() == 2 && (laboratoryId == -1 || !rc.canSenseRobot(laboratoryId))) {
            Direction bestDirection = RobotPlayer.findBestSpawnDirection(rc);
            if (rc.canBuildRobot(RobotType.LABORATORY, bestDirection)) {
                rc.buildRobot(RobotType.LABORATORY, bestDirection);
                laboratoryLocation = rc.adjacentLocation(bestDirection);
                laboratoryId = rc.senseRobotAtLocation(laboratoryLocation).getID();
            }
        }
    }
}
