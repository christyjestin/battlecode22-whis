package whisplayer1;

import battlecode.common.*;

import java.util.Random;
import java.util.HashMap;

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

  static HashMap<Integer, MapLocation> destinations = new HashMap<Integer, MapLocation>();

  /**
   * Run a single turn for a Miner.
   * This code is wrapped inside the infinite loop in run(), so it is called once
   * per turn.
   */
  static void runMiner(RobotController rc) throws GameActionException {
    final int id = rc.getID();
    if (!destinations.containsKey(id))
      destinations.put(id, new MapLocation(rng.nextInt(rc.getMapWidth()), rng.nextInt(rc.getMapHeight())));

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

    int visionRadius = rc.getType().visionRadiusSquared;
    MapLocation[] nearbyLocations = rc.getAllLocationsWithinRadiusSquared(me, visionRadius);

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

    int radiusSquared = rc.getType().visionRadiusSquared;
    Team opponent = rc.getTeam().opponent();
    RobotInfo[] enemies = rc.senseNearbyRobots(radiusSquared, opponent);
    for (RobotInfo enemy : enemies) {
      MapLocation loc = enemy.getLocation();
      if (enemy.getType().equals(RobotType.ARCHON)) {
        RobotPlayer.addEnemyArchon(loc, rc);
      }
    }

    // move using pathfinder algorithm
    if (targetLocation == null)
      targetLocation = destinations.get(id);
    RobotPlayer.pathfinder(targetLocation, rc);
  }
}
