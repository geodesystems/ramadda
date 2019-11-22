/*
* Copyright (c) 2008-2019 Geode Systems LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*     http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.ramadda.util;



import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;


import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


import ucar.unidata.util.*;



import java.io.FileOutputStream;
import java.io.InputStream;

import java.rmi.RemoteException;

import java.text.SimpleDateFormat;

import java.util.ArrayList;

import java.util.Date;
import java.util.HashSet;
import java.util.List;


/**
 * Class description
 *
 *
 * @version        $version$, Fri, Aug 23, '13
 * @author         Enter your name here...
 */
public class XlsUtil {

    /**
     * Convert excel to csv
     *
     * @param filename excel file
     * @param skipToFirstNumeric _more_
     * @param sdf If non null then use this to format any date cells
     *
     * @return csv
     *
     * @throws Exception On badness
     */
    public static String xlsToCsv(String filename) {
        try {

            StringBuffer sb   = new StringBuffer();
            InputStream myxls = IO.getInputStream(filename,
                                    XlsUtil.class);
            HSSFWorkbook wb         = new HSSFWorkbook(myxls);
            HSSFSheet    sheet      = wb.getSheetAt(0);
            boolean      seenNumber = false;
            for (int rowIdx = sheet.getFirstRowNum();
                    rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                HSSFRow row = sheet.getRow(rowIdx);
                if (row == null) {
                    continue;
                }

                short firstCol = row.getFirstCellNum();
                for (short col = firstCol; col < row.getLastCellNum();
                        col++) {
                    HSSFCell cell = row.getCell(col);
                    if (cell == null) {
                        break;
                    }
                    String value = cell.toString();
                    if (col > firstCol) {
                        sb.append(",");
                    }
                    sb.append(clean(value));
                }
                sb.append("\n");
            }

            return sb.toString();
        } catch (Exception exc) {
            throw new RuntimeException(exc);

        }
    }


    /**
     * _more_
     *
     * @param filename _more_
     *
     * @return _more_
     */
    public static String xlsxToCsv(String filename) {
        try {

            StringBuffer sb   = new StringBuffer();
            InputStream myxls = IO.getInputStream(filename,
                                    XlsUtil.class);
            XSSFWorkbook wb         = new XSSFWorkbook(myxls);
            XSSFSheet    sheet      = wb.getSheetAt(0);
            boolean      seenNumber = false;
            for (int rowIdx = sheet.getFirstRowNum();
                    rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                XSSFRow row = sheet.getRow(rowIdx);
                if (row == null) {
                    continue;
                }

                short firstCol = row.getFirstCellNum();
                for (short col = firstCol; col < row.getLastCellNum();
                        col++) {
                    XSSFCell cell = row.getCell(col);
                    if (cell == null) {
                        break;
                    }
                    String value = cell.toString();
                    if (col > firstCol) {
                        sb.append(",");
                    }
                    sb.append(clean(value));
                }
                sb.append("\n");
            }

            return sb.toString();
        } catch (Exception exc) {
            throw new RuntimeException(exc);

        }
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String clean(String s) {
        s = s.trim();
        while (s.endsWith(",")) {
            s = s.substring(0, s.length() - 1);
        }
        if ((s.indexOf(",") >= 0) || (s.indexOf("\n") >= 0)) {
            s = "\"" + s + "\"";
        }

        return s;
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        for (String arg : args) {
            String csv;
            if (arg.endsWith(".xlsx")) {
                csv = xlsxToCsv(arg);
            } else {
                csv = xlsToCsv(arg);
            }
            String newFile = IOUtil.stripExtension(arg) + ".csv";
            IOUtil.writeFile(newFile, csv);
        }
    }



}
