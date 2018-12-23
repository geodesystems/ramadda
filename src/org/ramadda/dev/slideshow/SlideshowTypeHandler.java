/*
* Copyright (c) 2008-2018 Geode Systems LLC
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

package org.ramadda.plugins.slideshow;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.OutputType;
import org.ramadda.repository.type.*;


import org.ramadda.util.sql.Clause;


import org.ramadda.util.sql.SqlUtil;
import org.ramadda.util.sql.SqlUtil;
import org.ramadda.util.FormInfo;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.WikiUtil;


import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.util.ArrayList;
import java.util.List;


/**
 */
public class SlideshowTypeHandler extends GenericTypeHandler {

    /** _more_ */
    public static final String ICON_SLIDESHOW = "ramadda.icon.slideshow";

    /** _more_ */
    public static final String ICON_SLIDE_UP = "ramadda.icon.slide.up";

    /** _more_ */
    public static final String ICON_SLIDE_DOWN = "ramadda.icon.slide.down";

    /** _more_ */
    public static final String ICON_SLIDE_NEW = "ramadda.icon.slide.new";

    /** _more_ */
    public static final String ICON_SLIDE_COPY = "ramadda.icon.slide.copy";

    /** _more_ */
    public static final String ICON_SLIDE_DELETE =
        "ramadda.icon.slide.delete";

    /** _more_ */
    public static final String ARG_SLIDESHOW_SHOW = "slideshow.show";


    /** _more_ */
    public static final String ARG_SLIDE_DELETE = "slide.delete";

    /** _more_ */
    public static final String ARG_SLIDE_NEW = "slide.new";

    /** _more_ */
    public static final String ARG_SLIDE_UP = "slide.up";

    /** _more_ */
    public static final String ARG_SLIDE_DOWN = "slide.down";

    /** _more_ */
    public static final String ARG_SLIDE_COPY = "slide.copy";

    /** _more_ */
    public static final String ARG_SLIDE_NOTE = "slide.note";

    /** _more_ */
    public static final String ARG_SLIDE_EXTRA = "slide.extra";

    /** _more_ */
    public static final String ARG_SLIDE_TITLE = "slide.title";

    /** _more_ */
    public static final String ARG_SLIDE_CONTENT = "slide.content";

    /** _more_ */
    public static final String ARG_SLIDE_TYPE = "slide.type";

    /** _more_ */
    public static final String ARG_SLIDE_VISIBLE = "slide.visible";

    /** _more_ */
    public static final String ARG_SLIDE_ID = "slide.id";


    /** _more_ */
    public static final String TAG_SLIDESHOW = "slideshow";

    /** _more_ */
    public static final String TAG_SLIDE = "slide";

    /** _more_ */
    public static final String TAG_CONTENT = "content";

    /** _more_ */
    public static final String TAG_NOTE = "note";

    /** _more_ */
    public static final String TAG_SLIDE_ID = "slideid";

    /** _more_ */
    public static final String ATTR_SLIDE_TITLE = "title";

    /** _more_ */
    public static final String ATTR_SLIDE_VISIBLE = "visible";

    /** _more_ */
    public static final String ATTR_SLIDE_TYPE = "type";

    /** _more_ */
    public static final String TYPE_PLAIN = "type.plain";

    /** _more_ */
    public static final String TYPE_LIST = "type.list";

    /** _more_ */
    public static final String TYPE_INCREMENTAL = "type.incremental";

    /** _more_ */
    public static final String TYPE_INCREMENTALSHOWFIRST =
        "type.incrementalshowfirst";

    /** _more_ */
    public static final String CLASS_NOTES = "notes";

    /** _more_ */
    public static final String CLASS_INCREMENTAL = "incremental";

    /** _more_ */
    public static final String CLASS_INCREMENTAL_SHOWFIRST =
        "incremental show-first";

    /** _more_ */
    public static final String CLASS_SLIDE = "slide";



    //    public static final String TAG_="";
    //    public static final String TAG_="";
    //    public static final String TAG_="";
    //    public static final String TAG_="";

