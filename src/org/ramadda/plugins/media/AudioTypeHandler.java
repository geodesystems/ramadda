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

package org.ramadda.plugins.media;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.WikiUtil;


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
public class AudioTypeHandler extends GenericTypeHandler {


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public AudioTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
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
    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
        if ( !tag.equals("audio")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }
        StringBuffer sb = new StringBuffer();
        String html =
            "<audio controls preload=\"none\" style=\"width:480px;\">\n <source src=\"${url}\" type=\"${mime}\" />\n <p>Your browser does not support HTML5 audio.</p>\n </audio>";



        String getFileUrl = entry.getTypeHandler().getEntryResourceUrl(request,
                             entry);
        String mime = "audio/wav";
        String ext = IOUtil.getFileExtension(
                         entry.getResource().getPath()).toLowerCase();
        if (ext.equals("ogg")) {
            mime = "audio/ogg";
        } else if (ext.equals("oga")) {
            mime = "audio/ogg";
        } else if (ext.equals("wav")) {
            mime = "audio/wav";
        } else if (ext.equals("m4a")) {
            mime = "audio/mp4";
        } else if (ext.equals("mp4")) {
            mime = "audio/mp4";
        }

        html = html.replace("${url}", getFileUrl);
        html = html.replace("${mime}", mime);
        sb.append(html);

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
