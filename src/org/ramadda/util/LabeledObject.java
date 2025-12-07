/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

/**
 * Provides a wrapper around an object and keeps track of a date.
 *
 * @param <ObjectType>
 */
@SuppressWarnings("unchecked")
public class LabeledObject<ObjectType> implements Comparable<LabeledObject> {
    private String label;
    private ObjectType object;

    public LabeledObject(String label, ObjectType object) {
        this.label   = label;
        this.object = object;
    }

    public int 	compareTo(LabeledObject o) {
	return label.compareTo(o.label);
    }

    public static List<String> getLabels(List<LabeledObject> objects) {
	List<String> labels = new ArrayList<String>();
	for(LabeledObject obj: objects) labels.add(obj.label);
	return labels;
    }
    public static List<Object> getObjects(List<LabeledObject> objects) {
	List<Object> labels = new ArrayList<Object>();
	for(LabeledObject obj: objects) labels.add(obj.object);
	return labels;
    }

    public static List<LabeledObject> makeList(List<String> labels,
					       List objects) {

	List<LabeledObject> list = new ArrayList<LabeledObject>();
	for(int i=0;i<labels.size();i++) {
	    list.add(new LabeledObject(labels.get(i), objects.get(i)));
	}
	return list;
    }

    public void setLabel(String value) {
        label = value;
    }

    public String getLabel() {
        return label;
    }

    public void setObject(ObjectType value) {
        object = value;
    }


    public ObjectType getObject() {
        return object;
    }

}
