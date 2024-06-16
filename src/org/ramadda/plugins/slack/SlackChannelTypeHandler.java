/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.slack;



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
public class SlackChannelTypeHandler extends ExtensibleGroupTypeHandler {

    /** _more_ */
    public static final int IDX_CHANNEL_ID = 0;

    /** _more_ */
    public static final int IDX_CHANNEL_PURPOSE = 1;

    /** _more_ */
    private SimpleDateFormat displaySdf = new SimpleDateFormat("HH:mm");

    /** _more_ */
    private SimpleDateFormat labelSdf = new SimpleDateFormat("MMMMM dd");

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public SlackChannelTypeHandler(Repository repository, Element entryNode)
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
        Entry teamEntry = entry.getParentEntry();
        SlackTeamTypeHandler slackTeamTypeHandler =
            (SlackTeamTypeHandler) teamEntry.getTypeHandler();
        String token =
            (String) teamEntry.getValue(request,SlackTeamTypeHandler.IDX_TOKEN);


        List<Entry> children = getWikiManager().getEntries(request, null,
                                   originalEntry, entry, props);
        StringBuilder sb = new StringBuilder();
        sb.append(HtmlUtils.cssLink(getRepository().getUrlBase()
                                    + "/slack/slack.css"));


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
                                        "slack_day_divider_label")), HtmlUtils
                                            .cssClass("slack_day_divider")));
            }
            String entryUrl = getEntryManager().getEntryURL(request, message);
            sb.append(HtmlUtils.open(HtmlUtils.TAG_DIV,
                                     HtmlUtils.cssClass("slack_message")));
            String userId =
                message.getStringValue(request,SlackMessageTypeHandler.IDX_USER_ID, "");
            String userName =
                message.getStringValue(request,SlackMessageTypeHandler.IDX_USER_NAME, "");

            String icon = HtmlUtils.href(
                              entryUrl,
                              HtmlUtils.img(
                                  getRepository().getIconUrl(
                                      "/slack/slack.png")));

            SlackUser slackUser = slackTeamTypeHandler.getSlackUser(token,
                                      userId, userName);
            if (slackUser != null) {
                userName = slackUser.getName();
                if (slackUser.getImage48() != null) {
                    icon = HtmlUtils.href(
                        entryUrl, HtmlUtils.img(slackUser.getImage48()));
                }
            }

            sb.append(icon);
            sb.append(HtmlUtils.space(1));
            sb.append(HtmlUtils.b(userName));
            sb.append(HtmlUtils.space(1));
            sb.append(displaySdf.format(dttm));
            sb.append(HtmlUtils.div(message.getDescription(),
                                    HtmlUtils.cssClass("slack_text")));
            sb.append(HtmlUtils.close(HtmlUtils.TAG_DIV));
        }



        return sb.toString();

    }

}
