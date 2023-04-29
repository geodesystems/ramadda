/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.grid;


import org.ramadda.repository.*;



/**
 * This originally held a number of gridding utilities but those  have been moved into the IdwGrid
 * Still have this around as a place holder for any other gridding methods we might want to add
 *
 * @author Jeff McWhirter
 */
public class Gridder {


    /**
     * make a hill shaded image from the incoming grid
     *
     *
     * @param input the original llg
     * @param grid the data grid
     * @param azimuth azimuth for shading
     * @param angle angle for shading
     *
     * @return a new LLG that holds the hillshaded data
     */
    public static LatLonGrid doHillShade(LatLonGrid input, double[][] grid,
                                         double azimuth, double angle) {

        LatLonGrid hillshadeGrid = new LatLonGrid(input, grid);
        hillshadeGrid.fillMissing();
        grid = GridUtils.cloneArray(hillshadeGrid.getValueGrid());
        hillshadeGrid.resetGrid();



        double   z                = 1.0f;
        double   nsres            = 1.0f;
        double   scale            = 1.0f;
        double   ewres            = 1.0f;

        double   degreesToRadians = (double) (Math.PI / 180.0);
        double   radiansToDegrees = (double) (180.0 / Math.PI);
        int      nYSize           = grid.length;
        int      nXSize           = grid[0].length;

        double[] win              = new double[9];

        int      i, j;
        /*  0 1 2
         *  3 4 5
         *  6 7 8
         */

        for (i = 0; i < nYSize; i++) {
            for (j = 0; j < nXSize; j++) {
                if ((i == 0) || (j == 0) || (i == nYSize - 1)
                        || (j == nXSize - 1)) {
                    continue;
                }
                boolean containsNull = false;
                win[0] = grid[i - 1][j - 1];
                win[1] = grid[i - 1][j];
                win[2] = grid[i - 1][j + 1];
                win[3] = grid[i][j - 1];
                win[4] = grid[i][j];
                win[5] = grid[i][j + 1];
                win[6] = grid[i + 1][j - 1];
                win[7] = grid[i + 1][j];
                win[8] = grid[i + 1][j + 1];


                for (int n = 0; n <= 8; n++) {
                    if (Double.isNaN(win[n])
                            || (win[n] == LatLonGrid.GRID_MISSING)) {
                        containsNull = true;

                        break;
                    }
                }
                if (containsNull) {
                    continue;
                }
                // First Slope ...

                double x = (double) (((z * win[0] + z * win[3] + z * win[3]
                                       + z * win[6]) - (z * win[2]
                                           + z * win[5] + z * win[5]
                                           + z * win[8])) / (8.0 * ewres
                                               * scale));

                double y = (double) (((z * win[6] + z * win[7] + z * win[7]
                                       + z * win[8]) - (z * win[0]
                                           + z * win[1] + z * win[1]
                                           + z * win[2])) / (8.0 * nsres
                                               * scale));

                double key = (double) Math.sqrt(x * x + y * y);
                double slope = (double) (90.0
                                         - Math.atan(key) * radiansToDegrees);
                double slopePct = 100 * key;
                double value    = slopePct;

                // ... then aspect...
                double aspect = (double) Math.atan2(x, y);

                // ... then the shade value
                double cang =
                    (double) (Math.sin(angle * degreesToRadians)
                              * Math.sin(slope * degreesToRadians) + Math.cos(
                                  angle * degreesToRadians) * Math.cos(
                                  slope * degreesToRadians) * Math.cos(
                                  (azimuth - 90.0) * degreesToRadians
                                  - aspect));

                if (cang <= 0.0) {
                    cang = 1.0f;
                } else {
                    cang = 1.0f + (254.0f * cang);
                }

                value = cang;
                hillshadeGrid.setValueByIndex(i, j, value);
            }
        }

        return hillshadeGrid;
    }








}
