/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.geo;



import org.ramadda.util.JsonUtil;
import org.ramadda.util.IO;

import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;

import ucar.unidata.gis.GisPart;
import ucar.unidata.gis.shapefile.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;


/**
 * Class to hold a Collection of Features.  Modeled off the GEOJson spec.
 */
@SuppressWarnings("unchecked")
public class FeatureCollection {

    /** FeatureCollection type */
    public final static String TYPE_FEATURE_COLLECTION = "FeatureCollection";

    /** Feature type */
    public final static String TYPE_FEATURE = "Feature";

    /** Collection name */
    private String name = null;

    /** Collection description */
    private String description = null;

    /** List of Features */
    private List<Feature> features = null;

    /** properties */
    private Hashtable<String, Object> properties = new Hashtable();

    /** Schema property */
    public static final String PROP_SCHEMA = "Schema";

    /** Schema name property */
    public static final String PROP_SCHEMANAME = "SchemaName";

    /** Schema data property */
    public static final String PROP_SCHEMADATA = "SchemaData";

    /** Schema Id property */
    public static final String PROP_SCHEMAID = "SchemaId";

    /** _more_ */
    public static final String PROP_STYLEID = "StyleId";

    /** _more_ */
    public static final String PROP_BALLOON_TEMPLATE = "BalloonTemplate";

    /** _more_ */
    private Properties fieldProperties;

    /** _more_ */
    private List<DbaseDataWrapper> fieldDatum;


    /** _more_ */
    private static TTLCache<String, FeatureCollection> cache =
        new TTLCache<String, FeatureCollection>(60 * 1000 * 5);


    /**
     
     *
     * @param features _more_
     */
    public FeatureCollection(List<Feature> features) {
        this.features = features;
    }


    /**
     * Create a FeatureCollection
     */
    public FeatureCollection() {
        this("", null);
    }

    /**
     * Create a FeatureCollection
     *
     * @param name  the name
     * @param description the description
     */
    public FeatureCollection(String name, String description) {
        this(name, description, new Hashtable<String, Object>(), null);
        this.name        = name;
        this.description = description;
    }

    /**
     * Create a FeatureCollection
     *
     * @param name   the name
     * @param description  the description
     * @param properties  the properties
     * @param fieldDatum _more_
     */
    public FeatureCollection(String name, String description,
                             Hashtable<String, Object> properties,
                             List<DbaseDataWrapper> fieldDatum) {
        this.name        = name;
        this.description = description;
        this.properties  = properties;
        this.fieldDatum  = fieldDatum;
    }



