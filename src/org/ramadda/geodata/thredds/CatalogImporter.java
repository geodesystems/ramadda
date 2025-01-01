/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.thredds;


import org.ramadda.geodata.cdmdata.*;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.util.HtmlUtils;


import org.ramadda.util.sql.SqlUtil;


import org.w3c.dom.*;

import ucar.unidata.util.CatalogUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.io.File;


import java.net.*;


import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



import java.util.regex.*;

import java.util.zip.*;


/**
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class CatalogImporter extends OutputHandler {

    /** _more_ */
    public static final String ARG_CATALOG = "catalog";

    /** _more_ */
    public static final OutputType OUTPUT_CATALOG_IMPORT =
        new OutputType("Import THREDDS Catalog", "thredds.import.catalog",
                       OutputType.TYPE_FILE, "",
                       CatalogOutputHandler.ICON_CATALOG);

    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public CatalogImporter(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_CATALOG_IMPORT);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param state _more_
     * @param links _more_
     *
     * @throws Exception _more_
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
        if ( !request.getUser().getAdmin()) {
            return;
        }

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param group _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Result outputGroup(final Request request, OutputType outputType,
                              final Entry group, List<Entry> children)
            throws Exception {

        if ( !getAccessManager().canDoNew(request, group)) {
            throw new IllegalArgumentException(
                "No access to import a catalog");
        }


        request.ensureAdmin();
        if ( !request.exists(ARG_CATALOG)) {
            StringBuffer sb = new StringBuffer();
            sb.append(request.form(getRepository().URL_ENTRY_SHOW, ""));
            sb.append(HtmlUtils.hidden(ARG_GROUP, group.getId()));
            sb.append(HtmlUtils.hidden(ARG_OUTPUT,
                                       OUTPUT_CATALOG_IMPORT.getId()));
            sb.append(msgHeader("Import a THREDDS catalog"));
            sb.append(HtmlUtils.formTable());
            sb.append(HtmlUtils.formEntry(msgLabel("URL"),
                                          HtmlUtils.input(ARG_CATALOG, BLANK,
                                              HtmlUtils.SIZE_70)));

            sb.append(
                HtmlUtils.formEntry(
                    "",
                    HtmlUtils.checkbox(ARG_RECURSE, "true", false)
                    + HtmlUtils.space(1) + msg("Recurse")
                    + HtmlUtils.space(1)
                    + HtmlUtils.checkbox(ATTR_ADDMETADATA, "true", false)
                    + HtmlUtils.space(1) + msg("Add full metadata")
                    + HtmlUtils.space(1)
                    + HtmlUtils.checkbox(
                        ATTR_ADDSHORTMETADATA, "true",
                        false) + HtmlUtils.space(1)
                               + msg("Just add spatial/temporal metadata")
                               + HtmlUtils.space(1)
                               + HtmlUtils.checkbox(
                                   ARG_RESOURCE_DOWNLOAD, "true",
                                   false) + HtmlUtils.space(1)
                                          + msg("Download URLs")));
            sb.append(HtmlUtils.formEntry("", HtmlUtils.submit("Go")));
            sb.append(HtmlUtils.formTableClose());
            sb.append(HtmlUtils.formClose());

            return getEntryManager().makeEntryEditResult(request, group,
                    "Catalog Import", sb);
        }



        boolean      recurse     = request.get(ARG_RECURSE, false);
        boolean      addMetadata = request.get(ATTR_ADDMETADATA, false);
        boolean addShortMetadata = request.get(ATTR_ADDSHORTMETADATA, false);
        boolean      download    = request.get(ARG_RESOURCE_DOWNLOAD, false);
        StringBuffer sb          = new StringBuffer();
        sb.append("<p>");
        final String catalog = request.getString(ARG_CATALOG, "").trim();
        sb.append(request.form(getRepository().URL_ENTRY_SHOW, ""));
        sb.append(HtmlUtils.hidden(ARG_GROUP, group.getId()));
        sb.append(HtmlUtils.hidden(ARG_OUTPUT,
                                   OUTPUT_CATALOG_IMPORT.getId()));
        sb.append(HtmlUtils.submit("Import catalog:"));
        sb.append(HtmlUtils.space(1));
        sb.append(HtmlUtils.input(ARG_CATALOG, catalog, " size=\"75\""));

        sb.append(HtmlUtils.checkbox(ARG_RECURSE, "true", recurse));
        sb.append(HtmlUtils.space(1));
        sb.append(msg("Recurse"));



        sb.append(HtmlUtils.checkbox(ATTR_ADDMETADATA, "true", addMetadata));
        sb.append(HtmlUtils.space(1));
        sb.append(msg("Add Metadata"));

        sb.append(HtmlUtils.space(1));
        sb.append(HtmlUtils.checkbox(ATTR_ADDSHORTMETADATA, "true",
                                     addShortMetadata));
        sb.append(HtmlUtils.space(1));
        sb.append(msg("Just add spatial/temporal metadata"));


        sb.append(HtmlUtils.space(1));
        sb.append(HtmlUtils.checkbox(ARG_RESOURCE_DOWNLOAD, "true",
                                     download));
        sb.append(HtmlUtils.space(1));
        sb.append(msg("Download URLs"));
        sb.append("</form>");

        if (catalog.length() > 0) {
            ActionManager.Action action = new ActionManager.Action() {
                public void run(Object actionId) throws Exception {
                    boolean recurse     = request.get(ARG_RECURSE, false);
                    boolean addMetadata = request.get(ATTR_ADDMETADATA,
                                              false);
                    boolean addShortMetadata =
                        request.get(ATTR_ADDSHORTMETADATA, false);
                    boolean download = request.get(ARG_RESOURCE_DOWNLOAD,
                                           false);
                    CatalogHarvester harvester =
                        new CatalogHarvester(getRepository(), group, catalog,
                                             request.getUser(), recurse,
                                             download, actionId);
                    harvester.setAddMetadata(addMetadata);
                    harvester.setAddShortMetadata(addShortMetadata);
                    harvester.run();
                }
            };
            String href = HtmlUtils.href(
                              request.entryUrl(
                                  getRepository().URL_ENTRY_SHOW,
                                  group), "Continue");

            return getActionManager().doAction(request, action,
                    "Importing Catalog", "Continue: " + href);
        }

        return new Result(
            "", new StringBuffer("Humm, probably shouldn't get here"));

    }



}
