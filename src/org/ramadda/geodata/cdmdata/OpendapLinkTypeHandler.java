/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
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
    @Override
    public String getPathForEntry(Request request, Entry entry, boolean forRead)
            throws Exception {
        Resource resource = entry.getResource();
        return resource.getPath() + ".html";
    }

}
