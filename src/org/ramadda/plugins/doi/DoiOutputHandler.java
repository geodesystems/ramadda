/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.doi;


import edu.ucsb.nceas.ezid.*;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.AccessException;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.util.HtmlUtils;


import org.w3c.dom.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import ucar.unidata.xml.XmlUtil;


import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


/**
 *
 *
 */
@SuppressWarnings("unchecked")
public class DoiOutputHandler extends OutputHandler {


    /** _more_ */
    public static final String PROP_EZID_USERNAME = "ezid.username";

    /** _more_ */
    public static final String PROP_EZID_PASSWORD = "ezid.password";

    /** _more_ */
    public static final String PROP_DOI_ADMINONLY = "doi.adminonly";


    /** _more_ */
    public static final String PROP_DOI_PREFIXES = "doi.prefixes";

    /** _more_ */
    public static final String PROP_EZID_PROFILE = "ezid.profile";

    /** _more_ */
    public static final String METADATA_TARGET = "_target";

    /** _more_ */
    public static final String METADATA_PROFILE = "_profile";

    /** _more_ */
    public static final String PROFILE_ERC = "erc";

    /** _more_ */
    public static final String PROFILE_DATACITE = "datacite";

    /** _more_ */
    public static final String PROFILE_DC = "dc";

    /** _more_ */
    public static final String[] PROFILES = { PROFILE_ERC, PROFILE_DATACITE,
            PROFILE_DC };



    /** _more_ */
    public static final String ARG_CREATE = "doi.create";

    /** _more_ */
    public static final String ARG_PREFIX = "prefix";

    /** _more_ */
    public static final String ARG_DATACITE_CREATOR = "datacite.creator";

    /** _more_ */
    public static final String ARG_DATACITE_TITLE = "datacite.title";

    /** _more_ */
    public static final String ARG_DATACITE_PUBLISHER = "datacite.publisher";

    /** _more_ */
    public static final String ARG_DATACITE_PUBLICATIONYEAR =
        "datacite.publicationyear";

    /** _more_ */
    public static final String ARG_DATACITE_RESOURCETYPE =
        "datacite.resourcetype";

    /** _more_ */
    public static final String[] METADATA_DATACITE_ARGS = { ARG_DATACITE_CREATOR,
            ARG_DATACITE_TITLE, ARG_DATACITE_PUBLISHER,
            ARG_DATACITE_PUBLICATIONYEAR,
            ARG_DATACITE_RESOURCETYPE, };

    /** _more_ */
    public static final String[] METADATA_DATACITE_LABELS = { "Creator",
            "Title", "Publisher", "Publication Year", "Resource Type", };


    /** _more_ */
    public static final String ARG_ERC_WHO = "erc.who";

    /** _more_ */
    public static final String ARG_ERC_WHAT = "erc.what";

    /** _more_ */
    public static final String ARG_ERC_WHEN = "erc.when";


    /** _more_ */
    public static final String[] METADATA_ERC_ARGS = { ARG_ERC_WHO,
            ARG_ERC_WHAT, ARG_ERC_WHEN, };

    /** _more_ */
    public static final String[] METADATA_ERC_LABELS = { "Who", "What",
            "When", };


    /** _more_ */
    public static final String ARG_DC_CREATOR = "dc.creator";

    /** _more_ */
    public static final String ARG_DC_TITLE = "dc.title";

    /** _more_ */
    public static final String ARG_DC_PUBLISHER = "dc.publisher";

    /** _more_ */
    public static final String ARG_DC_DATE = "dc.date";


    /** _more_ */
    public static final String[] METADATA_DC_ARGS = { ARG_DC_CREATOR,
            ARG_DC_TITLE, ARG_DC_PUBLISHER, ARG_DC_DATE, };

    /** _more_ */
    public static final String[] METADATA_DC_LABELS = { "Creator", "Title",
            "Publisher", "Date", };

    /** _more_ */
    private boolean enabled = false;

    /** _more_ */
    private boolean adminOnly = true;

    /** _more_ */
    private List<String> dataciteResources;

    /** Map output type */
    public static final OutputType OUTPUT_DOI_CREATE =
        new OutputType("Create Identifier", "doi.create",
                       OutputType.TYPE_OTHER, "", "fa-passport");


