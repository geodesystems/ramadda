/*
* Copyright (c) 2008-2019 Geode Systems LLC
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

package org.ramadda.plugins.blog;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;


import org.ramadda.util.sql.Clause;


import org.ramadda.util.sql.SqlUtil;
import org.ramadda.util.sql.SqlUtil;


import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.WikiUtil;
import ucar.unidata.xml.XmlUtil;

import java.sql.PreparedStatement;

import java.sql.ResultSet;
import java.sql.Statement;


import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


/**
 *
 *
 */
public class BlogEntryTypeHandler extends ExtensibleGroupTypeHandler {


    /** _more_ */
    public static String TYPE_BLOGENTRY = "blogentry";

    /** _more_ */
    public static final String ARG_BLOG_TEXT = "blogentry.blogtext";

    /** _more_ */
    private WeblogOutputHandler weblogOutputHandler;

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public BlogEntryTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public String getEntryText(Entry entry) {
        Object[] values = entry.getValues();
        if ((values != null) && (values.length > 0) && (values[0] != null)) {
            String extra = ((String) values[0]).trim();

            return Utils.concatString(entry.getDescription(), extra);
        }

        return entry.getDescription();
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result getHtmlDisplay(Request request, Entry group,
                                 List<Entry> subGroups, List<Entry> entries)
            throws Exception {
        return getHtmlDisplay(request, group);
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
    @Override
    public Result getHtmlDisplay(Request request, Entry entry)
            throws Exception {
        if (weblogOutputHandler == null) {
            weblogOutputHandler =
                (WeblogOutputHandler) getRepository().getOutputHandler(
                    WeblogOutputHandler.OUTPUT_BLOG);
        }

        Entry group = entry.getParentEntry();
        if ((group != null) && !group.getTypeHandler().isType("weblog")) {
            group = null;
        }
        String entryHtml = weblogOutputHandler.getBlogEntry(request, entry,
                               true);
        entryHtml = HtmlUtils.div(entryHtml,
                                  HtmlUtils.cssClass("blog-entries"));
        StringBuilder sb = new StringBuilder();
        weblogOutputHandler.wrapContent(request, group, sb, entryHtml);

        return new Result("", sb);
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
    @Override
    public String getWikiTemplate(Request request, Entry entry)
            throws Exception {
        return "+section title={{name}}\n{{description}}\n-section";
    }



}
