/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.datapolicy;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.database.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.repository.util.SelectInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 */
@SuppressWarnings("unchecked")
public class DataPolicyTypeHandler extends GenericTypeHandler {


    /**  */
    private static int IDX = 0;

    /**  */
    public static final int IDX_ID = IDX++;

    /**  */
    public static final int IDX_CITATION = IDX++;

    /**  */
    public static final int IDX_LICENSE1 = IDX++;
    public static final int IDX_LICENSE2 = IDX++;    
    public static final int IDX_LICENSE3 = IDX++;

    /**  */
    public static final int IDX_VIEW_ROLES = IDX++;

    /**  */
    public static final int IDX_FILE_ROLES = IDX++;

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public DataPolicyTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    /**
     *
     * @param request _more_
     * @param entry _more_
     * @param fromImport _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {
	String id = (String) entry.getValue(request,IDX_ID);
	if(!Utils.stringDefined(id)) {
	    entry.setValue(IDX_ID,Utils.makeID(entry.getName(),false,"-"));
	}
        super.initializeNewEntry(request, entry, newType);
        getRepository().getAccessManager().updateLocalDataPolicies();

    }




    /**
     *
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void entryChanged(Entry entry) throws Exception {
        super.entryChanged(entry);
	System.err.println("entryChanged:" + entry);
        getRepository().getAccessManager().updateLocalDataPolicies();
    }


    @Override
    public void entryDeleted(String id) throws Exception {
	super.entryDeleted(id);
        getRepository().getAccessManager().updateLocalDataPolicies();
    }    


    /**
     *
     * @param request _more_
     * @param entry _more_
     * @param column _more_
     * @param tmpSb _more_
     * @param values _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void formatColumnHtmlValue(Request request, Entry entry,
                                      Column column, Appendable tmpSb,
                                      Object[] values)
            throws Exception {
        if (column.getName().equals("view_roles")
                || column.getName().equals("file_roles")) {
            String roles =
                (String) entry.getValue(request,column.getName().equals("view_roles")
                                        ? IDX_VIEW_ROLES
                                        : IDX_FILE_ROLES);
            for (String r : Utils.split(roles, ",", true, true)) {
                Role role = new Role(r);
                HtmlUtils.div(tmpSb, r,
                              HtmlUtils.cssClass(role.getCssClass()));
            }
            return;
        }

        if (column.getName().startsWith("license")) {
            String license = (String) entry.getValue(request,column.getOffset());
	    if(Utils.stringDefined(license) && !license.equals("none")) {
		String label = column.getEnumLabel(license);
		tmpSb.append(getMetadataManager().getLicenseHtml(license, label,false));
	    }
            return;
        }
        super.formatColumnHtmlValue(request, entry, column, tmpSb, values);

    }



}
