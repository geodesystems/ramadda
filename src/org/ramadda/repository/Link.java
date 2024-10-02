/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository;


import org.ramadda.repository.output.*;
import org.ramadda.util.HtmlUtils;

import ucar.unidata.util.Misc;

import java.io.File;

import java.util.ArrayList;
import java.util.List;



public class Link implements Constants {
    String url;
    String label;
    String icon;
    protected boolean hr = false;
    int type = OutputType.TYPE_ACTION;
    OutputType outputType;
    String category;
    String tooltip;
    
    public Link(boolean hr) {
        this.hr = hr;
    }


    public Link(String url, String icon, String label, int type) {
        this(url, icon, label, null, type);
    }

    public Link(String url, String icon, String label) {
        this(url, icon, label, null);
    }

    public Link(String url, String icon, String label,
                OutputType outputType) {
        this(url, icon, label, outputType, getLinkType(outputType));
    }

    public Link(String url, String icon, String label, OutputType outputType,
                int linkType) {
        this.url        = url;
        this.label      = label;
        this.icon       = icon;
        this.outputType = outputType;
        this.type       = linkType;
    }

    public boolean isType(int typeMask) {
        return (getType() & typeMask) != 0;
    }

    public void setLinkType(int type) {
        this.type = type;
    }

    public static int getLinkType(OutputType outputType) {
        if (outputType == null) {
            return OutputType.TYPE_ACTION;
        }

        return outputType.getType();
    }

    public int getType() {
        return type;
    }

    public OutputType getOutputType() {
        return outputType;
    }

    public boolean getHr() {
        return hr;
    }

    public String toString() {
        if (true) {
            return url + " " + label;
        }
        if (hr) {
            return "<hr>";
        }
        if (icon == null) {
            return HtmlUtils.href(url, label);
        }

        return HtmlUtils.href(url, HtmlUtils.img(icon, label,HtmlUtils.attr("width",ICON_WIDTH)));
    }

    public void setUrl(String value) {
        url = value;
    }


    public String getUrl() {
        return url;
    }

    public void setLabel(String value) {
        label = value;
    }

    public String getLabel() {
        return label;
    }

    public void setIcon(String value) {
        icon = value;
    }


    public String getIcon() {
        return icon;
    }


    public void setTooltip (String value) {
	tooltip = value;
    }


    public String getTooltip () {
	return tooltip;
    }


}
