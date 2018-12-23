/*
* Copyright (c) 2008-2019 Geode Systems LLC
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

    /** _more_ */
    private Date date;

    /** _more_ */
    private ObjectType object;


    /**
     * ctor
     *
     * @param date _more_
     * @param object _more_
     */
    public DatedObject(Date date, ObjectType object) {
        this.date   = date;
        this.object = object;
    }

    /**
     * Set the Date property.
     *
     * @param value The new value for Date
     */
    public void setDate(Date value) {
        date = value;
    }

    /**
     * Get the Date property.
     *
     * @return The Date
     */
    public Date getDate() {
        return date;
    }

    /**
     * Set the Object property.
     *
     * @param value The new value for Object
     */
    public void setObject(ObjectType value) {
        object = value;
    }

    /**
     * Get the Object property.
     *
     * @return The Object
     */
    public ObjectType getObject() {
        return object;
    }



}
