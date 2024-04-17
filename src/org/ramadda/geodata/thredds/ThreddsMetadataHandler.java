/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.thredds;


import org.ramadda.data.util.CdmUtil;

import org.ramadda.geodata.cdmdata.CdmDataOutputHandler;
import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.metadata.MetadataHandler;
import org.ramadda.repository.metadata.MetadataTypeBase;
import org.ramadda.util.Utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.ma2.MAMath;

import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.grid.GridDataset;

import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.units.DateRange;

import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.util.CatalogUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import visad.Unit;
import visad.UnitException;

import visad.data.units.NoSuchUnitException;

import visad.jmet.MetUnits;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public class ThreddsMetadataHandler extends MetadataHandler {

    /** _more_ */
    private static boolean debug = false;

    /** _more_ */
    public static final String TAG_VARIABLES = "variables";


    /** _more_ */
    public static final String ATTR_NAME = "name";

    /** _more_ */
    public static final String ATTR_UNITS = "units";

    /** _more_ */
    public static final String ATTR_ROLE = "role";

    /** _more_ */
    public static final String ATTR_EMAIL = "email";

    /** _more_ */
    public static final String ATTR_URL = "url";

    /** _more_ */
    public static final String ATTR_VOCABULARY = "vocabulary";

    /** _more_ */
    public static final String ATTR_VALUE = "value";



    /** _more_ */
    public static final String TYPE_CREATOR = "thredds.creator";

    /** _more_ */
    public static final String TYPE_LINK = "thredds.link";

    /** _more_ */
    public static final String TYPE_DATAFORMAT = "thredds.dataFormat";

    /** _more_ */
    public static final String TYPE_DATATYPE = "thredds.dataType";

    /** _more_ */
    public static final String TYPE_AUTHORITY = "thredds.authority";

    /** _more_ */
    public static final String TYPE_VARIABLES = "thredds.variables";

    /** _more_ */
    public static final String TYPE_VARIABLE = "thredds.variable";

    /** _more_ */
    public static final String TYPE_STANDARDNAME = "thredds.standardname";

    /** _more_ */
    public static final String TYPE_PUBLISHER = "thredds.publisher";

    /** _more_ */
    public static final String TYPE_PROJECT = "thredds.project";

    /** _more_ */
    public static final String TYPE_KEYWORD = "thredds.keyword";

    /** _more_ */
    public static final String TYPE_CONTRIBUTOR = "thredds.contributor";

    /** _more_ */
    public static final String TYPE_PROPERTY = "thredds.property";

    /** _more_ */
    public static final String TYPE_DOCUMENTATION = "thredds.documentation";

    /** _more_ */
    public static final String TYPE_ICON = "thredds.icon";

    /** _more_ */
    public static final String TYPE_CDL = "thredds.cdl";


    /** _more_ */
    public static final String NCATTR_STANDARD_NAME = "standard_name";


    /** _more_ */
    public static final String PROP_STARTTIME_ATTRIBUTES =
        "cdm.attribute.starttimes";

    /** _more_ */
    public static final String PROP_ENDTIME_ATTRIBUTES =
        "cdm.attribute.endtimes";

    /**
     * _more_
     *
     * @param repository _more_
     *
     * @throws Exception _more_
     */
    public ThreddsMetadataHandler(Repository repository) throws Exception {
        super(repository);
    }


    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public ThreddsMetadataHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }




    /**
     * _more_
     *
     * @param var _more_
     * @param a _more_
     * @param toUnit _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private static double[] getRange(VariableSimpleIF var, Array a,
                                     Unit toUnit)
            throws Exception {
        MAMath.MinMax minmax = MAMath.getMinMax(a);
        Unit fromUnit = parseUnit(var.getUnitsString(), var.getUnitsString());
        /*
        System.out.println(var.getFullName());
        System.out.println("\tminmax:" + minmax.min + " " + minmax.max + " " + fromUnit);
        System.out.println("\tto unit:" + toUnit.toThis(minmax.min, fromUnit) + " " +toUnit.toThis(minmax.min, fromUnit));
        System.out.println("\tto unit:" + new Date((long)(1000*toUnit.toThis(minmax.min, toUnit))));
        */
        double[] result = new double[] { toUnit.toThis(minmax.min, fromUnit),
                                         toUnit.toThis(minmax.max,
                                             fromUnit) };

        return result;
    }


    /**
     * _more_
     *
     * @param unitIdentifier _more_
     * @param unitName _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Unit parseUnit(String unitIdentifier, String unitName)
            throws Exception {

        if (unitIdentifier == null) {
            return null;
        }
        if (unitName == null) {
            unitName = unitIdentifier;
        }
        Unit u = null;
        // clean up ** and replace with nothing
        unitIdentifier = unitIdentifier.replaceAll("\\*\\*", "");
        try {

            try {
                String realUnitName = MetUnits.makeSymbol(unitIdentifier);
                //A hack to fix errors with oscar files
                realUnitName = realUnitName.replace("degrees-", "degrees_");
                u            = visad.data.units.Parser.parse(realUnitName);
            } catch (NoSuchUnitException nsu) {
                if (unitIdentifier.indexOf("_") >= 0) {
                    unitIdentifier = unitIdentifier.replace('_', ' ');
                    String realUnitName = MetUnits.makeSymbol(unitIdentifier);
                    u = visad.data.units.Parser.parse(realUnitName);
                } else {
                    throw new IllegalArgumentException("No such unit:" + nsu);
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();

            throw new IllegalArgumentException("Error parsing unit:\""
                    + unitIdentifier + "\"   " + exc);
        }
        try {
            u = u.clone(unitName);
        } catch (UnitException ue) {}

        return u;
    }




    /**
     * _more_
     *
     * @param var _more_
     * @param ca _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static List<Date> getDates(VariableSimpleIF var, CoordinateAxis ca)
            throws Exception {

        CalendarDateUnit dateUnit = getCalendarDateUnit(ca);
        List<Date>       dates    = new ArrayList<Date>();
        Array            a        = ca.read();
        IndexIterator    iter     = a.getIndexIterator();
        while (iter.hasNext()) {
            double val = iter.getDoubleNext();
            if (val != val) {
                continue;
            }
            CalendarDate cDate = dateUnit.makeCalendarDate(val);
            dates.add(CdmUtil.makeDate(cDate));
        }

        return dates;
    }

    /**
     * Get a CalendarDateUnit for the given axis
     *
     * @param timeAxis the axis
     * @return the CalendarDateUnit
     */
    private static CalendarDateUnit getCalendarDateUnit(
            CoordinateAxis timeAxis) {
        Attribute cattr = timeAxis.findAttribute(CF.CALENDAR);
        String    s     = (cattr == null)
                          ? null
                          : cattr.getStringValue();
        Calendar  cal   = null;
        if (s == null) {
            cal = Calendar.gregorian;
        } else {
            cal = ucar.nc2.time.Calendar.get(s);
        }
        CalendarDateUnit dateUnit =
        // this will throw exception on failure
        CalendarDateUnit.withCalendar(cal, timeAxis.getUnitsString());

        return dateUnit;
    }

    /**
     * _more_
     *
     * @param dataset _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static List<Date> getDates(NetcdfDataset dataset)
            throws Exception {
        List<Variable> variables = dataset.getVariables();
        for (Variable var : variables) {
            if (var instanceof CoordinateAxis) {
                CoordinateAxis ca       = (CoordinateAxis) var;
                AxisType       axisType = ca.getAxisType();
                if (axisType.equals(AxisType.Time)) {
                    return getDates(var, ca);
                }
            }
        }

        return null;
    }




    /**
     * _more_
     *
     * @param var _more_
     * @param ca _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Date[] getMinMaxDates(VariableSimpleIF var,
                                        CoordinateAxis ca)
            throws Exception {
        Date[] mmDate = null;
        if (ca instanceof CoordinateAxis1D) {
            CalendarDateUnit dateUnit = getCalendarDateUnit(ca);
            MAMath.MinMax    minmax   = MAMath.getMinMax(ca.read());
            CalendarDate     minDate  = dateUnit.makeCalendarDate(minmax.min);
            CalendarDate     maxDate  = dateUnit.makeCalendarDate(minmax.max);
            mmDate = new Date[] { CdmUtil.makeDate(minDate),
                                  CdmUtil.makeDate(maxDate) };

        } else {  // old way - doesn't work for non-standard (e.g. no leap) calendars
            double[] minmax = getRange(var, ca.read(),
                                       visad.CommonUnit.secondsSinceTheEpoch);

            mmDate = new Date[] { new Date((long) minmax[0] * 1000),
                                  new Date((long) minmax[1] * 1000) };
        }

        return mmDate;
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
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param metadataList _more_
     * @param extra _more_
     * @param shortForm _more_
     */
    public void getInitialMetadata(Request request, Entry entry,
                                   List<Metadata> metadataList,
                                   Hashtable extra, boolean shortForm) {

        Metadata      metadata        = null;
        String        varName         = null;
        NetcdfDataset dataset         = null;
        GridDataset   gridDataset     = null;
        boolean       haveDate        = false;
        StringBuffer  descriptionAttr = new StringBuffer();
        try {
            CdmDataOutputHandler dataOutputHandler = getDataOutputHandler();
            super.getInitialMetadata(request, entry, metadataList, extra,
                                     shortForm);

            if ( !dataOutputHandler.getCdmManager().canLoadAsCdm(entry)) {
                return;
            }
            //            System.err.println("metadata harvest");

            String path = dataOutputHandler.getCdmManager().getPath(entry);
            dataset = NetcdfDataset.openDataset(path);

            try {
                gridDataset = new GridDataset(dataset);
            } catch (Exception ignore) {
                //                System.err.println("Not a grid");
            }
            boolean         haveBounds = false;
            List<Attribute> attrs      = null;
            List            variables  = null;

            attrs     = dataset.getGlobalAttributes();
            variables = dataset.getVariables();


            for (Attribute attr : attrs) {
                String name  = attr.getFullName();
                String value = attr.getStringValue();
                if (value == null) {
                    value = "" + attr.getNumericValue();
                }
                if (CdmUtil.ATTR_MAXLON.equals(name)
                        || name.equals("EastBoundingCoordinate")) {
                    extra.put(ARG_MAXLON, Double.parseDouble(value));

                    continue;
                }
                if (CdmUtil.ATTR_MINLON.equals(name)
                        || name.equals("WestBoundingCoordinate")) {
                    extra.put(ARG_MINLON, Double.parseDouble(value));

                    continue;
                }
                if (CdmUtil.ATTR_MAXLAT.equals(name)
                        || name.equals("NorthBoundingCoordinate")) {
                    extra.put(ARG_MAXLAT, Double.parseDouble(value));

                    continue;
                }
                if (CdmUtil.ATTR_MINLAT.equals(name)
                        || name.equals("SouthBoundingCoordinate")) {
                    extra.put(ARG_MINLAT, Double.parseDouble(value));

                    continue;
                }

                boolean isStartTime = isStartTimeAttribute(name);
                boolean isEndTime   = isEndTimeAttribute(name);

                if (isStartTime || isEndTime) {
                    Date date = getDate(value);
                    if (isStartTime) {
                        extra.put(ARG_FROMDATE, date);
                    } else {
                        extra.put(ARG_TODATE, date);
                    }
                    haveDate = true;

                    continue;
                }
                if (shortForm) {
                    continue;
                }

                String metadataType =
                    getRepository().getProperty("metadata.type." + name);
                if (metadataType != null) {
                    metadata = new Metadata(getRepository().getGUID(),
                                            entry.getId(), metadataType,
                                            DFLT_INHERITED, value,
                                            Metadata.DFLT_ATTR,
                                            Metadata.DFLT_ATTR,
                                            Metadata.DFLT_ATTR,
                                            Metadata.DFLT_EXTRA);

                    if ( !entry.hasMetadata(metadata)) {
                        metadataList.add(metadata);
                    }

                    continue;
                }


                if (entry.getDescription().length() == 0) {
                    if (CdmUtil.ATTR_ABSTRACT.equals(name)) {
                        descriptionAttr.append(value);

                        continue;
                    } else if (CdmUtil.ATTR_DESCRIPTION.equals(name)) {
                        descriptionAttr.append(value);

                        continue;
                    }
                }

                //Only set the name if its not different from the file name
                //                if(CdmUtil.ATTR_TITLE.equals(name) && entry.getTypeHandler().entryHasDefaultName(entry)) {
                //                    entry.setName(value);
                //                    continue;
                //                }

                if (CdmUtil.ATTR_KEYWORDS.equals(name)) {
                    List<String> keywords =
                        (List<String>) StringUtil.split(value, ";", true,
                            true);
                    if (keywords.size() == 1) {
                        keywords.addAll(
                            (List<String>) StringUtil.split(
                                value, ",", true, true));
                    }
                    for (String keyword : keywords) {
                        try {
                            metadata =
                                new Metadata(getRepository().getGUID(),
                                             entry.getId(), TYPE_KEYWORD,
                                             DFLT_INHERITED, keyword,
                                             Metadata.DFLT_ATTR,
                                             Metadata.DFLT_ATTR,
                                             Metadata.DFLT_ATTR,
                                             Metadata.DFLT_EXTRA);
                        } catch (Exception exc) {
                            getRepository().getLogManager().logInfo(
                                "ThreddsMetadataHandler: Unable to add keyword metadata:"
                                + keyword);

                            continue;
                        }
                        if ( !entry.hasMetadata(metadata)) {
                            metadataList.add(metadata);
                        }
                    }

                    continue;
                }

                if (name.startsWith("_")) {
                    continue;
                }

                //Check if the string length is too long
                if ( !Metadata.lengthOK(name) || !Metadata.lengthOK(value)) {
                    getRepository().getLogManager().logInfo(
                        "ThreddsMetadataHandler: Unable to add attribute:"
                        + name);

                    continue;
                }


                metadata = new Metadata(getRepository().getGUID(),
                                        entry.getId(), TYPE_PROPERTY,
                                        DFLT_INHERITED, name, value,
                                        Metadata.DFLT_ATTR,
                                        Metadata.DFLT_ATTR,
                                        Metadata.DFLT_EXTRA);
                if ( !entry.hasMetadata(metadata)) {
                    metadataList.add(metadata);
                }
            }




            for (Object obj : variables) {
                VariableSimpleIF var = (VariableSimpleIF) obj;
                if (var instanceof CoordinateAxis) {
                    boolean        axisWasRecognized = true;
                    CoordinateAxis ca                = (CoordinateAxis) var;
                    AxisType       axisType          = ca.getAxisType();
                    if (axisType == null) {
                        continue;
                    }
                    if (axisType.equals(AxisType.Lat)) {
                        double[] minmax = getRange(var, ca.read(),
                                              visad.CommonUnit.degree);
                        if ((minmax[0] == minmax[0])
                                && (minmax[1] == minmax[1])) {
                            if (extra.get(ARG_MINLAT) == null) {
                                extra.put(ARG_MINLAT, minmax[0]);
                            }
                            if (extra.get(ARG_MAXLAT) == null) {
                                extra.put(ARG_MAXLAT, minmax[1]);
                            }
                            haveBounds = true;
                        }

                    } else if (axisType.equals(AxisType.Lon)) {
                        double[] minmax = getRange(var, ca.read(),
                                              visad.CommonUnit.degree);
                        if ((minmax[0] == minmax[0])
                                && (minmax[1] == minmax[1])) {
                            if (extra.get(ARG_MINLON) == null) {
                                extra.put(ARG_MINLON, minmax[0]);
                            }
                            if (extra.get(ARG_MAXLON) == null) {
                                extra.put(ARG_MAXLON, minmax[1]);
                            }
                            haveBounds = true;
                        }
                    } else if (axisType.equals(AxisType.Time)) {
                        try {
                            //For now always use the axis dates even if we had a date from the attributes
                            if (true || !haveDate) {
                                Date[] dates = getMinMaxDates(var, ca);
                                if (dates != null) {
                                    Date minDate =
                                        (Date) extra.get(ARG_FROMDATE);
                                    Date maxDate =
                                        (Date) extra.get(ARG_TODATE);
                                    if (minDate != null) {
                                        dates[0] = DateUtil.min(dates[0],
                                                minDate);
                                    }
                                    if (maxDate != null) {
                                        dates[1] = DateUtil.max(dates[1],
                                                maxDate);
                                    }

                                    extra.put(ARG_FROMDATE, dates[0]);
                                    extra.put(ARG_TODATE, dates[1]);
                                    haveDate = true;
                                }
                            }
                        } catch (Exception exc) {
                            System.out.println("Error reading time axis for:"
                                    + entry.getResource());
                            System.out.println(exc);
                        }
                    } else {
                        axisWasRecognized = false;
                        //                        System.err.println("unknown axis:" + axisType + " for var:" + var.getFullName());
                    }
                    if (axisWasRecognized) {
                        continue;
                    }
                }



                if ( !shortForm) {
                    varName = var.getShortName();
                    try {
                        metadata = new Metadata(getRepository().getGUID(),
                                entry.getId(), TYPE_VARIABLE, DFLT_INHERITED,
                                varName, var.getFullName(),
                                var.getUnitsString(), Metadata.DFLT_ATTR,
                                Metadata.DFLT_EXTRA);
                    } catch (Exception exc) {
                        getRepository().getLogManager().logInfo(
                            "ThreddsMetadataHandler: Unable to add variable metadata:"
                            + varName);

                        continue;
                    }
                    if ( !entry.hasMetadata(metadata)) {
                        metadataList.add(metadata);
                    }

                    //Also add in the standard name
                    ucar.nc2.Attribute att =
                        var.findAttributeIgnoreCase(NCATTR_STANDARD_NAME);

                    if (att != null) {
                        varName = att.getStringValue();
                        try {
                            metadata =
                                new Metadata(getRepository().getGUID(),
                                             entry.getId(),
                                             TYPE_STANDARDNAME,
                                             DFLT_INHERITED, varName,
                                             var.getFullName(),
                                             var.getUnitsString(),
                                             Metadata.DFLT_ATTR,
                                             Metadata.DFLT_EXTRA);
                        } catch (Exception exc) {
                            getRepository().getLogManager().logInfo(
                                "ThreddsMetadataHandler: Unable to add variable metadata:"
                                + varName);

                            continue;
                        }
                        if ( !entry.hasMetadata(metadata)) {
                            metadataList.add(metadata);
                        }
                    }
                }


                for (Attribute attr : var.getAttributes()) {
                    String key = "metadata.variable." + var.getFullName()
                                 + "." + attr.getFullName();
                    String metadataType = getRepository().getProperty(key,
                                              (String) null);
                    //                    System.err.println ("Looking for:" + key);
                    if (metadataType != null) {
                        System.err.println("making variable level metadata:"
                                           + metadataType);
                        metadata = new Metadata(getRepository().getGUID(),
                                entry.getId(), metadataType, DFLT_INHERITED,
                                var.getFullName(), attr.getStringValue(),
                                Metadata.DFLT_ATTR, Metadata.DFLT_ATTR,
                                Metadata.DFLT_EXTRA);
                        if ( !entry.hasMetadata(metadata)) {
                            metadataList.add(metadata);
                        }
                    }
                }

            }

            //            System.err.println("\thave bounds:" + haveBounds);


            if (gridDataset != null) {
                gridDataset.calcBounds();
                CalendarDateRange dateRange =
                    gridDataset.getCalendarDateRange();
                if (dateRange != null && extra.get(ARG_FROMDATE)==null) {
		    System.err.println("DATE RANGE:" + dateRange +" Result:" +
				       CdmUtil.makeDate(dateRange.getStart()));
		    
                    extra.put(ARG_FROMDATE,
                              CdmUtil.makeDate(dateRange.getStart()));
                    extra.put(ARG_TODATE,
                              CdmUtil.makeDate(dateRange.getEnd()));
                }

                LatLonRect llr = null;
                for (ucar.nc2.dt.GridDataset.Gridset gridset :
                        gridDataset.getGridsets()) {
                    GridCoordSystem gridCoordSys =
                        gridset.getGeoCoordSystem();
                    llr = gridCoordSys.getLatLonBoundingBox();

                    break;
                }

                if (llr != null) {
                    /*
                    System.err.println("llr:" + llr);
                    System.err.println(
                        "crosses dateline:" + llr.crossDateline()
                        + " upperLeft:"
                        + llr.getUpperLeftPoint().getLongitude()
                        + " upperRight:"
                        + llr.getUpperRightPoint().getLongitude());
                     */



                    if ((llr.getLatMin() == llr.getLatMin())
                            && (llr.getLatMax() == llr.getLatMax())
                            && (llr.getLonMax() == llr.getLonMax())
                            && (llr.getLonMin() == llr.getLonMin())) {
                        haveBounds = true;
                        LatLonPointImpl ul = llr.getUpperLeftPoint();
                        LatLonPointImpl lr = llr.getLowerRightPoint();
                        if (extra.get(ARG_MINLON) == null) {
                            extra.put(ARG_MINLAT, llr.getLatMin());
                            extra.put(ARG_MAXLAT, llr.getLatMax());
                            extra.put(ARG_MINLON, ul.getLongitude());
                            extra.put(ARG_MAXLON, lr.getLongitude());
                        }
                    }
                }
            }




            //If we didn't have a lat/lon coordinate axis then check projection
            //We do this here after because I've seen some point files that have an incorrect 360 bbox
            if ( !haveBounds) {
                for (CoordinateSystem coordSys :
                        (List<CoordinateSystem>) dataset
                            .getCoordinateSystems()) {
                    ProjectionImpl proj = coordSys.getProjection();
                    if (proj == null) {
                        continue;
                    }
                    LatLonRect llr = proj.getDefaultMapAreaLL();
                    if ((llr.getLatMin() == llr.getLatMin())
                            && (llr.getLatMax() == llr.getLatMax())
                            && (llr.getLonMax() == llr.getLonMax())
                            && (llr.getLonMin() == llr.getLonMin())) {
                        haveBounds = true;
                        if (extra.get(ARG_MINLAT) == null) {
                            //                            System.err.println("\t"  +" bounds from cs:" + llr);
                            //                            System.err.println("\t"  +" proj:" + proj);
                            extra.put(ARG_MINLAT, llr.getLatMin());
                            extra.put(ARG_MAXLAT, llr.getLatMax());
                            extra.put(ARG_MINLON, llr.getLonMin());
                            extra.put(ARG_MAXLON, llr.getLonMax());
                            System.out.println(extra);
                        }

                        break;
                    }

                }
            }
        } catch (Exception exc) {
            System.out.println("Error reading metadata:"
                               + entry.getResource());
            System.out.println("Error:" + exc);
            exc.printStackTrace();
        } finally {
            try {
                if (gridDataset != null) {
                    gridDataset.close();
                }
                if (dataset != null) {
                    dataset.close();
                }
            } catch (Exception ignore) {}
        }

        //Set the description
        if ((entry.getDescription().length() == 0)
                && (descriptionAttr.length() > 0)) {
            entry.setDescription(descriptionAttr.toString());
        }

    }



    /**
     * _more_
     *
     * @param dateString _more_
     *
     * @return _more_
     *
     * @throws java.text.ParseException _more_
     */
    private Date getDate(String dateString) throws java.text.ParseException {
        //        System.err.println ("getDate:" + dateString +" Date:" +Utils.parseDate(dateString));
        //return Utils.parseDate(dateString);
        return Utils.parseDate(dateString);
    }

    /** _more_ */
    private HashSet startTimeAttrs;

    /** _more_ */
    private HashSet endTimeAttrs;

    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    private boolean isEndTimeAttribute(String name) {
        if (endTimeAttrs == null) {
            HashSet tmp = new HashSet();
            for (String attr :
                    StringUtil.split(
                        getRepository().getProperty(
                            PROP_ENDTIME_ATTRIBUTES, ""), ",", true, true)) {
                tmp.add(attr);

            }
            endTimeAttrs = tmp;
        }

        return endTimeAttrs.contains(name);
    }


    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    private boolean isStartTimeAttribute(String name) {
        if (startTimeAttrs == null) {
            HashSet tmp = new HashSet();
            for (String attr :
                    StringUtil.split(
                        getRepository().getProperty(
                            PROP_STARTTIME_ATTRIBUTES, ""), ",", true,
                                true)) {
                tmp.add(attr);
            }
            startTimeAttrs = tmp;
        }

        return startTimeAttrs.contains(name);
    }



    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
    public static String getTag(String type) {
        int idx = type.indexOf(".");
        if (idx < 0) {
            return type;
        }

        return type.substring(idx + 1);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param xmlType _more_
     * @param entry _more_
     * @param metadata _more_
     * @param doc _more_
     * @param datasetNode _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    public boolean addMetadataToXml(Request request, String xmlType,
                                    Entry entry, Metadata metadata,
                                    Document doc, Element datasetNode)
            throws Exception {


        if (metadata.getType().equals(TYPE_VARIABLE)
                && xmlType.equals(MetadataTypeBase.TEMPLATETYPE_THREDDS)) {
            Element variablesNode = XmlUtil.getElement(datasetNode,
                                        TAG_VARIABLES);
            if (variablesNode == null) {
                variablesNode = XmlUtil.create(doc, TAG_VARIABLES,
                        datasetNode);
            }
            XmlUtil.create(doc, getTag(TYPE_VARIABLE), variablesNode,
                           metadata.getAttr2(), new String[] { ATTR_NAME,
                    metadata.getAttr1(), ATTR_UNITS, metadata.getAttr3() });

            return true;
        } else {
            return super.addMetadataToXml(request, xmlType, entry, metadata,
                                          doc, datasetNode);

        }
    }



    /**
     * _more_
     *
     * @param tag _more_
     * @param type _more_
     *
     * @return _more_
     */
    public boolean isTag(String tag, String type) {
        return ("thredds." + tag).toLowerCase().equals(type);
    }


    /**
     * _more_
     *
     * @param child _more_
     *
     * @return _more_
     */
    public Metadata makeMetadataFromCatalogNode(Element child) {
        String tag = child.getTagName();
        if (isTag(tag, TYPE_DOCUMENTATION)) {
            if (XmlUtil.hasAttribute(child, "xlink:href")) {
                String url = XmlUtil.getAttribute(child, "xlink:href");

                return new Metadata(getRepository().getGUID(), "", TYPE_LINK,
                                    DFLT_INHERITED,
                                    XmlUtil.getAttribute(child,
                                        "xlink:title", url), url,
                                            Metadata.DFLT_ATTR,
                                            Metadata.DFLT_ATTR,
                                            Metadata.DFLT_EXTRA);
            } else {
                String type = XmlUtil.getAttribute(child, "type", "summary");
                String text = XmlUtil.getChildText(child).trim();

                return new Metadata(getRepository().getGUID(), "",
                                    TYPE_DOCUMENTATION, DFLT_INHERITED, type,
                                    text, Metadata.DFLT_ATTR,
                                    Metadata.DFLT_ATTR, Metadata.DFLT_EXTRA);
            }
        } else if (isTag(tag, TYPE_PROJECT)) {
            String text = XmlUtil.getChildText(child).trim();

            return new Metadata(getRepository().getGUID(), "", TYPE_PROJECT,
                                DFLT_INHERITED, text,
                                XmlUtil.getAttribute(child, ATTR_VOCABULARY,
                                    ""), Metadata.DFLT_ATTR,
                                         Metadata.DFLT_ATTR,
                                         Metadata.DFLT_EXTRA);
        } else if (isTag(tag, TYPE_CONTRIBUTOR)) {
            String text = XmlUtil.getChildText(child).trim();

            return new Metadata(getRepository().getGUID(), "",
                                TYPE_CONTRIBUTOR, DFLT_INHERITED, text,
                                XmlUtil.getAttribute(child, ATTR_ROLE, ""),
                                Metadata.DFLT_ATTR, Metadata.DFLT_ATTR,
                                Metadata.DFLT_EXTRA);
        } else if (isTag(tag, TYPE_PUBLISHER) || isTag(tag, TYPE_CREATOR)) {
            Element nameNode = XmlUtil.findChild(child, CatalogUtil.TAG_NAME);
            String  name     = XmlUtil.getChildText(nameNode).trim();
            String vocabulary = XmlUtil.getAttribute(nameNode,
                                    ATTR_VOCABULARY, "");
            String email = "";
            String url   = "";
            Element contactNode = XmlUtil.findChild(child,
                                      CatalogUtil.TAG_CONTACT);
            if (contactNode != null) {
                email = XmlUtil.getAttribute(contactNode, ATTR_EMAIL, "");
                url   = XmlUtil.getAttribute(contactNode, ATTR_URL, "");
            }

            return new Metadata(getRepository().getGUID(), "",
                                getType("thredds." + tag), DFLT_INHERITED,
                                name, vocabulary, email, url,
                                Metadata.DFLT_EXTRA);
        } else if (isTag(tag, TYPE_KEYWORD)) {
            String text = XmlUtil.getChildText(child).trim();
            //Some of the catalogs have new lines in the keyword
            text = text.replace("\r\n", " ");
            text = text.replace("\n", " ");

            return new Metadata(getRepository().getGUID(), "", TYPE_KEYWORD,
                                DFLT_INHERITED, text,
                                XmlUtil.getAttribute(child, ATTR_VOCABULARY,
                                    ""), Metadata.DFLT_ATTR,
                                         Metadata.DFLT_ATTR,
                                         Metadata.DFLT_EXTRA);

        } else if (isTag(tag, TYPE_AUTHORITY) || isTag(tag, TYPE_DATATYPE)
                   || isTag(tag, TYPE_DATAFORMAT)) {
            String text = XmlUtil.getChildText(child).trim();
            text = text.replace("\n", "");

            return new Metadata(getRepository().getGUID(), "",
                                getType("thredds." + tag), DFLT_INHERITED,
                                text, Metadata.DFLT_ATTR, Metadata.DFLT_ATTR,
                                Metadata.DFLT_ATTR, Metadata.DFLT_EXTRA);
        } else if (isTag(tag, TYPE_PROPERTY)) {
            return new Metadata(getRepository().getGUID(), "",
                                getType("thredds." + tag), DFLT_INHERITED,
                                XmlUtil.getAttribute(child, ATTR_NAME),
                                XmlUtil.getAttribute(child, ATTR_VALUE),
                                Metadata.DFLT_ATTR, Metadata.DFLT_ATTR,
                                Metadata.DFLT_EXTRA);
        }

        return null;
    }




}
