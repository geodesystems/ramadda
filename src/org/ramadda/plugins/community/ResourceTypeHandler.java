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
        String type = entry.getValue(0, "");
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
