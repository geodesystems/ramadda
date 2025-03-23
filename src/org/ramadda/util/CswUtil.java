/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import java.text.DateFormat;

import java.text.SimpleDateFormat;

import java.util.Date;


/**
 *
 * @author Jeff McWhirter
 */

public class CswUtil {


    /*

    endpoint = 'http://www.ngdc.noaa.gov/geoportal/csw' # NGDC Geoportal

        #endpoint = 'http://www.nodc.noaa.gov/geoportal/csw'   # NODC Geoportal: granule level
        #endpoint = 'http://data.nodc.noaa.gov/geoportal/csw'  # NODC Geoportal: collection level
    #endpoint = 'http://geodiscover.cgdi.ca/wes/serviceManagerCSW/csw'  # NRCAN CUSTOM
    #endpoint = 'http://geoport.whoi.edu/gi-cat/services/cswiso' # USGS Woods Hole GI_CAT
    #endpoint = 'http://cida.usgs.gov/gdp/geonetwork/srv/en/csw' # USGS CIDA Geonetwork
    #endpoint = 'http://cmgds.marine.usgs.gov/geonetwork/srv/en/csw' # USGS Coastal and Marine Program
    #endpoint = 'http://geoport.whoi.edu/geoportal/csw' # USGS Woods Hole Geoportal
    #endpoint = 'http://geo.gov.ckan.org/csw'  # CKAN testing site for new Data.gov
    #endpoint = 'https://edg.epa.gov/metadata/csw'  # EPA
    #endpoint = 'http://cwic.csiss.gmu.edu/cwicv1/discovery' */




    /** _more_ */
    public static final String TAG_CSW_RECORD = "csw:Record";

    /** _more_ */
    public static final String TAG_DCT_ABSTRACT = "dct:abstract";

    /** _more_ */
    public static final String TAG_DCT_MODIFIED = "dct:modified";

    /** _more_ */
    public static final String TAG_DC_CONTRIBUTOR = "dc:contributor";

    /** _more_ */
    public static final String TAG_DC_CREATOR = "dc:creator";

    /** _more_ */
    public static final String TAG_DC_FORMAT = "dc:format";

    /** _more_ */
    public static final String TAG_DC_IDENTIFIER = "dc:identifier";

    /** _more_ */
    public static final String TAG_DC_LANGUAGE = "dc:language";

    /** _more_ */
    public static final String TAG_DC_PUBLISHER = "dc:publisher";

    /** _more_ */
    public static final String TAG_DC_RELATION = "dc:relation";

    /** _more_ */
    public static final String TAG_DC_RIGHTS = "dc:rights";

    /** _more_ */
    public static final String TAG_DC_SOURCE = "dc:source";

    /** _more_ */
    public static final String TAG_DC_SUBJECT = "dc:subject";

    /** _more_ */
    public static final String TAG_DC_TITLE = "dc:title";

    /** _more_ */
    public static final String TAG_DC_TYPE = "dc:type";

    /** _more_ */
    public static final String TAG_OWS_BOUNDINGBOX = "ows:BoundingBox";

    /** _more_ */
    public static final String TAG_OWS_LOWERCORNER = "ows:LowerCorner";

    /** _more_ */
    public static final String TAG_OWS_UPPERCORNER = "ows:UpperCorner";

    /** _more_ */
    public static final String TAG_OWS_WGS84BOUNDINGBOX =
        "ows:WGS84BoundingBox";

    /** _more_ */
    public static final String TAG_RECORD = "Record";

    /** _more_ */
    public static final String XMLNS_XMLNS =
        "http://www.opengis.net/cat/csw/2.0.2";

    /** _more_ */
    public static final String XMLNS_XMLNS_CSW =
        "http://www.opengis.net/cat/csw/2.0.2";

    /** _more_ */
    public static final String XMLNS_XMLNS_DC =
        "http://purl.org/dc/elements/1.1/";

    /** _more_ */
    public static final String XMLNS_XMLNS_DCT = "http://purl.org/dc/terms/";

    /** _more_ */
    public static final String XMLNS_XMLNS_OWS = "http://www.opengis.net/ows";

    /** _more_ */
    public static final String XMLNS_XMLNS_XSI =
        "http://www.w3.org/2001/XMLSchema-instance";


}
