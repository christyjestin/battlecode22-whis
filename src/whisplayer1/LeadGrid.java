package whisplayer1;

import battlecode.common.*;

public strictfp class LeadGrid {

    RobotController rc;
    int[][] grid = new int[10][10];
    int[] array = new int[20]; // internal copy of shared array
    int mapHeight;
    int mapWidth;
    int gridHeight;
    int gridWidth;
    int visionRadiusSquared; // visionRadiusSquared of the rc using this LeadGrid
    double rawValuePerLevel; // how much lead each grid square needs to have in total to go up a level
    final double leadLevelMultiplier = 20.0; // rawValuePerLevel = area of a grid square x this multiplier

    public LeadGrid(RobotController rc, int visionRadiusSquared, int mapHeight, int mapWidth) {
        this.rc = rc;
        this.visionRadiusSquared = visionRadiusSquared;
        this.mapHeight = mapHeight;
        this.mapWidth = mapWidth;
        this.gridHeight = (int) Math.ceil(mapHeight / 10.0);
        this.gridWidth = (int) Math.ceil(mapWidth / 10.0);
        this.rawValuePerLevel = gridHeight * gridWidth * leadLevelMultiplier;
    }

    // update internal array using shared array
    public void updatefromSharedArray() throws GameActionException {
        for (int i = 0; i < 20; i++) {
            array[i] = rc.readSharedArray(RobotPlayer.leadGridStartIndex + i);
        }
    }

    // write the encodings from internal array to shared array
    public void writeToSharedArray() throws GameActionException {
        for (int i = 0; i < 20; i++) {
            if (rc.readSharedArray(RobotPlayer.leadGridStartIndex + i) != array[i]) {
                rc.writeSharedArray(RobotPlayer.leadGridStartIndex + i, array[i]);
            }
        }
    }

    // encode and write grid to internal array
    public void writeGridToArray() {
        int arrayIndex = 0;
        int encoding = 0;
        for (int i = 0; i < 100; i++) {
            // times 8, plus next value (but written w/ bitwise operators)
            encoding = (encoding << 3) | grid[i / 10][i % 10];
            // 5 grid positions per array index
            if (i % 5 == 4) {
                array[arrayIndex] = encoding;
                encoding = 0;
                arrayIndex++;
            }
        }
    }

    // update grid by decoding internal array
    public void updateGridfromArray() {
        for (int i = 0; i < 20; i++) {
            int encoding = array[i];
            // go in reverse order while decoding
            for (int j = 5 * i + 4; j >= 5 * i; j--) {
                // equivalent to % 8 in bitwise
                grid[j / 10][j % 10] = encoding & 7;
                // divide by 8
                encoding = encoding >> 3;
            }
        }
    }

    // use array of map locations to update grid
    public void updateGridFromNearbyLocations(MapLocation rcLocation, MapLocation[] locations)
        throws GameActionException {
        Boolean[][] canWrite = new Boolean[10][10];
        int[][] rawValues = new int[10][10];
        for (MapLocation location : locations) {
            int[] indices = gridIndexFromLocation(location);
            int gridX = indices[0];
            int gridY = indices[1];
            if (canWrite[gridX][gridY] == null) {
                canWrite[gridX][gridY] = canWriteToGridIndex(rcLocation, indices);
            }
            if (canWrite[gridX][gridY]) rawValues[gridX][gridY] += rc.senseLead(location);
        }
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (canWrite[i][j] != null && canWrite[i][j]) {
                    grid[i][j] = (int) Math.ceil(rawValues[i][j] / rawValuePerLevel);
                    // max level in grid is 7
                    if (grid[i][j] > 7) grid[i][j] = 7;
                    System.out.println("x: " + i + ", y: " + j);
                }
            }
        }
        // update both internal and shared arrays
        writeGridToArray();
        writeToSharedArray();
    }

    // calculate the grid index containing this map location
    public int[] gridIndexFromLocation(MapLocation location) {
        return new int[] { location.x / gridWidth, location.y / gridHeight };
    }

    // checks if the rc can see the entire grid square based on its current location
    public boolean canWriteToGridIndex(MapLocation rcLocation, int[] gridIndices) {
        int xMin = gridWidth * gridIndices[0];
        int yMin = gridHeight * gridIndices[1];
        int xMax = xMin + gridWidth - 1;
        int yMax = yMin + gridHeight - 1;
        if (xMax > mapWidth - 1) xMax = mapWidth - 1;
        if (yMax > mapHeight - 1) yMax = mapHeight - 1;
        MapLocation topLeft = new MapLocation(xMin, yMax);
        MapLocation topRight = new MapLocation(xMax, yMax);
        MapLocation bottomLeft = new MapLocation(xMin, yMin);
        MapLocation bottomRight = new MapLocation(xMax, yMin);
        MapLocation[] corners = { topLeft, topRight, bottomLeft, bottomRight };
        for (MapLocation corner : corners) {
            if (!corner.isWithinDistanceSquared(rcLocation, visionRadiusSquared)) return false;
        }
        return true;
    }
}
