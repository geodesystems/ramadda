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
import org.ramadda.util.WikiUtil;

import org.ramadda.util.sql.Clause;


import org.ramadda.util.sql.SqlUtil;
import org.ramadda.util.sql.SqlUtil;


import org.w3c.dom.*;

import ucar.unidata.util.Misc;

import java.awt.Dimension;


import java.awt.Image;

import java.io.FileInputStream;




import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import javax.imageio.ImageIO;


/**
 *
 *
 */
public class BeforeAfterBase extends GenericTypeHandler {

    /** _more_ */
    private Hashtable<String, Dimension> dimensions = new Hashtable<String,
                                                          Dimension>();


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public BeforeAfterBase(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isGroup() {
        return true;
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Dimension getDimensions(Entry entry) throws Exception {
        Dimension dim = dimensions.get(entry.getId());
        if (dim == null) {
            Image image = ImageIO.read(new FileInputStream(entry.getFile()));
            dim = new Dimension(image.getWidth(null), image.getHeight(null));
            if ((dim.width > 0) && (dim.height > 0)) {
                dimensions.put(entry.getId(), dim);
            }
        }

        return dim;
    }



    /**
     * _more_
     *
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {

        if (tag.equals("imageoverlay")) {
            return getImageOverlay(wikiUtil, request, originalEntry, entry,
                                   tag, props);
        }

        return super.getWikiInclude(wikiUtil, request, originalEntry, entry,
                                    tag, props);
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
    private List<Entry> getEntries(Request request, Entry entry)
            throws Exception {
        List<Entry> entries = getEntryManager().getChildren(request, entry);
        List<Entry> entriesToUse = new ArrayList<Entry>();
        for (Entry child : entries) {
            if ( !child.isImage()) {
                continue;
            }
            entriesToUse.add(child);
        }
        entriesToUse = EntryUtil.sortEntriesOn(entriesToUse,
                "entryorder,createdate", false);

        return entriesToUse;
    }

    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getImageOverlay(WikiUtil wikiUtil, Request request,
                                  Entry originalEntry, Entry entry,
                                  String tag, Hashtable props)
            throws Exception {

        String        overrideWidth = (String) props.get("imageWidth");
        StringBuilder sb            = new StringBuilder();
        HtmlUtils.importJS(
            sb,
            getPageHandler().makeHtdocsUrl("/beforeafter/imageoverlay.js"));
        String width = entry.getValue(0, "800").trim();
        if (width.length() == 0) {
            width = "800";
        }
        if (Utils.stringDefined(overrideWidth)) {
            width = overrideWidth;
        }

        double dwidth = Double.parseDouble(width);
        width += "px";
        List<Entry> entries = getEntries(request, entry);
        for (int i = 0; i < entries.size(); i += 2) {
            String baseId = getRepository().getGUID();
            if (i >= entries.size() - 1) {
                break;
            }
            Entry entry1 = entries.get(i);
            Entry entry2 = entries.get(i + 1);
            String url1 = HtmlUtils.url(
                              request.makeUrl(repository.URL_ENTRY_GET) + "/"
                              + getStorageManager().getFileTail(
                                  entry1), ARG_ENTRYID, entry1.getId());
            String url2 = HtmlUtils.url(
                              request.makeUrl(repository.URL_ENTRY_GET) + "/"
                              + getStorageManager().getFileTail(
                                  entry2), ARG_ENTRYID, entry2.getId());


            Dimension dim1 = getDimensions(entry1);
            Dimension dim2 = getDimensions(entry2);
            Dimension dim = new Dimension(Math.max(dim1.width, dim2.width),
                                          Math.max(dim1.height, dim2.height));
            double ratio1 = dim1.width / (double) dim1.height;
            double ratio2 = dim2.width / (double) dim2.height;

            //Put a min height for when showing in tabs. add 20 for the slider
            int h1 = (int) (dwidth / ratio1) + 20;
            int h2 = (int) (dwidth / ratio2) + 20;
            int h  = Math.max(h1, h2);
            HtmlUtils.open(sb, "div", "style",
                           "position:relative;width:" + width
                           + ";min-height:" + h + "px;");
            HtmlUtils.center(sb,
                             HtmlUtils.div("",
                                           " id='slider_" + baseId
                                           + "' style='width:200px;' "));
            sb.append(HtmlUtils.img(url1, "",
                                    "id='before_" + baseId + "' width="
                                    + width + " style='position:absolute;'"));
            sb.append(
                HtmlUtils.img(
                    url2, "",
                    "id='after_" + baseId + "' width=" + width
                    + " style='opacity:0.5;position:absolute;'"));
            sb.append("</div>\n");
            String script = "imageOverlayInit('" + baseId + "');\n";
            HtmlUtils.script(sb, script);
        }

        return sb.toString();
    }





}
