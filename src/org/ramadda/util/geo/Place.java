/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.geo;


import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;


import org.ramadda.util.geo.Bounds;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 *     Class description
 *
 *
 *     @version        $version$, Tue, Oct 27, '15
 *     @author         Enter your name here...
 */
public class Place implements Comparable<Place> {

    /** _more_ */
    private static final Object MUTEX = new Object();


    /** _more_ */
    private String name;

    /** _more_ */
    private String suffix;

    /** _more_ */
    private String id;

    /** _more_ */
    private String fips;

    /** _more_ */
    private double latitude = Double.NaN;

    /** _more_ */
    private double longitude = Double.NaN;

    /** _more_ */
    private int population;

    /**
     * _more_
     */
    public Place() {}


    /**
     * _more_
     *
     * @param name _more_
     * @param lat _more_
     * @param lon _more_
     */
    public Place(String name, double lat, double lon) {
        this();
        this.id = name;
        setName(name);
        this.latitude  = lat;
        this.longitude = lon;
    }


    /**
     *
     *
     * @param name _more_
     * @param lat _more_
     * @param lon _more_
     */
    public Place(String name, String lat, String lon) {
        this(name, Double.parseDouble(lat), Double.parseDouble(lon));
    }


    public int 	compareTo(Place o) {
	return name.compareTo(o.name);
    }

    /**
     * _more_
     *
     * @param toks _more_
     * @param nameIndex _more_
     * @param idIndex _more_
     * @param fipsIndex _more_
     * @param latIndex _more_
     * @param lonIndex _more_
     * @param suffixIndex _more_
     * @param popIndex _more_
     */
    public void processLine(List<String> toks, int nameIndex, int idIndex,
                            int fipsIndex, int latIndex, int lonIndex,
                            int suffixIndex, int popIndex) {
        //        System.err.println("line:" + toks.size() +"  " + toks);
        if (fipsIndex >= toks.size()) {
            return;
        }
        String fips = (fipsIndex < 0)
                      ? ""
                      : toks.get(fipsIndex);
        suffix = (suffixIndex >= 0)
                 ? toks.get(suffixIndex)
                 : null;
        setName(toks.get(nameIndex));
        setId(toks.get(idIndex));
        setFips(fips);
        setLatitude(Double.parseDouble(toks.get(latIndex).trim()));
        setLongitude(Double.parseDouble(toks.get(lonIndex).trim()));
        if (popIndex > 0) {
            setPopulation(Integer.parseInt(toks.get(popIndex).trim()));
        }
        //        System.out.println(fips);
    }


    /**
     *  Set the Id property.
     *
     *  @param value The new value for Id
     */
    public void setId(String value) {
        id = value;
    }

    /**
     *  Get the Id property.
     *
     *  @return The Id
     */
    public String getId() {
        return id;
    }



    /**
     *  Set the Name property.
     *
     *  @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     *  Get the Name property.
     *
     *  @return The Name
     */
    public String getName() {
        return name;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getSuffix() {
        return suffix;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return " name:" + name + " id:" + id + " fips:" + fips + " lat:"
               + latitude + " lon:" + longitude;
    }

    /**
     *  Set the Fips property.
     *
     *  @param value The new value for Fips
     */
    public void setFips(String value) {
        fips = value;
    }

    /**
     *  Get the Fips property.
     *
     *  @return The Fips
     */
    public String getFips() {
        return fips;
    }


    /**
     *  Set the Latitude property.
     *
     *  @param value The new value for Latitude
     */
    public void setLatitude(double value) {
        latitude = value;
    }

    /**
     *  Get the Latitude property.
     *
     *  @return The Latitude
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     *  Set the Longitude property.
     *
     *  @param value The new value for Longitude
     */
    public void setLongitude(double value) {
        longitude = value;
    }

    /**
     *  Get the Longitude property.
     *
     *  @return The Longitude
     */
    public double getLongitude() {
        return longitude;
    }


    /**
     *  Set the Population property.
     *
     *  @param value The new value for Population
     */
    public void setPopulation(int value) {
        population = value;
    }

    /**
     *  Get the Population property.
     *
     *  @return The Population
     */
    public int getPopulation() {
        return population;
    }



    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Place getPlace(String id) throws Exception {
        return GeoResource.getPlaceFromAll(id);
    }

    /**
     *
     * @param bounds _more_
     *  @return _more_
     */
    public boolean within(Bounds bounds) {
        if (bounds == null) {
            return true;
        }

        return bounds.contains(getLatitude(), getLongitude());
    }


    /**
     * _more_
     *
     * @param s _more_
     * @param max _more_
     * @param bounds _more_
     * @param startsWith _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static List<Place> search(String s, int max, Bounds bounds,
                                     boolean startsWith)
            throws Exception {
        List<Place> result = new ArrayList<Place>();
        s = s.toLowerCase();

        for (GeoResource resource : GeoResource.RESOURCES) {
            for (Place place : resource.getPlaces()) {
                if (place.getName() == null) {
                    continue;
                }
                if ((bounds != null)
                        && !bounds.contains(place.getLatitude(),
                                            place.getLongitude())) {
                    continue;
                }
                String  _name = place.getName().toLowerCase();
                boolean match = (startsWith
                                 ? _name.startsWith(s)
                                 : _name.indexOf(s) >= 0);
                if (match) {
                    result.add(place);
                    if (result.size() > max) {
                        break;
                    }
                }
            }

        }

        return result;
    }






}
