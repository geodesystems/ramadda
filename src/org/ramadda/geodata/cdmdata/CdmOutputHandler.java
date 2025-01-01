/*
 * Copyright (c) 2008-2025 Geode Systems LLC
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

package org.ramadda.geodata.cdmdata;



import org.ramadda.data.record.RecordField;
import org.ramadda.repository.DateHandler;

import org.ramadda.repository.Entry;
import org.ramadda.repository.Link;
import org.ramadda.repository.PageHandler;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.ramadda.repository.Result;
import org.ramadda.repository.auth.AccessException;
import org.ramadda.repository.map.MapProperties;
import org.ramadda.repository.map.MapInfo;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.OutputType;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;

import org.w3c.dom.Element;


import thredds.server.ncss.format.SupportedFormat;
//import thredds.server.ncss.params.PointDataRequestParamsBean;
import thredds.server.ncss.params.NcssParamsBean;
import thredds.server.ncss.util.NcssRequestUtils;
import thredds.server.ncss.view.gridaspoint.PointDataStream;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Range;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;

import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridAsPointDataset;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft.FeatureCollection;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.NestedPointFeatureCollection;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.jni.netcdf.Nc4Iosp;
import ucar.nc2.ncml.NcMLWriter;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.util.DiskCache2;


import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.Counter;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;


import java.awt.Color;
import java.awt.image.BufferedImage;

import java.io.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import java.util.function.DoubleFunction;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;


/**
 * A class for handling CDM data output
 */
public class CdmOutputHandler extends OutputHandler implements CdmConstants {

    /** _more_ */
    private static Properties gridProperties;




    /**
     *
     * @param repository  the repository
     * @param name        the name of this handler
     *
     * @throws Exception problem creating class
     */
    public CdmOutputHandler(Repository repository, String name)
	throws Exception {
        super(repository, name);
    }

    /**
     *     Create a GridPointOutputHandler
     *
     *     @param repository  the repository
     *     @param element     the element
     *     @throws Exception On badness
     */
    public CdmOutputHandler(Repository repository, Element element)
	throws Exception {
        super(repository, element);
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public CdmDataOutputHandler getCdmDataOutputHandler() {
        return (CdmDataOutputHandler) getRepository().getOutputHandler(
								       CdmDataOutputHandler.class);
    }


    /**
     * Get the CdmManager
     *
     * @return  the CDM data manager
     */
    public CdmManager getCdmManager() {
        return getCdmDataOutputHandler().getCdmManager();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public static Properties getProperties() {
        if (gridProperties == null) {
            try {
                InputStream inputStream =
                    IOUtil.getInputStream(
					  "/org/ramadda/geodata/cdmdata/resources/netcdf.properties",
					  GridPointOutputHandler.class);
                Properties tmp = new Properties();
                tmp.load(inputStream);
                IOUtil.close(inputStream);
                gridProperties = tmp;
            } catch (Exception exc) {
                throw new IllegalArgumentException(exc);
            }
        }

        return gridProperties;
    }

    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public static String getAlias(String name) {
        String n = (String) getProperties().get(name + ".alias");
        if (n != null) {
            return n;
        }
        return name;
    }

    /**
     ** 
     */
    public static boolean isVariableKnownAs(String varName, String otherName) {
	if(Misc.equals(varName, otherName)) return true;
	String alias = getAlias(varName);
	if(alias!=null) {
	    return Misc.equals(alias, otherName);
	}
	return false;
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public static String getLabel(String name, String dflt) {
        if (name == null) {
            return dflt;
        }
        String n = (String) getProperties().get(name + ".label");
        if (n != null) {
            return n;
        }

        return dflt;
    }



    public static String getProperty(String name, String what, String dflt) {
        if (name == null) {
            return dflt;
        }
	name = name.toLowerCase();
        String n = (String) getProperties().get(name + "." + what);
        if (n != null) {
            return n;
        }
	String alias = getAlias(name);
	if(alias!=null)
	    return getProperty(alias+"." + what);
        return dflt;
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public static String getProperty(String name, String dflt) {
        String value = (String) getProperties().get(name);
	if(value==null)
	    value = (String) getProperties().get(name.toLowerCase());
        if (value != null) {
            return value;
        }

        return dflt;
    }

    public static String getPropertyWithSuffix(String suffix, String dflt,String...vars) {
	for(String var: vars) {
            String v = getProperty(var + suffix,null);
	    if(v!=null) return v;
	}
	return dflt;
    }


    public static String getProperty(String name) {
        return  (String) getProperties().get(name);
    }    

    public static double getProperty(String name, double dflt) {
        String n = (String) getProperties().get(name);
        if (n != null) {
            return Double.parseDouble(n);
        }
        return dflt;
    }



    /**
     * Get the path to the data
     *
     * @param entry  the Entry
     *
     * @return the path
     *
     * @throws Exception problemo
     */
    public String getPath(Entry entry) throws Exception {
        return getPath(null, entry);
    }


    /**
     * Get the path for the Entry
     *
     *
     * @param request the Request
     * @param entry   the Entry
     *
     * @return   the path
     *
     * @throws Exception problem getting the path
     */
    public String getPath(Request request, Entry entry) throws Exception {
        String path  = getCdmManager().getPath(request, entry);
	//	System.err.println("P:" +  path);
	return path;
    }


    
    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public static String getUnit(String unit) {
	return getProperty("unit."+ unit.toLowerCase(),unit);
    }



    public static DoubleFunction<Float> getScaler(String unit) {
	unit = unit.toLowerCase();
	String prefix = "unit." + unit+".";
	final float scale = (float)getProperty(prefix  + "scale", 1.0);
	final float offset1 = (float)getProperty(prefix  + "offset1", 0.0);
	final float offset2 = (float)getProperty(prefix    + "offset2", 0.0);
	//	System.err.println(prefix +" o:" + offset1);
	return  v -> (float)((v+offset1)*scale+offset2); 
    }

}
