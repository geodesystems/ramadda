/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.grid;


import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.IOUtil;

import ucar.unidata.util.StringUtil;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.*;


import java.io.File;

import java.util.ArrayList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;





/**
 */
@SuppressWarnings("unchecked")
public class LatLonGrid extends Grid {

    /** url arg */
    public static final double GRID_MISSING = -99999.9;

    /** The values */
    private double[][] valueGrid;

    /** _more_ */
    private double[][] averageGrid;

    /** _more_ */
    private double[][] minGrid;

    /** _more_ */
    private double[][] maxGrid;

    /** Array of counts */
    private int[][] countGrid;

    /** _more_ */
    private double min = Double.NaN;

    /** _more_ */
    private double max = Double.NaN;

    /**
     * ctor
     *
     * @param width number of columns
     * @param height number of rows
     * @param north northern bounds
     * @param west west bounds
     * @param south southern bounds
     * @param east east bounds
     */
    public LatLonGrid(int width, int height, double north, double west,
                      double south, double east) {
        super(width, height, north, west, south, east);
    }

    /**
     * _more_
     *
     * @param that _more_
     */
    public LatLonGrid(LatLonGrid that) {
        this(that, that.getValueGrid());
        if (that.minGrid != null) {
            minGrid = GridUtils.cloneArray(that.minGrid);
        }
        if (that.maxGrid != null) {
            maxGrid = GridUtils.cloneArray(that.maxGrid);
        }
    }

