/*
* Copyright (c) 2008-2018 Geode Systems LLC
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

    /** _more_ */
    private String id;

    /** _more_ */
    private String subject;

    /** _more_ */
    private String comment;

    /** _more_ */
    private Date date;

    /** _more_ */
    private User user;

    /** _more_ */
    private Entry entry;


    /**
     * _more_
     *
     *
     * @param id _more_
     * @param entry _more_
     * @param user _more_
     * @param date _more_
     * @param subject _more_
     * @param comment _more_
     */
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






    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return subject;
    }


}
