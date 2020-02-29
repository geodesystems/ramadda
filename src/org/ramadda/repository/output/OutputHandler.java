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

package org.ramadda.repository.output;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.map.*;

import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.BufferMapList;
import org.ramadda.util.CategoryBuffer;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JQuery;

import org.ramadda.util.TempDir;
import org.ramadda.util.Utils;

import org.ramadda.util.WikiUtil;
import org.ramadda.util.XmlUtils;


import org.ramadda.util.sql.SqlUtil;

import org.w3c.dom.Element;

import ucar.unidata.util.DateUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.net.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.regex.*;
import java.util.zip.*;


/**
 *
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class OutputHandler extends RepositoryManager {

    /** _more_ */
    public static final String PROP_PROCESSDIR = "processdir";


    /** _more_ */
    public static final String WIDTH_DATE = "150";

    /** _more_ */
    public static final String WIDTH_SIZE = "100";

    /** _more_ */
    public static final String WIDTH_KIND = "200";

    /** _more_ */
    public static final JQuery JQ = null;

    /** max connections attribute */
    public static final String ATTR_MAXCONNECTIONS = "maxconnections";

    /** the links label */
    public static final String LABEL_LINKS = "Actions";

    /** HTML OutputType */
    public static final OutputType OUTPUT_HTML =
        new OutputType("Entry Page", "default.html",
                       OutputType.TYPE_VIEW | OutputType.TYPE_FORSEARCH, "",
                       ICON_HOME);


    /** name */
    private String name;

    /** Output types */
    private List<OutputType> types = new ArrayList<OutputType>();

    /** hash of name to output type */
    private Hashtable<String, OutputType> typeMap = new Hashtable<String,
                                                        OutputType>();


    /** default max connnections */
    private int maxConnections = -1;

    /** number of connnections */
    private int numberOfConnections = 0;

    /** total calls */
    private int totalCalls = 0;

    /**
     * _more_
     */
    public OutputHandler() {
        super(null);
    }

    /**
     * Construct an OutputHandler
     *
     * @param repository  the repository
     * @param name        the OutputHandler name
     *
     * @throws Exception  problem with repository
     */
    public OutputHandler(Repository repository, String name)
            throws Exception {
        super(repository);
        this.name = name;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return name;
    }


    /**
     * Shutdown
     */
    public void shutdown() {}

    /**
     * Do we allow arachnids?
     *
     * @return  true if robots allowed
     */
    public boolean allowRobots() {
        return false;
    }

    /**
     * Find the output type matching the id
     *
     * @param id  the name of the type
     *
     * @return the OutputType or null
     */
    public OutputType findOutputType(String id) {
        return typeMap.get(id);
    }


    /**
     * Construct an OutputHandler
     *
     * @param repository  the repository
     * @param element     the Element
     * @throws Exception  problem with repository
     */
    public OutputHandler(Repository repository, Element element)
            throws Exception {
        this(repository,
             XmlUtil.getAttribute(element, ATTR_NAME, (String) null));
        maxConnections = XmlUtil.getAttribute(element, ATTR_MAXCONNECTIONS,
                maxConnections);


    }

    /**
     * Initialize
     */
    public void init() {}


    /**
     * Clear the cache
     */
    public void clearCache() {}

    /**
     * Add this to the Entry node
     *
     * @param request  the Request
     * @param entry    the Entry
     * @param node     the Node
     *
     * @throws Exception problem adding to Entry node
     */
    public void addToEntryNode(Request request, Entry entry, Element node)
            throws Exception {}


    /**
     * Add an OutputType to this handler
     *
     * @param type  the OutputType
     */
    public void addType(OutputType type) {
        getRepository().setOutputTypeOK(type);
        type.setGroupName(name);
        types.add(type);
        typeMap.put(type.getId(), type);
        repository.addOutputType(type);
    }

    /**
     * Get a list of types
     *
     * @return  the list
     */
    public List<OutputType> getTypes() {
        return types;
    }

    /**
     *  Set the Name property.
     *
     *  @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }


    /**
     *  Get the Name property.
     *
     *  @return The Name
     */
    public String getName() {
        if (name == null) {
            name = Misc.getClassName(getClass());
        }

        return name;
    }



    /**
     * Are we showing all
     *
     * @param request   the Request
     * @param subGroups the list of subgroups
     * @param entries   the list of entries
     *
     * @return    true if showing all
     */
    public boolean showingAll(Request request, List<Entry> subGroups,
                              List<Entry> entries) {
        int cnt = subGroups.size() + entries.size();
        int max = request.get(ARG_MAX, VIEW_MAX_ROWS);
        if ((cnt > 0) && ((cnt == max) || request.defined(ARG_SKIP))) {
            return false;
        }

        return true;
    }



    /**
     * Get the AuthorizationMethod
     *
     * @param request  the Request
     *
     * @return  the AuthorizationMethod
     */
    public AuthorizationMethod getAuthorizationMethod(Request request) {
        return AuthorizationMethod.AUTH_HTML;
    }

    /**
     * Show the next bunch
     *
     * @param request   the Request
     * @param subGroups the subgroups
     * @param entries   the List of Entries
     * @param sb        the output
     *
     * @throws Exception  problems showing entries
     */
    public void showNext(Request request, List<Entry> subGroups,
                         List<Entry> entries, Appendable sb)
            throws Exception {
        int cnt = subGroups.size() + entries.size();
        showNext(request, cnt, sb);
    }

    /**
     * Show the next bunch
     *
     * @param request   the Request
     * @param cnt       the number to show
     * @param sb        the output
     *
     * @throws Exception  problem showing them
     */
    public void showNext(Request request, int cnt, Appendable sb)
            throws Exception {

        //        Misc.printStack("Show next", 10);
        int max = request.get(ARG_MAX, VIEW_MAX_ROWS);
        if ((cnt > 0) && ((cnt == max) || request.defined(ARG_SKIP))) {
            int skip = Math.max(0, request.get(ARG_SKIP, 0));
            sb.append(HtmlUtils.open(HtmlUtils.TAG_DIV, "class",
                                     "entry-table-page"));
            sb.append(msgLabel("Showing") + (skip + 1) + "-" + (skip + cnt));
            sb.append(HtmlUtils.space(2));
            List<String> toks = new ArrayList<String>();
            if (skip > 0) {
                toks.add(
                    HtmlUtils.href(
                        request.getUrl(ARG_SKIP) + "&" + ARG_SKIP + "="
                        + (skip - max), HtmlUtils.faIcon(
                            "fa-step-backward", "title", "View previous")));
            }
            if (cnt >= max) {
                toks.add(
                    HtmlUtils.href(
                        request.getUrl(ARG_SKIP) + "&" + ARG_SKIP + "="
                        + (skip + max), HtmlUtils.faIcon(
                            "fa-step-forward", "title", "View next")));
            }
            int moreMax = (int) (max * 1.5);
            if (moreMax < 10) {
                moreMax = 10;
            }
            int lessMax = max / 2;
            if (lessMax < 1) {
                lessMax = 1;
            }
            request.put(ARG_MAX, "" + moreMax);
            if (cnt >= max) {
                toks.add(HtmlUtils.href(request.getUrl(),
                                        HtmlUtils.faIcon("fa-plus", "title",
                                            "View more")));

                request.put(ARG_MAX, "" + lessMax);
                toks.add(HtmlUtils.href(request.getUrl(),
                                        HtmlUtils.faIcon("fa-minus", "title",
                                            "View less")));
            }
            if (toks.size() > 0) {
                sb.append(StringUtil.join(" ", toks));

            }
            sb.append(HtmlUtils.close(HtmlUtils.TAG_DIV));
            request.put(ARG_MAX, "" + max);
        }

    }




    /**
     * Can we handle the OutputType?
     *
     * @param output  the OutputType
     *
     * @return  true if supported
     */
    public boolean canHandleOutput(OutputType output) {
        for (OutputType type : types) {
            if (type.equals(output)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get the system statistics
     *
     * @param sb  the StringBuffer to add to
     */
    public void getSystemStats(StringBuffer sb) {
        if (totalCalls > 0) {
            StringBuffer stats = new StringBuffer();
            for (OutputType outputType : types) {
                if (outputType.getNumberOfCalls() > 0) {
                    stats.append(outputType.getLabel() + " # "
                                 + msgLabel("Calls")
                                 + outputType.getNumberOfCalls()
                                 + HtmlUtils.br());
                }
            }

            sb.append(HtmlUtils.formEntryTop(msgLabel(name),
                                             stats.toString()));

        }
    }



    /**
     * Class to hold State
     *
     * @author RAMADDA Development Team
     */
    public static class State {

        /** for unknown flag */
        public static final int FOR_UNKNOWN = 0;

        /** for header flag */
        public static final int FOR_HEADER = 1;

        /** for what parameter */
        public int forWhat = FOR_UNKNOWN;

        /** the Entry */
        public Entry entry;

        /** the parent group */
        public Entry group;

        /** the subgroups */
        public List<Entry> subGroups;

        /** the entries */
        public List<Entry> entries;

        /** all entries */
        public List<Entry> allEntries;

        /**
         * Create some state for the Entry
         *
         * @param entry  the Entry
         */
        public State(Entry entry) {
            if (entry != null) {
                if (entry.isGroup()) {
                    group          = (Entry) entry;
                    this.subGroups = group.getSubGroups();
                    this.entries   = group.getSubEntries();
                } else {
                    this.entry = entry;
                }
            }

        }

        /**
         * Create some State for the Entry and others
         *
         * @param group     the parent group
         * @param subGroups subgroups
         * @param entries   list of entries in this
         */
        public State(Entry group, List<Entry> subGroups,
                     List<Entry> entries) {
            this.group     = group;
            this.entries   = entries;
            this.subGroups = subGroups;
        }


        /**
         * Create some State for the entries  and group
         *
         * @param group   the group Entry
         * @param entries  the list of Entrys
         */
        public State(Entry group, List<Entry> entries) {
            this.group   = group;
            this.entries = entries;
        }

        /**
         * Is this a dummy group?
         *
         * @return  true if stupid
         */
        public boolean isDummyGroup() {
            Entry entry = getEntry();
            if (entry == null) {
                return false;
            }
            if ( !entry.isGroup()) {
                return false;
            }

            return entry.isDummy();
        }

        /**
         * Is this for the header?
         *
         * @return  true if for header
         */
        public boolean forHeader() {
            return forWhat == FOR_HEADER;
        }

        /**
         * Get all the entries
         *
         * @return  a list of all the entries
         */
        public List<Entry> getAllEntries() {
            if (allEntries == null) {
                allEntries = new ArrayList();
                if (subGroups != null) {
                    allEntries.addAll(subGroups);
                }
                if (entries != null) {
                    allEntries.addAll(entries);
                }
                if (entry != null) {
                    allEntries.add(entry);
                }
            }

            return (List<Entry>) allEntries;
        }

        /**
         * Get the Entry for this State
         *
         * @return  the State of the Entry
         */
        public Entry getEntry() {
            if (entry != null) {
                return entry;
            }

            return group;
        }

    }


    /**
     * Make the links result
     *
     * @param request  the Request
     * @param title    the links title
     * @param sb       the buffer to add to
     * @param state    the State
     *
     * @return  the Result
     *
     * @throws Exception problem making Result
     */
    public Result makeLinksResult(Request request, String title,
                                  Appendable sb, State state)
            throws Exception {
        Result result = new Result(title, sb);
        addLinks(request, result, state);

        return result;
    }

    /**
     * Add links
     *
     * @param request   the Request
     * @param result    the Result
     * @param state     the State
     *
     * @throws Exception _more_
     */
    public void addLinks(Request request, Result result, State state)
            throws Exception {
        state.forWhat = State.FOR_HEADER;
        if (state.getEntry().getDescription().indexOf("<nolinks>") >= 0) {
            return;
        }
    }


    /**
     * Get the services
     *
     * @param request   the Request
     * @param entry     the Entry
     * @param services  the list of ServiceInfos to add to
     */
    public void getServiceInfos(Request request, Entry entry,
                                List<ServiceInfo> services) {}


    /**
     * Get the Entry links
     *
     * @param request   the Request
     * @param state     the State
     * @param links     the List of Links to add to
     *
     * @throws Exception  problem creating Links
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {}



    /**
     * Make a link for the OutputType
     *
     * @param request   The request
     * @param entry     the Entry
     * @param outputType  the OutputType
     *
     * @return the Link
     *
     * @throws Exception problem with repository
     */
    public Link makeLink(Request request, Entry entry, OutputType outputType)
            throws Exception {
        return makeLink(request, entry, outputType, "");
    }

    /**
     * Make a link for the OutputType and suffix
     *
     * @param request   The request
     * @param entry     the Entry
     * @param outputType  the OutputType
     * @param suffix  the suffix
     *
     * @return  the Link
     *
     * @throws Exception  problem with repository
     */
    public Link makeLink(Request request, Entry entry, OutputType outputType,
                         String suffix)
            throws Exception {
        String url;
        if (entry == null) {
            url = HtmlUtils.url(getRepository().URL_ENTRY_SHOW + suffix,
                                ARG_OUTPUT, outputType.toString());
        } else {
            url = request.getEntryUrlPath(getRepository().URL_ENTRY_SHOW
                                          + suffix, entry);
            url = HtmlUtils.url(url, ARG_ENTRYID, entry.getId(), ARG_OUTPUT,
                                outputType.toString());
        }

        return new Link(url, (outputType.getIcon() == null)
                             ? null
                             : outputType.getIcon(), outputType.getLabel(),
                             outputType);

    }


    /**
     * Add an OutputLink
     *
     * @param request   the Request
     * @param entry     the Entry
     * @param links     the list of Links
     * @param type      the OutputType
     *
     * @throws Exception   problem with the repository
     */
    public void addOutputLink(Request request, Entry entry, List<Link> links,
                              OutputType type)
            throws Exception {
        links.add(new Link(request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                            entry, ARG_OUTPUT,
                                            type.toString()), type.getIcon(),
                                                type.getLabel(), type));

    }


    /**
     * A not implemented result
     *
     * @param method   the method
     * @return  the Result
     */
    private Result notImplemented(String method) {
        throw new IllegalArgumentException("Method: " + method
                                           + " not implemented");
    }

    /**
     * Output the Entry for the type
     *
     * @param request     the Request
     * @param outputType  the OutputType
     * @param entry       the Entry
     *
     * @return  the Result'ing output
     *
     * @throws Exception  problem with the request
     */
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(entry);

        return outputGroup(request, outputType,
                           getEntryManager().getDummyGroup(),
                           new ArrayList<Entry>(), entries);
    }


    /**
     * Output a group
     *
     * @param request     the Request
     * @param outputType  the OutputType
     * @param group       the Entry group
     * @param subGroups   the subgroup Entrys
     * @param entries     Entries at the same level
     *
     * @return   the result
     *
     * @throws Exception  problem with the Repository
     */
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> subGroups,
                              List<Entry> entries)
            throws Exception {
        return notImplemented("outputGroup");
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public final Result xoutputGroup(Request request, Entry group,
                                     List<Entry> subGroups,
                                     List<Entry> entries)
            throws Exception {
        return null;
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
    public final Result xoutputEntry(Request request, Entry entry)
            throws Exception {
        return null;
    }


    /**
     * _more_
     *
     * @param output _more_
     *
     * @return _more_
     */
    public String getMimeType(OutputType output) {
        return null;
    }






    /**
     * _more_
     *
     * @param request _more_
     * @param elementId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String getGroupSelect(Request request, String elementId)
            throws Exception {
        return getSelect(request, elementId, "Select", false, "", null);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param elementId _more_
     * @param label _more_
     * @param allEntries _more_
     * @param type _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String getSelect(Request request, String elementId,
                                   String label, boolean allEntries,
                                   String type)
            throws Exception {

        return getSelect(request, elementId, label, allEntries, type, null);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param elementId _more_
     * @param label _more_
     * @param allEntries _more_
     * @param type _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String getSelect(Request request, String elementId,
                                   String label, boolean allEntries,
                                   String type, Entry entry)
            throws Exception {
        return getSelect(request, elementId, label, allEntries, type, entry,
                         true);
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param formId _more_
     * @param skipList _more_
     *
     * @throws Exception _more_
     */
    public static void addUrlShowingForm(Appendable sb, String formId,
                                         String skipList)
            throws Exception {
        addUrlShowingForm(sb, null, formId, skipList, null);
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param entry _more_
     * @param formId _more_
     * @param skipList _more_
     * @param hook _more_
     *
     * @throws Exception _more_
     */
    public static void addUrlShowingForm(Appendable sb, Entry entry,
                                         String formId, String skipList,
                                         String hook)
            throws Exception {
        String outputId = HtmlUtils.getUniqueId("output_");
        HtmlUtils.div(sb, "", HtmlUtils.id(outputId));
        HtmlUtils.script(sb,
                         HtmlUtils.call("HtmlUtil.makeUrlShowingForm",
                                        (entry == null)
                                        ? "null"
                                        : HtmlUtils.quote(
                                            entry.getId()), HtmlUtils.quote(
                                                formId), HtmlUtils.quote(
                                                    outputId), (skipList
                                                        != null)
                ? skipList
                : "null", "" + hook));
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param elementId _more_
     * @param label _more_
     * @param allEntries _more_
     * @param type _more_
     * @param entry _more_
     * @param addClear _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String getSelect(Request request, String elementId,
                                   String label, boolean allEntries,
                                   String type, Entry entry, boolean addClear)
            throws Exception {
        return getSelect(request, elementId, label, allEntries, type, entry,
                         addClear, "");
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param elementId _more_
     * @param label _more_
     * @param allEntries _more_
     * @param type _more_
     * @param entry _more_
     * @param addClear _more_
     * @param linkExtra _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String getSelect(Request request, String elementId,
                                   String label, boolean allEntries,
                                   String type, Entry entry,
                                   boolean addClear, String linkExtra)
            throws Exception {

        boolean hasType    = Utils.stringDefined(type);
        String  selectorId = elementId + ( !hasType
                                           ? ""
                                           : "_" + type);
        String event = HtmlUtils.call(
                           "selectInitialClick",
                           HtmlUtils.comma(
                               "event", HtmlUtils.squote(selectorId),
                               HtmlUtils.squote(elementId),
                               HtmlUtils.squote(
                                   Boolean.toString(allEntries)), ( !hasType
                ? "null"
                : HtmlUtils.squote(type)), ((entry != null)
                                            ? HtmlUtils.squote(entry.getId())
                                            : "null"), HtmlUtils.squote(
                                                request.getString(
                                                    ARG_ENTRYTYPE, ""))));

        String clearEvent = HtmlUtils.call("clearSelect",
                                           HtmlUtils.squote(selectorId));
        String link = HtmlUtils.mouseClickHref(event, label,
                          linkExtra
                          + HtmlUtils.id(selectorId + "_selectlink"));
        if (addClear) {
            link = link + " "
                   + HtmlUtils.mouseClickHref(clearEvent, "Clear",
                       HtmlUtils.id(selectorId + "_selectlink"));
        }

        return link;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param seen _more_
     * @param args _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getSelectLink(Request request, Entry entry, HashSet seen,
                                String... args)
            throws Exception {
        if (seen.contains(entry.getId())) {
            return "";
        }
        seen.add(entry.getId());
        String        target     = args[0];
        String        namePrefix = (args.length > 1)
                                   ? args[1]
                                   : null;

        String        linkText   = ((namePrefix != null)
                                    ? namePrefix
                                    : "") + getEntryDisplayName(entry);
        StringBuilder sb         = new StringBuilder();
        String        entryId    = entry.getId();
        String        icon       = getPageHandler().getIconUrl(request,
                                       entry);
        String        event;
        String        uid = "link_" + HtmlUtils.blockCnt++;
        String folderClickUrl =
            request.entryUrl(getRepository().URL_ENTRY_SHOW, entry) + "&"
            + HtmlUtils.args(new String[] {
            ARG_NOREDIRECT, "true", ARG_OUTPUT,
            request.getString(ARG_OUTPUT, "inline"), ATTR_TARGET, target,
            ARG_ALLENTRIES, request.getString(ARG_ALLENTRIES, "true"),
            ARG_ENTRYTYPE, request.getString(ARG_ENTRYTYPE, ""),
            ARG_SELECTTYPE, request.getString(ARG_SELECTTYPE, "")
        });

        String  message   = entry.isGroup()
                            ? "Click to open folder"
                            : "Click to view contents";
        boolean showArrow = true;
        String  prefix    = ( !showArrow
                              ? HtmlUtils.img(
                                  getRepository().getIconUrl(ICON_BLANK), "",
                                  HtmlUtils.attr(HtmlUtils.ATTR_WIDTH, "10"))
                              : HtmlUtils.img(
                                  getRepository().getIconUrl(
                                      ICON_TOGGLEARROWRIGHT), msg(message),
                                          HtmlUtils.id("img_" + uid)
                                          + HtmlUtils.onMouseClick(
                                              HtmlUtils.call(
                                                  "folderClick",
                                                  HtmlUtils.comma(
                                                      HtmlUtils.squote(uid),
                                                      HtmlUtils.squote(
                                                          folderClickUrl), HtmlUtils.squote(
                                                          getIconUrl(
                                                              ICON_TOGGLEARROWDOWN)))))));


        String img = prefix + HtmlUtils.space(1) + HtmlUtils.img(icon);

        sb.append(img);
        sb.append(HtmlUtils.space(1));

        String type      = request.getString(ARG_SELECTTYPE, "");
        String elementId = entry.getId();
        String value     = entry.isGroup()
                           ? ((Entry) entry).getName()
                           : getEntryDisplayName(entry);
        value = value.replace("'", "\\'");
        sb.append(HtmlUtils.mouseClickHref(HtmlUtils.call("selectClick",
                HtmlUtils.comma(HtmlUtils.squote(target),
                                HtmlUtils.squote(entry.getId()),
                                HtmlUtils.squote(value),
                                HtmlUtils.squote(type))), linkText));

        sb.append(HtmlUtils.br());
        sb.append(HtmlUtils.div("",
                                HtmlUtils.attrs(HtmlUtils.ATTR_STYLE,
                                    HtmlUtils.STYLE_HIDDEN,
                                    HtmlUtils.ATTR_CLASS,
                                    CSS_CLASS_FOLDER_BLOCK,
                                    HtmlUtils.ATTR_ID, uid)));

        return sb.toString();
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param contents _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result makeAjaxResult(Request request, String contents)
            throws Exception {
        StringBuilder xml = new StringBuilder("<content>\n");
        XmlUtils.appendCdata(xml, contents);
        xml.append("\n</content>");

        return new Result("", xml, "text/xml");
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param output _more_
     *
     * @return _more_
     */
    public List<Link> getNextPrevLinks(Request request, Entry entry,
                                       OutputType output) {
        Link       link;
        List<Link> links = new ArrayList<Link>();

        link = new Link(request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                         entry, ARG_OUTPUT,
                                         output.toString(), ARG_PREVIOUS,
                                         "true"), ICON_LEFT,
                                             "View Previous Entry");

        //        link.setLinkType(OutputType.TYPE_TOOLBAR);
        link.setLinkType(OutputType.TYPE_VIEW);
        links.add(link);
        link = new Link(request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                         entry, ARG_OUTPUT,
                                         output.toString(), ARG_NEXT,
                                         "true"), ICON_RIGHT,
                                             "View Next Entry");
        link.setLinkType(OutputType.TYPE_VIEW);
        //        link.setLinkType(OutputType.TYPE_TOOLBAR);
        links.add(link);

        return links;
    }



    /**
     * _more_
     *
     * @param buffer _more_
     *
     * @throws Exception _more_
     */
    public void addToSettingsForm(Appendable buffer) throws Exception {}

    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    public void applySettings(Request request) throws Exception {}


    /** _more_ */
    public static int entryCnt = 0;


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public String getSortLinks(Request request) {
        StringBuilder sb           = new StringBuilder();
        String oldOrderBy = request.getString(ARG_ORDERBY, SORTBY_FROMDATE);
        String        oldAscending = request.getString(ARG_ASCENDING,
                                         "false");

        //J--
        String[] order = {
            SORTBY_NAME, "true", msg("Name") + getRepository().getIconImage(ICON_UPARROW),  "Sort by name A-Z", 
            SORTBY_NAME, "false", msg("Name")  + getRepository().getIconImage(ICON_DOWNARROW), "Sort by name Z-A", 
            SORTBY_FROMDATE, "false", msg("Date")  + getRepository().getIconImage(ICON_UPARROW),"Sort by date newer to older", 
            SORTBY_FROMDATE, "true",   msg("Date") + getRepository().getIconImage(ICON_DOWNARROW),   "Sort by date older to newer", 
            SORTBY_CREATEDATE, "true",   msg("Created") + getRepository().getIconImage(ICON_UPARROW),  "Sort by created date older to newer", 
            SORTBY_CREATEDATE, "false",  msg("Created")            + getRepository().getIconImage(ICON_DOWNARROW),   "Sort by created date newer to older", 
            SORTBY_SIZE, "true",  msg("Size") + getRepository().getIconImage(ICON_UPARROW),"Sort by size smallest to largest", 
            SORTBY_SIZE, "false",  msg("Size") + getRepository().getIconImage(ICON_DOWNARROW),  "Sort by size largest to smallest",
            SORTBY_TYPE, "true",  msg("Type") + getRepository().getIconImage(ICON_UPARROW),"Sort by type A-Z", 
            SORTBY_TYPE, "false",  msg("Type") + getRepository().getIconImage(ICON_DOWNARROW),  "Sort by type Z-A",
        };
        //J++

        if (request.isMobile()) {
            sb.append(HtmlUtils.br());
        }
        sb.append(HtmlUtils.span(msgLabel("Sort"),
                                 HtmlUtils.cssClass("sortlinkoff")));
        String entryIds = request.getString(ARG_ENTRYIDS, (String) null);
        //Swap out the long value
        if (entryIds != null) {
            String extraId = getRepository().getGUID();
            request.put(
                ARG_ENTRYIDS,
                getRepository().getSessionManager().putSessionExtra(
                    entryIds));
        }

        int cnt = 0;
        for (int i = 0; i < order.length; i += 4) {
            if ((cnt > 0) && (cnt % 2) == 0) {
                sb.append(HtmlUtils.space(1));
            }
            cnt++;
            if (Misc.equals(order[i], oldOrderBy)
                    && Misc.equals(order[i + 1], oldAscending)) {
                sb.append(HtmlUtils.span(order[i + 2],
                                         HtmlUtils.cssClass("sortlinkon")));
            } else {
                request.put(ARG_ORDERBY, order[i]);
                request.put(ARG_ASCENDING, order[i + 1]);
                request.put(ARG_SHOWENTRYSELECTFORM, "true");
                String url = request.getUrl();
                sb.append(
                    HtmlUtils.span(
                        HtmlUtils.href(url, order[i + 2]),
                        HtmlUtils.title(order[i + 3])
                        + HtmlUtils.cssClass("sortlinkoff")));
            }
        }

        if (entryIds != null) {
            request.put(ARG_ENTRYIDS, entryIds);
        }

        request.remove(ARG_SHOWENTRYSELECTFORM);
        request.put(ARG_ORDERBY, oldOrderBy);
        request.put(ARG_ASCENDING, oldAscending);

        return sb.toString();

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     * @param hideIt _more_
     * @param dummyEntryName _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String[] getEntryFormStart(Request request, List entries,
                                      boolean hideIt, String dummyEntryName)
            throws Exception {

        if (hideIt) {
            hideIt = !request.get(ARG_SHOWENTRYSELECTFORM, false);
        }



        String        base   = "toggleentry" + (entryCnt++);
        String        formId = "entryform_" + (HtmlUtils.blockCnt++);
        StringBuilder formSB = new StringBuilder("");

        //FOR NOW - 
        //        formSB.append(request.formPost(getRepository().URL_ENTRY_GETENTRIES,
        //                                   HtmlUtils.id(formId)));
        formSB.append(request.formPost(getRepository().URL_ENTRY_GETENTRIES,
                                       HtmlUtils.id(formId)));


        long t1 = System.currentTimeMillis();
        List<Link> links = getRepository().getOutputLinks(
                               request,
                               new State(
                                   getEntryManager().getDummyGroup(
                                       dummyEntryName), entries));


        long t2 = System.currentTimeMillis();
        //        System.err.println("getOutputLinks:" + (t2-t1));
        List<String> linkCategories = new ArrayList<String>();
        Hashtable<String, List<HtmlUtils.Selector>> linkMap =
            new Hashtable<String, List<HtmlUtils.Selector>>();
        linkCategories.add("File");
        linkMap.put("File", new ArrayList<HtmlUtils.Selector>());

        linkCategories.add("Edit");
        linkMap.put("Edit", new ArrayList<HtmlUtils.Selector>());


        for (Link link : links) {
            OutputType outputType = link.getOutputType();
            if (outputType == null) {
                continue;
            }
            String category;
            if (outputType.getIsFile()) {
                category = "File";
            } else if (outputType.getIsEdit()) {
                category = "Edit";
            } else {
                continue;
                //Skip view items
                //                category = "View";
            }
            List<HtmlUtils.Selector> linksForCategory = linkMap.get(category);

            if (linksForCategory == null) {
                linksForCategory = new ArrayList<HtmlUtils.Selector>();
                linkCategories.add(category);
                linkMap.put(category, linksForCategory);
            }

            String icon = link.getIcon();
            if (icon == null) {
                icon = getRepository().getIconUrl(ICON_BLANK);
            }
            linksForCategory.add(
                new HtmlUtils.Selector(
                    outputType.getLabel(), outputType.getId(), icon, 20));
        }

        ArrayList<HtmlUtils.Selector> tfos =
            new ArrayList<HtmlUtils.Selector>();
        //        selectSB.append(msgLabel("Apply action"));

        tfos.add(new HtmlUtils.Selector("Apply action", "", null, 5, false));
        for (String category : linkCategories) {
            List<HtmlUtils.Selector> linksForCategory = linkMap.get(category);
            if (linksForCategory.size() == 0) {
                continue;
            }
            tfos.add(new HtmlUtils.Selector(category, "", null, 0, true));
            tfos.addAll(linksForCategory);
        }


        StringBuilder selectSB  = new StringBuilder();
        StringBuilder actionsSB = new StringBuilder();

        actionsSB.append(
            HtmlUtils.select(
                ARG_OUTPUT, tfos, (List<String>) null,
                HtmlUtils.cssClass("entry-action-list")));
        actionsSB.append(HtmlUtils.space(2));
        actionsSB.append(msgLabel("to"));

        StringBuilder js               = new StringBuilder();

        String        allButtonId      = HtmlUtils.getUniqueId("getall");
        String        selectedButtonId = HtmlUtils.getUniqueId("getselected");
        actionsSB.append(HtmlUtils.submit(msg("Selected"), "getselected",
                                          HtmlUtils.id(selectedButtonId)));
        actionsSB.append(HtmlUtils.space(1));
        actionsSB.append(HtmlUtils.submit(msg("All"), "getall",
                                          HtmlUtils.id(allButtonId)));
        js.append(JQuery.buttonize(JQuery.id(allButtonId)));
        js.append(JQuery.buttonize(JQuery.id(selectedButtonId)));

        String sortLinks = getSortLinks(request);

        selectSB.append(HtmlUtils.leftRightBottom(sortLinks,
                actionsSB.toString(), ""));
        String arrowImg = getRepository().getIconImage(hideIt
                ? "/icons/application_side_expand.png"
                : "/icons/application_side_contract.png", "title",
                    msg("Show/Hide Form"), "id", base + "img");
        String linkLabel = arrowImg;
        String linkExtra = HtmlUtils.cssClass("ramadda-entries-link");
        String link = HtmlUtils.jsLink(HtmlUtils.onMouseClick(base
                          + ".groupToggleVisibility()"), linkLabel,
                              linkExtra);
        String selectId = base + "select";
        formSB.append(HtmlUtils.div(selectSB.toString(),
                                    HtmlUtils.cssClass("entry-list-form")
                                    + HtmlUtils.id(selectId) + (hideIt
                ? HtmlUtils.style("display:none;")
                : "")));

        js.append(HtmlUtils.callln(base + "= new EntryFormList",
                                   HtmlUtils.comma(HtmlUtils.squote(formId),
                                       HtmlUtils.squote(base + "img"),
                                       HtmlUtils.squote(selectId), (hideIt
                ? "0"
                : "1"))));
        formSB.append(HtmlUtils.script(js.toString()));




        return new String[] { link, base, formSB.toString() };

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param htmlSB _more_
     * @param jsSB _more_
     * @param showDetails _more_
     *
     * @throws Exception _more_
     */
    public void addEntryTableRow(Request request, Entry entry,
                                 Appendable htmlSB, Appendable jsSB,
                                 boolean showDetails)
            throws Exception {
        String rowId        = HtmlUtils.getUniqueId("entryrow_");
        String cbxId        = HtmlUtils.getUniqueId("entry_");
        String cbxArgId     = ARG_SELENTRY;
        String cbxArgValue  = entry.getId();
        String cbxWrapperId = HtmlUtils.getUniqueId("cbx_");
        jsSB.append("new EntryRow(");
        HtmlUtils.squote(jsSB, entry.getId());
        jsSB.append(",");
        HtmlUtils.squote(jsSB, rowId);
        jsSB.append(",");
        HtmlUtils.squote(jsSB, cbxId);
        jsSB.append(",");
        HtmlUtils.squote(jsSB, cbxWrapperId);
        jsSB.append(",");
        jsSB.append(Boolean.toString(showDetails));
        jsSB.append(");\n");


        StringBuilder attrSB = new StringBuilder();
        HtmlUtils.id(attrSB, cbxId);
        HtmlUtils.clazz(attrSB, "ramadda-entry-select");
        HtmlUtils.attr(
            attrSB, HtmlUtils.ATTR_TITLE,
            "Shift-click: select range; Control-click: toggle all");
        //Spool this out to save concats
        attrSB.append(HtmlUtils.ATTR_ONCLICK);
        attrSB.append("=\"");
        attrSB.append("entryRowCheckboxClicked(");
        attrSB.append("event, ");
        HtmlUtils.squote(attrSB, cbxId);
        attrSB.append(");\"  ");
        String cbx = HtmlUtils.checkbox(cbxArgId, cbxArgValue, false,
                                        attrSB.toString());
        decorateEntryRow(request, entry, htmlSB,
                         getEntryManager().getAjaxLink(request, entry,
                             getEntryDisplayName(entry)), rowId, cbx,
                                 showDetails);

    }


    /**
     *     _more_
     *
     *     @param request _more_
     *     @param formId _more_
     *
     *     @return _more_
     */
    public String getEntryFormEnd(Request request, String formId) {
        return HtmlUtils.formClose();
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param entries _more_
     * @param doForm _more_
     * @param showCrumbs _more_
     * @param showDetails _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getEntriesList(Request request, Appendable sb,
                                 List entries, boolean doForm,
                                 boolean showCrumbs, boolean showDetails)
            throws Exception {

        long   t1          = System.currentTimeMillis();
        String link        = "";
        String base        = "";
        String afterHeader = "";

        if (doForm) {
            long x1 = System.currentTimeMillis();
            String[] tuple = getEntryFormStart(request, entries, true,
                                 "Search Results");
            long x2 = System.currentTimeMillis();
            //            System.err.println("entryFormStart:" + (x2-x1));
            link        = tuple[0];
            base        = tuple[1];
            afterHeader = tuple[2];
        }



        String prefix = (showDetails
                         ? "entry-list"
                         : "entry-tree");
        HtmlUtils.open(sb, HtmlUtils.TAG_DIV, "class", prefix + "-block");
        boolean isMobile       = request.isMobile();
        boolean showDate       = true;
        boolean showCreateDate = getPageHandler().showEntryTableCreateDate();
        if (isMobile) {
            showCreateDate = false;
            showDate       = false;
        }

        showNext(request, entries.size(), sb);

        if (showDetails) {
            String cls = isMobile
                         ? "entry-list-header-mobile"
                         : "entry-list-header";
            sb.append(
                "<table class=\"entry-list-header\" border=0 cellpadding=0 cellspacing=0 width=100%><tr>");
            sb.append(
                "<td align=center valign=center width=20><div class=\"entry-list-header-toggle\">");
            sb.append(link);
            sb.append("</div></td>");


            String  sortLink;
            Request tmpRequest = request.cloneMe();
            tmpRequest.put(ARG_SHOWENTRYSELECTFORM, "true");
            tmpRequest.put(ARG_ORDERBY, SORTBY_NAME);
            tmpRequest.put(ARG_ASCENDING,
                           Boolean.toString( !request.get(ARG_ASCENDING,
                               false)));
            sortLink = tmpRequest.getUrl();
            sb.append(
                HtmlUtils.tag(
                    HtmlUtils.TAG_TD,
                    HtmlUtils.attrs(
                        HtmlUtils.ATTR_CLASS,
                        "entry-list-header-column"), HtmlUtils.href(
                            sortLink, msg("Name"))));

            tmpRequest.put(ARG_ORDERBY, SORTBY_FROMDATE);
            sortLink = tmpRequest.getUrl();
            if (showDate) {
                sb.append(
                    HtmlUtils.tag(
                        HtmlUtils.TAG_TD,
                        HtmlUtils.attrs(
                            HtmlUtils.ATTR_WIDTH, WIDTH_DATE,
                            HtmlUtils.ATTR_CLASS,
                            "entry-list-header-column"), HtmlUtils.href(
                                sortLink, msg("Date"))));
            }

            if (showCreateDate) {
                tmpRequest.put(ARG_ORDERBY, SORTBY_CREATEDATE);
                sortLink = tmpRequest.getUrl();
                sb.append(
                    HtmlUtils.tag(
                        HtmlUtils.TAG_TD,
                        HtmlUtils.attrs(
                            HtmlUtils.ATTR_WIDTH, WIDTH_DATE,
                            HtmlUtils.ATTR_CLASS,
                            "entry-list-header-column"), HtmlUtils.href(
                                sortLink, msg("Created"))));


            }

            if ( !isMobile) {
                tmpRequest.put(ARG_ORDERBY, SORTBY_SIZE);
                sortLink = tmpRequest.getUrl();
                sb.append(
                    HtmlUtils.tag(
                        HtmlUtils.TAG_TD,
                        HtmlUtils.attrs(
                            HtmlUtils.ATTR_WIDTH, WIDTH_SIZE,
                            HtmlUtils.ATTR_CLASS,
                            "entry-list-header-column"), HtmlUtils.href(
                                sortLink, msg("Size"))));
                tmpRequest.put(ARG_ORDERBY, SORTBY_TYPE);
                sortLink = tmpRequest.getUrl();
                sb.append(
                    HtmlUtils.tag(
                        HtmlUtils.TAG_TD,
                        HtmlUtils.attrs(
                            HtmlUtils.ATTR_WIDTH, WIDTH_KIND,
                            HtmlUtils.ATTR_CLASS,
                            "entry-list-header-column-last"), HtmlUtils.href(
                                sortLink, msg("Type"))));
            }
            sb.append("</tr></table>");

            link = "";
            sb.append(afterHeader);
        }

        HtmlUtils.open(sb, HtmlUtils.TAG_DIV, "class", prefix);
        boolean        doCategories = request.get(ARG_SHOWCATEGORIES, false);
        CategoryBuffer cb           = new CategoryBuffer();
        int            cnt          = 0;
        StringBuilder  jsSB         = new StringBuilder();
        StringBuilder  cbxSB        = new StringBuilder();

        long           t2           = System.currentTimeMillis();

        for (Entry entry : (List<Entry>) entries) {

            cbxSB.setLength(0);
            String rowId        = base + (cnt++);
            String cbxArgId     = ARG_SELENTRY;
            String cbxArgValue  = entry.getId();
            String cbxId        = HtmlUtils.getUniqueId("entry_");
            String cbxWrapperId = HtmlUtils.getUniqueId("checkboxwrapper_");

            jsSB.append("new EntryRow(");
            HtmlUtils.squote(jsSB, entry.getId());
            jsSB.append(",");
            HtmlUtils.squote(jsSB, rowId);
            jsSB.append(",");
            HtmlUtils.squote(jsSB, cbxId);
            jsSB.append(",");
            HtmlUtils.squote(jsSB, cbxWrapperId);
            jsSB.append(",");
            jsSB.append(showDetails
                        ? "true"
                        : "false");
            jsSB.append(");\n");

            if (doForm) {
                HtmlUtils.hidden(cbxSB, ARG_ALLENTRY, entry.getId(), "");
                HtmlUtils.open(cbxSB, HtmlUtils.TAG_SPAN, "id", cbxWrapperId);

                HtmlUtils.dangleOpen(cbxSB, HtmlUtils.TAG_INPUT);
                HtmlUtils.id(cbxSB, cbxId);
                HtmlUtils.attr(cbxSB, HtmlUtils.ATTR_STYLE, "display:none;");
                HtmlUtils.clazz(cbxSB, "ramadda-entry-select");
                HtmlUtils.attr(
                    cbxSB, HtmlUtils.ATTR_TITLE,
                    "Shift-click: select range; Control-click: toggle all");
                HtmlUtils.attr(cbxSB, HtmlUtils.ATTR_ONCLICK,
                               "entryRowCheckboxClicked(event, '" + cbxId
                               + "');");
                HtmlUtils.attrs(cbxSB, HtmlUtils.ATTR_TYPE,
                                HtmlUtils.TYPE_CHECKBOX, HtmlUtils.ATTR_NAME,
                                cbxArgId, HtmlUtils.ATTR_VALUE, cbxArgValue);
                cbxSB.append(">");
                HtmlUtils.close(cbxSB, HtmlUtils.TAG_SPAN);
            }


            String crumbs = "";
            if (showCrumbs) {
                crumbs = getPageHandler().getBreadCrumbs(request,
                        entry.getParentEntry(), null, null, 60);
                crumbs = HtmlUtils.makeToggleInline("",
                        Utils.concatString(crumbs, BREADCRUMB_SEPARATOR_PAD),
                        false);
            }

            String displayName = getEntryDisplayName(entry);


            EntryLink entryLink = getEntryManager().getAjaxLink(request,
                                      entry, displayName, null, true, crumbs);


            Appendable buffer = cb.get(doCategories
                                       ? entry.getTypeHandler().getCategory(
                                           entry).getLabel().toString()
                                       : "");

            decorateEntryRow(request, entry, buffer, entryLink, rowId,
                             cbxSB.toString(), showDetails);
        }

        long t3 = System.currentTimeMillis();

        for (String category : cb.getCategories()) {
            if (doCategories) {
                if (category.length() > 0) {
                    sb.append(subHeader(category));
                }
                HtmlUtils.div(sb, cb.get(category).toString(),
                              HtmlUtils.cssClass(prefix));
                sb.append(HtmlUtils.p());
            } else {
                sb.append(cb.get(category));
            }
        }

        if (doForm) {
            sb.append(getEntryFormEnd(request, base));
        }
        HtmlUtils.close(sb, "div");
        HtmlUtils.close(sb, "div");
        HtmlUtils.script(sb, jsSB.toString());
        sb.append("\n\n");

        long t4 = System.currentTimeMillis();

        //        System.err.println("getEntryList: loop:" + (t3 - t2));

        return link;

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param link _more_
     * @param rowId _more_
     * @param extra _more_
     * @param showDetails _more_
     *
     * @throws Exception _more_
     */
    protected void decorateEntryRow(Request request, Entry entry,
                                    Appendable sb, EntryLink link,
                                    String rowId, String extra,
                                    boolean showDetails)
            throws Exception {

        if (rowId == null) {
            rowId = HtmlUtils.getUniqueId("entryrow_");
        }


        sb.append("<div id=\"");
        sb.append(rowId);
        sb.append("\"  class=\"");
        sb.append(showDetails
                  ? CSS_CLASS_ENTRY_LIST_ROW
                  : CSS_CLASS_ENTRY_TREE_ROW);
        sb.append("\" ");
        sb.append(HtmlUtils.ATTR_ONCLICK);
        sb.append("=\"entryRowClick(event, '");
        sb.append(rowId);
        sb.append("');\" ");
        sb.append(HtmlUtils.ATTR_ONMOUSEOVER);
        sb.append("=\"entryRowOver('");
        sb.append(rowId);
        sb.append("'); \" ");
        sb.append(HtmlUtils.ATTR_ONMOUSEOUT);
        sb.append("=\"entryRowOut('");
        sb.append(rowId);
        sb.append("'); \"  >");
        sb.append(
            "<table class=\"entry-row-table\" border=\"0\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\"><tr><td>");
        sb.append(extra);
        sb.append(link.getLink());
        sb.append("</td>");

        boolean showDate       = !request.get(ARG_TREEVIEW, false);
        boolean showCreateDate = getPageHandler().showEntryTableCreateDate();
        if (request.isMobile()) {
            showDate       = false;
            showCreateDate = false;
        }

        if ( !showDetails) {
            showDate       = false;
            showCreateDate = false;
        }

        boolean isMobile = request.isMobile();

        if (showDate) {
            String dttm = getDateHandler().formatDateShort(request, entry,
                              entry.getStartDate());
            HtmlUtils.open(sb, HtmlUtils.TAG_TD, HtmlUtils.ATTR_WIDTH,
                           WIDTH_DATE, HtmlUtils.ATTR_ALIGN, "right");
            HtmlUtils.div(sb, dttm,
                          HtmlUtils.cssClass(CSS_CLASS_ENTRY_ROW_LABEL));
            HtmlUtils.close(sb, HtmlUtils.TAG_TD);
        }

        if (showCreateDate) {
            String dttm = getDateHandler().formatDateShort(request, entry,
                              entry.getCreateDate());
            HtmlUtils.open(sb, HtmlUtils.TAG_TD, HtmlUtils.ATTR_WIDTH,
                           WIDTH_DATE, HtmlUtils.ATTR_ALIGN, "right");
            HtmlUtils.div(sb, dttm,
                          HtmlUtils.cssClass(CSS_CLASS_ENTRY_ROW_LABEL));
            HtmlUtils.close(sb, HtmlUtils.TAG_TD);
        }

        if ( !isMobile && showDetails) {
            HtmlUtils.open(sb, HtmlUtils.TAG_TD, "width", WIDTH_SIZE,
                           "align", "right", "class",
                           CSS_CLASS_ENTRY_ROW_LABEL);
            if (entry.getResource().isFile()) {
                sb.append(
                    formatFileLength(entry.getResource().getFileSize()));
            } else {
                sb.append("---");
            }
            HtmlUtils.close(sb, HtmlUtils.TAG_TD);
        }

        if ( !isMobile && showDetails) {
            HtmlUtils.open(sb, HtmlUtils.TAG_TD, "width", WIDTH_KIND,
                           "align", "right", "class",
                           CSS_CLASS_ENTRY_ROW_LABEL);
            HtmlUtils.div(
                sb, entry.getTypeHandler().getFileTypeDescription(
                    request, entry), HtmlUtils.attrs(
                    "style", "max-width:190px; overflow-x: hidden;"));
            HtmlUtils.close(sb, HtmlUtils.TAG_TD);
        }


        if (showDetails) {
            HtmlUtils.open(sb, HtmlUtils.TAG_TD, "width", "1%", "align",
                           "right", "class", CSS_CLASS_ENTRY_ROW_LABEL);
            sb.append(HtmlUtils.space(1));
            sb.append("  ");
            HtmlUtils.div(sb,
                          getRepository().getIconImage(ICON_BLANK, "width",
                              "10", "id",
                              "entrymenuarrow_"
                              + rowId), HtmlUtils.clazz("entrymenuarrow"));
            HtmlUtils.close(sb, HtmlUtils.TAG_TD);
        }

        HtmlUtils.close(sb, HtmlUtils.TAG_TR);
        HtmlUtils.close(sb, HtmlUtils.TAG_TABLE);
        HtmlUtils.close(sb, HtmlUtils.TAG_DIV);
        sb.append(link.getFolderBlock());
    }





    /**
     * _more_
     *
     * @param request _more_
     * @param output _more_
     * @param links _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List getHeader(Request request, OutputType output,
                             List<Link> links)
            throws Exception {

        List   items          = new ArrayList();
        Object initialMessage = request.remove(ARG_MESSAGE);
        String onLinkTemplate =
            getRepository().getPageHandler().getTemplateProperty(request,
                "ramadda.template.sublink.on", "");
        String offLinkTemplate =
            getRepository().getPageHandler().getTemplateProperty(request,
                "ramadda.template.sublink.off", "");
        for (Link link : links) {
            OutputType outputType = link.getOutputType();
            String     url        = link.getUrl();
            String     template;
            if (Misc.equals(outputType, output)) {
                template = onLinkTemplate;
            } else {
                template = offLinkTemplate;
            }
            String html = template.replace("${label}", link.getLabel());
            html = html.replace("${url}", url);
            html = html.replace("${root}", getRepository().getUrlBase());
            items.add(html);
        }
        if (initialMessage != null) {
            request.put(ARG_MESSAGE, initialMessage);
        }

        return items;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param typeHandlers _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result listTypes(Request request, List<TypeHandler> typeHandlers)
            throws Exception {
        return notImplemented("listTypes");
    }





    /**
     * protected Result listTags(Request request, List<Tag> tags)
     *       throws Exception {
     *   return notImplemented("listTags");
     * }
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result listAssociations(Request request) throws Exception {
        return notImplemented("listAssociations");
    }





    /** _more_ */
    public static final String RESOURCE_ENTRYTEMPLATE = "entrytemplate.txt";

    /** _more_ */
    public static final String RESOURCE_GROUPTEMPLATE = "grouptemplate.txt";


    /** _more_ */
    public static final String PROP_ENTRY = "entry";

    /** _more_ */
    public static final String PROP_REQUEST = "request";



    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getImageUrl(Request request, Entry entry) throws Exception {
        return getImageUrl(request, entry, false);
    }


    /** _more_ */
    private static int imageVersionCnt = 0;

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param addVersion _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getImageUrl(Request request, Entry entry,
                              boolean addVersion)
            throws Exception {
        if ( !entry.isImage()) {
            if (true) {
                return null;
            }

            /*
            if (entry.hasAreaDefined()) {
                return request.makeUrl(repository.URL_GETMAP, ARG_SOUTH,
                                   "" + entry.getSouth(), ARG_WEST,
                                   "" + entry.getWest(), ARG_NORTH,
                                   "" + entry.getNorth(), ARG_EAST,
                                   "" + entry.getEast());
                                   }*/
            return null;
        }


        String url = entry.getResource().getPath();
        if (entry.getResource().isUrl()) {
            return entry.getTypeHandler().getPathForEntry(request, entry);
        }
        if (url != null) {
            if (url.startsWith("ftp:") || url.startsWith("http:")) {
                return url;
            }
        }


        return HtmlUtils.url(request.makeUrl(repository.URL_ENTRY_GET) + "/"
                             + (addVersion
                                ? ("v" + (imageVersionCnt++))
                                : "") + getStorageManager().getFileTail(
                                    entry), ARG_ENTRYID, entry.getId());
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param onlyIfWeHaveThem _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    public StringBuilder getCommentBlock(Request request, Entry entry,
                                         boolean onlyIfWeHaveThem)
            throws Exception {
        StringBuilder sb = new StringBuilder();
        if (getRepository().getCommentsEnabled()) {
            List<Comment> comments =
                getRepository().getCommentManager().getComments(request,
                    entry);
            if ( !onlyIfWeHaveThem || (comments.size() > 0)) {
                sb.append(getPageHandler().getCommentHtml(request, entry));
            }
        }

        return sb;

    }


    /**
     * _more_
     *
     * @return _more_
     */
    public int getMaxEntryCount() {
        return -1;
    }

    /**
     *  Set the MaxConnections property.
     *
     *  @param value The new value for MaxConnections
     */
    public void setMaxConnections(int value) {
        this.maxConnections = value;
    }


    /**
     *  Get the MaxConnections property.
     *
     *  @return The MaxConnections
     */
    public int getMaxConnections() {
        return this.maxConnections;
    }

    /**
     *  Set the NumberOfConnections property.
     *
     *  @param value The new value for NumberOfConnections
     */
    public void setNumberOfConnections(int value) {
        this.numberOfConnections = value;
    }

    /**
     *  Get the NumberOfConnections property.
     *
     *  @return The NumberOfConnections
     */
    public int getNumberOfConnections() {
        return this.numberOfConnections;
    }

    /**
     * _more_
     */
    public void incrNumberOfConnections() {
        numberOfConnections++;
        totalCalls++;
    }



    /**
     * _more_
     */
    public void decrNumberOfConnections() {
        numberOfConnections--;
        if (numberOfConnections < 0) {
            numberOfConnections = 0;
        }
    }

    /** _more_ */
    public static final String CLASS_TAB_CONTENT = "tab_content";

    /** _more_ */
    public static final String CLASS_TAB_CONTENTS = "tab_contents";

    /** _more_ */
    private static int tabCnt = 0;


    /**
     * Make tabs
     *
     * @param titles   the titles for the tabs
     * @param tabs     the list of tabs (entries)
     * @param skipEmpty  skip empty tab flag
     *
     * @return  the tabs HTML
     */
    public static String makeTabs(List titles, List tabs, boolean skipEmpty) {
        return makeTabs(titles, tabs, skipEmpty, false);
    }

    /**
     * Make tabs
     *
     * @param titles   the titles for the tabs
     * @param tabs     the list of tabs (entries)
     * @param skipEmpty  skip empty tab flag
     * @param useCookies  use cookies flag
     *
     * @return  the tabs HTML
     */
    public static String makeTabs(List titles, List tabs, boolean skipEmpty,
                                  boolean useCookies) {
        StringBuilder tabHtml = new StringBuilder();
        String        tabId   = "tabId" + (tabCnt++);
        tabHtml.append("\n\n");
        HtmlUtils.open(tabHtml, HtmlUtils.TAG_DIV, "id", tabId, "class",
                       "ui-tabs");
        HtmlUtils.open(tabHtml, HtmlUtils.TAG_UL);
        int cnt = 1;
        for (int i = 0; i < titles.size(); i++) {
            String title       = titles.get(i).toString();
            String tabContents = tabs.get(i).toString();
            if (skipEmpty
                    && ((tabContents == null)
                        || (tabContents.length() == 0))) {
                continue;
            }
            tabHtml.append("<li><a href=\"#" + tabId + "-" + (cnt++) + "\">"
                           + title + "</a></li>");
        }
        HtmlUtils.close(tabHtml, HtmlUtils.TAG_UL);
        cnt = 1;
        for (int i = 0; i < titles.size(); i++) {
            String tabContents = tabs.get(i).toString();
            if (skipEmpty
                    && ((tabContents == null)
                        || (tabContents.length() == 0))) {
                continue;
            }
            tabHtml.append(HtmlUtils.div(tabContents, HtmlUtils.id(tabId
                    + "-" + (cnt++)) + HtmlUtils.cssClass("ui-tabs-hide")));
            tabHtml.append("\n");
        }

        tabHtml.append(HtmlUtils.close(HtmlUtils.TAG_DIV));
        tabHtml.append("\n");
        String args = "activate: HtmlUtil.tabLoaded";
        if (useCookies) {
            args += ",\ncookie: {expires:1}";
        }
        tabHtml.append(HtmlUtils.script("\njQuery(function(){\njQuery('#"
                                        + tabId + "').tabs({" + args
                                        + "})});\n\n"));

        return tabHtml.toString();
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param arg _more_
     * @param dflt _more_
     * @param width _more_
     *
     * @return _more_
     */
    public String htmlInput(Request request, String arg, String dflt,
                            int width) {
        return HtmlUtils.input(arg, request.getString(arg, dflt),
                               HtmlUtils.attr(HtmlUtils.ATTR_SIZE,
                                   "" + width));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param arg _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String htmlInput(Request request, String arg, String dflt) {
        return htmlInput(request, arg, dflt, 5);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param parent _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean canAddTo(Request request, Entry parent) throws Exception {
        return getEntryManager().canAddTo(request, parent);
    }


    /**
     * Did the user choose an entry to publish to
     *
     * @param request _more_
     *
     * @return _more_
     */
    public boolean doingPublish(Request request) {
        return request.defined(ARG_PUBLISH_ENTRY + "_hidden");
    }

    /**
     * If the user is not anonymous then add the "Publish to" widget.
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param header _more_
     *
     * @throws Exception _more_
     */
    public void addPublishWidget(Request request, Entry entry, Appendable sb,
                                 String header)
            throws Exception {
        addPublishWidget(request, entry, sb, header, true);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param header _more_
     * @param addNameField _more_
     *
     * @throws Exception _more_
     */
    public void addPublishWidget(Request request, Entry entry, Appendable sb,
                                 String header, boolean addNameField)
            throws Exception {
        addPublishWidget(request, entry, sb, header, addNameField, true);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param header _more_
     * @param addNameField _more_
     * @param addMetadataField _more_
     *
     * @throws Exception _more_
     */
    public void addPublishWidget(Request request, Entry entry, Appendable sb,
                                 String header, boolean addNameField,
                                 boolean addMetadataField)
            throws Exception {
        if (request.getUser().getAnonymous()) {
            return;
        }

        String entryId = request.getString(ARG_PUBLISH_ENTRY + "_hidden", "");
        String entryName = "";
        if (Utils.stringDefined(entryId)) {
            Entry selectedEntry = getEntryManager().getEntry(request,
                                      entryId);
            if (selectedEntry != null) {
                entryName = selectedEntry.getName();
            }
        }
        StringBuilder publishSB = new StringBuilder();
        sb.append(HtmlUtils.hidden(ARG_PUBLISH_ENTRY + "_hidden", entryId,
                                   HtmlUtils.id(ARG_PUBLISH_ENTRY
                                       + "_hidden")));
        HtmlUtils.row(sb, HtmlUtils.colspan(header, 2));

        String select = OutputHandler.getSelect(request, ARG_PUBLISH_ENTRY,
                            "Select folder", false, null, entry);
        String addMetadata = !addMetadataField
                             ? ""
                             : HtmlUtils.checkbox(ARG_METADATA_ADD,
                                 HtmlUtils.VALUE_TRUE,
                                 request.get(ARG_METADATA_ADD,
                                             false)) + msg("Add properties");
        sb.append(
            HtmlUtils.formEntry(
                msgLabel("Folder"),
                HtmlUtils.disabledInput(
                    ARG_PUBLISH_ENTRY, entryName,
                    HtmlUtils.id(ARG_PUBLISH_ENTRY)
                    + HtmlUtils.SIZE_60) + select + HtmlUtils.space(2)
                                         + addMetadata));

        if (addNameField) {
            sb.append(HtmlUtils.formEntry(msgLabel("Name"),
                                          htmlInput(request,
                                              ARG_PUBLISH_NAME, "", 30)));
        }

    }

    /**
     * _more_
     *
     * @return _more_
     */
    public CalendarOutputHandler getCalendarOutputHandler() {
        try {
            return (CalendarOutputHandler) getRepository().getOutputHandler(
                CalendarOutputHandler.OUTPUT_CALENDAR);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param title _more_
     * @param msg _more_
     *
     * @return _more_
     */
    public Result getErrorResult(Request request, String title, String msg) {
        return new Result(
            title, new StringBuilder(getPageHandler().showDialogError(msg)));
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public String getProductDirName() {
        return "products";
    }


    /** _more_ */
    private TempDir productDir;


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public File getProductDir() throws Exception {
        if (productDir == null) {
            TempDir tempDir =
                getStorageManager().makeTempDir(getProductDirName());
            //keep things around for 7 day  
            tempDir.setMaxAge(1000 * 60 * 60 * getProductDirTTLHours());
            productDir = tempDir;
        }

        return productDir.getDir();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getProductDirTTLHours() {
        return 24 * 7;
    }


    /**
     * _more_
     *
     * @param jobId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public File getWorkDir(Object jobId) throws Exception {
        if (jobId == null) {
            jobId = getRepository().getGUID();
        }
        File theProductDir = new File(IOUtil.joinDir(getProductDir(),
                                 jobId.toString()));
        IOUtil.makeDir(theProductDir);

        return theProductDir;
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public String getServiceFilename(Entry entry) {
        return entry.getName().replace(" ", "_").replace(":", "_");
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    public String getEntryLink(Request request, Entry entry) {
        return getEntryManager().getEntryLink(request, entry);
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String xxxgetEntryLink(Request request, Entry entry)
            throws Exception {
        return getEntryManager().getTooltipLink(request, entry,
                getEntryDisplayName(entry), null);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param makeIfNeeded _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public File getCurrentProcessingDir(Request request, boolean makeIfNeeded)
            throws Exception {
        return getCurrentProcessingDir(request, makeIfNeeded, null);
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    public void clearCurrentProcessingDir(Request request) throws Exception {
        getSessionManager().removeSessionProperty(request, PROP_PROCESSDIR);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param makeIfNeeded _more_
     * @param name _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public File getCurrentProcessingDir(Request request,
                                        boolean makeIfNeeded, String name)
            throws Exception {
        String currentDir =
            (String) getSessionManager().getSessionProperty(request,
                PROP_PROCESSDIR, null);
        File f = ((currentDir == null)
                  ? null
                  : new File(currentDir));
        if ((currentDir == null) || !f.exists()) {
            if (makeIfNeeded) {
                f = getStorageManager().createProcessDir();
                if (name != null) {
                    String xml = "<entry type=\"group\" name=\"" + name
                                 + "\"/>";
                    IOUtil.writeFile(IOUtil.joinDir(f, ".this.ramadda.xml"),
                                     xml);
                }
                getSessionManager().putSessionProperty(request,
                        PROP_PROCESSDIR, f.toString());
            }

            return f;
        }

        return f;
    }





}
