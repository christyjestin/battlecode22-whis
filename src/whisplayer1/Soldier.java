package whisplayer1;

import battlecode.common.*;
import java.util.Random;

public class Soldier {

  static final Random rng = new Random();

  /** Array containing all the possible movement directions. */
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

  static MapLocation exploreDest = null;
  static MapLocation targetLocation = null;
  static int visionRadiusSquared = RobotType.SOLDIER.visionRadiusSquared;
  static int actionRadiusSquared = RobotType.SOLDIER.actionRadiusSquared;

  /**
   * Run a single turn for a Soldier.
   * This code is wrapped inside the infinite loop in run(), so it is called once
   * per turn.
   */
  public static void runSoldier(RobotController rc) throws GameActionException {
    if (!rc.isActionReady() && !rc.isMovementReady()) return;

    if (exploreDest == null) exploreDest =
      new MapLocation(
        rng.nextInt(rc.getMapWidth()),
        rng.nextInt(rc.getMapHeight())
      );

    // Try to attack someone
    Team opponent = rc.getTeam().opponent();
    RobotInfo[] enemies = rc.senseNearbyRobots(actionRadiusSquared, opponent);
    MapLocation target = null;
    for (RobotInfo enemy : enemies) {
      MapLocation loc = enemy.getLocation();
      if (rc.canAttack(loc)) {
        if (enemy.getType().equals(RobotType.ARCHON)) {
          RobotPlayer.addEnemyArchon(loc, rc);
          rc.attack(loc);
          target = null;
          break;
        } else {
          target = loc;
        }
      }
    }

    RobotPlayer.checkEnemyArchons(rc);

    if (target != null) {
      rc.attack(target);
    }

    if (!rc.isMovementReady()) return;

    // move using pathfinder algorithm
    targetLocation = exploreDest;
    if (targetLocation.equals(rc.getLocation())) {
      exploreDest =
        new MapLocation(
          rng.nextInt(rc.getMapWidth()),
          rng.nextInt(rc.getMapHeight())
        );
      targetLocation = exploreDest;
    }

    for (int i = 60; i < 64; i++) {
      if (rc.readSharedArray(i) != 0) {
        targetLocation =
          new MapLocation(
            rc.readSharedArray(i) / 100,
            rc.readSharedArray(i) % 100
          );
      }
    }

    System.out.println(targetLocation.toString());
    RobotPlayer.pathfinder(targetLocation, rc);
  }
}
