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
public class DatedObject<ObjectType> {

    private Date date;

    private ObjectType object;

    public DatedObject(Date date, ObjectType object) {
        this.date   = date;
        this.object = object;
    }

    public void setDate(Date value) {
        date = value;
    }

    public Date getDate() {
        return date;
    }

    public void setObject(ObjectType value) {
        object = value;
    }

    public ObjectType getObject() {
        return object;
    }

}
