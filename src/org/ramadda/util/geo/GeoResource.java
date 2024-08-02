/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.geo;


import org.ramadda.util.IO;
import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;


/**
 *     Class description
 *
 *
 *     @version        $version$, Tue, Oct 27, '15
 *     @author         Enter your name here...
 */
@SuppressWarnings("unchecked")
public class GeoResource {

    /**  */
    private static boolean debugMemory = false;

    /** _more_ */
    public static final String RESOURCE_ROOT =
        "/org/ramadda/repository/resources/geo";

    //name,id,fips,lat,lon,opt state index,suffix

    /** _more_ */
    public static final GeoResource RESOURCE_TRACTS =
        new GeoResource(RESOURCE_ROOT + "/tracts.txt", new int[] { 1,
            1, 1, 8, 9 }, "");

    /** _more_ */
    public static final GeoResource RESOURCE_CITIES =
        new GeoResource(RESOURCE_ROOT + "/cities.txt", new int[] {
        1, 1, 0, 3, 4, 2, 2
    }, "");

    /** _more_ */
    public static final GeoResource RESOURCE_COUNTRIES =
        new GeoResource(RESOURCE_ROOT + "/countries.txt", new int[] { 3,
            0, -1, 1, 2, }, "");

    /** _more_ */
    public static final GeoResource RESOURCE_STATES =
        new GeoResource(RESOURCE_ROOT + "/states.txt", new int[] { 1,
            0, 2, 3, 4, }, "", 5);

    /** _more_ */
    public static final GeoResource RESOURCE_COUNTIES =
        new GeoResource(RESOURCE_ROOT + "/counties.txt", new int[] {
        3, 1, 1, 8, 9, -1, 0
    }, "", -1);

    /** _more_ */
    public static final GeoResource RESOURCE_SUBDIVISIONS =
        new GeoResource(RESOURCE_ROOT + "/subdivisions.txt", new int[] {
        3, 1, 1, 11, 12, -1, 0
    }, "");

    /** _more_ */
    public static final GeoResource RESOURCE_PLACES =
        new GeoResource(RESOURCE_ROOT + "/places.txt", new int[] {
        3, 1, 1, 12, 13, -1, 0
    }, "");

    //name,id,fips,lat,lon,opt state index,suffix
    /** _more_ */
    public static final GeoResource RESOURCE_CONGRESS =
        new GeoResource(RESOURCE_ROOT + "/uscongress.txt", new int[] { 1,
								    1, 1, 6, 7,0 }, "congress:");


    /** _more_ */
    public static final GeoResource RESOURCE_ZIPCODES =
        new GeoResource(RESOURCE_ROOT + "/zipcodes.txt", new int[] { 0,
            0, 0, 3, 4 }, "zip:");


    public static final GeoResource RESOURCE_ZCTA =
        new GeoResource(RESOURCE_ROOT + "/zcta.txt", new int[] { 0,
            0, 0, 5, 6 }, "zcta:");    

    /** _more_ */
    public static final GeoResource RESOURCE_ALLLOCATIONS =
        new GeoResource(RESOURCE_ROOT + "/alllocations.txt", new int[] { 0,
            0, 0, 1, 2 }, "");

    /** _more_ */
    public static final GeoResource[] RESOURCES = {
        RESOURCE_STATES, RESOURCE_COUNTIES, RESOURCE_CITIES, RESOURCE_COUNTRIES,
	RESOURCE_TRACTS, 
	RESOURCE_SUBDIVISIONS, RESOURCE_PLACES,
        RESOURCE_ZIPCODES, RESOURCE_ALLLOCATIONS,
    };



    /** _more_ */
    String id;

    /** _more_ */
    String base;

    /** _more_ */
    String file;
    //name,id,fips,lat,lon,opt state index

    /** _more_ */
    int[] indices;

    /** _more_ */
    String prefix;


    /** _more_ */
    int populationIndex;

    /** _more_ */
    int population = 0;


    /** _more_ */
    private static TTLCache<String, Hashtable<String, Place>> cache =
        new TTLCache<String, Hashtable<String, Place>>(1000 * 60 * 10);


    /**
     * _more_
     *
     * @param file _more_
     * @param indices _more_
     * @param prefix _more_
     */
    public GeoResource(String file, int[] indices, String prefix) {
        this(file, indices, prefix, -1);
    }

