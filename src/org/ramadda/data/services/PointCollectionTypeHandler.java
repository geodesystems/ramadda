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

package org.ramadda.data.services;


import org.ramadda.data.record.RecordField;
import org.ramadda.data.services.*;


import org.ramadda.repository.*;
import org.ramadda.repository.map.MapInfo;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.AtomUtil;

import org.ramadda.util.HtmlUtils;


import org.w3c.dom.*;

import ucar.unidata.xml.XmlUtil;


import java.util.Date;
import java.util.HashSet;
import java.util.List;


/**
 *
 *
 * @author Jeff McWhirter
 * @version $Revision: 1.3 $
 */
public class PointCollectionTypeHandler extends RecordCollectionTypeHandler {


    /**
     * ctor
     *
     * @param repository the repository
     * @param node the xml node from the types.xml file
     * @throws Exception On badness
     */
    public PointCollectionTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public RecordOutputHandler getRecordOutputHandler() {
        return (RecordOutputHandler) getRepository().getOutputHandler(
            org.ramadda.data.services.PointOutputHandler.class);
    }


}
