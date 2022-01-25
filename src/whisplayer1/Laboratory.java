package whisplayer1;

import battlecode.common.*;

strictfp class Laboratory {

    static final int visionRadiusSquared = RobotType.LABORATORY.visionRadiusSquared;
    static final int actionRadiusSquared = RobotType.LABORATORY.actionRadiusSquared;
    static Team ownTeam = null;
    static Team opponent = null;

    int nearbyArchonLevel(RobotController rc) throws GameActionException {
        RobotInfo[] nearbyBots = rc.senseNearbyRobots(visionRadiusSquared, ownTeam);
        for (RobotInfo bot : nearbyBots) {
            if (bot.getType().equals(RobotType.ARCHON)) return bot.getLevel();
        }
        return -1;
    }

    public void runLaboratory(RobotController rc) throws GameActionException {
        // init code
        if (ownTeam == null) ownTeam = rc.getTeam();
        if (opponent == null) opponent = ownTeam.opponent();

        int goldGap = rc.getTeamGoldAmount(ownTeam) - rc.getTeamGoldAmount(opponent);
        int archonLevel = nearbyArchonLevel(rc);
        // die when the archon dies
        if (archonLevel == -1) rc.disintegrate();
        // only expend lead if we're not being attacked or if we're in late game
        // also only try transmuting if we can gain an advantage in gold or we can use it to transmute an archon
        if ((!RobotPlayer.anyArchonHealthDrops(rc) || rc.getRoundNum() > 1900) && (archonLevel != 3 || goldGap < 20)) {
            if (rc.canTransmute()) rc.transmute();
        }
    }
}
