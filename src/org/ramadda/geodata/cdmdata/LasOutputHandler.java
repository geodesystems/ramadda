/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.cdmdata;


import org.ramadda.repository.*;
import org.ramadda.repository.output.*;


import org.w3c.dom.*;

import ucar.nc2.Variable;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.grid.GridDataset;

import ucar.unidata.xml.XmlUtil;


import java.util.ArrayList;
import java.util.List;




/**
 *
 *
 * @version $Revision: 1.3 $
 */
public class LasOutputHandler extends OutputHandler {

    /** netcdf standard name */
    public static final String NCATTR_STANDARD_NAME = "standard_name";

    /** las xml tag */
    public static final String TAG_LASDATA = "lasdata";

    /** las xml tag */
    public static final String TAG_INSTITUTION = "institution";

    /** las xml tag */
    public static final String TAG_OPERATIONS = "operations";

    /** las xml tag */
    public static final String TAG_SHADE = "shade";

    /** las xml tag */
    public static final String TAG_ARG = "arg";

    /** las xml tag */
    public static final String TAG_DATASETS = "datasets";

    /** las xml tag */
    public static final String TAG_VARIABLES = "variables";

    /** las xml tag */
    public static final String TAG_LINK = "link";

    /** las xml tag */
    public static final String TAG_COMPOSITE = "composite";

    /** las xml tag */
    public static final String TAG_GRIDS = "grids";

    /** las xml tag */
    public static final String TAG_AXES = "axes";

    /** las xml */
    public static final String ATTR_NAME = "name";

    /** las xml attribute name */
    public static final String ATTR_URL = "url";

    /** las xml attribute name */
    public static final String ATTR_CLASS = "class";

    /** las xml attribute name */
    public static final String ATTR_METHOD = "method";

    /** las xml attribute name */
    public static final String ATTR_TYPE = "type";

    /** las xml attribute name */
    public static final String ATTR_DOC = "doc";

    /** las xml attribute name */
    public static final String ATTR_UNITS = "units";

    /** las xml attribute name */
    public static final String ATTR_MATCH = "match";

    /** las xml attribute name */
    public static final String ATTR_JS = "js";

    /** las xml attribute name */
    public static final String ATTR_SIZE = "size";

    /** las xml attribute name */
    public static final String ATTR_START = "start";

    /** las xml attribute name */
    public static final String ATTR_STEP = "step";



    /** The output type */
    public static final OutputType OUTPUT_LAS_XML =
        new OutputType("LAS-XML", "las.xml", OutputType.TYPE_FEEDS, "", null);


    /**
     * ctor
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public LasOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_LAS_XML);
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public CdmDataOutputHandler getDataOutputHandler() throws Exception {
        return (CdmDataOutputHandler) getRepository().getOutputHandler(
            CdmDataOutputHandler.OUTPUT_OPENDAP.toString());
    }



    /**
     * This method gets called to determine if the given entry or entries can be displays as las xml
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

        CdmDataOutputHandler dataOutputHandler = getDataOutputHandler();
        if (state.group != null) {
            for (Entry child : state.getAllEntries()) {
                if (child.getType().equals(
                        OpendapLinkTypeHandler.TYPE_OPENDAPLINK)) {
                    continue;
                }

                if (dataOutputHandler.getCdmManager().canLoadAsGrid(child)) {
                    links.add(makeLink(request, state.group, OUTPUT_LAS_XML));

                    break;
                }
            }
        } else if (state.entry != null) {
            if ( !state.entry.getType().equals(
                    OpendapLinkTypeHandler.TYPE_OPENDAPLINK)) {
                if (dataOutputHandler.getCdmManager().canLoadAsGrid(
                        state.entry)) {
                    links.add(makeLink(request, state.entry, OUTPUT_LAS_XML));
                }
            }
        }
    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    private String getTagName(String s) {
        s = s.replace(" ", "_");
        s = s.replace("-", "_");
        s = s.replace("%", "_");
        s = s.replace(":", "_");
        s = s.replace("=", "_");
        s = s.replace("=", "_");

        return s;
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
        return outputLas(request, children);
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
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(entry);

        return outputLas(request, entries);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputLas(Request request, List<Entry> entries)
            throws Exception {

        CdmDataOutputHandler dataOutputHandler = getDataOutputHandler();
        Document             doc               = XmlUtil.makeDocument();

        //create the root element
        Element root = XmlUtil.create(doc, TAG_LASDATA, null,
                                      new String[] {});

        XmlUtil.create(TAG_INSTITUTION, root, new String[] { ATTR_NAME,
                getRepository().getRepositoryName(), ATTR_URL,
                request.getAbsoluteUrl("") });

        Element datasetsNode = XmlUtil.create(TAG_DATASETS, root);

        //Loop on the entries
        for (Entry entry : entries) {
            if ( !dataOutputHandler.getCdmManager().canLoadAsGrid(entry)) {
                //not a grid
                continue;
            }

            //<coads_climatology_cdf name="COADS Climatology" url="file:coads_climatology" doc="doc/coads_climatology.html">
            String id = entry.getId();

            //for now use the entry id as the tag name
            String tagName = "data_" + getTagName(id);

            XmlUtil.create(tagName, datasetsNode);

            Element entryNode = XmlUtil.create(tagName, datasetsNode,
                                    new String[] {
                ATTR_NAME, entry.getName(), ATTR_URL,
                request.getAbsoluteUrl(
                    getRepository().URL_ENTRY_SHOW
                    + dataOutputHandler.getOpendapUrl(entry)),
                ATTR_DOC,
                request.getAbsoluteUrl(
                    request.makeUrl(
                        getRepository().URL_ENTRY_SHOW, ARG_ENTRYID,
                        entry.getId()))
            });

            Element variablesNode = XmlUtil.create(TAG_VARIABLES, entryNode);

            //Get the netcdf dataset from the dataoutputhandler
            String path = dataOutputHandler.getCdmManager().getPath(entry);
            NetcdfDataset dataset =
                dataOutputHandler.getCdmManager().getNetcdfDataset(entry,
                    path);
            try {
                //TODO: determine which variables are the actual data variables
                //and add in the axis information
                for (Variable var : dataset.getVariables()) {
                    if (var instanceof CoordinateAxis) {
                        CoordinateAxis ca       = (CoordinateAxis) var;
                        AxisType       axisType = ca.getAxisType();
                        if (axisType == null) {
                            continue;
                        }
                        if (axisType.equals(AxisType.Lat)) {}
                        else if (axisType.equals(AxisType.Lon)) {}
                        else if (axisType.equals(AxisType.Time)) {}
                        else {
                            // System.err.println("unknown axis:" + axisType + " for var:" + var.getName());
                        }

                        continue;
                    }


                    //<variables>     <airt name="Air Temperature" units="DEG C">
                    String varName = var.getShortName();
                    ucar.nc2.Attribute att =
                        var.findAttribute(NCATTR_STANDARD_NAME);
                    if (att != null) {
                        varName = att.getStringValue();
                    }
                    XmlUtil.create(getTagName(varName), variablesNode,
                                   new String[] { ATTR_NAME,
                            var.getFullName(), ATTR_UNITS,
                            var.getUnitsString() });
                }
            } finally {
                dataOutputHandler.getCdmManager().returnNetcdfDataset(path,
                        dataset);
            }
        }

        //Create the xml and return the result
        StringBuffer sb = new StringBuffer();
        sb.append(XmlUtil.XML_HEADER);
        sb.append(XmlUtil.toString(root));

        return new Result("dif", sb, "text/xml");
    }

}
