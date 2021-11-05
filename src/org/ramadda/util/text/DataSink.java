/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.text;


import java.util.List;


/**
 *
 * @author Jeff McWhirter
 */

public abstract class DataSink extends Processor implements Cloneable,
        CsvPlugin {

    /**
     * _more_
     */
    public DataSink() {}

    /**
     *
     * @param csvUtil _more_
     * @param arg _more_
     *
     * @return _more_
     */
    public abstract boolean canHandle(CsvUtil csvUtil, String arg);


    /**
     *
     * @param csvUtil _more_
     * @param args _more_
     * @param index _more_
     *
     * @return _more_
     */
    public int processArgs(CsvUtil csvUtil, List<String> args, int index) {
        return index;
    }


    /**
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public DataSink cloneMe() throws Exception {
        return (DataSink) this.clone();
    }

    /**
     * _more_
     *
     * @param ctx _more_
     * @param row _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Row processRow(TextReader ctx, Row row) throws Exception {
        return row;
    }


    /**
     * _more_
     *
     * @param ctx _more_
     * @param rows _more_
     *
     *
     * @return _more_
     * @throws Exception On badness
     */
    public List<Row> finish(TextReader ctx, List<Row> rows) throws Exception {
        return rows;
    }


}
