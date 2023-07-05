/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.docs;


import org.ramadda.util.IO;

import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;


import org.ramadda.repository.*;
import org.ramadda.util.text.Seesv;

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

    /** _more_ */
    private Entry entry;

    /** _more_ */
    private List<String> commands;

    private ConvertibleTypeHandler cth;

    private Request request;

    /**
     * ctor
     */
    public ConvertibleFile() {}


    /**
     * ctor
     *
     *
     *
     *
     * @param cth _more_
     * @param entry _more_
     * @param commands _more_
     *
     * @throws IOException on badness
     */
    public ConvertibleFile(Request request,ConvertibleTypeHandler cth, Entry entry,
                           List<String> commands, IO.Path path)
            throws IOException {
        super(path, cth, null);
	this.cth = cth;
	this.request = request;
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

    @Override
    public List<String>  preprocessCsvCommands(List<String>  commands) throws Exception {
	return  cth.preprocessCsvCommands(request, commands);
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
    public void runSeesv(Seesv csvUtil, boolean buffered)
            throws Exception {
	List<IO.Path> files = null;
        if (entry.getResource().hasResource()) {
            files = new ArrayList<IO.Path>();
	    files.add(getPath());
	    //            files.add(new IO.Path(entry.getTypeHandler().getStorageManager().getEntryFile(entry).toString()));
        }
        csvUtil.run(files);
    }




}
