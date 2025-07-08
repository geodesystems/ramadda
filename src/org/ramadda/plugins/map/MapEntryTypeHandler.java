/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.map;

import org.ramadda.repository.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;

import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.awt.geom.Rectangle2D;

import java.io.File;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

/**
 */
public class MapEntryTypeHandler extends GenericTypeHandler {

    public MapEntryTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }

}
