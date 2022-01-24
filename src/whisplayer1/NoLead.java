package whisplayer1;

import battlecode.common.*;
import java.util.Random;

public strictfp class NoLead {

    RobotController rc;
    final Random rng = new Random();
    final int gridLength = 12;
    boolean[][] grid = new boolean[gridLength][gridLength];
    final int arrayLength = 9;
    int[] array = new int[arrayLength]; // internal copy of shared array
    int mapHeight;
    int mapWidth;
    int visionRadiusSquared; // visionRadiusSquared of the rc using this NoLead grid
    float visionRadius;
    final int sensingRadiusSquared = 8;
    final float sensingRadius = (float) Math.sqrt(sensingRadiusSquared);
    float differenceRadius;
    int differenceRadiusSquared;

    public NoLead(RobotController rc, int visionRadiusSquared, int mapHeight, int mapWidth) {
        this.rc = rc;
        this.visionRadiusSquared = visionRadiusSquared;
        this.visionRadius = (float) Math.sqrt(visionRadiusSquared);
        this.differenceRadius = visionRadius - sensingRadius;
        this.differenceRadiusSquared = (int) (Math.pow(differenceRadius, 2.0));
        this.mapHeight = mapHeight;
        this.mapWidth = mapWidth;
    }

    // update internal array using shared array
    public void updatefromSharedArray() throws GameActionException {
        for (int i = 0; i < arrayLength; i++) {
            array[i] = rc.readSharedArray(RobotPlayer.noLeadGridStartIndex + i);
        }
    }

    // write the encodings from internal array to shared array
    public void writeToSharedArray() throws GameActionException {
        for (int i = 0; i < arrayLength; i++) {
            if (rc.readSharedArray(RobotPlayer.noLeadGridStartIndex + i) != array[i]) {
                rc.writeSharedArray(RobotPlayer.noLeadGridStartIndex + i, array[i]);
            }
        }
    }

    // encode and write grid to internal array
    public void writeGridToArray() {
        int arrayIndex = 0;
        int encoding = 0;
        for (int i = 0; i < gridLength * gridLength; i++) {
            // times 2, plus next value (but written w/ bitwise operators)
            encoding = (encoding << 1) | (grid[i / gridLength][i % gridLength] ? 1 : 0);
            // 16 grid positions per array index
            if (i % 16 == 15) {
                array[arrayIndex] = encoding;
                encoding = 0;
                arrayIndex++;
            }
        }
    }

    // update grid by decoding internal array
    public void updateGridfromArray() {
        for (int i = 0; i < arrayLength; i++) {
            int encoding = array[i];
            // go in reverse order while decoding
            for (int j = 16 * i + 15; j >= 16 * i; j--) {
                // equivalent to encoding % 2 == 1 in bitwise operators
                grid[j / gridLength][j % gridLength] = (encoding & 1) == 1;
                // divide by 2
                encoding = encoding >> 1;
            }
        }
    }

    public void updateGrid(MapLocation rcLocation) throws GameActionException {
        // refresh both internal array and grid
        // without this step, the function would write outdated data back to the shared array
        updatefromSharedArray();
        updateGridfromArray();

        int i = rcLocation.x / 5;
        int j = rcLocation.y / 5;
        MapLocation sensingLocation = new MapLocation(5 * i + 2, 5 * j + 2);
        if (rcLocation.distanceSquaredTo(sensingLocation) <= differenceRadiusSquared) {
            grid[i][j] = rc.senseNearbyLocationsWithLead(sensingLocation, sensingRadiusSquared).length == 0;
        }

        // update both internal and shared arrays
        writeGridToArray();
        writeToSharedArray();
    }

    public MapLocation minerRandomLocation() throws GameActionException {
        updatefromSharedArray();
        updateGridfromArray();
        MapLocation location = new MapLocation(rng.nextInt(mapWidth), rng.nextInt(mapHeight));
        int i = location.x / 5;
        int j = location.y / 5;
        while (grid[i][j]) {
            location = new MapLocation(rng.nextInt(mapWidth), rng.nextInt(mapHeight));
            i = location.x / 5;
            j = location.y / 5;
        }
        return location;
    }
}
