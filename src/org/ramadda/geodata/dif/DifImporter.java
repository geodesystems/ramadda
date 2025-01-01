/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.dif;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.util.DifUtil;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;


import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;

import ucar.unidata.util.IOUtil;


import ucar.unidata.util.StringUtil;

import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

import java.util.List;


/**
 * Class description
 *
 *
 * @version        $version$, Thu, Nov 25, '10
 * @author         Enter your name here...
 */
@SuppressWarnings("unchecked")
public class DifImporter extends ImportHandler {

    /** _more_ */
    public static final String TYPE_DIF = "DIF";

    /** _more_ */
    public static final String ARG_DIF_TYPE = "dif.type";

    /**
     * ctor
     *
     * @param repository _more_
     */
    public DifImporter(Repository repository) {
        super(repository);
    }


    /**
     * _more_
     *
     * @param importTypes _more_
     * @param formBuffer _more_
     */
    @Override
    public void addImportTypes(List<TwoFacedObject> importTypes,
                               Appendable formBuffer) {
        super.addImportTypes(importTypes, formBuffer);
        importTypes.add(new TwoFacedObject("Dif Import", TYPE_DIF));
        //        formBuffer.append(HtmlUtils.formEntry(msgLabel("DIF Type"), HtmlUtils.input(ARG_DIF_TYPE,"")));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param repository _more_
     * @param uploadedFile _more_
     * @param parentEntry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Result handleRequest(Request request, Repository repository,
                                String uploadedFile, Entry parentEntry)
            throws Exception {
        if ( !request.getString(ARG_IMPORT_TYPE, "").equals(TYPE_DIF)) {
            return null;
        }
        List<Entry> entries = new ArrayList<Entry>();
        if (uploadedFile.endsWith(".zip")) {
            List<File> unzippedFiles =
                getStorageManager().unpackZipfile(request, uploadedFile);
            for (File f : unzippedFiles) {
                //                if(f.getName().endsWith(".txt")) {
                String xml = new String(
                                 IOUtil.readBytes(
                                     getStorageManager().getFileInputStream(
                                         f)));
                processXml(request, parentEntry, xml, entries);
            }
        } else {
            String xml = new String(
                             IOUtil.readBytes(
                                 getStorageManager().getFileInputStream(
                                     uploadedFile)));
            processXml(request, parentEntry, xml, entries);
        }

        StringBuffer sb = new StringBuffer();
        for (Entry entry : entries) {
            entry.setUser(request.getUser());
        }
        getEntryManager().addNewEntries(request, entries);
        sb.append(msgHeader("Imported entries"));
        sb.append("<ul>");
        for (Entry entry : entries) {
            sb.append("<li> ");
            sb.append(getPageHandler().getBreadCrumbs(request, entry,
                    parentEntry));
        }

        return getEntryManager().addEntryHeader(request, parentEntry,
                new Result("", sb));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param parentEntry _more_
     * @param xml _more_
     * @param entries _more_
     *
     * @throws Exception _more_
     */
    private void processXml(Request request, Entry parentEntry, String xml,
                            List<Entry> entries)
            throws Exception {

        String type = request.getString(ARG_DIF_TYPE, "project_project");
        Entry entry = getRepository().getTypeHandler(type).createEntry(
                          getRepository().getGUID());
        Object[] values  = entry.getTypeHandler().getEntryValues(entry);

        Element  root    = XmlUtil.getRoot(xml);
        Element  difRoot = null;
        if (root.getTagName().equals(DifUtil.TAG_DIF)) {
            difRoot = root;
        } else {
            List difs = XmlUtil.findDescendants(root, DifUtil.TAG_DIF);
            if (difs.size() == 0) {
                throw new IllegalArgumentException("Could not find DIF tag");
            }
            //TODO: handle multiples?
            difRoot = (Element) difs.get(0);
        }

        String title = XmlUtil.getGrandChildText(difRoot,
                           DifUtil.TAG_Entry_Title, "no name");
        String id = XmlUtil.getGrandChildText(difRoot, DifUtil.TAG_Entry_ID,
                        "");
        values[0] = id;


        addMetadata(request,entry, difRoot, DifUtil.TAG_Keyword,
                    DifMetadataHandler.TYPE_KEYWORD);
        addMetadata(request,entry, difRoot, DifUtil.TAG_ISO_Topic_Category,
                    DifMetadataHandler.TYPE_ISO_TOPIC_CATEGORY);
        addMetadata(request,entry, difRoot, DifUtil.TAG_Originating_Center,
                    DifMetadataHandler.TYPE_ORIGINATING_CENTER);
        addMetadata(request,entry, difRoot, DifUtil.TAG_Data_Set_Language,
                    DifMetadataHandler.TYPE_DATA_SET_LANGUAGE);
        addMetadata(request,entry, difRoot, DifUtil.TAG_Reference,
                    DifMetadataHandler.TYPE_REFERENCE);
        addMetadata(request,entry, difRoot, DifUtil.TAG_Distribution,
                    DifMetadataHandler.TYPE_DISTRIBUTION,
                    DifUtil.TAGS_Distribution);
        addMetadata(request,entry, difRoot, DifUtil.TAG_Related_URL,
                    DifMetadataHandler.TYPE_RELATED_URL,
                    DifUtil.TAGS_Related_URL);
        addMetadata(request,entry, difRoot, DifUtil.TAG_Project,
                    DifMetadataHandler.TYPE_PROJECT, DifUtil.TAGS_Project);
        addMetadata(request,entry, difRoot, DifUtil.TAG_Parameters,
                    DifMetadataHandler.TYPE_PARAMETERS,
                    DifUtil.TAGS_Parameters);
        addMetadata(request,entry, difRoot, DifUtil.TAG_Data_Set_Citation,
                    DifMetadataHandler.TYPE_DATA_SET_CITATION,
                    DifUtil.TAGS_Data_Set_Citation);
        addMetadata(request,entry, difRoot, DifUtil.TAG_Sensor_Name,
                    DifMetadataHandler.TYPE_INSTRUMENT,
                    DifUtil.TAGS_Sensor_Name);
        addMetadata(request,entry, difRoot, DifUtil.TAG_Source_Name,
                    DifMetadataHandler.TYPE_PLATFORM,
                    DifUtil.TAGS_Source_Name);
        addMetadata(request,entry, difRoot, DifUtil.TAG_Location,
                    DifMetadataHandler.TYPE_LOCATION, DifUtil.TAGS_Location);



        Element spatialNode = XmlUtil.findChild(difRoot,
                                  DifUtil.TAG_Spatial_Coverage);
        if (spatialNode != null) {
            String tmp;

            tmp = XmlUtil.getGrandChildText(
                spatialNode, DifUtil.TAG_Northernmost_Latitude, null);
            if (tmp != null) {
                entry.setNorth(Double.parseDouble(tmp));
            }
            tmp = XmlUtil.getGrandChildText(
                spatialNode, DifUtil.TAG_Westernmost_Longitude, null);
            if (tmp != null) {
                entry.setWest(Double.parseDouble(tmp));
            }
            tmp = XmlUtil.getGrandChildText(
                spatialNode, DifUtil.TAG_Southernmost_Latitude, null);
            if (tmp != null) {
                entry.setSouth(Double.parseDouble(tmp));
            }
            tmp = XmlUtil.getGrandChildText(
                spatialNode, DifUtil.TAG_Easternmost_Longitude, null);
            if (tmp != null) {
                entry.setEast(Double.parseDouble(tmp));
            }
        }


        Element temporalNode = XmlUtil.findChild(difRoot,
                                   DifUtil.TAG_Temporal_Coverage);
        if (temporalNode != null) {
            Date dttm = null;
            String startDate = XmlUtil.getGrandChildText(temporalNode,
                                   DifUtil.TAG_Start_Date, null);
            if (startDate != null) {
                dttm = Utils.parseDate(startDate);
                entry.setStartDate(dttm.getTime());
            }
            String stopDate = XmlUtil.getGrandChildText(temporalNode,
                                  DifUtil.TAG_Stop_Date, null);
            if (stopDate != null) {
                dttm = Utils.parseDate(stopDate);
                entry.setEndDate(dttm.getTime());
            } else {
                //Pick up the start date
                entry.setEndDate(dttm.getTime());
            }
        }

        /*
<object class="java.util.ArrayList">
    <method name="add">
        <object class="java.util.Hashtable">
            <method name="put">
                <java.lang.Integer>1</java.lang.Integer>
                <string><![CDATA[investigator]]></string>
            </method>
        </object>
    </method>
</object>
        */

        for (Element node :
                (List<Element>) XmlUtil.findChildren(difRoot,
                    DifUtil.TAG_Personnel)) {
            List roles = new ArrayList();
            int  cnt   = 1;
            for (Element roleNode :
                    (List<Element>) XmlUtil.findChildren(node,
                        DifUtil.TAG_Role)) {
                String    role = XmlUtil.getChildText(roleNode);
                Hashtable ht   = new Hashtable();
                ht.put(Integer.valueOf(cnt), role);
                roles.add(ht);
                cnt++;
            }
            String roleXml = Repository.encodeObject(roles);
            //            System.err.println(roleXml);

            Metadata metadata =
                new Metadata(getRepository().getGUID(), entry.getId(),
                             getMetadataManager().findType(DifMetadataHandler.TYPE_PERSONNEL),
                             DFLT_INHERITED, roleXml,
                             XmlUtil.getGrandChildText(node,
                                 DifUtil.TAG_First_Name,
                                 ""), XmlUtil.getGrandChildText(node,
                                     DifUtil.TAG_Middle_Name,
                                     ""), XmlUtil.getGrandChildText(node,
                                         DifUtil.TAG_Last_Name,
                                         ""), Metadata.DFLT_EXTRA);
            metadata.setAttr(5, XmlUtil.getGrandChildText(node,
                    DifUtil.TAG_Email, ""));
            getMetadataManager().addMetadata(request,entry, metadata);

        }


        entry.setDescription(XmlUtil.getGrandChildText(difRoot,
                DifUtil.TAG_Summary, ""));
        entry.setName(title);
        entry.setParentEntryId(parentEntry.getId());
        entries.add(entry);

    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param difRoot _more_
     * @param tag _more_
     * @param metadataId _more_
     *
     * @throws Exception _more_
     */
    private void addMetadata(Request request,Entry entry, Element difRoot, String tag,
                             String metadataId)
            throws Exception {
        for (Element node :
                (List<Element>) XmlUtil.findChildren(difRoot, tag)) {
            String value = XmlUtil.getChildText(node);
            Metadata metadata = new Metadata(getRepository().getGUID(),
                                             entry.getId(), getMetadataManager().findType(metadataId),
                                             DFLT_INHERITED, value,
                                             Metadata.DFLT_ATTR,
                                             Metadata.DFLT_ATTR,
                                             Metadata.DFLT_ATTR,
                                             Metadata.DFLT_EXTRA);
            getMetadataManager().addMetadata(request,entry, metadata);
        }
    }



    /**
     * _more_
     *
     * @param entry _more_
     * @param difRoot _more_
     * @param tag _more_
     * @param metadataId _more_
     * @param subTags _more_
     *
     * @throws Exception _more_
     */
    private void addMetadata(Request request,Entry entry, Element difRoot, String tag,
                             String metadataId, String[] subTags)
            throws Exception {
        for (Element node :
                (List<Element>) XmlUtil.findChildren(difRoot, tag)) {
            String[] values = new String[subTags.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = XmlUtil.getGrandChildText(node, subTags[i], "");
            }
            Metadata metadata = new Metadata(getRepository().getGUID(),
                                             entry.getId(), getMetadataManager().findType(metadataId),
                                             values);
            getMetadataManager().addMetadata(request,entry, metadata);
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
        DifImporter importer = new DifImporter(null);
        for (String file : args) {
            List<File>   files = new ArrayList<File>();
            StringBuffer sb    = new StringBuffer();
            //            importer.processXml(null, "parent", IOUtil.readContents(file,(String)null),files, sb);
        }
    }

}
