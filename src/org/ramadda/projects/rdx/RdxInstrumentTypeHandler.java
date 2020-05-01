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

package org.ramadda.projects.rdx;


import org.ramadda.repository.*;
import org.ramadda.repository.type.*;


import org.ramadda.util.HtmlUtils;


import org.w3c.dom.*;

import ucar.unidata.util.StringUtil;

import java.util.Date;
import java.util.List;


/**
 *
 *
 */
public class RdxInstrumentTypeHandler extends ExtensibleGroupTypeHandler {

    private static int IDX = 0;
    public static final int IDX_INSTRUMENT_id = IDX++;
    public static final int IDX_TYPE = IDX++;
    public static final int IDX_LOCALE_CITY = IDX++;
    public static final int IDX_LOCALE_STATE = IDX++;
    public static final int IDX_NETWORK_UP = IDX++;
    public static final int IDX_DATA_DOWN = IDX++;
    public static final int IDX_LAST_NETWORK_CONNECTION = IDX++;
    public static final int IDX_LAST_DATA = IDX++;



    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public RdxInstrumentTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getEntryIconUrl(Request request, Entry entry)
            throws Exception {
	return super.getEntryIconUrl(request, entry);
	//        return getIconUrl(icon);
    }


    public String decorateValue(Request request, Entry entry, Column column, String s) {
	if(!column.getName().equals("data_down")) return super.decorateValue(request,entry, column, s);
	Date d  = (Date)entry.getValue(IDX_LAST_DATA);
	if(d==null) return super.decorateValue(request,entry, column, s);
	return s;
    }



}
