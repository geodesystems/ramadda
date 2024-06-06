/*
 * Copyright (c) 2008-2023 Geode Systems LLC
 * SPDX-License-Identifier: Apache-2.0
 */

package org.ramadda.repository.output;
import  org.ramadda.util.Utils;

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;
import ucar.unidata.xml.XmlUtil;


public class WikiMacro {
    private String name;
    private String label;
    private String icon;
    private String wikiText;
    private String properties;
    private List<String> tags;
    private boolean isOutput=false;
    public WikiMacro(Element node) {
	name = XmlUtil.getAttribute(node,"name","name");
	tags  =Utils.split(XmlUtil.getAttribute(node,"tags",""),",",true,true);
	icon = XmlUtil.getAttribute(node,"icon",(String)null);
	label = XmlUtil.getAttribute(node,"label",name);
	isOutput = XmlUtil.getAttribute(node,"isoutput",false);
	properties = XmlUtil.getAttribute(node,"properties","");
	wikiText = XmlUtil.getChildText(node);
	if(wikiText!=null) wikiText = wikiText.trim();
    }

    public WikiMacro(String name, String wikiText) {
	this.name  = name;
	if(wikiText!=null) wikiText = wikiText.trim();
	this.wikiText = wikiText;
    }

    public boolean hasTag(String tag) {
	return tags.contains(tag);
    }

    public boolean isOutput() {
	return isOutput;
    }

    /**
       Set the Name property.

       @param value The new value for Name
    **/
    public void setName (String value) {
	name = value;
    }

    /**
       Get the Name property.

       @return The Name
    **/
    public String getName () {
	return name;
    }

    public String getIcon () {
	return icon;
    }

    /**
       Set the WikiText property.

       @param value The new value for WikiText
    **/
    public void setWikiText (String value) {
	wikiText = value;
    }

    public String getLabel() {
	return label;
    }

    /**
       Get the WikiText property.

       @return The WikiText
    **/
    public String getWikiText () {
	return wikiText;
    }

    public String getProperties() {
	return properties;
    }

    public String toString() {
	return name;
    }



}
