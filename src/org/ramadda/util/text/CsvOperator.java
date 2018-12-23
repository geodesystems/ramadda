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

package org.ramadda.util.text;


import org.ramadda.util.GeoUtils;


import org.ramadda.util.Utils;


import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;

import java.text.DateFormat;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import java.util.regex.*;



/**
 * Class description
 *
 *
 * @version        $version$, Fri, Jan 9, '15
 * @author         Jeff McWhirter
 */
public abstract class CsvOperator {

    /** _more_ */
    protected int rowCnt = 0;

    /** _more_ */
    public static final int INDEX_ALL = -9999;

    /** _more_ */
    protected int index = -1;

    /** _more_ */
    List<String> sindices;

    /** _more_ */
    List<Integer> indices;

    /** _more_ */
    HashSet<Integer> indexMap;

    /** _more_          */
    HashSet<Integer> colsSeen = new HashSet<Integer>();

    /** _more_ */
    private List header;

    /** _more_ */
    private Hashtable<String, Integer> columnMap;

    /** _more_ */
    private List<String> columnNames;


    /** _more_ */
    private String scol;

    /**
     * _more_
     */
    public CsvOperator() {}

    /**
     * _more_
     *
     * @param col _more_
     */
    public CsvOperator(String col) {
        sindices = new ArrayList<String>();
        sindices.add(col);
        scol = col;
    }

    /**
     * _more_
     *
     * @param cols _more_
     */
    public CsvOperator(List<String> cols) {
        this.sindices = cols;
    }


    /**
     * _more_
     *
     * @param info _more_
     * @param row _more_
     * @param line _more_
     *
     * @throws Exception _more_
     */
    public void processFirstRow(TextReader info, Row row, String line)
            throws Exception {}

    /**
     * _more_
     *
     * @param header _more_
     */
    public void setHeader(List header) {
        this.header = header;
    }


    /**
     * _more_
     *
     * @param info _more_
     *
     * @return _more_
     */
    public int getIndex(TextReader info) {
        return getIndices(info).get(0);
    }



    /**
     * _more_
     *
     *
     * @param indices _more_
     * @param s _more_
     *
     */
    public void getColumnIndex(List<Integer> indices, String s) {
        s = s.toLowerCase();
        List<String> toks  = StringUtil.splitUpTo(s, "-", 2);
        int          start = -1;
        int          end   = -1;
        try {
            if (toks.size() == 1) {
                start = end = Integer.parseInt(s);
            } else {
                start = Integer.parseInt(toks.get(0));
                end   = Integer.parseInt(toks.get(1));
            }
        } catch (NumberFormatException exc) {
            if (columnNames == null) {
                if (header == null) {
                    return;
                }
                columnMap = new Hashtable<String, Integer>();
                for (int i = 0; i < header.size(); i++) {
                    String colName = (String) header.get(i);
                    columnMap.put(colName, i);
                    columnMap.put(colName.toLowerCase(), i);
                }
            }
            if (toks.size() == 1) {
                String tok = toks.get(0);
                if (tok.equals("*")) {
                    for (int i = 0; i < header.size(); i++) {
                        if ( !colsSeen.contains(i)) {
                            colsSeen.add(i);
                            indices.add(i);
                        }
                    }

                    return;
                }
                Integer iv = columnMap.get(tok);
                if (iv != null) {
                    start = end = iv;
                }
            } else {
                Integer iv1 = columnMap.get(toks.get(0));
                Integer iv2 = columnMap.get(toks.get(1));
                if ((iv1 != null) && (iv2 != null)) {
                    start = iv1;
                    end   = iv2;
                }
            }
        }
        if (start >= 0) {
            for (int i = start; i <= end; i++) {
                colsSeen.add(i);
                indices.add(i);
            }
        }

        /*
        for (int i = 0; i < columnNames.size(); i++) {
            String v = columnNames.get(i);
            if (v.startsWith(s)) {
                columnMap.put(v, i);
                indices.add(i);
                return;
            }
            }*/
    }


    /**
     * _more_
     *
     * @param info _more_
     *
     * @return _more_
     */
    public List<Integer> getIndices(TextReader info) {
        if (indices == null) {
            indices = new ArrayList<Integer>();
            for (String s : sindices) {
                getColumnIndex(indices, s);
            }
        }

        return indices;
    }


    /**
     * _more_
     *
     * @param info _more_
     * @param row _more_
     *
     * @return _more_
     */
    public Row filterValues(TextReader info, Row row) {
        row.setValues(filterValues(info, row.getValues()));

        return row;
    }

    /**
     * _more_
     *
     * @param info _more_
     * @param values _more_
     *
     * @return _more_
     */
    public List filterValues(TextReader info, List values) {
        List             newValues = new ArrayList();
        HashSet<Integer> indexMap  = getIndexMap(info);
        for (int i = 0; i < values.size(); i++) {
            if ( !indexMap.contains(i)) {
                newValues.add(values.get(i));
            }
        }

        return newValues;
    }


    /**
     * _more_
     *
     * @param info _more_
     *
     * @return _more_
     */
    public HashSet<Integer> getIndexMap(TextReader info) {
        if (indexMap == null) {
            List<Integer> indices = getIndices(info);
            indexMap = new HashSet();
            for (Integer i : indices) {
                indexMap.add(i);
            }
        }

        return indexMap;
    }





}
