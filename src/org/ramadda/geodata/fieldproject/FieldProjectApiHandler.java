/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.fieldproject;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.HtmlUtils;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import java.io.*;

import java.util.Hashtable;
import java.util.regex.*;


/**
 * Provides a top-level API
 *
 */
public class FieldProjectApiHandler extends SpecialSearch implements RequestHandler {



    /**
     * ctor
     *
     * @param repository the repository
     * @param node xml from api.xml
     * @param props propertiesn
     *
     * @throws Exception on badness
     */
    public FieldProjectApiHandler(Repository repository, Element node,
                                  Hashtable props)
            throws Exception {
        super(repository, node, props);
    }




}
