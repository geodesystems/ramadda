/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.search;



import org.ramadda.repository.*;


import org.ramadda.repository.*;
import org.ramadda.repository.map.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;

import org.ramadda.repository.util.DateArgument;

import org.ramadda.repository.util.SelectInfo;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JQuery;
import org.ramadda.util.Utils;
import org.ramadda.util.sql.Clause;

import org.w3c.dom.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.awt.geom.Rectangle2D;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

import java.util.List;


/**
 *
 *
 * @author jeff mcwhirter
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public class SpecialSearch extends RepositoryManager implements RequestHandler {

    /** _more_ */
    public static final String ARG_FIELDS = "fields";

    /** _more_ */
    public static final String ARG_METADATA = "metadata";

    /** _more_ */
    public static final String ATTR_TABS = "tabs";

    /** _more_ */
    public static final String TAB_LIST = "list";

    /** _more_ */
    public static final String TAB_MAP = "map";


    /** _more_ */
    public static final String TAB_TIMELINE = "timeline";


    /** _more_ */
    private RequestUrl URL_SEARCH;

    /** _more_ */
    public static final String ARG_SEARCH_SUBMIT = "search.submit";

    /** _more_ */
    public static final String ARG_SEARCH_REFINE = "search.refine";

    /** _more_ */
    public static final String ARG_SEARCH_CLEAR = "search.clear";

    /** _more_ */
    private String theType;

    /** _more_ */
    private TypeHandler typeHandler;

    /** _more_ */
    private String searchUrl;

    /** _more_ */
    private boolean searchOpen = true;

    /** _more_ */
    private boolean doSearchInitially = true;

    /** _more_ */
    private boolean showText = true;
    private boolean showAncestor = true;    

    /** _more_ */
    private boolean showName = true;

    /** _more_ */
    private boolean showDesc = false;

    /** _more_ */
    private boolean showArea = true;

    /** _more_ */
    private boolean showDate = true;

    private String orderByTypes;

    /** _more_ */
    private String label;

    /** _more_ */
    private List<String> metadataTypes = new ArrayList<String>();

    /** _more_ */
    private List<String> tabs = new ArrayList<String>();

    /** _more_ */
    private List<SyntheticField> syntheticFields =
        new ArrayList<SyntheticField>();

    /**
     * _more_
     *
     * @param repository _more_
     */
    public SpecialSearch(Repository repository) {
        super(repository);
    }

    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @param props _more_
     *
     * @throws Exception _more_
     */
    public SpecialSearch(Repository repository, Element node, Hashtable props)
            throws Exception {
        super(repository);
        init(node, props);
    }

    /**
     * _more_
     *
     * @param typeHandler _more_
     */
    public SpecialSearch(TypeHandler typeHandler) {
        super(typeHandler.getRepository());
        this.typeHandler = typeHandler;
        /*
          search.synthetic.fields="authors"
          search.synthetic.authors.label="Authors"
          search.synthetic.authors.fields="primary_author,other_authors"
        */
        String syntheticIds =
            (String) typeHandler.getTypeProperty("search.synthetic.fields",
                null);
        if (syntheticIds != null) {
            for (String id : Utils.split(syntheticIds, ",", true, true)) {
                String label =
                    (String) typeHandler.getTypeProperty("search.synthetic."
                        + id + ".label", id);
                String fieldString =
                    (String) typeHandler.getTypeProperty("search.synthetic."
                        + id + ".fields", null);
                syntheticFields.add(new SyntheticField(id, label,
                        Utils.split(fieldString, ",", true, true)));
            }
        }

        String types =
            (String) typeHandler.getTypeProperty("search.metadatatypes",
                null);
        if (types != null) {
            for (String type : Utils.split(types, ",", true, true)) {
                metadataTypes.add(type);
            }
        }
        String tabsToUse =
            (String) typeHandler.getTypeProperty("search.tabs", TAB_LIST);
        tabs.addAll(Utils.split(tabsToUse, ",", true, true));

        searchOpen = typeHandler.getTypeProperty("search.searchopen",
                "true").equals("true");
        doSearchInitially = typeHandler.getTypeProperty("search.initsearch",
                "false").equals("true");
        showAncestor = typeHandler.getTypeProperty("search.form.showAncestor",
                "true").equals("true");
        showText = typeHandler.getTypeProperty("search.form.text.show",
                "true").equals("true");
        showName = typeHandler.getTypeProperty("search.form.name.show",
                showName + "").equals("true");
        showDesc =
            typeHandler.getTypeProperty("search.form.description.show",
                                        showDesc + "").equals("true");
        showArea = typeHandler.getTypeProperty("search.form.area.show",
					       "true").equals("true");
        showDate = typeHandler.getTypeProperty("search.form.date.show",
					       "true").equals("true");
        searchUrl = "/search/type/" + typeHandler.getType();
 	orderByTypes= typeHandler.getTypeProperty("search.form.orderby",null);
        label     = typeHandler.getTypeProperty("search.label", null);
        if (label == null) {
            label = typeHandler.getDescription();
        }
        theType = typeHandler.getType();
    }


    /**
     * _more_
     *
     * @param node _more_
     * @param props _more_
     *
     * @throws Exception _more_
     */
    public void init(Element node, Hashtable props) throws Exception {
        String types = (String) props.get("metadatatypes");
        if (types != null) {
            for (String type : Utils.split(types, ",", true, true)) {
                metadataTypes.add(type);
            }
        }
        String tabsToUse = (String) props.get(ATTR_TABS);
        if (tabsToUse != null) {
            tabs.addAll(Utils.split(tabsToUse, ",", true, true));
        } else {
            tabs.add(TAB_LIST);
            tabs.add(TAB_MAP);
            tabs.add(TAB_TIMELINE);
        }


        searchOpen        = Misc.getProperty(props, "searchopen", true);
        doSearchInitially = Misc.getProperty(props, "initsearch", true);
        showText          = Misc.getProperty(props, "form.text.show", true);
        showName = Misc.getProperty(props, "form.name.show", showName);
        showDesc = Misc.getProperty(props, "form.description.show", showDesc);
        showArea          = Misc.getProperty(props, "form.area.show", true);
        showDate          = Misc.getProperty(props, "form.date.show", true);
        searchUrl         = (String) props.get("searchurl");
        label             = (String) props.get("label");
        theType           = (String) props.get("type");
        typeHandler       = getRepository().getTypeHandler(theType);
        if (label == null) {
            label = msgLabel("Search") + typeHandler.getDescription();
        }
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
    public Result processCapabilitiesRequest(Request request)
            throws Exception {
        request.put(ARG_OUTPUT, "atom");
        request.put(ARG_TYPE, theType);
        //        request.put("atom.id", theType);
        Result result =
            getRepository().getSearchManager().processEntrySearch(request);

        return result;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void makeHeader(Request request, Appendable sb) throws Exception {}


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processSearchRequest(Request request) throws Exception {

        StringBuilder sb     = new StringBuilder();
        Result        result = processSearchRequest(request, sb);
        if (result != null) {
            return result;
        }
        result = new Result("Search", sb);

        return getRepository().getEntryManager().addEntryHeader(request,
                request.getRootEntry(), result);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processSearchRequest(Request request, Appendable sb)
            throws Exception {

        int contentsWidth  = 750;
        int contentsHeight = 450;
        int minWidth       = contentsWidth + 200;
        request.put(ARG_TYPE, theType);
        List<Entry> allEntries = new ArrayList<Entry>();
        boolean     refinement = request.exists(ARG_SEARCH_REFINE);
        if ( !request.exists(ARG_MAX)) {
            request.put(ARG_MAX, DEFAULT_SEARCH_SIZE);
        }


        List<String> tabsToUse = tabs;
        String tabsProp = request.getString("search.tabs",
                                            request.getString("tabs",
							      (String) null));
        if (tabsProp != null) {
            tabsToUse = Utils.split(tabsProp, ",", true, true);
        }



        makeHeader(request, sb);
	//        sb.append(HU.sectionOpen());
	String label = HU.href(request.getRequestPath(),this.label,HU.cssClass("ramadda-nodecor ramadda-clickable"));
	sb.append(HU.div(label,HU.attrs("class","ramadda-heading")));
	makeSearchForm(request, sb,tabsToUse);
	//        sb.append(HU.sectionClose());
	if(true) return null;



        int     cnt      = getEntryUtil().getEntryCount(typeHandler);
        boolean doSearch = (refinement
                            ? false
                            : ((cnt < 100) || doSearchInitially));
        doSearch = true;

        if (request.defined(ARG_SEARCH_SUBMIT)) {
            doSearch = true;
        }




	request.put("forsearch","true");
	allEntries = getSearchManager().doSearch(request, new SelectInfo(request));
        if (request.isOutputDefined()) {
            OutputHandler outputHandler =
                getRepository().getOutputHandler(request);
            return outputHandler.outputGroup(
					     request, null, getEntryManager().getDummyGroup(), allEntries);
        }

        if (request.exists("timelinexml")) {
            Entry group = getRepository().getEntryManager().getDummyGroup();
            return getRepository().getCalendarOutputHandler()
                .outputTimelineXml(request, group, allEntries);
        }


        if (URL_SEARCH == null) {
            URL_SEARCH = new RequestUrl(this, searchUrl);
        }

	


	StringBuffer formSB = new StringBuffer();	
	makeSearchForm(request, formSB,tabsToUse);

        List<String> tabContents = new ArrayList<String>();
        List<String> tabTitles   = new ArrayList<String>();

        if (refinement) {
            tabTitles.add(msg("Results"));
            tabContents.add(
                HU.div(
                    getPageHandler().showDialogNote(
                        "Search criteria refined"), HU.style(
                        "min-width:" + minWidth + "px")));
        } else if ( !doSearch) {
            tabTitles.add(msg("Results"));
            tabContents.add("");

        } else {
            if (allEntries.size() == 0) {
                tabTitles.add(msg("Results"));
                tabContents.add(
                    HU.div(
                        getPageHandler().showDialogNote(
                            LABEL_NO_ENTRIES_FOUND), HU.style(
                            "min-width:" + minWidth + "px")));
            } else {
                for (String tab : tabsToUse) {
                    if (tab.equals(TAB_LIST)) {
                        StringBuffer listSB = new StringBuffer();
                        makeEntryList(request, listSB, allEntries);
                        tabContents.add(HU.div(listSB.toString(),
					       HU.style("max-height","3000px","overflow-y","auto",
							"min-width",  minWidth	+ "px")));
                        tabTitles.add(HU.img(getIconUrl(ICON_LIST))
                                      + " " + msg("List"));
                    } else if (tab.equals(TAB_MAP)) {
			StringBuilder mapSB= makeMap(request, allEntries,contentsWidth,  contentsHeight);
                        tabContents.add(HU.div(mapSB.toString(),
                                HU.style("min-width:" + minWidth
                                    + "px")));
                        tabTitles.add(HU.img(getIconUrl(ICON_MAP))
                                      + " " + msg("Map"));
                    } else if (tab.equals(TAB_TIMELINE)) {
			StringBuffer timelineSB  = new StringBuffer();
			getRepository().getCalendarOutputHandler().makeTimeline(request, null,  //Pass null for the main entry
										allEntries, timelineSB,
										"width:" + contentsWidth + "px; height: " + contentsHeight
										+ "px;", new Hashtable());
                        tabContents.add(HU.div(timelineSB.toString(),
                                HU.style("min-width:" + minWidth
                                    + "px")));
                        tabTitles.add(
                            HU.img(getIconUrl(ICON_TIMELINE)) + " "
                            + msg("Timeline"));
                    }
                }
            }
        }
        String tabs;
        if (tabContents.size() == 1) {
            tabs = tabContents.get(0);
        } else {
            tabs = OutputHandler.makeTabs(tabTitles, tabContents, true);
        }
	tabs = HU.div(tabs,   HU.cssClass("ramadda-search-results"));


        StringBuffer rightSide = new StringBuffer();
        getRepository().getHtmlOutputHandler().showNext(request,
                allEntries.size(), rightSide);
        rightSide.append(tabs);

        boolean showForm = request.get(ARG_SEARCH_SHOWFORM, true);
        if (showForm) {
            sb.append(
                "<table width=100% border=0 cellpadding=0 cellspacing=0><tr valign=top>");
            sb.append(HU.col(formSB.toString(), ""));
            sb.append(
                HU.col(
                    rightSide.toString(),
                    HU.style("min-width:" + minWidth + "px;")
                    + HU.attr(HU.ATTR_ALIGN, "left")));
            sb.append("</table>");
        } else {
            sb.append(rightSide);
        }
        sb.append(HU.sectionClose());
        return null;

    }



    private StringBuilder  makeMap(Request request,List<Entry> allEntries,int contentsWidth, int contentsHeight)  throws Exception {
	//Clone and clear in case a .css file gets added into the map bubbles
	request= request.cloneMe();
	request.clearExtraProperties();
        MapInfo map = getRepository().getMapManager().createMap(request,
                          null, "" + contentsWidth, "" + contentsHeight,
                          true, null);


        getMapManager().addToMap(request, map, allEntries,
                                 Utils.makeMap(MapManager.PROP_DETAILED,
                                     "false", MapManager.PROP_SCREENBIGRECTS,
                                     "true"));
        Rectangle2D.Double bounds = getEntryUtil().getBounds(allEntries);


        //shrink the bounds down
        if ((bounds != null) && (bounds.getWidth() > 180)) {
            double cx = bounds.getX() + bounds.getWidth() / 2;
            double cy = bounds.getY() + bounds.getHeight() / 2;
            int    f  = (int) (bounds.getWidth() / 3);
            bounds = new Rectangle2D.Double(cx - f, cy - f / 2, f * 2, f);
        }
        //        map.centerOn(bounds);
        map.addJS(map.getVariableName() + ".initMap(true);\n");
        if (request.defined(ARG_AREA_NORTH) && request.defined(ARG_AREA_WEST)
                && request.defined(ARG_AREA_SOUTH)
                && request.defined(ARG_AREA_EAST)) {
            map.addJS(
                HU.call(
                    map.getVariableName() + ".setSelectionBox",
                    request.get(ARG_AREA_NORTH, 0.0) + ","
                    + request.get(ARG_AREA_WEST, 0.0) + ","
                    + request.get(ARG_AREA_SOUTH, 0.0) + ","
                    + request.get(ARG_AREA_EAST, 0.0)));

        }

	String id = (String)request.getExtraProperty("mapselectorid");
	if(id==null) id = ARG_AREA;
        String initParams = HU.squote(id) + "," + true + "," + "0";
        map.addJS(map.getVariableName() + ".setSelection(" + initParams  + ");\n");
        map.centerOn(bounds, true);
	StringBuilder sb = new StringBuilder();
        sb.append(HU.italics(msg("Shift-drag to select region")));
        sb.append(map.getHtml());
	return sb;
    }

    private void   addAttr(Appendable sb,Object...values) throws Exception {
	for(int i=0;i<values.length;i+=2) {
	    if(values[i+1]==null) continue;
	    sb.append(values[i] +"=\"");
	    sb.append(values[i+1].toString());
	    sb.append("\" ");
	}
	sb.append("\n");
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param formSB _more_
     *
     * @throws Exception _more_
     */
    private void makeSearchForm(Request request, Appendable formSB,List<String> tabs)
            throws Exception {

	if(true) {
	    StringBuilder sb = new StringBuilder();
	    sb.append("{{display_entrylist ");
	    addAttr(sb, "searchDirect","false");
	    addAttr(sb, "providers","this,type:ramadda");
	    addAttr(sb, "showAncestor",typeHandler.getTypeProperty("search.form.showAncestor",true));
	    addAttr(sb,"entryTypes",typeHandler.getType(),"displayTypes",Utils.join(tabs,","));
	    addAttr(sb,"orderByTypes",orderByTypes);
	    addAttr(sb, "showDate",showDate,"showArea",showArea, "showText",showText,
		    "showAncestor",showAncestor,"showName",showName,"showDescription",showDesc);
	    addAttr(sb,"startDateLabel",typeHandler.getTypeProperty("search.form.startdate.label",
								   typeHandler.getTypeProperty("form.startdate.label",null)));
	    addAttr(sb,"createDateLabel",typeHandler.getTypeProperty("search.form.createdate.label",null));
	    addAttr(sb,"areaLabel",typeHandler.getTypeProperty("search.form.area.label",null));
	    addAttr(sb,"showCreateDate",typeHandler.getTypeProperty("search.form.createdate.show",null));	    	    	    
	    addAttr(sb,"orderByTypes",typeHandler.getTypeProperty("search.form.orderby",null));	    	    	    

	    for(String line:Utils.split(typeHandler.getTypeProperty("search.form.args",""),"\n",true,true)) {
		sb.append(line);
		sb.append("\n");
	    }

	    sb.append("}}\n");
	    formSB.append(getRepository().getWikiManager().wikifyEntry(request,
								       getEntryManager().getRootEntry(),
								       sb.toString()));
	    return;

	}



        boolean      showDefault        = true;
        List<String> metadataTypesToUse = metadataTypes;
        if (request.defined(ARG_METADATA)) {
            metadataTypesToUse =
                Utils.split(request.getSanitizedString(ARG_METADATA, ""),
                            ",", true, true);
        }
        List<String> fieldsList =
            Utils.split(request.getSanitizedString(ARG_FIELDS, ""), ",",
                        true, true);

        HashSet<String> fieldsToShow = null;

        if (fieldsList.size() > 0) {
            fieldsToShow = new HashSet<String>();
            fieldsToShow.addAll(fieldsList);
            showDefault = false;
        }

        formSB.append("<div class='ramadda-search-special-form'>");
        formSB.append(request.form(URL_SEARCH,
                                   HU.attr(HU.ATTR_NAME,
                                       "apisearchform")));
        formSB.append(HU.submit("Search", ARG_SEARCH_SUBMIT));
        formSB.append(HU.br());

        if (getSearchManager().isLuceneEnabled()) {
            String ancestor = request.getString(ARG_ANCESTOR + "_hidden",
                                  request.getString(ARG_ANCESTOR, null));

            Entry ancestorEntry = (ancestor == null)
                                  ? null
                                  : getEntryManager().getEntry(request, ancestor);
            String select =
                getRepository().getHtmlOutputHandler().getSelect(request,
								 ARG_ANCESTOR, "Search under", true, "", ancestorEntry,
								 true,true,"",true);
	    formSB.append(select);
        }

	String vspace = "<div style='height:0.5em'></div>";

	List<String> contents = new ArrayList<String>();
	List<String> titles = new ArrayList<String>();	
	getSearchManager().addSearchProviders( request,  contents, titles,true,true);
	if(contents.size()>0) {
            formSB.append(HU.div(HU.makeShowHideBlock("Search Providers",
					       HU.div(contents.get(0),HU.cssClass("ramadda-search-bytype")),
						      false),""));
	}


        if (showDefault && showText) {
	    formSB.append(vspace);
	    formSB.append(HU.input(ARG_TEXT,  request.getSanitizedString(ARG_TEXT, ""),
				  HU.attr("placeholder","Text")+
				  HU.id("searchinput")
				  + HU.SIZE_25
				  + " autocomplete='off'   autofocus "));
        }

        if (showDefault && showName) {
	    formSB.append(vspace);
            formSB.append(HU.input(ARG_NAME,
				   request.getSanitizedString(ARG_NAME, ""),
				   HU.SIZE_15 + " autofocus " + HU.attr("placeholder","Name")));
        }

        if (showDefault && showDesc) {
	    formSB.append(vspace);
            formSB.append(HU.input(
                        ARG_DESCRIPTION,
                        request.getSanitizedString(ARG_DESCRIPTION, ""),
                        HU.SIZE_15 + " autofocus "+
			HU.attr("placeholder","Description")));
        }


        formSB.append(HU.formTable());

        if (showDefault && showDate) {
	    String startLabel = typeHandler.getTypeProperty("form.startdate.label","Date");
            TypeHandler.addDateSearch(getRepository(), request, formSB,
				      new DateArgument(DateArgument.TYPE_DATA,DateArgument.ARG_DATA_DATE, startLabel),
				      false);

	    String createLabel = typeHandler.getTypeProperty("form.createdate.label","Create Date");
            TypeHandler.addDateSearch(getRepository(), request, formSB,
				      new DateArgument(DateArgument.TYPE_CREATE,DateArgument.ARG_CREATE_DATE, createLabel),
                                      false);

        }

        if (showDefault && showArea) {
            String[] nwse = new String[] {
                                request.getSanitizedString(ARG_AREA_NORTH,
                                    ""),
                                request.getSanitizedString(ARG_AREA_WEST, ""),
                                request.getSanitizedString(ARG_AREA_SOUTH,
                                    ""),
                                request.getSanitizedString(ARG_AREA_EAST,
                                    ""), };

            MapInfo selectMap =
                getRepository().getMapManager().createMap(request, null, true, null);
            String mapSelector = selectMap.makeSelector(ARG_AREA, true, nwse);
	    mapSelector +=TypeHandler.getSpatialSearchTypeWidget(request);
	    request.putExtraProperty("mapselectorid",selectMap.getMapId());
	    String label = typeHandler.getTypeProperty("form.location.label","Location");
            HU.formEntry(formSB,HU.b(msgLabel(label))+ HU.br()+mapSelector);
        }


        if (showDefault) {
            for (SyntheticField field : syntheticFields) {
                String id = field.id;
                formSB.append(formEntry(request, msgLabel(field.label),
                                        HU.input(id,
                                            request.getSanitizedString(id,
                                                ""), HU.SIZE_20)));
            }
        }

        typeHandler.addToSpecialSearchForm(request, formSB, fieldsToShow);


        for (String type : metadataTypesToUse) {
            MetadataType metadataType =
                getRepository().getMetadataManager().findType(type);
            if (metadataType != null) {
                StringBuffer tmpsb = new StringBuffer();
                metadataType.getHandler().addToSearchForm(request, tmpsb,
                        metadataType);
                formSB.append(tmpsb);
            }
        }



	HU.formEntry(formSB,"<div class=ramadda-thin-hr></div>");
        HU.formEntry(formSB,HU.b("Order By:")+HU.br()+
		     getSearchManager().makeOrderBy(request,false));

	HU.formEntry(formSB, HU.b("Max:")+HU.space(1) +
		     HU.input(ARG_MAX,request.getString(ARG_MAX,DEFAULT_SEARCH_SIZE),
			      HU.SIZE_5));
        StringBuffer buttons = new StringBuffer();
        buttons.append(HU.submit("Search", ARG_SEARCH_SUBMIT));
        boolean doSearch = true;
        if (doSearch) {
            buttons.append(" ");
            String baseUrl = request.getUrl();
            buttons.append(HU.br());
            StringBuffer links = new StringBuffer();
            for (OutputType outputType : new OutputType[] {
		    XmlOutputHandler.OUTPUT_XML,
		    KmlOutputHandler.OUTPUT_KML, ZipOutputHandler.OUTPUT_ZIPTREE,
		    AtomOutputHandler.OUTPUT_ATOM, JsonOutputHandler.OUTPUT_JSON,
		    CsvOutputHandler.OUTPUT_CSV, ZipOutputHandler.OUTPUT_EXPORT,
		    BulkDownloadOutputHandler.OUTPUT_CURL
		}) {
                if (outputType.getIcon() != null) {
                    links.append(
				 HU.img(getIconUrl(outputType.getIcon()),"",
					       HU.attr(HU.ATTR_WIDTH, ICON_WIDTH)));
                    links.append(" ");
                }

                links.append(
                    HU.href(
                        baseUrl + "&"
                        + HU.arg(
                            ARG_OUTPUT,
                            outputType.toString()), outputType.getLabel()));
                links.append(HU.br());
            }
            buttons.append(HU.makeShowHideBlock(msg("More..."),
                    links.toString(), false));
        }


        if (request.exists(ARG_USER_ID)) {
            formSB.append(HU.formEntry(msgLabel("User"),
                    HU.input(ARG_USER_ID,
                                    request.getSanitizedString(ARG_USER_ID,
                                        ""), HU.SIZE_20)));
        }

	HU.formEntry(formSB, buttons.toString());
        formSB.append(HU.formTableClose());
        formSB.append(HU.formClose());
        formSB.append("</div>");


    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param entries _more_
     *
     * @throws Exception _more_
     */
    public void makeEntryList(Request request, Appendable sb,
                              List<Entry> entries)
            throws Exception {

	Hashtable props = new Hashtable();
	props.put("showChangeDate","false");	
	if(!showDate) 
	    props.put("showDate","false");
	if(entries.size()>0 && entries.get(0).getTypeHandler().hasSearchDisplayText(request,  entries.get(0))) {
	    sb.append("<div class=ramadda-search-entrylist>");
	    for(Entry entry: entries) {
		sb.append(entry.getTypeHandler().getSearchDisplayText(request,  entry));
	    }
	    sb.append("</div>");
	    return;
	}
        getRepository().getHtmlOutputHandler().makeTable(request, entries,
							 sb, props);
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Aug 23, '13
     * @author         Enter your name here...
     */
    private static class SyntheticField {

        /** _more_ */
        String label;

        /** _more_ */
        String id;

        /** _more_ */
        List<String> fields = new ArrayList<String>();

        /**
         * _more_
         *
         * @param id _more_
         * @param label _more_
         * @param fields _more_
         */
        public SyntheticField(String id, String label, List<String> fields) {
            this.id     = id;
            this.label  = label;
            this.fields = fields;
        }
    }


}
