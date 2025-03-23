/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import org.w3c.dom.*;

import ucar.unidata.xml.XmlUtil;

import java.text.SimpleDateFormat;

import java.util.Date;


/**
 */
public class IsoUtil {

    /** _more_ */
    public static final String METADATA_STANDARD_NAME =
        "ISO 19115 Geographic Information - Metadata First Edition";

    /** _more_ */
    public static final String METADATA_STANDARD_VERSION = "ISO 19115:2003";


    /** _more_ */
    public static final String HEADER =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";

    /** _more_ */
    public static final String XMLNS_GCO = "http://www.isotc211.org/2005/gco";

    /** _more_ */
    public static final String XMLNS_GMD = "http://www.isotc211.org/2005/gmd";

    /** _more_ */
    public static final String XMLNS_GMI =
        "http://eden.ign.fr/xsd/isotc211/isofull/20090316/gmi/";

    /** _more_ */
    public static final String XMLNS_GML = "http://www.opengis.net/gml";

    /** _more_ */
    public static final String XMLNS_GTS = "http://www.isotc211.org/2005/gts";

    /** _more_ */
    public static final String XMLNS_XSI =
        "http://www.w3.org/2001/XMLSchema-instance";

    /** _more_ */
    public static final String TAG_GMI_MI_METADATA = "gmi:MI_Metadata";

    /** _more_ */
    public static final String TAG_GMD_LANGUAGE = "gmd:language";

    /** _more_ */
    public static final String TAG_GCO_CHARACTERSTRING =
        "gco:CharacterString";

    /** _more_ */
    public static final String TAG_GMD_CHARACTERSET = "gmd:characterSet";

    /** _more_ */
    public static final String TAG_GMD_MD_CHARACTERSETCODE =
        "gmd:MD_CharacterSetCode";

    /** _more_ */
    public static final String TAG_GMD_HIERARCHYLEVEL = "gmd:hierarchyLevel";

    /** _more_ */
    public static final String TAG_GMD_MD_SCOPECODE = "gmd:MD_ScopeCode";

    /** _more_ */
    public static final String TAG_GMD_HIERARCHYLEVELNAME =
        "gmd:hierarchyLevelName";

    /** _more_ */
    public static final String TAG_GMD_CONTACT = "gmd:contact";

    /** _more_ */
    public static final String TAG_GMD_CI_RESPONSIBLEPARTY =
        "gmd:CI_ResponsibleParty";

    /** _more_ */
    public static final String TAG_GMD_INDIVIDUALNAME = "gmd:individualName";

    /** _more_ */
    public static final String TAG_GMD_ORGANISATIONNAME =
        "gmd:organisationName";

    /** _more_ */
    public static final String TAG_GMD_POSITIONNAME = "gmd:positionName";

    /** _more_ */
    public static final String TAG_GMD_CONTACTINFO = "gmd:contactInfo";

    /** _more_ */
    public static final String TAG_GMD_CI_CONTACT = "gmd:CI_Contact";

    /** _more_ */
    public static final String TAG_GMD_PHONE = "gmd:phone";

    /** _more_ */
    public static final String TAG_GMD_CI_TELEPHONE = "gmd:CI_Telephone";

    /** _more_ */
    public static final String TAG_GMD_VOICE = "gmd:voice";

    /** _more_ */
    public static final String TAG_GMD_ADDRESS = "gmd:address";

    /** _more_ */
    public static final String TAG_GMD_CI_ADDRESS = "gmd:CI_Address";

    /** _more_ */
    public static final String TAG_GMD_DELIVERYPOINT = "gmd:deliveryPoint";

    /** _more_ */
    public static final String TAG_GMD_CITY = "gmd:city";

    /** _more_ */
    public static final String TAG_GMD_ADMINISTRATIVEAREA =
        "gmd:administrativeArea";

    /** _more_ */
    public static final String TAG_GMD_POSTALCODE = "gmd:postalCode";

    /** _more_ */
    public static final String TAG_GMD_COUNTRY = "gmd:country";

    /** _more_ */
    public static final String TAG_GMD_ELECTRONICMAILADDRESS =
        "gmd:electronicMailAddress";

