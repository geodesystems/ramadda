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
public class GroupTypeHandler extends TypeHandler {


    /**
     * _more_
     *
     * @param repository _more_
     *
     * @throws Exception _more_
     */
    public GroupTypeHandler(Repository repository) throws Exception {
        super(repository, TypeHandler.TYPE_GROUP, "Folder");
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
     * @return _more_
     */
    public String getNodeType() {
        if (getParent() != null) {
            return getParent().getNodeType();
        }

        return NODETYPE_GROUP;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param links _more_
     *
     * @throws Exception _more_
     */
    public void getEntryLinks(Request request, Entry entry, List<Link> links)
            throws Exception {
        super.getEntryLinks(request, entry, links);


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


    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    public Entry createEntry(String id) {
        return new Entry(id, this, true);
    }
}
