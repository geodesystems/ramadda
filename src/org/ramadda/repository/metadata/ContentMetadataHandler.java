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

package org.ramadda.repository.metadata;


import org.ramadda.repository.*;


import org.ramadda.util.HtmlTemplate;
import org.ramadda.util.HtmlUtils;


import org.w3c.dom.*;


import ucar.unidata.util.Misc;


import java.awt.Image;


import java.io.*;

import java.net.URL;
import java.net.URLConnection;



import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class ContentMetadataHandler extends MetadataHandler {


    /** _more_ */
    public static final String TYPE_THUMBNAIL = "content.thumbnail";

    /** _more_ */
    public static final String TYPE_ICON = "content.icon";

    /** _more_ */
    public static final String TYPE_ATTACHMENT = "content.attachment";

    /** _more_ */
    public static final String TYPE_PAGESTYLE = "content.pagestyle";

    /** _more_ */
    public static final String TYPE_KEYWORD = "content.keyword";

    /** _more_ */
    public static final String TYPE_URL = "content.url";

    /** _more_ */
    public static final String TYPE_EMAIL = "content.email";

    /** _more_ */
    public static final String TYPE_AUTHOR = "content.author";

    /** _more_ */
    public static final String TYPE_LOGO = "content.logo";

    /** _more_ */
    public static final String TYPE_JYTHON = "content.jython";

    /** _more_ */
    public static final String TYPE_CONTACT = "content.contact";

    /** _more_ */
    public static final String TYPE_SORT = "content.sort";

    /** _more_ */
    public static final String TYPE_TIMEZONE = "content.timezone";

    /** _more_ */
    public static final String TYPE_ALIAS = "content.alias";

    /** _more_ */
    public static final String TYPE_TEMPLATE = "content.pagetemplate";

    /**
     * _more_
     *
     * @param repository _more_
     *
     * @throws Exception _more_
     */
    public ContentMetadataHandler(Repository repository) throws Exception {
        super(repository);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String[] getHtml(Request request, Entry entry, Metadata metadata)
            throws Exception {
        if (metadata.getType().equals(TYPE_ALIAS)) {
            Hashtable props =
                (Hashtable) request.getExtraProperty("wiki.props");
            String title = "Alias";
            String a     = metadata.getAttr1();
            String label = a;
            if (props != null) {
                title = Misc.getProperty(props, "title", title);
                label = Misc.getProperty(props, "label", label);
            }

            if (a.startsWith("http:")) {
                return new String[] { title, HtmlUtils.href(a, label) };
            }
        }

        return super.getHtml(request, entry, metadata);
    }

    /**
     * _more_
     *
     * @param element _more_
     *
     * @return _more_
     */
    public String getEnumerationValues(MetadataElement element) {
        if (element.getName().equals("template")) {
            StringBuffer sb = new StringBuffer();
            for (HtmlTemplate htmlTemplate :
                    getRepository().getPageHandler().getTemplates()) {
                sb.append(htmlTemplate.getId());
                sb.append(":");
                sb.append(htmlTemplate.getName());
                sb.append(",");
            }

            return sb.toString();
        }

        return "";
    }

}
