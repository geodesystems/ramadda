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
    private Row header = null;

    public CsvPointWriter() {
    }

    public  boolean canHandle(CsvUtil csvUtil, String arg){
	return arg.equals("-tonc");
    }


    public int processArgs(CsvUtil csvUtil, List<String> args, int index) {
	return index;
    }


    /**
     * _more_
    *
     * @param info _more_
     * @param row _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Row processRow(TextReader ctx, Row row) throws Exception {
	if(header == null) {

	}
	System.err.println("processRow:" +row);
        return row;
    }


    /**
     * _more_
     *
     * @param info _more_
     * @param rows _more_
     *
     *
     * @return _more_
     * @throws Exception On badness
     */
    public List<Row> finish(TextReader ctx, List<Row> rows)
	throws Exception {
        return rows;
    }



}
