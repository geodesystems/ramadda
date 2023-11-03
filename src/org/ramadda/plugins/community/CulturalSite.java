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
	if(type.equals("archeological"))
	    return "/cultural/archeological.png";
	if(type.equals("place"))
	    return "/cultural/place.png";
	if(type.equals("spring"))
	    return "/cultural/spring.png";
	if(type.equals("food"))
	    return "/cultural/food.png";
	if(type.equals("story"))
	    return "/cultural/story.png";
	if(type.equals("historic"))
	    return "/cultural/historic.png";
	if(type.equals("historical"))
	    return "/cultural/historic.png";	
	if(type.equals("battle"))
	    return "/cultural/battle.png";
	if(type.equals("mining"))
	    return "/cultural/mining.png";
	if(type.equals("rockart"))
	    return "/cultural/rockart.png";
	if(type.equals("sacred"))
	    return "/cultural/sacred.png";
	if(type.equals("travel"))
	    return "/cultural/travel.png";
        return super.getIconUrl(request, entry);
    }

}
