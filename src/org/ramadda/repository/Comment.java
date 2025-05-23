/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository;

import org.ramadda.repository.auth.*;

import ucar.unidata.util.Misc;

import java.util.Date;

/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.30 $
 */
public class Comment {

    private String id;

    private String subject;

    private String comment;

    private Date date;

    private User user;

    private Entry entry;

    public Comment(String id, Entry entry, User user, Date date,
                   String subject, String comment) {
        this.id      = id;
        this.entry   = entry;
        this.user    = user;
        this.subject = subject;
        this.comment = comment;
        this.date    = date;
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
     * Set the Subject property.
     *
     * @param value The new value for Subject
     */
    public void setSubject(String value) {
        subject = value;
    }

    /**
     * Get the Subject property.
     *
     * @return The Subject
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Set the Comment property.
     *
     * @param value The new value for Comment
     */
    public void setComment(String value) {
        comment = value;
    }

    /**
     * Get the Comment property.
     *
     * @return The Comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * Set the User property.
     *
     * @param value The new value for User
     */
    public void setUser(User value) {
        user = value;
    }

    /**
     * Get the User property.
     *
     * @return The User
     */
    public User getUser() {
        return user;
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

    public String toString() {
        return subject;
    }

}
