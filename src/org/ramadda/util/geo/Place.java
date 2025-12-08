/**
Copyright (c) 2008-2026 Geode Systems LLC
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


public class Place implements Comparable<Place> {
    private static final Object MUTEX = new Object();
    private String name;
    private String suffix;
    private String id;
    private String fips;
    private double latitude = Double.NaN;
    private double longitude = Double.NaN;
    private int population;

    public Place() {}
    public Place(String name, double lat, double lon) {
        this();
        this.id = name;
        setName(name);
        this.latitude  = lat;
        this.longitude = lon;
    }

    public Place(String name, String lat, String lon) {
        this(name, Double.parseDouble(lat), Double.parseDouble(lon));
    }

    public int 	compareTo(Place o) {
	return name.compareTo(o.name);
    }

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

    public void setId(String value) {
        id = value;
    }

    public String getId() {
        return id;
    }

    public void setName(String value) {
        name = value;
    }

    public String getName() {
        return name;
    }

    public String getSuffix() {
        return suffix;
    }

    public String toString() {
        return " name:" + name + " id:" + id + " fips:" + fips + " lat:"
               + latitude + " lon:" + longitude;
    }

    public void setFips(String value) {
        fips = value;
    }

    public String getFips() {
        return fips;
    }

    public void setLatitude(double value) {
        latitude = value;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLongitude(double value) {
        longitude = value;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setPopulation(int value) {
        population = value;
    }

    public int getPopulation() {
        return population;
    }

    public static Place getPlace(String id) throws Exception {
        return GeoResource.getPlaceFromAll(id);
    }

    public boolean within(Bounds bounds) {
        if (bounds == null) {
            return true;
        }

        return bounds.contains(getLatitude(), getLongitude());
    }

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
