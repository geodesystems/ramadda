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

package org.ramadda.data.record;


import java.io.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 * Holds information about the record's parameters
 *
 *
 * @author Jeff McWhirter
 */
public interface ValueGetter {

    /**
     * _more_
     *
     * @param record _more_
     * @param field _more_
     * @param visitInfo _more_
     *
     * @return _more_
     */
    public double getValue(BaseRecord record, RecordField field,
                           VisitInfo visitInfo);

    /**
     * _more_
     *
     * @param record _more_
     * @param field _more_
     * @param visitInfo _more_
     *
     * @return _more_
     */
    public String getStringValue(BaseRecord record, RecordField field,
                                 VisitInfo visitInfo);
}
