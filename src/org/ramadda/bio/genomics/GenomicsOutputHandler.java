/*
* Copyright (c) 2008-2025 Geode Systems LLC
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

package org.ramadda.bio.genomics;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.output.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import org.ramadda.util.sql.SqlUtil;


import org.w3c.dom.*;

import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.util.WmsUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;


import java.io.File;


import java.net.*;


import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Date;
import java.util.Enumeration;


import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;



import java.util.regex.*;

import java.util.zip.*;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class GenomicsOutputHandler extends OutputHandler {


    /** _more_ */
    public static final OutputType OUTPUT_GENOMICS_TEST1 =
        new OutputType("Genomics Test 1", "genomics_test1",
                       OutputType.TYPE_VIEW, "", "/genomics/dna.png");

    /** _more_ */
    public static final OutputType OUTPUT_GENOMICS_TEST2 =
        new OutputType("Genomics Test 2", "genomics_test2",
                       OutputType.TYPE_VIEW, "", "/genomics/dna.png");


    /**
     * _more_
     */
    public GenomicsOutputHandler() {}

    /**
     * _more_
     *
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public GenomicsOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        //        addType(OUTPUT_GENOMICS_TEST1);
        //        addType(OUTPUT_GENOMICS_TEST2);
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
        if (true) {
            return;
        }
        for (Entry entry : state.getAllEntries()) {
            if (entry.getTypeHandler().isType("bio_genomics")) {
                links.add(makeLink(request, state.getEntry(),
                                   OUTPUT_GENOMICS_TEST1));
                links.add(makeLink(request, state.getEntry(),
                                   OUTPUT_GENOMICS_TEST2));

                return;
            }
        }

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(entry);

        return outputEntries(request, outputType, entries);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param group _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> children)
            throws Exception {
        return outputEntries(request, outputType, children);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntries(Request request, OutputType outputType,
                                List<Entry> entries)
            throws Exception {

        StringBuffer sb =
            new StringBuffer("Test output handler for genomics data");
        if (outputType.equals(OUTPUT_GENOMICS_TEST1)) {
            sb.append("Test 1<br>");
        } else if (outputType.equals(OUTPUT_GENOMICS_TEST2)) {
            sb.append("Test 2<br>");
        }
        for (Entry entry : entries) {
            if ( !entry.getTypeHandler().isType("bio_genomics")) {
                continue;
            }
            sb.append("File:" + entry.getName());
            sb.append(HtmlUtils.br());
        }
        Result result = new Result("", sb);

        return result;
    }




}
