/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.datapolicy;


import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;


import org.w3c.dom.Element;

import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Hashtable;
import java.util.List;


/**
 * Class description
 *
 *
 * @version        $version$, Sat, Dec 28, '13
 * @author         Enter your name here...
 */
public class DataPolicyApiHandler extends RepositoryManager implements RequestHandler {




    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @param props _more_
     *
     * @throws Exception _more_
     */
    public DataPolicyApiHandler(Repository repository, Element node,
                              Hashtable props)
            throws Exception {
        super(repository);
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processDataPolicyRequest(Request request) throws Exception {
        StringBuilder sb = new StringBuilder();
	List<String> top = new ArrayList<String>();
	List<String> policies = new ArrayList<String>();
	Utils.add(top,"version","0.1","name",JsonUtil.quote(getRepository().getProperty("ramadda.datapolicy.name","Data Policy Collection")));
        Request searchRequest = getRepository().getAdminRequest();
        searchRequest.put(ARG_TYPE, "type_datapolicy");
	StringBuilder tmp = new StringBuilder();
	List<Entry> entries = getEntryManager().getEntries(searchRequest, tmp);
	for (Entry entry : entries) {
	    addPolicy(entry, policies);
	}
	top.add("policies");
	top.add(JsonUtil.list(policies));
	sb.append(JsonUtil.map(top));

	return new Result("",sb, JsonUtil.MIMETYPE);
    }
    private String qt(Object v) {
	return JsonUtil.quote(v.toString());
    }

    private void addPolicy(Entry entry, List<String> policies) {
	List<String> policy = new ArrayList<String>();
	Utils.add(policy,"id", qt(entry.getValue(DataPolicyTypeHandler.IDX_ID)));
	Utils.add(policy,"citation",qt(entry.getValue(DataPolicyTypeHandler.IDX_CITATION)));
	Utils.add(policy,"license",qt(entry.getValue(DataPolicyTypeHandler.IDX_LICENSE)));
	Utils.add(policy,"license_description",qt(entry.getValue(DataPolicyTypeHandler.IDX_LICENSE_DESCRIPTION)));			
	List<String> access = new ArrayList<String>();

	String viewRoles = (String)entry.getValue(DataPolicyTypeHandler.IDX_VIEW_ROLES);
	String fileRoles = (String)entry.getValue(DataPolicyTypeHandler.IDX_FILE_ROLES);	
	if(Utils.stringDefined(viewRoles)) {
	    access.add(JsonUtil.map("action",JsonUtil.quote("view"),"roles",
				makeRoles(viewRoles)));
	}
	if(Utils.stringDefined(fileRoles)) {
	    access.add(JsonUtil.map("action",JsonUtil.quote("file"),"roles",
				    makeRoles(fileRoles)));
	}	
	Utils.add(policy,"access",JsonUtil.list(access));
	policies.add(JsonUtil.map(policy));
    }

    private String makeRoles(String roles) {
	List<String> r = new ArrayList<String>();
	r.addAll(Utils.split(roles,",",true,true));
	return JsonUtil.list(JsonUtil.quote(r));
    }



}
