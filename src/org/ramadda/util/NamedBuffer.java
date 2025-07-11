/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NamedBuffer implements Appendable {

    private String name;


    private StringBuilder buffer = new StringBuilder();

    public NamedBuffer(String name) {
	if(name==null) name="";
        this.name = name;
    }

    public NamedBuffer(String name, String b) {
        this(name);
        buffer.append(b);
    }

    public static NamedBuffer append(List<NamedBuffer> list,
				  String label, String contents) throws Exception {
	NamedBuffer buffer =null;
	if(list.size()==0) {
	    list.add(buffer=new NamedBuffer(label));
	} else {
	    buffer = list.get(list.size()-1);
	}
	if(label!=null && label.length()>0 && !buffer.name.equals(label)) {
	    list.add(buffer=new NamedBuffer(label));
	}


	if(contents!=null)
	    buffer.append(contents);
	return buffer;
    }


    public void setName(String n) {
	this.name = n;
    }

    public String getName() {
        return name;
    }

    public StringBuilder getBuffer() {
        return buffer;
    }

    public void setBuffer(StringBuilder sb) {
	buffer=sb;
    }


    public void append(Object o) {
        buffer.append(o);
    }



    public Appendable 	append(char c) throws IOException {
	buffer.append(c);
	return this;
    }

    public Appendable 	append(CharSequence csq) throws IOException {
	buffer.append(csq);
	return this;
    }

    public Appendable 	append(CharSequence csq, int start, int end) throws IOException {
	buffer.append(csq,start,end);
	return this;
    }

    public String toString() {
        return name + " " + buffer;
    }
}
