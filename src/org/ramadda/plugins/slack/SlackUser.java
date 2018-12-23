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

package org.ramadda.plugins.slack;


import org.json.*;



import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
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
