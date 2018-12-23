/*
* Copyright (c) 2008-2019 Geode Systems LLC
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

package org.ramadda.repository.client;


import org.ramadda.repository.RepositoryBase;
import org.ramadda.repository.RepositoryUtil;
import org.ramadda.repository.RequestUrl;
import org.ramadda.repository.util.ServerInfo;
import org.ramadda.util.HtmlUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ucar.unidata.ui.HttpFormEntry;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.xml.XmlUtil;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import java.net.URL;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;



/**
 *
 *
 * @author RAMADDA Development Team
 */
public class RepositoryClient extends RepositoryBase {

    /** _more_ */
    public static final String PROP_REPOSITORY = "RAMADDA_CLIENT_REPOSITORY";

    /** _more_ */
    public static final String PROP_USER = "RAMADDA_CLIENT_USER";

    /** _more_ */
    public static final String PROP_PASSWORD = "RAMADDA_CLIENT_PASSWORD";


    /** _more_ */
    public static final String CMD_URL = "-url";



    /** _more_ */
    public static final String CMD_FETCH = "-fetch";

    /** _more_ */
    public static final String CMD_SEARCH = "-search";

    /** _more_ */
    public static final String CMD_PRINT = "-print";

    /** _more_ */
    public static final String CMD_TIMEOUT = "-timeout";

    /** _more_ */
    public static final String CMD_PRINTXML = "-printxml";

    /** _more_ */
    public static final String CMD_IMPORT = "-import";

    /** _more_ */
    public static final String CMD_DEBUG = "-debug";

    /** _more_ */
    public static final String CMD_EXIT = "-exit";

    /** _more_ */
    public static final String CMD_FOLDER = "-folder";

    /** _more_ */
    public static final String CMD_FILE = "-file";

    /** _more_ */
    public static final String CMD_FILES = "-files";
    //    public static final String CMD_ = ;

    /** _more_ */
    private static String ATTR_LOCALFILES = "localfiles";

    /** _more_ */
    private static String ATTR_LOCALFILE_PATH = "localfilepath";


    //Note: This is also defined in SessionManager

    /** _more_ */
    public static final String COOKIE_NAME = "repositorysession";

    /** _more_ */
    private static final String ID_PREVIOUS = "previous";

    /** _more_ */
    private String sessionId;

    /** _more_ */
    private String user = "";

    /** _more_ */
    private String password = "";

    /** _more_ */
    private String name = "RAMADDA Client";


    /** _more_ */
    private String defaultGroupId;

    /** _more_ */
    private String defaultGroupName;

    /** _more_ */
    private int sslPort;

    /** _more_ */
    private String title;

    /** _more_ */
    private String description;


    /** _more_ */
    private String lastId = "";

    /** _more_ */
    private File importFile;

    /** _more_ */
    private String importParentId;

    /** _more_ */
    private boolean doingSearch = false;

    /** _more_ */
    private List<String[]> searchArgs = new ArrayList<String[]>();

    /** _more_ */
    private int timeout = 0;

    /** _more_ */
    private int callTimestamp = 0;

    /**
     * _more_
     */
    public RepositoryClient() {
        initCertificates();
    }

    /**
     * _more_
     *
     * @param serverUrl _more_
     * @param user _more_
     * @param password _more_
     *
     * @throws Exception _more_
     */
    public RepositoryClient(URL serverUrl, String user, String password)
            throws Exception {
        setPort(serverUrl.getPort());
        setHostname(serverUrl.getHost());
        setUrlBase(serverUrl.getPath());
        this.user     = user;
        this.password = password;
        initCertificates();
    }




    /**
     * _more_
     *
     * @param hostname _more_
     * @param port _more_
     * @param base _more_
     *
     * @throws Exception _more_
     */
    public RepositoryClient(String hostname, int port, String base)
            throws Exception {
        this(hostname, port, base, "", "");
    }

    /**
     * _more_
     *
     * @param hostname _more_
     * @param port _more_
     * @param base _more_
     * @param user _more_
     * @param password _more_
     *
     * @throws Exception _more_
     */
    public RepositoryClient(String hostname, int port, String base,
                            String user, String password)
            throws Exception {
        super(port);
        setHostname(hostname);
        this.user     = user;
        this.password = password;
        setUrlBase(base);
        initCertificates();
    }


