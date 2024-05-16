/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.map;

import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;
import java.util.ArrayList;
import java.util.List;


/**
 * A class to hold some map box properties
 *
 * @author   RAMADDA development team
 */
@SuppressWarnings("unchecked")
public class MapProperties {

    List<String> props = new ArrayList<String>();


    /**
     * Create a MapProperties
     *
     * @param color  the color
     * @param selectable  true if selectable
     */
    public MapProperties(String color, boolean selectable) {
        this(color, selectable, false);
    }

    /**
     * Create a MapProperties
     *
     * @param color the color
     * @param selectable  true if selectable
     * @param zoom  true if should zoom to bounds
     */
    public MapProperties(String color, boolean selectable, boolean zoom) {
	props.add("strokeColor");
	props.add(color);
	props.add("selectable");
	props.add("" + selectable);
	props.add("zoomToExtent");
	props.add("" + zoom);	
    }

    public MapProperties(List<String> props) {
	this.props.addAll(props);
    }

    public MapProperties(String props) {
	this.props=(List<String>)Utils.makeListFromDictionary(Utils.getProperties(props));

    }


    public String toString() {
	return "map props:" +props.toString();
    }

    public String getJson() {
	return JsonUtil.mapAndGuessType(props);
    }



}
