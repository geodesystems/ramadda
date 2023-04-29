/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import java.util.ArrayList;
import java.util.List;




/**
 * Class description
 *
 * @version        $version$, Wed, Mar 10, '21
 * @author         Enter your name here...
 */
public class NamedBuffer {

    /** _more_ */
    private String name;

    /** _more_ */
    private StringBuilder buffer = new StringBuilder();

    /**
     * _more_
     *
     * @param name _more_
     */
    public NamedBuffer(String name) {
        this.name = name;
    }

    /**
     *
     *
     * @param name _more_
     * @param b _more_
     */
    public NamedBuffer(String name, String b) {
        this(name);
        buffer.append(b);
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
     *
     * @return _more_
     */
    public StringBuilder getBuffer() {
        return buffer;
    }


    /**
     *
     * @param o _more_
     */
    public void append(Object o) {
        buffer.append(o);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return name + " " + buffer;
    }
}
