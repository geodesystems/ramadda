/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.dif;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;

import org.ramadda.util.DifUtil;
import org.ramadda.util.HtmlUtils;

import org.w3c.dom.*;


import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;






/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class DifOutputHandler extends OutputHandler {



    /** _more_ */
    public static final OutputType OUTPUT_DIF_XML =
        new OutputType("Dif-XML", "dif.xml", OutputType.TYPE_FEEDS, "",
                       ICON_DIF);


    /** _more_ */
    public static final OutputType OUTPUT_DIF_TEXT =
        new OutputType("Dif-Text", "dif.text", OutputType.TYPE_FEEDS, "",
                       ICON_DIF);


    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public DifOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_DIF_XML);
        //        addType(OUTPUT_DIF_TEXT);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param state _more_
     * @param links _more_
     *
     *
     * @throws Exception _more_
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
        if (state.isDummyGroup()) {
            return;
        }
        if (state.getEntry() != null) {
            links.add(makeLink(request, state.getEntry(), OUTPUT_DIF_XML));
            //            links.add(makeLink(request, state.getEntry(), OUTPUT_DIF_TEXT));
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param group _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> children)
            throws Exception {
        return outputEntry(request, outputType, group);
    }


    /**
     * _more_
     *
     * @param tag _more_
     * @param parent _more_
     * @param text _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Element tag(String tag, Element parent, String text)
            throws Exception {
        return XmlUtil.create(parent.getOwnerDocument(), tag, parent, text,
                              null);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
        Document doc = XmlUtil.makeDocument();
        Element root = XmlUtil.create(doc, DifUtil.TAG_DIF, null,
                                      new String[] {
            "xmlns", "http://gcmd.gsfc.nasa.gov/Aboutus/xml/dif/",
            "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance",
            "xsi:schemaLocation",
            "http://gcmd.gsfc.nasa.gov/Aboutus/xml/dif/ http://gcmd.nasa.gov/Aboutus/xml/dif/dif_v9.7.1.xsd"
        });


        Element parent;


        tag(DifUtil.TAG_Entry_ID, root, entry.getId());
        tag(DifUtil.TAG_Entry_Title, root, entry.getName());
        tag(DifUtil.TAG_Summary, root, entry.getDescription());
        parent = tag(DifUtil.TAG_Temporal_Coverage, root, null);
        tag(DifUtil.TAG_Start_Date, parent,
            getDateHandler().formatYYYYMMDD(new Date(entry.getStartDate())));
        tag(DifUtil.TAG_Stop_Date, parent,
            getDateHandler().formatYYYYMMDD(new Date(entry.getEndDate())));
        if (entry.hasAreaDefined()) {
            parent = tag(DifUtil.TAG_Spatial_Coverage, root, null);
            tag(DifUtil.TAG_Northernmost_Latitude, parent,
                "" + entry.getNorth(request));
            tag(DifUtil.TAG_Southernmost_Latitude, parent,
                "" + entry.getSouth(request));
            tag(DifUtil.TAG_Westernmost_Longitude, parent,
                "" + entry.getWest(request));
            tag(DifUtil.TAG_Easternmost_Longitude, parent,
                "" + entry.getEast(request));
        }


        List<Metadata> metadataList = getMetadataManager().getMetadata(request,entry);
        List<MetadataHandler> metadataHandlers =
            repository.getMetadataManager().getMetadataHandlers();
        for (Metadata metadata : metadataList) {
            MetadataHandler metadataHandler =
                getMetadataManager().findMetadataHandler(metadata);
            if (metadataHandler != null) {
                metadataHandler.addMetadataToXml(request,
                        MetadataTypeBase.TEMPLATETYPE_DIF, entry, metadata,
                        doc, root);

            }
        }


        StringBuffer sb = new StringBuffer();
        if (outputType.equals(OUTPUT_DIF_TEXT)) {
            XmlUtil.toHtml(sb, root);

            return new Result("DIF-Text", sb);
        } else {
            sb.append(XmlUtil.XML_HEADER);
            sb.append(XmlUtil.toString(root));

            return new Result("dif", sb, "text/xml");
        }
    }

}
