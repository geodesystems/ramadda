/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.cdmdata;


import org.ramadda.util.text.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Pattern;


/**
 * A manager for netCDF-Java CDM data
 */
public class CsvPointWriter extends DataSink {

    /**  */
    private Row header = null;

    /**
     *
     */
    public CsvPointWriter() {}

    /**
     *
     * @param csvUtil _more_
     * @param arg _more_
     *  @return _more_
     */
    public boolean canHandle(CsvUtil csvUtil, String arg) {
        return arg.equals("-tonc");
    }


    /**
     *
     * @param csvUtil _more_
     * @param args _more_
     * @param index _more_
     *  @return _more_
     */
    public int processArgs(CsvUtil csvUtil, List<String> args, int index) {
        return index;
    }


    /**
     * _more_
     *
     *
     * @param ctx _more_
     * @param row _more_
     *  @return _more_
     * @throws Exception _more_
     */
    public Row processRow(TextReader ctx, Row row) throws Exception {
        if (header == null) {}
        System.err.println("processRow:" + row);

        return row;
    }


    /**
     * _more_
     *
     *
     * @param ctx _more_
     * @param rows _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    public List<Row> finish(TextReader ctx, List<Row> rows) throws Exception {
        return rows;
    }



}
