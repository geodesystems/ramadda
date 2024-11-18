/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import org.ramadda.util.seesv.Seesv;

import com.monitorjbl.xlsx.StreamingReader;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFShape;




import org.apache.poi.xssf.streaming.*;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;


import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import ucar.unidata.util.*;

import java.io.*;

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

    private static SimpleDateFormat sdf =  new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * _more_
     *
     * @param filename _more_
     *
     * @return _more_
     */
    public static InputStream xlsxToCsv(IO.Path path) {
        return xlsxToCsv(path, -1,-1);
    }


    /**
     *
     * @param filename _more_
     * @param maxRows _more_
      * @return _more_
     */
    public static InputStream xlsxToCsv(final IO.Path path, int maxRows,final int sheetNumber) {
        try {
            final PipedOutputStream    pos = new PipedOutputStream();
            final BufferedOutputStream bos = new BufferedOutputStream(pos);
            final PipedInputStream     pis = new PipedInputStream(pos);
            final PrintWriter          pw  = new PrintWriter(pos);
            ucar.unidata.util.Misc.run(new Runnable() {
                public void run() {
                    try {
			int _sheetNumber = sheetNumber;
                        InputStream is = new BufferedInputStream(
                                             IO.getInputStream(
							       path.getPath(), XlsUtil.class));
                        Workbook wb = StreamingReader.builder()
                        //                      .rowCacheSize(100)    
                        //                      .bufferSize(4096)     
                        .open(is);
                        //Only read the first sheet
                        for (Sheet sheet : wb) {
			    if(--_sheetNumber>0) {
				continue;
			    }
                            int rowIdx = 0;
                            for (Row row : sheet) {
                                rowIdx++;
                                if ((maxRows >= 0) && (rowIdx > maxRows)) {
                                    break;
                                }
                                if (row == null) {
                                    continue;
                                }
                                short firstCol = row.getFirstCellNum();
                                for (short col = firstCol;
                                        col < row.getLastCellNum(); col++) {
                                    Cell cell = row.getCell(col);
                                    if (cell == null) {
                                        break;
                                    }
				    String value = getCellValue(cell);
                                    if (col > firstCol) {
                                        pw.print(",");
                                    }
                                    pw.print(clean(value));
                                }
                                pw.println("");
                            }

                            break;
                        }

                        pw.flush();
                        pw.close();
                        wb.close();
                        is.close();
                    } catch (Exception exc) {
                        System.err.println("Error converting xls:" +path+"\nError:" + exc);
                        exc.printStackTrace();
                    }
                }
            });

            return pis;
        } catch (Exception exc) {
            throw new RuntimeException(exc);

        }
    }

    public static String getCellValue(Cell cell) {
	if(cell==null) return "";
	if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
	    Date date = cell.getDateCellValue();
	    synchronized(sdf) {
		return  sdf.format(date);
	    }
	} else {
	    return cell.getStringCellValue();
	}
    }

    public static void explodeXls(final IO.Path path, File directory) throws Exception {
	InputStream is = new BufferedInputStream(IO.getInputStream(path.getPath(), XlsUtil.class));
	Workbook wb = StreamingReader.builder()
	    //                      .rowCacheSize(100)    
	    //                      .bufferSize(4096)     
	    .open(is);
	for (Sheet sheet : wb) {
	    String name = Utils.makeID(sheet.getSheetName())+".csv";
	    FileOutputStream fos = new FileOutputStream(IOUtil.joinDir(directory, name));
	    PrintWriter          pw  = new PrintWriter(fos);
	    int rowIdx = 0;
	    for (Row row : sheet) {
		rowIdx++;
		if (row == null) {
		    continue;
		}
		short firstCol = row.getFirstCellNum();
		for (short col = firstCol;
		     col < row.getLastCellNum(); col++) {
		    Cell cell = row.getCell(col);
		    String value = getCellValue(cell);
		    if (col > firstCol) {
			pw.print(",");
		    }
		    pw.print(clean(value));
		}
		pw.println("");
	    }
	    pw.flush();
	    pw.close();
	    fos.close();
	}
    }



    /**
     *
     * @param filename _more_
     * @param maxRows _more_
      * @return _more_
     */
    public static InputStream OLD_xlsxToCsv(final String filename,
                                            int maxRows) {
        try {
            final PipedOutputStream pos = new PipedOutputStream();
            final PipedInputStream  pis = new PipedInputStream(pos);
            final PrintWriter       pw  = new PrintWriter(pos);
            ucar.unidata.util.Misc.run(new Runnable() {
                public void run() {
                    try {
                        System.err.println("start");
                        long         t1   = System.currentTimeMillis();
                        File         file = new File(filename);
                        XSSFWorkbook wb;
                        if (file.exists()) {
                            System.err.println("before:" + file);
                            OPCPackage opcPackage = OPCPackage.open(file,
                                                        PackageAccess.READ);
                            wb = new XSSFWorkbook(opcPackage);
                            System.err.println("opened");
                        } else {
                            InputStream myxls = new BufferedInputStream(
                                                    IO.getInputStream(
                                                        filename,
                                                        XlsUtil.class));
                            wb = new XSSFWorkbook(myxls);
                        }
                        long t2 = System.currentTimeMillis();
                        System.err.println("making wb:" + (t2 - t1) + " ms");
                        XSSFSheet sheet      = wb.getSheetAt(0);
                        boolean   seenNumber = false;
                        System.err.println("rows:" + sheet.getLastRowNum());
                        for (int rowIdx = sheet.getFirstRowNum();
                                rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                            if ((maxRows >= 0) && (rowIdx > maxRows)) {
                                break;
                            }
                            XSSFRow row = sheet.getRow(rowIdx);
                            if (row == null) {
                                continue;
                            }

                            short firstCol = row.getFirstCellNum();
                            for (short col = firstCol;
                                    col < row.getLastCellNum(); col++) {
                                XSSFCell cell = row.getCell(col);
                                if (cell == null) {
                                    break;
                                }
                                String value = cell.toString();
                                if (col > firstCol) {
                                    pw.print(",");
                                }
                                pw.print(clean(value));
                            }
                            pw.print("\n");
                        }

                        pw.flush();
                        pw.close();
                    } catch (Exception exc) {
                        System.err.println("Error converting xls:" +filename+"\nError:" + exc);
                        exc.printStackTrace();
                    }
                }
            });

            return pis;
        } catch (Exception exc) {
            throw new RuntimeException(exc);

        }
    }

    /**
     *
     * @param filename _more_
     *  @return _more_
     */
    public static InputStream xlsToCsv(IO.Path path) {
        return xlsToCsv(path, -1,0);
    }

    /**
     *
     * @param maxRows _more_
     * @return _more_
     */
    public static InputStream xlsToCsv(final IO.Path path, final int maxRows,final int sheetNumber) {
        try {
            final PipedOutputStream pos = new PipedOutputStream();
            final PipedInputStream  pis = new PipedInputStream(pos);
            final PrintWriter       pw  = new PrintWriter(pos);
            ucar.unidata.util.Misc.run(new Runnable() {
                public void run() {
                    try {
                        InputStream myxls = IO.getInputStream(path.getPath(),
                                                XlsUtil.class);
                        HSSFWorkbook wb         = new HSSFWorkbook(myxls);
                        HSSFSheet    sheet      = wb.getSheetAt(sheetNumber);
                        boolean      seenNumber = false;
                        for (int rowIdx = sheet.getFirstRowNum();
                                rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                            if ((maxRows >= 0) && (rowIdx > maxRows)) {
                                break;
                            }
                            HSSFRow row = sheet.getRow(rowIdx);
                            if (row == null) {
                                continue;
                            }
                            short firstCol = row.getFirstCellNum();
                            for (short col = firstCol;
                                    col < row.getLastCellNum(); col++) {
                                HSSFCell cell = row.getCell(col);
                                if (cell == null) {
                                    break;
                                }
                                String value = cell.toString();
                                if (col > firstCol) {
                                    pw.print(",");
                                }
				value = clean(value);
                                pw.print(value);
                            }
                            pw.print("\n");
                        }
                        pw.flush();
                        pw.close();
                    } catch (Exception exc) {
                        System.err.println("Error converting xls:" +path+"\nError:" + exc);
                        exc.printStackTrace();
                    }
                }
            });

            return pis;
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
	if(true) return Seesv.cleanColumnValue(s);
        while (s.endsWith(",")) {
            s = s.substring(0, s.length() - 1);
        }
        if ((s.indexOf(",") >= 0) || (s.indexOf("\n") >= 0)) {
            s = "\"" + s + "\"";
        }

        return s;
    }

    


    /**
       via gpt
     */
    public static void extractImages(String file) throws IOException {
        File pptFile = new File(file);
        XMLSlideShow ppt = new XMLSlideShow(new FileInputStream(pptFile));
        // get the first slide
        XSLFSlide slide = ppt.getSlides().get(0);

        // iterate over the shapes on the slide
        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFPictureShape) {
                XSLFPictureShape picture = (XSLFPictureShape) shape;
                PictureData pictureData = picture.getPictureData();
                if (pictureData instanceof XSLFPictureData) {
                    XSLFPictureData xslfPictureData = (XSLFPictureData) pictureData;

                    // create a BufferedImage from the picture data
                    BufferedImage image = ImageIO.read(xslfPictureData.getPackagePart().getInputStream());

                    // save the image to a file
                    File outputFile = new File("output.png");
                    ImageIO.write(image, "png", outputFile);
                }
            }
        }
        ppt.close();
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
	    explodeXls(new IO.Path(arg),new File("."));
	    /*
            String csv = null;
            for (int i = 0; i < 10; i++) {
                long t1 = System.currentTimeMillis();
                if (arg.endsWith(".xlsx")) {
                    csv = IO.readInputStream(xlsxToCsv(new IO.Path(arg), -50,1));
                } else {
                    csv = IO.readInputStream(xlsToCsv(new IO.Path(arg)));
                }
		System.err.println(csv);
                long t2 = System.currentTimeMillis();
                Utils.printTimes("read", t1, t2);
            }
            String newFile = IOUtil.stripExtension(arg) + ".csv";
            IOUtil.writeFile(newFile, csv);
	    */
        }
    }



}
