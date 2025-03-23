/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;




/**
 */
public class WfsUtil {

    /** _more_ */
    public static final String HEADER_ARGS = "";

    /** _more_ */
    public static final String ARG_SERVICE = "service";

    /** _more_ */
    public static final String ARG_VERSION = "version";

    /** _more_ */
    public static final String ARG_REQUEST = "request";

    /** _more_ */
    public static final String ARG_TYPENAME = "typeName";


    /** _more_ */
    public static final String VERSION = "1.1.0";

    /** _more_ */
    public static final String REQUEST_GETCAPABILITIES = "GetCapabilities";

    /** _more_ */
    public static final String REQUEST_DESCRIBEFEATURETYPE =
        "DescribeFeatureType";




    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Oct 15, '13
     * @author         Enter your name here...
     */
    public static class Cap {

        /** _more_ */
        public static final String XMLNS_XMLNS_GML =
            "http://www.opengis.net/gml";

        /** _more_ */
        public static final String XMLNS_XMLNS_OGC =
            "http://www.opengis.net/ogc";

        /** _more_ */
        public static final String XMLNS_XMLNS_OWS =
            "http://www.opengis.net/ows";

        /** _more_ */
        public static final String XMLNS_XMLNS_WFS =
            "http://www.opengis.net/wfs";

        /** _more_ */
        public static final String XMLNS_XMLNS_XLINK =
            "http://www.w3.org/1999/xlink";

        /** _more_ */
        public static final String XMLNS_XMLNS_XSI =
            "http://www.w3.org/2001/XMLSchema-instance";

        /** _more_ */
        public static final String TAG_WFS_WFS_CAPABILITIES =
            "wfs:WFS_Capabilities";

        /** _more_ */
        public static final String TAG_OWS_SERVICEIDENTIFICATION =
            "ows:ServiceIdentification";

        /** _more_ */
        public static final String TAG_OWS_TITLE = "ows:Title";

        /** _more_ */
        public static final String TAG_OWS_ABSTRACT = "ows:Abstract";

        /** _more_ */
        public static final String TAG_OWS_KEYWORDS = "ows:Keywords";

        /** _more_ */
        public static final String TAG_OWS_KEYWORD = "ows:Keyword";

        /** _more_ */
        public static final String TAG_OWS_SERVICETYPE = "ows:ServiceType";

        /** _more_ */
        public static final String TAG_OWS_SERVICETYPEVERSION =
            "ows:ServiceTypeVersion";

        /** _more_ */
        public static final String TAG_OWS_FEES = "ows:Fees";

        /** _more_ */
        public static final String TAG_OWS_ACCESSCONSTRAINTS =
            "ows:AccessConstraints";

        /** _more_ */
        public static final String TAG_OWS_SERVICEPROVIDER =
            "ows:ServiceProvider";

        /** _more_ */
        public static final String TAG_OWS_PROVIDERNAME = "ows:ProviderName";

        /** _more_ */
        public static final String TAG_OWS_SERVICECONTACT =
            "ows:ServiceContact";

        /** _more_ */
        public static final String TAG_OWS_INDIVIDUALNAME =
            "ows:IndividualName";

        /** _more_ */
        public static final String TAG_OWS_POSITIONNAME = "ows:PositionName";

        /** _more_ */
        public static final String TAG_OWS_CONTACTINFO = "ows:ContactInfo";

        /** _more_ */
        public static final String TAG_OWS_PHONE = "ows:Phone";

        /** _more_ */
        public static final String TAG_OWS_VOICE = "ows:Voice";

        /** _more_ */
        public static final String TAG_OWS_FACSIMILE = "ows:Facsimile";

        /** _more_ */
        public static final String TAG_OWS_ADDRESS = "ows:Address";

        /** _more_ */
        public static final String TAG_OWS_DELIVERYPOINT =
            "ows:DeliveryPoint";

        /** _more_ */
        public static final String TAG_OWS_CITY = "ows:City";

        /** _more_ */
        public static final String TAG_OWS_ADMINISTRATIVEAREA =
            "ows:AdministrativeArea";

        /** _more_ */
        public static final String TAG_OWS_POSTALCODE = "ows:PostalCode";

        /** _more_ */
        public static final String TAG_OWS_COUNTRY = "ows:Country";