    /**
     * If there is no trustStore property defined then always trust self-signed certificates
     */
    private void initCertificates() {
        if (System.getProperty("javax.net.ssl.trustStore") == null) {
            org.ramadda.util.NaiveTrustProvider.setAlwaysTrust(true);
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public int getHttpsPort() {
        return sslPort;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean useSsl() {
        return sslPort > 0;
    }

    /**
     * _more_
     *
     * @param port _more_
     */
    public void setHttpsPort(int port) {
        sslPort = port;
        boolean useSsl = sslPort > 0;
        URL_INFO.setNeedsSsl(useSsl);
        URL_USER_LOGIN.setNeedsSsl(useSsl);
        URL_USER_HOME.setNeedsSsl(useSsl);
        URL_ENTRY_SHOW.setNeedsSsl(useSsl);
        URL_ENTRY_GET.setNeedsSsl(useSsl);
        URL_ENTRY_XMLCREATE.setNeedsSsl(useSsl);
        URL_PING.setNeedsSsl(useSsl);
        URL_USER_HOME.setNeedsSsl(useSsl);
    }




    /**
     * _more_
     *
     * @param url _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String[] doPost(RequestUrl url, List<HttpFormEntry> entries)
            throws Exception {
        return doPost(url.getFullUrl(), entries);
    }

    /**
     * _more_
     *
     * @param url _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String[] doPost(String url, List<HttpFormEntry> entries)
            throws Exception {
        String[] result = HttpFormEntry.doPost(entries, url);
        callTimestamp++;

        return result;
    }


    /**
     * _more_
     */
    private void doTimeout() {
        if (timeout == 0) {
            return;
        }
        final int myTimestamp = callTimestamp;
        Misc.run(new Runnable() {
            public void run() {
                Misc.sleepSeconds(timeout);
                if (myTimestamp == callTimestamp) {
                    System.err.println("RepositoryClient: call timed out");
                    System.exit(1);
                } else {
                    //                        System.err.println ("call succeeded within timeout");
                }
            }
        });
    }

    /**
     * _more_
     *
     * @param host _more_
     * @param port _more_
     * @param base _more_
     * @param user _more_
     * @param passwd _more_
     * @param entryName _more_
     * @param entryDescription _more_
     * @param parent _more_
     * @param filePath _more_
     *
     * @return The id of the uploaded entry
     * @throws Exception _more_
     */
    public static String uploadFileToRamadda(String host, int port,
                                             String base, String user,
                                             String passwd, String entryName,
                                             String entryDescription,
                                             String parent, String filePath)
            throws Exception {
        return uploadFileToRamadda(new URL("http://" + host + ":" + port
                                           + base), user, passwd, entryName,
                                               entryDescription, parent,
                                                   filePath);
    }



    /**
     * _more_
     *
     * @param host _more_
     * @param user _more_
     * @param passwd _more_
     * @param entryName _more_
     * @param entryDescription _more_
     * @param parent _more_
     * @param filePath _more_
     *
     * @return The id of the uploaded entry
     * @throws Exception _more_
     */
    public static String uploadFileToRamadda(URL host, String user,
                                             String passwd, String entryName,
                                             String entryDescription,
                                             String parent, String filePath)
            throws Exception {

        RepositoryClient client = new RepositoryClient(host, user, passwd);

        return client.uploadFile(entryName, entryDescription, parent,
                                 filePath);
    }


    /**
     * _more_
     *
     * @param entryName _more_
     * @param entryDescription _more_
     * @param parent _more_
     * @param filePath _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String uploadFile(String entryName, String entryDescription,
                             String parent, String filePath)
            throws Exception {

        checkSession();

        Document doc = XmlUtil.makeDocument();
        Element root = XmlUtil.create(doc, TAG_ENTRIES, null,
                                      new String[] {});
        Element entryNode = XmlUtil.create(doc, TAG_ENTRY, root,
                                           new String[] {});

        /*
         * name
         */
        entryNode.setAttribute(ATTR_NAME, entryName);

        /*
         * description
         */
        Element descNode = XmlUtil.create(doc, TAG_DESCRIPTION, entryNode);
        descNode.appendChild(XmlUtil.makeCDataNode(doc, entryDescription,
                false));
        /*
         * parent
         */
        entryNode.setAttribute(ATTR_PARENT, parent);
        /*
         * file
         */
        File file = new File(filePath);
        entryNode.setAttribute(ATTR_FILE, IOUtil.getFileTail(filePath));
        /*
         * addmetadata
         */
        entryNode.setAttribute(ATTR_ADDMETADATA, "true");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream       zos = new ZipOutputStream(bos);

        /*
         * write the xml definition into the zip file
         */
        String xml = XmlUtil.toString(root);
        //        System.out.println(xml);
        zos.putNextEntry(new ZipEntry("entries.xml"));
        byte[] bytes = getXmlBytes(xml);
        zos.write(bytes, 0, bytes.length);
        zos.closeEntry();

        /*
         * add also the file
         */
        String file2string = file.toString();
        zos.putNextEntry(new ZipEntry(IOUtil.getFileTail(file2string)));
        bytes = IOUtil.readBytes(new FileInputStream(file));
        zos.write(bytes, 0, bytes.length);
        zos.closeEntry();

        zos.close();
        bos.close();

        List<HttpFormEntry> postEntries = new ArrayList<HttpFormEntry>();
        postEntries.add(HttpFormEntry.hidden(ARG_SESSIONID, getSessionId()));
        /*
        postEntries.add(
            HttpFormEntry.hidden(
            ARG_AUTHTOKEN, RepositoryUtil.hashString(getSessionId())));
        */
        postEntries.add(HttpFormEntry.hidden(ARG_RESPONSE, RESPONSE_XML));
        postEntries.add(new HttpFormEntry(ARG_FILE, "entries.zip",
                                          bos.toByteArray()));

        RequestUrl URL_ENTRY_XMLCREATE = new RequestUrl(this,
                                             "/entry/xmlcreate", useSsl());
        String[] result = doPost(URL_ENTRY_XMLCREATE, postEntries);
        if (result[0] != null) {
            throw new EntryErrorException(result[0]);
        }

        Element response = XmlUtil.getRoot(result[1]);
        if ( !responseOk(response)) {
            String body = XmlUtil.getChildText(response);

            throw new EntryErrorException(body);
        }
        Element newEntryNode = XmlUtil.findChild(response, TAG_ENTRY);
        if (newEntryNode == null) {
            throw new IllegalStateException("No entry node found in:"
                                            + XmlUtil.toString(response));
        }

        return XmlUtil.getAttribute(newEntryNode, ATTR_ID);

    }



    /**
     * _more_
     *
     * @param files _more_
     *
     * @throws Exception _more_
     */
    public void uploadFiles(List<EntryFile> files) throws Exception {
        checkSession();

        Document doc = XmlUtil.makeDocument();
        Element root = XmlUtil.create(doc, TAG_ENTRIES, null,
                                      new String[] {});
        for (EntryFile f : files) {
            Element entryNode = XmlUtil.create(doc, TAG_ENTRY, root,
                                    new String[] {});
            /*
             * name
             */
            entryNode.setAttribute(ATTR_NAME, f.entryName);

            /*
             * description
             */
            Element descNode = XmlUtil.create(doc, TAG_DESCRIPTION,
                                   entryNode);
            descNode.appendChild(XmlUtil.makeCDataNode(doc,
                    f.entryDescription, false));
            /*
             * parent
             */
            entryNode.setAttribute(ATTR_PARENT, f.parent);
            /*
             * file
             */


            entryNode.setAttribute(ATTR_FILE, IOUtil.getFileTail(f.filePath));

            //            entryNode.setAttribute(ATTR_EAST, f.east);
            //            entryNode.setAttribute(ATTR_WEST, f.west);
            //            entryNode.setAttribute(ATTR_NORTH, f.north);
            //            entryNode.setAttribute(ATTR_SOUTH, f.south);

            /*
             * addmetadata
             */
            //entryNode.setAttribute(ATTR_ADDMETADATA, "true");
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream       zos = new ZipOutputStream(bos);

        /*
         * write the xml definition into the zip file
         */
        String xml = XmlUtil.toString(root);
        //        System.out.println(xml);
        zos.putNextEntry(new ZipEntry("entries.xml"));
        byte[] bytes = getXmlBytes(xml);
        zos.write(bytes, 0, bytes.length);
        zos.closeEntry();

        /*
         * add all the files
         */
        for (EntryFile f : files) {
            File   file        = new File(f.filePath);
            String file2string = file.toString();
            zos.putNextEntry(new ZipEntry(IOUtil.getFileTail(file2string)));
            bytes = IOUtil.readBytes(new FileInputStream(file));
            zos.write(bytes, 0, bytes.length);
            zos.closeEntry();
        }
        zos.close();
        bos.close();

        List<HttpFormEntry> postEntries = new ArrayList<HttpFormEntry>();

        addUrlArgs(postEntries);

        postEntries.add(new HttpFormEntry(ARG_FILE, "entries.zip",
                                          bos.toByteArray()));

        RequestUrl URL_ENTRY_XMLCREATE = new RequestUrl(this,
                                             "/entry/xmlcreate", useSsl());
        String[] result = doPost(URL_ENTRY_XMLCREATE, postEntries);

        if (result[0] != null) {
            throw new EntryErrorException(result[0]);
        }

        Element response = XmlUtil.getRoot(result[1]);
        if ( !responseOk(response)) {
            String body = XmlUtil.getChildText(response);

            throw new EntryErrorException(body);
        }
        Element newEntryNode = XmlUtil.findChild(response, TAG_ENTRY);
        if (newEntryNode == null) {
            throw new IllegalStateException("No entry node found in:"
                                            + XmlUtil.toString(response));
        }
    }



    /**
     * _more_
     *
     * @param entryName _more_
     * @param entryDescription _more_
     * @param parent _more_
     *
     * @return _more_
     */
    public boolean newFile(String entryName, String entryDescription,
                           String parent) {
        checkSession();
        try {
            Document doc = XmlUtil.makeDocument();
            Element root = XmlUtil.create(doc, TAG_ENTRIES, null,
                                          new String[] {});
            Element entryNode = XmlUtil.create(doc, TAG_ENTRY, root,
                                    new String[] {});

            /*
             * name
             */
            entryNode.setAttribute(ATTR_NAME, entryName);

            /*
             * description
             */
            Element descNode = XmlUtil.create(doc, TAG_DESCRIPTION,
                                   entryNode);
            descNode.appendChild(XmlUtil.makeCDataNode(doc, entryDescription,
                    false));
            /*
             * parent
             */
            entryNode.setAttribute(ATTR_PARENT, parent);

            /*
             * addmetadata
             */
            entryNode.setAttribute(ATTR_ADDMETADATA, "true");

            /*
             * write the xml definition into the zip file
             */
            String              xml         = XmlUtil.toString(root);
            //        System.out.println(xml);


            List<HttpFormEntry> postEntries = new ArrayList<HttpFormEntry>();

            addUrlArgs(postEntries);


            postEntries.add(new HttpFormEntry(ARG_FILE, "entries.xml",
                    getXmlBytes(xml)));

            String[] result = doPost(URL_ENTRY_XMLCREATE, postEntries);

            if (result[0] != null) {
                handleError("Error creating file:\n" + result[0], null);

                return false;
            }
            Element response = XmlUtil.getRoot(result[1]);
            if (responseOk(response)) {
                handleMessage("file created");

                return true;
            }
            String body = XmlUtil.getChildText(response).trim();
            handleError("Error creating file:" + body, null);
        } catch (Exception exc) {
            handleError("Error creating file", exc);
        }

        return false;
    }




    /**
     * _more_
     *
     * @param entryId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getEntryXml(String entryId) throws Exception {
        checkSession();
        String[] args = new String[] {
            ARG_ENTRYID, entryId, ARG_OUTPUT, "xml.xml", ARG_SESSIONID,
            getSessionId()
        };
        String url = HtmlUtils.url(URL_ENTRY_SHOW.getFullUrl(), args);
        String xml = IOUtil.readContents(url, getClass());

        return xml;
    }


    /**
     * _more_
     *
     * @param entryId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public ClientEntry getEntry(String entryId) throws Exception {
        Element root = XmlUtil.getRoot(getEntryXml(entryId));

        return getEntry(root);
    }


    /**
     * _more_
     *
     * @param node _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public ClientEntry getEntry(Element node) throws Exception {
        ClientEntry entry = new ClientEntry(XmlUtil.getAttribute(node,
                                ATTR_ID));
        entry.init(node);

        return entry;



    }


    /**
     * This gets a input stream to the file download for the given entry
     *
     * @param entryId entry id
     *
     * @return  inputstream to the file download
     *
     * @throws Exception On badness
     */
    public InputStream getResourceInputStream(String entryId)
            throws Exception {
        checkSession();
        String url = HtmlUtils.url(URL_ENTRY_GET.getFullUrl(),
                                   new String[] { ARG_ENTRYID,
                entryId, ARG_SESSIONID, getSessionId() });

        return IOUtil.getInputStream(url, getClass());
    }



    /**
     * Download the resource for the given entry and write it to the given file. If the
     * toFileOrDirectory argument is a directory then download the Entry information from
     * RAMADDA and use the file name as the name of the file to write to. If a file of that name
     * already exists then it will get overwritten
     *
     * @param entryId The entry id
     * @param toFileOrDirectory The file or directory to write to
     *
     * @return the file that was written
     * @throws Exception On badness
     */
    public File writeFile(String entryId, File toFileOrDirectory)
            throws Exception {
        //If this is a directory then we need to get the file name from ramadda
        if (toFileOrDirectory.isDirectory()) {
            ClientEntry entry = getEntry(entryId);
            if ((entry.getResource() == null)
                    || (entry.getResource().getPath() == null)) {
                throw new IllegalStateException(
                    "Given entry does not have a file");
            }
            String fileTail =
                RepositoryUtil.getFileTail(entry.getResource().getPath());
            toFileOrDirectory = new File(IOUtil.joinDir(toFileOrDirectory,
                    fileTail));
        }

        InputStream stream = getResourceInputStream(entryId);
        IOUtil.copyFile(stream, toFileOrDirectory);
        stream.close();

        return toFileOrDirectory;
    }


    /**
     * _more_
     *
     * @param entryId _more_
     *
     * @throws Exception _more_
     */
    public void printEntry(String entryId) throws Exception {
        ClientEntry entry = getEntry(entryId);
        System.out.println(entry.toString());
    }



    /**
     * _more_
     *
     * @param entryId _more_
     *
     * @throws Exception _more_
     */
    public void printEntryXml(String entryId) throws Exception {
        System.out.println(getEntryXml(entryId));
    }



    /**
     * _more_
     *
     * @param node _more_
     * @param tags _more_
     *
     * @throws Exception _more_
     */
    public void addTags(Element node, List<String> tags) throws Exception {
        for (String tag : tags) {
            XmlUtil.create(node.getOwnerDocument(), TAG_METADATA, node,
                           new String[] { ATTR_TYPE,
                                          "enum_tag", ATTR_ATTR1, tag });
        }
    }

    /**
     * _more_
     *
     * @param node _more_
     * @param filename _more_
     *
     * @throws Exception _more_
     */
    public void addAttachment(Element node, String filename)
            throws Exception {
        XmlUtil.create(node.getOwnerDocument(), TAG_METADATA, node,
                       new String[] { ATTR_TYPE,
                                      "content.attachment", ATTR_ATTR1,
                                      filename });
    }

    /**
     * _more_
     *
     * @param node _more_
     * @param filename _more_
     *
     * @throws Exception _more_
     */
    public void addThumbnail(Element node, String filename) throws Exception {
        Element metadataNode = XmlUtil.create(node.getOwnerDocument(),
                                   TAG_METADATA, node,
                                   new String[] { ATTR_TYPE,
                "content.thumbnail", ATTR_ATTR1, filename });
        XmlUtil.create(node.getOwnerDocument(), "attr1", metadataNode,
                       new String[] { "fileid",
                                      filename });
    }

    /**
     * _more_
     *
     * @param node _more_
     * @param fromId _more_
     * @param toId _more_
     * @param name _more_
     * @param type _more_
     *
     * @throws Exception _more_
     */
    public void addAssociation(Element node, String fromId, String toId,
                               String type)
            throws Exception {
        addAssociation(node, fromId, toId, type, "");
    }

    /**
     * _more_
     *
     * @param node _more_
     * @param fromId _more_
     * @param toId _more_
     * @param type _more_
     * @param name _more_
     *
     * @throws Exception _more_
     */
    public void addAssociation(Element node, String fromId, String toId,
                               String type, String name)
            throws Exception {
        XmlUtil.create(node.getOwnerDocument(), TAG_ASSOCIATION, node,
                       new String[] {
            ATTR_FROM, fromId, ATTR_TO, toId, ATTR_TYPE, type, ATTR_NAME, name
        });
    }





    /**
     * _more_
     *
     * @param error _more_
     * @param exc _more_
     */
    public void handleError(String error, Exception exc) {
        System.err.println(error);
        if (exc != null) {
            exc.printStackTrace();
        }
    }


    /**
     * _more_
     *
     * @param message _more_
     */
    public void handleMessage(String message) {
        System.err.println(message);
    }


    /** _more_ */
    public int idCnt = 0;

    /**
     * _more_
     *
     * @return _more_
     */
    public String createId() {
        return "dummyid_" + Math.random() + "_" + idCnt++;
    }


    /**
     * _more_
     *
     * @param parentNode _more_
     * @param parentId _more_
     * @param name _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Element makeGroupNode(Element parentNode, String parentId,
                                 String name)
            throws Exception {
        return makeGroupNode(parentNode, parentId, name, createId());
    }



    /**
     * _more_
     *
     * @param parentNode _more_
     * @param parentId _more_
     * @param name _more_
     * @param dummyId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Element makeGroupNode(Element parentNode, String parentId,
                                 String name, String dummyId)
            throws Exception {
        if (parentId.equals(ID_PREVIOUS) || (parentId.trim().length() == 0)) {
            parentId = lastId;
        }
        lastId = dummyId;

        return XmlUtil.create(parentNode.getOwnerDocument(), TAG_ENTRY,
                              parentNode, new String[] {
            ATTR_ID, dummyId, ATTR_TYPE, TYPE_GROUP, ATTR_PARENT, parentId,
            ATTR_NAME, name
        });
    }


    /**
     * _more_
     *
     * @param root _more_
     * @param name _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Element makeEntryNode(Element root, String name) throws Exception {
        return makeEntryNode(root, name, null);
    }


    /**
     * _more_
     *
     * @param root _more_
     * @param name _more_
     * @param parentId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Element makeEntryNode(Element root, String name, String parentId)
            throws Exception {
        String dummyId = createId();
        if ((parentId != null) && parentId.equals(ID_PREVIOUS)) {
            parentId = lastId;
        }
        lastId = dummyId;

        if (parentId != null) {
            return XmlUtil.create(root.getOwnerDocument(), TAG_ENTRY, root,
                                  new String[] {
                ATTR_ID, dummyId, ATTR_PARENT, parentId, ATTR_NAME, name
            });
        }

        return XmlUtil.create(root.getOwnerDocument(), TAG_ENTRY, root,
                              new String[] { ATTR_ID,
                                             dummyId, ATTR_NAME, name });
    }


    /**
     * _more_
     *
     *
     * @param parentId _more_
     * @param name _more_
     *
     * @return _more_
     */
    public boolean newGroup(String parentId, String name) {
        return newGroup(parentId, name, createId());
    }




    /**
     * _more_
     *
     * @param parentId _more_
     * @param name _more_
     * @param dummyid _more_
     *
     * @return _more_
     */
    public boolean newGroup(String parentId, String name, String dummyid) {
        try {
            if (name == null) {
                return false;
            }
            Document doc = XmlUtil.makeDocument();
            Element root = XmlUtil.create(doc, TAG_ENTRIES, null,
                                          new String[] {});
            makeGroupNode(root, parentId, name, dummyid);
            String xml     = XmlUtil.toString(root);
            List   entries = new ArrayList();
            addUrlArgs(entries);
            entries.add(new HttpFormEntry(ARG_FILE, "entries.xml",
                                          getXmlBytes(xml)));
            String[] result = doPost(URL_ENTRY_XMLCREATE, entries);

            if (result[0] != null) {
                handleError("Error creating folder:\n" + result[0], null);

                return false;
            }
            Element response = XmlUtil.getRoot(result[1]);
            if (responseOk(response)) {
                handleMessage("Folder created");

                return true;
            }
            String body = XmlUtil.getChildText(response).trim();
            handleError("Error creating folder:" + body, null);
        } catch (Exception exc) {
            handleError("Error creating folder", exc);
        }

        return false;
    }







    /**
     * _more_
     *
     * @param entries _more_
     */
    public void addUrlArgs(List entries) {
        entries.add(HttpFormEntry.hidden(ARG_SESSIONID, getSessionId()));
        //        String authToken = RepositoryUtil.hashString(getSessionId());
        //        entries.add(HttpFormEntry.hidden(ARG_AUTHTOKEN, authToken));
        entries.add(HttpFormEntry.hidden(ARG_RESPONSE, RESPONSE_XML));
        if (isAnonymous()) {
            entries.add(HttpFormEntry.hidden(ARG_ANONYMOUS, "true"));
        }
    }




    /**
     * _more_
     *
     * @return _more_
     */
    public String getSessionId() {
        if (isAnonymous()) {
            return "";
        }

        return sessionId;
    }





    /**
     * _more_
     *
     * @throws InvalidSession _more_
     */
    public void checkSession() throws InvalidSession {
        String[] msg = { "" };
        if ( !isValidSession(true, msg)) {
            throw new InvalidSession(msg[0]);
        }
    }



    /**
     * _more_
     *
     * @param doLogin _more_
     * @param msg _more_
     *
     * @return _more_
     */
    public boolean isValidSession(boolean doLogin, String[] msg) {
        if ( !isValidSession(msg)) {
            if (isAnonymous()) {
                return false;
            }
            if (doLogin) {
                return doLogin(msg);
            }

            return false;
        }

        return true;
    }


    /**
     * _more_
     *
     *
     * @param msg _more_
     * @return _more_
     *
     */
    public boolean isValidSession(String[] msg) {
        try {
            String url;
            if (isAnonymous()) {
                url = HtmlUtils.url(URL_PING.getFullUrl(),
                                    new String[] { ARG_RESPONSE,
                        RESPONSE_XML });
            } else {
                if (sessionId == null) {
                    msg[0] = "No session id";

                    return false;
                }
                url = HtmlUtils.url(URL_USER_HOME.getFullUrl(),
                                    new String[] { ARG_RESPONSE,
                        RESPONSE_XML, ARG_SESSIONID, sessionId });
            }
            //            System.err.println("isValidSession:" + url);
            String contents = IOUtil.readContents(url, getClass());
            //            System.err.println ("contents:" + contents);
            Element root = XmlUtil.getRoot(contents);
            if (responseOk(root)) {
                return true;
            } else {
                msg[0] = XmlUtil.getChildText(root).trim();

                return false;
            }
        } catch (Exception exc) {
            System.err.println("error:" + exc);
            msg[0] = "xxx Could not connect to server: " + getHostname();

            return false;
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasSession() {
        return sessionId != null;
    }


    /**
     * _more_
     *
     * @param root _more_
     *
     * @return _more_
     */
    public boolean responseOk(Element root) {
        return XmlUtil.getAttribute(root, ATTR_CODE).equals("ok");
    }



    /**
     * _more_
     *
     *
     * @param msg _more_
     * @return _more_
     *
     */
    public boolean doLogin(String[] msg) {
        if (isAnonymous()) {
            return true;
        }
        try {
            //            System.err.println ("RepositoryClient: doLogin");
            //first get the basic information including the ssl port
            getInfo();

            List entries = new ArrayList();
            entries.add(HttpFormEntry.hidden(ARG_RESPONSE, RESPONSE_XML));
            entries.add(HttpFormEntry.hidden(ARG_AGREE, "true"));
            entries.add(HttpFormEntry.hidden(ARG_USER_PASSWORD,
                                             getPassword()));
            entries.add(HttpFormEntry.hidden(ARG_USER_ID, getUser()));
            doTimeout();
            String[] result = doPost(URL_USER_LOGIN, entries);
            if (result[0] != null) {
                msg[0] = "Error logging in: " + result[0];

                return false;
            }
            String contents = result[1];
            //            System.err.println(contents);
            Element root = XmlUtil.getRoot(contents);
            String  body = XmlUtil.getChildText(root).trim();
            if (responseOk(root)) {
                sessionId = body;

                return true;
            } else {
                msg[0] = body;

                return false;
            }
        } catch (java.io.IOException exc) {
            exc.printStackTrace();
            msg[0] = "Could not connect to server: " + getHostname();

        } catch (Exception exc) {
            msg[0] = "An error occurred: " + exc + "\n";
        }

        return false;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public boolean login() {
        try {
            String[] msg = { "login ok!", "login fail!" };
            if (doLogin(msg)) {
                return true;
            }
        } catch (Exception e) {
            System.out.println("login fail !");
        }

        return false;
    }



    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void getInfo() throws Exception {
        String url = HtmlUtils.url(URL_INFO.getFullUrl(),
                                   new String[] { ARG_RESPONSE,
                RESPONSE_XML });

        //        System.err.println("url:" + url);
        doTimeout();
        String contents = IOUtil.readContents(url, getClass());
        callTimestamp++;

        //        System.err.println(contents);
        Element root = XmlUtil.getRoot(contents);

        String sslPortProp = XmlUtil.getGrandChildText(root,
                                 ServerInfo.TAG_INFO_SSLPORT);
        if ((sslPortProp != null) && (sslPortProp.trim().length() > 0)) {
            sslPort = new Integer(sslPortProp.trim()).intValue();
        }

        title = XmlUtil.getGrandChildText(root, ServerInfo.TAG_INFO_TITLE);
        description = XmlUtil.getGrandChildText(root,
                ServerInfo.TAG_INFO_DESCRIPTION);
        //        System.err.println (sslPort + "  "+ title +" " + description);
        setHttpsPort(sslPort);
    }


    /**
     *  Method for encoding to xml the password. This simply obfuscates what is saved to disk
     *
     *  @param value The new value
     */
    public void setTmp(byte[] value) {
        if (value == null) {
            password = null;
        } else {
            password =
                new String(RepositoryUtil.decodeBase64(new String(value)));
        }
    }


    /**
     *  Method for encoding to xml the password. This simply obfuscates what is saved to disk
     *
     *  @return The Password
     */
    public byte[] getTmp() {
        if (password == null) {
            return null;
        }

        return RepositoryUtil.encodeBase64(password.getBytes()).getBytes();
    }

    /**
     *  Set the User property.
     *
     *  @param value The new value for User
     */
    public void setUser(String value) {
        user = value;
    }

    /**
     *  Get the User property.
     *
     *  @return The User
     */
    public String getUser() {
        return user;
    }


    /**
     * Set the Name property.
     *
     * @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * Get the Name property.
     *
     * @return The Name
     */
    public String getName() {
        return name;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected String getPassword() {
        return password;
    }

    /**
     * _more_
     *
     * @param s _more_
     */
    protected void setPassword(String s) {
        password = s;
    }

    /**
     *  Get the Anonymous property.
     *
     *  @return The Anonymous
     */
    public boolean isAnonymous() {
        return (user != null)
               && ((user.trim().length() == 0) || user.equals("anonymous"));
    }


    /**
     *  Set the DefaultGroup property.
     *
     *  @param value The new value for DefaultGroup
     */
    public void setDefaultGroupId(String value) {
        defaultGroupId = value;
    }

    /**
     *  Get the DefaultGroup property.
     *
     *  @return The DefaultGroup
     */
    public String getDefaultGroupId() {
        return defaultGroupId;
    }

    /**
     *  Set the DefaultGroupName property.
     *
     *  @param value The new value for DefaultGroupName
     */
    public void setDefaultGroupName(String value) {
        defaultGroupName = value;
    }

    /**
     *  Get the DefaultGroupName property.
     *
     *  @return The DefaultGroupName
     */
    public String getDefaultGroupName() {
        return defaultGroupName;
    }




    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {

        //        if (rc.login()) {
        //            rc.uploadFile("testImg.jpg", "", "69dff72c-c0ea-479e-a4dd-22cd4dc5bba6", "d:/test1.jpg");
        //        }



        if (args.length < 3) {
            usage("Incorrect number of arguments");
        }
        try {
            if (args[0].startsWith("-") || args[1].startsWith("-")
                    || args[2].startsWith("-")) {
                usage("Incorrect argument");
            }
            RepositoryClient client = new RepositoryClient(new URL(args[0]),
                                          args[1], args[2]);
            client.preProcessArgs(args);
            String[] msg = { "" };
            if ( !client.isValidSession(true, msg)) {
                System.err.println("Error: invalid session:" + msg[0]);

                return;
            }

            //            client.writeFile("46d283c8-b852-4a6f-a6e6-055b7ac19fe9",
            //                             new File("test.nc"));
            client.processCommandLine(args);
        } catch (Exception exc) {
            System.err.println("Error:" + exc);
            exc.printStackTrace();
        }

    }



    /**
     * _more_
     *
     * @param arg _more_
     * @param desc _more_
     *
     * @return _more_
     */
    public static String argLine(String arg, String desc) {
        return "\t" + arg + " " + desc + "\n";
    }

    /**
     * _more_
     *
     * @param msg _more_
     */
    public static void usage(String msg) {
        System.err.println(msg);
        System.err.println(
            "Usage: RepositoryClient <server url> <user id> <password> <arguments>");
        System.err.println(
            "e.g,  RepositoryClient http://localhost:8080/repository <user id> <password> <arguments>");
        System.err.println("Where arguments are:\nFor fetching: \n"
                + argLine(CMD_PRINT, "<entry id> Create and print the given entry")
                + argLine(CMD_PRINTXML, " <entry id> Print out the xml for the given entry id")
                + argLine(CMD_FETCH, "<entry id> <destination file or directory>")
                + argLine(CMD_SEARCH, "<any number of search arguments pairs>")
                + "\n" + "For creating a new folder:\n"
                + argLine(CMD_FOLDER, "<folder name> <parent folder id (see below)>")
                + "\n" + "For uploading files:\n"
                + argLine(CMD_IMPORT, "entries.xml <parent entry id or path>")
                + "\n"
                + argLine(CMD_FILE, "<entry name> <file to upload> <parent folder id (see below)>")
                + "\n"
                + argLine(CMD_FILES, "<parent folder id (see below)> <one or more files to upload>")
                + "\n"
                + argLine(CMD_TIMEOUT, "<timeout in seconds for server info and login attempts>")
                + "\n"
                + "The following arguments get applied to the previously created folder or file:\n"
                + "\t-description <entry description>\n"
                + "\t-attach <file to attach>\n"
                + "\t-addmetadata (Add full metadata to entry)\n"
                + "\t-addshortmetadata (Add spatial/temporal metadata to entry)\n"
                + "\n" + "Miscellaneous:\n"
                + "\t-url (login to server and access url)\n"
                + "\t-debug (print out the generated xml)\n"
                + "\t-exit (exit without adding anything to the repository\n");


        System.err.println(
            "Note: the  <parent folder id> can be an identifier from a existing folder in the repository or it can be \"previous\" which will use the id of the previously specified folder\n"
            + "For example you could do:\n"
            + " ...  -folder \"Some new folder\" \"some id from the repository\" -file \"\" somefile1.nc -file somefile2.nc \"previous\" -folder \"some other folder\" \"previous\" -file \"\" someotherfile.nc \"previous\"\n" + "This results in the heirarchy:\n" + "Some new folder\n" + "\tsomefile1.nc\n" + "\tsomefile2.nc\n" + "\tsome other folder\n" + "\t\tsomeotherfile.nc\n");

        System.exit(1);
    }




    /**
     * _more_
     *
     * @param file _more_
     * @param parent _more_
     *
     * @throws Exception _more_
     */
    private void importFile(File file, String parent) throws Exception {
        List<HttpFormEntry> postEntries = new ArrayList<HttpFormEntry>();
        addUrlArgs(postEntries);
        postEntries.add(HttpFormEntry.hidden(ARG_GROUP, parent));
        postEntries.add(
            new HttpFormEntry(
                ARG_FILE, IOUtil.getFileTail(file.toString()),
                IOUtil.readBytes(new FileInputStream(file))));
        String[] result = doPost(URL_ENTRY_XMLCREATE, postEntries);
        if (result[0] != null) {
            System.err.println("Error:" + result[0]);

            return;
        }

        System.err.println("result:" + result[1]);
        Element response = XmlUtil.getRoot(result[1]);

        String  body     = XmlUtil.getChildText(response).trim();
        if (responseOk(response)) {
            System.err.println("OK:" + body);
        } else {
            System.err.println("Error:" + body);
        }



    }


    /**
     * _more_
     *
     * @param arg _more_
     * @param i _more_
     * @param length _more_
     * @param howMany _more_
     */
    private void assertArgs(String arg, int i, int length, int howMany) {
        //        System.err.println(i + " " + length + " " + howMany);
        if (i >= length - howMany) {
            usage("Bad argument: " + arg);
        }
    }



    /**
     * _more_
     *
     * @param url _more_
     *
     * @throws Exception _more_
     */
    private void hitUrl(String url) throws Exception {
        checkSession();
        url = HtmlUtils.url(url, new String[] {
            ARG_RESPONSE, RESPONSE_XML, ARG_SESSIONID, getSessionId(),
            /*ARG_AUTHTOKEN, RepositoryUtil.hashString(getSessionId())*/
        }, false);
        System.err.println("url:" + url);
        String  xml  = IOUtil.readContents(url, getClass());
        Element root = XmlUtil.getRoot(xml);
        String  body = XmlUtil.getChildText(root).trim();
        if (responseOk(root)) {
            System.err.println("OK:" + body);
        } else {
            System.err.println("ERROR:" + body);
        }
        //        System.out.println(xml);
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void xxxdoSearch() throws Exception {
        RequestUrl URL_ENTRY_SEARCH = new RequestUrl(this, "/search/do",
                                          useSsl());
        List<String> argList = new ArrayList<String>();
        String       output  = "xml.xml";
        for (String[] args : searchArgs) {
            if (args[0].startsWith("-")) {
                args[0] = args[0].substring(1);
            }
            if (args[0].equals("output")) {
                output = args[1];
            }
            argList.add(args[0]);
            argList.add(args[1]);
        }
        checkSession();
        argList.add(ARG_OUTPUT);
        argList.add(output);
        argList.add(ARG_SESSIONID);
        argList.add(getSessionId());
        String url = HtmlUtils.url(URL_ENTRY_SEARCH.getFullUrl(), argList);
        String xml = IOUtil.readContents(url, getClass());
        System.out.println(xml);
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public void preProcessArgs(String[] args) throws Exception {
        for (int i = 3; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals(CMD_TIMEOUT)) {
                if (i >= args.length - 1) {
                    usage("Bad argument: " + arg);
                }
                timeout = new Integer(args[++i]);

                continue;
            }
        }
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public void processCommandLine(String[] args) throws Exception {

        String     xmlFile   = null;
        List<File> files     = new ArrayList<File>();
        Element    root      = null;
        Element    entryNode = null;
        Document   doc       = null;

        doc  = XmlUtil.makeDocument();
        root = XmlUtil.create(doc, TAG_ENTRIES, null, new String[] {});

        int entryCnt = 0;
        for (int i = 3; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals(CMD_SEARCH)) {
                doingSearch = true;

                continue;
            }

            if (doingSearch) {
                if (i >= args.length - 1) {
                    usage("Bad search argument: " + arg);
                }
                searchArgs.add(new String[] { arg, args[i + 1] });
                i++;

                continue;
            }

            if (arg.equals(CMD_TIMEOUT)) {
                ++i;

                continue;
            }

            if (arg.equals(CMD_FETCH)) {
                if (i >= args.length - 1) {
                    usage("Bad argument: " + arg);
                }
                if (i >= args.length - 2) {
                    usage("Bad argument: " + arg);
                }
                File f = writeFile(args[i + 1], new File(args[i + 2]));
                System.err.println("Wrote file to:" + f);

                return;
            }

            if (arg.equals(CMD_PRINT)) {
                if (i >= args.length - 1) {
                    usage("Bad argument: " + arg);
                }
                printEntry(args[i + 1]);

                return;
            }

            if (arg.equals(CMD_PRINTXML)) {
                if (i >= args.length - 1) {
                    usage("Bad argument: " + arg);
                }
                printEntryXml(args[i + 1]);

                return;
            }

            if (arg.equals(CMD_IMPORT)) {
                assertArgs(arg, i, args.length, 2);
                importFile(new File(args[i + 1]), args[i + 2]);

                return;
            }

            if (arg.equals(CMD_URL)) {
                if (i == args.length) {
                    usage("Bad argument: " + arg);
                }
                hitUrl(args[++i]);

                return;
            }

            if (arg.equals(CMD_DEBUG)) {
                System.out.println(XmlUtil.toString(root));
            } else if (arg.equals(CMD_EXIT)) {
                return;
            } else if (arg.equals(CMD_FOLDER)) {
                if (i == args.length) {
                    usage("Bad argument: " + arg);
                }
                entryCnt++;
                String name     = args[++i];
                String parentId = args[++i];

                entryNode = makeGroupNode(root, parentId, name);

            } else if (arg.equals(CMD_FILES)) {
                if (i >= args.length - 2) {
                    usage("Bad argument: " + arg);
                }
                String parentId = args[++i];

                for (i++; i < args.length; i++) {
                    File f = new File(args[i]);
                    if ( !f.exists()) {
                        usage("File does not exist:" + f);
                    }
                    String name = f.getName();
                    entryCnt++;
                    entryNode = makeEntryNode(root, name, parentId);
                    entryNode.setAttribute(ATTR_FILE,
                                           IOUtil.getFileTail(f.toString()));
                    files.add(f);
                }
            } else if (arg.equals(CMD_FILE)) {
                if (i >= args.length - 2) {
                    usage("Bad argument: " + arg);
                }
                String name = args[++i];
                File   f    = new File(args[++i]);
                if ( !f.exists()) {
                    usage("File does not exist:" + f);
                }
                if (name.length() == 0) {
                    name = IOUtil.getFileTail(f.toString());
                }
                String parentId = args[++i];
                entryCnt++;
                entryNode = makeEntryNode(root, name, parentId);
                entryNode.setAttribute(ATTR_FILE,
                                       IOUtil.getFileTail(f.toString()));
                files.add(f);
            } else if (arg.equals("-localfile")) {
                if (i == args.length) {
                    usage("Bad argument: " + arg);
                }
                i++;
                File f = new File(args[i]);
                //                if(!f.exists()) usage("Bad file:" + args[i]);
                entryCnt++;
                entryNode = makeEntryNode(root, IOUtil.getFileTail(args[i]));
                entryNode.setAttribute(ATTR_LOCALFILE, f.getPath());
            } else if (arg.equals("-localfiletomove")) {
                if (i == args.length) {
                    usage("Bad -localfiletomove argument");
                }
                i++;
                File f = new File(args[i]);
                //                if(!f.exists()) usage("Bad file:" + args[i]);
                entryCnt++;
                entryNode = makeEntryNode(root, IOUtil.getFileTail(args[i]));
                entryNode.setAttribute(ATTR_LOCALFILETOMOVE, f.getPath());

            } else if (arg.equals("-description")) {
                checkEntryNode(entryNode, "-description");
                if (i == args.length) {
                    usage("Bad -description argument");
                }
                i++;
                Element descNode = XmlUtil.create(doc, TAG_DESCRIPTION,
                                       entryNode);
                descNode.appendChild(XmlUtil.makeCDataNode(doc, args[i],
                        false));

            } else if (arg.equals("-addmetadata")) {
                checkEntryNode(entryNode, "-addmetadata");
                entryNode.setAttribute(ATTR_ADDMETADATA, "true");
            } else if (arg.equals("-addshortmetadata")) {
                checkEntryNode(entryNode, "-addshortmetadata");
                entryNode.setAttribute(ATTR_ADDSHORTMETADATA, "true");
            } else if (arg.equals("-attach")) {
                checkEntryNode(entryNode, "-attach");
                if (i == args.length) {
                    usage("Bad -file argument");
                }
                i++;
                File f = new File(args[i]);
                if ( !f.exists()) {
                    usage("File does not exist:" + args[i]);
                }
                if (entryNode == null) {
                    usage("Need to specify a -file first");
                }
                addAttachment(entryNode, IOUtil.getFileTail(f.toString()));
                files.add(f);
            } else {
                if ( !new File(args[i]).exists()) {
                    usage("Unknown argument:" + args[i]);
                }
                files.add(new File(args[i]));
            }
        }

        if (doingSearch) {
            //            doSearch();
            return;
        }


        if (entryCnt == 0) {
            usage("");
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream       zos = new ZipOutputStream(bos);

        //Write the xml if we have it
        if (root != null) {
            //Tack on a utf-8 header so Chinese characters can encode properly
            String header = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>";
            String xml    = header + "\n" + XmlUtil.toString(root);
            //System.out.println(xml);
            zos.putNextEntry(new ZipEntry("entries.xml"));
            byte[] bytes = getXmlBytes(xml);
            zos.write(bytes, 0, bytes.length);
            zos.closeEntry();

        }
        //Now write the files
        for (File f : files) {
            zos.putNextEntry(new ZipEntry(IOUtil.getFileTail(f.toString())));
            byte[] bytes = IOUtil.readBytes(new FileInputStream(f));
            zos.write(bytes, 0, bytes.length);
            zos.closeEntry();
        }
        zos.close();
        bos.close();

        List<HttpFormEntry> postEntries = new ArrayList<HttpFormEntry>();
        addUrlArgs(postEntries);
        postEntries.add(new HttpFormEntry(ARG_FILE, "entries.zip",
                                          bos.toByteArray()));
        String[] result = doPost(URL_ENTRY_XMLCREATE, postEntries);
        if (result[0] != null) {
            System.err.println("Error:" + result[0]);

            return;
        }

        System.err.println("result:" + result[1]);
        Element response = XmlUtil.getRoot(result[1]);

        String  body     = XmlUtil.getChildText(response).trim();
        if (responseOk(response)) {
            System.err.println("OK:" + body);
        } else {
            System.err.println("Error:" + body);
        }

    }


    /**
     * _more_
     *
     * @param entryNode _more_
     * @param arg _more_
     */
    private void checkEntryNode(Element entryNode, String arg) {
        if (entryNode == null) {
            usage("You need to create an entry first (either with -folder or -file) when using the argument:"
                  + arg);
        }
    }



    /**
     * _more_
     *
     * @param entryId _more_
     *
     * @return _more_
     */
    public boolean deleteEntry(String entryId) {
        try {
            if (entryId == null) {
                return false;
            }
            List entries = new ArrayList();
            addUrlArgs(entries);
            entries.add(HttpFormEntry.hidden(ARG_ENTRYID, entryId));
            entries.add(HttpFormEntry.hidden(ARG_DELETE_CONFIRM, "OK"));
            String[] result = doPost(URL_ENTRY_DELETE, entries);
            if (result[0] != null) {
                handleError("Error deleting entry:\n" + result[0], null);

                return false;
            }

            return true;
        } catch (Exception exc) {
            handleError("Error deleting entry", exc);
        }

        return false;

    }




    /**
     * Class InvalidSession _more_
     *
     *
     * @author RAMADDA Development Team
     */
    public static class InvalidSession extends RuntimeException {

        /**
         * _more_
         *
         * @param message _more_
         */
        public InvalidSession(String message) {
            super(message);
        }
    }

    /**
     * _more_
     *
     * @param parentId _more_
     * @param name _more_
     * @param localfilepath _more_
     *
     * @return _more_
     */
    public boolean newLocalfiles(String parentId, String name,
                                 String localfilepath) {
        try {
            if (name == null) {
                return false;
            }
            String   dummyid = createId();
            Document doc     = XmlUtil.makeDocument();
            Element root = XmlUtil.create(doc, TAG_ENTRIES, null,
                                          new String[] {});
            Element ele = XmlUtil.create(root.getOwnerDocument(), TAG_ENTRY,
                                         root, new String[] {
                ATTR_ID, dummyid, ATTR_TYPE, ATTR_LOCALFILES, ATTR_PARENT,
                parentId, ATTR_NAME, name, ATTR_LOCALFILE_PATH, localfilepath
            });
            String xml     = XmlUtil.toString(root);
            List   entries = new ArrayList();
            addUrlArgs(entries);
            entries.add(new HttpFormEntry(ARG_FILE, "entries.xml",
                                          xml.getBytes()));
            String[] result = doPost(URL_ENTRY_XMLCREATE, entries);

            if (result[0] != null) {
                handleError("Error creating folder:\n" + result[0], null);

                return false;
            }
            Element response = XmlUtil.getRoot(result[1]);
            if (responseOk(response)) {
                handleMessage("Folder created");

                return true;
            }
            String body = XmlUtil.getChildText(response).trim();
            handleError("Error creating folder:" + body, null);
        } catch (Exception exc) {
            handleError("Error creating folder", exc);
        }

        return false;
    }





    /**
     * _more_
     *
     * @param entryName _more_
     * @param wikitext _more_
     * @param parent _more_
     *
     * @return _more_
     */
    public boolean newWiki(String entryName, String wikitext, String parent) {
        checkSession();
        try {
            Document doc = XmlUtil.makeDocument();
            Element root = XmlUtil.create(doc, TAG_ENTRIES, null,
                                          new String[] {});
            Element entryNode = XmlUtil.create(doc, TAG_ENTRY, root,
                                    new String[] {});

            /*
             * name and id
             */
            entryNode.setAttribute(ATTR_NAME, entryName);
            entryNode.setAttribute(ATTR_ID, entryName);

            /*
             * type
             */
            entryNode.setAttribute(ATTR_TYPE, "wikipage");
            /*
             * wikitext
             */
            Element descNode = XmlUtil.create(doc, TAG_WIKITEXT, entryNode);
            descNode.appendChild(XmlUtil.makeCDataNode(doc, wikitext, false));
            /*
             * parent
             */
            entryNode.setAttribute(ATTR_PARENT, parent);

            /*
             * addmetadata
             */
            //entryNode.setAttribute(ATTR_ADDMETADATA, "true");

            /*
             * write the xml definition into the zip file
             */
            String              xml         = XmlUtil.toString(root);
            //        System.out.println(xml);


            List<HttpFormEntry> postEntries = new ArrayList<HttpFormEntry>();

            addUrlArgs(postEntries);


            postEntries.add(new HttpFormEntry(ARG_FILE, "entries.xml",
                    getXmlBytes(xml)));

            String[] result = doPost(URL_ENTRY_XMLCREATE, postEntries);

            if (result[0] != null) {
                handleError("Error creating file:\n" + result[0], null);

                return false;
            }
            Element response = XmlUtil.getRoot(result[1]);
            if (responseOk(response)) {
                handleMessage("file created");

                return true;
            }
            String body = XmlUtil.getChildText(response).trim();
            handleError("Error creating file:" + body, null);
        } catch (Exception exc) {
            handleError("Error creating file", exc);
        }

        return false;
    }


    /**
     *  encode the text as utf-8
     *
     * @param xml _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private byte[] getXmlBytes(String xml) throws Exception {
        return xml.getBytes("UTF-8");
    }


    /**
     * Class EntryErrorException _more_
     *
     *
     * @author RAMADDA Development Team
     */
    public static class EntryErrorException extends RuntimeException {

        /**
         * _more_
         *
         * @param message _more_
         */
        public EntryErrorException(String message) {
            super(message);
        }
    }


}
