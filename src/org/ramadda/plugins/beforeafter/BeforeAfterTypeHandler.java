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

package org.ramadda.plugins.beforeafter;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JQuery;

import org.ramadda.util.Utils;

import javax.imageio.ImageIO;


import org.w3c.dom.*;
import ucar.unidata.util.Misc;

import java.awt.Dimension;
import java.io.FileInputStream;

import java.awt.Image;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


/**
 *
 *
 */
public class BeforeAfterTypeHandler extends BeforeAfterBase {


    private static int cnt=0;
    
    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public BeforeAfterTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
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
    public Result getHtmlDisplay(Request request, Entry entry)
            throws Exception {
        if ( !isDefaultHtmlOutput(request)) {
            return null;
        }
        List<Entry> children = getEntryManager().getChildren(request, entry);
        return getHtmlDisplay(request, entry, new ArrayList<Entry>(),
                              children);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result getHtmlDisplay(Request request, Entry group,
                                 List<Entry> subGroups, List<Entry> entries)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        getPageHandler().entrySectionOpen(request, group, sb, "");

        String desc = group.getDescription();
        if ((desc != null) && (desc.length() > 0)) {
            sb.append(getWikiManager().wikifyEntry(request, group,desc));
        }
        StringBuffer divs = new StringBuffer();
        int          col  = 1;
        sb.append("\n");
        sb.append(
            HtmlUtils.importJS(
                getRepository().getUrlBase()
                + "/beforeandafter/jquery.beforeafter.js"));

        StringBuffer jq           = new StringBuffer();
        List<Entry>  entriesToUse = new ArrayList<Entry>();
        for (Entry child : entries) {
            if ( !child.isImage()) {
                continue;
            }
            entriesToUse.add(child);
        }

        for (int i = 0; i < entriesToUse.size(); i += 2) {
            if (i >= entriesToUse.size() - 1) {
                break;
            }
            Entry     entry1 = entriesToUse.get(i);
            Entry     entry2 = entriesToUse.get(i + 1);
            String width  = "800";
            String height = "366";
	    String swidth = (String) group.getValue(0,"800");
	    if(swidth!=null) swidth= swidth.trim();
	    if(!Utils.stringDefined(swidth)) swidth="800";
	    Dimension dim    = getDimensions(entry1);
	    if(Misc.equals(swidth,"default")) {
		width = ""+dim.width;
		height = ""+dim.height;
	    } else {
		if ((dim.width > 0) && (dim.height > 0)) {
		    if(dim.height> dim.width) {
			height =   "600";
			width = Integer.toString(600*dim.width/dim.height);
		    } else {
			width  = swidth;
			height = Integer.toString((int) (dim.height * new Integer(width) / (float) dim.width));
		    }
		}
	    }


            if (entry1.getCreateDate() > entry2.getCreateDate()) {
                Entry tmp = entry1;
                entry1 = entry2;
                entry2 = tmp;
            }
            String id = "bandacontainer" + (cnt++);
            divs.append("<div id=\"" + id + "\">\n");
            String url1 = HtmlUtils.url(
                              request.makeUrl(repository.URL_ENTRY_GET) + "/"
                              + getStorageManager().getFileTail(
                                  entry1), ARG_ENTRYID, entry1.getId());
            String url2 = HtmlUtils.url(
                              request.makeUrl(repository.URL_ENTRY_GET) + "/"
                              + getStorageManager().getFileTail(
                                  entry2), ARG_ENTRYID, entry2.getId());


            divs.append(
                    "<img src=\"" + url1 + "\""
                    + HtmlUtils.attr(HtmlUtils.ATTR_WIDTH,  width)
                    + HtmlUtils.attr(HtmlUtils.ATTR_HEIGHT,  height)		    
                    + ">\n");

            divs.append(
                    "<img src=\"" + url2 + "\""
		    + HtmlUtils.attr(HtmlUtils.ATTR_WIDTH,  width)
		    + HtmlUtils.attr(HtmlUtils.ATTR_HEIGHT,  height)
                    + ">\n");

            divs.append("</div>\n");
            String path = getRepository().getUrlBase() + "/beforeandafter/";
            String args = "{imagePath:'" + path + "'}";
            sb.append("\n");
            jq.append(HtmlUtils.script(JQuery.ready("\n$(function(){$('#" + id
						    + "').beforeAfter(" + args
						    + ");});\n")));
        }
        sb.append("\n");
        sb.append(divs);
        sb.append("\n");
        sb.append(jq);
        sb.append("\n");
        getPageHandler().entrySectionClose(request, group, sb);

        return new Result(msg("Before/After Image"), sb);
    }



}
