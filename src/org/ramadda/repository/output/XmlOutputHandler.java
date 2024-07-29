/**
Copyright (c) 2008-2024 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.output;


import org.ramadda.repository.Entry;
import org.ramadda.repository.EntryManager;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.ramadda.repository.Resource;
import org.ramadda.repository.Result;
import org.ramadda.repository.util.FileWriter;
import org.ramadda.util.Utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ucar.unidata.xml.XmlUtil;


import java.util.Date;
import java.util.List;
import java.util.zip.ZipOutputStream;

public class XmlOutputHandler extends OutputHandler {

    /** XML Output type */
    public static final OutputType OUTPUT_XML = new OutputType("RAMADDA XML Export",
                                                    "xml.xml",
                                                    OutputType.TYPE_FEEDS,
                                                    "", ICON_XML);


    /** XML Entry output type */
    public static final OutputType OUTPUT_XMLENTRY =
        new OutputType("RAMADDA XML Entry Export", "xml.xmlentry", OutputType.TYPE_FEEDS,
                       "", ICON_XML);


    /**
     * Create an XML output handler
     *
     * @param repository   the Repository
     * @param element      the Element
     * @throws Exception   problem creating the handler
     */
    public XmlOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_XML);
        addType(OUTPUT_XMLENTRY);
    }


    /**
     * Get the MIME type for the output type
     *
     * @param output  the output type
     *
     * @return  the mimetype
     */
    public String getMimeType(OutputType output) {
        if (output.equals(OUTPUT_XML) || output.equals(OUTPUT_XMLENTRY)) {
            return repository.getMimeTypeFromSuffix(".xml");
        }

        return super.getMimeType(output);
    }


    /**
     * Output the entry
     *
     * @param request   the Request
     * @param outputType the outputType
     * @param entry      the Entry
     *
     * @return  the Result
     *
     * @throws Exception problem creating the result
     */
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
        StringBuffer sb = new StringBuffer(getEntryXml(request, entry));
        return new Result("", sb, repository.getMimeTypeFromSuffix(".xml"));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getEntryXml(Request request, Entry entry) throws Exception {
        Document doc = XmlUtil.makeDocument();
        Element root = getEntryTag(request, entry, null, doc, null, false,
                                   true);

        return XmlUtil.toString(root);
    }



    /**
     * Output a group of entries
     *
     * @param request      the Request
     * @param outputType   the output type
     * @param group        the group Entry
     * @param children _more_
     *
     * @return  the Result
     *
     * @throws Exception  couldn't generate the Result
     */
    @Override
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> children)
            throws Exception {

	boolean doChildren = request.get(ARG_DO_CHILDREN,false);
        if (!doChildren && outputType!=null && outputType.equals(OUTPUT_XMLENTRY)) {
            return outputEntry(request, outputType, group);
        }

        Document doc  = XmlUtil.makeDocument();
        Element  root;
	if(doChildren) {
	    root = getGroupTag(request, getEntryManager().getDummyGroup("parent"), doc, null);
	} else {
	    root = getGroupTag(request, group, doc, null);
	}
        for (Entry child : children) {
            if (getEntryManager().handleEntryAsGroup(child)) {
                getGroupTag(request, child, doc, root);
            } else {
                getEntryTag(request, child, null, doc, root, false, true);
            }
        }
        StringBuffer sb = new StringBuffer(XmlUtil.toString(root));

        return new Result("", sb, repository.getMimeTypeFromSuffix(".xml"));
    }



    /**
     * Get the entry element as XML
     *
     *
     * @param request   the Request
     * @param entry     the Entry
     * @param fileWriter _more_
     * @param doc       the document to add to
     * @param parent    the parent Entry
     * @param forExport true for export
     * @param includeParentId  true to include the parent ID
     *
     * @return  the XML
     *
     * @throws Exception problem creating the tag
     */
    public Element getEntryTag(Request request, Entry entry,
                               FileWriter fileWriter, Document doc,
                               Element parent, boolean forExport,
                               boolean includeParentId)
            throws Exception {

	boolean encode  = request.get("encode",true);

        Element node = XmlUtil.create(doc, TAG_ENTRY, parent, new String[] {
            ATTR_ID, entry.getId(), ATTR_NAME, entry.getName(), ATTR_PARENT,
            (includeParentId
             ? entry.getParentEntryId()
             : ""), ATTR_TYPE, entry.getTypeHandler().getType(),
            ATTR_ISGROUP, "" + entry.isGroup(), ATTR_FROMDATE,
            getDateHandler().formatDate(new Date(entry.getStartDate())),
            ATTR_TODATE,
            getDateHandler().formatDate(new Date(entry.getEndDate())),
            ATTR_CREATEDATE,
            getDateHandler().formatDate(new Date(entry.getCreateDate())),
            ATTR_CHANGEDATE,
            getDateHandler().formatDate(new Date(entry.getChangeDate())),
            ATTR_PATH, entry.getFullName(false), ATTR_ENTRYORDER,
            "" + entry.getEntryOrder()
        });


        if (entry.hasAltitude()) {
            node.setAttribute(ATTR_ALTITUDE, "" + entry.getAltitude());
        } else {
            if (entry.hasAltitudeBottom()) {
                node.setAttribute(ATTR_ALTITUDE_BOTTOM,
                                  "" + entry.getAltitudeBottom());
            }
            if (entry.hasAltitudeTop()) {
                node.setAttribute(ATTR_ALTITUDE_TOP,
                                  "" + entry.getAltitudeTop());
            }
        }

        if (entry.hasNorth()) {
            node.setAttribute(ATTR_NORTH, "" + entry.getNorth(request));
        }
        if (entry.hasSouth()) {
            node.setAttribute(ATTR_SOUTH, "" + entry.getSouth(request));
        }
        if (entry.hasEast()) {
            node.setAttribute(ATTR_EAST, "" + entry.getEast(request));
        }
        if (entry.hasWest()) {
            node.setAttribute(ATTR_WEST, "" + entry.getWest(request));
        }

        if (entry.getResource().isDefined()) {
            Resource resource = entry.getResource();
            if (forExport) {
                if (resource.isUrl()) {
                    XmlUtil.setAttributes(node, new String[] { ATTR_URL,
                            resource.getPath() });
                }
            } else {
                XmlUtil.setAttributes(node, new String[] { ATTR_RESOURCE,
                        resource.getPath(), ATTR_RESOURCE_TYPE,
                        resource.getType() });
                String md5 = resource.getMd5();
                if (md5 != null) {
                    node.setAttribute(ATTR_MD5, md5);
                }
                long filesize = resource.getFileSize();
                if (filesize >= 0) {
                    node.setAttribute(ATTR_FILESIZE, "" + filesize);
                }
            }

            //Add the service nodes
            if ( !forExport) {
                for (OutputHandler outputHandler :
                        getRepository().getOutputHandlers()) {
                    outputHandler.addToEntryNode(request, entry, node);
                }

                if (getRepository().getAccessManager().canAccessFile(request,
                        entry)) {
                    node.setAttribute(ATTR_FILESIZE,
                                      "" + entry.getResource().getFileSize());
                    String url =
                        getRepository().getEntryManager().getEntryResourceUrl(
                            request, entry, EntryManager.ARG_INLINE_DFLT,
                            true, EntryManager.ARG_ADDPATH_DFLT);
                    Element serviceNode = XmlUtil.create(TAG_SERVICE, node);
                    XmlUtil.setAttributes(serviceNode,
                                          new String[] { ATTR_TYPE,
                            SERVICE_FILE, ATTR_URL, url });
                }
            }
        }


        if (Utils.stringDefined(entry.getDescription())) {
            Element descNode = XmlUtil.create(doc, TAG_DESCRIPTION, node);
            descNode.setAttribute("encoded", encode?"true":"false");
            descNode.appendChild(XmlUtil.makeCDataNode(doc,
                    entry.getDescription(), encode));
        }
        getMetadataManager().addMetadata(request, entry, fileWriter, doc,
                                         node,encode);
        entry.getTypeHandler().addToEntryNode(request, entry, fileWriter,
					      node,encode);

	getAccessManager().addEntryXml(entry, doc, node);
        return node;
    }


    /**
     * Get the tag for the group
     *
     *
     * @param request   the Request
     * @param group     the group Entry
     * @param doc       the document to append to
     * @param parent    the parent Element
     *
     * @return the XML Element
     *
     * @throws Exception  unable to create group tag
     */
    private Element getGroupTag(Request request, Entry group, Document doc,
                                Element parent)
            throws Exception {
        Element node = getEntryTag(request, group, null, doc, parent, false,
                                   true);
        boolean canDoNew    = getAccessManager().canDoNew(request, group);
        boolean canDoUpload = getAccessManager().canDoUpload(request, group);
        node.setAttribute(ATTR_CANDONEW, "" + canDoNew);
        node.setAttribute(ATTR_CANDOUPLOAD, "" + canDoUpload);

        return node;

    }


}