    /** _more_ */
    public static final String TAG_GMD_ONLINERESOURCE = "gmd:onlineResource";

    /** _more_ */
    public static final String TAG_GMD_CI_ONLINERESOURCE =
        "gmd:CI_OnlineResource";

    /** _more_ */
    public static final String TAG_GMD_LINKAGE = "gmd:linkage";

    /** _more_ */
    public static final String TAG_GMD_URL = "gmd:URL";

    /** _more_ */
    public static final String TAG_GMD_ROLE = "gmd:role";

    /** _more_ */
    public static final String TAG_GMD_CI_ROLECODE = "gmd:CI_RoleCode";

    /** _more_ */
    public static final String TAG_GMD_DATESTAMP = "gmd:dateStamp";

    /** _more_ */
    public static final String TAG_GCO_DATE = "gco:Date";

    /** _more_ */
    public static final String TAG_GCO_DATETIME = "gco:DateTime";

    /** _more_ */
    public static final String TAG_GMD_METADATASTANDARDNAME =
        "gmd:metadataStandardName";

    /** _more_ */
    public static final String TAG_GMD_METADATASTANDARDVERSION =
        "gmd:metadataStandardVersion";

    /** _more_ */
    public static final String TAG_GMD_DATASETURI = "gmd:dataSetURI";

    /** _more_ */
    public static final String TAG_GMD_IDENTIFICATIONINFO =
        "gmd:identificationInfo";

    /** _more_ */
    public static final String TAG_GMD_MD_DATAIDENTIFICATION =
        "gmd:MD_DataIdentification";

    /** _more_ */
    public static final String TAG_GMD_CITATION = "gmd:citation";

    /** _more_ */
    public static final String TAG_GMD_CI_CITATION = "gmd:CI_Citation";

    /** _more_ */
    public static final String TAG_GMD_TITLE = "gmd:title";

    /** _more_ */
    public static final String TAG_GMD_DATE = "gmd:date";

    /** _more_ */
    public static final String TAG_GMD_CI_DATE = "gmd:CI_Date";

    /** _more_ */
    public static final String TAG_GMD_DATETYPE = "gmd:dateType";

    /** _more_ */
    public static final String TAG_GMD_CI_DATETYPECODE =
        "gmd:CI_DateTypeCode";

    /** _more_ */
    public static final String TAG_GMD_EDITIONDATE = "gmd:editionDate";

    /** _more_ */
    public static final String TAG_GMD_IDENTIFIER = "gmd:identifier";

    /** _more_ */
    public static final String TAG_GMD_MD_IDENTIFIER = "gmd:MD_Identifier";

    /** _more_ */
    public static final String TAG_GMD_CODE = "gmd:code";

    /** _more_ */
    public static final String TAG_GMD_CITEDRESPONSIBLEPARTY =
        "gmd:citedResponsibleParty";

    /** _more_ */
    public static final String TAG_GMD_ABSTRACT = "gmd:abstract";

    /** _more_ */
    public static final String TAG_GMD_PURPOSE = "gmd:purpose";

    /** _more_ */
    public static final String TAG_GMD_CREDIT = "gmd:credit";

    /** _more_ */
    public static final String TAG_GMD_STATUS = "gmd:status";

    /** _more_ */
    public static final String TAG_GMD_MD_PROGRESSCODE =
        "gmd:MD_ProgressCode";

    /** _more_ */
    public static final String TAG_GMD_POINTOFCONTACT = "gmd:pointOfContact";

    /** _more_ */
    public static final String TAG_GMD_RESOURCEMAINTENANCE =
        "gmd:resourceMaintenance";

    /** _more_ */
    public static final String TAG_GMD_MD_MAINTENANCEINFORMATION =
        "gmd:MD_MaintenanceInformation";

    /** _more_ */
    public static final String TAG_GMD_MAINTENANCEANDUPDATEFREQUENCY =
        "gmd:maintenanceAndUpdateFrequency";

    /** _more_ */
    public static final String TAG_GMD_MD_MAINTENANCEFREQUENCYCODE =
        "gmd:MD_MaintenanceFrequencyCode";

