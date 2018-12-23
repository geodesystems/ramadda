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
import java.util.Hashtable;
import java.util.List;



/**
 * A collection of utilities for rss feeds xml.
 *
 * @author Jeff McWhirter
 */

public class CategoryBuffer {

    /** _more_ */
    List<String> categories = new ArrayList<String>();

    /** _more_ */
    Hashtable<String, StringBuilder> buffers = new Hashtable<String,
                                                   StringBuilder>();

    /**
     * _more_
     */
    public CategoryBuffer() {}

    /**
     * _more_
     *
     * @param category _more_
     *
     * @return _more_
     */
    public StringBuilder get(String category) {
        return get(category, false);
    }


    /**
     * _more_
     *
     * @param category _more_
     * @param addToFront _more_
     *
     * @return _more_
     */
    public StringBuilder get(String category, boolean addToFront) {
        if (category == null) {
            category = "";
        }
        StringBuilder sb = buffers.get(category);
        if (sb == null) {
            sb = new StringBuilder();
            buffers.put(category, sb);
            if (addToFront) {
                categories.add(0, category);
            } else {
                categories.add(category);
            }
        }

        return sb;
    }

    /**
     * _more_
     *
     * @param category _more_
     */
    public void moveToFront(String category) {
        categories.remove(category);
        categories.add(0, category);
    }


    /**
     * _more_
     *
     * @param category _more_
     * @param object _more_
     */
    public void append(String category, Object object) {
        get(category).append(object);
    }

    /**
     * _more_
     *
     * @param category _more_
     *
     * @return _more_
     */
    public boolean contains(String category) {
        return buffers.get(category) != null;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<String> getCategories() {
        return categories;
    }


}
