/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.services;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.Utils;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import org.w3c.dom.*;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public class OaiManager extends RepositoryManager {

    public static final String ARG_VERB = "verb";

    public static final String ARG_IDENTIFIER = "identifier";

    public static final String ARG_RESUMPTIONTOKEN = "resumptionToken";

    public static final String ARG_FROM = "from";

    public static final String ARG_UNTIL = "until";

    public static final String ARG_SET = "set";

    public static final String ARG_METADATAPREFIX = "metadataPrefix";

    private static final String[] ALLARGS = {
        ARG_VERB, ARG_IDENTIFIER, ARG_RESUMPTIONTOKEN, ARG_FROM, ARG_UNTIL,
        ARG_SET, ARG_METADATAPREFIX
    };

    private static final String[] formats = { "yyyy-MM-dd'T'HH:mm:ss Z",
            "yyyyMMdd'T'HHmmss Z" };

    private SimpleDateFormat[] parsers;

    private static HashSet argSet;

    private static HashSet verbSet;

    public static final String VERB_IDENTIFY = "Identify";

    public static final String VERB_LISTMETADATAFORMATS =
        "ListMetadataFormats";

    public static final String VERB_LISTSETS = "ListSets";

    public static final String VERB_LISTIDENTIFIERS = "ListIdentifiers";

    public static final String VERB_LISTRECORDS = "ListRecords";

    public static final String VERB_GETRECORD = "GetRecord";

    private static final String[] ALLVERBS = {
        VERB_IDENTIFY, VERB_LISTMETADATAFORMATS, VERB_LISTSETS,
        VERB_LISTIDENTIFIERS, VERB_LISTRECORDS, VERB_GETRECORD
    };

    public static final String ERROR_BADARGUMENT = "badArgument";

    public static final String ERROR_BADRESUMPTIONTOKEN =
        "badResumptionToken";

    public static final String ERROR_BADVERB = "badVerb";

    public static final String ERROR_CANNOTDISSEMINATEFORMAT =
        "cannotDisseminateFormat";

    public static final String ERROR_IDDOESNOTEXIST = "idDoesNotExist";

    public static final String ERROR_NORECORDSMATCH = "noRecordsMatch";

    public static final String ERROR_NOMETADATAFORMATS = "noMetaDataFormats";

    public static final String ERROR_NOSETHIERARCHY = "noSetHierarchy";

    public static final String TAG_OAI_PMH = "OAI-PMH";

    public static final String TAG_RESPONSEDATE = "responseDate";

    public static final String TAG_REQUEST = "request";

    public static final String TAG_IDENTIFY = "Identify";

    public static final String TAG_REPOSITORYNAME = "repositoryName";

    public static final String TAG_BASEURL = "baseURL";

    public static final String TAG_PROTOCOLVERSION = "protocolVersion";

    public static final String TAG_ADMINEMAIL = "adminEmail";

    public static final String TAG_EARLIESTDATESTAMP = "earliestDatestamp";

    public static final String TAG_DELETEDRECORD = "deletedRecord";

    public static final String TAG_GRANULARITY = "granularity";

    public static final String TAG_DESCRIPTION = "description";

    public static final String TAG_OAI_IDENTIFIER = "oai-identifier";

    public static final String TAG_SCHEME = "scheme";

    public static final String TAG_REPOSITORYIDENTIFIER =
        "repositoryIdentifier";

    public static final String TAG_DELIMITER = "delimiter";

    public static final String TAG_SAMPLEIDENTIFIER = "sampleIdentifier";

    public static final String TAG_ERROR = "error";

    public static final String TAG_LISTMETADATAFORMATS =
        "ListMetadataFormats";

    public static final String TAG_METADATAFORMAT = "metadataFormat";

    public static final String TAG_METADATAPREFIX = "metadataPrefix";

    public static final String TAG_SCHEMA = "schema";

    public static final String TAG_METADATANAMESPACE = "metadataNamespace";

    public static final String TAG_LISTIDENTIFIERS = "ListIdentifiers";

    public static final String TAG_LISTRECORDS = "ListRecords";

    public static final String TAG_DC_TITLE = "dc:title";

    public static final String TAG_DC_CREATOR = "dc:creator";

    public static final String TAG_DC_PUBLISHER = "dc:publisher";

    public static final String TAG_DC_SUBJECT = "dc:subject";

    public static final String TAG_DC_DESCRIPTION = "dc:description";

    public static final String TAG_DC_CONTRIBUTOR = "dc:contributor";

    public static final String TAG_DC_TYPE = "dc:type";

    public static final String TAG_DC_IDENTIFIER = "dc:identifier";

    public static final String TAG_DC_LANGUAGE = "dc:language";

    public static final String TAG_DC_RELATION = "dc:relation";

    public static final String TAG_GETRECORD = "GetRecord";

    public static final String TAG_RECORD = "record";

    public static final String TAG_HEADER = "header";

    public static final String TAG_IDENTIFIER = "identifier";

    public static final String TAG_DATESTAMP = "datestamp";

    public static final String TAG_METADATA = "metadata";

    public static final String TAG_OAIDC_DC = "oaidc:dc";

    public static final String TAG_RESUMPTIONTOKEN = "resumptionToken";

    public static final String ATTR_CODE = "code";

    public static final String ATTR_XMLNS = "xmlns";

    public static final String ATTR_XMLNS_DC = "xmlns:dc";

    public static final String ATTR_XMLNS_OAIDC = "xmlns:oaidc";

    public static final String ATTR_XMLNS_XSI = "xmlns:xsi";

    public static final String ATTR_VERB = "verb";

    public static final String ATTR_IDENTIFIER = "identifier";

    public static final String ATTR_METADATAPREFIX = "metadataPrefix";

    public static final String ATTR_XSI_SCHEMALOCATION = "xsi:schemaLocation";

    public static final String SCHEMA =
        "http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd";

    private SimpleDateFormat sdf;

    private String repositoryIdentifier;

    public OaiManager(Repository repository) {
        super(repository);
        sdf    = RepositoryUtil.makeDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        argSet = new HashSet();
        for (String arg : ALLARGS) {
            argSet.add(arg);
        }
        verbSet = new HashSet();
        for (String verb : ALLVERBS) {
            verbSet.add(verb);
        }

        SimpleDateFormat[] parsers = new SimpleDateFormat[formats.length];
        for (int i = 0; i < formats.length; i++) {
            parsers[i] = RepositoryUtil.makeDateFormat(formats[i]);
        }
    }

    private Element getRoot(Request request) throws Exception {
        Document doc  = XmlUtil.makeDocument();
        Element  root = XmlUtil.create(doc, TAG_OAI_PMH, (String[]) null);
        XmlUtil.setAttributes(root, new String[] {
            ATTR_XMLNS, "http://www.openarchives.org/OAI/2.0/",
            ATTR_XMLNS_XSI, "http://www.w3.org/2001/XMLSchema-instance",
            ATTR_XSI_SCHEMALOCATION,
            "http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd"
        });

        XmlUtil.create(TAG_RESPONSEDATE, root, format(new Date()));

        return root;
    }

    private void addRequest(Request request, Element root) throws Exception {
        String  url         =
            request.getAbsoluteUrl(request.getRequestPath());
        Element requestNode = XmlUtil.create(TAG_REQUEST, root, url);

        if ( !Misc.equals(request.getString(ARG_METADATAPREFIX, "oai_dc"),
                          "oai_dc")) {
            throw new MyException(ERROR_CANNOTDISSEMINATEFORMAT,
                                  "bad metadataPrefix argument");
        }

        if (request.defined(ARG_RESUMPTIONTOKEN)) {
            try {
                Integer.parseInt(request.getString(ARG_RESUMPTIONTOKEN,
						   ""));
            } catch (Exception exc) {
                throw new MyException(ERROR_BADRESUMPTIONTOKEN,
                                      "bad resumption token");
            }
        }

        Date fromDate  = null;
        Date untilDate = null;

        if (request.exists(ARG_FROM)) {
            fromDate = parseUTC(request.getString(ARG_FROM, ""));
            if (fromDate == null) {
                throw new IllegalArgumentException(
                    "could not parse from date");
            }
        }

        if (request.exists(ARG_UNTIL)) {
            untilDate = parseUTC(request.getString(ARG_UNTIL, ""));
            if (untilDate == null) {
                throw new IllegalArgumentException(
                    "could not parse until date");
            }
        }

        if ((fromDate != null) && (untilDate != null)) {
            if (fromDate.getTime() > untilDate.getTime()) {
                throw new IllegalArgumentException("from date > until date");
            }

        }

        if (request.exists(ARG_FROM) && request.exists(ARG_UNTIL)) {
            if (request.getString(ARG_FROM).length()
                    != request.getString(ARG_UNTIL).length()) {
                throw new IllegalArgumentException(
                    "different granularity of from and until arguments");
            }
        }

        for (Enumeration keys = request.getArgs().keys();
                keys.hasMoreElements(); ) {
            String key = (String) keys.nextElement();
            if (argSet.contains(key)) {
                String value = request.getString(key, "");
                requestNode.setAttribute(key, value);
            }
        }
    }

    public Result processRequest(Request request) throws Exception {
        Element root = getRoot(request);
        try {
            String verb = request.getString(ARG_VERB, VERB_IDENTIFY);
            //            if(verb.equals(VERB_GETRECORD)) {
            //                return new Result("",new StringBuffer(getRepository().getResource("/org/ramadda/repository/resources/test.xml")),"text/xml");
            //            }
            processRequestInner(request, root);
        } catch (MyException myexc) {
            handleError(request, root, myexc.code, myexc.toString());
        } catch (Exception exc) {
            handleError(request, root, ERROR_BADARGUMENT, exc.toString());
        }

        return makeResult(request, root);
    }

    private void processRequestInner(Request request, Element root)
            throws Exception {
        if ( !request.exists(ARG_VERB)) {
            handleError(request, root, ERROR_BADARGUMENT,
                        "'" + ARG_VERB + "' is missing");

            return;
        }
        String verb = request.getString(ARG_VERB, VERB_IDENTIFY);
        if ( !verbSet.contains(verb)) {
            //Add in an attributeless request node
            String url = request.getAbsoluteUrl(request.getRequestPath());
            XmlUtil.create(TAG_REQUEST, root, url);
            handleError(request, root, ERROR_BADVERB, "Bad verb:" + verb);

            return;
        }

        addRequest(request, root);

        for (Enumeration keys = request.getArgs().keys();
                keys.hasMoreElements(); ) {
            String key = (String) keys.nextElement();
            if ( !argSet.contains(key)) {
                handleError(request, root, ERROR_BADARGUMENT,
                            "Bad argument:" + key);

                return;
            }
            if (request.hasMultiples(key)) {
                handleError(request, root, ERROR_BADARGUMENT,
                            "Multiple arguments:" + key);

                return;
            }
        }

        if (verb.equals(VERB_IDENTIFY)) {
            handleIdentify(request, root);
        } else if (verb.equals(VERB_LISTMETADATAFORMATS)) {
            handleListMetadataformats(request, root);
        } else if (verb.equals(VERB_LISTSETS)) {
            handleListSets(request, root);
        } else if (verb.equals(VERB_LISTIDENTIFIERS)) {
            handleListIdentifiers(request, root);
        } else if (verb.equals(VERB_LISTRECORDS)) {
            handleListRecords(request, root);
        } else if (verb.equals(VERB_GETRECORD)) {
            handleGetRecord(request, root);
        } else {
            handleError(request, root, ERROR_BADVERB);
        }

    }

    private Result makeResult(Request request, Element root)
            throws Exception {
        return new Result("", new StringBuffer(XmlUtil.toString(root, true)),
                          "text/xml");
    }

    private void handleError(Request request, Element root, String code)
            throws Exception {
        handleError(request, root, code, null);
    }

    private void handleError(Request request, Element root, String code,
                             String contents)
            throws Exception {
        if (contents != null) {
            XmlUtil.create(TAG_ERROR, root, contents,
                           new String[] { ATTR_CODE,
                                          code });
        } else {
            XmlUtil.create(TAG_ERROR, root, new String[] { ATTR_CODE, code });
        }
    }

    private void handleIdentify(Request request, Element root)
            throws Exception {
        Element id = XmlUtil.create(TAG_IDENTIFY, root);
        XmlUtil.create(TAG_REPOSITORYNAME, id,
                       repository.getRepositoryName());
        String url = request.getAbsoluteUrl(request.getUrl());
        XmlUtil.create(TAG_BASEURL, id, url);
        XmlUtil.create(TAG_PROTOCOLVERSION, id, "2.0");
        XmlUtil.create(TAG_ADMINEMAIL, id,
                       getRepository().getProperty(PROP_ADMIN_EMAIL, ""));
        XmlUtil.create(TAG_EARLIESTDATESTAMP, id, "1990-01-01T00:00:00Z");
        XmlUtil.create(TAG_DELETEDRECORD, id, "no");
        XmlUtil.create(TAG_GRANULARITY, id, "YYYY-MM-DDThh:mm:ssZ");
        Element desc = XmlUtil.create(TAG_DESCRIPTION, id);
        Element oai  = XmlUtil.create(TAG_OAI_IDENTIFIER, desc, new String[] {
            ATTR_XMLNS, "http://www.openarchives.org/OAI/2.0/oai-identifier",
            ATTR_XMLNS_XSI, "http://www.w3.org/2001/XMLSchema-instance",
            ATTR_XSI_SCHEMALOCATION,
            "http://www.openarchives.org/OAI/2.0/oai-identifier  http://www.openarchives.org/OAI/2.0/oai-identifier.xsd"
        });

        XmlUtil.create(TAG_SCHEME, oai, "oai");
        XmlUtil.create(TAG_REPOSITORYIDENTIFIER, oai,
                       getRepositoryIdentifier());
        XmlUtil.create(TAG_DELIMITER, oai, ":");
        XmlUtil.create(TAG_SAMPLEIDENTIFIER, oai,
                       makeId(getEntryManager().getRootEntry().getId()));
    }

    private void handleListMetadataformats(Request request, Element root)
            throws Exception {
        if (request.exists(ARG_IDENTIFIER)) {
            String id    = getId(request.getString(ARG_IDENTIFIER, ""));
            Entry  entry = getEntryManager().getEntry(request, id);
            if (entry == null) {
                handleError(request, root, ERROR_IDDOESNOTEXIST);

                return;
            }
        }

        Element node = XmlUtil.create(TAG_LISTMETADATAFORMATS, root);
        Element fmt  = XmlUtil.create(TAG_METADATAFORMAT, node);
        XmlUtil.create(TAG_METADATAPREFIX, fmt, "oai_dc");
        XmlUtil.create(TAG_SCHEMA, fmt,
                       "http://www.openarchives.org/OAI/2.0/oai_dc.xsd");
        XmlUtil.create(TAG_METADATANAMESPACE, fmt,
                       "http://www.openarchives.org/OAI/2.0/oai_dc/");
    }

    private String getId(String id) {
        id = id.replace("oai:" + getRepositoryIdentifier() + ":", "");

        return id;
    }

    private String getRepositoryIdentifier() {
        if (repositoryIdentifier == null) {
            repositoryIdentifier = StringUtil.join(
                ".",
                Misc.reverseList(
                    Utils.split(
                        getRepository().getHostname(), ".", true, true)));
        }

        return repositoryIdentifier;
    }

    private String makeId(String id) {
        return "oai:" + getRepositoryIdentifier() + ":" + id;
    }

    /**
     * Class EntryList _more_
     *
     *
     * @author RAMADDA Development Team
     */
    private static class EntryList {

        String resumptionToken;

        List<Entry> entries = new ArrayList<Entry>();

        public EntryList(List<Entry> entries, String token) {
            this.entries         = entries;
            this.resumptionToken = token;
        }
    }

    private String format(Date d) {
        return sdf.format(d) + "Z";
    }

    private void makeHeader(Entry entry, Element node) throws Exception {
        Element header = XmlUtil.create(TAG_HEADER, node);
        XmlUtil.create(TAG_IDENTIFIER, header, makeId(entry.getId()));
        XmlUtil.create(TAG_DATESTAMP, header,
                       format(new Date(entry.getStartDate())));
    }

    /**
     * Class MyException _more_
     *
     *
     * @author RAMADDA Development Team
     */
    public static class MyException extends IllegalArgumentException {

        String code;

        public MyException(String code, String msg) {
            super(msg);
            this.code = code;
        }
    }

    private Date parseUTC(String s) {
        for (SimpleDateFormat parser : parsers) {
            try {
                return parser.parse(s);
            } catch (Exception exc) {}
        }

        return null;
    }

    private EntryList getEntries(Request request) throws Exception {
        int     max        = request.get(ARG_MAX, 5);
        int     skip       = request.get(ARG_RESUMPTIONTOKEN, 0);
        Request newRequest = new Request(getRepository(), request.getUser());
        newRequest.put(ARG_SKIP, "" + skip);
        newRequest.put(ARG_MAX, "" + max);

        if (request.exists(ARG_FROM)) {
            newRequest.put(ARG_FROMDATE, request.getString(ARG_FROM, ""));
        }

        if (request.exists(ARG_UNTIL)) {
            newRequest.put(ARG_UNTIL, request.getString(ARG_UNTIL, ""));
        }

        List<Entry> entries = getEntryManager().getEntriesFromDb(newRequest);
        String      token   = null;
        if (entries.size() > 0) {
            if (entries.size() >= max) {
                token = "" + (skip + max);
            }
        }

        return new EntryList(entries, token);
    }

    private void handleListSets(Request request, Element root)
            throws Exception {
        handleError(request, root, ERROR_NOSETHIERARCHY);
    }

    private void addResumption(Request request, Element root,
                               EntryList entries)
            throws Exception {
        if (entries.resumptionToken != null) {
            XmlUtil.create(TAG_RESUMPTIONTOKEN, root,
                           entries.resumptionToken);
        } else if (request.exists(ARG_RESUMPTIONTOKEN)) {
            //Put in the blank one
            XmlUtil.create(TAG_RESUMPTIONTOKEN, root, "");
        }

    }

    private void handleListIdentifiers(Request request, Element root)
            throws Exception {
        if ( !request.exists(ARG_METADATAPREFIX)) {
            handleError(request, root, ERROR_BADARGUMENT,
                        "'" + ARG_METADATAPREFIX + "' is missing");

            return;
        }

        EntryList entryList = getEntries(request);
        if (entryList.entries.size() == 0) {
            handleError(request, root, ERROR_NORECORDSMATCH,
                        "No records match");

            return;
        }

        Element listNode = XmlUtil.create(TAG_LISTIDENTIFIERS, root);
        for (Entry entry : entryList.entries) {
            makeHeader(entry, listNode);
        }
        addResumption(request, listNode, entryList);
    }

    private void handleListRecords(Request request, Element root)
            throws Exception {
        EntryList entryList = getEntries(request);

        if ( !request.exists(ARG_METADATAPREFIX)) {
            handleError(request, root, ERROR_BADARGUMENT,
                        "'" + ARG_METADATAPREFIX + "' is missing");

            return;
        }

        if ( !request.exists(ARG_METADATAPREFIX)) {
            throw new IllegalArgumentException(
                "no metadataPrefix argument defined");
        }

        if (entryList.entries.size() == 0) {
            handleError(request, root, ERROR_NORECORDSMATCH,
                        "No records match");

            return;
        }
        Element listRecordNode = XmlUtil.create(TAG_LISTRECORDS, root);
        for (Entry entry : entryList.entries) {
            makeRecord(request, entry, listRecordNode);
        }
        addResumption(request, listRecordNode, entryList);
    }

    private void addMetadata(Request request, Entry entry, Element node)
            throws Exception {
        List<Metadata> metadataList = getMetadataManager().getMetadata(request,entry);
        List<MetadataHandler> metadataHandlers =
            repository.getMetadataManager().getMetadataHandlers();
        for (Metadata metadata : metadataList) {
            MetadataHandler metadataHandler =
                getMetadataManager().findMetadataHandler(metadata);
            if (metadataHandler != null) {
                metadataHandler.addMetadataToXml(request,
                        MetadataTypeBase.TEMPLATETYPE_OAIDC, entry, metadata,
                        node.getOwnerDocument(), node);
            }
        }
    }

    private void handleGetRecord(Request request, Element root)
            throws Exception {
        if ( !request.exists(ARG_IDENTIFIER)) {
            handleError(request, root, ERROR_BADARGUMENT,
                        "'identifier' is missing");

            return;
        }
        if ( !request.exists(ARG_METADATAPREFIX)) {
            handleError(request, root, ERROR_BADARGUMENT,
                        "'" + ARG_METADATAPREFIX + "' is missing");

            return;
        }

        String id    = getId(request.getString(ARG_IDENTIFIER, ""));
        Entry  entry = getEntryManager().getEntry(request, id);
        if (entry == null) {
            handleError(request, root, ERROR_IDDOESNOTEXIST);

            return;
        }

        Element node = XmlUtil.create(TAG_GETRECORD, root);
        makeRecord(request, entry, node);
    }

    private void makeRecord(Request request, Entry entry, Element node)
            throws Exception {
        Element record = XmlUtil.create(TAG_RECORD, node);
        makeHeader(entry, record);

        Element metadata = XmlUtil.create(TAG_METADATA, record);
        Element oaidc = XmlUtil.create(TAG_OAIDC_DC, metadata, new String[] {
            ATTR_XMLNS_OAIDC, "http://www.openarchives.org/OAI/2.0/oai_dc/",
            ATTR_XMLNS_DC, "http://purl.org/dc/elements/1.1/", ATTR_XMLNS_XSI,
            "http://www.w3.org/2001/XMLSchema-instance",
            ATTR_XSI_SCHEMALOCATION,
            "http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"
        });

        String entryUrl = request.getAbsoluteUrl(
                              request.entryUrl(
                                  getRepository().URL_ENTRY_SHOW, entry));
        XmlUtil.create(TAG_DC_IDENTIFIER, oaidc, entryUrl);
        XmlUtil.create(TAG_DC_TITLE, oaidc, entry.getName());
        XmlUtil.create(TAG_DC_DESCRIPTION, oaidc, entry.getDescription());
        addMetadata(request, entry, oaidc);

    }

}
