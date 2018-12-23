/*
* Copyright (c) 2008-2018 Geode Systems LLC
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

package org.ramadda.geodata.echo;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.database.DatabaseManager;
import org.ramadda.repository.database.Tables;
import org.ramadda.repository.harvester.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.OutputType;
import org.ramadda.repository.type.*;

import org.ramadda.util.sql.Clause;
import org.ramadda.util.sql.SqlUtil;

import org.ramadda.util.HtmlUtils;


import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.xml.XmlUtil;




import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.sql.ResultSet;
import java.sql.Statement;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.*;


/**
 *
 */
public class EchoPublisher extends Harvester {

    /** _more_ */
    public static final String ATTR_FTP_URL = "echo.ftp.url";

    /** _more_ */
    public static final String ATTR_FTP_USER = "echo.ftp.user";

    /** _more_ */
    private String ftpUrl = "";

    /** _more_ */
    private String ftpUser = "";

    /** _more_ */
    private SimpleDateFormat sdf;

    /**
     * _more_
     *
     *
     *
     * @param repository _more_
     * @param id _more_
     * @throws Exception _more_
     */
    public EchoPublisher(Repository repository, String id) throws Exception {
        super(repository, id);
    }

    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     *
     * @throws Exception _more_
     */
    public EchoPublisher(Repository repository, Element element)
            throws Exception {
        super(repository, element);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getDescription() {
        return "ECHO Publisher";
    }



    /**
     * _more_
     *
     * @param element _more_
     *
     * @throws Exception _more_
     */
    protected void init(Element element) throws Exception {
        super.init(element);

        ftpUrl  = XmlUtil.getAttribute(element, ATTR_FTP_URL, ftpUrl);
        ftpUser = XmlUtil.getAttribute(element, ATTR_FTP_USER, ftpUrl);


    }

    /**
     * _more_
     *
     * @param t _more_
     *
     * @return _more_
     */
    private String formatDate(long t) {
        if (sdf == null) {
            sdf = RepositoryUtil.makeDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        }
        synchronized (sdf) {
            return sdf.format(new Date(t)) + "Z";
        }
    }


    /**
     * _more_
     *
     * @param element _more_
     *
     * @throws Exception _more_
     */
    public void applyState(Element element) throws Exception {
        super.applyState(element);
        element.setAttribute(ATTR_FTP_URL, ftpUrl);
        element.setAttribute(ATTR_FTP_USER, ftpUser);
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    public void applyEditForm(Request request) throws Exception {
        super.applyEditForm(request);
        ftpUrl  = request.getString(ATTR_FTP_URL, ftpUrl);
        ftpUser = request.getString(ATTR_FTP_USER, ftpUser);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void createEditForm(Request request, StringBuffer sb)
            throws Exception {

        super.createEditForm(request, sb);
        sb.append(HtmlUtils.formEntry("ECHO FTP URL",
                                      HtmlUtils.input(ATTR_FTP_URL, ftpUrl,
                                          60)));
        sb.append(HtmlUtils.formEntry("ECHO User",
                                      HtmlUtils.input(ATTR_FTP_USER, ftpUser,
                                          60)));
    }

    /**
     * _more_
     *
     * @param timestamp _more_
     *
     * @throws Exception _more_
     */
    protected void runInner(int timestamp) throws Exception {
        if ( !canContinueRunning(timestamp)) {
            return;
        }

        status = new StringBuffer("");
        int cnt = 0;
        logHarvesterInfo("EchoPublisher: starting");
        while (canContinueRunning(timestamp)) {
            doPublish();
            cnt++;
            if ( !getMonitor()) {
                status.append("Done<br>");
                logHarvesterInfo("Ran one time only. Exiting loop");

                break;
            }

            status.append("Done... sleeping for " + getSleepMinutes()
                          + " minutes<br>");
            logHarvesterInfo("Sleeping for " + getSleepMinutes()
                             + " minutes");
            doPause();
            status = new StringBuffer();
        }
        logHarvesterInfo("Done running");
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void doPublish() throws Exception {
        //Find all entries that have attached echo collection data
        Statement statement =
            getDatabaseManager().select(SqlUtil.comma(Tables.METADATA.COL_ID,
                Tables.METADATA.COL_ENTRY_ID), Tables.METADATA.NAME,
                    Clause.eq(Tables.METADATA.COL_TYPE, "echo.collection"));
        SqlUtil.Iterator iterator =
            getDatabaseManager().getIterator(statement);

        ResultSet   results;
        List<Entry> collections = new ArrayList<Entry>();
        while ((results = iterator.getNext()) != null) {
            String metadataId = results.getString(1);
            String entryId    = results.getString(2);
            Entry  entry = getEntryManager().getEntry(getRequest(), entryId);
            if (entry != null) {
                collections.add(entry);
            }
        }

        if (collections.size() == 0) {
            return;
        }



        File zipFile =
            getRepository().getStorageManager().getTmpFile(getRequest(),
                ".zip");
        OutputStream os = getStorageManager().getFileOutputStream(zipFile);
        writeCollections(collections, os, true);
        IOUtil.close(os);
    }


    /**
     * _more_
     *
     * @param collections _more_
     * @param os _more_
     * @param includeGranules _more_
     *
     * @throws Exception _more_
     */
    public void writeCollections(List<Entry> collections, OutputStream os,
                                 boolean includeGranules)
            throws Exception {
        ZipOutputStream zos = new ZipOutputStream(os);
        Document        doc = XmlUtil.makeDocument();
        Element root = XmlUtil.create(doc,
                                      EchoUtil.TAG_COLLECTIONMETADATAFILE,
                                      null, new String[] { "xmlns:xsi",
                "http://www.w3.org/2001/XMLSchema-instance",
                "xsi:noNamespaceSchemaLocation",
                "http://www.echo.nasa.gov/ingest/schemas/operations/Collection.xsd" });
        Element collectionsNode = XmlUtil.create(root.getOwnerDocument(),
                                      EchoUtil.TAG_COLLECTIONS, root);
        for (Entry entry : collections) {
            makeCollectionNode(entry, collectionsNode);
        }

        String xml = XmlUtil.toString(root);
        System.err.println(xml);
        zos.putNextEntry(new ZipEntry("collections.xml"));
        byte[] bytes = xml.getBytes();
        zos.write(bytes, 0, bytes.length);
        zos.closeEntry();
        IOUtil.close(zos);
    }



    /**
     * _more_
     *
     * @param entry _more_
     * @param collectionsNode _more_
     *
     * @throws Exception _more_
     */
    private void makeCollectionNode(Entry entry, Element collectionsNode)
            throws Exception {
        Document doc = collectionsNode.getOwnerDocument();
        Element collectionNode = XmlUtil.create(doc, EchoUtil.TAG_COLLECTION,
                                     collectionsNode);

        /*
      <ShortName>${entry.name}</ShortName>
      <InsertTime>${entry.publishdate}</InsertTime>
      <LastUpdate>${entry.changedate}</LastUpdate>
      <DataSetId>${entry.id}</DataSetId>
      <Description>${entry.description.cdata}</Description>
        */

        XmlUtil.create(doc, EchoUtil.TAG_SHORTNAME,
                       collectionNode).appendChild(XmlUtil.makeCDataNode(doc,
                           entry.getName(), false));
        XmlUtil.create(doc, EchoUtil.TAG_DESCRIPTION,
                       collectionNode).appendChild(XmlUtil.makeCDataNode(doc,
                           entry.getDescription(), false));


        XmlUtil.create(doc, EchoUtil.TAG_DATASETID, collectionNode,
                       entry.getId(), null);
        XmlUtil.create(doc, EchoUtil.TAG_INSERTTIME, collectionNode,
                       formatDate(entry.getCreateDate()), null);
        XmlUtil.create(doc, EchoUtil.TAG_LASTUPDATE, collectionNode,
                       formatDate(entry.getChangeDate()), null);

        /*
      <Temporal>
        <RangeDateTime>
          <BeginningDateTime>2007-04-01T01:00:00Z</BeginningDateTime>
            <EndingDateTime>2007-05-01T01:00:00Z</EndingDateTime>
        </RangeDateTime>
      </Temporal>
        */

        Element temporalNode = XmlUtil.create(doc, EchoUtil.TAG_TEMPORAL,
                                   collectionNode);
        Element rangeNode = XmlUtil.create(doc, EchoUtil.TAG_RANGEDATETIME,
                                           temporalNode);
        XmlUtil.create(doc, EchoUtil.TAG_BEGINNINGDATETIME, rangeNode,
                       formatDate(entry.getStartDate()), null);
        XmlUtil.create(doc, EchoUtil.TAG_ENDINGDATETIME, rangeNode,
                       formatDate(entry.getEndDate()), null);



        List<Metadata> metadataList = getMetadataManager().getMetadata(entry);
        List<MetadataHandler> metadataHandlers =
            repository.getMetadataManager().getMetadataHandlers();
        for (Metadata metadata : metadataList) {
            MetadataHandler metadataHandler =
                getMetadataManager().findMetadataHandler(metadata);
            if (metadataHandler != null) {
                metadataHandler.addMetadataToXml(getRequest(), "echo", entry,
                        metadata, doc, collectionNode);

            }
        }
    }
}
