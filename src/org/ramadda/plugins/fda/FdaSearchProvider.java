/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.fda;


import org.json.*;

import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;


import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Proxy that searches youtube
 *
 */
public class FdaSearchProvider extends SearchProvider {


    /** _more_ */
    private static final String FDA_ID = "openfda";

    /** _more_ */
    private String myType = null;

    /**
     * _more_
     *
     * @param repository _more_
     */
    public FdaSearchProvider(Repository repository) {
        super(repository, FDA_ID, "openFDA Search");
    }


    /**
     * _more_
     *
     * @param repository _more_
     * @param args _more_
     */
    public FdaSearchProvider(Repository repository, List<String> args) {
        super(repository, FDA_ID + "-" + args.get(0), args.get(1));
        myType = args.get(0);
    }



    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSiteUrl() {
        return "https://open.fda.gov/";
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSearchProviderIconUrl() {
        return "${root}/fda/fda.png";
    }





    /**
     * _more_
     *
     * @param request _more_
     * @param searchInfo _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public List<Entry> getEntries(Request request, org.ramadda.repository.util.SelectInfo searchInfo)
            throws Exception {

        List<Entry> entries = new ArrayList<Entry>();
        TypeHandler typeHandler =
            getRepository().getTypeHandler("type_fda_series");
        //what, how, count, searchField,name
        String desc1 =
            "<wiki>+section # label=\"FDA Data: {{name}}\"\n<br>\n{{group showMenu=\"true\"}}{{display type=\"linechart\" title=\"\"  height=\"400\" }}\n-section\n";
        String desc2 =
            "<wiki>+section # label=\"FDA Data: {{name}}\"\n<br>\n{{group showMenu=\"true\"}}{{display type=\"bartable\" title=\"\"  height=\"400\" }}\n-section\n";



        String[][] calls = {
            {
                "food", "enforcement", "report_date", "reason_for_recall",
                "Food enforcement reports over time", desc1
            }, {
                "food", "enforcement", "voluntary_mandated.exact",
                "reason_for_recall",
                "Food enforcement reports - Who initiates", desc2
            }, {
                "food", "enforcement", "classification.exact",
                "reason_for_recall",
                "Food enforcement reports  - Seriousness", desc2
            }, {
                "device", "event", "date_received", "device.generic_name",
                "Devices -  Adverse event reports over time", desc1
            }, {
                "device", "event", "device.generic_name.exact",
                "device.generic_name", "Devices - Reported types", desc2
            }, {
                "device", "event", "event_type.exact", "device.generic_name",
                "Devices - Types of events", desc2
            }, {
                "drug", "event", "receivedate", "patient.drug.drugindication",
                "Drugs - Adverse event reports over time", desc2
            }, {
                "drug", "event", "primarysource.qualification",
                "patient.drug.drugindication", "Drugs - Who reports", desc2
            }, {
                "drug", "event", "patient.drug.openfda.pharm_class_epc.exact",
                "patient.drug.drugindication", "Drug classes", desc2
            }, {
                "drug", "event", "patient.drug.drugindication.exact",
                "patient.drug.drugindication", "Drug indications", desc2
            }, {
                "drug", "event", "patient.reaction.reactionmeddrapt.exact",
                "patient.drug.drugindication",
                "Drugs - Reported interactions", desc2
            },
        };


        String template =
            "https://api.fda.gov/{what}/{how}.json?&count={count}";
        String searchTemplate = "&search={searchField}:\"{query}\"";
        String query = HtmlUtils.urlEncodeSpace(request.getString(ARG_TEXT,
                           ""));
        if (query.equals("all")) {
            query = "";
        }
        Entry parent = getSynthTopLevelEntry();
        for (String[] call : calls) {
            String what = call[0];
            if ((myType != null) && !myType.equals(what)) {
                continue;
            }
            String how   = call[1];
            String count = call[2];
            String url = template.replace("{what}", what).replace("{how}",
                                          how).replace("{count}", count);
            if (query.length() > 0) {
                url += searchTemplate.replace("{query}",
                        query).replace("{searchField}", call[3]);
            }
            String name = "openFDA - " + call[4];
            String desc = call[5];
            if (query.length() > 0) {
                name = name + " - " + query;
            }
            Date     dttm     = new Date();
            Date     fromDate = dttm,
                     toDate   = dttm;
            Resource resource = new Resource(new URL(url));
            String   quid     = query.replaceAll(" ", "_");
            Entry newEntry = new Entry(Repository.ID_PREFIX_SYNTH + getId()
                                       + TypeHandler.ID_DELIMITER + what
                                       + TypeHandler.ID_DELIMITER + how
                                       + TypeHandler.ID_DELIMITER + count
                                       + TypeHandler.ID_DELIMITER
                                       + quid, typeHandler);
            Object[] values = typeHandler.makeEntryValues(null);
            newEntry.initEntry(name, desc, parent,
                               getUserManager().getLocalFileUser(), resource,
                               "", Entry.DEFAULT_ORDER, dttm.getTime(),
                               dttm.getTime(), fromDate.getTime(),
                               toDate.getTime(), values);
            getEntryManager().cacheSynthEntry(newEntry);
            entries.add(newEntry);
        }

        return entries;
    }


}
