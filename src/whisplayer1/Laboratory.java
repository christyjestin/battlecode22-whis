package whisplayer1;

import battlecode.common.*;

public strictfp class Laboratory {

    static final int visionRadiusSquared = RobotType.LABORATORY.visionRadiusSquared;
    static final int actionRadiusSquared = RobotType.LABORATORY.actionRadiusSquared;
    static Team ownTeam = null;
    static Team opponent = null;

    static void runLaboratory(RobotController rc) throws GameActionException {
        if (ownTeam == null) ownTeam = rc.getTeam();
        if (opponent == null) opponent = ownTeam.opponent();
        RobotPlayer.updateEnemyArchons(rc, visionRadiusSquared, opponent);

        if (rc.getTeamGoldAmount(ownTeam) - rc.getTeamGoldAmount(opponent) < 20 && rc.canTransmute()) {
            rc.transmute();
        }
    }
}
