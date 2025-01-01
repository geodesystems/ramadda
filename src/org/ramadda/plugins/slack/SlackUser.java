/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.slack;


import org.json.*;



import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;

import java.util.Hashtable;



/**
 */
public class SlackUser {

    /** _more_ */
    private static Hashtable<String, TTLCache<String, SlackUser>> cache =
        new Hashtable<String, TTLCache<String, SlackUser>>();

    /** _more_ */
    private String id;

    /** _more_ */
    private String name;

    /** _more_ */
    private String image24;

    /** _more_ */
    private String image48;

    /**
     * _more_
     *
     * @param token _more_
     * @param id _more_
     * @param name _more_
     * @param image24 _more_
     * @param image48 _more_
     */
    public SlackUser(String token, String id, String name, String image24,
                     String image48) {
        getUsers(token).put(id, this);
        this.id      = id;
        this.name    = name;
        this.image24 = image24;
        this.image48 = image48;
    }


    /**
     * _more_
     *
     * @param token _more_
     *
     * @return _more_
     */
    private static TTLCache<String, SlackUser> getUsers(String token) {
        TTLCache<String, SlackUser> users = cache.get(token);
        if (users == null) {
            users = new TTLCache<String, SlackUser>(60 * 60 * 1000);
            cache.put(token, users);
        }

        return users;
    }


    /**
     * _more_
     *
     * @param token _more_
     * @param id _more_
     *
     * @return _more_
     */
    public static SlackUser getUser(String token, String id) {
        return getUsers(token).get(id);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getName() {
        return name;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getImage24() {
        return image24;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getImage48() {
        return image48;
    }


}
