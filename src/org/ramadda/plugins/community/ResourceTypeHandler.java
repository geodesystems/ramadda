/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.community;


import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;

import org.w3c.dom.*;


/**
 *
 *
 */
public class ResourceTypeHandler extends ExtensibleGroupTypeHandler {


    /** _more_ */
    public static String TYPE_RESOURCE = "community_resource";

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public ResourceTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
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
    public String getEntryIconUrl(Request request, Entry entry)
            throws Exception {
        String type = entry.getStringValue(request,0, "");
        if (type.equals("civic")) {
            return getIconUrl("/community/group.png");
        }
        if (type.equals("service_provider")) {
            return getIconUrl("/community/lifebuoy.png");
        }

        if (type.equals("church")) {
            return getIconUrl("/community/building.png");
        }

        if (type.equals("educational_facility")) {
            return getIconUrl("/community/building.png");
        }

        if (type.equals("daycare")) {
            return getIconUrl("/community/home.png");
        }

        if (type.equals("biking")) {
            return getIconUrl("/icons/bike.png");
        }
        if (type.equals("camping")) {
            return getIconUrl("/community/tent.png");
        }

        if (type.equals("marijuana_facility")) {
            return getIconUrl("/community/cookies.png");
        }


        return super.getIconUrl(request, entry);
    }


}
