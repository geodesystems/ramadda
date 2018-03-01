/*
* Copyright (c) 2008-2018 Geode Systems LLC
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