    /** _more_ */
    public static final String TAG_GMD_UPDATESCOPE = "gmd:updateScope";

    /** _more_ */
    public static final String TAG_GMD_GRAPHICOVERVIEW =
        "gmd:graphicOverview";

    /** _more_ */
    public static final String TAG_GMD_MD_BROWSEGRAPHIC =
        "gmd:MD_BrowseGraphic";

    /** _more_ */
    public static final String TAG_GMD_FILENAME = "gmd:fileName";

    /** _more_ */
    public static final String TAG_GMD_FILEDESCRIPTION =
        "gmd:fileDescription";

    /** _more_ */
    public static final String TAG_GMD_FILETYPE = "gmd:fileType";

    /** _more_ */
    public static final String TAG_GMD_DESCRIPTIVEKEYWORDS =
        "gmd:descriptiveKeywords";

    /** _more_ */
    public static final String TAG_GMD_MD_KEYWORDS = "gmd:MD_Keywords";

    /** _more_ */
    public static final String TAG_GMD_KEYWORD = "gmd:keyword";

    /** _more_ */
    public static final String TAG_GMD_TYPE = "gmd:type";

    /** _more_ */
    public static final String TAG_GMD_MD_KEYWORDTYPECODE =
        "gmd:MD_KeywordTypeCode";

    /** _more_ */
    public static final String TAG_GMD_THESAURUSNAME = "gmd:thesaurusName";

    /** _more_ */
    public static final String TAG_GMD_RESOURCECONSTRAINTS =
        "gmd:resourceConstraints";

    /** _more_ */
    public static final String TAG_GMD_MD_LEGALCONSTRAINTS =
        "gmd:MD_LegalConstraints";

    /** _more_ */
    public static final String TAG_GMD_ACCESSCONSTRAINTS =
        "gmd:accessConstraints";

    /** _more_ */
    public static final String TAG_GMD_MD_RESTRICTIONCODE =
        "gmd:MD_RestrictionCode";

    /** _more_ */
    public static final String TAG_GMD_USECONSTRAINTS = "gmd:useConstraints";

    /** _more_ */
    public static final String TAG_GMD_OTHERCONSTRAINTS =
        "gmd:otherConstraints";

    /** _more_ */
    public static final String TAG_GMD_TOPICCATEGORY = "gmd:topicCategory";

    /** _more_ */
    public static final String TAG_GMD_MD_TOPICCATEGORYCODE =
        "gmd:MD_TopicCategoryCode";

    /** _more_ */
    public static final String TAG_GMD_EXTENT = "gmd:extent";

    /** _more_ */
    public static final String TAG_GMD_EX_EXTENT = "gmd:EX_Extent";

    /** _more_ */
    public static final String TAG_GMD_GEOGRAPHICELEMENT =
        "gmd:geographicElement";

    /** _more_ */
    public static final String TAG_GMD_EX_GEOGRAPHICBOUNDINGBOX =
        "gmd:EX_GeographicBoundingBox";

    /** _more_ */
    public static final String TAG_GMD_WESTBOUNDLONGITUDE =
        "gmd:westBoundLongitude";

    /** _more_ */
    public static final String TAG_GCO_DECIMAL = "gco:Decimal";

    /** _more_ */
    public static final String TAG_GMD_EASTBOUNDLONGITUDE =
        "gmd:eastBoundLongitude";

    /** _more_ */
    public static final String TAG_GMD_SOUTHBOUNDLATITUDE =
        "gmd:southBoundLatitude";

    /** _more_ */
    public static final String TAG_GMD_NORTHBOUNDLATITUDE =
        "gmd:northBoundLatitude";

    /** _more_ */
    public static final String TAG_GMD_TEMPORALELEMENT =
        "gmd:temporalElement";

    /** _more_ */
    public static final String TAG_GMD_EX_TEMPORALEXTENT =
        "gmd:EX_TemporalExtent";

    /** _more_ */
    public static final String TAG_GMD_TIMEPERIOD = "gmd:TimePeriod";

    /** _more_ */
    public static final String TAG_GMD_BEGINPOSITION = "gmd:beginPosition";

