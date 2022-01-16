package whisplayer1;
import battlecode.common.*;
import java.lang.Math;
import java.util.ArrayList;
import java.util.Random;

public class Builder {

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

  static boolean autoPilot = false;
  static boolean buildReady = false;

  public static void runBuilder(RobotController rc) throws GameActionException {
    rc.setIndicatorString(buildReady + " " + rc.getLocation().distanceSquaredTo(returnClosestArchon(rc)) + " " + " " +
                    returnClosestArchon(rc) +
            rc.canBuildRobot(RobotType.LABORATORY,rc.getLocation().directionTo(returnClosestArchon(rc))) + " " +
            returnClosestArchon(rc).toString());
    buildLabLocation(rc);
  }

  public static MapLocation buildLabLocation(RobotController rc) throws GameActionException {
    MapLocation corner = findCorner(rc);

    if(buildReady && rc.canBuildRobot(RobotType.LABORATORY,rc.getLocation().directionTo(returnClosestArchon(rc)))){
      rc.buildRobot(RobotType.LABORATORY,rc.getLocation().directionTo(returnClosestArchon(rc)));
    }

      if(rc.getLocation().equals(corner)){
      buildReady = true;
    } else if(rc.canMove(rc.getLocation().directionTo(corner))
            && rc.getLocation().distanceSquaredTo(returnClosestArchon(rc)) < 25){
      RobotPlayer.pathfinder(corner,rc);
    } else if(rc.getLocation().distanceSquaredTo(returnClosestArchon(rc)) >= 25){
      buildReady = true;
    }
      return corner;
  }

  public static MapLocation findCorner(RobotController rc) throws GameActionException {
    MapLocation a = returnClosestArchon(rc);
    int x,y;

    if(rc.getMapWidth() - a.x> a.x){
      x = 0;
    } else{
      x = rc.getMapWidth();
    }

    if(rc.getMapHeight() - a.y> a.y){
      y = 0;
    } else{
      y= rc.getMapHeight();
    }

    return new MapLocation(x,y);
  }



  public static void headTowardLead(RobotController rc ) throws GameActionException {
    if(autoPilot){
      if(rc.senseLead(rc.getLocation()) == 0){
        rc.disintegrate();
      } else {
        MapLocation destination = awayFromLead(rc);
        if(destination == null){
          int turncount = 0;
          while(true) {
            rc.move(directions[rng.nextInt(directions.length)]);
            turncount ++;
          }
        }
        RobotPlayer.pathfinder(destination, rc);
      }
    }


    MapLocation[] locations = new MapLocation[rc.getArchonCount()];
    for(int i = 0; i < rc.getArchonCount(); i ++){
      locations[i] = new MapLocation(rc.readSharedArray(i + RobotPlayer.archonLocationStartIndex)/100, rc.readSharedArray(i + RobotPlayer.archonLocationStartIndex)%100);
    }
    MapLocation closest = RobotPlayer.closestTo(locations, rc.getLocation());
    if(rc.canMove(rc.getLocation().directionTo(closest))){
      rc.move(rc.getLocation().directionTo(closest));
    }

    if(rc.getLocation().isWithinDistanceSquared(closest, 4)){
      if(rc.senseLead(rc.getLocation()) == 0){
        rc.disintegrate();
      }
      else {
        autoPilot = true;
      }
    }

  }

  public static MapLocation returnClosestArchon(RobotController rc) throws GameActionException {
    MapLocation[] locations = new MapLocation[rc.getArchonCount()];
    for(int i = 0; i < rc.getArchonCount(); i ++){
        locations[i] = new MapLocation(rc.readSharedArray(i + RobotPlayer.archonLocationStartIndex) / 100, rc.readSharedArray(i + RobotPlayer.archonLocationStartIndex) % 100);
    }
    return RobotPlayer.closestTo(locations, rc.getLocation());
  }

  public static MapLocation awayFromLead(RobotController rc) throws GameActionException {
    MapLocation me = rc.getLocation();
    int visionRadius = (int)Math.sqrt(RobotType.BUILDER.visionRadiusSquared/2.0);
    ArrayList<MapLocation> possibleSpots = new ArrayList<>();
    for (int dx = -visionRadius; dx <= visionRadius; dx++) {
      for (int dy = -visionRadius; dy <= visionRadius; dy++) {
        MapLocation location = new MapLocation(me.x + dx, me.y + dy);
        if(!rc.onTheMap(location)){
          continue;
        }
        if(rc.senseLead(location) == 0){
          possibleSpots.add(location);
        }
      }
    }
    return RobotPlayer.closestTo(possibleSpots.toArray(new MapLocation[0]), rc.getLocation());
  }
}
