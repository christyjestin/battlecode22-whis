package whisplayer1;

import battlecode.common.*;
import java.util.HashMap;

public strictfp class Deposit {

    static final int leadPerLevel = 16;

    static void addDeposits(RobotController rc, int visionRadiusSquared, int mapHeight, int mapWidth)
        throws GameActionException {
        removeDeposits(rc, rc.getLocation(), mapHeight, mapWidth);
        HashMap<Integer[], Integer[]> map = new HashMap<>();
        MapLocation[] leadLocations = rc.senseNearbyLocationsWithLead(visionRadiusSquared);
        MapLocation[] goldLocations = rc.senseNearbyLocationsWithGold(visionRadiusSquared);

        for (MapLocation location : leadLocations) {
            int lead = rc.senseLead(location);
            // skip if the robot can't sense the entire box or if there's too little lead
            if (lead <= 1 || !shouldWrite(rc, location, mapHeight, mapWidth)) continue;

            Integer[] key = { location.x / 2, location.y / 2 };
            Integer[] val = { (map.containsKey(key) ? map.get(key)[0] : 0) + lead, 0 };
            map.put(key, val);
        }
        for (MapLocation location : goldLocations) {
            Integer[] key = { location.x / 2, location.y / 2 };
            Integer[] val = { (map.containsKey(key) ? map.get(key)[0] : 0), 1 };
            map.put(key, val);
        }

        for (Integer[] key : map.keySet()) {
            Integer[] val = map.get(key);
            int x = key[0];
            int y = key[1];
            int lead = val[0];
            int gold = val[1];
            int encoding = encodeDeposit(x, y, lead, gold);
            int minLead = 30;
            int index = 0;
            for (int i = RobotPlayer.depositStartIndex; i < RobotPlayer.depositStopIndex; i++) {
                int arrayVal = rc.readSharedArray(i);
                int[] decoded = decodeDeposit(arrayVal);
                if (arrayVal == 0 || (decoded[0] == x && decoded[1] == y)) {
                    rc.writeSharedArray(i, encoding);
                    index = 0;
                    break;
                } else if (decoded[3] == 0 && decoded[2] < minLead) {
                    minLead = decoded[2];
                    index = i;
                }
            }
            if (index != 0 && (lead / leadPerLevel) >= minLead) rc.writeSharedArray(index, encoding);
        }
    }

    static void removeDeposits(RobotController rc, MapLocation rcLocation, int mapHeight, int mapWidth)
        throws GameActionException {
        for (int i = RobotPlayer.depositStartIndex; i < RobotPlayer.depositStopIndex; i++) {
            int arrayVal = rc.readSharedArray(i);
            if (arrayVal == 0) continue;
            int[] decoded = decodeDeposit(arrayVal);
            int x = 2 * decoded[0];
            int y = 2 * decoded[1];
            if (!shouldWrite(rc, new MapLocation(x, y), mapHeight, mapWidth)) continue;
            boolean noLead = true;
            // check the box; if none of the squares contain lead, remove the deposit from shared array
            for (int s = 0; s < 2; s++) {
                for (int t = 0; t < 2; t++) {
                    MapLocation corner = new MapLocation(x + s, y + t);
                    if (onTheMap(corner, mapHeight, mapWidth) && rc.senseLead(corner) > 1) noLead = false;
                }
            }
            if (noLead) rc.writeSharedArray(i, 0);
        }
    }

    static boolean onTheMap(MapLocation loc, int mapHeight, int mapWidth) {
        return loc.x >= 0 && loc.y >= 0 && loc.x < mapWidth && loc.y < mapHeight;
    }

    static boolean shouldWrite(RobotController rc, MapLocation location, int mapHeight, int mapWidth)
        throws GameActionException {
        // get x and y of the bottom left corner
        int x = location.x - (location.x % 2);
        int y = location.y - (location.y % 2);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                MapLocation corner = new MapLocation(x + i, y + j);
                // if the rc can't sense a location on the map within the 2x2 box, then it shouldn't write
                if (onTheMap(corner, mapHeight, mapWidth) && !rc.canSenseLocation(corner)) return false;
            }
        }
        return true;
    }

    static int encodeDeposit(int x, int y, int lead, int gold) {
        int leadLevel = Math.min(29, lead / leadPerLevel);
        return ((x * 30 + y) * 30 + leadLevel) * 2 + gold;
    }

    static int[] decodeDeposit(int encoding) {
        int gold = encoding % 2;
        encoding = encoding / 2;
        int leadLevel = encoding % 30;
        encoding = encoding / 30;
        int y = encoding % 30;
        encoding = encoding / 30;
        int x = encoding;
        return new int[] { x, y, leadLevel, gold };
    }

    static MapLocation nearestDeposit(RobotController rc, MapLocation rcLocation) throws GameActionException {
        int bestScore = -7200;
        MapLocation bestLocation = null;
        for (int i = RobotPlayer.depositStartIndex; i < RobotPlayer.depositStopIndex; i++) {
            int arrayVal = rc.readSharedArray(i);
            if (arrayVal == 0) continue;

            int[] decoded = decodeDeposit(arrayVal);
            MapLocation loc = new MapLocation(2 * decoded[0], 2 * decoded[1]);
            if (
                rc.canSenseLocation(loc) &&
                rc.canSenseRobotAtLocation(loc) &&
                rc.senseRobotAtLocation(loc).getType().equals(RobotType.MINER)
            ) continue;
            int lead = decoded[2];
            int gold = decoded[3];
            System.out.println(
                "round: " + rc.getRoundNum() + " location: " + loc.toString() + " lead: " + lead + " gold: " + gold
            );
            int score = 2000 * gold + 20 * lead - rcLocation.distanceSquaredTo(loc);
            if (score > bestScore) {
                bestScore = score;
                bestLocation = loc;
            }
        }
        return bestLocation;
    }
}
