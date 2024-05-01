/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.output;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.map.*;

import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.BufferMapList;
import org.ramadda.util.CategoryBuffer;
import org.ramadda.util.IO;
import org.ramadda.util.JQuery;
import org.ramadda.util.JsonUtil;

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
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import java.util.function.Function;
import java.util.regex.*;
import java.util.zip.*;


/**
 *
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public class OutputHandler extends RepositoryManager implements OutputConstants {

    /** _more_ */
    public static final String PROP_PROCESSDIR = "processdir";


    /** _more_ */
    public static final String WIDTH_DATE = "120";

    /** _more_ */
    public static final String WIDTH_SIZE = "100";

    /** _more_ */
    public static final String WIDTH_KIND = "120";

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
        if (name == null) {
            name = getClass().getSimpleName();
        }
        this.name = name;

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
     *
     * @param request _more_
     * @param type _more_
     * @param parent _more_
      * @return _more_
     */
    public boolean requiresChildrenEntries(Request request, OutputType type,
                                           Entry parent) {
        return true;
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
     * @param children _more_
     *
     * @return    true if showing all
     */
    public boolean showingAll(Request request, List<Entry> children) {
        int cnt = children.size();
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
     *
     * @param request _more_
     * @param children _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void showNext(Request request, List<Entry> children, Appendable sb)
            throws Exception {
        int cnt = children.size();
        showNext(request, cnt, sb);
    }

    /**
     * Show the next bunch
     *
     * @param request   the Request
     * @param cnt       the number to show
     * @param sb        the output
     * @param message _more_
     *
     * @throws Exception  problem showing them
     */
    public void showNext(Request request, int cnt, Appendable sb,
                         String... message)
            throws Exception {
        int max = request.get(ARG_MAX, VIEW_MAX_ROWS);
        showNext(request, cnt, max,  sb, message);
    }


    public boolean shouldShowPaging(Request request, int cnt, int max) {
	return Utils.stringDefined((String)request.getPropertyOrArg(ARG_MARKER)) ||
	    Utils.stringDefined((String)request.getPropertyOrArg(ARG_PREVMARKERS)) ||
	    max>0;
    }


    /**
     *
     * @param request _more_
     * @param cnt _more_
     * @param max _more_
     * @param sb _more_
     * @param message _more_
     *
     * @throws Exception _more_
     */
    public void showNext(Request request, int cnt, int max, Appendable sb,
                         String... message)
            throws Exception {
	if(!shouldShowPaging(request, cnt, max)) {
	    //	    System.err.println("No show paging:" + request);
	    return;
	}

        //      Misc.printStack("Show next", 10);
	List<String> toks = new ArrayList<String>();
	String marker = (String)request.getPropertyOrArg(ARG_MARKER);
	String prevMarkers = (String)request.getPropertyOrArg(ARG_PREVMARKERS);	
	if(Utils.stringDefined(marker) || Utils.stringDefined(prevMarkers)) {
	    HashSet<String> exclude = new HashSet<String>();
	    exclude.add(ARG_MARKER);
	    exclude.add(ARG_PREVMARKERS);		
	    List<String> prevList = Utils.stringDefined(prevMarkers)?Utils.split(prevMarkers,",",true,true):new ArrayList<String>();
	    String fullPrev = Utils.join(prevList,",");
	    String tmp = Utils.join(Utils.Y(prevList),",");	    
	    if(prevList.size()>0) {
		String lastMarker = prevList.remove(prevList.size()-1);
		if(prevList.size()==0) lastMarker = "";
		String formId = HU.getUniqueId("form_");
		sb.append(HU.formPost(request.getRequestPath(),HU.id(formId)));
		request.addFormHiddenArguments(sb, exclude);
		System.err.println("FORM PREV:\n\tprev:"+
				   Utils.join(Utils.Y(prevList),",") +"\n\tmarker:" +Utils.X(lastMarker));
		
		sb.append(HU.hidden(ARG_PREVMARKERS, Utils.join(prevList,",")));
		sb.append(HU.hidden(ARG_MARKER, lastMarker));		
		toks.add(HU.div("Previous",
				HU.onMouseClick("$('#"  + formId +"').submit();") + HU.cssClass("ramadda-clickable ramadda-next-previous ramadda-previous")));
		sb.append(HU.formClose());
	    }
	    if(marker!=null) {
		String formId = HU.getUniqueId("form_");
		sb.append(HU.formPost(request.getRequestPath(),HU.id(formId)));
		request.addFormHiddenArguments(sb, exclude);
		System.err.println("FORM NEXT:\n\tprev:"+
				   tmp +"\n\tmarker:" +Utils.X(marker));

		
		sb.append(HU.hidden(ARG_PREVMARKERS, fullPrev));
		sb.append(HU.hidden(ARG_MARKER, marker));		
		toks.add(HU.div("Next",
				HU.onMouseClick("$('#"  + formId +"').submit();") + HU.cssClass("ramadda-clickable ramadda-next-previous ramadda-next")));
		sb.append(HU.formClose());
	    }

	    HU.div(sb, Utils.join(toks,""),
		   HU.cssClass("ramadda-next-previous-block"));
	    return;
	}

        boolean haveSkip = request.defined(ARG_SKIP);
        boolean haveMax  = request.defined(ARG_MAX);
        boolean show     = ((cnt > 0) && (cnt == max)) || haveSkip || haveMax;
        int     skip     = Math.max(0, request.get(ARG_SKIP, 0));
        if (show) {
	    String label;
            if ((message != null) && (message.length > 0)
                    && (message[0] != null)) {
                label =message[0];
            } else {
                label = "Showing: "  + (skip + 1) + "-"  + (skip + cnt);
            }
	    if (skip > 0) {
		toks.add(HU.href(
					request.getUrl(ARG_SKIP) + "&" + ARG_SKIP + "="
					+ (skip - max), "Previous",
					HU.cssClass("ramadda-next-previous ramadda-previous")					
					//HU.faIcon("fa-caret-left", "title", "View previous")
					));
	    }
	
            //      if (cnt >= max) {
	    toks.add(HU.href(
				    request.getUrl(ARG_SKIP) + "&" + ARG_SKIP + "="
				    + (skip + max), "Next",
				    HU.cssClass("ramadda-next-previous ramadda-next")));
	    //HU.faIcon("fa-caret-right", "title", "View next")));
            //            }
            int moreMax = (int) (max * 1.5);
            if (moreMax < 10) {
                moreMax = 10;
            }
            int lessMax = max / 2;
            if (lessMax < 1) {
                lessMax = 1;
            }
            request = request.cloneMe();
	    toks.add(HU.SPACE2);
            request.put(ARG_MAX, "" + lessMax);
            toks.add(HU.href(request.getUrl(),
                                    HU.faIcon("fa-minus", "title",
                                        "View less")));
	    toks.add(HU.SPACE);
            request.put(ARG_MAX, "" + moreMax);
            toks.add(HU.href(request.getUrl(),
                                    HU.faIcon("fa-plus", "title",
                                        "View more")));

            if (toks.size() > 0) {
		label = HU.div(label,HU.style("margin-right:5px;"));
		sb.append(HU.leftRightBottom(HU.div(Utils.join(toks,""),
						    HU.cssClass("ramadda-next-previous-block")),
					     label,""));
            }
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
                                 + HU.br());
                }
            }

            sb.append(HU.formEntryTop(msgLabel(name),
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
                    group        = (Entry) entry;
                    this.entries = group.getChildren();
                } else {
                    this.entry = entry;
                }
            }

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
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getWikiText(Request request, Entry entry) throws Exception {
        String wikiText = getWikiTextInner(request, entry);
        if (wikiText != null) {
            wikiText = entry.getTypeHandler().preProcessWikiText(request,
                    entry, wikiText);
        }

        return wikiText;
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
    public String getWikiTextInner(Request request, Entry entry)
            throws Exception {
        String description = entry.getDescription();
        String wikiInner   = null;
        //If it begins with <wiki> then it overrides anything else
        if (TypeHandler.isWikiText(description)) {
            if (description.startsWith("<wiki_inner>")) {
                wikiInner = description;
            } else {
                return description;
            }
        }

	String wikiTemplate = null;
        if (wikiTemplate == null) {
            PageStyle pageStyle = request.getPageStyle(entry);
            wikiTemplate = pageStyle.getWikiTemplate(entry);
        }

	if(wikiTemplate==null) {
	     wikiTemplate = entry.getTypeHandler().getWikiTemplate(request,
								   entry);
	}

        if (wikiInner != null) {
            if (wikiTemplate == null) {
                return wikiInner;
            }
            wikiTemplate = wikiTemplate.replace("${innercontent}", wikiInner);
        }

        return wikiTemplate;
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
            url = HU.url(getRepository().URL_ENTRY_SHOW + suffix,
                                ARG_OUTPUT, outputType.toString());
        } else {
            url = request.getEntryUrlPath(getRepository().URL_ENTRY_SHOW
                                          + suffix, entry);
            url = HU.url(url, ARG_ENTRYID, entry.getId(), ARG_OUTPUT,
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
                           getEntryManager().getDummyGroup(), entries);
    }


    /**
     *
     * @param request _more_
     * @param outputType _more_
     * @param group _more_
     * @param children _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> children)
            throws Exception {
        return notImplemented("outputGroup:" + outputType + " "
                              + getClass().getName());
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
                         true, true);
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
     * @param includeCopyArgs _more_
     *
     * @throws Exception _more_
     */
    public static void addUrlShowingForm(Appendable sb, Entry entry,
                                         String formId, String skipList,
                                         String hook, String...opts)
            throws Exception {
        String outputId = HU.getUniqueId("output_");
        HU.div(sb, "", HU.id(outputId));
	//	String args = JsonUtil.map("includeCopyArgs","" + includeCopyArgs);
	List<String> tmp = new ArrayList<String>();
	for(String opt:opts) tmp.add(opt);
	String args = JsonUtil.map(tmp);
        HU.script(sb,
                         HU.call("HtmlUtil.makeUrlShowingForm",
                                        (entry == null)
                                        ? "null"
                                        : HU.quote(
                                            entry.getId()), HU.quote(
                                                formId), HU.quote(
                                                    outputId), (skipList
                                                        != null)
                ? skipList
					: "null", hook==null?"null": hook, args));
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
                                   String type, Entry entry, boolean addView, boolean addClear)
            throws Exception {
        return getSelect(request, elementId, label, allEntries, type, entry,
                         addView, addClear, "",false);
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
                                   boolean addView, boolean addClear, String linkExtra,
				   boolean addField)
            throws Exception {

        boolean hasType    = Utils.stringDefined(type);
        String  selectorId = elementId + ( !hasType
                                           ? ""
                                           : "_" + type);
        String event = getSelectEvent(request, elementId, allEntries, type, entry);
        String link = (label == null)
                      ? ""
                      : HU.mouseClickHref(event, label,
                                          linkExtra
					  + HU.cssClass("ramadda-button ramadda-clickable")
                                          + HU.id(selectorId
                                              + "_selectlink"));
        if (addView) {
            String viewEvent = HU.call("RamaddaUtils.viewSelect", HU.squote(selectorId));
            link = link + " "
                   + HU.mouseClickHref(viewEvent,
                                       HU.getIconImage("fas fa-link"),
                                       HU.attr("title", "View selection")
                                       + HU.id(selectorId + "_selectlink"));

	}


        if (addClear) {
            String clearEvent = HU.call("RamaddaUtils.clearSelect", HU.squote(selectorId));
            link = link + " "
                   + HU.mouseClickHref(clearEvent,
                                       HU.getIconImage("fas fa-eraser"),
                                       HU.attr("title", "Clear selection")
                                       + HU.id(selectorId + "_selectlink"));
        }

	if(addField) {
            link+= HU.hidden(elementId + "_hidden",
			     (entry != null)
			     ? entry.getId()
			     : "", HU.id(elementId + "_hidden"));
	    link += "<br>";
	    link+= HU.disabledInput(elementId,
				    (entry!= null)
				    ? entry.getName()
				    : "",
				    HU.cssClass(HU.CLASS_DISABLEDINPUT+" ramadda-clickable")+
				    HU.attrs("title","Click to select entry",
					     "onclick",event)+
				    HU.SIZE_25 + HU.id(elementId));
	}
        return link;
    }

    /**
     *
     * @param request _more_
     * @param elementId _more_
     * @param allEntries _more_
     * @param type _more_
     * @param entry _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    public static String getSelectEvent(Request request, String elementId,
                                        boolean allEntries, String type,
                                        Entry entry)
            throws Exception {
        boolean hasType    = Utils.stringDefined(type);
        String  selectorId = elementId + ( !hasType
                                           ? ""
                                           : "_" + type);
        String event = HU.call(
                           "RamaddaUtils.selectInitialClick",
                           HU.comma(
                               "event", HU.squote(selectorId),
                               HU.squote(elementId),
                               HU.squote(
                                   Boolean.toString(allEntries)), ( !hasType
                ? "null"
                : HU.squote(type)), ((entry != null)
                                            ? HU.squote(entry.getId())
                                            : "null"), HU.squote(
                                                (request == null)
                ? ""
                : request.getString(ARG_ENTRYTYPE, ""))));

        return event;
    }

    /**
     *
     * @param request _more_
     * @param arg _more_
     * @param allEntries _more_
     * @param type _more_
     * @param entry _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    public static String makeEntrySelect(Request request, String arg,
                                         boolean allEntries, String type,
                                         Entry entry)
            throws Exception {
        String event = OutputHandler.getSelectEvent(request, arg, allEntries,
                           type, entry);

        return HU.hidden(arg + "_hidden", (entry != null)
                                          ? entry.getId()
                                          : "", HU.id(arg
                                          + "_hidden")) + HU.span(HU.span(HU.faIcon("fas fa-hand-pointer"),
                                              "class=ramadda-clickable") + " " + HU.disabledInput(arg, (entry != null)
                ? entry.getName()
                : "", HU.clazz("disabledinput ramadda-clickable ramadda-entry-popup-select")
                      + HU.SIZE_40 + HU.id(arg)), HU.attr("onClick", event));
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
        HU.open(sb, "span", HU.attrs("class","ramadda-highlightable","title","Type: " + entry.getTypeHandler().getLabel()));
        String entryId = entry.getId();
        String entryIconImage    = getPageHandler().getEntryIconImage(request, entry);
        String event;
        String uid = "link_" + HU.blockCnt++;
        String folderClickUrl =
            request.entryUrl(getRepository().URL_ENTRY_SHOW, entry) + "&"
            + HU.args(new String[] {
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
        String  prefix;
        if ( !showArrow) {
            prefix = HU.img(getIconUrl(ICON_BLANK), "",
                            HU.attr(HU.ATTR_WIDTH, "10"));
        } else {
            String click = HU.onMouseClick(
                               HU.call(
                                   "Ramadda.folderClick",
                                   HU.comma(
                                       HU.squote(uid),
                                       HU.squote(folderClickUrl),
                                       HU.squote(
                                           getIconUrl(
                                               ICON_TOGGLEARROWDOWN)))));
            prefix = HU.img(getIconUrl(ICON_TOGGLEARROWRIGHT), message);
            prefix = HU.span(prefix,
                             HU.id("img_" + uid) + click
                             + HU.cssClass("ramadda-clickable"));
        }

        String img = prefix + HU.space(1) + entryIconImage;

        sb.append(img);
        sb.append(HU.space(1));

        String  type      = request.getString(ARG_SELECTTYPE, "");
        String  elementId = entry.getId();
        String  name      = entry.isGroup()
                            ? ((Entry) entry).getName()
                            : getEntryDisplayName(entry);
        boolean isGroup   = entry.getTypeHandler().isGroup();
        boolean isImage   = entry.isImage();

        name = name.replace("'", "\\'");
	

	List attrs = Utils.makeList(
                                "entryType",
                                HU.squote(entry.getTypeHandler().getType()),
				"entryName",JsonUtil.quote(entry.getName()),
				"icon",HU.squote(getPageHandler().getIconUrl(request, entry)),
                                "isGroup", "" + isGroup, "isImage",
                                "" + isImage);
	if(entry.isFile()) {
	    Utils.add(attrs, "filename",JsonUtil.quote(IO.getFileTail(entry.getResource().getPath())));
	}
	if(entry.isGeoreferenced()) {
	    Utils.add(attrs, "isGeo","true");
	    if(entry.hasAreaDefined()) {
		Utils.add(attrs, "north",entry.getNorth(),"west",entry.getWest(),
			  "south",entry.getSouth(),
			  "east",entry.getEast());
	    } else if(entry.hasLocationDefined()) {
		Utils.add(attrs, "latitude",entry.getLatitude(),
			  "longitude",entry.getLongitude());

	    }
	}

	List<String> urls = new ArrayList<String>();
	getMetadataManager().getThumbnailUrls(request, entry, urls);
	if (urls.size() > 0) {
	    Utils.add(attrs,"thumbnailUrl", JsonUtil.quote(urls.get(0)));
	}


	String mapGlyphs = entry.getTypeHandler().getProperty(entry,"mapglyphs",null);
	if(mapGlyphs!=null) {
            Utils.add(attrs,  "mapglyphs", JsonUtil.quote(mapGlyphs));
	    //"base64:" + Utils.encodeBase64(mapGlyphs));		      
	}


        sb.append(HU.mouseClickHref(HU.call(
					    "RamaddaUtils.selectClick",
					    HU.comma(
                        HU.squote(target), HU.squote(entry.getId()),
                        HU.squote(name),
                        JsonUtil.map(attrs), HU.squote(
                                    type))), linkText));

        HU.close(sb, "span");
        sb.append(HU.br());
        sb.append(HU.div("",
                         HU.attrs(HU.ATTR_STYLE, HU.STYLE_HIDDEN,
                                  HU.ATTR_CLASS, CSS_CLASS_FOLDER_BLOCK,
                                  HU.ATTR_ID, uid)));

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
     * @param entry _more_
     * @param htmlSB _more_
     * @param jsSB _more_
     * @param showDetails _more_
     * @param showIcon _more_
     *
     * @throws Exception _more_
     */
    public void addEntryTableRow(Request request, Entry entry,
                                 Appendable htmlSB, Appendable jsSB,
                                 boolean showDetails, boolean showIcon)
            throws Exception {
        String label = (showIcon
                        ? getPageHandler().getEntryIconImage(request, entry)
                          + " "
                        : "") + getEntryManager().getEntryDisplayName(entry);
        String link = HU.href(getEntryManager().getEntryURL(request, entry),
                              label,HU.cssClass("ramadda-clickable"));
        htmlSB.append(link + "<br>");

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
        return HU.formClose();
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
            return null;
        }


        String url = entry.getResource().getPath();
        if (entry.getResource().isUrl()) {
            return entry.getTypeHandler().getPathForEntry(request, entry,false);
        }
        if (url != null) {
            if (url.startsWith("ftp:") || url.startsWith("http:")) {
                return url;
            }
        }


	String name = HU.urlEncode(getStorageManager().getFileTail(entry));
	url =  HU.url(request.makeUrl(repository.URL_ENTRY_GET) + "/"
		      + (addVersion
			 ? ("v" + (imageVersionCnt++))
			 : "") + name, ARG_ENTRYID, entry.getId());
	return url;
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
        HU.open(tabHtml, HU.TAG_DIV, "id", tabId, "class", "ui-tabs");
        HU.open(tabHtml, HU.TAG_UL);
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
        HU.close(tabHtml, HU.TAG_UL);
        cnt = 1;
        for (int i = 0; i < titles.size(); i++) {
            String tabContents = tabs.get(i).toString();
            if (skipEmpty
                    && ((tabContents == null)
                        || (tabContents.length() == 0))) {
                continue;
            }
            tabHtml.append(HU.div(tabContents,
                                  HU.id(tabId + "-" + (cnt++))
                                  + HU.cssClass("ui-tabs-hide")));
            tabHtml.append("\n");
        }

        tabHtml.append(HU.close(HU.TAG_DIV));
        tabHtml.append("\n");
        String args = "activate: HtmlUtil.tabLoaded";
        if (useCookies) {
            args += ",\ncookie: {expires:1}";
        }
        tabHtml.append(HU.script("\njQuery(function(){\njQuery('#" + tabId
                                 + "').tabs({" + args + "})});\n\n"));

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
        return HU.input(arg, request.getString(arg, dflt),
                        HU.attr(HU.ATTR_SIZE, "" + width));
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


    public boolean  isHtml() {
	return false;
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
            Entry selectedEntry = getEntryManager().getEntry(request, entryId);
            if (selectedEntry != null) {
                entryName = selectedEntry.getName();
            }
        }
        StringBuilder publishSB = new StringBuilder();
        sb.append(HU.hidden(ARG_PUBLISH_ENTRY + "_hidden", entryId,
                            HU.id(ARG_PUBLISH_ENTRY + "_hidden")));
	header = HU.div(header,HU.clazz("ramadda-form-help"));
	HU.formEntry(sb,"",header);

        String select = OutputHandler.getSelect(request, ARG_PUBLISH_ENTRY,
						"Select folder", false, null, entry);


        String addMetadata = !addMetadataField
                             ? ""
                             : HU.labeledCheckbox(
                                 ARG_METADATA_ADD, HU.VALUE_TRUE,
                                 request.get(ARG_METADATA_ADD, true),
				 "Add properties");

	String event = getSelectEvent(request, ARG_PUBLISH_ENTRY,true,null, entry);
        sb.append(HU.formEntry(msgLabel("Folder"),
                               HU.disabledInput(ARG_PUBLISH_ENTRY, entryName,
						HU.attrs("class","disabledinput ramadda-clickable ramadda-hoverable",
							 "id",ARG_PUBLISH_ENTRY,
							 "title","Click to select entry",
							 "onclick",event)+
						HU.SIZE_60) +HU.br()+ select + HU.space(2)
                                       + addMetadata));

        if (addNameField) {
            sb.append(HU.formEntry(msgLabel("Name"),
                                   htmlInput(request, ARG_PUBLISH_NAME, "",
                                             30)));
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
        return getEntryManager().getEntryLink(request, entry, "");
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


    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Apr 13, '22
     * @author         Enter your name here...
     */
    public static class ResultHandler {

        /**  */
        private boolean newWay = false;

        /**  */
        private Request request;

        /**  */
        private Entry entry;

        /**  */
        private State state;

        /**  */
        private Result result;

        /**  */
        private PrintWriter pw;

        /**  */
        private Appendable sb;

        /**  */
        private OutputHandler outputHandler;


        /**
         *
         *
         * @param request _more_
         * @param outputHandler _more_
         * @param entry _more_
         * @param state _more_
         *
         * @throws Exception _more_
         */
        public ResultHandler(Request request, OutputHandler outputHandler,
                             Entry entry, State state)
                throws Exception {
            this.request       = request;
            this.entry         = entry;
            this.outputHandler = outputHandler;
            this.state         = state;
            newWay = outputHandler.getRepository().getStreamOutput();
            OutputStream outputStream = request.getOutputStream();
            if ((outputStream == null) || !request.getCanStreamResult()) {
                newWay = false;
            }
            //      System.err.println("ResultHandler newWay:" + newWay);
            if (newWay) {
                result = request.getOutputStreamResult(entry.getName(),
                        "text/html");
                sb = pw = new PrintWriter(outputStream);
                request.setPrintWriter(pw);
                outputHandler.getEntryManager().addEntryHeader(request,
                        entry, result);
                outputHandler.addLinks(request, result, state);
                outputHandler.getPageHandler().decorateResult(request,
                        result, sb, true, false);
                pw.flush();
            } else {
                sb = new StringBuilder("");
            }
        }

        /**
         *  @return _more_
         */
        public Appendable getAppendable() {
            return sb;
        }

        /**
         *  @return _more_
         */
        public Result getResult() {
            return result;
        }

        /**
         *
         * @throws Exception _more_
         */
        public void finish() throws Exception {
            if (newWay) {
                outputHandler.getPageHandler().decorateResult(request,
                        result, sb, false, true);
                if (pw != null) {
                    pw.flush();
                }
            } else {
                result = outputHandler.makeLinksResult(request,
                        entry.getName(), sb, state);
                //              outputHandler.getEntryManager().addEntryHeader(request, entry, result);
                //              outputHandler.getPageHandler().decorateResult(request,  result);
            }
        }

    }






}