    /**
     * _more_
     *
     * @param that _more_
     * @param grid _more_
     */
    public LatLonGrid(LatLonGrid that, double[][] grid) {
        this(that.getWidth(), that.getHeight(), that.getNorth(),
             that.getWest(), that.getSouth(), that.getEast());
        this.max = that.max;
        this.min = that.min;

        if (that.valueGrid != null) {
            valueGrid = GridUtils.cloneArray(grid);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<double[]> getBoundingPolygon() {
        return getBoundingPolygon(countGrid);
    }

    /**
     * _more_
     *
     * @param countGrid _more_
     *
     * @return _more_
     */
    public List<double[]> getBoundingPolygon(int[][] countGrid) {
        List<double[]> polygon = new ArrayList<double[]>();
        if (countGrid == null) {
            return polygon;
        }
        ArrayList<Point> points   = new ArrayList<Point>();
        int              firstCol = -1;
        int              lastRow  = -1;
        int              lastCol  = 0;
        for (int row = 0; row < getHeight(); row++) {
            for (int col = 0; col < getWidth(); col++) {
                if (countGrid[row][col] != 0) {
                    if (firstCol == -1) {
                        firstCol = col;
                    }
                    lastCol = col;
                    polygon.add(new double[] { getLatitude(row),
                            getLongitude(col) });

                    //                    points.add(new Point(col,row));
                    break;
                }
            }
        }




        lastRow = getHeight() - 1;
        for (int col = lastCol; col < getWidth(); col++) {
            for (int row = getHeight() - 1; row >= 0; row--) {
                if (countGrid[row][col] != 0) {
                    lastRow = row;
                    polygon.add(new double[] { getLatitude(row),
                            getLongitude(col) });

                    //                    points.add(new Point(col,row));
                    break;
                }
            }
        }
        lastCol = getWidth() - 1;
        for (int row = lastRow; row >= 0; row--) {
            for (int col = getWidth() - 1; col >= 0; col--) {
                if (countGrid[row][col] != 0) {
                    lastCol = col;
                    polygon.add(new double[] { getLatitude(row),
                            getLongitude(col) });

                    //                    points.add(new Point(col, row));
                    break;
                }
            }
        }
        for (int col = lastCol; col >= firstCol; col--) {
            for (int row = 0; row < getHeight(); row++) {
                if (countGrid[row][col] != 0) {
                    polygon.add(new double[] { getLatitude(row),
                            getLongitude(col) });

                    //                    points.add(new Point(col,row));
                    break;
                }
            }
        }


        /*
        points  = FastConvexHull.execute(points);
        polygon = new ArrayList<double[]>();
        for(Point p: points) {
            int col = p.x;
            int row = p.y;
            polygon.add(new double[] { getLatitude(row),
                                       getLongitude(col) });
        }
        */
        return polygon;
    }



    /**
     * _more_
     */
    public void resetGrid() {
        fillValue(Double.NaN);
        fillMin(Double.NaN);
        fillMax(Double.NaN);
        min = Double.NaN;
        max = Double.NaN;
    }

    /**
     * create if needed and return the grid
     *
     * @return the grid
     */
    public double[][] getValueGrid() {
        if (valueGrid == null) {
            valueGrid = new double[getHeight()][getWidth()];
        }

        return valueGrid;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public double[][] getAverageGrid() {
        return getAverageGrid(Double.NaN);
    }

    /**
     * _more_
     *
     * @param fill _more_
     *
     * @return _more_
     */
    public double[][] getAverageGrid(double fill) {
        if (averageGrid == null) {
            averageGrid = new double[getHeight()][getWidth()];
            GridUtils.fill(averageGrid, fill);
        }

        return averageGrid;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public double[][] getMinGrid() {
        if (minGrid == null) {
            minGrid = new double[getHeight()][getWidth()];
            GridUtils.fill(minGrid, Double.NaN);
        }

        return minGrid;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public double[][] getMaxGrid() {
        if (maxGrid == null) {
            maxGrid = new double[getHeight()][getWidth()];
            GridUtils.fill(maxGrid, Double.NaN);
        }

        return maxGrid;
    }

    /**
     * create/initialize to 0 if needed and return the cnt grid
     *
     * @return count grid
     */
    public int[][] getCountGrid() {
        if (countGrid == null) {
            countGrid = new int[getHeight()][getWidth()];
            GridUtils.fill(countGrid, 0);
        }

        return countGrid;
    }

    /**
     * fill the grid
     *
     * @param value fill value
     *
     * @return for convenience return the grid
     */
    public double[][] fillValue(double value) {
        GridUtils.fill(getValueGrid(), value);

        return getValueGrid();
    }

    /**
     * _more_
     *
     * @param value _more_
     *
     * @return _more_
     */
    public double[][] fillMin(double value) {
        GridUtils.fill(getMinGrid(), value);

        return getMinGrid();
    }


    /**
     * _more_
     *
     * @param value _more_
     *
     * @return _more_
     */
    public double[][] fillMax(double value) {
        GridUtils.fill(getMaxGrid(), value);

        return getMaxGrid();
    }




    /**
     * _more_
     *
     * @param lat _more_
     * @param lon _more_
     */
    public void incrementCount(double lat, double lon) {
        int yIndex = getLatitudeIndex(lat);
        int xIndex = getLongitudeIndex(lon);
        incrementCountByIndex(yIndex, xIndex);
    }



    /**
     * _more_
     *
     * @param yIndex _more_
     * @param xIndex _more_
     */
    public void incrementCountByIndex(int yIndex, int xIndex) {
        getCountGrid()[yIndex][xIndex]++;
    }

    /**
     * _more_
     *
     * @param lat _more_
     * @param lon _more_
     *
     * @return _more_
     */
    public int getCount(double lat, double lon) {
        int yIndex = getLatitudeIndex(lat);
        int xIndex = getLongitudeIndex(lon);

        return getCountGrid()[yIndex][xIndex];
    }

    /**
     * _more_
     *
     * @param lat _more_
     * @param lon _more_
     *
     * @return _more_
     */
    public double getValue(double lat, double lon) {
        int yIndex = getLatitudeIndex(lat);
        int xIndex = getLongitudeIndex(lon);

        return getValueGrid()[yIndex][xIndex];
    }

    /**
     * _more_
     *
     * @param yIndex _more_
     * @param xIndex _more_
     *
     * @return _more_
     */
    public double getValueFromIndex(int yIndex, int xIndex) {
        return getValueGrid()[yIndex][xIndex];
    }


    /**
     * _more_
     *
     * @param lat _more_
     * @param lon _more_
     *
     * @return _more_
     */
    public double getMin(double lat, double lon) {
        int yIndex = getLatitudeIndex(lat);
        int xIndex = getLongitudeIndex(lon);

        return getMinGrid()[yIndex][xIndex];
    }

    /**
     * _more_
     *
     * @param yIndex _more_
     * @param xIndex _more_
     *
     * @return _more_
     */
    public double getMinFromIndex(int yIndex, int xIndex) {
        return getMinGrid()[yIndex][xIndex];
    }

    /**
     * _more_
     *
     * @param lat _more_
     * @param lon _more_
     *
     * @return _more_
     */
    public double getMax(double lat, double lon) {
        int yIndex = getLatitudeIndex(lat);
        int xIndex = getLongitudeIndex(lon);

        return getMaxGrid()[yIndex][xIndex];
    }

    /**
     * _more_
     *
     * @param yIndex _more_
     * @param xIndex _more_
     *
     * @return _more_
     */
    public double getMaxFromIndex(int yIndex, int xIndex) {
        return getMaxGrid()[yIndex][xIndex];
    }

    /**
     * _more_
     *
     * @param yIndex _more_
     * @param xIndex _more_
     * @param value _more_
     */
    private void checkMinMax(int yIndex, int xIndex, double value) {
        if ((min != min) || (value < min)) {
            min = value;
        }
        if ((max != max) || (value > max)) {
            max = value;
        }

        double[][] minGrid = getMinGrid();
        double[][] maxGrid = getMaxGrid();
        if (Double.isNaN(minGrid[yIndex][xIndex])) {
            minGrid[yIndex][xIndex] = value;
        } else {
            minGrid[yIndex][xIndex] = Math.min(minGrid[yIndex][xIndex],
                    value);
        }
        if (Double.isNaN(maxGrid[yIndex][xIndex])) {
            maxGrid[yIndex][xIndex] = value;
        } else {
            maxGrid[yIndex][xIndex] = Math.max(minGrid[yIndex][xIndex],
                    value);
        }

    }



    /**
     * _more_
     *
     * @return _more_
     */
    public double getMin() {
        return min;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public double getMax() {
        return max;
    }

    /**
     * _more_
     *
     * @param lat _more_
     * @param lon _more_
     * @param value _more_
     */
    public void setValue(double lat, double lon, double value) {
        setValueByIndex(getLatitudeIndex(lat), getLongitudeIndex(lon), value);
    }

    /**
     * _more_
     *
     * @param yIndex _more_
     * @param xIndex _more_
     * @param value _more_
     */
    public void setValueByIndex(int yIndex, int xIndex, double value) {
        checkMinMax(yIndex, xIndex, value);
        double[][] valueGrid = getValueGrid();
        valueGrid[yIndex][xIndex]      = value;
        getCountGrid()[yIndex][xIndex] = 1;
    }


    /**
     * _more_
     *
     * @param lat _more_
     * @param lon _more_
     * @param value _more_
     */
    public void addValue(double lat, double lon, double value) {
        int yIndex = getLatitudeIndex(lat);
        int xIndex = getLongitudeIndex(lon);
        if ((yIndex < 0) || (xIndex < 0)) {
            System.err.println("Bad index:" + yIndex + "/" + xIndex + " "
                               + lat + "/" + lon);
            System.err.println("grid:" + this);
        }
        addValueToIndex(yIndex, xIndex, value);
    }

    /**
     * _more_
     *
     * @param yIndex _more_
     * @param xIndex _more_
     * @param value _more_
     */
    public void addValueToIndex(int yIndex, int xIndex, double value) {
        double[][] valueGrid = getValueGrid();
        double     current   = valueGrid[yIndex][xIndex];
        if (Double.isNaN(current)) {
            valueGrid[yIndex][xIndex] = value;
        } else {
            valueGrid[yIndex][xIndex] += value;
        }
        getCountGrid()[yIndex][xIndex]++;
        checkMinMax(yIndex, xIndex, value);
    }



    /**
     * _more_
     */
    public void doAverageValues() {
        double[][] valueGrid   = getValueGrid();
        double[][] averageGrid = getAverageGrid();
        int[][]    countGrid   = getCountGrid();
        int        height      = getHeight();
        int        width       = getWidth();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double value = valueGrid[y][x];
                int    count = countGrid[y][x];
                if (count > 0) {
                    averageGrid[y][x] = valueGrid[y][x] / count;
                }
            }
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return "llg: north=" + getNorth() + " west=" + getWest() + " south="
               + getSouth() + " east=" + getEast() + " dimensions:"
               + getWidth() + "/" + getHeight();
    }







    /**
     * _more_
     */
    public static void generateGrid() {
        for (double lon = -180; lon <= 180; lon++) {
            for (double lat = -90; lat <= 90; lat++) {
                double value = 0;
                value = (lat + 90) / (180 + 90);
                System.out.println(lon + "," + lat + "," + value);
            }
        }
    }






    /**
     * _more_
     *
     * @param filename _more_
     *
     * @throws Exception _more_
     */
    public void readGrid(String filename) throws Exception {
        for (String line :
                StringUtil.split(IOUtil.readContents(filename,
                    LatLonGrid.class), "\n", true, true)) {
            if (line.startsWith("#")) {
                continue;
            }
            List<String> toks = StringUtil.split(line, ",", true, true);
            if (toks.size() != 3) {
                throw new IllegalArgumentException("Bad line:" + line);
            }
            double lon   = Double.parseDouble(toks.get(0));
            double lat   = Double.parseDouble(toks.get(1));
            double value = Double.parseDouble(toks.get(2));
            setValue(lat, lon, value);
        }
    }

    /**
     * _more_
     *
     * @param filename _more_
     *
     * @throws Exception _more_
     */
    public void writeImage(String filename) throws Exception {
        writeImage(filename, null);
    }

    /**
     * _more_
     *
     * @param filename _more_
     * @param label _more_
     *
     * @throws Exception _more_
     */
    public void writeImage(String filename, String label) throws Exception {
        writeImage(filename, GRAYSCALE, Color.CYAN, label);
    }

    /**
     * _more_
     *
     * @param filename _more_
     * @param colorScale _more_
     * @param bgColor _more_
     *
     * @throws Exception _more_
     */
    public void writeImage(String filename, int[][] colorScale, Color bgColor)
            throws Exception {
        writeImage(filename, colorScale, bgColor, null);
    }

    /**
     * _more_
     *
     * @param filename _more_
     * @param colorScale _more_
     * @param bgColor _more_
     * @param label _more_
     *
     * @throws Exception _more_
     */
    public void writeImage(String filename, int[][] colorScale,
                           Color bgColor, String label)
            throws Exception {
        int[]  pixels     = new int[getWidth() * getHeight()];
        int    index      = 0;

        double gridHeight = getGridHeight();
        double gridWidth  = getGridWidth();

        //Fill with cyan                                                
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                pixels[index++] = ((0xff << 24) | (bgColor.getRed() << 16)
                                   | (bgColor.getGreen() << 8)
                                   | bgColor.getBlue());
            }
        }

        double[][] grid = getValueGrid();
        index = 0;
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                double value = grid[y][x];
                if (value == value) {
                    pixels[index] = getPixelValue(colorScale, value);
                } else {
                    //NaN
                }
                index++;
            }
        }

        Image newImage = Toolkit.getDefaultToolkit().createImage(
                             new MemoryImageSource(
                                 getWidth(), getHeight(), pixels, 0,
                                 getWidth()));
        if (label != null) {
            Image newNewImage = new BufferedImage(getWidth(), getHeight(),
                                    BufferedImage.TYPE_INT_ARGB);
            Graphics g = newNewImage.getGraphics();
            g.drawImage(newImage, 0, 0, null, null);
            g.setColor(Color.magenta);
            g.drawString(label, 10, getHeight() - 10);
            newImage = newNewImage;
        }

        File imageFile = new File(filename);
        ImageUtils.writeImageToFile(newImage, imageFile);
    }





    /**
     * _more_
     *
     * @param table _more_
     * @param percent _more_
     *
     * @return _more_
     */
    public static Color getColor(int[][] table, double percent) {
        int index = (int) (percent * table.length) - 1;
        if (index < 0) {
            index = 0;
        }
        if (index >= table.length) {
            index = table.length - 1;
        }
        int[] c = table[index];

        return new Color(c[0], c[1], c[2]);
    }


    /**
     * _more_
     *
     * @param table _more_
     * @param value _more_
     *
     * @return _more_
     */
    public int getPixelValue(int[][] table, double value) {
        double percent = ((max == min)
                          ? 0.5
                          : (value - min) / (max - min));
        Color  c       = getColor(table, percent);

        return ((0xff << 24) | (c.getRed() << 16) | (c.getGreen() << 8)
                | c.getBlue());
    }

    /** _more_ */
    public static final int[][] GRAYSCALE = {
        { 0, 0, 0 }, { 1, 1, 1 }, { 2, 2, 2 }, { 3, 3, 3 }, { 4, 4, 4 },
        { 5, 5, 5 }, { 6, 6, 6 }, { 7, 7, 7 }, { 8, 8, 8 }, { 9, 9, 9 },
        { 10, 10, 10 }, { 11, 11, 11 }, { 12, 12, 12 }, { 13, 13, 13 },
        { 14, 14, 14 }, { 15, 15, 15 }, { 16, 16, 16 }, { 17, 17, 17 },
        { 18, 18, 18 }, { 19, 19, 19 }, { 20, 20, 20 }, { 21, 21, 21 },
        { 22, 22, 22 }, { 23, 23, 23 }, { 24, 24, 24 }, { 25, 25, 25 },
        { 26, 26, 26 }, { 27, 27, 27 }, { 28, 28, 28 }, { 29, 29, 29 },
        { 30, 30, 30 }, { 31, 31, 31 }, { 32, 32, 32 }, { 33, 33, 33 },
        { 34, 34, 34 }, { 35, 35, 35 }, { 36, 36, 36 }, { 37, 37, 37 },
        { 38, 38, 38 }, { 39, 39, 39 }, { 40, 40, 40 }, { 41, 41, 41 },
        { 42, 42, 42 }, { 43, 43, 43 }, { 44, 44, 44 }, { 45, 45, 45 },
        { 46, 46, 46 }, { 47, 47, 47 }, { 48, 48, 48 }, { 49, 49, 49 },
        { 50, 50, 50 }, { 51, 51, 51 }, { 52, 52, 52 }, { 53, 53, 53 },
        { 54, 54, 54 }, { 55, 55, 55 }, { 56, 56, 56 }, { 57, 57, 57 },
        { 58, 58, 58 }, { 59, 59, 59 }, { 60, 60, 60 }, { 61, 61, 61 },
        { 62, 62, 62 }, { 63, 63, 63 }, { 64, 64, 64 }, { 65, 65, 65 },
        { 66, 66, 66 }, { 67, 67, 67 }, { 68, 68, 68 }, { 69, 69, 69 },
        { 70, 70, 70 }, { 71, 71, 71 }, { 72, 72, 72 }, { 73, 73, 73 },
        { 74, 74, 74 }, { 75, 75, 75 }, { 76, 76, 76 }, { 77, 77, 77 },
        { 78, 78, 78 }, { 79, 79, 79 }, { 80, 80, 80 }, { 81, 81, 81 },
        { 82, 82, 82 }, { 83, 83, 83 }, { 84, 84, 84 }, { 85, 85, 85 },
        { 86, 86, 86 }, { 87, 87, 87 }, { 88, 88, 88 }, { 89, 89, 89 },
        { 90, 90, 90 }, { 91, 91, 91 }, { 92, 92, 92 }, { 93, 93, 93 },
        { 94, 94, 94 }, { 95, 95, 95 }, { 96, 96, 96 }, { 97, 97, 97 },
        { 98, 98, 98 }, { 99, 99, 99 }, { 100, 100, 100 }, { 101, 101, 101 },
        { 102, 102, 102 }, { 103, 103, 103 }, { 104, 104, 104 },
        { 105, 105, 105 }, { 106, 106, 106 }, { 107, 107, 107 },
        { 108, 108, 108 }, { 109, 109, 109 }, { 110, 110, 110 },
        { 111, 111, 111 }, { 112, 112, 112 }, { 113, 113, 113 },
        { 114, 114, 114 }, { 115, 115, 115 }, { 116, 116, 116 },
        { 117, 117, 117 }, { 118, 118, 118 }, { 119, 119, 119 },
        { 120, 120, 120 }, { 121, 121, 121 }, { 122, 122, 122 },
        { 123, 123, 123 }, { 124, 124, 124 }, { 125, 125, 125 },
        { 126, 126, 126 }, { 127, 127, 127 }, { 128, 128, 128 },
        { 129, 129, 129 }, { 130, 130, 130 }, { 131, 131, 131 },
        { 132, 132, 132 }, { 133, 133, 133 }, { 134, 134, 134 },
        { 135, 135, 135 }, { 136, 136, 136 }, { 137, 137, 137 },
        { 138, 138, 138 }, { 139, 139, 139 }, { 140, 140, 140 },
        { 141, 141, 141 }, { 142, 142, 142 }, { 143, 143, 143 },
        { 144, 144, 144 }, { 145, 145, 145 }, { 146, 146, 146 },
        { 147, 147, 147 }, { 148, 148, 148 }, { 149, 149, 149 },
        { 150, 150, 150 }, { 151, 151, 151 }, { 152, 152, 152 },
        { 153, 153, 153 }, { 154, 154, 154 }, { 155, 155, 155 },
        { 156, 156, 156 }, { 157, 157, 157 }, { 158, 158, 158 },
        { 159, 159, 159 }, { 160, 160, 160 }, { 161, 161, 161 },
        { 162, 162, 162 }, { 163, 163, 163 }, { 164, 164, 164 },
        { 165, 165, 165 }, { 166, 166, 166 }, { 167, 167, 167 },
        { 168, 168, 168 }, { 169, 169, 169 }, { 170, 170, 170 },
        { 171, 171, 171 }, { 172, 172, 172 }, { 173, 173, 173 },
        { 174, 174, 174 }, { 175, 175, 175 }, { 176, 176, 176 },
        { 177, 177, 177 }, { 178, 178, 178 }, { 179, 179, 179 },
        { 180, 180, 180 }, { 181, 181, 181 }, { 182, 182, 182 },
        { 183, 183, 183 }, { 184, 184, 184 }, { 185, 185, 185 },
        { 186, 186, 186 }, { 187, 187, 187 }, { 188, 188, 188 },
        { 189, 189, 189 }, { 190, 190, 190 }, { 191, 191, 191 },
        { 192, 192, 192 }, { 193, 193, 193 }, { 194, 194, 194 },
        { 195, 195, 195 }, { 196, 196, 196 }, { 197, 197, 197 },
        { 198, 198, 198 }, { 199, 199, 199 }, { 200, 200, 200 },
        { 201, 201, 201 }, { 202, 202, 202 }, { 203, 203, 203 },
        { 204, 204, 204 }, { 205, 205, 205 }, { 206, 206, 206 },
        { 207, 207, 207 }, { 208, 208, 208 }, { 209, 209, 209 },
        { 210, 210, 210 }, { 211, 211, 211 }, { 212, 212, 212 },
        { 213, 213, 213 }, { 214, 214, 214 }, { 215, 215, 215 },
        { 216, 216, 216 }, { 217, 217, 217 }, { 218, 218, 218 },
        { 219, 219, 219 }, { 220, 220, 220 }, { 221, 221, 221 },
        { 222, 222, 222 }, { 223, 223, 223 }, { 224, 224, 224 },
        { 225, 225, 225 }, { 226, 226, 226 }, { 227, 227, 227 },
        { 228, 228, 228 }, { 229, 229, 229 }, { 230, 230, 230 },
        { 231, 231, 231 }, { 232, 232, 232 }, { 233, 233, 233 },
        { 234, 234, 234 }, { 235, 235, 235 }, { 236, 236, 236 },
        { 237, 237, 237 }, { 238, 238, 238 }, { 239, 239, 239 },
        { 240, 240, 240 }, { 241, 241, 241 }, { 242, 242, 242 },
        { 243, 243, 243 }, { 244, 244, 244 }, { 245, 245, 245 },
        { 246, 246, 246 }, { 247, 247, 247 }, { 248, 248, 248 },
        { 249, 249, 249 }, { 250, 250, 250 }, { 251, 251, 251 },
        { 252, 252, 252 }, { 253, 253, 253 }, { 254, 254, 254 },
        { 255, 255, 255 },
    };


    /**
     * If called with no arguments then it writes out a sample grid to the stdout
     * If it is provided with an grid file as an argument then it generates a grid.png image
     *
     * @param args cmd line args
     *
     * @throws Exception On badness
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            generateGrid();

            return;
        }
        LatLonGrid llg = new LatLonGrid(360, 180, 90, -180, -90, 180);
        llg.fillValue(1);
        llg.readGrid(args[0]);
        llg.writeImage("grid.png");
    }


    /**
     * _more_
     */
    public void fillMissing() {
        int        width     = getWidth();
        int        height    = getHeight();
        double[][] valueGrid = getValueGrid();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (valueGrid[y][x] != valueGrid[y][x]) {
                    int     delta                 = width / 100;
                    boolean foundNonMissingNearby = false;
                    for (int dx = -delta; dx < delta; dx++) {
                        for (int dy = -delta; dy < delta; dy++) {
                            int nx = x + dx;
                            int ny = y + dy;
                            if ((nx >= 0) && (nx < valueGrid[0].length)
                                    && (ny >= 0) && (ny < valueGrid.length)) {
                                if ((valueGrid[ny][nx] == valueGrid[ny][nx])
                                        && (valueGrid[ny][nx]
                                            != GRID_MISSING)) {
                                    foundNonMissingNearby = true;
                                }
                            }
                        }
                    }
                    if ( !foundNonMissingNearby) {
                        valueGrid[y][x] = GRID_MISSING;
                    }
                }
            }
        }

