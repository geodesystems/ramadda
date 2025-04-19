/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.type;

import org.ramadda.repository.*;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.sql.SqlUtil;

import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.util.WrapperException;
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

public class GroupTypeHandler extends TypeHandler {

    public GroupTypeHandler(Repository repository) throws Exception {
        super(repository, TypeHandler.TYPE_GROUP, "Folder");
    }

    public boolean isGroup() {
        return true;
    }

    public String getNodeType() {
        if (getParent() != null) {
            return getParent().getNodeType();
        }

        return NODETYPE_GROUP;
    }

    @Override
    public void getEntryLinks(Request request, Entry entry, OutputHandler.State state, List<Link> links)
            throws Exception {
        super.getEntryLinks(request, entry, state, links);
        if ( !entry.getIsLocalFile()) {
            /*
            links.add(
                new Link(
                    request.makeUrl(
                        getRepository().URL_SEARCH_FORM, ARG_GROUP,
                        entry.getId()), ICON_SEARCH,
                                        "Search in Folder"));
            */
        }

    }

    public Entry createEntry(String id) {
        return new Entry(id, this, true);
    }
}
