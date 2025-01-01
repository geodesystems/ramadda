/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.slack;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.output.*;


import org.w3c.dom.*;

import java.io.*;

import java.net.*;



import java.util.ArrayList;
import java.util.Date;

import java.util.List;
import java.util.regex.*;
import java.util.zip.*;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class SlackOutputHandler extends OutputHandler {


    /** _more_ */
    public static final String PROP_SLACK_API_TOKEN = "slack.api.token";

    /** _more_ */
    public static final OutputType OUTPUT_SLACK_PUBLISH =
        new OutputType("Publish to Slack", "slack_publish",
                       OutputType.TYPE_VIEW, "", "/slack/slack.png");


    /**
     * _more_
     */
    public SlackOutputHandler() {}

    /**
     * _more_
     *
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public SlackOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_SLACK_PUBLISH);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param state _more_
     * @param links _more_
     *
     *
     * @throws Exception _more_
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
        if ( !request.isAnonymous() && (state.getEntry() != null)
                && state.getEntry().isFile()
                && getAccessManager().canDoEdit(
                    request, state.getEntry()) && (getRepository().getProperty(
                        PROP_SLACK_API_TOKEN, (String) null) != null)) {
            links.add(makeLink(request, state.getEntry(),
                               OUTPUT_SLACK_PUBLISH));
        }
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param outputType _more_
     * @param entry _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
        if ( !getAccessManager().canDoEdit(request, entry)) {
            throw new IllegalArgumentException("No access");
        }
        if (getRepository().getProperty(PROP_SLACK_API_TOKEN, (String) null)
                == null) {
            return new Result(
                "", new StringBuilder("No Slack API token defined"));
        }

        StringBuilder sb = new StringBuilder("slack publish stuff here");

        return new Result("", sb);

    }


}
