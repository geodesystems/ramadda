/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.cdmdata;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;

import org.ramadda.repository.output.*;
import org.ramadda.util.HtmlUtils;


import org.ramadda.util.sql.SqlUtil;


import org.w3c.dom.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;



import java.io.*;

import java.net.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



import java.util.regex.*;

import java.util.zip.*;


/**
 *
 *
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public class LdmOutputHandler extends OutputHandler {



    /** _more_ */
    public static final OutputType OUTPUT_LDM = new OutputType("LDM Insert",
                                                    "ldm",
                                                    OutputType.TYPE_FILE, "",
                                                    ICON_DATA);


    /**
     * _more_
     *
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public LdmOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_LDM);
    }


    /** _more_ */
    private boolean enabled = false;

    /** _more_ */
    private String pqinsert;

    /**
     * _more_
     */
    @Override
    public void initAttributes() {
        super.initAttributes();
        pqinsert = getRepository().getProperty(LdmAction.PROP_LDM_PQINSERT,
                (String) null);
        enabled = pqinsert != null;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param state _more_
     * @param links _more_
     *
     *
     * @throws Exception _more_
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {

        //Are we configured to do the LDM
        if ( !enabled) {
            return;
        }
        if (getRepository().getProperty(LdmAction.PROP_LDM_QUEUE,
                                        "").length() == 0) {
            return;
        }

        if ( !request.getUser().getAdmin()) {
            return;
        }
        if (state.entry != null) {
            if ( !state.entry.isFile()) {
                return;
            }
        } else {

            boolean anyFiles = false;
            for (Entry entry : state.getAllEntries()) {
                if (entry.getResource().isFile()) {
                    anyFiles = true;

                    break;
                }
            }
            if ( !anyFiles) {
                return;
            }
        }
        links.add(makeLink(request, state.getEntry(), OUTPUT_LDM));

    }



    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
        return handleEntries(request, entry,
                             (List<Entry>) Misc.newList(entry));
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
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> children)
            throws Exception {
        return handleEntries(request, group, children);
    }

    /** _more_ */
    private String lastFeed = "SPARE";

    /** _more_ */
    private String lastProductId = "${filename}";

    /**
     * _more_
     *
     * @param request _more_
     * @param parent _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result handleEntries(Request request, Entry parent,
                                 List<Entry> entries)
            throws Exception {
        StringBuffer sb          = new StringBuffer();
        List<Entry>  fileEntries = new ArrayList<Entry>();
        List<String> ids         = new ArrayList<String>();
        for (Entry entry : entries) {
            if (entry.isFile()) {
                fileEntries.add(entry);
                ids.add(entry.getId());
            }
        }

        String feed = request.getString(LdmAction.PROP_LDM_FEED, lastFeed);
        String productId = request.getString(LdmAction.PROP_LDM_PRODUCTID,
                                             lastProductId);
        if ( !request.defined(LdmAction.PROP_LDM_FEED)) {
            String formUrl;
            if (parent.isGroup() && parent.isDummy()) {
                formUrl =
                    request.makeUrl(getRepository().URL_ENTRY_GETENTRIES);
                sb.append(HtmlUtils.form(formUrl));
                sb.append(HtmlUtils.hidden(ARG_ENTRYIDS,
                                           StringUtil.join(",", ids)));
            } else {
                formUrl = request.makeUrl(getRepository().URL_ENTRY_SHOW);
                sb.append(HtmlUtils.form(formUrl));
                sb.append(HtmlUtils.hidden(ARG_ENTRYID, parent.getId()));
            }
            sb.append(HtmlUtils.hidden(ARG_OUTPUT, OUTPUT_LDM.getId()));
            sb.append(HtmlUtils.formTable());

            if (fileEntries.size() == 1) {
                File f = fileEntries.get(0).getFile();
                String fileTail =
                    getStorageManager().getFileTail(fileEntries.get(0));
                String size = " (" + f.length() + " bytes)";
                sb.append(HtmlUtils.formEntry("File:", fileTail + size));
            } else {
                int size = 0;
                for (Entry entry : fileEntries) {
                    size += entry.getFile().length();
                }
                sb.append(HtmlUtils.formEntry("Files:",
                        fileEntries.size() + " files. Total size:" + size));
            }


            sb.append(
                HtmlUtils.formEntry(
                    "Feed:",
                    HtmlUtils.select(
                        LdmAction.PROP_LDM_FEED,
                        Misc.toList(LdmAction.LDM_FEED_TYPES), feed)));
            String tooltip =
                "macros: ${fromday}  ${frommonth} ${fromyear} ${frommonthname}  <br>"
                + "${today}  ${tomonth} ${toyear} ${tomonthname} <br> "
                + "${filename}  ${fileextension}";
            sb.append(
                HtmlUtils.formEntry(
                    "Product ID:",
                    HtmlUtils.input(
                        LdmAction.PROP_LDM_PRODUCTID, productId,
                        HtmlUtils.SIZE_60 + HtmlUtils.title(tooltip))));

            sb.append(HtmlUtils.formTableClose());
            if (fileEntries.size() > 1) {
                sb.append(HtmlUtils.submit("Insert files into LDM"));
            } else {
                sb.append(HtmlUtils.submit("Insert file into LDM"));
            }
        } else {
            String queue =
                getRepository().getProperty(LdmAction.PROP_LDM_QUEUE, "");
            for (Entry entry : fileEntries) {
                String id =
                    getRepository().getEntryManager().replaceMacros(entry,
                        productId);
                LdmAction.insertIntoQueue(getRepository(), pqinsert, queue,
                                          feed, id,
                                          entry.getResource().getPath());
                sb.append("Inserted: "
                          + getStorageManager().getFileTail(entry));
                sb.append(HtmlUtils.br());
            }
            lastFeed      = feed;
            lastProductId = productId;
        }

        return makeLinksResult(request, msg("LDM Insert"), sb,
                               new State(parent));
    }



}
