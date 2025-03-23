/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;



/**
 * Class description
 *
 *
 * @param <T>
 *
 * @version        $version$, Wed, Mar 10, '21
 * @author         Enter your name here...
 */
public class NamedValue<T> {

    /** _more_ */
    private String name;

    /** _more_ */
    private T value;

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     */
    public NamedValue(String name, T value) {
        this.name  = name;
        this.value = value;
    }

    /**
     *
     * @param name _more_
     * @param list _more_
     *
     * @return _more_
     */
    public static Object getValue(String name, NamedValue[] list) {
        for (NamedValue v : list) {
            if (v.getName().equals(name)) {
                return v.getValue();
            }
        }

        return null;
    }

    /**
     * _more_
     * t
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
    public T getValue() {
        return value;
    }

}
