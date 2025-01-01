/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.thredds;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.util.SelectInfo;

import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;






import org.w3c.dom.*;

import ucar.unidata.util.CatalogUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.io.File;

import java.net.URL;

import java.sql.PreparedStatement;

import java.sql.ResultSet;
import java.sql.Statement;




import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


/**
 * Class TypeHandler _more_
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public class CatalogTypeHandler extends ExtensibleGroupTypeHandler {

    /** _more_ */
    public static final String ARG_CATALOG = "catalog";


    /** _more_ */
    public static final String TYPE_CATALOG = "catalog";


    /** _more_ */
    private static Hashtable<String, DomHolder> domCache = new Hashtable();

    /** _more_ */
    private Hashtable childIdToParent = new Hashtable();



    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public CatalogTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getEntryIconUrl(Request request, Entry entry)
            throws Exception {
        if (entry.isGroup()) {
            return getIconUrl(ICON_FOLDER_CLOSED);
        }

        return super.getIconUrl(request, entry);
    }


    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    private String[] parseId(String id) {
        if (id.startsWith(ID_PREFIX_SYNTH)) {
            id = id.substring(ID_PREFIX_SYNTH.length());
            id = new String(Utils.decodeBase64(id));
        }
        int idx = id.indexOf(":id:");
        if (idx < 0) {
            return new String[] { id, null };
        }
        String[] toks = new String[] { id.substring(0, idx),
                                       id.substring(idx + 4) };

        return toks;
    }


    /**
     * _more_
     *
     *
     * @param mainEntry _more_
     * @param parent _more_
     * @param ids _more_
     */
    private void walkTree(Entry mainEntry, Element parent, Hashtable ids) {
        NodeList elements = XmlUtil.getElements(parent);
        for (int i = 0; i < elements.getLength(); i++) {
            Element child = (Element) elements.item(i);
            if ( !child.getTagName().equals(CatalogUtil.TAG_DATASET)) {
                continue;
            }
            ids.put(getId(mainEntry, child), child);
            walkTree(mainEntry, child, ids);
        }
    }

    /**
     * _more_
     *
     *
     * @param mainEntry _more_
     * @param node _more_
     *
     * @return _more_
     */
    private String getId(Entry mainEntry, Element node) {
        String id = XmlUtil.getAttribute(node, ATTR_ID, (String) null);
        if (id == null) {
            id = getNamePath(node);
        }

        return id;
    }

    /**
     * _more_
     *
     * @param node _more_
     *
     * @return _more_
     */
    private String getNamePath(Node node) {
        if (node == null) {
            return null;
        }
        Node parent = node.getParentNode();
        if ( !(parent instanceof Element)) {
            return null;
        }
        String parentId = getNamePath(parent);
        String name     = XmlUtil.getAttribute(node, ATTR_NAME, "");
        if (parentId == null) {
            return name;
        }

        return parentId + "::" + name;
    }




    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Element getDom(String url) throws Exception {
        Element   root   = null;
        DomHolder holder = domCache.get(url);
        if (holder != null) {
            if (holder.isValid()) {
                root = holder.root;
            } else {
                domCache.remove(url);
            }
        }

        if (root == null) {
            root = XmlUtil.getRoot(url, getClass());
            domCache.put(url, new DomHolder(root));
        }

        return root;
    }

    /**
     * _more_
     *
     *
     * @param mainEntry _more_
     * @param id _more_
     *
     * @return _more_
     */
    private String getCatalogId(Entry mainEntry, String id) {
        id = Utils.encodeBase64(id);

        return ID_PREFIX_SYNTH + id;
    }

    /**
     * _more_
     *
     *
     * @param mainEntry _more_
     * @param url _more_
     * @param subid _more_
     *
     * @return _more_
     */
    private String getId(Entry mainEntry, String url, String subid) {
        if (subid == null) {
            return getCatalogId(mainEntry, url);
        }

        return getCatalogId(mainEntry, url + ":id:" + subid);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param parentEntry _more_
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public List<String> getSynthIds(Request request, SelectInfo select, Entry mainEntry,
                                    Entry parentEntry, String id)
            throws Exception {
        if (id == null) {
            id = mainEntry.getId();
        }


        String[]     loc        = parseId(id);
        String       catalogUrl = request.getString(ARG_CATALOG, null);
        List<String> ids        = new ArrayList<String>();
        String       url        = loc[0];
        String       nodeId     = loc[1];
        if ( !id.startsWith(ID_PREFIX_SYNTH)) {
            url    = mainEntry.getResource().getPath();
            nodeId = null;
        }
        URL     baseUrl = new URL(url);

        Element root    = getDom(url);
        if (root == null) {
            throw new IllegalArgumentException("Could not load catalog:"
                    + url);
        }




        Element dataset = (Element) XmlUtil.findChild(root,
                              CatalogUtil.TAG_DATASET);
        if (dataset != null) {
            root = dataset;
        }

        System.err.println("got root");

        String    parentId = getId(mainEntry, url, nodeId);

        Hashtable idMap    = new Hashtable();
        walkTree(mainEntry, dataset, idMap);
        if (nodeId != null) {
            dataset = (Element) idMap.get(nodeId);
        }
        if (dataset == null) {
            throw new IllegalArgumentException("Could not find dataset:"
                    + nodeId + " in catalog:" + url);
        }


        NodeList elements = XmlUtil.getElements(dataset);
        for (int i = 0; i < elements.getLength(); i++) {
            Element child = (Element) elements.item(i);
            if (child.getTagName().equals(CatalogUtil.TAG_DATASET)) {
                //                String datasetId = getId(mainEntry, child);
                String datasetId = getId(mainEntry, child.getBaseURI(), "");
                String entryId = getCatalogId(mainEntry,
                                     url + ":id:" + datasetId);
                childIdToParent.put(entryId, parentId);
                ids.add(entryId);
            } else if (child.getTagName().equals(
                    CatalogUtil.TAG_CATALOGREF)) {
                String href = XmlUtil.getAttribute(child,
                                  CatalogUtil.ATTR_XLINK_HREF);
                String catUrl    = new URL(baseUrl, href).toString();
                String datasetId = getCatalogId(mainEntry, catUrl);
                childIdToParent.put(datasetId, parentId);
                ids.add(datasetId);
            }
        }
        System.err.println("ids:" + ids);

        return ids;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isSynthType() {
        return true;
    }



    /**
     * Class DomHolder _more_
     *
     *
     * @version $Revision: 1.3 $
     */
    public static class DomHolder {

        /** _more_ */
        Element root;

        /** _more_ */
        Date dttm;

        /**
         * _more_
         *
         * @param root _more_
         */
        public DomHolder(Element root) {
            this.root = root;
            dttm      = new Date();
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean isValid() {
            Date now = new Date();
            //Only keep around catalogs for 5 minutes
            if ((now.getTime() - dttm.getTime()) > 1000 * 60 * 5) {
                return false;
            }

            return true;
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param parentEntry _more_
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry makeSynthEntry(Request request, Entry parentEntry, String id)
            throws Exception {
        System.err.println("make synth entry:" + id);
        String[] loc   = parseId(id);
        String   url   = loc[0];
        String   newId = getId(parentEntry, loc[0], loc[1]);
        if (parentEntry == null) {
            String parentId = (String) childIdToParent.get(newId);
            if (parentId != null) {
                //                parentEntry = getEntryManager().findGroup(request, parentId);
                parentEntry = getEntryManager().getEntry(request, parentId);
            }
        }

        if (parentEntry == null) {
            parentEntry = getEntryManager().getRootEntry();
        }

        URL     catalogUrl = new URL(url);
        Element root       = getDom(url);
        if (root == null) {
            throw new IllegalArgumentException("Could not load catalog:"
                    + url);
        }
        Element child = (Element) XmlUtil.findChild(root,
                            CatalogUtil.TAG_DATASET);

        if (child != null) {
            root = child;
        }

        Hashtable idMap = new Hashtable();
        walkTree(parentEntry, root, idMap);
        if (loc[1] != null) {
            root = (Element) idMap.get(loc[1]);
        }

        String   name = XmlUtil.getAttribute(root, ATTR_NAME, "");
        Entry    entry;
        Resource resource;
        if (CatalogUtil.haveChildDatasets(root)
                || CatalogUtil.haveChildCatalogs(root)) {
            entry    = new Entry(newId, this, true);
            resource = new Resource("", Resource.TYPE_URL);
        } else {
            String urlPath = XmlUtil.getAttribute(root,
                                 CatalogUtil.ATTR_URLPATH, (String) null);
            if (urlPath == null) {
                Element accessNode = XmlUtil.findChild(root,
                                         CatalogUtil.TAG_ACCESS);
                if (accessNode != null) {
                    urlPath = XmlUtil.getAttribute(accessNode,
                            CatalogUtil.ATTR_URLPATH);
                }
            }

            if (urlPath != null) {
                Element serviceNode =
                    CatalogUtil.findServiceNodeForDataset(root, false, null);
                if (serviceNode != null) {
                    String path = XmlUtil.getAttribute(serviceNode, "base");
                    urlPath = new URL(catalogUrl, path + urlPath).toString();
                }
            }
            entry    = new Entry(newId, this);
            resource = new Resource(((urlPath != null)
                                     ? urlPath
                                     : ""), Resource.TYPE_URL);
        }

        List<Metadata> metadataList = new ArrayList<Metadata>();
        CatalogOutputHandler.collectMetadata(repository, metadataList, root);
        metadataList.add(new Metadata(repository.getGUID(), entry.getId(),
                                      getMetadataManager().findType(ThreddsMetadataHandler.TYPE_LINK),
                                      DFLT_INHERITED,
                                      "Imported from catalog", url,
                                      Metadata.DFLT_ATTR, Metadata.DFLT_ATTR,
                                      Metadata.DFLT_EXTRA));
        for (Metadata metadata : metadataList) {
            metadata.setEntryId(entry.getId());
            getMetadataManager().addMetadata(request,entry, metadata);
        }

        Date now = new Date();
        entry.initEntry(name, "", (Entry) parentEntry,
                        getUserManager().getLocalFileUser(), resource, "",
                        Entry.DEFAULT_ORDER, now.getTime(), now.getTime(),
                        now.getTime(), now.getTime(), null);

        return entry;
    }

    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    public Entry createEntry(String id) {
        return new Entry(id, this, true);
    }

}
