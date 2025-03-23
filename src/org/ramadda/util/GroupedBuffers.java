/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;




@SuppressWarnings("unchecked")
public class GroupedBuffers implements Appendable {

    StringBuilder currentSB;
    String currentGroup;
    List<Appendable> sbs = new ArrayList<Appendable>();
    List<String> groups = new ArrayList<String>();    


    public GroupedBuffers() {
	this("");
    }

    public GroupedBuffers(String group) {
	setGroup(group);
    }

    public Appendable 	append(char c) {
	currentSB.append(c);
	return this;
    }

    public Appendable 	append(CharSequence csq) {
	currentSB.append(csq);
	return this;
    }

    public Appendable 	append(CharSequence csq, int start, int end) {
	currentSB.append(csq,start,end);
	return this;
    }    


    public void append(Object o) {
	currentSB.append(o);
    }

    public void setGroup(String group) {
	sbs.add(currentSB=new StringBuilder());
	groups.add(currentGroup=group);
    }

    public List<Appendable> getBuffers() {
	return sbs;
    }

    public List<String> getGroups() {
	return groups;
    }
    

}
