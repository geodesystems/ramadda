/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.thredds;


import org.ramadda.geodata.cdmdata.*;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.harvester.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.sql.SqlUtil;



import org.w3c.dom.*;


import ucar.unidata.util.CatalogUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;



import java.io.*;



import java.net.*;



import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;



/**
 * Class CatalogHarvester _more_
 *
 *
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public class CatalogHarvester extends Harvester {

    /** arg and xml attr */
    private static final String ATTR_TOPURL = "topurl";

    /** arg and xml attr */
    private static final String ATTR_DOWNLOAD = "download";

    /** arg and xml attr */
    private static final String ATTR_RECURSE = "recurse";


    /** Should we follow catalog refs */
    private boolean recurse = false;

    /** Should we download files */
    private boolean download = false;

    /** _more_ */
    private HashSet seenCatalog;

    /** _more_ */
    private List<String> groups;

    /** _more_ */
    private int catalogCnt = 0;

    /** _more_ */
    private int entryCnt = 0;

    /** _more_ */
    private int groupCnt = 0;


    /** _more_ */
    private String topUrl;

    /** _more_ */
    private List<Entry> entries = new ArrayList<Entry>();

    /** _more_ */
    private Object jobId;


    /**
     * _more_
     *
     * @param repository _more_
     * @param id _more_
     *
     * @throws Exception _more_
     */
    public CatalogHarvester(Repository repository, String id)
            throws Exception {
        super(repository, id);
    }


    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     *
     * @throws Exception _more_
     */
    public CatalogHarvester(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }


    /**
     * _more_
     *
     * @param repository _more_
     * @param group _more_
     * @param url _more_
     * @param user _more_
     * @param recurse _more_
     * @param download _more_
     * @param jobId _more_
     */
    public CatalogHarvester(Repository repository, Entry group, String url,
                            User user, boolean recurse, boolean download,
                            Object jobId) {
        super(repository);
        this.jobId = jobId;
        setName("Catalog harvester");
        this.recurse  = recurse;
        this.download = download;
        setTopUrl(url);
        setUser(user);
        baseGroupId = group.getId();
    }


    /**
     * _more_
     *
     * @param url _more_
     */
    private void setTopUrl(String url) {
        if (url.endsWith(".html")) {
            url = url.replace(".html", ".xml");
        }
        this.topUrl = url;
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
        topUrl   = XmlUtil.getAttribute(element, ATTR_TOPURL, topUrl);
        recurse  = XmlUtil.getAttribute(element, ATTR_RECURSE, recurse);
        download = XmlUtil.getAttribute(element, ATTR_DOWNLOAD, download);
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
        element.setAttribute(ATTR_TOPURL, topUrl);
        element.setAttribute(ATTR_DOWNLOAD, "" + download);
        element.setAttribute(ATTR_RECURSE, "" + recurse);
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
        setTopUrl(request.getUnsafeString(ATTR_TOPURL, topUrl));
        if (request.exists(ATTR_RECURSE)) {
            recurse = request.get(ATTR_RECURSE, recurse);
        } else {
            recurse = false;
        }
        if (request.exists(ATTR_DOWNLOAD)) {
            download = request.get(ATTR_DOWNLOAD, download);
        } else {
            download = false;
        }
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
        addBaseGroupSelect(ATTR_BASEGROUP, sb);

        sb.append(HU.formEntry(msgLabel("Catalog URL"),
                                      HU.input(ATTR_TOPURL, topUrl,
                                          HU.SIZE_60)));
        HU.formEntry(sb,"",HU.labeledCheckbox(ATTR_RECURSE,"true", recurse,"Recurse"));
        HU.formEntry(sb, "",HU.labeledCheckbox(ATTR_DOWNLOAD,"true", download,"Download Files"));
	HU.formEntry(sb, msgLabel("Metadata"),
		     HU.labeledCheckbox(ATTR_ADDMETADATA, "true",getAddMetadata(),"Add full metadata")
		     + HU.space(4)
		     + HU.labeledCheckbox(ATTR_ADDSHORTMETADATA, "true", getAddShortMetadata(), "Just add spatial/temporal metadata"));
        sb.append(HU.formEntry(msgLabel("User"),
                                      HU.input(ATTR_USER,
                                          (getUserName() != null)
                                          ? getUserName().trim()
                                          : "", HU.SIZE_30)));

    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getDescription() {
        return "THREDDS Catalog Harvester";
    }


    /**
     * _more_
     *
     *
     * @param timestamp _more_
     * @throws Exception _more_
     */
    protected void runInner(int timestamp) throws Exception {
        entryCnt    = 0;
        catalogCnt  = 0;
        groupCnt    = 0;
        groups      = new ArrayList<String>();
        entries     = new ArrayList<Entry>();
        seenCatalog = new HashSet();
        importCatalog(topUrl, getBaseGroup(), 0, timestamp);
        if ((entries.size() > 0) && !getTestMode()) {
            getEntryManager().processEntries(this, null, entries, false);
        }
        entries = new ArrayList<Entry>();
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void checkToAddEntries() throws Exception {
        if (entries.size() > 100) {
            if ( !getTestMode()) {
                getEntryManager().processEntries(this, null, entries, false);
            }
            entries = new ArrayList<Entry>();
        }
    }

    /**
     * _more_
     *
     * @param url _more_
     * @param parent _more_
     * @param depth _more_
     * @param timestamp _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private boolean importCatalog(String url, Entry parent, int depth,
                                  int timestamp)
            throws Exception {
        if ( !canContinueRunning(timestamp)) {
            return true;
        }
        if (seenCatalog.contains(url)) {
            return true;
        }
        if (depth > 10) {
            logHarvesterInfo("Catalogs go too deep:" + url);

            return true;
        }
        if (url.indexOf("hyrax/LBA") >= 0) {
            logHarvesterInfo("hyrax/LBA bad catalog");

            return true;
        }
        catalogCnt++;
        seenCatalog.add(url);
        try {
            Element root = XmlUtil.getRoot(url, getClass());
            if (root == null) {
                logHarvesterInfo("Could not load catalog:" + url);

                //                System.err.println("xml:"
                //                                   + IOUtil.readContents(url, getClass()));
                return true;
            }
            //                System.err.println("loaded:" + url);
            NodeList children    = XmlUtil.getElements(root);
            int      cnt         = 0;
            Element  datasetNode = null;
            for (int i = 0; i < children.getLength(); i++) {
                Element child = (Element) children.item(i);
                if (XmlUtil.isTag(child, CatalogUtil.TAG_DATASET)
                        || XmlUtil.isTag(child, CatalogUtil.TAG_CATALOGREF)) {
                    if (XmlUtil.isTag(child, CatalogUtil.TAG_DATASET)) {
                        datasetNode = (Element) child;
                    }
                    cnt++;
                }
            }

            //If there is just one top-level dataset node then just load that
            if ((cnt == 1) && (datasetNode != null)) {
                recurseCatalog((Element) datasetNode, parent, url, 0, depth,
                               timestamp);
            } else {
                recurseCatalog((Element) root, parent, url, 0, depth,
                               timestamp);
            }

            return true;
        } catch (Exception exc) {
            logHarvesterError("Error harvesting catalog:" + url, exc);

            return false;
        }
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param metadataList _more_
     */
    private void insertMetadata(Entry entry, List<Metadata> metadataList) {
        for (Metadata metadata : metadataList) {
            metadata.setEntryId(entry.getId());
            try {
                if (metadata.getAttr1().length() > 10000) {
                    logHarvesterInfo("Too long metadata:"
                                     + metadata.getAttr1().substring(0, 100)
                                     + "...");

                    continue;
                }
                getMetadataManager().insertMetadata(metadata);
            } catch (Exception exc) {
                logHarvesterError("Bad metadata", exc);
                /*
                System.err.println("metadata attr1" + metadata.getAttr1());
                System.err.println("metadata attr2" + metadata.getAttr2());
                System.err.println("metadata attr3" + metadata.getAttr3());
                System.err.println("metadata attr4" + metadata.getAttr4());
                */
            }
        }
    }

    /**
     * _more_
     *
     * @param timestamp _more_
     *
     * @return _more_
     */
    public boolean canContinueRunning(int timestamp) {
        if ( !super.canContinueRunning(timestamp)) {
            return false;
        }
        if (getTestMode() && (entryCnt >= getTestCount())) {
            return false;
        }
        if ((jobId != null) && !getActionManager().getActionOk(jobId)) {
            getActionManager().setActionMessage(jobId,
                    "Catalog import canceled");

            return false;
        }

        return true;
    }


    /**
     * _more_
     *
     * @param node _more_
     * @param parent _more_
     * @param catalogUrlPath _more_
     * @param xmlDepth _more_
     * @param recurseDepth _more_
     * @param timestamp _more_
     *
     * @throws Exception _more_
     */
    private void recurseCatalog(Element node, Entry parent,
                                String catalogUrlPath, int xmlDepth,
                                int recurseDepth, int timestamp)
            throws Exception {

        if ( !canContinueRunning(timestamp)) {
            return;
        }

        String tab = "";
        for (int i = 0; i < xmlDepth; i++) {
            tab = tab + "  ";
        }

        URL catalogUrl = new URL(catalogUrlPath);
        String name =
            XmlUtil.getAttribute(node, ATTR_NAME,
                                 IOUtil.getFileTail(catalogUrlPath));
        NodeList elements = XmlUtil.getElements(node);
        String urlPath = XmlUtil.getAttribute(node, CatalogUtil.ATTR_URLPATH,
                             (String) null);
        if (urlPath == null) {
            Element accessNode = XmlUtil.findChild(node,
                                     CatalogUtil.TAG_ACCESS);
            if (accessNode != null) {
                urlPath = XmlUtil.getAttribute(accessNode,
                        CatalogUtil.ATTR_URLPATH);
            }
        }

        boolean haveChildDatasets = false;
        for (int i = 0; i < elements.getLength(); i++) {
            Element child = (Element) elements.item(i);
            if (XmlUtil.isTag(child, CatalogUtil.TAG_DATASET)) {
                haveChildDatasets = true;

                break;
            }
        }

        //If we are harvesting a RAMADDA thredds catalog then skip the latestopendap dataset
        if ((urlPath != null)
                && (urlPath.indexOf("latestopendap=true") >= 0)) {
            return;
        }
        /*
        System.err.println(tab + "name:" + name + "  #children:"
                           + elements.getLength() + " depth:" + xmlDepth
                           + " " + urlPath + " " + haveChildDatasets);*/
        //        if(entries.size()>2) {return;}

        if ( !haveChildDatasets && (xmlDepth > 0) && (urlPath != null)) {
            if (makeEntry(node, parent, catalogUrlPath, urlPath, name)) {
                return;
            }
        }

        name = name.replace(Entry.IDDELIMITER, "--");
        name = name.replace("'", "");
        Entry group = null;
        for (Entry newGroup :
                getEntryManager().findEntriesWithName(null, parent, name)) {
            if (newGroup.isGroup()) {
                group = (Entry) newGroup;

                break;
            }
        }
	Request request = getRequest();


        if (group == null) {
            group = getEntryManager().makeNewGroup(request,parent, name, getUser());
            List<Metadata> metadataList = new ArrayList<Metadata>();
            CatalogOutputHandler.collectMetadata(repository, metadataList,
                    node);
            metadataList.add(makeImportMetadata(group.getId(),
                    catalogUrlPath));
            insertMetadata(group, metadataList);
            String crumbs = getPageHandler().getBreadCrumbs(getRequest(),
                                group, getBaseGroup());
            crumbs = crumbs.replace("class=", "xclass=");
            groups.add(crumbs);
            groupCnt++;
            if (groups.size() > 100) {
                groups = new ArrayList<String>();
            }
        }

        for (int i = 0; i < elements.getLength(); i++) {
            Element child = (Element) elements.item(i);
            String  tag   = XmlUtil.getLocalName(child);
            if (tag.equals(CatalogUtil.TAG_DATASET)) {
                recurseCatalog(child, group, catalogUrlPath, xmlDepth + 1,
                               recurseDepth, timestamp);
            } else if (tag.equals(CatalogUtil.TAG_CATALOGREF)) {
                if ( !recurse) {
                    continue;
                }
                String url = XmlUtil.getAttribute(child, "xlink:href");
                URL    newCatalogUrl = new URL(catalogUrl, url);
                //                System.err.println("url:" + newCatalogUrl);
                if ( !importCatalog(newCatalogUrl.toString(), group,
                                    recurseDepth + 1, timestamp)) {
                    logHarvesterInfo("Could not load catalog:" + url);
                    logHarvesterInfo("Base catalog:" + catalogUrl);
                    logHarvesterInfo("Base URL:"
                                     + XmlUtil.getAttribute(child,
                                         "xlink:href"));
                }
            }
        }
    }


    /**
     * _more_
     *
     * @param node _more_
     * @param parent _more_
     * @param catalogUrlPath _more_
     * @param urlPath _more_
     * @param name _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private boolean makeEntry(Element node, Entry parent,
                              String catalogUrlPath, String urlPath,
                              String name)
            throws Exception {

	Request request = getRequest();
        URL catalogUrl = new URL(catalogUrlPath);
        Element serviceNode = CatalogUtil.findServiceNodeForDataset(node,
                                  false, (download
                                          ? CatalogUtil.SERVICE_HTTP
                                          : null));


        boolean isOpendap = false;
        if (serviceNode != null) {
            String path = XmlUtil.getAttribute(serviceNode, "base");
            urlPath = new URL(catalogUrl, path + urlPath).toString();
            String serviceType = XmlUtil.getAttribute(serviceNode,
                                     CatalogUtil.ATTR_SERVICETYPE,
                                     "").toLowerCase();
            isOpendap = serviceType.equals("opendap")
                        || serviceType.equals("dods");
        }

        //Check if we already have one with this name under the parent folder
        //If we are downloading then go by name not resource
        String[] existingIds = (download
                                ? getEntryManager().findEntryIdsWithName(
                                    getRequest(), parent, name)
                                : getEntryManager().findEntryIdsWithResource(
                                    getRequest(), parent, urlPath));


        //return if we have already harvested this one
        if (existingIds.length > 0) {
            //Reharvest the metadata
            if (isOpendap && (getAddMetadata() || getAddShortMetadata())) {
                Entry existingEntry =
                    getEntryManager().getEntry(getRequest(), existingIds[0]);
                logHarvesterInfo("Reharvesting metadata for:"
                                 + existingEntry);
                List tmpEntries = Misc.newList(existingEntry);
                getEntryManager().addInitialMetadata(getRequest(),
                        tmpEntries, getAddMetadata(), getAddShortMetadata());
                getEntryManager().updateEntries(getRequest(), tmpEntries);
            }

            return false;
        }


        boolean needToDownload = download
                                 && (urlPath.startsWith("http:")
                                     || urlPath.startsWith("https:")
                                     || urlPath.startsWith("ftp:"));

        boolean  didDownload = false;
        Resource resource    = null;
        if (needToDownload) {
            String tail    = IOUtil.getFileTail(urlPath);
            File   newFile = getStorageManager().getTmpFile(null, tail);
            try {
                URL           fromUrl    = new URL(urlPath);
                URLConnection connection = fromUrl.openConnection();
                InputStream   fromStream = connection.getInputStream();
                OutputStream toStream =
                    getStorageManager().getFileOutputStream(newFile);
                long bytes = IOUtil.writeTo(fromStream, toStream);
                toStream.close();
                fromStream.close();
                if (bytes > 0) {
                    String theFile =
                        getStorageManager().moveToStorage((Request) null,
                            newFile).toString();
                    resource = new Resource(new File(theFile),
                                            Resource.TYPE_STOREDFILE);
                    didDownload = true;
                }
            } catch (Exception ignore) {
                logHarvesterError("Error downloading: " + urlPath, ignore);
            }
        }

        if (resource == null) {
            resource = new Resource(urlPath, Resource.TYPE_URL);
        }

        TypeHandler typeHandler = repository.getTypeHandler(((isOpendap
                                      && !didDownload)
                ? OpendapLinkTypeHandler.TYPE_OPENDAPLINK
                : TypeHandler.TYPE_FILE));

        entryCnt++;
        if (jobId != null) {
            getActionManager().setActionMessage(jobId,
                    "Loaded " + catalogCnt + " catalogs<br>" + "Created "
                    + entryCnt + " entries<br>" + "Created " + groupCnt
                    + " groups<br>");
        }

        Entry entry      = typeHandler.createEntry(repository.getGUID());
        long  createDate = new Date().getTime();

        entry.initEntry(name, "", parent, getUser(), resource, "",
                        Entry.DEFAULT_ORDER, createDate, createDate,
                        createDate, createDate, null);

        entries.add(entry);
        typeHandler.initializeNewEntry(getRepository().getTmpRequest(),
                                       entry, TypeHandler.NewType.NEW);
        List<Metadata> metadataList = new ArrayList<Metadata>();
        CatalogOutputHandler.collectMetadata(repository, metadataList, node);
        metadataList.add(makeImportMetadata(entry.getId(), catalogUrlPath));
        for (Metadata metadata : metadataList) {
            metadata.setEntryId(entry.getId());
            getMetadataManager().addMetadata(request,entry, metadata);
        }

        if (isOpendap && (getAddMetadata() || getAddShortMetadata())) {
            getEntryManager().addInitialMetadata(null,
                    (List<Entry>) Misc.newList(entry), getAddMetadata(),
                    getAddShortMetadata());

        }
        checkToAddEntries();

        return true;

    }

    /**
     * Make the importedfromcatalog metadata
     *
     * @param entryId entry id
     * @param catalogUrlPath catalog url
     *
     * @return the metadata
     */
    private Metadata makeImportMetadata(String entryId,
                                        String catalogUrlPath) {
        return new Metadata(repository.getGUID(), entryId,
                            ThreddsMetadataHandler.TYPE_LINK, DFLT_INHERITED,
                            "Imported from catalog", catalogUrlPath,
                            Metadata.DFLT_ATTR, Metadata.DFLT_ATTR,
                            Metadata.DFLT_EXTRA);
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getExtraInfo() throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append("Catalog: " + topUrl + "<br>");
        sb.append("Loaded " + catalogCnt + " catalogs<br>");
        sb.append("Created " + entryCnt + " entries<br>");
        sb.append("Created " + groupCnt + " groups<br>");

        if (groups != null) {
            StringBuffer groupSB = new StringBuffer();
            groupSB.append("<div class=\"scrollablediv\"><ul>");
            for (String groupLine : groups) {
                groupSB.append("<li>");
                groupSB.append(groupLine);
            }
            groupSB.append("</ul></div>");
            sb.append(HU.makeShowHideBlock("Entries",
                    groupSB.toString(), false));
        }

        return sb.toString();
    }


}