    /**
     *
     *
     * @param repository  the repository
     * @param element     the Element
     * @throws Exception  problem generating handler
     */
    public DoiOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        adminOnly = getRepository().getProperty(PROP_DOI_ADMINONLY,
                adminOnly);
        enabled = (getRepository().getProperty(
            PROP_EZID_USERNAME,
            (String) null) != null) && (getRepository().getProperty(
                PROP_EZID_PASSWORD,
                (String) null) != null) && (getRepository().getProperty(
                    PROP_DOI_PREFIXES, (String) null) != null);
        addType(OUTPUT_DOI_CREATE);

        dataciteResources = Misc.toList(new String[] {
            "Collection", "Dataset", "Event", "Film", "Image",
            "InteractiveResource", "Model", "PhysicalObject", "Service",
            "Software", "Sound", "Text",
        });
    }

    /**
     * _more_
     *
     * @param profile _more_
     *
     * @return _more_
     */
    private String getMetadataLabel(String profile) {
        if (profile.equals(PROFILE_ERC)) {
            return "ERC Profile";
        } else if (profile.equals(PROFILE_DATACITE)) {
            return "Datacite Profile";
        } else {
            return "DC Profile";
        }
    }


    /**
     * _more_
     *
     * @param profile _more_
     *
     * @return _more_
     */
    private String[] getMetadataArgs(String profile) {
        if (profile.equals(PROFILE_ERC)) {
            return METADATA_ERC_ARGS;
        } else if (profile.equals(PROFILE_DATACITE)) {
            return METADATA_DATACITE_ARGS;
        } else {
            return METADATA_DC_ARGS;
        }
    }

    /**
     * _more_
     *
     * @param profile _more_
     *
     * @return _more_
     */
    private String[] getMetadataLabels(String profile) {
        if (profile.equals(PROFILE_ERC)) {
            return METADATA_ERC_LABELS;
        } else if (profile.equals(PROFILE_DATACITE)) {
            return METADATA_DATACITE_LABELS;
        } else {
            return METADATA_DC_LABELS;
        }
    }


    /**
     * Get the entry links
     *
     * @param request  the Request
     * @param state    the repository State
     * @param links    the links
     *
     * @throws Exception  problem creating links
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
        if (canAccess(request, state.getEntry())) {
            links.add(makeLink(request, state.getEntry(), OUTPUT_DOI_CREATE));
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private boolean canAccess(Request request, Entry entry) throws Exception {
        if ( !enabled) {
            return false;
        }
        if ( !getAccessManager().canDoEdit(request, entry)) {
            return false;
        }
        if (adminOnly) {
            return request.getUser().getAdmin();
        }

        return true;
    }

    /**
     * Output the entry
     *
     * @param request      the Request
     * @param outputType   the type of output
     * @param entry        the Entry to output
     *
     * @return  the Result
     *
     * @throws Exception  problem outputting entry
     */
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
        if ( !canAccess(request, entry)) {
            throw new AccessException("Cannot edit:" + entry.getLabel(),
                                      request);
        }

        StringBuffer sb = new StringBuffer();

        String profile = getRepository().getProperty(PROP_EZID_PROFILE,
                             PROFILE_ERC);
        if (request.defined(PROP_EZID_PROFILE)) {
            profile = request.getString(PROP_EZID_PROFILE, profile);
        } else if (request.defined(PROFILE_ERC)) {
            profile = PROFILE_ERC;
        } else if (request.defined(PROFILE_DC)) {
            profile = PROFILE_DC;
        } else {
            profile = PROFILE_DATACITE;
        }
        if ( !request.exists(ARG_CREATE)) {
	    getPageHandler().entrySectionOpen(request, entry, sb,
					      "Create Identifier");
            sb.append(HtmlUtils.formTable());
            sb.append(
                HtmlUtils.form(getRepository().URL_ENTRY_SHOW.toString()));
            sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
            sb.append(HtmlUtils.hidden(ARG_OUTPUT,
                                       OUTPUT_DOI_CREATE.toString()));
            sb.append(HtmlUtils.formEntry(msgLabel("Profile"),
                                          getMetadataLabel(profile)));
            sb.append(
                HtmlUtils.formEntry(
                    msgLabel("Prefix"),
                    HtmlUtils.select(
                        ARG_PREFIX,
                        StringUtil.split(
                            getRepository().getProperty(
                                PROP_DOI_PREFIXES, ""), ",", true, true))));

            addToForm(request, entry, sb, getMetadataArgs(profile),
                      getMetadataLabels(profile));

            StringBuffer buttons =
                new StringBuffer(HtmlUtils.submit("Create Identifier",
                    ARG_CREATE));
            for (String otherProfile : PROFILES) {
                if ( !profile.equals(otherProfile)) {
                    buttons.append(HtmlUtils.space(1));
                    buttons.append(HtmlUtils.submit("Use "
                            + getMetadataLabel(otherProfile), otherProfile));
                }
            }
            sb.append(HtmlUtils.formEntry("", buttons.toString()));
            sb.append(HtmlUtils.formEntry(HtmlUtils.space(25), ""));
            sb.append(HtmlUtils.formClose());
            sb.append(HtmlUtils.formTableClose());
	    getPageHandler().entrySectionClose(request, entry, sb);
        } else {
            EZIDService ezid = new EZIDService();
            ezid.login(getRepository().getProperty(PROP_EZID_USERNAME, ""),
                       getRepository().getProperty(PROP_EZID_PASSWORD, ""));
            HashMap<String, String> doiMetadata = new HashMap<String,
                                                      String>();
            String entryUrl = request.getAbsoluteUrl(
                                  request.entryUrl(
                                      getRepository().URL_ENTRY_SHOW, entry));
            doiMetadata.put(METADATA_PROFILE, profile);
            doiMetadata.put(METADATA_TARGET, entryUrl);
            addMetadata(request, doiMetadata, getMetadataArgs(profile));
            String prefix = request.getString(ARG_PREFIX);
            String doi    = ezid.mintIdentifier(prefix, null);
            Metadata metadata = new Metadata(getRepository().getGUID(),
                                             entry.getId(),
                                             getMetadataManager().findType(DoiMetadataHandler.TYPE_DOI),
                                             false,
                                             DoiMetadataHandler.ID_TYPE_DOI,
                                             doi, "", "", "");
            getMetadataManager().insertMetadata(metadata);
            getMetadataManager().addMetadata(request,entry, metadata);

            sb.append(HtmlUtils.p());
            sb.append("DOI has been created");
            sb.append(HtmlUtils.p());
            sb.append(DoiMetadataHandler.getHref(doi));
        }

        return new Result("", sb);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param args _more_
     * @param labels _more_
     */
    private void addToForm(Request request, Entry entry, StringBuffer sb,
                           String[] args, String[] labels) {
        for (int i = 0; i < args.length; i++) {
            String arg   = args[i];
            String value = "";
            if (arg.equals(ARG_DC_TITLE) || arg.equals(ARG_DATACITE_TITLE)
                    || arg.equals(ARG_ERC_WHAT)) {
                value = entry.getName();
            } else if (arg.equals(ARG_ERC_WHEN) || arg.equals(ARG_DC_DATE)) {
                value = formatDate(request, new Date(entry.getStartDate()));
            } else if (arg.equals(ARG_DATACITE_PUBLICATIONYEAR)) {
                //TODO: does this need to be just a year?
                value = formatDate(request, new Date(entry.getStartDate()));
            } else if (arg.equals(ARG_DATACITE_CREATOR)
                       || arg.equals(ARG_DC_CREATOR)
                       || arg.equals(ARG_ERC_WHO)) {
                value = entry.getUser().getLabel();
            } else if (arg.equals(ARG_DATACITE_PUBLISHER)
                       || arg.equals(ARG_DC_PUBLISHER)) {
                value = request.getUser().getLabel();
            }
            String widget = null;

            if (request.defined(arg)) {
                value = request.getString(arg, "");
            }
            if (arg.equals(ARG_DATACITE_RESOURCETYPE)) {
                widget = HtmlUtils.select(arg, dataciteResources, value);
            }

            if (widget == null) {
                widget = HtmlUtils.input(args[i], value, HtmlUtils.SIZE_30);
            }
            sb.append(HtmlUtils.formEntry(msgLabel(labels[i]), widget));

        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param metadata _more_
     * @param args _more_
     */
    private void addMetadata(Request request,
                             HashMap<String, String> metadata,
                             String[] args) {
        for (String arg : args) {
            if (request.defined(arg)) {
                metadata.put(arg, request.getString(arg, ""));
            }
        }
    }


    /**
     * Output a group
     *
     * @param request      The Request
     * @param outputType   the type of output
     * @param group        the group Entry
     * @param subGroups    the subgroups
     * @param entries      The list of Entrys
     *
     * @return  the resule
     *
     * @throws Exception    problem on output
     */
    @Override
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> children)
            throws Exception {
        return outputEntry(request, outputType, group);
    }


}
