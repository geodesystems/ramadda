/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 *
 * @param <T>
 */
public class BufferMapList<T> extends MapList<T, Appendable> {


    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public Appendable get(T key) {
        Appendable sb = super.get(key);
        if (sb == null) {
            sb = new StringBuilder();
            initNewBuffer(sb);
            super.put(key, sb);
        }

        return sb;
    }

    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public Appendable peek(T key) {
        return super.get(key);
    }


    /**
     * _more_
     *
     * @param sb _more_
     */
    public void initNewBuffer(Appendable sb) {}

}
