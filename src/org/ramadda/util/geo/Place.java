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

package org.ramadda.util.geo;

import org.ramadda.util.Bounds;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
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
public class Place {

    /** _more_ */
    private static final Object MUTEX = new Object();

    /** _more_ */
    public static final String RESOURCE_ROOT =
        "/org/ramadda/repository/resources/geo";


    /** _more_ */
    public static final Resource[] RESOURCES = {
        //name,id,fips,lat,lon,opt state index,suffix
        new Resource(RESOURCE_ROOT + "/alllocations.txt", new int[] { 0, 0, 0,
								      1, 2 }, ""),
        new Resource(RESOURCE_ROOT + "/countries.txt", new int[] { 3, 0, -1,
								   1, 2, }, ""),
        new Resource(RESOURCE_ROOT + "/states.txt", new int[] { 1, 0, 2, 3,
								4, }, "",5),
	new Resource(RESOURCE_ROOT + "/counties.txt", new int[] {
		3, 1, 1, 10, 11, -1, 0
	    }, "",4),
	new Resource(RESOURCE_ROOT + "/subdivisions.txt", new int[] {
		3, 1, 1, 11, 12, -1, 0
	    }, ""),
        //new Resource(RESOURCE_ROOT +"/districts.txt",new int[]{},""),
        new Resource(RESOURCE_ROOT + "/places.txt", new int[] {
		3, 1, 1, 12, 13, -1, 0
	    }, ""),
        //name,id,fips,lat,lon,opt state index,suffix
        new Resource(RESOURCE_ROOT + "/cities.txt", new int[] {
		1, 1, 0, 3, 4, 2, 2
	    }, ""),
        //#GEOID        NAME    UATYPE  POP10   HU10    ALAND   AWATER  ALAND_SQMI      AWATER_SQMI     INTPTLAT        INTPTLONG
        //        new Resource(RESOURCE_ROOT + "/urbanareas.txt", new int[] { 1, 0, 0,
        //                9, 10 }, "urban:"),
        //#GEOID        POP10   HU10    ALAND   AWATER  ALAND_SQMI      AWATER_SQMI     INTPTLAT        INTPTLONG
        new Resource(RESOURCE_ROOT + "/zipcodes.txt", new int[] { 0, 0, 0, 3,
								  4 }, "zip:"),
        //#USPS GEOID   POP10   HU10    ALAND   AWATER  ALAND_SQMI      AWATER_SQMI     INTPTLAT        INTPTLONG
        new Resource(RESOURCE_ROOT + "/tracts.txt", new int[] { 1, 1, 1, 8,
								9 }, "")
    };

    //        { 0, 1, 0, 7, 8 }

    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Jan 30, '18
     * @author         Enter your name here...
     */
    public static class Resource {

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
        List<Place> places = new ArrayList<Place>();
	
	int populationIndex;

	int population = 0;

        /** _more_          */
        Hashtable<String, Place> map = new Hashtable<String, Place>();

	private boolean loaded = false;

        /**
         * _more_
         *
         * @param file _more_
         * @param indices _more_
         * @param prefix _more_
         */
        public Resource(String file, int[] indices, String prefix) {
	    this(file, indices, prefix,-1);
	}

        public Resource(String file, int[] indices, String prefix,int populationIndex) {	    
            this.base    = new File(file).getName();
            this.id      = IOUtil.stripExtension(this.base);
            this.file    = file;
            this.indices = indices;
            this.prefix  = prefix;
	    this.populationIndex = populationIndex;
	    System.err.println("P:" + prefix);
        }