        /** _more_ */
        public static final String TAG_OWS_ELECTRONICMAILADDRESS =
            "ows:ElectronicMailAddress";

        /** _more_ */
        public static final String TAG_OWS_HOURSOFSERVICE =
            "ows:HoursOfService";

        /** _more_ */
        public static final String TAG_OWS_CONTACTINSTRUCTIONS =
            "ows:ContactInstructions";

        /** _more_ */
        public static final String TAG_OWS_OPERATIONSMETADATA =
            "ows:OperationsMetadata";

        /** _more_ */
        public static final String TAG_OWS_OPERATION = "ows:Operation";

        /** _more_ */
        public static final String TAG_OWS_DCP = "ows:DCP";

        /** _more_ */
        public static final String TAG_OWS_HTTP = "ows:HTTP";

        /** _more_ */
        public static final String TAG_OWS_GET = "ows:Get";

        /** _more_ */
        public static final String TAG_OWS_POST = "ows:Post";

        /** _more_ */
        public static final String TAG_OWS_PARAMETER = "ows:Parameter";

        /** _more_ */
        public static final String TAG_OWS_VALUE = "ows:Value";

        /** _more_ */
        public static final String TAG_OWS_EXTENDEDCAPABILITIES =
            "ows:ExtendedCapabilities";

        /** _more_ */
        public static final String TAG_OWS_CONSTRAINT = "ows:Constraint";

        /** _more_ */
        public static final String TAG_WFS_FEATURECOLLECTION =
            "wfs:FeatureCollection";

        /** _more_ */
        public static final String TAG_WFS_FEATURETYPELIST =
            "wfs:FeatureTypeList";

        /** _more_ */
        public static final String TAG_WFS_FEATURETYPE = "wfs:FeatureType";

        /** _more_ */
        public static final String TAG_WFS_NAME = "wfs:Name";

        /** _more_ */
        public static final String TAG_WFS_TITLE = "wfs:Title";

        /** _more_ */
        public static final String TAG_WFS_DEFAULTSRS = "wfs:DefaultSRS";

        /** _more_ */
        public static final String TAG_WFS_OUTPUTFORMATS =
            "wfs:OutputFormats";

        /** _more_ */
        public static final String TAG_WFS_FORMAT = "wfs:Format";

        /** _more_ */
        public static final String TAG_OWS_WGS84BOUNDINGBOX =
            "ows:WGS84BoundingBox";

        /** _more_ */
        public static final String TAG_OWS_LOWERCORNER = "ows:LowerCorner";

        /** _more_ */
        public static final String TAG_OWS_UPPERCORNER = "ows:UpperCorner";

        /** _more_ */
        public static final String TAG_OGC_FILTER_CAPABILITIES =
            "ogc:Filter_Capabilities";

        /** _more_ */
        public static final String TAG_OGC_SPATIAL_CAPABILITIES =
            "ogc:Spatial_Capabilities";

        /** _more_ */
        public static final String TAG_OGC_GEOMETRYOPERANDS =
            "ogc:GeometryOperands";

        /** _more_ */
        public static final String TAG_OGC_GEOMETRYOPERAND =
            "ogc:GeometryOperand";

        /** _more_ */
        public static final String TAG_OGC_SPATIALOPERATORS =
            "ogc:SpatialOperators";

        /** _more_ */
        public static final String TAG_OGC_SPATIALOPERATOR =
            "ogc:SpatialOperator";

        /** _more_ */
        public static final String TAG_OGC_SCALAR_CAPABILITIES =
            "ogc:Scalar_Capabilities";

        /** _more_ */
        public static final String TAG_OGC_LOGICALOPERATORS =
            "ogc:LogicalOperators";

        /** _more_ */
        public static final String TAG_OGC_COMPARISONOPERATORS =
            "ogc:ComparisonOperators";

        /** _more_ */
        public static final String TAG_OGC_COMPARISONOPERATOR =
            "ogc:ComparisonOperator";

        /** _more_ */
        public static final String TAG_OGC_ID_CAPABILITIES =
            "ogc:Id_Capabilities";

        /** _more_ */
        public static final String TAG_OGC_EID = "ogc:EID";

        /** _more_ */
        public static final String TAG_OGC_FID = "ogc:FID";

        /** _more_ */
        public static final String ATTR_VERSION = "version";

        /** _more_ */
        public static final String ATTR_NAME = "name";
    }

}
