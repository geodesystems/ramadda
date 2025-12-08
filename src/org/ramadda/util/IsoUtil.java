/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;

import org.w3c.dom.*;
import ucar.unidata.xml.XmlUtil;
import java.text.SimpleDateFormat;
import java.util.Date;

public class IsoUtil {

    public static final String METADATA_STANDARD_NAME =
        "ISO 19115 Geographic Information - Metadata First Edition";

    public static final String METADATA_STANDARD_VERSION = "ISO 19115:2003";

    public static final String HEADER =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";

    public static final String XMLNS_GCO = "http://www.isotc211.org/2005/gco";

    public static final String XMLNS_GMD = "http://www.isotc211.org/2005/gmd";

    public static final String XMLNS_GMI =
        "http://eden.ign.fr/xsd/isotc211/isofull/20090316/gmi/";

    public static final String XMLNS_GML = "http://www.opengis.net/gml";

    public static final String XMLNS_GTS = "http://www.isotc211.org/2005/gts";

    public static final String XMLNS_XSI =
        "http://www.w3.org/2001/XMLSchema-instance";

    public static final String TAG_GMI_MI_METADATA = "gmi:MI_Metadata";

    public static final String TAG_GMD_LANGUAGE = "gmd:language";

    public static final String TAG_GCO_CHARACTERSTRING =
        "gco:CharacterString";

    public static final String TAG_GMD_CHARACTERSET = "gmd:characterSet";

    public static final String TAG_GMD_MD_CHARACTERSETCODE =
        "gmd:MD_CharacterSetCode";

    public static final String TAG_GMD_HIERARCHYLEVEL = "gmd:hierarchyLevel";

    public static final String TAG_GMD_MD_SCOPECODE = "gmd:MD_ScopeCode";

    public static final String TAG_GMD_HIERARCHYLEVELNAME =
        "gmd:hierarchyLevelName";

    public static final String TAG_GMD_CONTACT = "gmd:contact";

    public static final String TAG_GMD_CI_RESPONSIBLEPARTY =
        "gmd:CI_ResponsibleParty";

    public static final String TAG_GMD_INDIVIDUALNAME = "gmd:individualName";

    public static final String TAG_GMD_ORGANISATIONNAME =
        "gmd:organisationName";

    public static final String TAG_GMD_POSITIONNAME = "gmd:positionName";

    public static final String TAG_GMD_CONTACTINFO = "gmd:contactInfo";

    public static final String TAG_GMD_CI_CONTACT = "gmd:CI_Contact";

    public static final String TAG_GMD_PHONE = "gmd:phone";

    public static final String TAG_GMD_CI_TELEPHONE = "gmd:CI_Telephone";

    public static final String TAG_GMD_VOICE = "gmd:voice";

    public static final String TAG_GMD_ADDRESS = "gmd:address";

    public static final String TAG_GMD_CI_ADDRESS = "gmd:CI_Address";

    public static final String TAG_GMD_DELIVERYPOINT = "gmd:deliveryPoint";

    public static final String TAG_GMD_CITY = "gmd:city";

    public static final String TAG_GMD_ADMINISTRATIVEAREA =
        "gmd:administrativeArea";

    public static final String TAG_GMD_POSTALCODE = "gmd:postalCode";

    public static final String TAG_GMD_COUNTRY = "gmd:country";

    public static final String TAG_GMD_ELECTRONICMAILADDRESS =
        "gmd:electronicMailAddress";

    public static final String TAG_GMD_ONLINERESOURCE = "gmd:onlineResource";

    public static final String TAG_GMD_CI_ONLINERESOURCE =
        "gmd:CI_OnlineResource";

    public static final String TAG_GMD_LINKAGE = "gmd:linkage";

    public static final String TAG_GMD_URL = "gmd:URL";

    public static final String TAG_GMD_ROLE = "gmd:role";

    public static final String TAG_GMD_CI_ROLECODE = "gmd:CI_RoleCode";

    public static final String TAG_GMD_DATESTAMP = "gmd:dateStamp";

    public static final String TAG_GCO_DATE = "gco:Date";

    public static final String TAG_GCO_DATETIME = "gco:DateTime";

