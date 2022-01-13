package whisplayer1;

import battlecode.common.*;
import java.lang.Math;
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
  static int mapHeight = 0;
  static int mapWidth = 0;

  static double halfGridLength = -1;

  static MapLocation generateLocation(RobotController rc)
    throws GameActionException {
    // one square on our grid should be the same size as the inscribed square in the vision radius circle
    if (halfGridLength == -1) {
      halfGridLength = Math.sqrt(visionRadiusSquared / 2.0);
    }
    if (mapHeight == 0) mapHeight = rc.getMapHeight();
    if (mapWidth == 0) mapWidth = rc.getMapWidth();

    double fullGridLength = 2.0 * halfGridLength;
    // number of squares to fill the map along each dimension
    int gridHeight = (int) (Math.ceil(mapHeight / fullGridLength));
    int gridWidth = (int) (Math.ceil(mapWidth / fullGridLength));
    int x = (int) (rng.nextInt(gridWidth) * fullGridLength + halfGridLength);
    int y = (int) (rng.nextInt(gridHeight) * fullGridLength + halfGridLength);

    // x, y must be less than mapWidth and mapHeight in order to be a valid location
    if (x > mapWidth - 1) x = mapWidth - 1;
    if (y > mapHeight - 1) y = mapHeight - 1;
    return new MapLocation(x, y);
  }

  /**
   * Run a single turn for a Miner.
   * This code is wrapped inside the infinite loop in run(), so it is called once
   * per turn.
   */
  static void runMiner(RobotController rc) throws GameActionException {
    if (destination == null) destination = generateLocation(rc);

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
    if (!rc.isMovementReady()) return;

    MapLocation[] nearbyLocations = rc.getAllLocationsWithinRadiusSquared(
      me,
      visionRadiusSquared
    );

    MapLocation targetLocation = null;
    int targetScore = -3600;

    for (MapLocation tryLocation : nearbyLocations) {
      // ignore the location if another robot is already there
      if (rc.senseRobotAtLocation(tryLocation) != null) continue;

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
        RobotPlayer.addEnemyArchon(loc, rc);
      }
    }

    // move using pathfinder algorithm
    if (targetLocation == null) {
      // randomly generate a new destination if you're already there
      if (destination.equals(rc.getLocation())) {
        destination = generateLocation(rc);
      }
      targetLocation = destination;
    }

    RobotPlayer.pathfinder(targetLocation, rc);
  }
}
