/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.notes;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;


import org.ramadda.util.sql.Clause;


import org.ramadda.util.sql.SqlUtil;
import org.ramadda.util.sql.SqlUtil;


import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.sql.PreparedStatement;

import java.sql.ResultSet;
import java.sql.Statement;


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
public class PasteitCollectionTypeHandler extends ExtensibleGroupTypeHandler {



    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public PasteitCollectionTypeHandler(Repository repository,
                                        Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Result getHtmlDisplay(Request request, Entry group,  Entries children)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        boolean canAdd = getAccessManager().canDoNew(request, group);
        if (canAdd) {
            /*
            sb.append(HtmlUtils
                .href(HtmlUtils
                    .url(request
                        .entryUrl(getRepository().URL_ENTRY_FORM, group,
                            ARG_GROUP), ARG_TYPE,
                         FaqEntryTypeHandler.TYPE_FAQENTRY), HtmlUtils
                      .img(getRepository().getIconUrl(ICON_NEW),
                           msg("New FAQ Question"))));
            */
        }


        return new Result(msg("Paste It Collection"), sb);
    }




}
