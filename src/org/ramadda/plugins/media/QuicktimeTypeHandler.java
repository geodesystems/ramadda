/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.media;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;


import org.ramadda.service.*;


import org.ramadda.service.Service;

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
public class QuicktimeTypeHandler extends MediaTypeHandler {

    private static int IDX = MediaTypeHandler.IDX_LAST+1;

    /** _more_ */
    public static final int IDX_AUTOPLAY = IDX++;


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

    public String getMediaType(Request request, Entry entry) {
	return  MEDIA_OTHER;
    }
}
