/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.db;


import org.ramadda.util.NamedBuffer;


/**
 * Class description
 *
 *
 * @version        $version$, Tue, Nov 2, '21
 * @author         Enter your name here...
 */
public class DbNamedBuffer extends NamedBuffer {

    /** _more_ */
    String anchor;

    /**
     * _more_
     *
     * @param name _more_
     */
    DbNamedBuffer(String name) {
        super(name);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param b _more_
     */
    DbNamedBuffer(String name, String b) {
        super(name, b);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param b _more_
     * @param anchor _more_
     */
    DbNamedBuffer(String name, String b, String anchor) {
        super(name, b);
        this.anchor = anchor;
    }
}
