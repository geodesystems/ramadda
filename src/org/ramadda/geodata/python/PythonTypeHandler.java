/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.python;



import org.ramadda.repository.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.output.WikiConstants;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;


import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PatternFileFilter;


import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.awt.geom.Rectangle2D;

import java.io.File;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 *
 */

public class PythonTypeHandler extends TypeHandler {


    /**
     * Construct a new GridAggregationTypeHandler
     *
     * @param repository   the Repository
     * @param node         the defining Element
     * @throws Exception   problems
     */
    public PythonTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }






}
