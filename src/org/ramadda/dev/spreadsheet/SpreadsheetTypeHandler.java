/*
* Copyright (c) 2008-2018 Geode Systems LLC
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

package org.ramadda.plugins.spreadsheet;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.OutputType;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;


import org.w3c.dom.*;

import ucar.unidata.xml.XmlUtil;



import java.util.ArrayList;
import java.util.List;


/**
 *
 */

public class SpreadsheetTypeHandler extends GenericTypeHandler {

    /** _more_ */
    public static final String ICON_SPREADSHEET = "ramadda.icon.spreadsheet";

    /** _more_ */
    public static final String ARG_SPREADSHEET_STOREDATA =
        "spreadsheet.storedata";

    /** _more_ */
    public static final String ARG_SPREADSHEET_GETXML = "spreadsheet.getxml";

    /** _more_ */
    public static final String ARG_SPREADSHEET_GETCSV = "spreadsheet.getcsv";




    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public SpreadsheetTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param links _more_
     *
     * @throws Exception _more_
     */
    public void getEntryLinks(Request request, Entry entry, List<Link> links)
            throws Exception {

        super.getEntryLinks(request, entry, links);
        /*
        links.add(
            new Link(
                request.url(
                    getRepository().URL_ENTRY_SHOW, ARG_ENTRYID,
                    entry.getId(), ARG_SLIDESHOW_SHOW,
                    "true"), getRepository().iconUrl(ICON_SLIDESHOW),
                             "View Slideshow", OutputType.TYPE_VIEW));
        */
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param formBuffer _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void addColumnsToEntryForm(Request request,
                                      StringBuffer formBuffer, Entry entry)
            throws Exception {}



    /**
     *
     *
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result getHtmlDisplay(Request request, Entry entry)
            throws Exception {

        Object[] values  = ((entry == null)
                            ? null
                            : entry.getValues());
        String   content = null;
        if ((values == null) || (values[0] == null)
                || (values[0].toString().trim().length() == 0)) {}
        else {
            content = values[0].toString();
        }

        if (request.get(ARG_SPREADSHEET_GETXML, false)) {
            if (content == null) {
                content = "";
            }

            return getRepository().getHtmlOutputHandler().makeAjaxResult(
                request, content);
        }

        if (request.get(ARG_SPREADSHEET_GETCSV, false)) {
            if (content == null) {
                content = "";
            }
            Result result = new Result("", new StringBuffer(content),
                                       "text/csv");
            result.setShouldDecorate(false);

            return result;
        }

        if (request.defined(ARG_SPREADSHEET_STOREDATA)) {
            if ( !getAccessManager().canDoAction(request, entry,
                    Permission.ACTION_EDIT)) {
                throw new AccessException("Cannot edit:" + entry.getLabel(),
                                          request);
            }
            String ss = request.getString(ARG_SPREADSHEET_STOREDATA, "");
            entry.setValues(new Object[] { ss });
            List<Entry> entries = new ArrayList<Entry>();
            entries.add(entry);
            getEntryManager().updateEntries(request, entries);
            System.err.println("storing:" + ss);

            return getRepository().getHtmlOutputHandler().makeAjaxResult(
                request, "ok");
            //            String url = request.entryUrl(getRepository().URL_ENTRY_SHOW, entry);
            //            return new Result(url);
        }

        return getSpreadsheet(request, entry);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result getSpreadsheet(Request request, Entry entry)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        //TODO: use real path
        sb.append(
            "<link media=\"all\" href=\"/repository/spreadsheet/styles.css\" rel=\"stylesheet\" type=\"text/css\" />\n");
        sb.append(
            HtmlUtils.importJS("/repository/spreadsheet/spreadsheet.js"));
        sb.append(
            HtmlUtils.importJS("/repository/spreadsheet/myspreadsheet.js"));
        sb.append(
            "<div class=\"data\" id=\"data\"></div><div id=\"source\" align=\"center\">");


        //        sb.append(HtmlUtils.script(HtmlUtils.call("loadSheetFromUrl", HtmlUtils.squote(spreadsheetUrl))));
        sb.append("</div>");
        sb.append(request.form(getRepository().URL_ENTRY_SHOW,
                               HtmlUtils.attr("name", "ssform")
                               + HtmlUtils.id("ssform")));

        sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HtmlUtils.hidden(ARG_SPREADSHEET_STOREDATA, "",
                                   HtmlUtils.id(ARG_SPREADSHEET_STOREDATA)));
        sb.append(HtmlUtils.formClose());

        StringBuffer js = new StringBuffer();
        js.append("var entryId = " + HtmlUtils.squote(entry.getId()) + ";\n");
        js.append(HtmlUtils.call("loadFromRamadda",
                                 HtmlUtils.squote(entry.getId())));

        sb.append(HtmlUtils.script(js.toString()));
        //        System.err.println("******\n" + sb+"\n******");
        Result result = new Result("", sb);

        return result;
    }




}
