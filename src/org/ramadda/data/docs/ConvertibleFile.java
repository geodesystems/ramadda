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

package org.ramadda.data.docs;


import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;


import org.ramadda.repository.*;
import org.ramadda.util.text.CsvUtil;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import java.io.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;


/**
 *
 */
public class ConvertibleFile extends CsvFile {

    /** _more_          */
    private Entry entry;

    /** _more_          */
    private List<String> commands;

    /**
     * ctor
     */
    public ConvertibleFile() {}


    /**
     * ctor
     *
     *
     *
     * @param entry _more_
     * @param commands _more_
     * @param filename _more_
     *
     * @throws IOException on badness
     */
    public ConvertibleFile(Entry entry, List<String> commands,
                           String filename)
            throws IOException {
        super(filename);
        this.entry    = entry;
        this.commands = commands;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public List<String> getCsvCommands() {
        return commands;
    }

    /**
     * _more_
     *
     * @param csvUtil _more_
     * @param buffered _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void runCsvUtil(CsvUtil csvUtil, boolean buffered)
            throws Exception {
        List<String> files = null;
        if (entry.getResource().hasResource()) {
            files = new ArrayList<String>();
            files.add(entry.getResource().getPath());
        }
        csvUtil.run(files);
    }




}
