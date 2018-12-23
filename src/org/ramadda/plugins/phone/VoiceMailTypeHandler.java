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

package org.ramadda.plugins.phone;


import org.ramadda.repository.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;

import org.w3c.dom.*;

import ucar.unidata.util.StringUtil;

import java.util.List;


/**
 *
 *
 */
public class VoiceMailTypeHandler extends GenericTypeHandler {



    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public VoiceMailTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result getHtmlDisplay(Request request, Entry entry)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        String html =
            "<table><tr><td><div class=\"audio-player\"><object>\n<param name=\"autostart\" value=\"false\">\n<param name=\"src\" value=\"${url}\">\n<param name=\"autoplay\" value=\"false\">\n<param name=\"controller\" value=\"true\">\n<embed src=\"${url}\" controller=\"true\" autoplay=\"false\" autostart=\"False\" type=\"audio/wav\" /\n></object></div></td></tr></table>\n";

        String getFileUrl =
            entry.getTypeHandler().getEntryResourceUrl(request, entry);
        html = html.replace("${url}", getFileUrl);
        sb.append(html);
        sb.append(HtmlUtils.p());
        sb.append(entry.getDescription());

        return new Result(msg("Voice Mail"), sb);
    }




}
