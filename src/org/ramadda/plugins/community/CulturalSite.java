/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.community;


import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;

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


    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {
        super.initializeNewEntry(request, entry, newType);
	if(stringDefined(entry.getName())) return;
        String url = entry.getResource().getPath();
	if(!url.startsWith("http")) return;
	try {
	    String html  = IOUtil.readContents(url, "");
	    String title = StringUtil.findPattern(html, "<title>(.*)</title>");
	    if (title == null) {
		title = StringUtil.findPattern(html, "<TITLE>(.*)</TITLE>");
	    }
	    if (title != null) {
		//In case it is YT
		title = title.replace("- YouTube", "");
		entry.setName(title);
	    }
	} catch(Exception exc) {
	    System.err.println("Error reading URL:" + url+" " +exc);
	}
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
	switch (type) {
	case "archeological":
	    return "/cultural/archeological.png";
	case "place":
	    return "/cultural/place.png";
	case "spring":
	    return "/cultural/spring.png";
	case "food":
	    return "/cultural/food.png";
	case "story":
	    return "/cultural/story.png";
	case "historic":
	    return "/cultural/historic.png";
	case "historical":
	    return "/cultural/historic.png";	
	case "battle":
	    return "/cultural/battle.png";
	case "mining":
	    return "/cultural/mining.png";
	case "rockart":
	    return "/cultural/rockart.png";
	case "sacred":
	    return "/cultural/sacred.png";
	case "travel":
	    return "/cultural/travel.png";
	default:
	    return "/cultural/archeological.png";
	}
    }

}
