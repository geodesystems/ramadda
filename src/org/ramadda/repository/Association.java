/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository;

import org.ramadda.repository.database.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.Misc;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

/**
 * Class Entry _more_
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class Association {

    public static final String TYPE_ATTACHMENT = "attachment";

    private String id;

    private String name;

    private String type;

    private String fromId;

    private String toId;

    public Association(String id, String name, String type, String fromId,
                       String toId) {
        this.id   = id;
        this.name = name;
        if (type == null) {
            type = "";
        }
        this.type   = type;
        this.fromId = fromId;
        this.toId   = toId;
    }

    /**
     * Set the Id property.
     *
     * @param value The new value for Id
     */
    public void setId(String value) {
        id = value;
    }

    /**
     * Get the Id property.
     *
     * @return The Id
     */
    public String getId() {
        return id;
    }

    /**
     * Set the Name property.
     *
     * @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * Get the Name property.
     *
     * @return The Name
     */
    public String getName() {
        return name;
    }

    public String getLabel() {
        if ((name == null) || (name.length() == 0)) {
            return type;
        }

        return name;
    }

    public boolean equals(Object o) {
        if ( !(o instanceof Association)) {
            return false;
        }
        Association that = (Association) o;

        return Misc.equals(this.name, that.name)
               && Misc.equals(this.type, that.type)
               && Misc.equals(this.fromId, that.fromId)
               && Misc.equals(this.toId, that.toId);

    }

    public String toString() {
        return "name:" + name + " type:" + type + " fromId:" + fromId;
        //        return "name:" + name +" type:" + type + " fromId:" + fromId +" toId:" + toId;
    }

    /**
     * Set the FromId property.
     *
     * @param value The new value for FromId
     */
    public void setFromId(String value) {
        fromId = value;
    }

    /**
     * Get the FromId property.
     *
     * @return The FromId
     */
    public String getFromId() {
        return fromId;
    }

    /**
     * Set the ToId property.
     *
     * @param value The new value for ToId
     */
    public void setToId(String value) {
        toId = value;
    }

    /**
     * Get the ToId property.
     *
     * @return The ToId
     */
    public String getToId() {
        return toId;
    }

    /**
     * Set the Type property.
     *
     * @param value The new value for Type
     */
    public void setType(String value) {
        type = value;
    }

    /**
     * Get the Type property.
     *
     * @return The Type
     */
    public String getType() {
        return type;
    }

}
