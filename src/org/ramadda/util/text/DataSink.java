/*
 * Copyright (c) 2008-2021 Geode Systems LLC
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

import java.util.List;

/**
 *
 * @author Jeff McWhirter
 */

public abstract class DataSink extends Processor implements Cloneable, CsvPlugin  {

    /**
     * _more_
     */
    public DataSink() {}

    public abstract boolean canHandle(CsvUtil csvUtil, String arg);


    public int processArgs(CsvUtil csvUtil, List<String> args, int index) {
	return index;
    }


    public DataSink cloneMe() throws Exception {
	return (DataSink)this.clone();
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
    public Row processRow(TextReader info, Row row) throws Exception {
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
    public List<Row> finish(TextReader info, List<Row> rows)
	throws Exception {
        return rows;
    }


}
