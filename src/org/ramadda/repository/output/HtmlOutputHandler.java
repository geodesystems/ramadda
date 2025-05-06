/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.output;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.LabeledObject;
import org.ramadda.util.CategoryBuffer;
import org.ramadda.util.SortedCategoryList;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JQuery;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.SortableObject;
import org.ramadda.util.TTLCache;

import org.ramadda.util.Utils;
import org.ramadda.util.sql.SqlUtil;

import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.net.*;

import java.text.SimpleDateFormat;

import java.util.function.BiConsumer;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;
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
public class HtmlOutputHandler extends OutputHandler {

    public static final OutputType OUTPUT_TEST = new OutputType("test",
                                                     "html.test",
                                                     OutputType.TYPE_VIEW,
                                                     "", ICON_DATA);

    public static final OutputType OUTPUT_GRID =
        new OutputType("Grid Layout", "html.grid", OutputType.TYPE_VIEW, "",
                       ICON_DATA);

    public static final OutputType OUTPUT_FRAMES =
        new OutputType("Frames", "html.frames", OutputType.TYPE_VIEW, "",
                       "fa-newspaper");

    public static final OutputType OUTPUT_INFO =
        new OutputType("Information", "html.info", OutputType.TYPE_VIEW, "",
                       ICON_INFORMATION);

    public static final OutputType OUTPUT_GRAPH = new OutputType("Graph",
                                                      "default.graph",
                                                      OutputType.TYPE_VIEW,
                                                      "", ICON_GRAPH);

    public static final OutputType OUTPUT_TABLE =
        new OutputType("Table", "html.table",
                       OutputType.TYPE_VIEW | OutputType.TYPE_FORSEARCH, "",
                       ICON_TABLE);

    public static final OutputType OUTPUT_CLOUD = new OutputType("Cloud",
                                                      "default.cloud",
                                                      OutputType.TYPE_VIEW);

    public static final OutputType OUTPUT_INLINE =
        new OutputType("inline", OutputType.TYPE_INTERNAL);

    public static final OutputType OUTPUT_MAPINFO =
        new OutputType("mapinfo", OutputType.TYPE_INTERNAL);

    public static final OutputType OUTPUT_SELECTXML =
        new OutputType("selectxml", OutputType.TYPE_INTERNAL);

    public static final OutputType OUTPUT_METADATAXML =
        new OutputType("metadataxml", OutputType.TYPE_INTERNAL);

    public static final OutputType OUTPUT_LINKSXML =
        new OutputType("linksxml", OutputType.TYPE_INTERNAL);

    public static final String ATTR_WIKI_SECTION = "wiki-section";

    public static final String ATTR_WIKI_URL = "wiki-url";

    public static final String ASSOCIATION_LABEL = "Connections";

