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

package org.ramadda.geodata.ogc;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.WmsUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.util.ArrayList;

import java.util.List;


/**
 * A place holder class that provides services for WMS URL entry types.
 * Right now this does nothing but we could use it to provide a new defalt html display
 */
public class WmsLayerTypeHandler extends GenericTypeHandler {


    /**
     * ctor
     *
     * @param repository the repository
     * @param node the types.xml node
     * @throws Exception On badness
     */
    public WmsLayerTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }

}
