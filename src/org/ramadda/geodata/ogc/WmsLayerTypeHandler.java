/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
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