        for (int pass = 0; pass < 1; pass++) {
            boolean anyMissing = false;
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    if (fillMissingFromNeighbors(valueGrid, x, y)) {
                        anyMissing = true;
                    }
                }
            }
            if (anyMissing) {
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        if (fillMissingFromNeighbors(valueGrid, x, y)) {
                            anyMissing = true;
                        }
                    }
                }
            }
            if (anyMissing) {
                for (int y = height - 1; y >= 0; y--) {
                    for (int x = width - 1; x >= 0; x--) {
                        if (fillMissingFromNeighbors(valueGrid, x, y)) {
                            anyMissing = true;
                        }
                    }
                }
            }
            if (anyMissing) {
                for (int x = width - 1; x >= 0; x--) {
                    for (int y = height - 1; y >= 0; y--) {
                        if (fillMissingFromNeighbors(valueGrid, x, y)) {
                            anyMissing = true;
                        }
                    }
                }
            }
            if ( !anyMissing) {
                break;
            }
        }
    }


    /**
     * _more_
     *
     * @param theGrid the grid to fill
     * @param x _more_
     * @param y _more_
     *
     * @return _more_
     */
    public static boolean fillMissingFromNeighbors(double[][] theGrid, int x,
            int y) {
        if (theGrid[y][x] == theGrid[y][x]) {
            return false;
        }
        if (theGrid[y][x] == GRID_MISSING) {
            return false;
        }
        double sum = 0;
        int    cnt = 0;
        for (int dx = -1; dx < 2; dx++) {
            for (int dy = -1; dy < 2; dy++) {
                int nx = x + dx;
                int ny = y + dy;
                if ((nx >= 0) && (nx < theGrid[0].length) && (ny >= 0)
                        && (ny < theGrid.length)) {
                    if ((theGrid[ny][nx] == theGrid[ny][nx])
                            && (theGrid[ny][nx] != GRID_MISSING)) {
                        sum += theGrid[ny][nx];
                        cnt++;
                    }
                }
            }
        }
        if (cnt > 0) {
            theGrid[y][x] = sum / cnt;
        }

        return true;
    }


    //Copied from http://code.google.com/p/convex-hull/source/checkout

    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Aug 23, '13
     * @author         Enter your name here...
     */
    public static class FastConvexHull {

        /**
         * _more_
         *
         * @param points _more_
         *
         * @return _more_
         */
        public static ArrayList<Point> execute(ArrayList<Point> points) {
            ArrayList<Point> xSorted = (ArrayList<Point>) points.clone();
            Collections.sort(xSorted, new XCompare());

            int     n      = xSorted.size();

            Point[] lUpper = new Point[n];

            lUpper[0] = xSorted.get(0);
            lUpper[1] = xSorted.get(1);

            int lUpperSize = 2;

            for (int i = 2; i < n; i++) {
                lUpper[lUpperSize] = xSorted.get(i);
                lUpperSize++;

                while ((lUpperSize > 2)
                        && !rightTurn(lUpper[lUpperSize - 3],
                                      lUpper[lUpperSize - 2],
                                      lUpper[lUpperSize - 1])) {
                    // Remove the middle point of the three last
                    lUpper[lUpperSize - 2] = lUpper[lUpperSize - 1];
                    lUpperSize--;
                }
            }

            Point[] lLower = new Point[n];

            lLower[0] = xSorted.get(n - 1);
            lLower[1] = xSorted.get(n - 2);

            int lLowerSize = 2;

            for (int i = n - 3; i >= 0; i--) {
                lLower[lLowerSize] = xSorted.get(i);
                lLowerSize++;

                while ((lLowerSize > 2)
                        && !rightTurn(lLower[lLowerSize - 3],
                                      lLower[lLowerSize - 2],
                                      lLower[lLowerSize - 1])) {
                    // Remove the middle point of the three last
                    lLower[lLowerSize - 2] = lLower[lLowerSize - 1];
                    lLowerSize--;
                }
            }

            ArrayList<Point> result = new ArrayList<Point>();

            for (int i = 0; i < lUpperSize; i++) {
                result.add(lUpper[i]);
            }

            for (int i = 1; i < lLowerSize - 1; i++) {
                result.add(lLower[i]);
            }

            return result;
        }

        /**
         * _more_
         *
         * @param a _more_
         * @param b _more_
         * @param c _more_
         *
         * @return _more_
         */
        private static boolean rightTurn(Point a, Point b, Point c) {
            return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x) > 0;
        }

        /**
         * Class description
         *
         *
         * @version        $version$, Fri, Aug 23, '13
         * @author         Enter your name here...
         */
        private static class XCompare implements Comparator<Point> {

            /**
             * _more_
             *
             * @param o1 _more_
             * @param o2 _more_
             *
             * @return _more_
             */
            public int compare(Point o1, Point o2) {
                return (Integer.valueOf(o1.x)).compareTo(Integer.valueOf(o2.x));
            }
        }
    }


}