    /**
     * _more_
     *
     * @param file _more_
     * @param indices _more_
     * @param prefix _more_
     * @param populationIndex _more_
     */
    public GeoResource(String file, int[] indices, String prefix,
                       int populationIndex) {
        this.base            = new File(file).getName();
        this.id              = IOUtil.stripExtension(this.base);
        this.file            = file;
        this.indices         = indices;
        this.prefix          = prefix;
        this.populationIndex = populationIndex;
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
    public static Place getPlaceFromAll(String id) throws Exception {
        String _id = id.toLowerCase();
        for (GeoResource resource : RESOURCES) {
            Place place = resource.getPlace(id);
            if (place != null) {
                return place;
            }
            place = resource.getPlace(_id);
            if (place != null) {
                return place;
            }
        }

        return null;
    }

    /**
     * _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    private synchronized Hashtable<String, Place> init() {
        Hashtable<String, Place> placeMap = cache.get(this.id);
        if (placeMap != null) {
            return placeMap;
        }
        try {
            double mem1 = 0;
            placeMap = new Hashtable<String, Place>();
            if (debugMemory) {
                Runtime.getRuntime().gc();
                mem1 = Utils.getUsedMemory();
            }

            InputStream in = IO.getInputStream(this.file, GeoResource.class);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            int            cnt     = 0;
            String         line    = null;
            int[]          indices = this.indices;
            while ((line = br.readLine()) != null) {
                cnt++;
                if (line.startsWith("#")) {
                    continue;
                }
                Place place  = new Place();
                int   suffix = (indices.length >= 7)
                               ? indices[6]
                               : -1;
                place.processLine(StringUtil.split(line, "\t", false, false),
                                  indices[0], indices[1], indices[2],
                                  indices[3], indices[4], suffix,
                                  populationIndex);

                if ((indices.length >= 6) && (indices[5] >= 0)) {
                    place.setFips(place.getFips().substring(indices[5]));
                }
                if (place.getId() != null) {
                    placeMap.put(place.getId().toLowerCase(), place);
                }
                //                this.places.add(place);
                if (Utils.stringDefined(place.getFips())) {
                    placeMap.put(place.getFips(), place);
                }
                if (Utils.stringDefined(place.getId())) {
                    placeMap.put(place.getId().toLowerCase(), place);
                }
                if (Utils.stringDefined(place.getName())) {
                    String _name = place.getName().toLowerCase();
                    placeMap.put(_name, place);
                    if (Utils.stringDefined(place.getSuffix())) {
                        placeMap.put(
                            _name + "," + place.getSuffix().toLowerCase(),
                            place);
                    }
                }
                /*
                if(Utils.stringDefined(this.prefix)) {
                    String key;
                    key = this.prefix + place.getFips();
                    placeMap.put(key.toLowerCase(), place);
                    key = this.prefix + place.getId();
                    placeMap.put(key.toLowerCase(), place);
                    placeMap.put(key, place);
                    key = this.prefix + place.getName();
                    placeMap.put(key.toLowerCase(), place);
                }
                */

            }
            br = null;
            in.close();
            cache.put(this.id, placeMap);
            if (debugMemory) {
                Runtime.getRuntime().gc();
                double mem2 = Utils.getUsedMemory();
                System.err.println("GeoResource:" + id + " #:"
                                   + this.getPlaces().size() + " memory: "
                                   + Utils.decimals(mem2 - mem1, 1));
            }

            return placeMap;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    /**
     *
     * @return _more_
     */
    public Hashtable<String, Place> getPlaceMap() {
        return init();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<Place> getPlaces() {
        return (List<Place>) Utils.getValues(getPlaceMap());
    }

    /** _more_ */
    static boolean printKeys = false;

    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public Place getPlace(String key) {
        Hashtable<String, Place> placeMap = getPlaceMap();
        key = key.toLowerCase();
        if (printKeys) {
            for (Enumeration keys =
                    placeMap.keys(); keys.hasMoreElements(); ) {
                String k = (String) keys.nextElement();
                System.out.println("key:" + k + ":");
            }
        }
        printKeys = false;

        //          System.out.println("try:" + key);
        return placeMap.get(key);
    }

    /**
     * _more_
     */
    public void debug() {
        Hashtable<String, Place> placeMap = getPlaceMap();
        for (Enumeration keys = placeMap.keys(); keys.hasMoreElements(); ) {
            String k = (String) keys.nextElement();
            System.out.println("key:" + k + ":");
        }
    }



    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        debugMemory = true;
        for (int i = 0; i < 60; i++) {
            double      mem1   = Utils.getUsedMemory();
            List<Place> places = new ArrayList<Place>();
            System.err.println("testing");
            Place.getPlace("test");
            Runtime.getRuntime().gc();
            double mem2 = Utils.getUsedMemory();
            ucar.unidata.util.Misc.sleepSeconds(1);
            //            System.err.println("#:" + Place.cnt + " mem:" + (mem2 - mem1));
        }
        Utils.exitTest(0);
        System.err.println(Place.search("boulder", 50, null, false));
    }



}