    /**
     * _more_
     *
     * @param fieldDatum _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static DbaseDataWrapper getNameField(
            List<DbaseDataWrapper> fieldDatum)
            throws Exception {
        DbaseDataWrapper nameField = null;
        if (fieldDatum != null) {
            int firstChar = -1;
            for (int i = 0; i < fieldDatum.size(); i++) {
                DbaseDataWrapper dbd = fieldDatum.get(i);
                if (dbd.getType() == DbaseData.TYPE_CHAR) {
                    if (firstChar < 0) {
                        firstChar = i;
                    }
                    if (dbd.getName().toLowerCase().indexOf("name") >= 0) {
                        if (nameField == null) {
                            nameField = dbd;
                        }
                    }
                }
            }
            if ((nameField == null) && (firstChar >= 0)) {
                nameField = fieldDatum.get(firstChar);
            }
        }

        return nameField;
    }

    /**
     * _more_
     *
     * @param path _more_
     * @param is _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static FeatureCollection getFeatureCollection(String path,
            InputStream is)
            throws Exception {
        FeatureCollection fc = cache.get(path);
        if ((fc == null) && (is != null)) {
            if (path.toLowerCase().endsWith("json")) {
                fc = new FeatureCollection(GeoJson.getFeatures(path));
            } else {
                fc = makeFeatureCollection(is);
            }
            cache.put(path, fc);
        }

        return fc;
    }




    /**
     * _more_
     *
     * @param file _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static FeatureCollection makeFeatureCollection(File file)
            throws Exception {
        return makeFeatureCollection(new FileInputStream(file));
    }


    /**
     * _more_
     *
     * @param file _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static FeatureCollection makeFeatureCollection(InputStream file)
            throws Exception {
        Hashtable extraProps = new Hashtable();
        Hashtable<String, Object> collectionProps = new Hashtable<String,
                                                        Object>();
        EsriShapefile shapefile = new EsriShapefile(file, null, 0.0f);

        return makeFeatureCollection("", "", shapefile, extraProps,
                                     collectionProps);
    }


    /**
     * _more_
     *
     * @param fcname _more_
     * @param desc _more_
     * @param shapefile _more_
     * @param extraProps _more_
     * @param collectionProps _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static FeatureCollection makeFeatureCollection(String fcname,
            String desc, EsriShapefile shapefile, Hashtable extraProps,
            Hashtable<String, Object> collectionProps)
            throws Exception {

        DbaseFile dbfile = shapefile.getDbFile();
        if (dbfile != null) {
            collectionProps.put("dbfile", dbfile);
        }
        collectionProps.put("shapefile", shapefile);

        List<DbaseDataWrapper> fieldDatum =
            FeatureCollection.getDatum(dbfile, extraProps, collectionProps);
        DbaseDataWrapper nameField = getNameField(fieldDatum);
        collectionProps.put(FeatureCollection.PROP_SCHEMA,
                            DbaseDataWrapper.getSchema(fieldDatum));

        List features = shapefile.getFeatures();
        FeatureCollection fc = new FeatureCollection(fcname, desc,
                                   collectionProps, fieldDatum);
        List<Feature> fcfeatures = new ArrayList<Feature>(features.size());
        for (int i = 0; i < features.size(); i++) {
            EsriShapefile.EsriFeature gf =
                (EsriShapefile.EsriFeature) features.get(i);
            String type = getGeometryType(gf, gf.getNumParts());
            if (type == null) {
                if (gf.getNumParts() != 0) {
                    System.out.println("Can't handle feature type "
                                       + gf.getClass().toString());
                }
                continue;
            }
            List<float[][]> parts =
                new ArrayList<float[][]>(gf.getNumParts());
            java.util.Iterator pi = gf.getGisParts();
            while (pi.hasNext()) {
                GisPart  gp = (GisPart) pi.next();
                double[] xx = gp.getX();
                double[] yy = gp.getY();
                // TODO:  Why are we down casting to floats?
                // pts is in x,y order
                float[][] pts = new float[2][xx.length];
                for (int ptIdx = 0; ptIdx < xx.length; ptIdx++) {
                    pts[0][ptIdx] = (float) xx[ptIdx];
                    pts[1][ptIdx] = (float) yy[ptIdx];
                }
                parts.add(pts);
            }
            Geometry geom = new Geometry(type, parts);
            String   name = "";
            if (nameField != null) {
                name = nameField.getString(i).trim();
            }
            Hashtable<String, Object> featureProps = new Hashtable<String,
                                                         Object>();
            if (fieldDatum != null) {
                String schemaName = (String) collectionProps.get(
                                        FeatureCollection.PROP_SCHEMANAME);
                String schemaId = (String) collectionProps.get(
                                      FeatureCollection.PROP_SCHEMAID);
                if (schemaName == null) {
                    schemaName = "";
                }
                if (schemaId == null) {
                    schemaId = "";
                }
                featureProps.put(FeatureCollection.PROP_SCHEMANAME,
                                 schemaName);
                featureProps.put(FeatureCollection.PROP_SCHEMAID, schemaId);
                LinkedHashMap<String, Object> schemaData =
                    new LinkedHashMap<String, Object>(fieldDatum.size());
                for (int j = 0; j < fieldDatum.size(); j++) {
                    // since shapefile parser makes no distinction between ints & doubles, this hack will fix that.
                    Object data = fieldDatum.get(j).getData(i);
                    if (data instanceof Double) {
                        double d = ((Double) data).doubleValue();
                        if ((int) d == d) {
                            data = Integer.valueOf((int) d);
                        }
                    }
                    schemaData.put(fieldDatum.get(j).getName(), data);
                }
                featureProps.put(FeatureCollection.PROP_SCHEMADATA,
                                 schemaData);
            }

            Feature feature = new Feature(name, geom, featureProps,
                                          collectionProps);
            if (fieldDatum != null) {
                Hashtable data = new Hashtable();
                for (DbaseDataWrapper ddw : fieldDatum) {
                    data.put(ddw.getName(), ddw.getData(i));
                }
                feature.setData(data);
            }
            fcfeatures.add(feature);
        }
        fc.setFeatures(fcfeatures);

        return fc;
    }


    /**
     * _more_
     *
     * @param dbfile _more_
     * @param properties _more_
     * @param pluginProperties _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static List<DbaseDataWrapper> getDatum(DbaseFile dbfile,
            Hashtable properties, Hashtable pluginProperties)
            throws Exception {
        if (dbfile == null) {
            return null;
        }

        List<DbaseDataWrapper> fieldDatum  = null;
        List<String>           extraFields = null;
        String                 extraKey    = (properties != null)
                                             ? (String) properties.get(
                                                 "map.key")
                                             : null;
        if (properties != null) {
            String fields = (String) properties.get("map.fields");
            if (fields != null) {
                extraFields = Utils.split(fields, ",");
            }
        }
        DbaseDataWrapper keyWrapper = null;
        String[]         fieldNames = dbfile.getFieldNames();
        fieldDatum = new ArrayList<DbaseDataWrapper>();
        Hashtable<String, DbaseDataWrapper> wrapperMap =
            new Hashtable<String, DbaseDataWrapper>();
        for (int j = 0; j < fieldNames.length; j++) {
            DbaseDataWrapper dbd =
                new DbaseDataWrapper(fieldNames[j].toLowerCase(),
                                     dbfile.getField(j), properties,
                                     pluginProperties);
            wrapperMap.put(dbd.getName(), dbd);
            if (properties != null) {
                String v = (String) properties.get("map." + dbd.getName()
                               + ".drop");
                if ((v != null) && v.equals("true")) {
                    continue;
                }
            }
            fieldDatum.add(dbd);
        }
        if (extraFields != null) {
            if (extraKey != null) {
                keyWrapper = wrapperMap.get(extraKey);
            }
            for (String extraField : extraFields) {
                DbaseDataWrapper dbd = new DbaseDataWrapper(extraField,
                                           keyWrapper, properties,
                                           pluginProperties);
                String combine = (String) properties.get("map." + extraField
                                     + ".combine");
                if (combine != null) {
                    List<DbaseDataWrapper> combineList = new ArrayList();
                    for (String id : Utils.split(combine, ",", true, true)) {
                        DbaseDataWrapper other = wrapperMap.get(id);
                        if (other != null) {
                            combineList.add(other);
                        }
                    }
                    dbd.setCombine(combineList);
                }
                fieldDatum.add(dbd);
                wrapperMap.put(dbd.getName(), dbd);
            }
        }

        return fieldDatum;
    }



    /**
     * Get the Geometry type corresponding the the EsriFeature
     * @param feature the feature
     * @param numParts _more_
     * @return the corresponding KML tag or null
     */
    public static String getGeometryType(EsriShapefile.EsriFeature feature,
                                         int numParts) {
        String tag = null;
        if (numParts == 0) {
            return tag;
        }
        if ((feature instanceof EsriShapefile.EsriPoint)
                || (feature instanceof EsriShapefile.EsriPointZ)) {
            if (numParts == 1) {
                tag = Geometry.TYPE_POINT;
            } else {
                tag = Geometry.TYPE_MULTIPOINT;
            }
        } else if ((feature instanceof EsriShapefile.EsriPolyline)
                   || (feature instanceof EsriShapefile.EsriPolylineZ)) {
            if (numParts == 1) {
                tag = Geometry.TYPE_LINESTRING;
            } else {
                tag = Geometry.TYPE_MULTILINESTRING;
            }
        } else if ((feature instanceof EsriShapefile.EsriPolygon)
                   || (feature instanceof EsriShapefile.EsriPolygonZ)) {
            if (numParts == 1) {
                tag = Geometry.TYPE_POLYGON;
            } else {
                tag = Geometry.TYPE_MULTIPOLYGON;
            }
        }

        return tag;
    }


