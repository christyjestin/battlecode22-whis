package whisplayer1;

import battlecode.common.*;

import java.util.Random;

public strictfp class Miner {
  /**
   * A random number generator.
   */
  static final Random rng = new Random();

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

    static MapLocation destination = null;

    static final int visionRadiusSquared = RobotType.MINER.visionRadiusSquared;
    static final int actionRadiusSquared = RobotType.MINER.actionRadiusSquared;

    /**
     * Run a single turn for a Miner.
     * This code is wrapped inside the infinite loop in run(), so it is called once
     * per turn.
     */
    static void runMiner(RobotController rc) throws GameActionException {
        if (destination == null)
            destination = new MapLocation(rng.nextInt(rc.getMapWidth()), rng.nextInt(rc.getMapHeight()));

    // Try to mine on squares around us.
    MapLocation me = rc.getLocation();
    for (int dx = -1; dx <= 1; dx++) {
      for (int dy = -1; dy <= 1; dy++) {
        MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
        // Notice that the Miner's action cooldown is very low.
        // You can mine multiple times per turn!
        while (rc.canMineGold(mineLocation)) {
          rc.mineGold(mineLocation);
        }
        while (rc.canMineLead(mineLocation) && rc.senseLead(mineLocation) > 1) {
          rc.mineLead(mineLocation);
        }
      }
    }

    // return immediately if you can't move
    if (!rc.isMovementReady())
      return;

    MapLocation[] nearbyLocations = rc.getAllLocationsWithinRadiusSquared(me, visionRadiusSquared);

    MapLocation targetLocation = null;
    int targetScore = -3600;

    for (MapLocation tryLocation : nearbyLocations) {
      // ignore the location if another robot is already there
      if (rc.senseRobotAtLocation(tryLocation) != null)
        continue;

            int gold = rc.senseGold(tryLocation);
            int lead = rc.senseLead(tryLocation);
            if (gold > 0 || lead > 1) {
                int score = 100 * gold + 10 * lead - me.distanceSquaredTo(tryLocation);
                if (score > targetScore) {
                    targetLocation = tryLocation;
                    targetScore = score;
                }
            }
        }

        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(visionRadiusSquared, opponent);
        for (RobotInfo enemy : enemies) {
            MapLocation loc = enemy.getLocation();
            if (enemy.getType().equals(RobotType.ARCHON)) {
                rc.writeSharedArray(30, loc.x);
                rc.writeSharedArray(31, loc.y);
                break;
            }
        }
      }
    }

    Team opponent = rc.getTeam().opponent();
    RobotInfo[] enemies = rc.senseNearbyRobots(visionRadiusSquared, opponent);
    for (RobotInfo enemy : enemies) {
      MapLocation loc = enemy.getLocation();
      if (enemy.getType().equals(RobotType.ARCHON)) {
        RobotPlayer.addEnemyArchon(loc, rc);
      }
        // move using pathfinder algorithm
        if (targetLocation == null) {
            // randomly generate a new destination if you're already there
            if (destination.equals(rc.getLocation()))
                destination = new MapLocation(rng.nextInt(rc.getMapWidth()), rng.nextInt(rc.getMapHeight()));
            targetLocation = destination;
        }

        RobotPlayer.pathfinder(targetLocation, rc);
  }
}