    public HtmlOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_HTML);
        addType(OUTPUT_INFO);
        addType(OUTPUT_TABLE);
        addType(OUTPUT_GRID);
        addType(OUTPUT_FRAMES);
        addType(OUTPUT_GRAPH);
        addType(OUTPUT_INLINE);
        addType(OUTPUT_MAPINFO);
        addType(OUTPUT_SELECTXML);
        addType(OUTPUT_METADATAXML);
        addType(OUTPUT_LINKSXML);
        //        addType(OUTPUT_TEST);
    }

    public HtmlOutputHandler(Repository repository, Element element,
                             boolean fromChildClass)
            throws Exception {
        super(repository, element);
    }

    @Override
    public boolean  isHtml() {
	return true;
    }

    @Override
    public boolean allowRobots() {
        return true;
    }

    @Override
    public boolean checkForHuman(Request request,OutputType outputType) {
	return true;
    }


    /**
     *  override base method. This tells the EntryManager not to prefetch the children entries
     *
     * @param request _more_
     * @param type _more_
     * @param parent _more_
     *  @return _more_
     */
    @Override
    public boolean requiresChildrenEntries(Request request, OutputType type,
                                           Entry parent) {
        return false;
    }

    public String getHtmlHeader(Request request, Entry entry)
            throws Exception {
        if (entry.isDummy() || !entry.isGroup()) {
            return "";
        }

        //        return makeHtmlHeader(request, entry, "Layout");
        return makeHtmlHeader(request, entry, "");
    }

    public String makeHtmlHeader(Request request, Entry entry, String title)
            throws Exception {
        OutputType[] types = new OutputType[] { OUTPUT_INFO, OUTPUT_TABLE,
        /*OUTPUT_GRID,*/
        OUTPUT_FRAMES, CalendarOutputHandler.OUTPUT_TIMELINE,
                CalendarOutputHandler.OUTPUT_CALENDAR };
        Appendable sb =
            new StringBuilder(
                "<table border=0 cellspacing=0 cellpadding=0><tr>");
        String selected = request.getString(ARG_OUTPUT, OUTPUT_INFO.getId());
        if (title.length() > 0) {
            sb.append("<td align=center>" + msgLabel(title) + "</td>");
        }
        for (OutputType output : types) {
            String link = HU.href(
                              request.entryUrl(
                                  getRepository().URL_ENTRY_SHOW, entry,
                                  ARG_OUTPUT, output.toString()), HU.img(
                                      getIconUrl(output.getIcon()),
                                      output.getLabel()));
            sb.append("<td align=center>");
            if (output.getId().equals(selected)) {
                HU.div(sb, link, HU.cssClass("toolbar-selected"));
            } else {
                HU.div(sb, link, HU.cssClass("toolbar"));
            }
            sb.append(" ");
            sb.append("</td>");
        }
        sb.append("</table>");

        return "<table border=0 cellspacing=0 cellpadding=0 width=100%><tr><td align=right>"
               + sb.toString() + "</td></tr></table>";
    }

    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
        List<Entry> entries = state.getAllEntries();
        if (state.getEntry() != null) {
            links.add(makeLink(request, state.getEntry(), OUTPUT_HTML));
            links.add(makeLink(request, state.getEntry(), OUTPUT_INFO));
            if (entries.size() > 1) {
                links.add(makeLink(request, state.getEntry(), OUTPUT_TABLE));
                links.add(makeLink(request, state.getEntry(),
                                   OUTPUT_FRAMES));
                //                links.add(makeLink(request, state.getEntry(), OUTPUT_TEST));
                //                links.add(makeLink(request, state.getEntry(), OUTPUT_GRID));
            }
        }
    }

    public Result getMapInfo(Request request, Entry entry) throws Exception {
        return getMapInfo(request, entry, true);
    }

    public Result getMapInfo(Request request, Entry entry, boolean asXml)
            throws Exception {
        String html = null;
        Result typeResult = entry.getTypeHandler().getHtmlDisplay(request,
                                entry);
        if (typeResult != null) {
            byte[] content = typeResult.getContent();
            if (content != null) {
                html = new String(content);
            }
        }

        if (html == null) {
            String wikiTemplate = getWikiText(request, entry);
            if (wikiTemplate != null) {
                String wiki = getWikiManager().wikifyEntry(request, entry,
                                  wikiTemplate);
                html = wiki;
            } else {
                html = getMapManager().makeInfoBubble(request, entry);
            }
        }

        if (asXml) {
            StringBuffer xml = new StringBuffer(XmlUtil.XML_HEADER);
            xml.append("\n<content>\n");
            XmlUtil.appendCdata(xml, html);
            xml.append("\n</content>");

            return new Result("", xml, "text/xml");
        } else {
            return new Result("", html);
        }
    }

    private Result getMetadataXml(Request request, Entry entry,
                                  boolean showLinks)
            throws Exception {
        String contents;
        if (showLinks) {
            contents = getEntryManager().getEntryActionsTable(request, entry,
                    OutputType.TYPE_MENU);
        } else {
            StringBuilder sb = new StringBuilder();
            String snippet = getWikiManager().getSnippet(request, entry,
                                 true, null);
            if (Utils.stringDefined(snippet)) {
                sb.append(snippet);
                sb.append(HU.br());
            }
            request.put(WikiConstants.ATTR_SHOWTITLE, "false");
            entry.getTypeHandler().getEntryContent(request, entry,
						   false, true, null,false,sb);
            contents = sb.toString();
        }
        StringBuffer xml = new StringBuffer("<content>\n");
        XmlUtil.appendCdata(xml,contents);
        xml.append("\n</content>");

        return new Result("", xml, "text/xml");

    }

    public Result getLinksXml(Request request, Entry entry) throws Exception {
        StringBuffer sb = new StringBuffer("<content>\n");
        String links = getEntryManager().getEntryActionsTable(request, entry,
                           OutputType.TYPE_ALL);
        StringBuffer inner = new StringBuffer();
        String cLink =
            HU.jsLink(HU.onMouseClick("HtmlUtils.hidePopupObject();"),
                      getIconImage(ICON_CLOSE), "");
        inner.append(cLink);
        inner.append(HU.br());
        inner.append(links);
        XmlUtil.appendCdata(sb, inner.toString());
        sb.append("\n</content>");

        return new Result("", sb, "text/xml");
    }

    private void addToSelectMenu(Request request, Entry entry, StringBuilder sb) throws Exception {
	if (entry.isImage()) {
	    String url = getImageUrl(request, entry, true);
	    sb.append(HU.img(url,"",HU.attr("width","200px")));
	    return;
	} 
	List<String> urls = new ArrayList<String>();
	getMetadataManager().getThumbnailUrls(request, entry, urls);
	if (urls.size() > 0) {
	    sb.append(HU.img(urls.get(0),"",HU.attr("width","200px")));
	}

    }

    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
        TypeHandler typeHandler =
            getRepository().getTypeHandler(entry.getType());
        if (outputType.equals(OUTPUT_METADATAXML)) {
            return getMetadataXml(request, entry, true);
        }
        if (outputType.equals(OUTPUT_TEST)) {
            return outputTest(request, entry);
        }

        if (outputType.equals(OUTPUT_SELECTXML)) {
            request.setCORSHeaderOnResponse();
            StringBuilder sb     = new StringBuilder();
            String        target = request.getString(ATTR_TARGET, "");
            String        type   = request.getString(ARG_SELECTTYPE, "");
            entry.getTypeHandler().addToSelectMenu(request, entry, sb, type,
                    target);
	    if(sb.length()==0) {
		//Add the parent link for ease of navigation
		Entry parent = entry.getParentEntry();
		if (parent != null) {
		    sb.append(getSelectLink(request, parent, new HashSet(), target, "../"));
		}
		addToSelectMenu(request, entry, sb);
	    }
            return makeAjaxResult(request, sb.toString());
        }

        if (outputType.equals(OUTPUT_LINKSXML)) {
            return getLinksXml(request, entry);
        }
        if (outputType.equals(OUTPUT_MAPINFO)) {
            return getMapInfo(request, entry);
        }
        if (outputType.equals(OUTPUT_INLINE)) {
            request.setCORSHeaderOnResponse();
            String inline = typeHandler.getInlineHtml(request, entry);
            if (inline != null) {
                StringBuffer xml = new StringBuffer("<content>\n");
                XmlUtil.appendCdata(xml,
                                    "<div class=inline>" + inline + "</div>");
                xml.append("\n</content>");

                return new Result("", xml, "text/xml");
            }

            return getMetadataXml(request, entry, false);
        }

	boolean isInfo = outputType.equals(OUTPUT_INFO);
	if(isInfo) request.putExtraProperty("isinfo","true");
        return getHtmlResult(request, outputType, entry,  !isInfo);
    }

    public Result getHtmlResult(Request request, OutputType outputType,
                                Entry entry)
            throws Exception {
        return getHtmlResult(request, outputType, entry, true);
    }

    public Result getHtmlResult(Request request, OutputType outputType,
                                Entry entry, boolean checkType)
            throws Exception {

        TypeHandler typeHandler = entry.getTypeHandler();
        if (checkType) {
            Result typeResult = typeHandler.getHtmlDisplay(request, entry);
            if (typeResult != null) {
                return typeResult;
            }
        }

        ResultHandler resultHandler = new ResultHandler(request, this, entry,
                                          new State(entry));
        Appendable sb        = resultHandler.getAppendable();
        boolean    doingInfo = outputType.equals(OUTPUT_INFO);
        if (doingInfo) {
            getPageHandler().entrySectionOpen(request, entry, sb, "Entry Information");
            StringBuilder suffix = new StringBuilder();
            addDescription(request, entry, sb, true, suffix);
            String informationBlock = getInformationTabs(request, entry, false,null,true,null,true);
            sb.append(informationBlock);
            sb.append(suffix);
            getPageHandler().entrySectionClose(request, entry, sb);
        } else {
            handleDefaultWiki(request, entry, sb);
        }

        resultHandler.finish();

        return resultHandler.getResult();
        //        return makeLinksResult(request, entry.getName(), sb, new State(entry));
    }

    public void handleDefaultWiki(Request request, Entry entry, Appendable sb)
            throws Exception {
        String wikiTemplate = getWikiText(request, entry);
        String innerContent = null;
        if ((wikiTemplate != null)
                && wikiTemplate.startsWith("<wiki_inner>")) {
            innerContent = wikiTemplate;
            wikiTemplate = null;
        }

        if (innerContent == null) {
            innerContent = entry.getTypeHandler().getWikiTemplateInner();
        }

        if (wikiTemplate == null) {
            wikiTemplate = getPageHandler().getWikiTemplate(request, entry,
                    PageHandler.TEMPLATE_DEFAULT);
        }
        if (wikiTemplate == null) {
            wikiTemplate = "=={{name}}=={{description}}{{information}}";
        }

        boolean needsInnerContent = wikiTemplate.indexOf("${innercontent}")
                                    >= 0;

        if (needsInnerContent) {
            if (innerContent == null) {
                innerContent =
                    entry.getTypeHandler().getInnerWikiContent(request,
                        entry, wikiTemplate);
            }
            if (innerContent == null) {
                innerContent = getPageHandler().getWikiTemplate(request,
                        entry, PageHandler.TEMPLATE_CONTENT);
            }

            if (innerContent != null) {
                wikiTemplate = wikiTemplate.replace("${innercontent}",
                        innerContent);
            }
        }

        sb.append(getWikiManager().wikifyEntry(request, entry, wikiTemplate,
                true));
    }

    public String getAttachmentsHtml(Request request, Entry entry)
            throws Exception {
        StringBuffer metadataSB = new StringBuffer();
        getMetadataManager().decorateEntry(request, entry, metadataSB, false);
        String metataDataHtml = metadataSB.toString();
        if (metataDataHtml.length() > 0) {
            return HU.makeShowHideBlock(msg("Attachments"),
                                        "<div class=\"description\">"
                                        + metadataSB + "</div>", true);
        }

        return "";
    }

    public String getMimeType(OutputType output) {
        if (output.equals(OUTPUT_GRID) || output.equals(OUTPUT_FRAMES)
                || output.equals(OUTPUT_TABLE)) {
            return getRepository().getMimeTypeFromSuffix(".html");
        } else if (output.equals(OUTPUT_GRAPH)) {
            return getRepository().getMimeTypeFromSuffix(".xml");
        } else if (output.equals(OUTPUT_HTML) || output.equals(OUTPUT_INFO)) {
            return getRepository().getMimeTypeFromSuffix(".html");
        } else {
            return super.getMimeType(output);
        }
    }

    public List<TwoFacedObject> getMetadataHtml(Request request, Entry entry,
						MetadataType.Checker checker,
						boolean showTitle, String separator, boolean decorate,
						boolean stripe,boolean inherited,Hashtable props)
            throws Exception {

	String headingClass=Utils.getProperty(props,"headingClass","ramadda-metadata-heading");
        List<TwoFacedObject> result = new ArrayList<TwoFacedObject>();
        boolean showMetadata        = request.get(ARG_SHOWMETADATA, false);
        int toggleLimit        = Utils.getProperty(props,"propertyToggleLimit",100);
        boolean oneLine       = Utils.getProperty(props,"oneLine",false);
        boolean              tags   = request.get("tags", false);
        List<Metadata> metadataList = getMetadataManager().findMetadata(request,entry,(String)null,inherited);
        if (metadataList.size() == 0) {
            return result;
        }

        Hashtable      catMap  = new Hashtable();
        List<SortableObject<String>> cats =
            new ArrayList<SortableObject<String>>();
        List<MetadataHandler> metadataHandlers =
            getMetadataManager().getMetadataHandlers();

        boolean canEdit = getAccessManager().canDoEdit(request, entry);

        boolean smallDisplay = request.getString(ARG_DISPLAY,
                                   "").equals(DISPLAY_SMALL);
        boolean                    didone  = false;
        Hashtable<String, Boolean> typeRow = new Hashtable<String, Boolean>();

        for (Metadata metadata : metadataList) {
            MetadataType type = getRepository().getMetadataManager().findType(
                                    metadata.getType());
            if (type == null) {
                continue;
            }
	    if(checker!=null && !checker.typeOk(type)) {
		continue;
	    }

	    if(!type.getCanDisplay()) {
		continue;
	    }

            MetadataHandler metadataHandler = type.getHandler();
            String[] html = metadataHandler.getHtml(request, entry, metadata);
            if (html == null) {
                continue;
            }
            String         cat = type.getDisplayCategory();
            SortedCategoryList cb  = (SortedCategoryList) catMap.get(cat);
            if (cb == null) {
                cb = new SortedCategoryList();
                catMap.put(cat, cb);
                cats.add(new SortableObject<String>(type.getPriority(), cat));
            }

            String group = type.getDisplayGroup();
            List list =
                cb.get(type.getPriority(),group);
            Boolean rowFlag = typeRow.get(group);
            if (rowFlag == null) {
                rowFlag = Boolean.TRUE;
                typeRow.put(group, rowFlag);
            }
            boolean even = rowFlag.booleanValue();
            typeRow.put(group, Boolean.valueOf( !even));
            //      even=true;

            String  rowClass = "metadata-row "+
		(!stripe
		 ? "metadata-row"
		 : "metadata-row-" + (even
				      ? "even"
				      : "odd"));

            boolean first    = list.size() == 0;
            String  label    = html[0];
            String  contents = html[1];
            if (decorate) {
                contents = HU.div(contents,
                                  HU.cssClass("metadata-tag")
                                  + HU.attr("metadata-tag", contents));
            }
            if (tags) {
                list.add(HU.tag("div", HU.cssClass("metadata-tag"),
                                 metadata.getAttr1()));
            } else if (smallDisplay) {
		StringBuilder sb = new StringBuilder();
		list.add(sb);
                HU.open(sb, "tr", HU.attr("valign", "top") + (decorate
                        ? ""
                        : HU.cssClass(rowClass)));
                HU.open(sb, "td");
                if ( !first && (separator != null)) {
                    sb.append(separator);
                }
                sb.append(HU.tag("div", HU.cssClass("metadata-small-label"),
                                 label));
                sb.append(HU.tag("div",
                                 HU.cssClass("metadata-small-content"),
                                 contents));
                sb.append(HU.close("td"));
                sb.append(HU.close("tr"));

            } else if(oneLine) {
		StringBuilder sb = new StringBuilder();
		list.add(sb);
		sb.append("\n<table><tr><td valign=right>\n");
		sb.append(HU.b(label));
		sb.append("\n</td><td>\n");
		sb.append(contents);
		sb.append("</td></tr></table>");

            } else {
		StringBuilder sb = new StringBuilder();
		list.add(sb);
                if ( !first) {
                    sb.append("<div class=\"metadata-row-divider\"></div>");
                }
                if ( !first && (separator != null)) {
                    sb.append(separator);
                }
                HU.div(sb, contents, (decorate
                                      ? ""
                                      : HU.cssClass(rowClass)));
            }
        }

        java.util.Collections.sort(cats);

        for (SortableObject<String> po : cats) {
            String         cat = po.getValue();
            SortedCategoryList cb  = (SortedCategoryList) catMap.get(cat);
            StringBuilder  sb  = new StringBuilder();
            for (String category : cb.getCategories()) {
		List list = cb.get(category);
		StringBuilder listSB = new StringBuilder();
		for(Object o:list)
		    listSB.append(o);		
                if (tags) {
                    if (showTitle) {
                        sb.append(category);
                    }
		    sb.append(listSB);
                    sb.append("<br>");
                    continue;
                }
		String block =     HU.div(listSB.toString(),
					  HU.cssClass("metadata-block"));
		if(showTitle && list.size()>toggleLimit) {
		    sb.append(HU.open("div",HU.clazz("metadata-toggle-block")));
		    sb.append(HU.makeShowHideBlock(category, block, false));
		    sb.append(HU.close("div"));
		} else {
		    category = 	HU.div(category, HU.cssClass(headingClass));
		    if (showTitle) {
			sb.append(category);
			HU.open(sb, "div", HU.cssClass("metadata-group"));
		    }
		    sb.append(block);
		    if (showTitle) {
			HU.close(sb, "div");
		    }
		}
            }
            result.add(new TwoFacedObject(cat, sb));
        }

        return result;
    }

    public Result getActionXml(Request request, Entry entry)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(getEntryManager().getEntryActionsTable(request, entry,
                OutputType.TYPE_ALL));

        StringBuffer xml = new StringBuffer("<content>\n");
        XmlUtil.appendCdata(xml,sb.toString());
        xml.append("\n</content>");

        return new Result("", xml, "text/xml");
    }

    public Result getChildrenXml(Request request, Entry parent,
                                 List<Entry> children)
            throws Exception {
        StringBuffer sb         = new StringBuffer();
        String       folder     = getIconUrl(ICON_FOLDER_CLOSED);
        boolean      showUrl    = request.get(ARG_DISPLAYLINK, true);
        boolean      onlyGroups = request.get(ARG_ONLYGROUPS, false);

        int          cnt        = 0;
        StringBuffer jsSB       = new StringBuffer();
        String       rowId;
        String       cbxId;
        String       cbxWrapperId;

	//See if we include the
	boolean showInfo =parent.getTypeHandler().getTypeProperty("inline.includeinformation",false);
	if(showInfo) {
            String inline = parent.getTypeHandler().getInlineHtml(request, parent) ;
	    if(inline!=null) sb.append(inline);
	}

        if ( !showingAll(request, children)) {
            sb.append(msgLabel("Showing") + " 1.." + (children.size()));
            sb.append(HU.space(2));
            String url = request.getEntryUrlPath(
                             getRepository().URL_ENTRY_SHOW.toString(),
                             parent);
            url = HU.url(url, ARG_ENTRYID, parent.getId());
            HU.href(sb, url, msg("More..."));
            sb.append(HU.br());
        }

        boolean showDetails = request.get(ARG_DETAILS, true);
        boolean showIcon    = request.get("showIcon", true);

        for (Entry subGroup : children) {
            cnt++;
            addEntryTableRow(request, subGroup, sb, jsSB, showDetails,
                             showIcon);
        }

	//Only add the info when there are no children and we haven't included the info above
        if (cnt == 0 && !showInfo) {
	    parent.getTypeHandler().handleNoEntriesHtml(request, parent, sb);
            String snippet = getWikiManager().getSnippet(request, parent,
                                 true, null);
            if (Utils.stringDefined(snippet)) {
                sb.append(snippet);
                sb.append(HU.br());
            }
            request.put(WikiConstants.ATTR_SHOWTITLE, "false");
            parent.getTypeHandler().getEntryContent(request,parent, false, true, null,false,sb);
        }

        StringBuffer xml = new StringBuffer("<response><content>\n");
        XmlUtil.appendCdata(xml, sb.toString());
        xml.append("\n</content>");

        xml.append("<javascript>");
        XmlUtil.appendCdata(xml, jsSB.toString());
        xml.append("</javascript>");
        xml.append("\n</response>");

        return new Result("", xml, "text/xml");
    }

    @Override
    public void getServiceInfos(Request request, Entry entry,
                                List<ServiceInfo> services) {
        super.getServiceInfos(request, entry, services);
        try {
            String url = request.getAbsoluteUrl(
                             request.makeUrl(
                                 getRepository().URL_ENTRY_SHOW, ARG_ENTRYID,
                                 entry.getId(), ARG_OUTPUT,
                                 OUTPUT_HTML.toString()));
            String icon = getPageHandler().getIconUrl(request, entry);
            ServiceInfo serviceInfo = new ServiceInfo(OUTPUT_HTML.toString(),
                                          "HTML Display - "
                                          + entry.getName(), url,
                                              request.getAbsoluteUrl(icon));
            if ( !services.contains(serviceInfo)) {
                services.add(serviceInfo);
            }
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    private List<Entry> getSelectEntries(Request request, List<Entry> children) {
	if(request.defined(ARG_ENTRYTYPE)) {
	    List<Entry> tmp = new ArrayList<Entry>();
	    List<Entry> byType = new ArrayList<Entry>();	    
	    List<String> types = Utils.split(request.getString(ARG_ENTRYTYPE,""), ",",true,true);
	    for(Entry child: children) {
		boolean didIt = false;
		for(String type: types) {
		    if(type.equals("isgroup") && child.getTypeHandler().isGroup() ||
		       child.getTypeHandler().isType(type)) {
			byType.add(child);
			didIt = true;
			break;
		    }
		}
		if(didIt) continue;
		if(child.isGroup()) {
		    tmp.add(child);
		    continue;
		}
	    }
	    byType.addAll(tmp);
	    children=byType;
	}
	return children;
    }

    public Result getSelectXml(Request request, Entry group, List<Entry> children)
            throws Exception {
	children = getSelectEntries(request, children);
        String        selectType = request.getString(ARG_SELECTTYPE, "");
        boolean       isImage    = Utils.equals(selectType, "image");
        boolean       isFieldName    = Utils.equals(selectType, "fieldname");	
        String        localeId   = request.getString(ARG_LOCALEID, null);
        String        target     = request.getString(ATTR_TARGET, "");
        StringBuilder sb         = new StringBuilder();
        boolean       didExtra   = false;
        HashSet       seen       = new HashSet();
        String sectionDivider =
            HU.tag("hr",
                   HU.style("padding:0px;margin:0px;margin-bottom:0px;"));

        boolean firstCall = false;

	Entry localeEntry = null;
        //If we have a localeid that means this is the first call
        if (localeId != null) {
            firstCall = true;
            localeEntry = getEntryManager().getEntry(request, localeId);
	}

        if (firstCall) {
	    String type = request.getString(ARG_ENTRYTYPE,null);
	    if(type!=null)  {
		StringBuilder tmp = new StringBuilder();
		String typeLabel  = null;
		if(stringDefined(type)) {
		    for(String _type:Utils.split(type,",",true,true)) {
			TypeHandler typeHandler = getRepository().getTypeHandler(_type);
			if(typeHandler==null) continue;
			if(tmp.length()>0) tmp.append(" - ");
			tmp.append(typeHandler.getDescription());
		    }
		    typeLabel = tmp.toString();
		}
		if(typeLabel==null)
		    typeLabel  = Utils.makeLabel(type);
		typeLabel = request.getString("typelabel",typeLabel);
		Request newRequest = new Request(getRepository(), request.getUser());
		newRequest.put(Constants.ARG_MAX, "100");
		newRequest.put(Constants.ARG_ORDERBY, Constants.ORDERBY_CREATEDATE);
		newRequest.put(Constants.ARG_ASCENDING,"false");
		List<Entry> byType =   getEntryManager().getEntriesWithType(newRequest, type);
		boolean didOne = false;
		for(Entry entry: byType) {
		    String link = getSelectLink(request, entry, seen, target);
		    if (link.length() == 0) {
			continue;
		    }
		    if (!didOne) {
			sb.append(HU.center(HU.b(typeLabel)));
			HU.open(sb, "div", HU.cssClass("ramadda-select-block  ramadda-select-inner"));
		    }
		    didOne = true;
		    sb.append(link);
		}		    
		if (didOne) {
		    HU.close(sb, "div");
		    sb.append(sectionDivider);
		}
	    }
	}

	if (localeEntry != null) {
	    if (true || target.endsWith("_fieldname")) {
		sb.append(getSelectLink(request, localeEntry, seen,
					target));
	    }
	    localeEntry = getEntryManager().getParent(request,  localeEntry);
	    if (localeEntry != null) {
		sb.append(HU.open("div",
				  HU.cssClass("ramadda-select-block ramadda-select-inner")));
		Entry grandParent = getEntryManager().getParent(request,
								localeEntry);
		String indent = "";
		sb.append(indent);
		sb.append(getSelectLink(request, localeEntry, seen,
					target));
		localeId = localeEntry.getId();
		if (grandParent != null) {
		    sb.append(getSelectLink(request, grandParent, seen,
					    target));
		}
		sb.append(HU.close("div"));
		sb.append(sectionDivider);
	    }
	}

        if (request.get("firstclick", false)) {
            firstCall = true;
        }

        if (firstCall) {
            List<Entry> recents =
                getEntryManager().getSessionEntries(request);
            if (recents.size() > 0) {
                List    favoriteLinks = new ArrayList();
                boolean didOne        = false;
                for (Entry recent : getSelectEntries(request, recents)) {
                    if (isImage && !recent.isImage() && !recent.isGroup()) {
                        continue;
                    }
                    String link = getSelectLink(request, recent, seen,
                                      target);
                    if (link.length() == 0) {
                        continue;
                    }
                    if ( !didOne) {
                        sb.append(HU.center(HU.b(msg("Recent"))));
			HU.open(sb, "div", HU.cssClass("ramadda-select-block  ramadda-select-inner"));
                    }
                    didOne = true;
                    sb.append(link);
                }

                if (didOne) {
		    HU.close(sb, "div");
                    sb.append(sectionDivider);
                }
	    }
	    if(!isFieldName) {
		HU.open(sb, "div", HU.cssClass("ramadda-select-search"));
		String searchId = HU.getUniqueId("search");
		HU.div(sb, "", HU.attrs("id", searchId));
		sb.append(HU.script( HU.call("RamaddaUtils.initEntryPopup",
					     HU.squote(searchId),
					     HU.squote(target),
					     HU.squote(request.getString("entrytype","")))));
		HU.close(sb, "div");
		sb.append(sectionDivider);
	    }

            List<FavoriteEntry> favoritesList =
                getUserManager().getFavorites(request, request.getUser());
            if (favoritesList.size() > 0) {
                HU.open(sb, "div", HU.cssClass("ramadda-select-block"));
                List    favoriteLinks = new ArrayList();
                boolean didOne        = false;
                for (FavoriteEntry favorite : favoritesList) {
                    Entry favEntry = favorite.getEntry();
                    String link = getSelectLink(request, favEntry, seen,
                                      target);
                    if (link.length() == 0) {
                        continue;
                    }
                    if ( !didOne) {
                        sb.append(HU.center(HU.b(msg("Favorites"))));
                    }
                    didOne = true;
                    sb.append(link);
                }
                HU.close(sb, "div");
                if (didOne) {
                    sb.append(sectionDivider);
                }
            }
        }

        Entry parent = group.getParentEntry();
        if (parent != null) {
            sb.append(getSelectLink(request, parent, seen, target, "../"));
        }
        HU.open(sb, "div", HU.clazz(firstCall?"ramadda-select-block":"ramadda-select-inner"));
        for (Entry subGroup : children) {
            if (Utils.equals(localeId, subGroup.getId())) {
                continue;
            }
            if (isImage && !subGroup.isImage() && !subGroup.isGroup()) {
                continue;
            }
            sb.append(getSelectLink(request, subGroup, seen, target));
        }

        HU.close(sb, "div");
        HU.close(sb, "div");
        String s = sb.toString();
        s = HU.div(s, HU.cssClass("ramadda-select-popup"));

        return makeAjaxResult(request,s);
    }

    private void addDescription(Request request, Entry entry, Appendable sb,
                                boolean open, Appendable suffix)
            throws Exception {
        String  desc   = entry.getDescription().trim();
        boolean isWiki = TypeHandler.isWikiText(desc);
        if ((desc.length() > 0) && !isWiki && !desc.equals("<nolinks>")) {
            suffix.append(HtmlUtils.sectionOpen("Description", false));
            entry.getTypeHandler().addReadOnlyWikiEditor(request, entry,
                    suffix, desc);
            suffix.append(HtmlUtils.sectionClose());
        }
        if (isWiki) {
            suffix.append(HtmlUtils.sectionOpen("Wiki Text", false));
            entry.getTypeHandler().addReadOnlyWikiEditor(request, entry,
                    suffix, desc);
            suffix.append(HtmlUtils.sectionClose());
        } else {
            String wikiTemplate =
                entry.getTypeHandler().getWikiTemplate(request, entry);
            if (Utils.stringDefined(wikiTemplate)) {
                suffix.append(HtmlUtils.sectionOpen("Wiki Template", false));
                entry.getTypeHandler().addReadOnlyWikiEditor(request, entry,
                        suffix, wikiTemplate.trim());
                suffix.append(HtmlUtils.sectionClose());
            }
        }
	List<WikiMacro> macros = entry.getTypeHandler().getWikiMacros();
	if(macros!=null){
	    suffix.append(HtmlUtils.sectionOpen("Wiki Macros", false));
	    for(WikiMacro macro: macros) {
		suffix.append(HU.b(macro.getLabel() +" - "+macro.getName()));
		suffix.append(HU.pre("{{macro name=\"" + macro.getName() +"\" entry=\"" + entry.getId()+"\"}}",
				     HU.cssClass("ramadda-wiki-macro")));
                entry.getTypeHandler().addReadOnlyWikiEditor(request, entry,
							     suffix, macro.getWikiText().trim());
	    }
	    suffix.append(HtmlUtils.sectionClose());
	    HU.script(suffix,"Utils.initCopyable('.ramadda-wiki-macro');");
	}
    }

    public String getInformationTabs(Request request, Entry entry,
                                     boolean includeSnippet,
				     List<LabeledObject>extras,
				     boolean showResource,
				     Hashtable props,boolean ...forOutput)
            throws Exception {
        List         tabTitles   = new ArrayList<String>();
        List         tabContents = new ArrayList<String>();
        StringBuffer basicSB     = new StringBuffer();
        if (includeSnippet) {
            String snippet = getWikiManager().getSnippet(request, entry,
                                 true, null);
            if (Utils.stringDefined(snippet)) {
                basicSB.append(snippet);
                basicSB.append(HU.br());
            }
        }
	if(Utils.getProperty(props,"includeSnippet",false)) {
	    String snippet = getWikiManager().getSnippet(request, entry, true,"");
	    if(stringDefined(snippet)) {
		tabTitles.add("Details");
		tabContents.add(snippet);
	    }
	}

        request.put(WikiConstants.ATTR_SHOWTITLE, "false");
        entry.getTypeHandler().getEntryContent(request, entry,
					       false, showResource, props,
					       forOutput.length>0?forOutput[0]:false,
					       basicSB);

        tabTitles.add("Information");
        tabContents.add(basicSB.toString());

	String dd = entry.getTypeHandler().getDictionary();
	if(stringDefined(dd)) {
	    tabTitles.add("Data Description");
	    tabContents.add(HU.div(dd,HU.style("min-height:200px;")));
	}

        for (TwoFacedObject tfo :
		 getMetadataHtml(request, entry, null, true, null,
				 false, true,false,props)) {
            tabTitles.add(tfo.toString());
            tabContents.add(tfo.getId());
        }
        entry.getTypeHandler().addToInformationTabs(request, entry,
                tabTitles, tabContents);

        StringBuilder comments = getCommentBlock(request, entry, true);
        if (comments.length() > 0) {
            tabTitles.add(msg("Comments"));
            //        System.out.println (comments);
            tabContents.add(comments);
        }

        String attachments = getAttachmentsHtml(request, entry);
        if (attachments.length() > 0) {
            tabTitles.add(msg("Attachments"));
            tabContents.add(attachments);
        }

        StringBuilder associationBlock = new StringBuilder();
	getAssociationManager().getAssociationBlock(request, entry,associationBlock);
        if (associationBlock.length() > 0) {
            if (request.get(ARG_SHOW_ASSOCIATIONS, false)) {
                tabTitles.add(0, msg(ASSOCIATION_LABEL));
                tabContents.add(0, associationBlock);
            } else {
                tabTitles.add(msg(ASSOCIATION_LABEL));
                tabContents.add(associationBlock);
            }
        }

	if(extras!=null) {
	    for(LabeledObject labeledObject :extras) {
		tabTitles.add(labeledObject.getLabel());
		tabContents.add(labeledObject.getObject().toString());
	    }
	}

        if (tabContents.size() == 1) {
            return tabContents.get(0).toString();
        }

        return OutputHandler.makeTabs(tabTitles, tabContents, true);
    }

    public Result outputGrid(Request request, Entry group,
                             List<Entry> children)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        makeGrid(request, children, sb);

        return makeLinksResult(request, group.getName(), sb,
                               new State(group, children));
    }

    public Result outputFrames(Request request, Entry group,
                                 List<Entry> children)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        getPageHandler().entrySectionOpen(request, group, sb, "Frames");
        makeFrames(request, children, sb, 750, "500px", null,null);
        getPageHandler().entrySectionClose(request, group, sb);

        return makeLinksResult(request, group.getName(), sb,
                               new State(group, children));
    }

    public Result outputTest(Request request, Entry group,
                             List<Entry> children)
            throws Exception {
        return outputTest(request, group);
    }

    private TTLCache<String, StringBuffer> testCache =
        new TTLCache<String, StringBuffer>(60 * 60 * 1000, -1, "HTML Test");

    public Result outputTest(Request request, Entry entry) throws Exception {
        StringBuffer sb        = new StringBuffer();
        String       selectArg = "select";

        if (request.exists(selectArg + "1")) {
            List<String> values = new ArrayList<String>();
            for (int i = 1; i < 5; i++) {
                if ( !request.exists(selectArg + i)) {
                    break;
                }
                values.add(request.getString(selectArg + i, ""));
            }
            String valueKey = entry.getId() + "::"
                              + StringUtil.join("::", values);
            StringBuffer json = testCache.get(valueKey);
            if (json == null) {
                json = new StringBuffer();
                String lastValue = values.get(values.size() - 1);
                json.append(JsonUtil.map(Utils.makeListFromValues("values",
                        JsonUtil.list(Utils.makeListFromValues("--", lastValue + "-v1",
                            lastValue + "-v2", lastValue + "-v3")))));
                //                System.err.println(json);
                testCache.put(valueKey, json);
            }

            return new Result(BLANK, json,
                              getRepository().getMimeTypeFromSuffix(".json"));
        }

        String       formId = "form" + HU.blockCnt++;
        StringBuffer js     = new StringBuffer();
        js.append("var " + formId + " = new Form(" + HU.squote(formId) + ","
                  + HU.squote(entry.getId()) + ");\n");
        sb.append(request.form(getRepository().URL_ENTRY_FORM,
                               HU.attr("id", formId)));
        sb.append(HU.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HU.input("value", "", HU.attr("id", formId + "_value")));
        js.append(JQ.submit(JQ.id(formId),
                            "return " + HU.call(formId + ".submit", "")));
        for (int selectIdx = 1; selectIdx < 10; selectIdx++) {
            sb.append(HU.p());
            List values = new ArrayList();
            values.add(new TwoFacedObject("--", ""));
            if (selectIdx == 1) {
                values.add(new TwoFacedObject("Apple", "apple"));
                values.add(new TwoFacedObject("Banana", "banana"));
                values.add(new TwoFacedObject("Orange", "orange"));
            }
            sb.append(HU.select(selectArg + +selectIdx, values,
                                (String) null,
                                HU.attr("id",
                                        formId + "_" + selectArg
                                        + selectIdx)));
            js.append(JQ.change(JQ.id(formId + "_" + selectArg + selectIdx),
                                "return "
                                + HU.call(formId + ".select",
                                          HU.squote("" + selectIdx))));
        }
        sb.append(HU.p());
        sb.append(HU.submit("submit", "Submit"));

        sb.append(HU.hr());
        sb.append(HU.img(getIconUrl("/icons/arrow.gif"), "",
                         HU.attr("id", formId + "_image")));

        sb.append(HU.script(js.toString()));
        //        System.err.println(sb);

        return new Result("test", sb);
    }

    public Result outputTable(Request request, Entry group,
                              List<Entry> children)
            throws Exception {
        StringBuffer sb     = new StringBuffer();
        String       prefix = request.getPrefixHtml();
        if (stringDefined(prefix)) {
            sb.append(prefix);
        } else {
            getPageHandler().entrySectionOpen(request, group, sb, "Table");
        }

        makeTable(request, children, sb, null);
        if (prefix == null) {
            getPageHandler().entrySectionClose(request, group, sb);
        }

        return makeLinksResult(request, group.getName(), sb,
                               new State(group, children));
    }

    public void makeGrid(Request request, List<Entry> allEntries,
                         Appendable sb)
            throws Exception {
        int cols = request.get(ARG_COLUMNS, 4);
        sb.append("<table width=100% border=0 cellpadding=10>");
        int     col           = 0;
        boolean needToOpenRow = true;
        int     width         = (int) (100 * 1.0 / (float) cols);
        for (Entry entry : allEntries) {
            if (col >= cols) {
                sb.append("</tr>");
                needToOpenRow = true;
                col           = 0;
            }
            if (needToOpenRow) {
                sb.append("<tr align=bottom>");
                needToOpenRow = false;
            }
            col++;
            sb.append("<td valign=bottom align=center width=" + width
                      + "% >");
            String url = request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                          entry, ARG_OUTPUT,
                                          OUTPUT_GRID.toString());
            List<String> urls = new ArrayList<String>();
            getMetadataManager().getThumbnailUrls(request, entry, urls);
            if (urls.size() > 0) {
                sb.append(HU.href(url,
                                  HU.img(urls.get(0), "",
                                         HU.attr(HU.ATTR_WIDTH, "100")
                                         + HU.attr("loading", "lazy"))));
                sb.append(HU.br());
            } else if (entry.isImage()) {
                String thumburl = HU.url(request.makeUrl(
                                      repository.URL_ENTRY_GET) + "/"
                                          + getStorageManager().getFileTail(
                                              entry), ARG_ENTRYID,
                                                  entry.getId(),
                                                  ARG_IMAGEWIDTH, "" + 100);

                sb.append(HU.href(url, HU.img(thumburl)));
                sb.append(HU.br());
            } else {
                sb.append(HU.br());
                sb.append(HU.space(1));
                sb.append(HU.br());
            }

            String img = getPageHandler().getEntryIconImage(request, entry);
            sb.append(HU.href(url, img));
            sb.append(HU.space(1));
            sb.append(getEntryManager().getTooltipLink(request, entry,
                    getEntryDisplayName(entry), url));
            sb.append(HU.br());
            sb.append(getDateHandler().formatDateShort(request, entry,
                    entry.getStartDate()));
            sb.append("</td>");
        }
        sb.append("</table>");
    }

    public void makeFrames(Request request, List<Entry> children,
			   Appendable sb, int width, String height,
			   String template,
			   Hashtable props)
            throws Exception {
	if(props == null) props = new Hashtable();
	int wtl = Utils.getProperty(props,"leftWidth",3);
	int wtr = Utils.getProperty(props,"rightWidth",12-wtl);
	String tocStyle = Utils.getProperty(props, "tocStyle","");
        StringBuilder listSB = new StringBuilder();
        String entryShowUrl  = request.makeUrl(getRepository().URL_ENTRY_SHOW);
        listSB.append("\n");
        String firstLink = null;
        String containerId    = HU.getUniqueId("frames_container");
        String viewId    = HU.getUniqueId("frames_");
	boolean showIcon  = Utils.getProperty(props,"showIcon",true);
	String icon  = Utils.getProperty(props,"icon",null);
	int cnt= 0;
	boolean havePrefix = false;
        for (Entry child : children) {
	    String prefix = (String)props.get("category." +child.getId());
	    if(prefix!=null)  {
		havePrefix = true;
		HU.div(listSB,  prefix, HU.attrs(new String[] {"class", "ramadda-frames-category"}));
	    }
	    cnt++;
            String entryIcon = getPageHandler().getIconUrl(request, child);
	    if(icon!=null) entryIcon = getPageHandler().getIconUrl(icon);
	    String labelId    = HU.getUniqueId("frames_label_");
            String label = Utils.getProperty(props,"title." +child.getId(),getEntryManager().getEntryListName(request, child));
            String leftLabel = showIcon?HU.img(entryIcon,"",HU.attr("width",ICON_WIDTH)) + " " + label:label;
            label = label.replace("'", "\\'");
            String url = HU.url(entryShowUrl, ARG_ENTRYID, child.getId());
            if (firstLink == null) {
                firstLink = HU.href(
                    url,
                    HU.img(getRepository().getIconUrl("fa-solid fa-link"))
                    + " " + label, HU.attr("target","_link") +HU.cssClass("ramadda-clickable"));
            }

            String call = Utils.concatString(
                "javascript:",
                HU.call(
                    "Utils.framesClick",
                    HU.jsMakeArgs(
				  false, HU.squote(containerId),
				  HU.squote(viewId), HU.squote(labelId),
				  HU.squote(child.getId()),
				  HU.squote(url), HU.squote(label),
				  template==null? HU.squote("empty"):HU.squote(template),
				  HU.squote(entryIcon))));
            HU.open(listSB, HU.TAG_DIV, HU.attrs(new String[] {"id",labelId,
			"style",tocStyle,
			"data-template", template==null? HU.squote("empty"):HU.squote(template),
			"data-url",url,
			"data-label",label,
			"class","ramadda-clickable ramadda-frames-entry " + (cnt==1?"ramadda-frames-entry-active":"") }));
            HU.div(listSB, (havePrefix?"&nbsp;&nbsp;":"")+leftLabel,
		   HU.style("display:inline-block;width:100%;")+HU.attr("title", "Click to view " + label));
            HU.close(listSB, HU.TAG_DIV);
	    listSB.append("\n");
        }

        String left = HU.div(listSB.toString(),
			     HU.id(containerId)+
                             HU.cssClass("ramadda-frames-entries")+
			     HU.style(HU.css("height", HU.makeDim(height, "px"))));
        sb.append("<div class=\"row\" style=\"margin:0px; \">");
	HU.div(sb,"",HU.attrs("class","col-md-" + wtl+" ramadda-frames-leftheader",
			      "id",viewId + "_leftheader",
			      "style","margin:0px; padding:0px;"));
	HU.open(sb,"div",HU.attrs("class","col-md-" + wtr + " ramadda-frames-header",
				  "id",viewId + "_header"));
        HU.div(sb, firstLink, HU.id(viewId + "_header_link"));
        HU.close(sb,"div","div");
	sb.append("\n");
        sb.append("<div class=\"ramadda-frames\">\n");
        sb.append("<div class=\"row\" style=\"margin:0px; \">\n");
        sb.append("<div class=\"col-md-" + wtl + " ramadda-frames-left \" >\n");
        sb.append(left);
        HU.close(sb,"div");
	sb.append("\n");
        String initUrl = getRepository().getUrlBase() + "/blank";
        if (children.size() > 0) {
            Entry  initEntry = children.get(0);
            String initId    = request.getString("initEntry", (String) null);
            if (initId != null) {
                for (Entry child : children) {
                    if (child.getId().equals(initId)) {
                        initEntry = child;

                        break;
                    }
                }
            }
            initUrl = request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                       initEntry);
	    if(template!=null) {
		if(!template.equals("default"))
		    initUrl = HU.url(initUrl, "template",template);
	    }  else {
		initUrl = HU.url(initUrl, "template","empty");
	    }
        }
	HU.open(sb,"div",HU.cssClass("col-md-" + wtr     + " ramadda-frames-right") +
		   HU.style(HU.css("height", HU.makeDim(height, "px"))));
        String attrs = HU.attrs("id", viewId, "src", initUrl, "width",
                                "" + ((width < 0)
                                      ? (-width + "%")
                                      : width), "height", "100%");
        HU.tag(sb, "iframe", attrs, "");
        HU.close(sb,"div","div","div");
	HU.script(sb, HU.call("Utils.framesInit",
			      HU.jsMakeArgs(true,containerId, viewId,
					    template==null? "empty":template)));

    }

    public void makeTable(Request request, List<Entry> allEntries,
                          Appendable sb, Hashtable props)
            throws Exception {

	boolean doInlineEdit = false;

	String NA = "---";

        if (props == null) {
            props = new Hashtable();
        }
        boolean showCategories = request.get(ARG_SHOWCATEGORIES,
                                             Utils.getProperty(props,
                                                 ARG_SHOWCATEGORIES, true));
        boolean showAllTypes = request.get("showAllTypes",
                                             Utils.getProperty(props,
                                                 "showAllTypes", false));

	if(showAllTypes) showCategories = true;

        Hashtable<String, List<Entry>> map = new Hashtable<String,
                                                 List<Entry>>();
        List<String> displayColumns = null;
	String tmpColumns = Utils.getProperty(props, "columns", null);
	if(tmpColumns!=null) {
	    displayColumns = new ArrayList<String>();
	    for(String col: Utils.split(tmpColumns,",",true,true)) {
		if(!col.startsWith("#")) displayColumns.add(col);

	    }
	}
        boolean showColumns = Utils.getProperty(props, "showColumns", true);
        boolean showDate = Utils.getProperty(props, "showDate", true);
        boolean showCreateDate = Utils.getProperty(props, "showCreateDate", false);
        boolean showChangeDate = Utils.getProperty(props, "showChangeDate", true);
        boolean showFromDate = Utils.getProperty(props, "showFromDate", false);
        boolean showToDate = Utils.getProperty(props, "showToDate", false);		
        boolean showEntryDetails = Utils.getProperty(props, "showEntryDetails",  true);	
        List<String> types = new ArrayList<String>();
        for (Entry entry : allEntries) {
            TypeHandler  typeHandler = entry.getTypeHandler();
            String       type        = typeHandler.getType();
            List<Column> columns     = typeHandler.getColumns();
            boolean      hasFields   = false;
            if (columns != null) {
                for (Column column : columns) {
                    if (column.getCanList() && column.getCanShow()
                            && (column.getRows() <= 1)) {
                        hasFields = true;
                    }
                }
            }
            if ( !hasFields) {
                if (typeHandler.isGroup()) {
                    type = "Folders";
                } else if (entry.isFile()) {
                    type = "Files";
                }
            }

            if ( !showCategories) {
                type = "entries";
            }

	    if(showAllTypes) type  =entry.getTypeHandler().getType();
            List<Entry> entries = map.get(type);
            if (entries == null) {
                entries = new ArrayList<Entry>();
                map.put(type, entries);
                types.add(type);
            }
            entries.add(entry);
        }
        List<String> contents = new ArrayList<String>();
        List<String> titles   = new ArrayList<String>();

        int          typeCnt  = 0;
	String guid = HU.getUniqueId("div_");
	sb.append(HU.open("div",HU.attrs("id",guid)));

        for (String type : types) {
            typeCnt++;
            List<Entry>  entries     = map.get(type);
            TypeHandler  typeHandler = entries.get(0).getTypeHandler();
	    List<Column> columns     = typeHandler.getColumns();
            String       typeLabel   = type.equals("File")
                                       ? "File"
                                       : typeHandler.getLabel();

            StringBuffer tableSB     = new StringBuffer();
            tableSB.append("<div class=\"entry-table-wrapper\">");
            String tableId = HU.getUniqueId("entrytable_");
            tableSB.append(HU.open(HU.TAG_TABLE, HU.attrs(new String[] {
                "class", "ramadda-table entry-table", "width", "100%", "cellspacing", "0",
		"table-ordering",Utils.getProperty(props,"tableOrdering","false"),
                "cellpadding", "0", "border", "0", HU.ATTR_ID, tableId
            })));
            tableSB.append("<thead>");
            tableSB.append("<tr valign=bottom>");
	    List<MetadataType> showMetadata = null;
	    String tmp = Utils.getProperty(props,"showMetadata",null);
	    if(tmp!=null) {
		showMetadata = new ArrayList<MetadataType>();
		for(String mtdType:Utils.split(tmp,",",true,true)) {
		    MetadataType mtd = getMetadataManager().findType(mtdType);
		    if(mtd!=null) showMetadata.add(mtd);
		}
	    }
            boolean haveFiles = false;
	    List<String> headers = new ArrayList<String>();
	    if(displayColumns!=null) {
		for(String col: displayColumns) {
		    String label=Utils.getProperty(props,col+"Label",null);
		    if(label==null) {
			label = typeHandler.getFieldLabel(col);
		    }
		    headers.add(label);
		}
	    } else {
		headers.add(msg(Utils.getProperty(props,"nameLabel","Name")));
		if (showDate) {
		    headers.add(Utils.getProperty(props,"dateLabel","Date"));
		}
		if (showCreateDate) {
		    headers.add(Utils.getProperty(props,"createDateLabel","Create Date"));
		}
		if (showChangeDate) {
		    headers.add(Utils.getProperty(props,"changeDateLabel","Change Date"));
		}
		if (showFromDate) {
		    headers.add(Utils.getProperty(props,"fromDateLabel","From Date"));
		}
		if (showToDate) {
		    headers.add(Utils.getProperty(props,"toDateLabel","To Date"));
		}
	    }
	    if(displayColumns==null) {
		for (Entry entry : entries) {
		    if (entry.isFile()) {
			haveFiles = true;
			break;
		    }
		}
		if (haveFiles) {
		    headers.add("Size");
		}

		if (columns != null) {
		    for (Column column : columns) {
			if ((column.getRows() <= 1) && column.getCanShow()) {
			    if (column.getCanList() && 
				Utils.getProperty(props,
						  "show" + column.getName(), showColumns)) {
				headers.add(column.getLabel());
			    }
			}
		    }
		}
	    }

	    if(showMetadata!=null) {
		for(MetadataType mtd: showMetadata) {
		    headers.add(mtd.getName());
		}
	    }

	    int numCols = headers.size();
	    for(String header: headers) {
		tableSB.append(HU.th(HU.b(header)));
	    }
            tableSB.append("</tr></thead><tbody>");

            String  blank = HU.img(getRepository().getIconUrl(ICON_BLANK));
            boolean odd   = true;
	    String dateAttrs = HU.cssClass("entry-table-date")+" width=10% align=right ";
	    BiConsumer<Entry,Long> fmt = (entry,date)->{
		String value =  getDateHandler().formatDateShort(request,  entry, date);
		tableSB.append(HU.col(value,HU.attr("data-sort",""+date) +dateAttrs));
	    };

	    int cnt = 0;
	    tableSB.append("\n\n");
            for (Entry entry : entries) {
		boolean canEdit = getAccessManager().canDoEdit(request, entry);
		typeHandler = entry.getTypeHandler();
		columns     = typeHandler.getColumns();
		String name = getEntryDisplayName(entry);
		EntryLink entryLink = showEntryDetails?getEntryManager().getAjaxLink(request, entry, name):null;
		StringBuilder toggle = new StringBuilder();
		//Limit to 3 ancestors
		HU.makeToggleInline(toggle,"",
				    getPageHandler().getBreadCrumbs(request, entry.getParentEntry(),null,null,80,3)+
				    HU.space(1),false,"title","Click to view ancestors");

		if(entryLink!=null) {
		    HU.span(toggle,
			    getIconImage("fas fa-circle-info"),
			    //			    getIconImage("fa-solid fa-info"),
			    HU.attrs("style","margin-right:4px;","class", "entry-arrow ramadda-clickable",
				     "title","Click to view contents",
				     "data-title",entry.getName(),
				     "data-url",entryLink.getFolderClickUrl()));
		}

                HU.open(tableSB, "tr",
			HU.attrs(new String[] { "class", odd
				? "odd"
				: "even", "valign", "top" }));
		if(displayColumns!=null) {
		    for(String col: displayColumns) {
			String value=null;
			if(col.equals(TypeHandler.FIELD_NAME)) {
			    String entryIcon = getPageHandler().getEntryIconImage(request, entry);
			    String url = getEntryManager().getEntryUrl(request, entry);
			    HU.col(tableSB,toggle+HU.href(url,entryIcon)+HU.space(1) +HU.href(url,name),
				   HU.attr("data-sort",name)+
				   " nowrap "
				   + HU.cssClass("entry-table-name"));
			    continue;
			} else if(col.equals(TypeHandler.FIELD_FILE)) {
			    if (entry.isFile()) {
				String downloadLink =
				    HU.href(entry.getTypeHandler().getEntryResourceUrl(request, entry),
					    HU.img(getIconUrl(ICON_DOWNLOAD), msg("Download"),""));
				HU.col(tableSB,formatFileLength(entry.getResource().getFileSize()) + " "
				       + downloadLink, HU.attr("data-sort",""+entry.getResource().getFileSize()) + " align=right nowrap ");
			    } else {
				HU.col(tableSB,"---", " align=right nowrap ");
			    }
			    continue;
			} else if(col.equals(TypeHandler.FIELD_CREATEDATE)) {
			    fmt.accept(entry,entry.getCreateDate());
			    continue;
			} else if(col.equals(TypeHandler.FIELD_CHANGEDATE)) {
			    fmt.accept(entry,entry.getChangeDate());
			    continue;
			} else if(col.equals(TypeHandler.FIELD_FROMDATE)) {
			    fmt.accept(entry,entry.getStartDate());
			    continue;
			} else if(col.equals(TypeHandler.FIELD_TODATE)) {
			    fmt.accept(entry,entry.getEndDate());
			    continue;
			} else {
			    Column column = typeHandler.getColumn(col);
			    if(column!=null) {
				String v = entry.getStringValue(request, column,"");
				String s = entry.getTypeHandler().decorateValue(request, entry, column, v);
				if (column.isNumeric()) {
				    HU.col(tableSB, s,HU.attr("align","right") +HU.attr("data-sort",v));
				} else {
				    HU.col(tableSB, s,HU.attr("data-sort",v));
				}
				continue;
			    }
			    HU.col(tableSB,NA);
			}
		    }
		}  else {
		    String entryIcon = getPageHandler().getEntryIconImage(request, entry);
		    String url = getEntryManager().getEntryUrl(request, entry);
		    String label = HU.href(url,entryIcon) + HU.space(1) +HU.href(url,name);
		    if(toggle!=null) {
			label = toggle+label;
		    } 
		    HU.col(tableSB,label,  " nowrap "+ HU.cssClass("entry-table-name"));

		    if (showDate) {
			String date = getDateHandler().formatDateShort(request,
								       entry, entry.getStartDate());
			HU.col(tableSB, date, " class=\"entry-table-date\" width=10% align=right ");
		    }

		    if (showCreateDate) {
			fmt.accept(entry,entry.getCreateDate());

		    }

		    if (showChangeDate) {
			fmt.accept(entry,entry.getChangeDate());
		    }
		    if (showFromDate) {
			fmt.accept(entry,entry.getStartDate());
		    }
		    if (showToDate) {
			fmt.accept(entry,entry.getEndDate());
		    }		
		    if (haveFiles) {
			String downloadLink =
			    HU.href(
				    entry.getTypeHandler().getEntryResourceUrl(
									       request, entry), HU.img(
												       getIconUrl(ICON_DOWNLOAD), msg("Download"),
												       ""));

			if (entry.isFile()) {
			    HU.col(tableSB,formatFileLength(entry.getResource().getFileSize()) + " "
				   + downloadLink, " align=right nowrap ");
			} else {
			    HU.col(tableSB, NA, " align=right nowrap ");
			}
		    }

		    if (columns != null) {
			for (Column column : columns) {
			    if (column.getCanShow() && (column.getRows() <= 1)) {
				if (column.getCanList()
				    && Utils.getProperty(props,
							 "show" + column.getName(), showColumns)) {
				    String s = entry.getStringValue(request, column,"");
				    s = entry.getTypeHandler().decorateValue(
									     request, entry, column, s);

				    if(canEdit && column.getDoInlineEdit()) {
					doInlineEdit = true;

					if(column.isBoolean()) {
					    List values = Utils.add(null,"true","false");
					    s = HU.select("",values,s,
							  HU.attrs("class","ramadda-entry-inlineedit",
								   "entryid",entry.getId(),
								   "data-field",column.getName()));

					} else {
					    s = HU.input("",s,HU.attrs("size","10","class","ramadda-entry-inlineedit",
								       "entryid",entry.getId(),
								       "data-field",column.getName()));
					}
				    }

				    if (column.isNumeric()) {
					tableSB.append(HU.colRight(s));
				    } else {
					HU.col(tableSB, s);
				    }
				}
			    }
			}
		    }
		}

		if(showMetadata!=null) {
		    List<Metadata> metadataList = getMetadataManager().getMetadata(request,entry);
		    for(MetadataType mtd: showMetadata) {
			List<Metadata> byType = getMetadataManager().getMetadata(request,entry,metadataList, mtd.getId());
			if(byType!=null && byType.size()>0) {
			    HU.col(tableSB,byType.get(0).getAttr1());
			} else {
			    HU.col(tableSB,NA);
			}
		    }
		}
		HU.close(tableSB, "tr");
		tableSB.append("\n");
		/*
		if(entryLink!=null) {
		    HU.open(tableSB, "tr", "class", (odd ? "odd" : "even"));
		    HU.makeTag(tableSB, "td", entryLink.getFolderBlock(),
			       "class", "entry-table-block", "colspan",
			       "" + numCols);
		    for(int i=0;i<numCols-1;i++)
			HU.col(tableSB,"",HU.style("display:none;"));
		    HU.close(tableSB, "tr");
		    }*/
                odd = !odd;
            }
            tableSB.append("\n</tbody></table>\n");
            if (typeCnt > 1) {
                sb.append("<p>");
            }

            tableSB.append("</div>");
	    tableSB.append(HU.script("RamaddaUtils.initToggleTable('#" + tableId+"');"));
            contents.add(tableSB.toString());
            titles.add(typeLabel);

        }
        if (types.size() == 1) {
            sb.append(contents.get(0));
        } else {
            HU.makeAccordion(sb, titles, contents);
            /*            for(int i=0;i<titles.size();i++) {
                String title = titles.get(i);
                String content = contents.get(i);
                sb.append(HU.makeShowHideBlock(title,
                                                      HU.insetLeft(content, 10), true));
                                                      }*/
        }

	sb.append(HU.close("div"));
	if(doInlineEdit) {
	    sb.append(HU.script(HU.call("RamaddaUtils.applyInlineEdit",
					HU.squote("#" + guid +" .ramadda-entry-inlineedit"))));
	}

    }

    @Override
    public Result outputGroup(final Request request,
                              final OutputType outputType, final Entry group,
                              final List<Entry> children)
            throws Exception {

        final boolean[] haveCalled = { false };
        TypeHandler.Entries getChildren = () -> {
            try {
                if(children.size()==0 && !haveCalled[0]) {
                    haveCalled[0] = true;
                    getEntryManager().getChildrenEntries(request, this, group,children);
                }
		return children;
            } catch(Exception exc) {
                throw new RuntimeException(exc);
            }
        };
        //This is a terrible hack but check if the request is for the timeline xml. If it is let the 
        //CalendarOutputHandler handle it.
        if (request.get("timelinexml", false)) {
            Result timelineResult =
                getCalendarOutputHandler().handleIfTimelineXml(request,
                    group, getChildren.get());

            return timelineResult;
        }

        boolean isSearchResults = group.isDummy();
        TypeHandler typeHandler =
            getRepository().getTypeHandler(group.getType());

        if (outputType.equals(OUTPUT_INLINE)) {
            request.setCORSHeaderOnResponse();
            return getChildrenXml(request, group, getChildren.get());
        }

        if (outputType.equals(OUTPUT_SELECTXML)) {
            //First check if this entry type can add anything
            request.setCORSHeaderOnResponse();
            StringBuilder sb     = new StringBuilder();
            String        target = request.getString(ATTR_TARGET, "");
            String        type   = request.getString(ARG_SELECTTYPE, "");
            group.getTypeHandler().addToSelectMenu(request, group, sb, type,
						   target);

            if (sb.length() > 0) {
                return makeAjaxResult(request, sb.toString());
            }

            //Else handle it as a group
            return getSelectXml(request, group, getChildren.get());
        }

        if (outputType.equals(OUTPUT_METADATAXML)) {
            return getMetadataXml(request, group, true);
        }
        if (outputType.equals(OUTPUT_MAPINFO)) {
            return getMapInfo(request, group);
        }

        if (outputType.equals(OUTPUT_LINKSXML)) {
            return getLinksXml(request, group);
        }

        if (outputType.equals(OUTPUT_GRID)) {
            return outputGrid(request, group, getChildren.get());
        }
        if (outputType.equals(OUTPUT_FRAMES)) {
            return outputFrames(request, group, getChildren.get());
        }

        if (outputType.equals(OUTPUT_TEST)) {
            return outputTest(request, group, getChildren.get());
        }

        if (outputType.equals(OUTPUT_TABLE)) {
            return outputTable(request, group, getChildren.get());
        }

        boolean doSimpleListing = !request.exists(ARG_OUTPUT);
        boolean doingInfo       = outputType.equals(OUTPUT_INFO);
        if ( !doingInfo) {
            if (typeHandler != null) {
                Result typeResult = typeHandler.getHtmlDisplay(request,
                                        group, getChildren);
                if (typeResult != null) {
                    return typeResult;
                }
            }
        }

        ResultHandler resultHandler = new ResultHandler(request, this, group,
                                          new State(group));
        Appendable sb = resultHandler.getAppendable();
        request.appendMessage(sb);
        String prefix = request.getPrefixHtml();
        if (prefix != null) {
            sb.append(prefix);
        }
        String        wikiTemplate = null;
        StringBuilder suffix       = new StringBuilder();
        if ( !doingInfo && !group.isDummy()) {
            handleDefaultWiki(request, group, sb);
        } else {
            if ( !group.isDummy()) {
                getPageHandler().entrySectionOpen(request, group, sb,
                        "Entry Information");
                addDescription(request, group, sb, true, suffix);
                if ( !doSimpleListing) {
                    sb.append(getInformationTabs(request, group, false,null,true,null,true));
                }

                StringBuffer metadataSB = new StringBuffer();
		getMetadataManager().decorateEntry(request, group,
						   metadataSB, false);
		String metataDataHtml = metadataSB.toString();
		if (metataDataHtml.length() > 0) {
		    sb.append(HU.makeShowHideBlock(msg("Attachments"),
						   "<div class=\"description\">" + metadataSB
						   + "</div>", false));
		}
            }

            List<Entry> myChildren = getChildren.get();
            if (request.defined(ARG_ORDERBY)) {
                myChildren = getEntryUtil().sortEntriesOn(myChildren,
                        request.getString(ARG_ORDERBY),
                        !request.get(ARG_ASCENDING, false));
            }

            if (myChildren.size() > 0) {
                Hashtable props = new Hashtable();
                props.put(ARG_SHOWCRUMBS, "" + group.isDummy());
		HU.addPageSearch(sb,".entry-list-row-data",null,"Find in page");
		if(doingInfo) {
		    props.put("showEntryOrder","true");
		    props.put("inlineEdit","true");		    
		}
                sb.append(getWikiManager().makeTableTree(request, null,
                        props, myChildren));
            }

            if ( !group.isDummy() && (myChildren.size() == 0)) {
                if (getAccessManager().hasPermissionSet(group,
                        Permission.ACTION_VIEWCHILDREN)) {
                    if ( !getAccessManager().canDoViewChildren(request,
                            group)) {
                        sb.append(
                            getPageHandler().showDialogWarning(
                                "You do not have permission to view the sub-folders of this entry"));
                    }
                }
            }
        }

        if (doingInfo && !group.isDummy()) {
            getPageHandler().entrySectionClose(request, group, sb);
        }
        sb.append(suffix);
        String rsuffix = request.getSuffixHtml();
        if (rsuffix != null) {
            sb.append(rsuffix);
        }

        resultHandler.finish();

        return resultHandler.getResult();
    }

    public String getTimelineApplet(Request request, List<Entry> entries)
            throws Exception {
        String timelineAppletTemplate =
            getRepository().getResource(
                "/org/ramadda/repository/resources/web/timelineapplet.html");
        List times  = new ArrayList();
        List labels = new ArrayList();
        List ids    = new ArrayList();
        for (Entry entry : entries) {
            String label = getEntryDisplayName(entry);
            label = label.replaceAll(",", " ");
            times.add(SqlUtil.format(new Date(entry.getStartDate())));
            labels.add(label);
            ids.add(entry.getId());
        }
        String tmp = StringUtil.replace(timelineAppletTemplate, "${times}",
                                        StringUtil.join(",", times));
        tmp = StringUtil.replace(tmp, "${root}",
                                 getRepository().getUrlBase());
        tmp = StringUtil.replace(tmp, "${labels}",
                                 StringUtil.join(",", labels));
        tmp = StringUtil.replace(tmp, "${ids}", StringUtil.join(",", ids));
        tmp = StringUtil.replace(
            tmp, "${loadurl}",
            request.makeUrl(
                getRepository().URL_ENTRY_GETENTRIES, ARG_ENTRYIDS, "%ids%",
                ARG_OUTPUT, OUTPUT_HTML.toString()));

        return tmp;

    }

    public String processText(Request request, Entry entry, String text) {
        int idx = text.indexOf("<more>");
        if (idx >= 0) {
            String first  = text.substring(0, idx);
            String base   = "" + (HU.blockCnt++);
            String divId  = "morediv_" + base;
            String linkId = "morelink_" + base;
            String second = text.substring(idx + "<more>".length());
            String moreLink = "javascript:Utils.showMore(" + HU.squote(base)
                              + ")";
            String lessLink = "javascript:Utils.hideMore(" + HU.squote(base)
                              + ")";
            text = first + "<br><a " + HU.id(linkId) + " href="
                   + HU.quote(moreLink)
                   + ">More...</a><div style=\"\" class=\"moreblock\" "
                   + HU.id(divId) + ">" + second + "<br>" + "<a href="
                   + HU.quote(lessLink) + ">...Less</a>" + "</div>";
        }
        text = text.replaceAll("\r\n\r\n", "<p>");
        text = text.replace("\n\n", "<p>");

        return text;
    }

}
