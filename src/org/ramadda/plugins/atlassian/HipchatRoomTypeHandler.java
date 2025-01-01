/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.atlassian;



import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.AtomUtil;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.RssUtil;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;


import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.net.URL;


import java.text.SimpleDateFormat;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 */
public class HipchatRoomTypeHandler extends ExtensibleGroupTypeHandler {

    /** _more_ */
    public static final int IDX_ROOM_ID = 0;


    /** _more_ */
    private SimpleDateFormat displaySdf = new SimpleDateFormat("HH:mm");

    /** _more_ */
    private SimpleDateFormat labelSdf =
        new SimpleDateFormat("EEEE MMMMM dd, yyyy");

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public HipchatRoomTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {

        if ( !tag.equals("messages")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }
        Entry groupEntry = entry.getParentEntry();

        HipchatGroupTypeHandler hipchatGroupTypeHandler =
            (HipchatGroupTypeHandler) groupEntry.getTypeHandler();
        String token =
            (String) groupEntry.getValue(request,HipchatGroupTypeHandler.IDX_TOKEN);


        List<Entry> children = getWikiManager().getEntries(request, null,
                                   originalEntry, entry, props);
        StringBuilder sb = new StringBuilder();
        /*
<link rel="stylesheet" href="//aui-cdn.atlassian.com/aui-adg/5.9.0/css/aui.css" media="all">
<script src="//aui-cdn.atlassian.com/aui-adg/5.9.0/js/aui.js"/>
        */
        sb.append(HtmlUtils.cssLink(getRepository().getUrlBase()
                                    + "/atlassian/hipchat.css"));
        Date   currentDate  = null;
        String currentLabel = null;
        for (Entry message : children) {
            Date dttm = new Date(message.getStartDate());
            //            if(currentDate == null || !currentDate) {
            //                Date dttm = new Date(currentDate);
            //            }
            String label = labelSdf.format(dttm);
            if ( !Misc.equals(currentLabel, label)) {
                currentLabel = label;
                sb.append(
                    HtmlUtils
                        .div(HtmlUtils.hr()
                            + HtmlUtils
                                .div(label, HtmlUtils
                                    .cssClass(
                                        "hipchat_day_divider_label")), HtmlUtils
                                            .cssClass(
                                                "hipchat_day_divider")));
            }
            String entryUrl = getEntryManager().getEntryURL(request, message);
            String userId =
                message.getStringValue(request,HipchatMessageTypeHandler.IDX_FROM, "");
            String roomId =
                message.getStringValue(request,HipchatMessageTypeHandler.IDX_ROOM_ID, "");

            String color =
                message.getStringValue(request,HipchatMessageTypeHandler.IDX_COLOR, "");

            String icon = HtmlUtils.href(
                              entryUrl,
                              HtmlUtils.img(
                                  getRepository().getIconUrl(
                                      "/hipchat/hipchat.png")));

            sb.append(
                HtmlUtils.open(
                    HtmlUtils.TAG_DIV,
                    HtmlUtils.cssClass(
                        "hipchat_message hipchat_message_" + color)));


            /**
             * HipchatUser hipchatUser = hipchatGroupTypeHandler.getHipchatUser(token,
             *                         userId, userName);
             * if (hipchatUser != null) {
             *   userName = hipchatUser.getName();
             *   if (hipchatUser.getImage48() != null) {
             *       icon = HtmlUtils.href(
             *           entryUrl, HtmlUtils.img(hipchatUser.getImage48()));
             *   }
             * }
             */

            sb.append(icon);
            sb.append(HtmlUtils.space(1));
            sb.append(HtmlUtils.b(userId));
            sb.append(HtmlUtils.space(1));
            sb.append(displaySdf.format(dttm));

            List<String> urls = new ArrayList<String>();
            getMetadataManager().getThumbnailUrls(request, message, urls);
            for (String url : urls) {
                sb.append(HtmlUtils.br());
                sb.append(HtmlUtils.img(url));
            }
            if (urls.size() > 0) {
                sb.append(HtmlUtils.br());
            }




            sb.append(HtmlUtils.div(message.getDescription(),
                                    HtmlUtils.cssClass("hipchat_text")));
            sb.append(HtmlUtils.close(HtmlUtils.TAG_DIV));
        }



        return sb.toString();


    }

}