    public static final String TAG_GMD_METADATASTANDARDNAME =
        "gmd:metadataStandardName";

    public static final String TAG_GMD_METADATASTANDARDVERSION =
        "gmd:metadataStandardVersion";

    public static final String TAG_GMD_DATASETURI = "gmd:dataSetURI";

    public static final String TAG_GMD_IDENTIFICATIONINFO =
        "gmd:identificationInfo";

    public static final String TAG_GMD_MD_DATAIDENTIFICATION =
        "gmd:MD_DataIdentification";

    public static final String TAG_GMD_CITATION = "gmd:citation";

    public static final String TAG_GMD_CI_CITATION = "gmd:CI_Citation";

    public static final String TAG_GMD_TITLE = "gmd:title";

    public static final String TAG_GMD_DATE = "gmd:date";

    public static final String TAG_GMD_CI_DATE = "gmd:CI_Date";

    public static final String TAG_GMD_DATETYPE = "gmd:dateType";

    public static final String TAG_GMD_CI_DATETYPECODE =
        "gmd:CI_DateTypeCode";

    public static final String TAG_GMD_EDITIONDATE = "gmd:editionDate";

    public static final String TAG_GMD_IDENTIFIER = "gmd:identifier";

    public static final String TAG_GMD_MD_IDENTIFIER = "gmd:MD_Identifier";

    public static final String TAG_GMD_CODE = "gmd:code";

    public static final String TAG_GMD_CITEDRESPONSIBLEPARTY =
        "gmd:citedResponsibleParty";

    public static final String TAG_GMD_ABSTRACT = "gmd:abstract";

    public static final String TAG_GMD_PURPOSE = "gmd:purpose";

    public static final String TAG_GMD_CREDIT = "gmd:credit";

    public static final String TAG_GMD_STATUS = "gmd:status";

    public static final String TAG_GMD_MD_PROGRESSCODE =
        "gmd:MD_ProgressCode";

    public static final String TAG_GMD_POINTOFCONTACT = "gmd:pointOfContact";

    public static final String TAG_GMD_RESOURCEMAINTENANCE =
        "gmd:resourceMaintenance";

    public static final String TAG_GMD_MD_MAINTENANCEINFORMATION =
        "gmd:MD_MaintenanceInformation";

    public static final String TAG_GMD_MAINTENANCEANDUPDATEFREQUENCY =
        "gmd:maintenanceAndUpdateFrequency";

    public static final String TAG_GMD_MD_MAINTENANCEFREQUENCYCODE =
        "gmd:MD_MaintenanceFrequencyCode";

    public static final String TAG_GMD_UPDATESCOPE = "gmd:updateScope";

    public static final String TAG_GMD_GRAPHICOVERVIEW =
        "gmd:graphicOverview";

    public static final String TAG_GMD_MD_BROWSEGRAPHIC =
        "gmd:MD_BrowseGraphic";

    public static final String TAG_GMD_FILENAME = "gmd:fileName";

    public static final String TAG_GMD_FILEDESCRIPTION =
        "gmd:fileDescription";

    public static final String TAG_GMD_FILETYPE = "gmd:fileType";

    public static final String TAG_GMD_DESCRIPTIVEKEYWORDS =
        "gmd:descriptiveKeywords";

    public static final String TAG_GMD_MD_KEYWORDS = "gmd:MD_Keywords";

    public static final String TAG_GMD_KEYWORD = "gmd:keyword";

    public static final String TAG_GMD_TYPE = "gmd:type";

    public static final String TAG_GMD_MD_KEYWORDTYPECODE =
        "gmd:MD_KeywordTypeCode";

    public static final String TAG_GMD_THESAURUSNAME = "gmd:thesaurusName";

    public static final String TAG_GMD_RESOURCECONSTRAINTS =
        "gmd:resourceConstraints";

    public static final String TAG_GMD_MD_LEGALCONSTRAINTS =
        "gmd:MD_LegalConstraints";

    public static final String TAG_GMD_ACCESSCONSTRAINTS =
        "gmd:accessConstraints";

    public static final String TAG_GMD_MD_RESTRICTIONCODE =
        "gmd:MD_RestrictionCode";

