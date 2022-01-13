package whisplayer1;

import battlecode.common.*;
import java.util.Random;

public strictfp class Archon {

  /**
   * A random number generator.
   */
  static final Random rng = new Random(6147);

  /**
   * Array containing all the possible movement directions.
   */
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

  static MapLocation center = null;
  static boolean writtenToSharedArray = false;

  /**
   * Run a single turn for an Archon.
   * This code is wrapped inside the infinite loop in run(), so it is called once
   * per turn.
   */
  static void runArchon(RobotController rc) throws GameActionException {
    MapLocation[] leadDeposit = rc.senseNearbyLocationsWithLead();

    if (!writtenToSharedArray && leadDeposit.length > 0) {
      for (int i = 0; i < rc.getArchonCount(); i++) {
        if (rc.readSharedArray(i) == 0) {
          rc.writeSharedArray(i, leadDeposit[0].x * 100 + leadDeposit[0].y);
          writtenToSharedArray = true;
          return;
        }
      }
    }

    if (center == null) center =
      new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
    // some directions
    Direction towardsCenter = rc.getLocation().directionTo(center);
    Direction towardsRight = towardsCenter.rotateRight();
    Direction towardsLeft = towardsCenter.rotateLeft();
    Direction[] centerDirections = { towardsRight, towardsCenter, towardsLeft };
    Direction dir = centerDirections[rng.nextInt(centerDirections.length)];
    // stop early
    if (rc.getRobotCount() > (rc.getMapHeight() * rc.getMapWidth() / 3)) return;
    if (rng.nextBoolean()) {
      // spawn both miners and soldiers
      if (rc.canBuildRobot(RobotType.MINER, dir)) {
        rc.buildRobot(RobotType.MINER, dir);
      }
    } else {
      if (rc.canBuildRobot(RobotType.SOLDIER, dir)) {
        rc.buildRobot(RobotType.SOLDIER, dir);
      }
    }
  }
}
