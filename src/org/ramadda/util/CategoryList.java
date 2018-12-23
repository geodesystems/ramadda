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
 *
 * @param <T>
 */

public class CategoryList<T> {

    /** _more_ */
    List<String> categories = new ArrayList<String>();

    /** _more_ */
    Hashtable<String, List<T>> map = new Hashtable<String, List<T>>();

    /**
     * _more_
     */
    public CategoryList() {}

    /**
     * _more_
     *
     * @param category _more_
     *
     * @return _more_
     */
    public List<T> get(String category) {
        if (category == null) {
            category = "";
        }
        List<T> sb = map.get(category);
        if (sb == null) {
            sb = new ArrayList<T>();
            map.put(category, sb);
            categories.add(category);
        }

        return sb;
    }

    /**
     * _more_
     *
     * @param category _more_
     * @param object _more_
     */
    public void add(String category, T object) {
        get(category).add(object);
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
