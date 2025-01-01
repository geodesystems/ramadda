/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.wiki;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;

import java.util.Date;


/**
 *
 */

public class WikiPageHistory {

    /** _more_ */
    int version;

    /** _more_ */
    User user;

    /** _more_ */
    Date date;

    /** _more_ */
    String description;

    /** _more_ */
    String text;

    /**
     * _more_
     *
     * @param version _more_
     * @param user _more_
     * @param date _more_
     * @param description _more_
     */
    public WikiPageHistory(int version, User user, Date date,
                           String description) {
        this(version, user, date, description, null);
    }

    /**
     * _more_
     *
     * @param version _more_
     * @param user _more_
     * @param date _more_
     * @param description _more_
     * @param text _more_
     */
    public WikiPageHistory(int version, User user, Date date,
                           String description, String text) {
        this.version     = version;
        this.user        = user;
        this.date        = date;
        this.description = description;
        this.text        = text;
    }

    /**
     *  Set the Version property.
     *
     *  @param value The new value for Version
     */
    public void setVersion(int value) {
        version = value;
    }

    /**
     *  Get the Version property.
     *
     *  @return The Version
     */
    public int getVersion() {
        return version;
    }

    /**
     *  Set the User property.
     *
     *  @param value The new value for User
     */
    public void setUser(User value) {
        user = value;
    }

    /**
     *  Get the User property.
     *
     *  @return The User
     */
    public User getUser() {
        return user;
    }

    /**
     *  Set the Date property.
     *
     *  @param value The new value for Date
     */
    public void setDate(Date value) {
        date = value;
    }

    /**
     *  Get the Date property.
     *
     *  @return The Date
     */
    public Date getDate() {
        return date;
    }

    /**
     *  Set the Description property.
     *
     *  @param value The new value for Description
     */
    public void setDescription(String value) {
        description = value;
    }

    /**
     *  Get the Description property.
     *
     *  @return The Description
     */
    public String getDescription() {
        return description;
    }


    /**
     *  Set the Text property.
     *
     *  @param value The new value for Text
     */
    public void setText(String value) {
        text = value;
    }

    /**
     *  Get the Text property.
     *
     *  @return The Text
     */
    public String getText() {
        return text;
    }



}
