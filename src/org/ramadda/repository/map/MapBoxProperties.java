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

package org.ramadda.repository.map;



/**
 * A class to hold some map box properties
 *
 * @author   RAMADDA development team
 */
public class MapBoxProperties {

    /** color property */
    private String color = MapInfo.DFLT_BOX_COLOR;

    /** selectable property */
    private boolean selectable = false;

    /** zoom to extent property */
    private boolean zoomToExtent = false;

    /**
     * Create a MapBoxProperties
     *
     * @param color  the color
     * @param selectable  true if selectable
     */
    public MapBoxProperties(String color, boolean selectable) {
        this(color, selectable, false);
    }

    /**
     * Create a MapBoxProperties
     *
     * @param color the color
     * @param selectable  true if selectable
     * @param zoom  true if should zoom to bounds
     */
    public MapBoxProperties(String color, boolean selectable, boolean zoom) {
        this.color        = color;
        this.selectable   = selectable;
        this.zoomToExtent = zoom;
    }

    /**
     *  Set the Color property.
     *
     *  @param value The new value for Color
     */
    public void setColor(String value) {
        color = value;
    }

    /**
     *  Get the Color property.
     *
     *  @return The Color
     */
    public String getColor() {
        return color;
    }

    /**
     *  Set the Selectable property.
     *
     *  @param value The new value for Selectable
     */
    public void setSelectable(boolean value) {
        selectable = value;
    }

    /**
     *  Get the Selectable property.
     *
     *  @return The Selectable
     */
    public boolean getSelectable() {
        return selectable;
    }

    /**
     *  Set the ZoomToExtent property.
     *
     *  @param value The new value for ZoomToExtent
     */
    public void setZoomToExtent(boolean value) {
        zoomToExtent = value;
    }

    /**
     *  Get the ZoomToExtent property.
     *
     *  @return The ZoomToExtent
     */
    public boolean getZoomToExtent() {
        return zoomToExtent;
    }



}
