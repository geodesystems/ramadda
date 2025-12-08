/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;

public class WfsUtil {
    public static final String HEADER_ARGS = "";
    public static final String ARG_SERVICE = "service";
    public static final String ARG_VERSION = "version";
    public static final String ARG_REQUEST = "request";
    public static final String ARG_TYPENAME = "typeName";
    public static final String VERSION = "1.1.0";
    public static final String REQUEST_GETCAPABILITIES = "GetCapabilities";
    public static final String REQUEST_DESCRIBEFEATURETYPE =
        "DescribeFeatureType";

    public static class Cap {

        public static final String XMLNS_XMLNS_GML =
            "http://www.opengis.net/gml";

        public static final String XMLNS_XMLNS_OGC =
            "http://www.opengis.net/ogc";

        public static final String XMLNS_XMLNS_OWS =
            "http://www.opengis.net/ows";

        public static final String XMLNS_XMLNS_WFS =
            "http://www.opengis.net/wfs";

        public static final String XMLNS_XMLNS_XLINK =
            "http://www.w3.org/1999/xlink";

        public static final String XMLNS_XMLNS_XSI =
            "http://www.w3.org/2001/XMLSchema-instance";

        public static final String TAG_WFS_WFS_CAPABILITIES =
            "wfs:WFS_Capabilities";

        public static final String TAG_OWS_SERVICEIDENTIFICATION =
            "ows:ServiceIdentification";

        public static final String TAG_OWS_TITLE = "ows:Title";

        public static final String TAG_OWS_ABSTRACT = "ows:Abstract";

        public static final String TAG_OWS_KEYWORDS = "ows:Keywords";

        public static final String TAG_OWS_KEYWORD = "ows:Keyword";

        public static final String TAG_OWS_SERVICETYPE = "ows:ServiceType";

        public static final String TAG_OWS_SERVICETYPEVERSION =
            "ows:ServiceTypeVersion";

        public static final String TAG_OWS_FEES = "ows:Fees";

        public static final String TAG_OWS_ACCESSCONSTRAINTS =
            "ows:AccessConstraints";

        public static final String TAG_OWS_SERVICEPROVIDER =
            "ows:ServiceProvider";

        public static final String TAG_OWS_PROVIDERNAME = "ows:ProviderName";

        public static final String TAG_OWS_SERVICECONTACT =
            "ows:ServiceContact";

        public static final String TAG_OWS_INDIVIDUALNAME =
            "ows:IndividualName";

        public static final String TAG_OWS_POSITIONNAME = "ows:PositionName";

        public static final String TAG_OWS_CONTACTINFO = "ows:ContactInfo";

        public static final String TAG_OWS_PHONE = "ows:Phone";

        public static final String TAG_OWS_VOICE = "ows:Voice";

        public static final String TAG_OWS_FACSIMILE = "ows:Facsimile";

        public static final String TAG_OWS_ADDRESS = "ows:Address";

        public static final String TAG_OWS_DELIVERYPOINT =
            "ows:DeliveryPoint";

        public static final String TAG_OWS_CITY = "ows:City";

        public static final String TAG_OWS_ADMINISTRATIVEAREA =
            "ows:AdministrativeArea";

        public static final String TAG_OWS_POSTALCODE = "ows:PostalCode";

        public static final String TAG_OWS_COUNTRY = "ows:Country";

        public static final String TAG_OWS_ELECTRONICMAILADDRESS =
            "ows:ElectronicMailAddress";

        public static final String TAG_OWS_HOURSOFSERVICE =
            "ows:HoursOfService";

        public static final String TAG_OWS_CONTACTINSTRUCTIONS =
            "ows:ContactInstructions";

        public static final String TAG_OWS_OPERATIONSMETADATA =
            "ows:OperationsMetadata";

        public static final String TAG_OWS_OPERATION = "ows:Operation";

        public static final String TAG_OWS_DCP = "ows:DCP";

        public static final String TAG_OWS_HTTP = "ows:HTTP";

        public static final String TAG_OWS_GET = "ows:Get";

        public static final String TAG_OWS_POST = "ows:Post";

        public static final String TAG_OWS_PARAMETER = "ows:Parameter";

        public static final String TAG_OWS_VALUE = "ows:Value";

        public static final String TAG_OWS_EXTENDEDCAPABILITIES =
            "ows:ExtendedCapabilities";

        public static final String TAG_OWS_CONSTRAINT = "ows:Constraint";

        public static final String TAG_WFS_FEATURECOLLECTION =
            "wfs:FeatureCollection";

        public static final String TAG_WFS_FEATURETYPELIST =
            "wfs:FeatureTypeList";

        public static final String TAG_WFS_FEATURETYPE = "wfs:FeatureType";

        public static final String TAG_WFS_NAME = "wfs:Name";

        public static final String TAG_WFS_TITLE = "wfs:Title";

        public static final String TAG_WFS_DEFAULTSRS = "wfs:DefaultSRS";

        public static final String TAG_WFS_OUTPUTFORMATS =
            "wfs:OutputFormats";

        public static final String TAG_WFS_FORMAT = "wfs:Format";

        public static final String TAG_OWS_WGS84BOUNDINGBOX =
            "ows:WGS84BoundingBox";

        public static final String TAG_OWS_LOWERCORNER = "ows:LowerCorner";

        public static final String TAG_OWS_UPPERCORNER = "ows:UpperCorner";

        public static final String TAG_OGC_FILTER_CAPABILITIES =
            "ogc:Filter_Capabilities";

        public static final String TAG_OGC_SPATIAL_CAPABILITIES =
            "ogc:Spatial_Capabilities";

        public static final String TAG_OGC_GEOMETRYOPERANDS =
            "ogc:GeometryOperands";

        public static final String TAG_OGC_GEOMETRYOPERAND =
            "ogc:GeometryOperand";

        public static final String TAG_OGC_SPATIALOPERATORS =
            "ogc:SpatialOperators";

        public static final String TAG_OGC_SPATIALOPERATOR =
            "ogc:SpatialOperator";

        public static final String TAG_OGC_SCALAR_CAPABILITIES =
            "ogc:Scalar_Capabilities";

        public static final String TAG_OGC_LOGICALOPERATORS =
            "ogc:LogicalOperators";

        public static final String TAG_OGC_COMPARISONOPERATORS =
            "ogc:ComparisonOperators";

        public static final String TAG_OGC_COMPARISONOPERATOR =
            "ogc:ComparisonOperator";

        public static final String TAG_OGC_ID_CAPABILITIES =
            "ogc:Id_Capabilities";

        public static final String TAG_OGC_EID = "ogc:EID";

        public static final String TAG_OGC_FID = "ogc:FID";

        public static final String ATTR_VERSION = "version";

        public static final String ATTR_NAME = "name";
    }

}
