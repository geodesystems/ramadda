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

package org.ramadda.repository.auth;


import org.ramadda.repository.*;


import org.ramadda.repository.database.*;


import org.ramadda.util.sql.Clause;


import org.ramadda.util.sql.SqlUtil;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.TTLCache;

import ucar.unidata.util.Cache;
import ucar.unidata.util.DateUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.io.File;


import java.io.UnsupportedEncodingException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


import java.sql.ResultSet;
import java.sql.Statement;


import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;






/**
 * Class UserSession _more_
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class UserSession {

    /** _more_ */
    private String id;

    /** _more_ */
    private User user;

    /** _more_ */
    private Date createDate;

    /** _more_ */
    private Date lastActivity;

    /** _more_ */
    private Hashtable properties = new Hashtable();

    /**
     * _more_
     *
     * @param id _more_
     * @param user _more_
     * @param createDate _more_
     */
    public UserSession(String id, User user, Date createDate) {
        this(id, user, createDate, new Date());
    }

    /**
     * _more_
     *
     * @param id _more_
     * @param user _more_
     * @param createDate _more_
     * @param lastActivity _more_
     */
    public UserSession(String id, User user, Date createDate,
                       Date lastActivity) {
        this.id           = id;
        this.user         = user;
        this.createDate   = createDate;
        this.lastActivity = lastActivity;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return "session:" + user + " id:" + id;
    }

    /**
     * _more_
     *
     * @param key _more_
     * @param value _more_
     */
    public void putProperty(Object key, Object value) {
        properties.put(key, value);
    }

    /**
     * _more_
     *
     * @param key _more_
     */
    public void removeProperty(Object key) {
        properties.remove(key);
    }

    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public Object getProperty(Object key) {
        return properties.get(key);
    }

    /**
     *  Set the Id property.
     *
     *  @param value The new value for Id
     */
    public void setId(String value) {
        id = value;
    }

    /**
     *  Get the Id property.
     *
     *  @return The Id
     */
    public String getId() {
        return id;
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
     * _more_
     *
     * @return _more_
     */
    public String getUserId() {
        if (user == null) {
            return "";
        }

        return user.getId();
    }

    /**
     *  Set the CreateDate property.
     *
     *  @param value The new value for CreateDate
     */
    public void setCreateDate(Date value) {
        createDate = value;
    }

    /**
     *  Get the CreateDate property.
     *
     *  @return The CreateDate
     */
    public Date getCreateDate() {
        return createDate;
    }

    /**
     *  Set the LastActivity property.
     *
     *  @param value The new value for LastActivity
     */
    public void setLastActivity(Date value) {
        lastActivity = value;
    }

    /**
     *  Get the LastActivity property.
     *
     *  @return The LastActivity
     */
    public Date getLastActivity() {
        return lastActivity;
    }



}
