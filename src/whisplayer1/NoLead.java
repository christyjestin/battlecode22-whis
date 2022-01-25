package whisplayer1;

import battlecode.common.*;
import java.util.Random;

strictfp class NoLead {

    static final int gridLength = 12;
    static final int arrayLength = 9;
    static final int sensingRadiusSquared = 8;
    static final float sensingRadius = (float) Math.sqrt(sensingRadiusSquared);
    static int mapHeight;
    static int mapWidth;

    RobotController rc;
    final Random rng = new Random();
    boolean[][] grid = new boolean[gridLength][gridLength];
    int[] array = new int[arrayLength]; // internal copy of shared array
    int visionRadiusSquared; // visionRadiusSquared of the rc using this NoLead grid
    float visionRadius;
    float differenceRadius; // vision radius - sensing radius
    int differenceRadiusSquared;

    public NoLead(RobotController rc, int visionRadiusSquared, int mapHeight, int mapWidth) {
        this.rc = rc;
        this.visionRadiusSquared = visionRadiusSquared;
        this.visionRadius = (float) Math.sqrt(visionRadiusSquared);
        this.differenceRadius = visionRadius - sensingRadius;
        this.differenceRadiusSquared = (int) (Math.pow(differenceRadius, 2.0));
        NoLead.mapHeight = mapHeight;
        NoLead.mapWidth = mapWidth;
    }

    // update internal array using shared array
    void updatefromSharedArray() throws GameActionException {
        for (int i = 0; i < arrayLength; i++) {
            int sharedArrayVal = rc.readSharedArray(RobotPlayer.noLeadGridStartIndex + i);
            if (array[i] != sharedArrayVal) {
                array[i] = sharedArrayVal;
                updateGridfromArray(i);
            }
        }
    }

    // write the encodings from internal array to shared array
    void writeToSharedArray() throws GameActionException {
        for (int i = 0; i < arrayLength; i++) {
            if (rc.readSharedArray(RobotPlayer.noLeadGridStartIndex + i) != array[i]) {
                rc.writeSharedArray(RobotPlayer.noLeadGridStartIndex + i, array[i]);
            }
        }
    }

    // encode and write grid to internal array for one index
    void writeGridToArray(int index) {
        int encoding = 0;
        for (int i = index * 16; i < (index + 1) * 16; i++) {
            // times 2, plus next value (but written w/ bitwise operators)
            encoding = (encoding << 1) | (grid[i / gridLength][i % gridLength] ? 1 : 0);
        }
        array[index] = encoding;
    }

    // update grid by decoding internal array
    void updateGridfromArray(int index) {
        int encoding = array[index];
        // go in reverse order while decoding
        for (int j = 16 * index + 15; j >= 16 * index; j--) {
            // equivalent to encoding % 2 == 1 in bitwise operators
            grid[j / gridLength][j % gridLength] = (encoding & 1) == 1;
            // divide by 2
            encoding = encoding >> 1;
        }
    }

    public void updateGrid(MapLocation rcLocation) throws GameActionException {
        // refresh both internal array and grid
        // without this step, the function would write outdated data back to the shared array
        updatefromSharedArray();

        int rcX = rcLocation.x;
        int rcY = rcLocation.y;
        int xMin = Math.max((int) Math.floor(rcX - visionRadius), 0);
        int xMax = Math.min((int) Math.ceil(rcX + visionRadius), mapWidth - 1);
        int yMin = Math.max((int) Math.floor(rcY - visionRadius), 0);
        int yMax = Math.min((int) Math.ceil(rcY + visionRadius), mapHeight - 1);
        for (int i = xMin / 5; i <= xMax / 5; i++) {
            for (int j = yMin / 5; j <= yMax / 5; j++) {
                MapLocation sensingLocation = new MapLocation(5 * i + 2, 5 * j + 2);
                if (rcLocation.distanceSquaredTo(sensingLocation) <= differenceRadiusSquared) {
                    grid[i][j] = rc.senseNearbyLocationsWithLead(sensingLocation, sensingRadiusSquared).length == 0;
                    writeGridToArray((i * gridLength + j) / 16);
                }
            }
        }

        // update both internal and shared arrays
        writeToSharedArray();
    }

    public MapLocation minerRandomLocation() throws GameActionException {
        updatefromSharedArray();
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