    public static final String TAG_GMD_USECONSTRAINTS = "gmd:useConstraints";

    public static final String TAG_GMD_OTHERCONSTRAINTS =
        "gmd:otherConstraints";

    public static final String TAG_GMD_TOPICCATEGORY = "gmd:topicCategory";

    public static final String TAG_GMD_MD_TOPICCATEGORYCODE =
        "gmd:MD_TopicCategoryCode";

    public static final String TAG_GMD_EXTENT = "gmd:extent";

    public static final String TAG_GMD_EX_EXTENT = "gmd:EX_Extent";

    public static final String TAG_GMD_GEOGRAPHICELEMENT =
        "gmd:geographicElement";

    public static final String TAG_GMD_EX_GEOGRAPHICBOUNDINGBOX =
        "gmd:EX_GeographicBoundingBox";

    public static final String TAG_GMD_WESTBOUNDLONGITUDE =
        "gmd:westBoundLongitude";

    public static final String TAG_GCO_DECIMAL = "gco:Decimal";

    public static final String TAG_GMD_EASTBOUNDLONGITUDE =
        "gmd:eastBoundLongitude";

    public static final String TAG_GMD_SOUTHBOUNDLATITUDE =
        "gmd:southBoundLatitude";

    public static final String TAG_GMD_NORTHBOUNDLATITUDE =
        "gmd:northBoundLatitude";

    public static final String TAG_GMD_TEMPORALELEMENT =
        "gmd:temporalElement";

    public static final String TAG_GMD_EX_TEMPORALEXTENT =
        "gmd:EX_TemporalExtent";

    public static final String TAG_GMD_TIMEPERIOD = "gmd:TimePeriod";

    public static final String TAG_GMD_BEGINPOSITION = "gmd:beginPosition";

    public static final String TAG_GMD_ENDPOSITION = "gmd:endPosition";

    public static final String TAG_GMD_DISTRIBUTIONINFO =
        "gmd:distributionInfo";

    public static final String TAG_GMD_MD_DISTRIBUTION =
        "gmd:MD_Distribution";

    public static final String TAG_GMD_DISTRIBUTIONFORMAT =
        "gmd:distributionFormat";

    public static final String TAG_GMD_MD_FORMAT = "gmd:MD_Format";

    public static final String TAG_GMD_NAME = "gmd:name";

    public static final String TAG_GMD_VERSION = "gmd:version";

    public static final String TAG_GMD_SPECIFICATION = "gmd:specification";

    public static final String TAG_GMD_DISTRIBUTOR = "gmd:distributor";

    public static final String TAG_GMD_MD_DISTRIBUTOR = "gmd:MD_Distributor";

    public static final String TAG_GMD_DISTRIBUTORCONTACT =
        "gmd:distributorContact";

    public static final String TAG_GMD_TRANSFEROPTIONS =
        "gmd:transferOptions";

    public static final String TAG_GMD_MD_DIGITALTRANSFEROPTIONS =
        "gmd:MD_DigitalTransferOptions";

    public static final String TAG_GMD_ONLINE = "gmd:onLine";

    public static final String TAG_GMD_DESCRIPTION = "gmd:description";

    public static final String TAG_GMD_FUNCTION = "gmd:function";

    public static final String TAG_GMD_CI_ONLINEFUNCTIONCODE =
        "gmd:CI_OnLineFunctionCode";

    public static final String TAG_GMD_OFFLINE = "gmd:offLine";

    public static final String TAG_GMD_MD_MEDIUM = "gmd:MD_Medium";

    public static final String TAG_GMD_MD_MEDIUMNAMECODE =
        "gmd:MD_MediumNameCode";

    public static final String TAG_GMD_DENSITY = "gmd:density";

    public static final String TAG_GCO_REAL = "gco:Real";

    public static final String TAG_GMD_DENSITYUNITS = "gmd:densityUnits";

    public static final String TAG_GMD_MEDIUMNOTE = "gmd:mediumNote";

    public static final String TAG_GMI_ACQUISITIONINFORMATION =
        "gmi:acquisitionInformation";

    public static final String TAG_GMI_MI_ACQUISITIONINFORMATION =
        "gmi:MI_AcquisitionInformation";

