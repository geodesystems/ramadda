/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.media;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import org.w3c.dom.*;



import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


public class AnimatedGifTypeHandler extends ImageTypeHandler {


    public static int IDX = ImageTypeHandler.IDX_LAST + 1;

    public static final int IDX_SHOWCONTROLS = IDX++;

    public static final int IDX_ADDBUTTONS = IDX++;

    public static final int IDX_AUTOPLAY = IDX++;

    public static final int IDX_MAXWIDTH = IDX++;

    public static final int IDX_LOOPDELAY = IDX++;


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public AnimatedGifTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
        if (tag.equals("animatedgif")) {
            StringBuilder sb = new StringBuilder();
            sb.append("\n\n");
            request.putExtraProperty("libgif", "true");

            HU.importJS(sb,getPageHandler().getCdnPath("/lib/libgif/libgif.js"));
            HU.importJS(sb,getPageHandler().getCdnPath("/lib/libgif/rubbable.js"));
            HU.importJS(sb,getHtdocsPath("/src/org/ramadda/plugins/media/htdocs/media/animatedgif.js","/media/animatedgif.js"));
            HU.cssLink(sb,getHtdocsPath("/src/org/ramadda/plugins/media/htdocs/media/animatedgif.css","/media/animatedgif.css"));
            String imgUrl = entry.getTypeHandler().getEntryResourceUrl(request, entry);
            String id = HU.getUniqueId("image");
            boolean showControls =
                "true".equals(Utils.getString(props.get("showControls"),
                    entry.getStringValue(IDX_SHOWCONTROLS, "true")));
            boolean autoplay =
                "true".equals(Utils.getString(props.get("autoplay"),
                    entry.getStringValue(IDX_AUTOPLAY, "true")));
            boolean addButtons =
                "true".equals(Utils.getString(props.get("addButtons"),
                    entry.getStringValue(IDX_ADDBUTTONS, "true")));
            if (showControls) {
		sb.append("<div class=animatedgif-controls>");
                sb.append("<a href='javascript:;' onmousedown='" + id
                          + ".pause(); return false;'>"
                          + HtmlUtils.faIconClass("fa-stop",
                              "ramadda-clickable", "title",
                              "Pause") + "</a>&nbsp;&nbsp;");
                sb.append("<a href='javascript:;' onmousedown='" + id
                          + ".play(); return false;'>"
                          + HtmlUtils.faIconClass("fa-play",
                              "ramadda-clickable", "title",
                              "Play") + "</a>&nbsp;&nbsp;");
                sb.append("<a href='javascript:;' onmousedown='" + id
                          + ".move_to(0); return false;'>"
                          + HtmlUtils.faIconClass("fa-redo",
                              "ramadda-clickable", "title",
                              "Restart") + "</a>&nbsp;&nbsp;");
                sb.append("<a href='javascript:;' onmousedown='" + id
                          + ".move_relative(-1); return false;'>"
                          + HtmlUtils.faIconClass("fa-step-backward",
                              "ramadda-clickable", "title",
                              "Step back") + "</a>&nbsp;&nbsp;");
                sb.append("<a href='javascript:;' onmousedown='" + id
                          + ".move_relative(1); return false;'>"
                          + HtmlUtils.faIconClass("fa-step-forward",
                              "ramadda-clickable", "title",
                              "Step forward") + "</a>&nbsp;&nbsp;");
                sb.append("</div>");
            }
            HU.div(sb, "", HU.attrs("id", id + "_div"));
            String attrs = HU.attrs("id", id, "src", imgUrl,
                                    "rel:animated_src", imgUrl,
                                    "rel:auto_play", autoplay
                    ? "1"
                    : "0");
            int maxWidth =
                Integer.parseInt(Utils.getString(props.get("maxwidth"),
                    entry.getValue(IDX_MAXWIDTH), "-1"));
            int loopDelay =
                Integer.parseInt(Utils.getString(props.get("loopdelay"),
                    entry.getValue(IDX_LOOPDELAY), "-1"));
            List<String> objAttrs = new ArrayList<String>();
            objAttrs.add("gif");
            objAttrs.add("document.getElementById('" + id + "')");
            if (maxWidth > 0) {
                objAttrs.add("max_width");
                objAttrs.add("" + maxWidth);
            }
            if (loopDelay > 0) {
                objAttrs.add("loop_delay");
                objAttrs.add("" + loopDelay);
            }
            sb.append(HU.tag("img", attrs));
            HU.script(sb, "var " + id + " = new " + (true
                    ? "RubbableGif"
                    : "SuperGif") + "( " + JsonUtil.map(objAttrs) + " );\n");
            HU.script(sb,
                      "AnimatedGif.init(" + id + ",'" + id + "_div" + "',"
                      + addButtons + ");");

            return sb.toString();
        }

        return super.getWikiInclude(wikiUtil, request, originalEntry, entry,
                                    tag, props);

    }
}
