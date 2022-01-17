package whisplayer1;

import battlecode.common.*;

public strictfp class RubbleGrid {

    RobotController rc;
    int gridRows = 8;
    int gridCols = 10;
    int gridSize = gridRows * gridCols;
    int arrayLength = (int) Math.ceil(gridSize / 5.0);
    int[][] grid = new int[gridCols][gridRows];
    int[] array = new int[arrayLength]; // internal copy of shared array
    int lastGridCol;
    int lastGridRow;
    int mapHeight;
    int mapWidth;
    int gridHeight;
    int gridWidth;
    int visionRadiusSquared; // visionRadiusSquared of the rc using this LeadGrid
    double rawValuePerLevel; // how much lead each grid square needs to have in total to go up a level
    // rawValuePerLevel = area of a grid square x this multiplier
    final double rubbleLevelMultiplier = GameConstants.MAX_RUBBLE / 7.0;

    public RubbleGrid(RobotController rc, int visionRadiusSquared, int mapHeight, int mapWidth) {
        this.rc = rc;
        this.visionRadiusSquared = visionRadiusSquared;
        this.mapHeight = mapHeight;
        this.mapWidth = mapWidth;
        this.gridHeight = (int) Math.ceil(mapHeight / (double) gridRows);
        this.gridWidth = (int) Math.ceil(mapWidth / (double) gridCols);
        this.lastGridCol = (mapWidth - 1) / gridWidth;
        this.lastGridRow = (mapHeight - 1) / gridHeight;
        this.rawValuePerLevel = gridHeight * gridWidth * rubbleLevelMultiplier;
    }

    // update internal array using shared array
    public void updatefromSharedArray() throws GameActionException {
        for (int i = 0; i < arrayLength; i++) {
            array[i] = rc.readSharedArray(RobotPlayer.rubbleGridStartIndex + i);
        }
    }

    // write the encodings from internal array to shared array
    public void writeToSharedArray() throws GameActionException {
        for (int i = 0; i < arrayLength; i++) {
            if (rc.readSharedArray(RobotPlayer.rubbleGridStartIndex + i) != array[i]) {
                rc.writeSharedArray(RobotPlayer.rubbleGridStartIndex + i, array[i]);
            }
        }
    }

    // encode and write grid to internal array
    public void writeGridToArray() {
        int arrayIndex = 0;
        int encoding = 0;
        for (int i = 0; i < gridSize; i++) {
            // times 8, plus next value (but written w/ bitwise operators)
            encoding = (encoding << 3) | grid[i % gridCols][i / gridCols];
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
        for (int i = 0; i < arrayLength; i++) {
            int encoding = array[i];
            // go in reverse order while decoding
            for (int j = 5 * i + 4; j >= 5 * i; j--) {
                // equivalent to % 8 in bitwise
                grid[j % gridCols][j / gridCols] = encoding & 7;
                // divide by 8
                encoding = encoding >> 3;
            }
        }
    }

    // use array of map locations to update grid
    public void updateGridFromNearbyLocations(MapLocation rcLocation, MapLocation[] locations)
        throws GameActionException {
        // refresh both internal array and grid
        // without this step, the function would write outdated data back to the shared array
        updatefromSharedArray();
        updateGridfromArray();
        Boolean[][] canWrite = new Boolean[gridCols][gridRows];
        int[][] rawValues = new int[gridCols][gridRows];
        for (MapLocation location : locations) {
            int[] indices = gridIndexFromLocation(location);
            int gridX = indices[0];
            int gridY = indices[1];
            if (canWrite[gridX][gridY] == null) {
                canWrite[gridX][gridY] = canWriteToGridIndex(rcLocation, indices);
            }
            if (canWrite[gridX][gridY]) rawValues[gridX][gridY] += rc.senseRubble(location);
        }
        for (int i = 0; i < gridCols; i++) {
            for (int j = 0; j < gridRows; j++) {
                if (canWrite[i][j] != null && canWrite[i][j]) {
                    grid[i][j] = (int) Math.ceil(rawValues[i][j] / rawValuePerLevel);
                    // max level in grid is 7; should not be necessary based on rubble level multiplier
                    if (grid[i][j] > 7) grid[i][j] = 7;
                }
            }
        }
        // update both internal and shared arrays
        writeGridToArray();
        writeToSharedArray();
    }

    // check if the rc and the target location are in the same grid square
    public boolean sameGridSquare(MapLocation rcLocation, MapLocation targetLocation) {
        int[] rcIndices = gridIndexFromLocation(rcLocation);
        int[] targetIndices = gridIndexFromLocation(targetLocation);
        return rcIndices[0] == targetIndices[0] && rcIndices[1] == targetIndices[1];
    }

    // calculate the grid index containing this map location
    public int[] gridIndexFromLocation(MapLocation location) {
        return new int[] { location.x / gridWidth, location.y / gridHeight };
    }

    // returns the indices of the (valid) grid square in this direction
    public int[] gridSquareInDirection(int[] gridIndices, Direction direction) {
        int gridX = gridIndices[0];
        int gridY = gridIndices[1];
        int rightX = Math.min(lastGridCol, gridX + 1);
        int leftX = Math.max(0, gridX - 1);
        int upY = Math.min(lastGridRow, gridY + 1);
        int downY = Math.max(0, gridY - 1);
        switch (direction) {
            case NORTH:
                return new int[] { gridX, upY };
            case NORTHEAST:
                return new int[] { rightX, upY };
            case EAST:
                return new int[] { rightX, gridY };
            case SOUTHEAST:
                return new int[] { rightX, downY };
            case SOUTH:
                return new int[] { gridX, downY };
            case SOUTHWEST:
                return new int[] { leftX, downY };
            case WEST:
                return new int[] { leftX, gridY };
            case NORTHWEST:
                return new int[] { leftX, upY };
            case CENTER:
                return new int[] { gridX, gridY }; // should not happen
            default:
                return null; // should not happen
        }
    }

    // return the map location of the center of the grid square
    public MapLocation centerLocation(int[] gridIndices) {
        int xMin = gridWidth * gridIndices[0];
        int yMin = gridHeight * gridIndices[1];
        int xMax = xMin + gridWidth - 1;
        int yMax = yMin + gridHeight - 1;
        if (xMax > mapWidth - 1) xMax = mapWidth - 1;
        if (yMax > mapHeight - 1) yMax = mapHeight - 1;
        return new MapLocation((xMax + xMin) / 2, (yMax + yMin) / 2);
    }

    // return the rubble score at this grid square
    public int rubbleScoreAtGridSquare(int[] indices) {
        return grid[indices[0]][indices[1]];
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
