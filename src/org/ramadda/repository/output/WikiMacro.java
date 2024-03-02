/*
 * Copyright (c) 2008-2023 Geode Systems LLC
 * SPDX-License-Identifier: Apache-2.0
 */

package org.ramadda.repository.output;

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;
import ucar.unidata.xml.XmlUtil;


public class WikiMacro {
    private String name;
    private String label;
    private String wikiText;
    public WikiMacro(Element node) {
	name = XmlUtil.getAttribute(node,"name","name");
	wikiText = XmlUtil.getChildText(node);
	label = XmlUtil.getAttribute(node,"label",name);
    }

    public WikiMacro(String name, String wikiText) {
	this.name  = name;
	this.wikiText = wikiText;
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

    public String toString() {
	return name;
    }



}
