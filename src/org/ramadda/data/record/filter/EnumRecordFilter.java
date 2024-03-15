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

package org.ramadda.data.record.filter;


import org.ramadda.data.record.*;

import java.io.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


/**
 * Class description
 *
 *
 * @version        Enter version here..., Fri, May 21, '10
 * @author         Enter your name here...
 */
public class EnumRecordFilter implements RecordFilter {



    /** _more_ */
    private int attrId;

    private  HashSet enums;

    /**
     * _more_
     *
     * @param operator _more_
     * @param attrId _more_
     */
    public EnumRecordFilter(int attrId,HashSet enums) {
	this.enums = enums;
        this.attrId   = attrId;
    }

    /**
     * _more_
     *
     * @param record _more_
     * @param visitInfo _more_
     *
     * @return _more_
     */
    public boolean isRecordOk(BaseRecord record, VisitInfo visitInfo) {
        Object object = record.getObjectValue(attrId);
	if(object==null)
	    object = ""+record.getValue(attrId);
	return enums.contains(object.toString());
    }



}
