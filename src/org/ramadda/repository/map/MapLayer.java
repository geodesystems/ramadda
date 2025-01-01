/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.map;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.metadata.MetadataHandler;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import java.awt.geom.Rectangle2D;

import java.util.ArrayList;
import java.util.List;



/**
 * A MapInfo class to hold map info
 */
public class MapLayer {

    /** _more_ */
    private String url;

    /** _more_ */
    private String id;

    /** _more_ */
    private String label;

    /** _more_ */
    private String labelArg;

    /** _more_ */
    private String legendImage;

    /** _more_ */
    private String legendText = "";

    /** _more_ */
    private String legendLabel;

    /** _more_ */
    private boolean isDefault = false;

    /**
     * _more_
     *
     * @param id _more_
     * @param url _more_
     * @param label _more_
     * @param legendImage _more_
     * @param legendLabel _more_
     */
    public MapLayer(String id, String url, String label, String legendImage,
                    String legendLabel) {
        this.url         = url;
        this.id          = id;
        this.label       = label;
        this.legendImage = legendImage;
        this.legendLabel = legendLabel;
        this.labelArg    = this.label.replaceAll("'", "\\\\'");
    }

    /**
     * _more_
     *
     * @param repository _more_
     * @param prefix _more_
     * @param id _more_
     */
    public MapLayer(Repository repository, String prefix, String id) {
        this.id  = id;
        prefix   = prefix + "." + id;
        this.url = repository.getProperty(prefix + ".url", "");
        if (this.url.length() == 0) {
            throw new IllegalArgumentException(
                "no url defined for map layer:" + prefix + ".url");
        }
        this.label = repository.getProperty(prefix + ".label", "NONE");
        this.legendImage = repository.getProperty(prefix + ".legend.image",
                "");
        this.legendText = repository.getProperty(prefix + ".legend.text", "");
        this.legendLabel = repository.getProperty(prefix + ".legend.label",
                "");
        this.labelArg  = this.label.replaceAll("'", "\\\\'");
        this.isDefault = repository.getProperty(prefix + ".default", false);
    }



    /**
     * _more_
     *
     * @param repository _more_
     * @param prefix _more_
     *
     * @return _more_
     */
    public static List<MapLayer> makeLayers(Repository repository,
                                            String prefix) {
        List<MapLayer> layers = new ArrayList<MapLayer>();
        for (String id :
                Utils.split(repository.getProperty(prefix + ".maps", ""),
                            ",", true, true)) {
            try {
                layers.add(new MapLayer(repository, prefix, id));
            } catch (Exception exc) {
                repository.getLogManager().logError("Adding map layer:"
                        + exc, exc);
            }
        }

        return layers;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param mapInfo _more_
     */
    public void addToMap(Request request, MapInfo mapInfo) {
        mapInfo.addJS(
            HtmlUtils.call(
                mapInfo.getVariableName() + ".addMapLayer",
                HtmlUtils.jsMakeArgs(
                    false, HtmlUtils.squote(labelArg), HtmlUtils.squote(url),
                    HtmlUtils.squote(id), "true", "" + isDefault)));
        mapInfo.addJS("\n");
    }


    /**
     *  Set the Url property.
     *
     *  @param value The new value for Url
     */
    public void setUrl(String value) {
        url = value;
    }

    /**
     *  Get the Url property.
     *
     *  @return The Url
     */
    public String getUrl() {
        return url;
    }

    /**
     *  Set the Id property.
     *
     *  @param value The new value for Id
     */
    public void setId(String value) {
        id = value;
    }

    /**
     *  Get the Id property.
     *
     *  @return The Id
     */
    public String getId() {
        return id;
    }

    /**
     *  Set the Label property.
     *
     *  @param value The new value for Label
     */
    public void setLabel(String value) {
        label = value;
    }

    /**
     *  Get the Label property.
     *
     *  @return The Label
     */
    public String getLabel() {
        return label;
    }

    /**
     *  Set the LegendImage property.
     *
     *  @param value The new value for LegendImage
     */
    public void setLegendImage(String value) {
        legendImage = value;
    }

    /**
     *  Get the LegendImage property.
     *
     *  @return The LegendImage
     */
    public String getLegendImage() {
        return legendImage;
    }

    /**
     *  Set the LegendLabel property.
     *
     *  @param value The new value for LegendLabel
     */
    public void setLegendLabel(String value) {
        legendLabel = value;
    }

    /**
     *  Get the LegendLabel property.
     *
     *  @return The LegendLabel
     */
    public String getLegendLabel() {
        return legendLabel;
    }




    /**
     *  Set the LegendText property.
     *
     *  @param value The new value for LegendText
     */
    public void setLegendText(String value) {
        legendText = value;
    }

    /**
     *  Get the LegendText property.
     *
     *  @return The LegendText
     */
    public String getLegendText() {
        return legendText;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getIsDefault() {
        return isDefault;
    }
}