	public void loadResource() throws Exception {
	    if(this.loaded) return;
	    Hashtable<String, Place> resourceMap =
		new Hashtable<String, Place>();
	    Place.resourceMap.put(this.id, this);
	    resourcePlacesMap.put(this.id, resourceMap);
	    BufferedReader br = new BufferedReader(
						   new InputStreamReader(
									 IO.getInputStream(
											   this.file,
											   Place.class)));
	    int    cnt     = 0;
	    String line    = null;
	    int[]  indices = this.indices;
	    while ((line = br.readLine()) != null) {
		cnt++;
		if (line.startsWith("#")) {
		    continue;
		}
		Place place = new Place();
		int suffix = (indices.length >= 7)
		    ? indices[6]
		    : -1;
		place.processLine(StringUtil.split(line, "\t",
						   false, false), indices[0], indices[1],
				  indices[2], indices[3], indices[4],
				  suffix,populationIndex);

		if ((indices.length >= 6) && (indices[5] >= 0)) {
		    place.fips = place.fips.substring(indices[5]);
		}
		if(place.getId()!=null) {
		    resourceMap.put(place.getId().toLowerCase(), place);
		}
		if(place.getFips()!=null)
		    resourceMap.put(place.getFips(), place);
		String key;
		key =  this.prefix + place.getFips();
		    /*
		    resourceMap.put(key, place);
		    resourceMap.put(key.toLowerCase(), place);
		    resourceMap.put(key.toUpperCase(), place);
		    placesMap.put(key, place);
		    placesMap.put(key.toLowerCase(), place);
		    placesMap.put(key.toUpperCase(), place);
		    */
		key  = this.prefix + place.getId();
		    /*
		    placesMap.put(key, place);
		    placesMap.put(key.toLowerCase(), place);
		    placesMap.put(key.toUpperCase(), place);
		    */
		key  =  this.prefix + place.getName();
		/*
		    placesMap.put(key, place);
		    placesMap.put(key.toLowerCase(), place);
		    placesMap.put(key.toUpperCase(), place);
		    */
		this.places.add(place);
		if(place.getFips()!=null)
		    this.map.put(place.getFips(), place);
		if(place.getId()!=null) {
		    this.map.put(place.getId().toLowerCase(), place);
		}
		if (place._name != null) {
		    this.map.put(place._name, place);
		    if(place.getSuffix()!=null) {
			this.map.put(place._name+"," +place.getSuffix().toLowerCase(), place);
		    }
		}
	    }
	    this.loaded = true;
	}


	public List<Place> getPlaces() {
	    return places;
	}

	static boolean printKeys = false;

        /**
         * _more_
         *
         * @param key _more_
         *
         * @return _more_
         */
        public Place getPlace(String key) {
            key = key.toLowerCase();
	    if(printKeys) {
		for (Enumeration keys = this.map.keys(); keys.hasMoreElements(); ) {
		    String k = (String) keys.nextElement();
		    System.out.println("key:" + k+":");
		}
	    }
	    printKeys = false;
	    //	    System.out.println("try:" + key);
            return this.map.get(key);
        }

	public void debug() {
	    for (Enumeration keys = this.map.keys(); keys.hasMoreElements(); ) {
		String k = (String) keys.nextElement();
		System.out.println("key:" + k+":");
	    }
	}



    }


    /** _more_ */
    private static List<Place> allPlaces;

    /** _more_ */
    private static Hashtable<String, Place> placesMap = new Hashtable<String,
	Place>();

    /** _more_ */
    private static Hashtable<String, Resource> resourceMap =
        new Hashtable<String, Resource>();


    /** _more_ */
    private static Hashtable<String, Hashtable<String, Place>> resourcePlacesMap =
        new Hashtable<String, Hashtable<String, Place>>();



    /** _more_ */
    private static String name;

    /** _more_          */
    private  static String lcname;

    /** _more_ */
    private  static String suffix;

    /** _more_          */
    private  static String lcsuffix;

    /** _more_ */
    private  static String _name;

    /** _more_ */
    private  static String id;

    /** _more_ */
    private  static String fips;

    /** _more_ */
    private  static double latitude = Double.NaN;

    /** _more_ */
    private  static double longitude = Double.NaN;

    private  static int population;

    /**
     * _more_
     */
    public Place() {
	cnt++;
    }

