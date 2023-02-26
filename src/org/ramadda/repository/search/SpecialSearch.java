/**
Copyright (c) 2008-2021 Geode Systems LLC
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
    public static final String TAB_EARTH = "earth";

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

    /** _more_ */
    private boolean showName = false;

    /** _more_ */
    private boolean showDesc = false;

    /** _more_ */
    private boolean showArea = true;

    /** _more_ */
    private boolean showDate = true;

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
        label     = typeHandler.getTypeProperty("search.label", null);
        if (label == null) {
            label = msgLabel("Search") + " " + typeHandler.getDescription();
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
            tabs.add(TAB_EARTH);
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
            request.put(ARG_MAX, "50");
        }


        int     cnt      = getEntryUtil().getEntryCount(typeHandler);
        boolean doSearch = (refinement
                            ? false
                            : ((cnt < 100) || doSearchInitially));
        doSearch = true;

        if (request.defined(ARG_SEARCH_SUBMIT)) {
            doSearch = true;
        }


        if (doSearch) {
            List<Clause> extra      = null;
            if (syntheticFields.size() > 0) {
                extra = new ArrayList<Clause>();
                for (SyntheticField field : syntheticFields) {
                    String id = field.id;
                    if (request.defined(id)) {
                        for (String columnName : field.fields) {
                            Column column = typeHandler.getColumn(columnName);
                            if (column != null) {
                                column.addTextSearch(request.getString(id),
                                        extra, false);
                            }
                        }
                    }
                }
                if (extra.size() > 0) {
                    Clause orClause = Clause.or(extra);
                    extra = new ArrayList<Clause>();
                    extra.add(orClause);
                }
            }

            allEntries =
                getRepository().getEntryManager().searchEntries(request,extra);

        }


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


        StringBuffer js = new StringBuffer();
        if (URL_SEARCH == null) {
            URL_SEARCH = new RequestUrl(this, searchUrl);
        }


        makeHeader(request, sb);

        sb.append(HtmlUtils.sectionOpen());

        StringBuffer formSB = new StringBuffer();
        makeSearchForm(request, formSB);


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
                HtmlUtils.call(
                    map.getVariableName() + ".setSelectionBox",
                    request.get(ARG_AREA_NORTH, 0.0) + ","
                    + request.get(ARG_AREA_WEST, 0.0) + ","
                    + request.get(ARG_AREA_SOUTH, 0.0) + ","
                    + request.get(ARG_AREA_EAST, 0.0)));

        }



        String initParams = HtmlUtils.squote(ARG_AREA) + "," + true + ","
                            + "0";
        map.addJS(map.getVariableName() + ".setSelection(" + initParams
                  + ");\n");
        map.centerOn(bounds, true);


        List<String> tabsToUse = tabs;

        String tabsProp = request.getString("search.tabs",
                                            request.getString("tabs",
                                                (String) null));
        if (tabsProp != null) {
            tabsToUse = Utils.split(tabsProp, ",", true, true);
        }

        boolean georeferencedResults = tabsToUse.contains(TAB_MAP)
                                       || tabsToUse.contains(TAB_EARTH);


        List<String> tabContents = new ArrayList<String>();
        List<String> tabTitles   = new ArrayList<String>();
        StringBuffer timelineSB  = new StringBuffer();


        getRepository().getCalendarOutputHandler().makeTimeline(request, null,  //Pass null for the main entry
                allEntries, timelineSB,
                "width:" + contentsWidth + "px; height: " + contentsHeight
                + "px;", new Hashtable());


        StringBuffer mapSB = new StringBuffer(
                                 HtmlUtils.italics(
                                     msg("Shift-drag to select region")));
        mapSB.append(map.getHtml());

        if (refinement) {
            tabTitles.add(msg("Results"));
            tabContents.add(
                HtmlUtils.div(
                    getPageHandler().showDialogNote(
                        "Search criteria refined"), HtmlUtils.style(
                        "min-width:" + minWidth + "px")));
        } else if ( !doSearch) {
            tabTitles.add(msg("Results"));
            tabContents.add("");

        } else {
            if (allEntries.size() == 0) {
                tabTitles.add(msg("Results"));
                tabContents.add(
                    HtmlUtils.div(
                        getPageHandler().showDialogNote(
                            LABEL_NO_ENTRIES_FOUND), HtmlUtils.style(
                            "min-width:" + minWidth + "px")));
            } else {
                for (String tab : tabsToUse) {
                    if (tab.equals(TAB_LIST)) {
                        StringBuffer listSB = new StringBuffer();
                        makeEntryList(request, listSB, allEntries);
                        tabContents.add(HtmlUtils.div(listSB.toString(),
                                HtmlUtils.style("min-width:" + minWidth
                                    + "px")));
                        tabTitles.add(HtmlUtils.img(getIconUrl(ICON_LIST))
                                      + " " + msg("List"));
                    } else if (tab.equals(TAB_MAP)) {
                        tabContents.add(HtmlUtils.div(mapSB.toString(),
                                HtmlUtils.style("min-width:" + minWidth
                                    + "px")));
                        tabTitles.add(HtmlUtils.img(getIconUrl(ICON_MAP))
                                      + " " + msg("Map"));
                    } else if (tab.equals(TAB_TIMELINE)) {
                        tabContents.add(HtmlUtils.div(timelineSB.toString(),
                                HtmlUtils.style("min-width:" + minWidth
                                    + "px")));
                        tabTitles.add(
                            HtmlUtils.img(getIconUrl(ICON_TIMELINE)) + " "
                            + msg("Timeline"));
                    } else if (tab.equals(TAB_EARTH)
                               && getMapManager().isGoogleEarthEnabled(
                                   request)) {
                        StringBuffer earthSB = new StringBuffer();
                        getMapManager().getGoogleEarth(
                            request, allEntries, earthSB, ""
                            + (contentsWidth
                               - MapManager.EARTH_ENTRIES_WIDTH), ""
                                   + contentsHeight, true, false);
                        tabContents.add(HtmlUtils.div(earthSB.toString(),
                                HtmlUtils.style("min-width:" + minWidth
                                    + "px")));
                        tabTitles.add(
                            HtmlUtils.img(getIconUrl(ICON_GOOGLEEARTH)) + " "
                            + msg("Earth"));
                    }
                }
            }
        }
        String tabs;

        if (tabContents.size() == 1) {
            tabs = HtmlUtils.div(tabContents.get(0),
                                 HtmlUtils.cssClass("search-list"));
        } else {
            tabs = OutputHandler.makeTabs(tabTitles, tabContents, true);
        }
        if (request.get(ARG_SEARCH_SHOWHEADER, true)) {
            sb.append(HtmlUtils.h2(label));
        }

        StringBuffer rightSide = new StringBuffer();
        getRepository().getHtmlOutputHandler().showNext(request,
                allEntries.size(), rightSide);
        rightSide.append(tabs);

        boolean showForm = request.get(ARG_SEARCH_SHOWFORM, true);
        if (showForm) {
            sb.append(
                "<table width=100% border=0 cellpadding=0 cellspacing=0><tr valign=top>");
            sb.append(HtmlUtils.col(formSB.toString(), ""));
            sb.append(
                HtmlUtils.col(
                    rightSide.toString(),
                    HtmlUtils.style("min-width:" + minWidth + "px;")
                    + HtmlUtils.attr(HtmlUtils.ATTR_ALIGN, "left")));
            sb.append("</table>");
        } else {
            sb.append(rightSide);
        }
        sb.append(HtmlUtils.sectionClose());


        sb.append(HtmlUtils.script(js.toString()));

        return null;

    }




    /**
     * _more_
     *
     * @param request _more_
     * @param formSB _more_
     *
     * @throws Exception _more_
     */
    private void makeSearchForm(Request request, Appendable formSB)
            throws Exception {

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

        formSB.append("<div style=\"min-width:200px;\">");
        formSB.append(request.form(URL_SEARCH,
                                   HtmlUtils.attr(HtmlUtils.ATTR_NAME,
                                       "apisearchform")));
        formSB.append(HtmlUtils.br());
        if (getSearchManager().isLuceneEnabled()) {
            String ancestor = request.getString(ARG_ANCESTOR + "_hidden",
                                  request.getString(ARG_ANCESTOR, null));

            Entry ancestorEntry = (ancestor == null)
                                  ? null
                                  : getEntryManager().getEntry(request,
                                      ancestor);
            String select =
                getRepository().getHtmlOutputHandler().getSelect(request,
                    ARG_ANCESTOR, "Search under", true, "", ancestorEntry,
								 true,true);

            formSB.append(HU.hidden(ARG_ANCESTOR + "_hidden",
                                    (ancestor != null)
                                    ? ancestor
                                    : "", HU.id(ARG_ANCESTOR + "_hidden")));
            formSB.append(select + "<br>"
                          + HU.disabledInput(ARG_ANCESTOR,
                                             (ancestorEntry != null)
                                             ? ancestorEntry.getName()
                                             : "", HU.SIZE_30
                                             + HU.id(ARG_ANCESTOR)));
        }



        formSB.append(HtmlUtils.formTable());
        if (showDefault && showText) {
            formSB.append(HtmlUtils.formEntry(msgLabel("Text"),
                    HtmlUtils.input(ARG_TEXT,
                                    request.getSanitizedString(ARG_TEXT, ""),
                                    HtmlUtils.id("searchinput")
                                    + HtmlUtils.SIZE_15
                                    + " autocomplete='off'   autofocus ")));
            formSB.append("<div id=searchpopup xclass=ramadda-popup></div>");
            /*
            formSB.append(
                HtmlUtils.script(
                    "Utils.searchSuggestInit('searchinput',"
                    + ((theType == null)
                       ? "null"
                       : "'" + theType + "'") + ");"));
            */

        }

        if (showDefault && showName) {
            formSB.append(HtmlUtils.formEntry(msgLabel("Name"),
                    HtmlUtils.input(ARG_NAME,
                                    request.getSanitizedString(ARG_NAME, ""),
                                    HtmlUtils.SIZE_15 + " autofocus ")));
        }

        if (showDefault && showDesc) {
            formSB.append(
                HtmlUtils.formEntry(
                    msgLabel("Description"),
                    HtmlUtils.input(
                        ARG_DESCRIPTION,
                        request.getSanitizedString(ARG_DESCRIPTION, ""),
                        HtmlUtils.SIZE_15 + " autofocus ")));
        }

        if (showDefault && showDate) {
            TypeHandler.addDateSearch(getRepository(), request, formSB,
                                      DateArgument.ARG_DATA, false);

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
                getRepository().getMapManager().createMap(request, null,
                    true, null);
            String mapSelector = selectMap.makeSelector(ARG_AREA, true, nwse);
            formSB.append(formEntry(request, msgLabel("Location"),
                                    mapSelector));
        }


        if (showDefault) {
            for (SyntheticField field : syntheticFields) {
                String id = field.id;
                formSB.append(formEntry(request, msgLabel(field.label),
                                        HtmlUtils.input(id,
                                            request.getSanitizedString(id,
                                                ""), HtmlUtils.SIZE_20)));
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



        StringBuffer buttons = new StringBuffer();
        //        buttons.append(HtmlUtils.buttons(HtmlUtils.submit(msg("Search"),
        //                ARG_SEARCH_SUBMIT), HtmlUtils.submit(msg("Refine"),
        //                    ARG_SEARCH_REFINE)));
        buttons.append(HtmlUtils.submit(msg("Search"), ARG_SEARCH_SUBMIT));

        boolean doSearch = true;
        if (doSearch) {
            buttons.append(" ");
            String baseUrl = request.getUrl();
            buttons.append(HtmlUtils.br());
            StringBuffer links = new StringBuffer();


            for (OutputType outputType : new OutputType[] {
                KmlOutputHandler.OUTPUT_KML, ZipOutputHandler.OUTPUT_ZIPTREE,
                AtomOutputHandler.OUTPUT_ATOM, JsonOutputHandler.OUTPUT_JSON,
                CsvOutputHandler.OUTPUT_CSV, ZipOutputHandler.OUTPUT_EXPORT,
                BulkDownloadOutputHandler.OUTPUT_CURL
            }) {
                if (outputType.getIcon() != null) {
                    links.append(
                        HtmlUtils.img(getIconUrl(outputType.getIcon())));
                    links.append(" ");
                }

                links.append(
                    HtmlUtils.href(
                        baseUrl + "&"
                        + HtmlUtils.arg(
                            ARG_OUTPUT,
                            outputType.toString()), outputType.getLabel()));
                links.append(HtmlUtils.br());
            }
            buttons.append(HtmlUtils.makeShowHideBlock(msg("More..."),
                    links.toString(), false));
        }


        if (request.exists(ARG_USER_ID)) {
            formSB.append(HtmlUtils.formEntry(msgLabel("User"),
                    HtmlUtils.input(ARG_USER_ID,
                                    request.getSanitizedString(ARG_USER_ID,
                                        ""), HtmlUtils.SIZE_20)));
        }

        formSB.append(HtmlUtils.formEntry("", buttons.toString()));


        formSB.append(HtmlUtils.formTableClose());
        formSB.append(HtmlUtils.formClose());
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
        getRepository().getHtmlOutputHandler().makeTable(request, entries,
                sb, null);
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