    /** _more_ */
    List types = new ArrayList();



    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public SlideshowTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
        types.add(new TwoFacedObject("Plain", TYPE_PLAIN));
        types.add(new TwoFacedObject("List", TYPE_LIST));
        types.add(new TwoFacedObject("Incremental list", TYPE_INCREMENTAL));
        types.add(new TwoFacedObject("Incremental list - show first",
                                     TYPE_INCREMENTALSHOWFIRST));

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param links _more_
     *
     * @throws Exception _more_
     */
    public void getEntryLinks(Request request, Entry entry, List<Link> links)
            throws Exception {

        super.getEntryLinks(request, entry, links);
        links.add(
            new Link(
                request.url(
                    getRepository().URL_ENTRY_SHOW, ARG_ENTRYID,
                    entry.getId(), ARG_SLIDESHOW_SHOW,
                    "true"), getRepository().getIconUrl(ICON_SLIDESHOW),
                             "View Slideshow", OutputType.TYPE_VIEW));
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean returnToEditForm() {
        return true;
    }




    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Element getRoot(Entry entry) throws Exception {
        Object[] values = ((entry == null)
                           ? null
                           : entry.getValues());
        Element  root   = null;
        if (values != null) {
            String xml = (String) values[0];
            if (xml != null) {
                root = XmlUtil.getRoot(xml);
            }
        }

        return root;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param parentEntry _more_
     * @param entry _more_
     * @param formInfo _more_
     *
     * @throws Exception _more_
     */
    public void addToEntryForm(Request request, StringBuffer sb,
                               Entry parentEntry, Entry entry,
                               FormInfo formInfo)
            throws Exception {
        super.addToEntryForm(request, sb, parentEntry, entry, formInfo);
        Element root = getRoot(entry);
        int     cnt  = 1;

        /*
        <slideshow>
        <slide title="title" type="type">
        </slide>
        </slideshow>
        */
        StringBuffer slides         = new StringBuffer();

        int          numberOfSlides = 0;
        if (root != null) {
            NodeList children = XmlUtil.getElements(root);
            numberOfSlides = children.getLength();

            for (int i = 0; i < children.getLength(); i++) {
                Element node = (Element) children.item(i);


                String title = XmlUtil.getAttribute(node, ATTR_SLIDE_TITLE,
                                   "");
                String type = XmlUtil.getAttribute(node, ATTR_SLIDE_TYPE, "");
                boolean visible = XmlUtil.getAttribute(node,
                                      ATTR_SLIDE_VISIBLE, true);
                String contents = XmlUtil.getGrandChildText(node,
                                      TAG_CONTENT, "");
                String note = XmlUtil.getGrandChildText(node, TAG_NOTE, "");
                String slideBlock = getSlideEdit(request, entry, cnt, title,
                                        type, contents, note);
                String hdr = msg("Slide") + " #" + cnt + " " + title;
                String editForm =
                    HtmlUtils.insetLeft(makeCommands(cnt, true, visible)
                                        + HtmlUtils.br() + slideBlock, 30);
                slides.append(HtmlUtils.makeShowHideBlock(hdr, editForm,
                        false));
                cnt++;
            }
        }


        //Add 3 new ones at first or always a new one at the end

        for (int i = 0; i < 2; i++) {
            String title    = "";
            String type     = TYPE_PLAIN;
            String contents = "";
            String slideBlock = getSlideEdit(request, entry, cnt, title,
                                             type, contents, "");
            String hdr = msg("New Slide");
            String editForm = HtmlUtils.insetLeft(makeCommands(cnt, false,
                                  true) + HtmlUtils.br() + slideBlock, 30);
            slides.append(HtmlUtils.hidden(ARG_SLIDE_EXTRA + cnt, "true"));
            slides.append(HtmlUtils.makeShowHideBlock(hdr, editForm, false));
            cnt++;
        }

        sb.append(HtmlUtils.formEntryTop(msgLabel("Slides"),
                                         slides.toString()));
    }

    /**
     * _more_
     *
     * @param cnt _more_
     * @param exists _more_
     * @param visible _more_
     *
     * @return _more_
     */
    private String makeCommands(int cnt, boolean exists, boolean visible) {
        String newLink =
            HtmlUtils.submitImage(getRepository().getIconUrl(ICON_SLIDE_NEW),
                                  ARG_SLIDE_NEW + cnt, "Insert New Slide",
                                  "");
        String copyLink =
            HtmlUtils.submitImage(getRepository().getIconUrl(ICON_SLIDE_COPY),
                                  ARG_SLIDE_COPY + cnt, "Copy Slide", "");
        String upLink =
            HtmlUtils.submitImage(getRepository().getIconUrl(ICON_SLIDE_UP),
                                  ARG_SLIDE_UP + cnt, "Move Slide Up", "");
        String downLink =
            HtmlUtils.submitImage(getRepository().getIconUrl(ICON_SLIDE_DOWN),
                                  ARG_SLIDE_DOWN + cnt, "Move Slide Down",
                                  "");
        String deleteLink = ( !exists
                              ? ""
                              : HtmlUtils.submitImage(
                                  getRepository().getIconUrl(ICON_SLIDE_DELETE),
                                  ARG_SLIDE_DELETE + cnt, "Delete Slide",
                                  ""));
        String visibleCbx = HtmlUtils.checkbox(ARG_SLIDE_VISIBLE + cnt,
                                "true", visible);

        return visibleCbx + " " + msg("Visible") + HtmlUtils.space(2)
               + newLink + HtmlUtils.space(2) + copyLink + HtmlUtils.space(2)
               + deleteLink + HtmlUtils.space(2) + upLink
               + HtmlUtils.space(2) + downLink + HtmlUtils.space(2);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param i _more_
     * @param title _more_
     * @param type _more_
     * @param contents _more_
     * @param note _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String getSlideEdit(Request request, Entry entry, int i,
                                String title, String type, String contents,
                                String note)
            throws Exception {
        StringBuffer slideBlock = new StringBuffer();
        slideBlock.append(HtmlUtils.hidden(ARG_SLIDE_ID + i, "" + i));
        slideBlock.append(msgLabel("Title"));
        slideBlock.append(
            HtmlUtils.input(
                ARG_SLIDE_TITLE + i, title,
                HtmlUtils.attrs(HtmlUtils.ATTR_SIZE, "80")));
        slideBlock.append(HtmlUtils.br());
        slideBlock.append(msgLabel("Type"));
        slideBlock.append(HtmlUtils.select(ARG_SLIDE_TYPE + i, types, type));
        slideBlock.append(HtmlUtils.br());
        //        slideBlock.append(msgLabel("Slide Contents"));
        //        slideBlock.append(HtmlUtils.br());
        slideBlock.append(
            getRepository().getWikiManager().makeWikiEditBar(
                request, entry, ARG_SLIDE_CONTENT + i));
        slideBlock.append(HtmlUtils.br());
        slideBlock.append(HtmlUtils.textArea(ARG_SLIDE_CONTENT + i, contents,
                                             15, 80,
                                             HtmlUtils.id(ARG_SLIDE_CONTENT
                                                 + i)));
        slideBlock.append(HtmlUtils.br());
        slideBlock.append(msgLabel("Note"));
        slideBlock.append(HtmlUtils.br());
        slideBlock.append(HtmlUtils.textArea(ARG_SLIDE_NOTE + i, note, 5,
                                             80));
        slideBlock.append(HtmlUtils.p());

        return slideBlock.toString();
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param formBuffer _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void addColumnsToEntryForm(Request request,
                                      StringBuffer formBuffer, Entry entry)
            throws Exception {}



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param parent _more_
     * @param newEntry _more_
     *
     * @throws Exception _more_
     */
    public void initializeEntryFromForm(Request request, Entry entry,
                                        Entry parent, boolean newEntry)
            throws Exception {

        super.initializeEntryFromForm(request, entry, parent, newEntry);
        Document doc = XmlUtil.makeDocument();
        Element root = XmlUtil.create(doc, TAG_SLIDESHOW, null,
                                      new String[] {});
        int           cnt          = 0;
        StringBuffer  slides       = new StringBuffer();
        List<Element> nodes        = new ArrayList<Element>();
        Element       nodeToInsert = null;

        while (request.exists(ARG_SLIDE_ID + (cnt + 1))) {
            cnt++;

            boolean extra = request.get(ARG_SLIDE_EXTRA + cnt, false);
            if (request.exists(ARG_SLIDE_DELETE + cnt)) {
                continue;
            }
            boolean visible = request.get(ARG_SLIDE_VISIBLE + cnt, false);


            if (request.exists(ARG_SLIDE_NEW + cnt)) {
                nodes.add(createNode(doc, "", TYPE_PLAIN, true, "", ""));
            }

            boolean copy    = request.exists(ARG_SLIDE_COPY + cnt);
            String  title   = request.getString(ARG_SLIDE_TITLE + cnt, "");
            String  type    = request.getString(ARG_SLIDE_TYPE + cnt, "");
            String  content = request.getString(ARG_SLIDE_CONTENT + cnt, "");
            String  note    = request.getString(ARG_SLIDE_NOTE + cnt, "");
            if (extra) {
                if ((title.trim().length() == 0)
                        && (content.trim().length() == 0)) {
                    continue;
                }
            }

            Element slideNode = createNode(doc,
                                           request.getString(ARG_SLIDE_TITLE
                                               + cnt, ""), type, visible,
                                                   content, note);



            if (request.exists(ARG_SLIDE_UP + cnt) && (nodes.size() > 0)) {
                nodes.add(nodes.size() - 1, slideNode);
            } else if (request.exists(ARG_SLIDE_DOWN + cnt)) {
                nodeToInsert = slideNode;

                continue;
            } else {
                nodes.add(slideNode);
            }


            if (nodeToInsert != null) {
                nodes.add(nodeToInsert);
                nodeToInsert = null;
            }

            if (request.exists(ARG_SLIDE_COPY + cnt)) {
                nodes.add(createNode(doc,
                                     request.getString(ARG_SLIDE_TITLE + cnt,
                                         ""), type, visible, content, note));
            }
        }

        if (nodeToInsert != null) {
            nodes.add(nodeToInsert);
        }

        for (Element slideNode : nodes) {
            root.appendChild(slideNode);
        }
        String xml = XmlUtil.toString(root);
        entry.setValues(new Object[] { xml });
    }


    /**
     * _more_
     *
     * @param doc _more_
     * @param title _more_
     * @param type _more_
     * @param visible _more_
     * @param content _more_
     * @param note _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Element createNode(Document doc, String title, String type,
                               boolean visible, String content, String note)
            throws Exception {
        Element slideNode = XmlUtil.create(doc, TAG_SLIDE, new String[] {
            ATTR_SLIDE_TITLE, title, ATTR_SLIDE_TYPE, type,
            ATTR_SLIDE_VISIBLE, "" + visible
        });

        Element contentNode = XmlUtil.create(TAG_CONTENT, slideNode);
        Element noteNode    = XmlUtil.create(TAG_NOTE, slideNode);
        contentNode.appendChild(XmlUtil.makeCDataNode(doc, content, false));
        noteNode.appendChild(XmlUtil.makeCDataNode(doc, note, false));

        return slideNode;
    }


    /**
     *
     *
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result getHtmlDisplay(Request request, Entry entry)
            throws Exception {

        if (request.defined(ARG_SLIDESHOW_SHOW)) {
            return getSlideshow(request, entry);
        }

        return super.getHtmlDisplay(request, entry);
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
    public Result getSlideshow(Request request, Entry entry)
            throws Exception {

        Element      root = getRoot(entry);
        StringBuffer sb   = new StringBuffer();
        String template =
            getRepository().getResource(
                "/org.ramadda.plugins.slideshow/htdocs/slideshow/template.html");

        if (root != null) {
            WikiUtil wikiUtil = new WikiUtil(Misc.newHashtable(new Object[] {
                                    OutputHandler.PROP_REQUEST,
                                    request, OutputHandler.PROP_ENTRY,
                                    entry }));
            wikiUtil.setMakeHeadings(false);
            //      wikiUtil.setReplaceNewlineWithP(false);
            NodeList children = XmlUtil.getElements(root);
            for (int i = 0; i < children.getLength(); i++) {
                wikiUtil.removeProperty("image.class");
                String imageClass =
                    (String) wikiUtil.getProperty("image.class");
                Element node = (Element) children.item(i);
                if ( !XmlUtil.getAttribute(node, ATTR_SLIDE_VISIBLE, true)) {
                    continue;
                }
                String title = XmlUtil.getAttribute(node, ATTR_SLIDE_TITLE,
                                   "");
                String type = XmlUtil.getAttribute(node, ATTR_SLIDE_TYPE, "");
                String contents = XmlUtil.getGrandChildText(node,
                                      TAG_CONTENT, "");
                String note = XmlUtil.getGrandChildText(node, TAG_NOTE, "");
                sb.append("\n");
                sb.append(HtmlUtils.open(HtmlUtils.TAG_DIV,
                                         HtmlUtils.cssClass(CLASS_SLIDE)));
                sb.append("\n");
                if (title.length() > 0) {
                    sb.append(HtmlUtils.h1(title));
                }
                sb.append("\n");
                if (type.equals(TYPE_INCREMENTAL)
                        || type.equals(TYPE_INCREMENTALSHOWFIRST)) {
                    wikiUtil.putProperty("image.class", "incremental");
                }
                contents =
                    getRepository().getWikiManager().wikifyEntry(request,
                        entry, wikiUtil, contents, false, null, null);
                if (type.equals(TYPE_PLAIN)) {
                    sb.append(HtmlUtils.p(contents));
                    sb.append("\n");
                } else {
                    boolean hasUl = contents.indexOf("<ul>") >= 0;
                    hasUl = true;
                    if (type.equals(TYPE_LIST)) {
                        if ( !hasUl) {
                            sb.append(HtmlUtils.ul());
                        }
                    } else if (type.equals(TYPE_INCREMENTAL)) {
                        if ( !hasUl) {
                            sb.append(HtmlUtils.open(HtmlUtils.TAG_UL,
                                    HtmlUtils.cssClass(CLASS_INCREMENTAL)));
                        } else {
                            contents = contents.replace(
                                "<ul>",
                                HtmlUtils.open(
                                    HtmlUtils.TAG_UL,
                                    HtmlUtils.cssClass(CLASS_INCREMENTAL)));
                        }
                    } else {
                        String ul = HtmlUtils.open(
                                        HtmlUtils.TAG_UL,
                                        HtmlUtils.cssClass(
                                            CLASS_INCREMENTAL_SHOWFIRST));
                        if ( !hasUl) {
                            sb.append(ul);
                        } else {
                            contents = contents.replace("<ul>", ul);
                        }
                    }
                    sb.append("\n");
                    sb.append(contents);
                    sb.append("\n");
                    if ( !hasUl) {
                        sb.append(HtmlUtils.close(HtmlUtils.TAG_UL));
                    }
                    sb.append("\n");
                }
                if (note.trim().length() > 0) {
                    sb.append(HtmlUtils.div(note,
                                            HtmlUtils.cssClass(CLASS_NOTES)));
                    sb.append("\n");
                }
                sb.append(HtmlUtils.close(HtmlUtils.TAG_DIV));
                sb.append("\n");
            }
        }

        String header = entry.getName();
        String url = request.entryUrl(getRepository().URL_ENTRY_SHOW, entry);

        String footer = HtmlUtils.href(url, "Return to RAMADDA");



        template = template.replace("${head}",
                                    HtmlUtils.script("setEscapeUrl('" + url
                                        + "');"));
        template = template.replace("${urlroot}",
                                    getRepository().getUrlBase());
        template = template.replace("${title}", entry.getName());
        template = template.replace("${header}", header);
        template = template.replace("${footer}", footer);
        String jsContent = "";
        //TODO:        String jsContent = getRepository().getTemplateJavascriptContent();
        template = template.replace("${content}", sb.toString() + jsContent);

        template = getRepository().translate(request, template);

        Result result = new Result("", new StringBuffer(template));
        result.setShouldDecorate(false);

        return result;
    }




}
