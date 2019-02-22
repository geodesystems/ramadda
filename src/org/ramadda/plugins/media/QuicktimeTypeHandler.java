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

package org.ramadda.plugins.media;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;


import org.ramadda.service.*;


import org.ramadda.service.Service;

import org.ramadda.util.HtmlUtils;


import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import ucar.unidata.xml.XmlUtil;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 */
public class QuicktimeTypeHandler extends GenericTypeHandler {

    /** _more_ */
    public static final int IDX_WIDTH = 0;

    /** _more_ */
    public static final int IDX_HEIGHT = 1;

    public static final int IDX_AUTOPLAY = 2;


    /**
     * ctor
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public QuicktimeTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param props _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getSimpleDisplay(Request request, Hashtable props,
                                   Entry entry)
            throws Exception {
        String width  = entry.getValue(IDX_WIDTH, "320");
        String height = entry.getValue(IDX_HEIGHT, "256");

        boolean autoplay = entry.getValue(IDX_AUTOPLAY, "false").equals("true");

        String header = getWikiManager().wikifyEntry(request, entry,
                            DFLT_WIKI_HEADER);
        StringBuffer sb = new StringBuffer(header);
        String url = entry.getTypeHandler().getEntryResourceUrl(request,
                         entry);
        sb.append("\n");
        /*
 <video width="320" height="240" controls>
  <source src="movie.mp4" type="video/mp4">
  <source src="movie.ogg" type="video/ogg">
Your browser does not support the video tag.
</video>
        */



        String extra = (autoplay?" autoplay ":"");
        sb.append(HtmlUtils.tag("video", HtmlUtils.attrs(new String[] {
            HtmlUtils.ATTR_SRC, url, HtmlUtils.ATTR_CLASS,
            "ramadda-video-embed", HtmlUtils.ATTR_WIDTH, width,
            HtmlUtils.ATTR_HEIGHT, height, 
        }) + " controls " + extra, HtmlUtils.tag("source",
                                         HtmlUtils.attrs(new String[] {
                                             HtmlUtils.ATTR_SRC,
                                             url }))));

        /*
       sb.append(HtmlUtils.tag(HtmlUtils.TAG_EMBED,
                                 HtmlUtils.attrs(new String[] {
                                         HtmlUtils.ATTR_SRC, url,
                                         HtmlUtils.ATTR_CLASS,
                                         "ramadda-video-embed", HtmlUtils.ATTR_WIDTH, width,
                                         HtmlUtils.ATTR_HEIGHT, height, "autoplay", "false", "controls",""
                                     })));
         */
        return sb.toString();

    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param wikiTemplate _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getInnerWikiContent(Request request, Entry entry,
                                      String wikiTemplate)
            throws Exception {
        return getSimpleDisplay(request, null, entry);
    }


}
