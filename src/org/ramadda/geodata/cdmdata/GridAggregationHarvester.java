/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.cdmdata;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.harvester.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;


import org.ramadda.util.sql.SqlUtil;


import org.w3c.dom.*;

import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.lang.reflect.*;


import java.net.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



import java.util.regex.*;


/**
 * Class SqlUtil _more_
 *
 *
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public class GridAggregationHarvester extends PatternHarvester {

    /** _more_ */
    public static final String ATTR_AGGREGATIONTYPE = "aggregationtype";

    /** _more_ */
    public static final String ATTR_AGGREGATIONCOORDINATE =
        "aggregationcoordinate";

    /** _more_ */
    private String aggregationType = NcmlUtil.AGG_JOINEXISTING;

    /** _more_ */
    private String aggregationCoordinate = "time";

    /**
     * _more_
     *
     * @param repository _more_
     * @param id _more_
     *
     * @throws Exception _more_
     */
    public GridAggregationHarvester(Repository repository, String id)
            throws Exception {
        super(repository, id);
    }

    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     *
     * @throws Exception _more_
     */
    public GridAggregationHarvester(Repository repository, Element element)
            throws Exception {
        super(repository, element);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getLastGroupType() {
        return GridAggregationTypeHandler.TYPE_GRIDAGGREGATION;
    }

    /**
     * _more_
     *
     * @param element _more_
     *
     * @throws Exception _more_
     */
    protected void init(Element element) throws Exception {
        super.init(element);

        aggregationCoordinate = XmlUtil.getAttribute(element,
                ATTR_AGGREGATIONCOORDINATE, aggregationCoordinate);
        aggregationType = XmlUtil.getAttribute(element, ATTR_AGGREGATIONTYPE,
                aggregationType);

    }

    /**
     * _more_
     *
     * @param element _more_
     *
     * @throws Exception _more_
     */
    public void applyState(Element element) throws Exception {
        super.applyState(element);
        element.setAttribute(ATTR_AGGREGATIONCOORDINATE,
                             aggregationCoordinate);
        element.setAttribute(ATTR_AGGREGATIONTYPE, aggregationType);
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    public void applyEditForm(Request request) throws Exception {
        super.applyEditForm(request);
        aggregationCoordinate = request.getString(ATTR_AGGREGATIONCOORDINATE,
                aggregationCoordinate);
        aggregationType = request.getString(ATTR_AGGREGATIONTYPE,
                                            aggregationType);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void createEditForm(Request request, StringBuffer sb)
            throws Exception {
        super.createEditForm(request, sb);
        List<String> types =
            (List<String>) Misc.newList(NcmlUtil.AGG_JOINEXISTING,
                                        NcmlUtil.AGG_UNION);

        sb.append(
            HtmlUtils.formEntry(
                msgLabel("Time coordinate"),
                HtmlUtils.input(
                    ATTR_AGGREGATIONCOORDINATE, aggregationCoordinate,
                    HtmlUtils.SIZE_60)));
        sb.append(HtmlUtils.formEntry(msgLabel("Aggregation type"),
                                      HtmlUtils.select(ATTR_AGGREGATIONTYPE,
                                          types, aggregationType)));
    }


    /**
     * _more_
     *
     * @param entry _more_
     */
    @Override
    public void initEntry(Entry entry) {
        super.initEntry(entry);
        if (entry.getType().equals(
                GridAggregationTypeHandler.TYPE_GRIDAGGREGATION)) {
            //We're not using these 3 parameters
            String fields  = "";
            String files   = "";
            String pattern = "";
            entry.setValues(new Object[] { aggregationType,
                                           aggregationCoordinate, fields,
                                           files, pattern });

        }
    }



    /**
     * _more_
     *
     * @param fileInfo _more_
     * @param originalFile _more_
     * @param entry _more_
     *
     * @return _more_
     */
    public Entry initializeNewEntry(HarvesterFile fileInfo,
                                    File originalFile, Entry entry) {
        try {
            /*
    Object[] values = entry.getValues();
    if(values==null) values = new Object[2];
    values[1] = contents;*/
            return entry;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getDescription() {
        return "Grid Aggregation";
    }

}