    /** _more_ */
    public static final String TAG_GMD_ENDPOSITION = "gmd:endPosition";

    /** _more_ */
    public static final String TAG_GMD_DISTRIBUTIONINFO =
        "gmd:distributionInfo";

    /** _more_ */
    public static final String TAG_GMD_MD_DISTRIBUTION =
        "gmd:MD_Distribution";

    /** _more_ */
    public static final String TAG_GMD_DISTRIBUTIONFORMAT =
        "gmd:distributionFormat";

    /** _more_ */
    public static final String TAG_GMD_MD_FORMAT = "gmd:MD_Format";

    /** _more_ */
    public static final String TAG_GMD_NAME = "gmd:name";

    /** _more_ */
    public static final String TAG_GMD_VERSION = "gmd:version";

    /** _more_ */
    public static final String TAG_GMD_SPECIFICATION = "gmd:specification";

    /** _more_ */
    public static final String TAG_GMD_DISTRIBUTOR = "gmd:distributor";

    /** _more_ */
    public static final String TAG_GMD_MD_DISTRIBUTOR = "gmd:MD_Distributor";

    /** _more_ */
    public static final String TAG_GMD_DISTRIBUTORCONTACT =
        "gmd:distributorContact";

    /** _more_ */
    public static final String TAG_GMD_TRANSFEROPTIONS =
        "gmd:transferOptions";

    /** _more_ */
    public static final String TAG_GMD_MD_DIGITALTRANSFEROPTIONS =
        "gmd:MD_DigitalTransferOptions";

    /** _more_ */
    public static final String TAG_GMD_ONLINE = "gmd:onLine";

    /** _more_ */
    public static final String TAG_GMD_DESCRIPTION = "gmd:description";

    /** _more_ */
    public static final String TAG_GMD_FUNCTION = "gmd:function";

    /** _more_ */
    public static final String TAG_GMD_CI_ONLINEFUNCTIONCODE =
        "gmd:CI_OnLineFunctionCode";

    /** _more_ */
    public static final String TAG_GMD_OFFLINE = "gmd:offLine";

    /** _more_ */
    public static final String TAG_GMD_MD_MEDIUM = "gmd:MD_Medium";

    /** _more_ */
    public static final String TAG_GMD_MD_MEDIUMNAMECODE =
        "gmd:MD_MediumNameCode";

    /** _more_ */
    public static final String TAG_GMD_DENSITY = "gmd:density";

    /** _more_ */
    public static final String TAG_GCO_REAL = "gco:Real";

    /** _more_ */
    public static final String TAG_GMD_DENSITYUNITS = "gmd:densityUnits";

    /** _more_ */
    public static final String TAG_GMD_MEDIUMNOTE = "gmd:mediumNote";

    /** _more_ */
    public static final String TAG_GMI_ACQUISITIONINFORMATION =
        "gmi:acquisitionInformation";

    /** _more_ */
    public static final String TAG_GMI_MI_ACQUISITIONINFORMATION =
        "gmi:MI_AcquisitionInformation";

    /** _more_ */
    public static final String TAG_GMI_INSTRUMENT = "gmi:instrument";

    /** _more_ */
    public static final String TAG_GMI_MI_INSTRUMENT = "gmi:MI_Instrument";

    /** _more_ */
    public static final String TAG_GMI_IDENTIFIER = "gmi:identifier";

    /** _more_ */
    public static final String TAG_GMI_TYPE = "gmi:type";

    /** _more_ */
    public static final String TAG_GMI_PLATFORM = "gmi:platform";

    /** _more_ */
    public static final String TAG_GMI_MI_PLATFORM = "gmi:MI_Platform";

    /** _more_ */
    public static final String TAG_GMI_DESCRIPTION = "gmi:description";

    /** _more_ */
    public static final String ATTR_VERSION = "version";

    /** _more_ */
    public static final String ATTR_XMLNS_GCO = "xmlns:gco";

    /** _more_ */
    public static final String ATTR_XMLNS_GMD = "xmlns:gmd";

    /** _more_ */
    public static final String ATTR_XMLNS_GMI = "xmlns:gmi";

