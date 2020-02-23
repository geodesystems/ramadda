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

package org.ramadda.geodata.cdmdata;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;

import org.w3c.dom.Element;

import ucar.unidata.util.IOUtil;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class OpendapLinkTypeHandler extends GridTypeHandler {


    /** _more_ */
    public static final String TYPE_OPENDAPLINK = "opendaplink";

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public OpendapLinkTypeHandler(Repository repository, Element entryNode)
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
    public String xxxgetEntryResourceUrl(Request request, Entry entry)
            throws Exception {

        String fileTail =
            IOUtil.stripExtension(getStorageManager().getFileTail(entry))
            + ".html";
        System.err.println("getEntry:" + fileTail);

        return HtmlUtils.url(request.makeUrl(getRepository().URL_ENTRY_GET)
                             + "/" + fileTail, ARG_ENTRYID, entry.getId());
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
    @Override
    public String getPathForEntry(Request request, Entry entry)
            throws Exception {
        Resource resource = entry.getResource();

        return resource.getPath() + ".html";
    }

}
