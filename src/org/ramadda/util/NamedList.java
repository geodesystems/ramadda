/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import java.util.ArrayList;
import java.util.List;




/**
 * Class description
 *
 *
 * @param <T>
 *
 * @version        $version$, Wed, Mar 10, '21
 * @author         Enter your name here...
 */
public class NamedList<T> {

    /** _more_ */
    private String name;

    /** _more_ */
    List<T> list = new ArrayList<T>();

    /**
     * _more_
     *
     * @param name _more_
     */
    public NamedList(String name) {
        this.name = name;
    }

    /**
     *
     *
     * @param name _more_
     * @param list _more_
     */
    public NamedList(String name, List<T> list) {
        this.name = name;
        this.list = list;
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
    public List<T> getList() {
        return list;
    }


    /**
     * _more_
     *
     * @param item _more_
     */
    public void add(T item) {
        list.add(item);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return name + " #:" + list.size();
    }
}
