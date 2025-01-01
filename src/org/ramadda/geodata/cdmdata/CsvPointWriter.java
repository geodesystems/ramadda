/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.cdmdata;


import org.ramadda.util.Utils;
import org.ramadda.util.seesv.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.Dictionary;
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

    /**  */
    private Dictionary<String, String> props;

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
    public boolean canHandle(Seesv csvUtil, String arg) {
        return arg.equals("-tonc");
    }


    /**
     *
     * @param csvUtil _more_
     * @param args _more_
     * @param index _more_
     *  @return _more_
     */
    public int processArgs(Seesv csvUtil, List<String> args, int index) {
        if (index >= args.size() - 2) {
            throw new IllegalArgumentException(
                "Usage: -tonc \"name value\" ");
        }
        props = csvUtil.parseProps(args.get(++index));

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
        System.err.println("CsvPointWriter.processRow:" + row);

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
    @Override
    public void finish(TextReader ctx) throws Exception {
        System.err.println("CsvPointWriter.finish");
    }



}
