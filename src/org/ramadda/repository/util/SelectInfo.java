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

package org.ramadda.repository.util;


import org.ramadda.sql.Clause;
import org.ramadda.sql.SqlUtil;




import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 * Holds information for generating entries in entries.xml
 */
public class SelectInfo {

    /** _more_ */
    private List<Clause> where;

    /** _more_ */
    private int max = -1;

    /**
     * _more_
     */
    public SelectInfo() {}


    /**
     * _more_
     *
     * @param where _more_
     */
    public SelectInfo(List<Clause> where) {
        this(where, -1);
    }

    /**
     * _more_
     *
     * @param where _more_
     * @param max _more_
     */
    public SelectInfo(List<Clause> where, int max) {
        this.where = where;
        this.max   = max;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<Clause> getWhere() {
        return where;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getMaxCount() {
        return max;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return " max:" + max;
    }
}