    public static final String TAG_GMI_INSTRUMENT = "gmi:instrument";

    public static final String TAG_GMI_MI_INSTRUMENT = "gmi:MI_Instrument";

    public static final String TAG_GMI_IDENTIFIER = "gmi:identifier";

    public static final String TAG_GMI_TYPE = "gmi:type";

    public static final String TAG_GMI_PLATFORM = "gmi:platform";

    public static final String TAG_GMI_MI_PLATFORM = "gmi:MI_Platform";

    public static final String TAG_GMI_DESCRIPTION = "gmi:description";

    public static final String ATTR_VERSION = "version";

    public static final String ATTR_XMLNS_GCO = "xmlns:gco";

    public static final String ATTR_XMLNS_GMD = "xmlns:gmd";

    public static final String ATTR_XMLNS_GMI = "xmlns:gmi";

    public static final String ATTR_XMLNS_GML = "xmlns:gml";

    public static final String ATTR_XMLNS_GTS = "xmlns:gts";

    public static final String ATTR_XMLNS_XSI = "xmlns:xsi";

    public static final String ATTR_CODELIST = "codeList";

    public static final String ATTR_CODELISTVALUE = "codeListValue";

    public static final String ATTR_ID = "id";

    /*        <gmi:MI_Metadata xmlns:gco="http://www.isotc211.org/2005/gco" xmlns:gmd="http://www.isotc211.org/2005/gmd" xmlns:gml="http://www.opengis.net/gml" xmlns:gts="http://www.isotc211.org/2005/gts" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:gmi="http://eden.ign.fr/xsd/isotc211/isofull/20090316/gmi/" version="1.0">

     */

    public static Element makeRoot() throws Exception {
        return XmlUtil.getRoot(makeRootTag()
                               + XmlUtil.closeTag(TAG_GMI_MI_METADATA));
    }

    public static String makeRootTag() {
        return XmlUtil.openTag(TAG_GMI_MI_METADATA,
                               XmlUtil.attrs(new String[] {
            ATTR_XMLNS_GCO, XMLNS_GCO, ATTR_XMLNS_GMD, XMLNS_GMD,
            ATTR_XMLNS_GML, XMLNS_GML, ATTR_XMLNS_GTS, XMLNS_GTS,
            ATTR_XMLNS_XSI, XMLNS_XSI, ATTR_XMLNS_GMI, XMLNS_GMI,
            ATTR_VERSION, "1.0"
        }));
    }

    public static String format(Date date) throws Exception {
        return Utils.formatIso(date);
    }

    public static void addDateStamp(Element parent, Date date)
            throws Exception {
        Element dateStamp = XmlUtil.create(TAG_GMD_DATESTAMP, parent);
        Element dateTag = XmlUtil.create(TAG_GCO_DATETIME, dateStamp,
                                         format(date));
    }

    public static void addMetadataStandardTag(Element parent)
            throws Exception {
        addTextTag(parent, TAG_GMD_METADATASTANDARDNAME,
                   METADATA_STANDARD_NAME);
        addTextTag(parent, TAG_GMD_METADATASTANDARDVERSION,
                   METADATA_STANDARD_VERSION);

    }

    public static void xaddCharacterTag(Element parent, String contents)
            throws Exception {
        Element node = XmlUtil.create(TAG_GCO_CHARACTERSTRING, parent,
                                      (String) null);
        XmlUtil.createCDataNode(node, contents);
    }

    public static String makeCharacterTag(String contents) {
        return XmlUtil.tag(TAG_GCO_CHARACTERSTRING,
                           XmlUtil.getCdata(contents));
    }

    public static Element addTextTag(Element parent, String tagName,
                                     String contents)
            throws Exception {
        Element node = XmlUtil.create(tagName, parent, (String) null);
        addCharacterTag(node, contents);

        return node;
    }

    public static void addCharacterTag(Element parent, String contents)
            throws Exception {
        Element node = XmlUtil.create(TAG_GCO_CHARACTERSTRING, parent,
                                      (String) null);

        node.appendChild(XmlUtil.makeCDataNode(node.getOwnerDocument(),
                contents, false));
    }

}