    /** _more_ */
    public static final String ATTR_XMLNS_GML = "xmlns:gml";

    /** _more_ */
    public static final String ATTR_XMLNS_GTS = "xmlns:gts";

    /** _more_ */
    public static final String ATTR_XMLNS_XSI = "xmlns:xsi";

    /** _more_ */
    public static final String ATTR_CODELIST = "codeList";

    /** _more_ */
    public static final String ATTR_CODELISTVALUE = "codeListValue";

    /** _more_ */
    public static final String ATTR_ID = "id";


    /*        <gmi:MI_Metadata xmlns:gco="http://www.isotc211.org/2005/gco" xmlns:gmd="http://www.isotc211.org/2005/gmd" xmlns:gml="http://www.opengis.net/gml" xmlns:gts="http://www.isotc211.org/2005/gts" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:gmi="http://eden.ign.fr/xsd/isotc211/isofull/20090316/gmi/" version="1.0">

     */



    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Element makeRoot() throws Exception {
        return XmlUtil.getRoot(makeRootTag()
                               + XmlUtil.closeTag(TAG_GMI_MI_METADATA));
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public static String makeRootTag() {
        return XmlUtil.openTag(TAG_GMI_MI_METADATA,
                               XmlUtil.attrs(new String[] {
            ATTR_XMLNS_GCO, XMLNS_GCO, ATTR_XMLNS_GMD, XMLNS_GMD,
            ATTR_XMLNS_GML, XMLNS_GML, ATTR_XMLNS_GTS, XMLNS_GTS,
            ATTR_XMLNS_XSI, XMLNS_XSI, ATTR_XMLNS_GMI, XMLNS_GMI,
            ATTR_VERSION, "1.0"
        }));
    }


    /**
     * _more_
     *
     * @param date _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String format(Date date) throws Exception {
        return Utils.formatIso(date);
    }




    /**
     * _more_
     *
     * @param parent _more_
     * @param date _more_
     *
     * @throws Exception _more_
     */
    public static void addDateStamp(Element parent, Date date)
            throws Exception {
        Element dateStamp = XmlUtil.create(TAG_GMD_DATESTAMP, parent);
        Element dateTag = XmlUtil.create(TAG_GCO_DATETIME, dateStamp,
                                         format(date));
    }




    /**
     * _more_
     *
     * @param parent _more_
     *
     * @throws Exception _more_
     */
    public static void addMetadataStandardTag(Element parent)
            throws Exception {
        addTextTag(parent, TAG_GMD_METADATASTANDARDNAME,
                   METADATA_STANDARD_NAME);
        addTextTag(parent, TAG_GMD_METADATASTANDARDVERSION,
                   METADATA_STANDARD_VERSION);

    }



    /**
     * _more_
     *
     * @param parent _more_
     * @param contents _more_
     *
     * @throws Exception _more_
     */
    public static void xaddCharacterTag(Element parent, String contents)
            throws Exception {
        Element node = XmlUtil.create(TAG_GCO_CHARACTERSTRING, parent,
                                      (String) null);
        XmlUtil.createCDataNode(node, contents);
    }




    /**
     * _more_
     *
     * @param contents _more_
     *
     * @return _more_
     */
    public static String makeCharacterTag(String contents) {
        return XmlUtil.tag(TAG_GCO_CHARACTERSTRING,
                           XmlUtil.getCdata(contents));
    }



    /**
     * _more_
     *
     * @param parent _more_
     * @param tagName _more_
     * @param contents _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Element addTextTag(Element parent, String tagName,
                                     String contents)
            throws Exception {
        Element node = XmlUtil.create(tagName, parent, (String) null);
        addCharacterTag(node, contents);

        return node;
    }


    /**
     * _more_
     *
     * @param parent _more_
     * @param contents _more_
     *
     * @throws Exception _more_
     */
    public static void addCharacterTag(Element parent, String contents)
            throws Exception {
        Element node = XmlUtil.create(TAG_GCO_CHARACTERSTRING, parent,
                                      (String) null);

        node.appendChild(XmlUtil.makeCDataNode(node.getOwnerDocument(),
                contents, false));
    }




}
