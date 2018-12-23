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

package org.ramadda.geodata.astro;


import nom.tam.fits.*;


import org.ramadda.repository.*;
import org.ramadda.repository.map.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.AtomUtil;
import org.ramadda.util.HtmlUtils;


import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.awt.geom.Rectangle2D;

import java.io.File;

import java.text.SimpleDateFormat;


import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

import java.util.List;


/**
 *
 *
 * @author Jeff McWhirter
 * @version $Revision: 1.3 $
 */
public class EclipseTypeHandler extends FitsTypeHandler {

    /** _more_ */
    public static final int IDX_BASE = FITS_PROPS.length;

    /** _more_ */
    public static final int IDX_LOCATION = IDX_BASE + 0;

    /** _more_ */
    public static final int IDX_SOURCE = IDX_BASE + 1;

    /** _more_ */
    public static final int IDX_SOURCETYPE = IDX_BASE + 2;

    /** _more_ */
    public static final int IDX_MAGNITUDE = IDX_BASE + 3;

    /** _more_ */
    public static final String PROP_MAGNITUDE = "MAGNITUD";



    /**
     * ctor
     *
     * @param repository the repository
     * @param node the xml node from the types.xml file
     * @throws Exception On badness
     */
    public EclipseTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }



    /**
     * _more_
     *
     * @param entry _more_
     * @param header _more_
     * @param values _more_
     */
    public void processHeader(Entry entry, Header header, Object[] values) {
        super.processHeader(entry, header, values);


        values[IDX_MAGNITUDE] =
            new Double(header.getDoubleValue(PROP_MAGNITUDE, 0));
        values[IDX_LOCATION] = header.getStringValue("LOCATION");
        String source = header.getStringValue("SOURCE");
        if (source != null) {
            List<String> tuples = StringUtil.splitUpTo(source, " ", 2);
            if (tuples.size() == 2) {
                values[IDX_SOURCE]     = tuples.get(1).trim();
                values[IDX_SOURCETYPE] = tuples.get(0).trim();
            }
        }
    }



    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry)
            throws Exception {
        super.initializeNewEntry(request, entry);
        File imageFile = getFitsOutputHandler().outputImage(
                             getRepository().getTmpRequest(),
                             entry.getFile(), -1);
        if (imageFile != null) {
            String fileName = getStorageManager().copyToEntryDir(entry,
                                  imageFile).getName();
            Metadata thumbnailMetadata =
                new Metadata(getRepository().getGUID(), entry.getId(),
                             ContentMetadataHandler.TYPE_THUMBNAIL, false,
                             fileName, null, null, null, null);

            System.err.println("Adding metadata: " + entry);
            getMetadataManager().addMetadata(entry, thumbnailMetadata);
        } else {
            System.err.println("Failed to make image:" + entry.getFile());
        }
    }


}
