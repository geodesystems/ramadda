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

package org.ramadda.repository.search;



import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 * Holds information about a search
 */
public class SearchInfo {

    /** _more_ */
    private StringBuilder msgs = new StringBuilder();

    /**
     * _more_
     */
    public SearchInfo() {}

    /**
     * _more_
     *
     * @param provider _more_
     * @param msg _more_
     *
     * @throws Exception _more_
     */
    public void addMessage(SearchProvider provider, String msg)
            throws Exception {
        if (msg.length() > 0) {
            synchronized (msgs) {
                msgs.append(msg);
                msgs.append("<br>");
            }
        }
    }

}
