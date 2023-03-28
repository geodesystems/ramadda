/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.map;



/**
 * A class to hold some map box properties
 *
 * @author   RAMADDA development team
 */
public class MapBoxProperties {

    /** color property */
    private String color;

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
