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
public class CulturalSite extends ExtensibleGroupTypeHandler {


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
    public CulturalSite(Repository repository, Element entryNode)
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
        String type = entry.getStringValue(0, "");
	if(type.equals("archaeological"))
	    return "/community/cultural/archaeological.png";
	if(type.equals("spring"))
	    return "/community/cultural/spring.png";
	if(type.equals("food"))
	    return "/community/cultural/food.png";
	if(type.equals("story"))
	    return "/community/cultural/story.png";
	if(type.equals("historical"))
	    return "/community/cultural/historical.png";
	if(type.equals("battle"))
	    return "/community/cultural/battle.png";
	if(type.equals("mining"))
	    return "/community/cultural/mining.png";
	if(type.equals("rockart"))
	    return "/community/cultural/rockart.png";
	if(type.equals("sacred"))
	    return "/community/cultural/sacred.png";
	if(type.equals("travel"))
	    return "/community/cultural/travel.png";
	System.err.println("NA:" + type);
			   
        return super.getIconUrl(request, entry);
    }

}
