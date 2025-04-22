/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.type;

import org.ramadda.repository.*;
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

/**
 * Class TypeHandler _more_
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class ExtensibleGroupTypeHandler extends GenericTypeHandler {

    public ExtensibleGroupTypeHandler(Repository repository,
                                      Element entryNode)
            throws Exception {
        super(repository, entryNode);
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

    public Entry createEntry(String id) {
        return new Entry(id, this, true);
    }
}
