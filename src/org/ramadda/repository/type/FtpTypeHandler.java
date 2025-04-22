/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.type;

import org.ramadda.repository.util.SelectInfo;
import org.apache.commons.net.ftp.*;

import org.ramadda.repository.*;

import org.ramadda.util.IO;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * Class TypeHandler _more_
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public class FtpTypeHandler extends ExtensibleGroupTypeHandler {

    public static final int COL_SERVER = 0;

    public static final int COL_BASEDIR = 1;

    public static final int COL_USER = 2;

    public static final int COL_PASSWORD = 3;

    public static final int COL_MAXSIZE = 4;

    public static final int COL_FILE_PATTERN = 5;

    public static final int COL_DATE_PATTERN = 6;

    public static final int COL_DATE_FORMAT = 7;

    public FtpTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    public TypeHandler getTypeHandlerForCopy(Entry entry) throws Exception {
        if (getEntryManager().isSynthEntry(entry.getId())) {
            return getRepository().getTypeHandler(TypeHandler.TYPE_FILE);
        }

        return this;
    }

    public boolean canBeCreatedBy(Request request) {
        return request.getUser().getAdmin();
    }

    public boolean isSynthType() {
        return true;
    }

    @Override
    public String getEntryIconUrl(Request request, Entry entry)
            throws Exception {
        if (entry.isGroup()) {
            if ( !getEntryManager().isSynthEntry(entry.getId())) {
                return getIconUrl(ICON_FTP);
            }

            return getIconUrl(ICON_FTP);
        }

        return super.getIconUrl(request, entry);
    }

    public boolean canDownload(Request request, Entry entry)
            throws Exception {
        Entry mainEntry = getMainEntry(entry.getId());
        if (mainEntry == null) {
            return false;
        }
        double maxSize = entry.getDoubleValue(request, COL_MAXSIZE,0);
        return entry.getResource().getFileSize() < 1000000 * maxSize;
    }

    private Entry getMainEntry(String id) throws Exception {
        String[] pair   = getEntryManager().getSynthId(id);
        Entry    parent = getEntryManager().getEntry(null, pair[0]);

        return parent;
    }

    public File getFileForEntry(Request request, Entry entry) {
        try {
            File  badFile = new File("badfile");
            Entry parent  = getMainEntry(entry.getId());
            if (parent == null) {
                System.err.println("Could not find main entry");

                return badFile;
            }
	    double maxSize = parent.getDoubleValue(request, COL_MAXSIZE,0);
            String server = parent.getStringValue(request, COL_SERVER,"");
            if (entry.getResource().getFileSize() > 1000000 * maxSize) {
                //                System.err.println("FtpTypeHandler: Bad size "
                //                                   + entry.getResource().getFileSize()+" " +
                //                                   entry.getResource());
                return badFile;
            }
            FTPClient ftpClient = null;
            try {
                String[] pair = getEntryManager().getSynthId(entry.getId());
                MyFTPFile myFtpFile = getFileFromId(request,parent, pair[1],parent.getStringValue(request,COL_BASEDIR,""));
                ftpClient = getFtpClient(request,parent);
                if (ftpClient == null) {
                    System.err.println("no ftp client ");

                    return badFile;
                }
                //                String path = entry.getResource().getPath();
                String path   = myFtpFile.path;
                String prefix = "ftp://" + server;
                if (path.startsWith(prefix)) {
                    path = path.substring(prefix.length());
                }
                String cacheFileName = java.net.URLEncoder.encode("ftp:"
                                           + server + ":"
                                           + path, "UTF-8");
                File cacheFile =
                    getStorageManager().getCacheFile(cacheFileName);
                if (cacheFile.exists()) {
                    return cacheFile;
                }

                //                System.err.println("Fetching:" + path);
                //                System.err.println("writing to:" + cacheFile);
                OutputStream fos =
                    getStorageManager().getUncheckedFileOutputStream(
                        cacheFile);
                if (ftpClient.retrieveFile(path, fos)) {
                    fos.flush();
                    fos.close();

                    return cacheFile;
                }

                //                System.err.println ("BAD FILE");
                return badFile;
            } finally {
                if (ftpClient != null) {
                    closeConnection(ftpClient);
                }
            }
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }

    }

    public String getPathFromId(String id, String baseDir) throws Exception {
        if ((id == null) || (id.length() == 0)) {
            return baseDir;
        }

        return new String(Utils.decodeBase64(id));
    }

    public String getSynthId(Entry parentEntry, String rootDirPath,
                             String parentPath, FTPFile file) {
        String id = parentPath + "/" + file.getName();
        id = Utils.encodeBase64(id).replace("\n", "");

        return Repository.ID_PREFIX_SYNTH + parentEntry.getId() + ":" + id;
    }

    public String getSynthId(Entry parentEntry, String rootDirPath,
                             String parentPath) {
        String id = parentPath;
        id = Utils.encodeBase64(id).replace("\n", "");

        return Repository.ID_PREFIX_SYNTH + parentEntry.getId() + ":" + id;
    }

    private static boolean isDir(FTPClient ftpClient, String path)
            throws Exception {
        boolean isDir = false;
        //A hack but assume anything with a "." is not a directory
        //The problem is how to determine if the path is a directory
        //If we do changeWorkingDir for every file this becomes very 
        //expensive

        if (path.indexOf(".") < 0) {
            isDir = ftpClient.changeWorkingDirectory(path);
        }

        return isDir;
    }

    @Override
    public List<String> getSynthIds(Request request, SelectInfo select, Entry mainEntry,
                                    Entry parentEntry, String synthId)
            throws Exception {
        long         t0      = System.currentTimeMillis();
        List<String> ids     = new ArrayList<String>();

        String baseDir  = mainEntry.getStringValue(request, COL_BASEDIR,"");
        String       path    = getPathFromId(synthId, baseDir);

        /*        boolean descending = !request.get(ARG_ASCENDING, false);
        if (request.getString(ARG_ORDERBY, "").equals("name")) {
            files = IOUtil.sortFilesOnName(files, descending);
        } else {
            files = IOUtil.sortFilesOnAge(files, descending);
            }*/
        long      t1        = System.currentTimeMillis();

        FTPClient ftpClient = getFtpClient(request,mainEntry);
        if (ftpClient == null) {
            return ids;
        }
        long t2 = System.currentTimeMillis();
        //        System.err.println ("getFtpClient:" + (t2-t1));

        try {
            String pattern = mainEntry.getStringValue(request, COL_FILE_PATTERN,null);
            if (stringDefined(pattern)) {
                pattern = null;
            }
            boolean isDir = ftpClient.changeWorkingDirectory(path);
            if (isDir) {
                boolean checkReadme = parentEntry.getDescription().length()
                                      == 0;
                checkReadme = false;
                long      t3    = System.currentTimeMillis();
                FTPFile[] files = ftpClient.listFiles(path);
                long      t4    = System.currentTimeMillis();
                //                System.err.println ("listFiles:" + (t4-t3));

                for (int i = 0; i < files.length; i++) {
                    String name = files[i].getName().toLowerCase();
                    if ((pattern != null) && !name.matches(pattern)) {
                        continue;
                    }
                    if (checkReadme) {
                        if (name.equals("readme")
                                || name.equals("readme.txt")) {
                            try {
                                InputStream fis =
                                    ftpClient.retrieveFileStream(path + "/"
                                        + files[i].getName());
                                if (fis != null) {
                                    String desc = HU.entityEncode(
                                                      IOUtil.readInputStream(
                                                          fis));
                                    parentEntry.setDescription(
                                        HU.pre(desc));
                                    fis.close();
                                    ftpClient.completePendingCommand();
                                }
                            } catch (Exception exc) {
                                //                            exc.printStackTrace();
                            }
                        }
                    }

                    putCache(mainEntry, path + "/" + files[i].getName(),
                             files[i]);
                    ids.add(getSynthId(mainEntry, baseDir, path, files[i]));
                }
            }
        } finally {
            closeConnection(ftpClient);
        }
        long t5 = System.currentTimeMillis();

        //        System.err.println ("getSynthIds:" + (t5-t0));
        return ids;
    }

    private static void closeConnection(FTPClient ftpClient) {
        try {
            ftpClient.logout();
        } catch (Exception exc) {}
        try {
            ftpClient.disconnect();
        } catch (Exception exc) {}
    }

    public static String test(String server, String baseDir, String user,
                              String password)
            throws Exception {
        FTPClient ftpClient = new FTPClient();
        try {
            String file = baseDir;
            ftpClient.connect(server);
            //System.out.print(ftp.getReplyString());
            ftpClient.login(user, password);
            //            System.out.print(ftpClient.getReplyString());
            int reply = ftpClient.getReplyCode();
            if ( !FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
                System.err.println("FTP server refused connection.");

                return null;
            }
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();

            boolean isDir = isDir(ftpClient, file);
            //            System.err.println("file:" + file + " is dir: " + isDir);

            if (isDir) {
                FTPFile[] files = ftpClient.listFiles(file);
                for (int i = 0; i < files.length; i++) {
                    //                    System.err.println ("f:" + files[i].getName() + " " + files[i].isDirectory() + "  " + files[i].isFile());
                }
            } else {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                if (ftpClient.retrieveFile(file, bos)) {
                    //                    System.err.println(new String(bos.toByteArray()));
                } else {
                    throw new IOException("Unable to retrieve file:" + file);
                }
            }

            return "";
        } finally {
            closeConnection(ftpClient);
        }
    }

    private FTPClient getFtpClient(Request request,Entry parentEntry) throws Exception {
	String server   = parentEntry.getStringValue(request,COL_SERVER,"");
        String baseDir  = parentEntry.getStringValue(request,COL_BASEDIR,"");
        String user     = parentEntry.getStringValue(request,COL_USER,"");
        String password = parentEntry.getStringValue(request,COL_PASSWORD,"");
        if (password != null) {
            password =
                getRepository().getPageHandler().processTemplate(password,
                    false);
        } else {
            password = "";
        }
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(server);
            if (user != null) {
                ftpClient.login(user, password);
            }
            int reply = ftpClient.getReplyCode();
            if ( !FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
                System.err.println("FTP server refused connection.");

                return null;
            }
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();

            return ftpClient;
        } catch (Exception exc) {
            System.err.println("Could not connect to ftp server:" + server
                               + "\nError:" + exc);

            return null;
        }
    }

    private Hashtable<String, Hashtable> cache = new Hashtable<String,
                                                     Hashtable>();

    private int cacheCnt = 0;

    private void putCache(Entry entry, String path, FTPFile file) {
        if (cacheCnt++ > 1000) {
            cache    = new Hashtable<String, Hashtable>();
            cacheCnt = 0;
        }
        getCache(entry).put(path, file);
    }

    private Hashtable<String, FTPFile> getCache(Entry parentEntry) {
        Hashtable<String, FTPFile> map =
            (Hashtable<String, FTPFile>) cache.get(parentEntry.getId());
        if (map == null) {
            cache.put(parentEntry.getId(),
                      map = new Hashtable<String, FTPFile>());
        }

        //TODO:CHECK SIZE and flush
        return map;
    }

    public MyFTPFile getFileFromId(Request request, Entry parentEntry, String id,
                                   String baseDir)
            throws Exception {
        String path;
        if ((id == null) || (id.length() == 0)) {
            FTPFile file = new FTPFile();
            file.setName(baseDir);
            file.setType(FTPFile.DIRECTORY_TYPE);

            return new MyFTPFile(file, baseDir);
        } else {
            path = new String(Utils.decodeBase64(id));
        }
        FTPFile ftpFile = getCache(parentEntry).get(path);
        if (ftpFile != null) {
            return new MyFTPFile(ftpFile, path);
        }

        FTPClient ftpClient = getFtpClient(request,parentEntry);
        if (ftpClient == null) {
            return null;
        }

        //        xxx
        try {
            boolean isDir = isDir(ftpClient, path);
            if (isDir) {
                File   tmp    = new File(path);
                String parent = tmp.getParent().replace("\\", "/");
                //                System.err.println("getFileFromId path=" + path +" parent:" + parent);
                String    name              = tmp.getName();
                FTPFile[] files             = ftpClient.listFiles(parent);
                MyFTPFile lookingForThisOne = null;
                for (int i = 0; i < files.length; i++) {
                    String childPath = parent + "/" + files[i].getName();
                    putCache(parentEntry, childPath, files[i]);
                    if (files[i].getName().equals(name)) {
                        lookingForThisOne = new MyFTPFile(files[i],
                                childPath);
                    }
                }
                if (lookingForThisOne != null) {
                    return lookingForThisOne;
                }
                System.err.println("Could not find directory:" + name
                                   + " path:" + path);

                return null;
            } else {
                //                System.err.println("getFileFromId path=" + path);
                FTPFile[] files = ftpClient.listFiles(path);
                if (files.length == 1) {
                    putCache(parentEntry, path, files[0]);

                    return new MyFTPFile(files[0], path);
                } else {
                    System.err.println(
                        "Got bad # of files when getting file:"
                        + files.length + "  " + path);
                }

            }

            return null;

        } finally {
            closeConnection(ftpClient);
        }

    }

    public Entry makeSynthEntry(Request request, Entry parentEntry, String id)
            throws Exception {
        String    baseDir   = parentEntry.getStringValue(request,COL_BASEDIR,"");
        String    server    = parentEntry.getStringValue(request,COL_SERVER,"");
        MyFTPFile myFtpFile = getFileFromId(request,parentEntry, id, baseDir);
        if (myFtpFile == null) {
            return null;
        }
        if (myFtpFile.path.equals(baseDir)) {
            return parentEntry;
        }
        FTPFile     ftpFile = myFtpFile.ftpFile;
        TypeHandler handler = (ftpFile.isDirectory()
                               ? getRepository().getTypeHandler(
                                   TypeHandler.TYPE_GROUP)
                               : getRepository().getTypeHandler(
                                   TypeHandler.TYPE_FILE));
        handler = this;
        String synthId = Repository.ID_PREFIX_SYNTH + parentEntry.getId()
                         + ":" + id;

        boolean isDir = ftpFile.isDirectory();
        Entry   entry = (isDir
                         ? (Entry) new Entry(synthId, handler, true)
                         : new Entry(synthId, handler));

        String  name  = IO.getFileTail(ftpFile.getName());
        entry.setIsLocalFile(true);
        Entry parent;
        if (myFtpFile.path.equals(baseDir)) {
            parent = (Entry) parentEntry;
        } else {
            File   tmp        = new File(myFtpFile.path);
            String parentPath = tmp.getParent().replace("\\", "/");
            String parentId   = getSynthId(parentEntry, baseDir, parentPath);
            if (parentPath.equals(baseDir)) {
                parent = (Entry) parentEntry;
            } else {
                parent = (Entry) getEntryManager().getEntry(request,
                        parentId, false, false);
            }
        }

        double maxSize = parentEntry.getDoubleValue(request,COL_MAXSIZE,0);

        long dttm = ftpFile.getTimestamp().getTime().getTime();
        Resource resource;
        if (isDir) {
            resource = new Resource("ftp://" + server + myFtpFile.path,
                                    Resource.TYPE_URL);
        } else {
            if (ftpFile.getSize() > 1000000 * maxSize) {
                resource = new Resource("ftp://" + server + myFtpFile.path,
                                        Resource.TYPE_URL);
            } else {
                resource = new Resource(name, Resource.TYPE_REMOTE_FILE);

            }
            resource.setFileSize(ftpFile.getSize());
        }
        entry.initEntry(name, "", parent,
                        getUserManager().getLocalFileUser(), resource, "",
                        999, dttm, dttm, dttm, dttm, null);

        return entry;
    }

    /**
     * Class MyFTPFile _more_
     *
     *
     * @author RAMADDA Development Team
     */
    public static class MyFTPFile {

        FTPFile ftpFile;

        String path;

        public MyFTPFile(FTPFile ftpFile, String path) {
            this.ftpFile = ftpFile;
            this.path    = path;
        }
    }

    public Entry createEntry(String id) {
        return new Entry(id, this, true);
    }

    public static void main(String[] args) throws Exception {
        test("ftp.unidata.ucar.edu", "/pub", "anonymous", "");
        System.err.println("------------");
        test("ftp.unidata.ucar.edu", "/pub/idv/README", "anonymous", "");
        System.err.println("------------");
        test("ftp.unidata.ucar.edu", "/pub/ramadda/test", "anonymous", "");
        System.err.println("------------");
        test("ftp.unidata.ucar.edu", "/pub/ramadda/test/test", "anonymous",
             "");
    }

}
