/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.docs;


import com.monitorjbl.xlsx.StreamingReader;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.*;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.output.*;
import org.ramadda.service.*;
import org.ramadda.util.FileInfo;
import org.ramadda.util.GoogleChart;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.NamedInputStream;
import org.ramadda.util.Utils;
import org.ramadda.util.seesv.Seesv;
import org.ramadda.util.seesv.DataProvider;
import org.ramadda.util.seesv.Filter;
import org.ramadda.util.seesv.Processor;
import org.ramadda.util.seesv.SearchField;
import org.ramadda.util.seesv.TextReader;

import org.w3c.dom.*;


import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.image.*;

import java.io.*;

import java.net.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import java.util.zip.*;



/**
 */
@SuppressWarnings("unchecked")
public class TabularOutputHandler extends OutputHandler {

    /** _more_ */
    public static final int MAX_ROWS = 100;

    /** _more_ */
    public static final int MAX_COLS = 100;

    /** _more_ */
    public static final OutputType OUTPUT_XLS_HTML =
        new OutputType("Show Spreadsheet", "xls_html", OutputType.TYPE_VIEW,
                       "", "fa-file-excel");


    /**
     * _more_
     */
    public TabularOutputHandler() {}

    /**
     * _more_
     *
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public TabularOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_XLS_HTML);
    }





    /**
     * _more_
     *
     * @param request _more_
     * @param state _more_
     * @param links _more_
     *
     *
     * @throws Exception _more_
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
        Entry entry = state.getEntry();
        if (entry == null) {
            return;
        }
        boolean isTabular = isTabular(entry);
        if ( !isTabular) {
            if ( !entry.isFile()) {
                return;
            }
            String path = entry.getResource().getPath();
            if (path.endsWith(".xls") || path.endsWith(".xlsx")
                    || path.endsWith(".csv")) {
                isTabular = true;
            }
        }
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param s _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result makeHtmlResult(Request request, String s)
            throws Exception {
        s = new String(Utils.encodeBase64(s));
        s = JsonUtil.mapAndQuote(Utils.makeListFromValues("html", s));

        return new Result(s, "application/json");
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param f _more_
     *
     * @throws Exception _more_
     */
    private void addFileLink(Request request, StringBuffer sb, File f)
            throws Exception {
        String id = getEntryManager().getProcessFileTypeHandler().getSynthId(
                        getEntryManager().getProcessEntry(),
                        getStorageManager().getProcessDir().toString(), f);
        String url = HtmlUtils.url(
                         request.getAbsoluteUrl(
                             getRepository().URL_ENTRY_SHOW), ARG_ENTRYID,
                                 id);
        sb.append(HtmlUtils.href(url, f.getName(), "target=_output"));
    }





    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param harvester _more_
     * @param args _more_
     * @param buffer _more_
     * @param files _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    public boolean processCommandView(final org.ramadda.repository.harvester
            .CommandHarvester.CommandRequest request, final Entry entry,
                final org.ramadda.repository.harvester
                    .CommandHarvester harvester, final List<String> args,
                        final Appendable buffer, final List<FileInfo> files)
            throws Exception {

        if (args.contains("-help") || args.contains("?")) {
            buffer.append(
                "For tabular data:\n\t-maxrows <# rows to show>\n\t-columns <comma separated columns to show e.g., 1,3,4,6> \n\t-startcol col# -endcol col# \n\t-startrow row # -endrow row #\n");

            return false;
        }



        final boolean justHeader = args.contains("-header");
        final boolean doText     = args.contains("-text");
        final boolean doFile     = args.contains("-file");
        final boolean doImage    = args.contains("-image");
        final boolean doHtml     = !doText && !doFile && !doImage;
        List<String> columnsArg = StringUtil.split(Utils.getArg("-columns",
                                      args, ""), ",", true, true);
        final List<Integer> selectedColumns = (columnsArg.size() > 0)
                ? new ArrayList<Integer>()
                : null;


        //User indexes are 1 based
        if (columnsArg.size() > 0) {
            for (String col : columnsArg) {
                if (col.indexOf("-") >= 0) {
                    List<String> toks = StringUtil.split(col, "-", true,
                                            true);
                    if (toks.size() == 2) {
                        int start = Integer.parseInt(toks.get(0));
                        int end   = Integer.parseInt(toks.get(1));
                        for (int i = start; i <= end; i++) {
                            selectedColumns.add(Integer.valueOf(i - 1));
                        }
                    }
                } else {
                    selectedColumns.add(Integer.valueOf(Integer.parseInt(col)
                            - 1));
                }
            }
        }


        final int startCol = Math.max(0, Utils.getArg("-startcol", args, 1)
                                      - 1);
        final int endCol  = Utils.getArg("-endcol", args, 1000) - 1;
        final int maxCols = Utils.getArg("-maxcols", args, 1000);
        final int maxRows = Utils.getArg("-maxrows", args, 50);
        final int startRow = Math.max(0, Utils.getArg("-startrow", args, 1)
                                      - 1);
        final int endRow = Utils.getArg("-endrow", args, startRow + 1000) - 1;
        //        System.err.println ("max rows:" + maxRows + "  startRow: " + startRow +" end row:" + endRow);
        final StringBuilder html           = new StringBuilder("");
        final StringBuilder sb             = new StringBuilder("");
        final String        colDelimiter   = doFile
                                             ? ","
                                             : " | ";

        TabularVisitor      tabularVisitor = new TabularVisitor() {
            private List<Integer> dfltCols = new ArrayList<Integer>();
            @Override
            public boolean visit(TextReader info, String sheet,
                                 List<List<Object>> rows) {
                try {
                    return visitInner(info, sheet, rows);
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }
            }

            private boolean visitInner(TextReader info, String sheet,
                                       List<List<Object>> rows)
                    throws Exception {

                int padMaxCols = 1;
                for (List<Object> cols : rows) {
                    padMaxCols = Math.max(cols.size(), padMaxCols);
                }
                for (List<Object> cols : rows) {
                    while (cols.size() < padMaxCols) {
                        cols.add("");
                    }
                }

                List<List<Object>> rowsToUse = new ArrayList<List<Object>>();

                int                rowCnt    = 0;
                for (int rowIdx = startRow;
                        (rowIdx < rows.size()) && (rowIdx <= endRow);
                        rowIdx++) {
                    if (rowCnt++ > maxRows) {
                        break;
                    }

                    List<Object>  cols   = rows.get(rowIdx);
                    int           colCnt = 0;
                    StringBuilder lineSB = new StringBuilder();

                    List<Object>  tmp    = new ArrayList<Object>();
                    dfltCols = new ArrayList<Integer>();
                    for (int colIdx = startCol;
                            (colIdx < cols.size()) && (colIdx <= endCol);
                            colIdx++) {
                        if (colCnt++ > maxCols) {
                            break;
                        }
                        tmp.add(cols.get(colIdx));
                        dfltCols.add(Integer.valueOf(colIdx));
                    }
                    rowsToUse.add(tmp);
                }

                harvester.displayTabularData(request, entry, args, files,
                                             rowsToUse);

                return false;
            }
        };

        TextReader info = new TextReader();
        info.setSkipRows(0);
        info.setMaxRows(100);
        for (String s : args) {
            if (s.matches("(<|<=|>|>=|=|<>|!=)")) {
                info.addSearchExpression(s);
            }
        }
        visit(request.getRequest(), entry, info, tabularVisitor);



        if (doImage) {
            File imageFile = getRepository().getStorageManager().getTmpFile(entry.getName() + "_table.png");

            Font font = new Font("Dialog", Font.PLAIN, 12);
            Image image = ImageUtils.renderHtml(html.toString(), 1200, null,
                              font);
            ImageUtils.writeImageToFile(image, imageFile);
            FileInfo fileInfo = new FileInfo(imageFile);

            if ( !entry.getTypeHandler().isWikiText(entry.getDescription())) {
                fileInfo.setDescription(entry.getDescription());
            }
            fileInfo.setTitle("Table - " + entry.getName());
            files.add(fileInfo);
        }

        if (doFile) {
            File csvFile = getRepository().getStorageManager().getTmpFile(IOUtil.stripExtension(entry.getName())
                               + ".csv");
            IOUtil.writeFile(csvFile.toString(), sb.toString());
            FileInfo fileInfo = new FileInfo(csvFile);
            if ( !entry.getTypeHandler().isWikiText(entry.getDescription())) {
                fileInfo.setDescription(entry.getDescription());
            }
            fileInfo.setTitle("Table - "
                              + IOUtil.stripExtension(entry.getName())
                              + ".csv");
            files.add(fileInfo);
        }

        return true;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private int getSkipRows(Request request, Entry entry) throws Exception {
        return (int) request.get("table.skiprows", 0);
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String getDelimiter(Entry entry) throws Exception {
        if (isTabular(entry)) {
            TabularTypeHandler tth =
                (TabularTypeHandler) entry.getTypeHandler();

            return tth.getDelimiter(entry);
        }

        return null;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private int getRowCount(Request request, Entry entry, int dflt)
            throws Exception {
        int v = (int) request.get("table.rows", dflt);

        return v;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param visitInfo _more_
     * @param visitor _more_
     *
     * @throws Exception _more_
     */
    public void visit(Request request, Entry entry, TextReader visitInfo,
                      TabularVisitor visitor)
            throws Exception {

        File file = entry.getFile();
        //        System.err.println("visit:" + visitInfo);
        InputStream inputStream = null;
        String      suffix      = "";

        if ((file != null) && file.exists()) {
            if (file.toString().toLowerCase().endsWith(".zip")) {
                InputStream    fis = IO.getInputStream(file.toString());
                ZipInputStream zin = new ZipInputStream(fis);
                ZipEntry       ze  = null;
                while ((ze = zin.getNextEntry()) != null) {
                    if (ze.isDirectory()) {
                        continue;
                    }
                    String p = ze.getName().toLowerCase();
                    if (p.endsWith(".csv") || p.endsWith(".tsv")) {
                        inputStream = zin;
                        suffix      = "csv";

                        break;
                    }
                    //Apple health
                    if (p.equals("export.xml")) {
                        inputStream = zin;
                        suffix      = "xml";

                        break;
                    }
                }
            }


            if (inputStream == null) {
                inputStream = new BufferedInputStream(
                    getStorageManager().getFileInputStream(file));
                suffix =
                    IOUtil.getFileExtension(file.toString()).toLowerCase();
            }
	    /*
            if (suffix.equals(".xlsx") || suffix.equals(".xls")) {
                if (file.length() > 10 * 1000000) {
                    throw new IllegalArgumentException("File too big");
                }
            }
	    */

        }

        if (suffix.equals(".xlsx") || suffix.equals(".xls")) {
            //            System.err.println ("Visit xls");
            visitXls(request, entry, suffix, inputStream, visitInfo, visitor);
        } else if (suffix.endsWith(".csv") || suffix.endsWith(".txt")) {
            visitCsv(request, entry, inputStream, visitInfo, visitor);
        } else {
            if (isTabular(entry)) {
                TabularTypeHandler tth =
                    (TabularTypeHandler) entry.getTypeHandler();
                //                System.err.println ("Visit tabular");
                tth.visit(request, entry, inputStream, visitInfo, visitor);
            } else {
                throw new IllegalStateException("Unknown file type:"
                        + suffix);
            }
        }
        IOUtil.close(inputStream);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param inputStream _more_
     * @param textReader _more_
     * @param visitor _more_
     *
     * @throws Exception _more_
     */
    public void visitCsv(Request request, Entry entry,
                         InputStream inputStream,
                         final TextReader textReader, TabularVisitor visitor)
            throws Exception {
        BufferedReader br =
            new BufferedReader(new InputStreamReader(inputStream));
        final List<List<Object>> rows = new ArrayList<List<Object>>();

        ByteArrayOutputStream    bos  = new ByteArrayOutputStream();

        textReader.setInput(new NamedInputStream("input",
                new BufferedInputStream(inputStream)));
        textReader.setOutput(bos);
        textReader.addProcessor(new Processor() {
            @Override
            public org.ramadda.util.seesv.Row processRow(
                    TextReader textReader, org.ramadda.util.seesv.Row row)
                    throws Exception {
                //                System.err.println("TabularOutputHandler.processRow:" + line);
                List obj = new ArrayList();
                obj.addAll(row.getValues());
                rows.add((List<Object>) obj);

                return row;
            }
        });

        if (textReader.getSearchFields() != null) {
            for (SearchField searchField : textReader.getSearchFields()) {
                String id = "table." + searchField.getName();
                if (request.defined(id)) {
                    //Columns are 1 based to the user
                    if (searchField.getName().startsWith("column")) {
                        int column = Integer.parseInt(
                                         searchField.getName().substring(
                                             "column".length()).trim()) - 1;
                        List<String> cols = new ArrayList<String>();
                        cols.add(column + "");
                        String s = request.getString(id, "");
                        s = s.trim();
                        //                        System.err.println("column:" + column + " s:" + s);
                        String operator = StringUtil.findPattern(s,
                                              "^([<>=]+).*");
                        if (operator != null) {
                            System.err.println("operator:" + operator);
                            s = s.replace(operator, "").trim();
                            double value = Double.parseDouble(s);
                            int op = Filter.ValueFilter.getOperator(operator);
                            textReader.addProcessor(
						    new Filter.ValueFilter(textReader, cols, op, value));
                            continue;
                        }
                        textReader.addProcessor(
						new Filter.PatternFilter(textReader,
                                column, request.getString(id, "")));

                    }
                }

            }
        }

        String searchText = request.getString("table.text", (String) null);
        if (Utils.stringDefined(searchText)) {
            //match all
            textReader.addProcessor(new Filter.PatternFilter(textReader,-1,
                    "(?i:.*" + searchText + ".*)"));
        }
        Seesv csvUtil = new Seesv(new ArrayList<String>());
        csvUtil.process(textReader);
        visitor.visit(textReader, entry.getName(), rows);
    }



    /**
     * _more_
     *
     * @param suffix _more_
     * @param inputStream _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private static Workbook makeWorkbook(String suffix,
                                         InputStream inputStream)
            throws Exception {
	suffix = suffix.toLowerCase();
        if(suffix.equals(".xls"))
	   return  new HSSFWorkbook(inputStream);
	return  StreamingReader.builder()
	    //			    .rowCacheSize(100)    
	    //			    .bufferSize(4096)     
	    .open(inputStream);  
	//	   : new XSSFWorkbook(inputStream));

    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param suffix _more_
     * @param inputStream _more_
     * @param visitInfo _more_
     * @param visitor _more_
     *
     * @throws Exception _more_
     */
    private void visitXls(Request request, Entry entry, String suffix,
                          InputStream inputStream, TextReader visitInfo,
                          TabularVisitor visitor)
            throws Exception {
        //        System.err.println("visitXls: making workbook");
        Workbook wb = makeWorkbook(suffix, inputStream);
        //        System.err.println("visitXls:" + visitInfo.getSkip() + " max rows:" + visitInfo.getMaxRows()+ " #sheets:" + wb.getNumberOfSheets());
        int maxRows = visitInfo.getMaxRows();
        for (int sheetIdx = 0; sheetIdx < wb.getNumberOfSheets();
                sheetIdx++) {
            if ( !visitInfo.okToShowSheet(sheetIdx)) {
                continue;
            }
            Sheet sheet = wb.getSheetAt(sheetIdx);
            //            System.err.println("\tsheet:" + sheet.getSheetName() + " #rows:" + sheet.getLastRowNum());
            List<List<Object>> rows      = new ArrayList<List<Object>>();
            int                sheetSkip = visitInfo.getSkipRows();
	    int rowIdx=0;
	    for (Row row : sheet) {
		rowIdx++;
		if(rows.size() > maxRows) break;
                if (sheetSkip-- > 0) {
                    continue;
                }

                if (row == null) {
                    continue;
                }
                List<Object> cols     = new ArrayList<Object>();
                short        firstCol = row.getFirstCellNum();
                for (short col = firstCol;
                        (col < MAX_COLS) && (col < row.getLastCellNum());
                        col++) {
                    Cell cell = row.getCell(col);
                    if (cell == null) {
                        break;
                    }
                    Object   value = null;
                    CellType type  = cell.getCellType();
                    if (type == CellType.NUMERIC) {
                        value = Double.valueOf(cell.getNumericCellValue());
                    } else if (type == CellType.BOOLEAN) {
                        value = Boolean.valueOf(cell.getBooleanCellValue());
                    } else if (type == CellType.ERROR) {
                        value = "" + cell.getErrorCellValue();
                    } else if (type == CellType.BLANK) {
                        value = "";
                    } else if (type == CellType.FORMULA) {
                        value = cell.getCellFormula();
                    } else {
                        value = cell.getStringCellValue();
                    }
                    cols.add(value);
                }

                /**
                 * ** TODO
                 * org.ramadda.util.seesv.Row row = new Row(cols);
                 *
                 * if ( !visitInfo.rowOk(row)) {
                 *   if (rows.size() == 0) {
                 *       //todo: check for the header line
                 *   } else {
                 *       continue;
                 *   }
                 * }
                 */
                rows.add(cols);
	    }
	    wb.close();
            IOUtil.close(inputStream);
            if ( !visitor.visit(visitInfo, sheet.getSheetName(), rows)) {
                break;
            }
        }
    }



    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public static boolean isTabular(Entry entry) {
        if (entry == null) {
            return false;
        }

        return entry.getTypeHandler().isType(TabularTypeHandler.TYPE_TABULAR);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param service _more_
     * @param input _more_
     * @param args _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean csv(Request request, Service service, ServiceInput input,
                       List args)
            throws Exception {
        return true;
    }

    /*
      Entry entry = null;
      for (Entry e : input.getEntries()) {
      if (isTabular(e)) {
      entry = e;

      break;
      }
      }
      if (entry == null) {
      throw new IllegalArgumentException("No tabular entry found");
      }

      HashSet<Integer> sheetsToShow = getSheetsToShow((String) args.get(0));
      String name = getStorageManager().getFileTail(entry);
      if ( !Utils.stringDefined(name)) {
      name = entry.getName();
      }
      name = IOUtil.stripExtension(name);

      File newFile = new File(IOUtil.joinDir(input.getProcessDir(),
      name + ".csv"));

      String file = "";
      InputStream inputStream = new BufferedInputStream(
      getStorageManager().getFileInputStream(file));
      final TextReader info =
      new TextReader(new BufferedInputStream(inputStream), new FileOutputStream(newFile));


      TabularVisitor visitor = new TabularVisitor() {
      public boolean visit(TabularVisitInfo info, String sheetName,
      List<List<Object>> rows) {
      return true;
      }
      };

      TabularVisitInfo visitInfo =
      new TabularVisitInfo(
      request, entry, getSkipRows(request, entry),
      getRowCount(request, entry, Integer.MAX_VALUE), sheetsToShow);


      visit(request, entry, visitInfo, visitor);


      return true;

      }
    */

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        Workbook wb = makeWorkbook(IOUtil.getFileExtension(args[0]),
                                   new FileInputStream(args[0]));
        for (int sheetIdx = 0; sheetIdx < wb.getNumberOfSheets();
                sheetIdx++) {
            Sheet sheet = wb.getSheetAt(sheetIdx);
            System.err.println(sheet.getSheetName());
            for (int rowIdx = sheet.getFirstRowNum();
                    rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) {
                    continue;
                }
                short firstCol = row.getFirstCellNum();
                int   colCnt   = 0;
                for (short col = firstCol; col < row.getLastCellNum();
                        col++) {
                    Cell cell = row.getCell(col);
                    if (cell == null) {
                        break;
                    }
                    Object   value = null;
                    CellType type  = cell.getCellType();
                    if (type == CellType.NUMERIC) {
                        value = Double.valueOf(cell.getNumericCellValue());
                    } else if (type == CellType.BOOLEAN) {
                        value = Boolean.valueOf(cell.getBooleanCellValue());
                    } else if (type == CellType.ERROR) {
                        value = "" + cell.getErrorCellValue();
                    } else if (type == CellType.BLANK) {
                        value = "";
                    } else if (type == CellType.FORMULA) {
                        value = cell.getCellFormula();
                    } else {
                        value = cell.getStringCellValue();
                    }
                    if (colCnt++ > 0) {
                        System.out.print(",");
                    }
                    System.out.print(value);
                }
                System.out.println("");
            }
        }
    }



}