    static int cnt = 0;

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
     * _more_
     *
     * @param toks _more_
     * @param nameIndex _more_
     * @param idIndex _more_
     * @param fipsIndex _more_
     * @param latIndex _more_
     * @param lonIndex _more_
     * @param suffixIndex _more_
     */
    private void processLine(List<String> toks, int nameIndex, int idIndex,
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
	if(popIndex>0) 
	    setPopulation(new Integer(toks.get(popIndex).trim()));
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
        name  = value;
        _name = name.toLowerCase();
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
    public String getLowerCaseName() {
        if (lcname == null) {
            lcname = name.toLowerCase();
        }

        return lcname;
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
    public String getLowerCaseSuffix() {
        if ((lcsuffix == null) && (suffix != null)) {
            lcsuffix = suffix.toLowerCase();
        }

        return lcsuffix;
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
       Set the Population property.

       @param value The new value for Population
    **/
    public void setPopulation (int value) {
	population = value;
    }

    /**
       Get the Population property.

       @return The Population
    **/
    public int getPopulation () {
	return population;
    }




    /**
     * _more_
     *
     * @param places _more_
     * @param key _more_
     *
     * @return _more_
     */
    public static Place findPlace(List<Place> places, String key) {
        //        if(true) return null;
        key = key.toLowerCase();
        for (Place place : places) {
            if (key.equals(place._name)) {
                return place;
            }
        }

        return null;
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
    public static Resource getResource(String id) throws Exception {
        getPlaces(id);
        return resourceMap.get(id);
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
    public static List<Place> getPlacesForResource(String id)
	throws Exception {
        Resource resource = getResource(id);
        if (resource == null) {
            return null;
        }

        return resource.places;
    }



    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static List<Place> getPlaces() throws Exception {
        return getPlaces(null);
    }



    /**
     * _more_
     *
     * @param resourceId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static List<Place> getPlaces(String resourceId) throws Exception {
	synchronized (MUTEX) {
	    if(resourceId!=null) {
		Resource resource = Place.resourceMap.get(resourceId);
		if(resource!=null) return resource.places;
	    } else {
		if(allPlaces!=null) return allPlaces;
		allPlaces = new ArrayList<Place>();
	    }
	    for (int i = 0; i < RESOURCES.length; i++) {
		Resource resource = RESOURCES[i];
		if ((resourceId != null)
		    && !resource.id.equals(resourceId)) {
		    continue;
		}
		resource.loadResource();
		if (resourceId != null) {
		    return resource.places;
		}
		allPlaces.addAll(resource.places);
	    }
	}
	return allPlaces;
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
        return getPlace(id, null);
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
        getPlaces("alllocations");
        List<Place> result = new ArrayList<Place>();
        s = s.toLowerCase();

        for (Place place : allPlaces) {
            if (place._name == null) {
                continue;
            }
            if ((bounds != null)
		&& !bounds.contains(place.getLatitude(),
				    place.getLongitude())) {
                continue;
            }
            boolean match = (startsWith
                             ? place._name.startsWith(s)
                             : place._name.indexOf(s) >= 0);
            if (match) {
                result.add(place);
                if (result.size() > max) {
                    break;
                }
            }
        }

        return result;
    }

    /**
     * _more_
     *
     * @param id _more_
     * @param resource _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Place getPlace(String id, String resource)
	throws Exception {
        getPlaces(resource);
        Place place = placesMap.get(id);
        if (place != null) {
            return place;
        }
        id    = id.toLowerCase();
        place = placesMap.get(id);
        if (place != null) {
            return place;
        }
        place = placesMap.get("zip:" + id);
        if (place != null) {
            return place;
        }

        place = placesMap.get("urban:" + id);
        if (place != null) {
            return place;
        }


        return place;

    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
	for(int i=0;i<10;i++) {
	    allPlaces = null;
	    placesMap = new Hashtable<String,Place>();
	    resourceMap =   new Hashtable<String, Resource>();
	    resourcePlacesMap =    new Hashtable<String, Hashtable<String, Place>>();
	    for(Resource resource: RESOURCES) resource.loaded = false;
	    cnt = 0;
	    Runtime.getRuntime().gc();
	    int mem1 = Utils.getUsedMemory();
	    List<Place> places = new ArrayList<Place>();
	    for(int j=0;j<332689;j++) {
		//		Place place = new Place("some long name"+j,0,0);
		//		places.add(place);
	    }
	    Place.getPlace("test");			
	    Runtime.getRuntime().gc();
	    int mem2 = Utils.getUsedMemory();
	    System.err.println("#:" + cnt+" mem:" + (mem2-mem1));
	}
	if(true) return;


        System.err.println(search("boulder", 50, null, false));
        if (true) {
            return;
        }
        List<Place> places = getPlacesForResource((args.length > 0)
						  ? args[0]
						  : null);
        //        List<Place> places = getPlaces();
        if (places == null) {
            System.err.println("no resource:");


            return;
        }
        System.out.println("#name,lat,lon");
        for (Place place : places) {
            if (place.fips != null) {
                String label = place.name;
                if (place.suffix != null) {
                    label += "," + place.suffix;
                }
                System.out.println(label + "\t" + place.latitude + "\t"
                                   + place.longitude);
            }
        }
    }

}
