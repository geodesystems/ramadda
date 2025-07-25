/**
Copyright (c) 2008-2025 Geode Systems LLC
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

@SuppressWarnings("unchecked")
public class SpecialSearch extends RepositoryManager implements RequestHandler {
    public static final String ARG_FIELDS = "fields";
    public static final String ARG_METADATA = "metadata";
    public static final String ATTR_TABS = "tabs";
    public static final String TAB_LIST = "list,display";
    public static final String TAB_MAP = "map";
    public static final String TAB_TIMELINE = "timeline";
    private RequestUrl URL_SEARCH;
    public static final String ARG_SEARCH_SUBMIT = "search.submit";
    public static final String ARG_SEARCH_REFINE = "search.refine";
    public static final String ARG_SEARCH_CLEAR = "search.clear";
    private boolean newWay = true;
    private String theType;
    private TypeHandler typeHandler;
    private String searchUrl;
    private boolean searchOpen = true;
    private boolean doSearchInitially = true;
    private boolean showText = true;
    private boolean showAncestor = true;
    private boolean showProviders = true;
    private boolean showName = true;
    private boolean showDescription = false;
    private boolean showArea = true;
    private boolean showDate = true;
    private String orderByTypes;
    private String label;
    private List<String> metadataTypes = new ArrayList<String>();
    private List<String> tabs = new ArrayList<String>();
    private List<SyntheticField> syntheticFields =
        new ArrayList<SyntheticField>();

    public SpecialSearch(Repository repository) {
        super(repository);
    }

    public SpecialSearch(Repository repository, Element node, Hashtable props)
            throws Exception {
        super(repository);
        init(node, props);
    }

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
        showAncestor = typeHandler.getTypeProperty("search.ancestor.show",
						   "true").equals("true");
        showProviders= typeHandler.getTypeProperty("search.providers.show",
						   "true").equals("true");	
        showText = typeHandler.getTypeProperty("search.text.show",
                "true").equals("true");
        showName = typeHandler.getTypeProperty("search.name.show",
                showName + "").equals("true");
        showDescription =
            typeHandler.getTypeProperty("search.description.show",
                                        showDescription + "").equals("true");
        showArea = typeHandler.getTypeProperty("search.area.show",
					       "true").equals("true");
        showDate = typeHandler.getTypeProperty("search.date.show",
					       "true").equals("true");
        searchUrl = "/search/type/" + typeHandler.getType();
 	orderByTypes= typeHandler.getTypeProperty("search.orderby",null);
        label     = typeHandler.getTypeProperty("search.label", null);
        if (label == null) {
            label = typeHandler.getDescription();
        }
        theType = typeHandler.getType();
    }

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
        showDescription = Misc.getProperty(props, "form.description.show", showDescription);
        showArea          = Misc.getProperty(props, "form.area.show", true);
        showDate          = Misc.getProperty(props, "form.date.show", true);
        searchUrl         = (String) props.get("searchurl");
        label             = (String) props.get("label");
        theType           = (String) props.get("type");
        typeHandler       = getRepository().getTypeHandler(theType);
        if (label == null) {
            label = msgLabel("Search") + typeHandler.getDescription();
        }
        if (URL_SEARCH == null) {
            URL_SEARCH = new RequestUrl(this, searchUrl);
        }

    }

    public Result processCapabilitiesRequest(Request request)
            throws Exception {
        request.put(ARG_OUTPUT, "atom");
        request.put(ARG_TYPE, theType);
        //        request.put("atom.id", theType);
        Result result =
            getRepository().getSearchManager().processEntrySearch(request);

        return result;
    }

    public void makeHeader(Request request, Appendable sb) throws Exception {}

    public Result processSearchRequest(Request request) throws Exception {

        StringBuilder sb     = new StringBuilder();
        Result        result = processSearchRequest(request, sb,new Hashtable());
        if (result != null) {
            return result;
        }
        result = new Result("Search", sb);

        return getRepository().getEntryManager().addEntryHeader(request,
                request.getRootEntry(), result);
    }

    public Result processSearchRequest(Request request, Appendable sb,Hashtable props)
            throws Exception {
        int contentsWidth  = 750;
        int contentsHeight = 450;
        int minWidth       = contentsWidth + 200;
        request.put(ARG_TYPE, theType);
        boolean     refinement = request.exists(ARG_SEARCH_REFINE);
        if ( !request.exists(ARG_MAX)) {
            request.put(ARG_MAX, DEFAULT_SEARCH_SIZE);
        }

        List<String> tabsToUse = tabs;
        String tabsProp = request.getString("search.tabs",
                                            request.getString("tabs",
							      Utils.getProperty(props,"tabs",
										Utils.getProperty(props,"displayTypes",null))));
        if (tabsProp != null) {
            tabsToUse = Utils.split(tabsProp, ",", true, true);
        }

        makeHeader(request, sb);
	//        sb.append(HU.sectionOpen());
	String label = HU.href(request.getRequestPath(),this.label,HU.cssClass("ramadda-nodecor ramadda-clickable"));
	if(Utils.getProperty(props,"showTitle",true)) {
	    sb.append(HU.div(label,HU.attrs("class","ramadda-heading")));
	}

	if(newWay) {
	    makeSearchForm(request, sb,tabsToUse,props);
	    return null;
	}

        int     cnt      = getEntryUtil().getEntryCount(request,typeHandler);
        boolean doSearch = (refinement
                            ? false
                            : ((cnt < 100) || doSearchInitially));
        doSearch = true;

        if (request.defined(ARG_SEARCH_SUBMIT)) {
            doSearch = true;
        }

	request.put("forsearch","true");
        List<Entry> allEntries =  getSearchManager().doSearch(request, new SelectInfo(request));
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

	StringBuffer formSB = new StringBuffer();	
	makeSearchForm(request, formSB,tabsToUse,props);

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
        Rectangle2D.Double bounds = getEntryUtil().getBounds(request,allEntries);

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
	    //	    System.out.println(values[i]+"=" + values[i+1]);
	    sb.append(values[i] +"=\"");
	    sb.append(values[i+1].toString());
	    sb.append("\" ");
	}
	sb.append("\n");
    }

    private void makeSearchForm(Request request, Appendable formSB,List<String> tabs,Hashtable props)
            throws Exception {

        if (URL_SEARCH == null) {
            URL_SEARCH = new RequestUrl(this, searchUrl);
        }

	if(newWay) {
	    StringBuilder sb = new StringBuilder();
	    sb.append("{{display_entrylist ");

	    for(String line:Utils.split(typeHandler.getTypeProperty("search.args",""),"\n",true,true)) {
		sb.append(line);
		sb.append("\n");
	    }	    

	    addAttr(sb, "searchDirect","false");

	    String providers=Utils.getProperty(props,"providers","this,type:ramadda");
	    if(stringDefined(providers)) {
		addAttr(sb, "providers",providers);
		addAttr(sb, "showProviders","true");
	    }

	    for(String prop:new String[]{"tooltip",
					 "formHeight",
					 "entriesHeight",
					 "mapHeight",					 
					 "toggleClose",
					 "displayTypes",
					 "textToggleClose",
					 "dateToggleClose",
					 "areaToggleClose",
					 "columnsToggleClose",
					 "providersMultiple",
					 "providersMultipleSize",
					 "ancestor",
					 "showEntryImage",
					 "includeNonImages",
					 "showOutputs",
					 "metadataTypes",
					 "outputs",
					 "searchOutputs"}) {
		String v=Utils.getProperty(props,prop,null);
		if(stringDefined(v))
		    addAttr(sb, prop,v);
	    }

	    addAttr(sb,"entryTypes",typeHandler.getType());
	    addAttr(sb, "showAncestor",  Utils.getProperty(props,"showAncestor",
							   typeHandler.getTypeProperty("search.ancestor.show",""+showAncestor)));
	    addAttr(sb, "showProviders",  Utils.getProperty(props,"showProviders",""+showProviders));
	    addAttr(sb, "displayTypes",Utils.join(tabs,","));
	    addAttr(sb, "orderByTypes",Utils.getProperty(props,"orderByTypes",orderByTypes));
	    addAttr(sb, "showDate",Utils.getProperty(props,"showDate",
						     typeHandler.getTypeProperty("search.date.show",showDate)));
	    addAttr(sb, "showArea",Utils.getProperty(props,"showArea",
						     typeHandler.getTypeProperty("search.area.show",showArea)));
	    addAttr(sb, "searchOpen",Utils.getProperty(props,"searchOpen",searchOpen));
	    addAttr(sb, "showText",Utils.getProperty(props,"showText",typeHandler.getTypeProperty("search.text.show",showText)));
	    addAttr(sb, "showName",Utils.getProperty(props,"showName",
						     typeHandler.getTypeProperty("search.name.show",showName)));
	    addAttr(sb, "showDescription",Utils.getProperty(props,"showDescription",
							    typeHandler.getTypeProperty("search.description.show",showDescription)));
	    addAttr(sb, "showCreateDate",Utils.getProperty(props,"showCreateDate",
							   typeHandler.getTypeProperty("search.createdate.show",null)));	    	    	    

	    TypeHandler t = typeHandler;
	    addAttr(sb,"startDateLabel",
		    Utils.getProperty(props,"startDateLabel",
				      t.getTypeProperty("search.startdate.label",
							t.getTypeProperty("form.startdate.label",
									  t.getTypeProperty("form.date.label",null)))));

	    addAttr(sb,"createDateLabel",Utils.getProperty(props,"createDateLabel",
							   t.getTypeProperty("search.createdate.label",null)));
	    addAttr(sb,"areaLabel",Utils.getProperty(props,"areaLabel",
						     t.getTypeProperty("search.area.label",null)));
	    addAttr(sb,"orderByTypes",Utils.getProperty(props,"orderByTypes",
							t.getTypeProperty("search.orderby",null)));

	    if(metadataTypes.size()>0) {
		StringBuilder types=new StringBuilder();
		List<String> typeList=new ArrayList<String>();
		for(String type: metadataTypes) {
		    MetadataType metadataType =
			getRepository().getMetadataManager().findType(type);
		    if (metadataType != null) {
			typeList.add(metadataType.getId()+":"+ metadataType.getLabel());
		    }
		}
		addAttr(sb,"metadataTypes",Utils.join(typeList,","));
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

        if (showDefault && showDescription) {
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

    public void makeEntryList(Request request, Appendable sb, List<Entry> entries)
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

    private static class SyntheticField {

        String label;

        String id;

        List<String> fields = new ArrayList<String>();

        public SyntheticField(String id, String label, List<String> fields) {
            this.id     = id;
            this.label  = label;
            this.fields = fields;
        }
    }

}
