package whisplayer1;

import battlecode.common.*;

public strictfp class Laboratory {

    static final int visionRadiusSquared = RobotType.LABORATORY.visionRadiusSquared;
    static final int actionRadiusSquared = RobotType.LABORATORY.actionRadiusSquared;
    static Team ownTeam = null;
    static Team opponent = null;

    static int nearbyArchonLevel(RobotController rc) throws GameActionException {
        RobotInfo[] nearbyBots = rc.senseNearbyRobots(visionRadiusSquared, ownTeam);
        for (RobotInfo bot : nearbyBots) {
            if (bot.getType().equals(RobotType.ARCHON)) return bot.getLevel();
        }
        return -1;
    }

    static void runLaboratory(RobotController rc) throws GameActionException {
        if (ownTeam == null) ownTeam = rc.getTeam();
        if (opponent == null) opponent = ownTeam.opponent();
        RobotPlayer.updateEnemyArchons(rc, visionRadiusSquared, opponent);

        int goldGap = rc.getTeamGoldAmount(ownTeam) - rc.getTeamGoldAmount(opponent);
        int archonLevel = nearbyArchonLevel(rc);
        // die when the archon dies
        if (archonLevel == -1) rc.disintegrate();
        // only expend lead if we're not being attacked or if we're in late game
        // also only try transmuting if we can gain an advantage in gold or we can use it to transmute an archon
        if ((!Soldier.anyArchonHealthDrops(rc) || rc.getRoundNum() > 1900) && (archonLevel != 3 || goldGap < 20)) {
            if (rc.canTransmute()) rc.transmute();
        }
    }
}
