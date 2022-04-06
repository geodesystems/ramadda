/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.datapolicy;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.DataPolicy;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;


import org.w3c.dom.Element;

import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlUtil;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
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
        StringBuilder sb       = new StringBuilder();
        List<String>  top      = new ArrayList<String>();
        List<String>  policies = new ArrayList<String>();
        Utils.add(
            top, "version", "0.1", "name",
            JsonUtil.quote(
                getRepository().getProperty(
                    "ramadda.datapolicy.name", "Data Policy Collection")));
        Request       searchRequest = getRepository().getAdminRequest();
        StringBuilder tmp           = new StringBuilder();
        System.err.println("processDataPolicyRequest:");
        List<Entry> entries =
            getEntryManager().getEntriesWithType(searchRequest,
                "type_datapolicy");
        System.err.println("Entries:" + entries);

        for (Entry entry : entries) {
            addPolicy(entry, policies);
        }
        top.add("policies");
        top.add(JsonUtil.list(policies));
        sb.append(JsonUtil.map(top));

        return new Result("", sb, JsonUtil.MIMETYPE);
    }

    /**
     *
     * @param v _more_
      * @return _more_
     */
    private String qt(Object v) {
        return JsonUtil.quote(v.toString());
    }

    /**
     *
     * @param entry _more_
     * @param policies _more_
     *
     * @throws Exception _more_
     */
    private void addPolicy(Entry entry, List<String> policies)
            throws Exception {
        List<String> policy = new ArrayList<String>();
        String id = (String) entry.getValue(DataPolicyTypeHandler.IDX_ID);
        if ( !Utils.stringDefined(id)) {
            id = Utils.makeID(entry.getName());
        }
        Utils.add(policy, DataPolicy.FIELD_ID, qt(id));
        Utils.add(policy, DataPolicy.FIELD_NAME, qt(entry.getName()));
        Utils.add(policy, DataPolicy.FIELD_DESCRIPTION, qt(entry.getDescription()));
        Utils.add(policy, DataPolicy.FIELD_CITATION,
                  qt(entry.getValue(DataPolicyTypeHandler.IDX_CITATION)));
        Utils.add(policy, DataPolicy.FIELD_LICENSE,
                  qt(entry.getValue(DataPolicyTypeHandler.IDX_LICENSE)));
        String url = getRepository().getTmpRequest().getAbsoluteUrl(
                         getEntryManager().getEntryURL(null, entry));
        Utils.add(policy, DataPolicy.FIELD_URL, qt(url));

        List<String> permissions = new ArrayList<String>();

        String viewRoles =
            (String) entry.getValue(DataPolicyTypeHandler.IDX_VIEW_ROLES);
        String fileRoles =
            (String) entry.getValue(DataPolicyTypeHandler.IDX_FILE_ROLES);
        if (Utils.stringDefined(viewRoles)) {
            permissions.add(JsonUtil.map(DataPolicy.FIELD_ACTION, JsonUtil.quote("view"),
                                         DataPolicy.FIELD_ROLES, makeRoles(viewRoles)));
        }
        if (Utils.stringDefined(fileRoles)) {
            permissions.add(JsonUtil.map(DataPolicy.FIELD_ACTION, JsonUtil.quote("file"),
                                         DataPolicy.FIELD_ROLES, makeRoles(fileRoles)));
        }
        Utils.add(policy, DataPolicy.FIELD_PERMISSIONS, JsonUtil.list(permissions));
        policies.add(JsonUtil.map(policy));
    }

    /**
     *
     * @param roles _more_
      * @return _more_
     */
    private String makeRoles(String roles) {
        List<String> r = new ArrayList<String>();
        r.addAll(Utils.split(roles, ",", true, true));

        return JsonUtil.list(JsonUtil.quote(r));
    }



}