    /**
     * Get the Geometry type corresponding the the EsriShapefileFeature
     *
     * @param featureType  the feature type
     * @return the corresponding KML tag or null
     */
    public static String getGeometryType(int featureType) {
        String tag = null;
        switch (featureType) {

          case EsriShapefile.POINT :      // 1
          case EsriShapefile.POINTZ :     // 11
              tag = Geometry.TYPE_POINT;

              break;

          case EsriShapefile.POLYLINE :   // 3
          case EsriShapefile.POLYLINEZ :  // 13
              tag = Geometry.TYPE_LINESTRING;

              break;

          case EsriShapefile.POLYGON :    // 5
          case EsriShapefile.POLYGONZ :   // 15
              tag = Geometry.TYPE_POLYGON;

              break;

          default :
              tag = null;

              break;
        }

        return tag;
    }




    /**
     * _more_
     *
     * @param lat _more_
     * @param lon _more_
     *
     * @return _more_
     */
    public Feature find(float lat, float lon) {
        for (Feature feature : features) {
            if (feature.contains(lat, lon)) {
                return feature;
            }
        }

        return null;
    }

    /**
     * Set the name
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the name
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the description
     *
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the description
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the features
     *
     * @param features the features
     */
    public void setFeatures(List<Feature> features) {
        this.features = features;
    }

    /**
     * Get the features
     *
     * @return the features
     */
    public List<Feature> getFeatures() {
        return features;
    }

    /**
     * Set the properties
     *
     * @param props  the properties
     */
    public void setProperties(Hashtable props) {
        this.properties = props;
    }

    /**
     * Get the properties
     *
     * @return the properties
     */
    public Hashtable getProperties() {
        return properties;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<DbaseDataWrapper> getDatum() {
        return fieldDatum;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public EsriShapefile getShapefile() {
        return (EsriShapefile) properties.get("shapefile");
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public DbaseFile getDbFile() {
        return (DbaseFile) properties.get("dbfile");
    }

    /**
     * _more_
     *
     *
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void toGeoJson(Appendable sb) throws Exception {
        List<String> map = new ArrayList<String>();
        sb.append(JsonUtil.mapOpen());
        JsonUtil.attr(sb, "type", JsonUtil.quote(TYPE_FEATURE_COLLECTION));
        sb.append(",\n");
        sb.append(JsonUtil.mapKey("features"));
        sb.append(JsonUtil.listOpen());
        List<String> flist = new ArrayList<String>();
        int          cnt   = 0;
        for (Feature feature : features) {
            if (cnt++ > 0) {
                sb.append(",\n");
            }
            feature.toGeoJson(sb);
        }
        sb.append(JsonUtil.listClose());
        sb.append(JsonUtil.mapClose());
    }

    public static void main(String[]args) throws Exception {
	for(String file:args) {
	    System.err.println("opening:" +file);
            FeatureCollection.getFeatureCollection(file, IO.getInputStream(file));
	    System.err.println("OK");
	}
    }

}
