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

package org.ramadda.geodata.python;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;

import org.ramadda.util.DifUtil;
import org.ramadda.util.HtmlUtils;

import org.json.*;
import org.ramadda.util.Json;
import org.w3c.dom.*;


import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;






/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class IPythonNotebookOutputHandler extends OutputHandler {



    /** _more_ */
    public static final OutputType OUTPUT_IPYTHON_TOJSMD =
        new OutputType("Convert to JSMD", "ipython.tojsmd", OutputType.TYPE_VIEW, "",
                       "fa-python");



    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public IPythonNotebookOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_IPYTHON_TOJSMD);
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
        if (state.isDummyGroup()) {
            return;
        }
        if (state.getEntry() != null) {
            if(state.getEntry().getTypeHandler().isType("python_notebook")) {
                links.add(makeLink(request, state.getEntry(), OUTPUT_IPYTHON_TOJSMD));
            }
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> subGroups,
                              List<Entry> entries)
            throws Exception {
        return outputEntry(request, outputType, group);
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
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
        StringBuilder sb =new StringBuilder();
        String json = getStorageManager().readSystemResource(entry.getFile());
        JSONObject obj   = new JSONObject(new JSONTokener(json));
        JSONArray  cells = null;
        if (obj.has("cells")) {
            cells = obj.getJSONArray("cells");
        } else if (obj.has("worksheets")) {
            JSONArray  worksheets = obj.getJSONArray("worksheets");
            JSONObject worksheet  = worksheets.getJSONObject(0);
            cells = worksheet.getJSONArray("cells");
        }
        int index = 0;
        for (int i = 0; i < cells.length(); i++) {
            JSONObject    cell     = cells.getJSONObject(i);
            String        cellType = cell.getString("cell_type");
            StringBuilder ssb      = new StringBuilder();
            String input =null;
            if (cell.has("source")) {
                IPythonNotebookTypeHandler.readLines(cell, "source", ssb);
                input = ssb.toString();
            } else if (cell.has("input")) {
                IPythonNotebookTypeHandler.readLines(cell, "input", ssb);
                input = ssb.toString();
            }
            if (input != null) {
                if (cellType.equals("code")) {
                    if(input.startsWith("%%javascript")) {
                        sb.append("%%js\n");
                        input = input.replace("%%javascript","");
                    } else if(input.startsWith("%%html")) {
                        sb.append("%%html\n");
                        input = input.replace("%%html","");                       
                    } else if(input.startsWith("%%latex")) {
                        sb.append("%%md\n");
                        input = "$$\n" + input.replace("%%latex","") +"\n$$\n";                       
                    } else if(input.startsWith("%%markdown")) {
                        sb.append("%%md\n");
                        input = input.replace("%%markdown","");                       
                    } else if(input.startsWith("%%python")) {
                        sb.append("%%py\n");
                        input = input.replace("%%python","");                       
                    } else {
                        sb.append("%%py\n");
                        StringBuilder newSrc = new StringBuilder();
                        for(String line: StringUtil.split(input,"\n")) {
                            //Strip the ipy special functions
                            if(line.trim().startsWith("%")) line = "#"  + line;
                            newSrc.append(line);
                            newSrc.append("\n");
                        }
                        input  = newSrc.toString();
                    }
                } else if (cellType.equals("markdown")) {
                    sb.append("%%md\n");
                } else if (cellType.equals("raw")) {
                    sb.append("%%raw\n");
                } else if (cellType.equals("heading")) {
                    sb.append("%%md\n");
                    input = "# " + input.trim().replaceAll("\n"," ");
                } else {
                    sb.append("%%" + cellType +"\n");
                }
                sb.append(input);
                sb.append("\n");
            }
        }
        Result result =  new Result("jsmd", sb, "text");
        result.setReturnFilename(IOUtil.stripExtension(entry.getName())+".jsmd");
        return result;
    }

}
