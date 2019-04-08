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

package org.ramadda.repository;


import org.python.util.PythonInterpreter;

import org.ramadda.repository.auth.AccessException;
import org.ramadda.repository.job.JobManager;
import org.ramadda.repository.type.ProcessFileTypeHandler;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.TempDir;
import org.ramadda.util.Utils;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;


import java.net.URL;
import java.net.URLConnection;

import java.nio.charset.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;

import java.util.zip.*;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;


/**
 *  A class to manage the storage needs of the repository
 *
 * @author RAMADDA Development Team
 */
public class StorageManager extends RepositoryManager {


    /** file separator */
    public static final String FILE_SEPARATOR = "_file_";

    /** _more_ */
    public static final String DFLT_CIPHER = "DES";

    /** the full log file */
    public static final String FILE_FULLLOG = "fullrepository.log";

    /** the repository log file */
    public static final String FILE_LOG = "repository.log";

    /** the repository directory */
    public static final String DIR_REPOSITORY = "repository";

    /** the users directory */
    public static final String DIR_USERS = "users";

    /** _more_ */
    public static final String DIR_VOLATILE = "volatile";

    /** the backups directory */
    public static final String DIR_BACKUPS = "backups";

    /** the resources directory */
    public static final String DIR_RESOURCES = "resources";

    /** the htdocs directory */
    public static final String DIR_HTDOCS = "htdocs";

    /** anonymouse upload directory */
    public static final String DIR_ANONYMOUSUPLOAD = "anonymousupload";

    /** index flag */
    public static final String DIR_INDEX = "index";

    /** cache directory */
    public static final String DIR_CACHE = "cache";

    /** icons directory */
    public static final String DIR_ICONS = "icons";

    /** scratch directory */
    public static final String DIR_SCRATCH = "scratch";

    /** thumbnails directory */
    public static final String DIR_THUMBNAILS = "thumbnails";


    /** the directory depth property */
    public static final String PROP_DIRDEPTH = "ramadda.storage.dirdepth";

    /** the fast directory property */
    public static final String PROP_FASTDIR = "ramadda.storage.fastdir";

    /** the fast directory size property */
    public static final String PROP_FASTDIRSIZE =
        "ramadda.storage.fastdirsize";

    /** the deranged property */
    public static final String PROP_DIRRANGE = "ramadda.storage.dirrange";



    /** the temporary directory property */
    public static final String PROP_TMPDIR = "ramadda.storage.tmpdir";


    /** the temporary directory property */
    public static final String PROP_PROCESSDIR = "ramadda.storage.processdir";

    /** the log directory property */
    public static final String PROP_LOGDIR = "ramadda.storage.logdir";

    /** the storage directory property */
    public static final String PROP_STORAGEDIR = "ramadda.storage.storagedir";

    /** the entries directory property */
    public static final String PROP_ENTRIESDIR = "ramadda.storage.entriesdir";

    /** the upload directory property */
    public static final String PROP_UPLOADDIR = "ramadda.storage.uploaddir";

    /** the plugins directory property */
    public static final String PROP_PLUGINSDIR = "ramadda.storage.pluginsdir";

    /** the default directory depth */
    private int dirDepth = 2;

    /** the default directory range */
    private int dirRange = 10;

    /** the repository directory */
    private File repositoryDir;

    /** the temporary directory */
    private File tmpDir;


    /** _more_ */
    private File processDir;

    /** the htdocs directory */
    private String htdocsDir;

    /** the icons directory */
    private String iconsDir;

    /** the list of temporary directories to scour */
    private List<TempDir> tmpDirs = new ArrayList<TempDir>();

    /** the scratch directory */
    private TempDir scratchDir;

    /** the anonymous directory */
    private String anonymousDir;

    /** the cache directory */
    private TempDir cacheDir;

    /** the log directory */
    private File logDir;

    /** the cache directory size */
    private long cacheDirSize = -1;

    /** the upload directory */
    private File uploadDir;

    /** the plugins directory */
    private File pluginsDir;

    /** the entries directory */
    private File entriesDir;

    /** the users directory */
    private String usersDir;

    /** the storage directory */
    private File storageDir;

    /** the index directory */
    private File indexDir;

    /** the thumbnail directory */
    private TempDir thumbDir;

    /** list of directories that are okay to read from */
    private List<File> okToReadFromDirs = new ArrayList<File>();

    /** list of directories that are okay to write to */
    private List<File> okToWriteToDirs = new ArrayList<File>();

    /** _more_ */
    private String encryptionPassword;

    /**
     * Construct a new StorageManager for the repository
     *
     * @param repository  the repository
     */
    public StorageManager(Repository repository) {
        super(repository);
    }

    /**
     * Convert a resource to the actual location
     *
     * @param resource  the resource
     *
     * @return  the resource
     */
    public String resourceFromDB(String resource) {
        if (resource != null) {
            resource = resource.replace("${ramadda.storagedir}",
                                        getStorageDir().toString());
        }

        return resource;
    }

    /**
     * Convert a resource to the database location
     *
     * @param resource the resource
     *
     * @return the converted String
     */
    public String resourceToDB(String resource) {
        if (resource != null) {
            resource = resource.replace(getStorageDir().toString(),
                                        "${ramadda.storagedir}");
        }

        return resource;
    }


    /** flag for python initialization */
    private boolean haveInitializedPython = false;


    /**
     * Initialize python
     */
    public void initPython() {
        if (haveInitializedPython) {
            return;
        }
        haveInitializedPython = true;
        String     pythonCacheDir = getCacheDir().toString();
        Properties pythonProps    = new Properties();
        pythonProps.put("python.home", pythonCacheDir);
        PythonInterpreter.initialize(System.getProperties(), pythonProps,
                                     new String[] {});
    }


    /**
     * Do the final initialization
     */
    protected void doFinalInitialization() {

        //Add in the process tmp dir
        TempDir processTempDir = new TempDir(getProcessDir(), true);
        //TODO: set a property but for now hard code the scour to be 3 days
        processTempDir.setMaxAge(1000 * 60 * 60 * 24 * 3);
        tmpDirs.add(processTempDir);

        getUploadDir();
        getCacheDir();
        getScratchDir();
        getThumbDir();


        Misc.run(new Runnable() {
            public void run() {
                scourTmpDirs();
            }
        });
    }


    /**
     * _more_
     *
     * @return _more_
     */
    private String getDiskUsage() {
        //Note: this will break on systems without du
        List<String> commands = new ArrayList<String>();
        commands.add("du");
        commands.add("-s");
        commands.add("-h");
        commands.add(getRepositoryDir().toString());
        try {
            JobManager.CommandResults results =
                getRepository().getJobManager().executeCommand(commands,
                    getRepositoryDir());

            return results.getStdoutMsg();
        } catch (Exception exc) {
            return null;
        }
    }

    /**
     * Initialize the manager
     */
    protected void init() {
        encryptionPassword =
            getRepository().getProperty(PROP_ENCRYPT_PASSWORD, (String) null);
        String repositoryDirProperty =
            getRepository().getProperty(PROP_REPOSITORY_HOME, (String) null);

        if (repositoryDirProperty == null) {
            //Use the old <home>/.unidata/repository if its there
            repositoryDirProperty =
                IOUtil.joinDir(Misc.getSystemProperty("user.home", "."),
                               IOUtil.joinDir(".unidata", "repository"));
            //Else use  <home>/.ramadda
            if ( !new File(repositoryDirProperty).exists()) {
                repositoryDirProperty =
                    IOUtil.joinDir(Misc.getSystemProperty("user.home", "."),
                                   ".ramadda");
            } else {}
        }
        repositoryDir = new File(repositoryDirProperty);

        if ( !repositoryDir.exists()) {
            //            makeDirRecursive(repositoryDir);
            System.out.println("Error: The home directory does not exist: "
                               + repositoryDir);
            System.out.println(
                "Please create the directory and restart the server");

            throw new IllegalStateException(
                "RAMADDA: error: home directory does not exist: "
                + repositoryDir);

        }

        if (new File(IOUtil.joinDir(repositoryDir,
                                    "not_for_server.txt")).exists()) {
            System.out.println("RAMADDA: error: not supposed to run here: "
                               + repositoryDir);

            throw new IllegalStateException(
                "RAMADDA: error: not supposed to run here: " + repositoryDir);

        }

        /*
        if ( !repositoryDir.exists()) {
            System.out.println(
                "RAMADDA: error: home directory does not exist");
            throw new IllegalStateException(
                "RAMADDA: error: home directory does not exist");
        }
        */

        if ( !repositoryDir.canWrite()) {
            System.out.println(
                "RAMADDA: error: home directory is not writable");

            throw new IllegalStateException(
                "RAMADDA: error: home directory is not writable");
        }


        htdocsDir = IOUtil.joinDir(repositoryDir, DIR_HTDOCS);
        makeDir(htdocsDir);
        String resourcesDir = IOUtil.joinDir(repositoryDir, DIR_RESOURCES);
        makeDir(resourcesDir);

        dirDepth = getRepository().getProperty(PROP_DIRDEPTH, dirDepth);
        dirRange = getRepository().getProperty(PROP_DIRRANGE, dirRange);
    }




    /**
     * _more_
     *
     * @return _more_
     */
    public File getResourceDir() {
        return new File(IOUtil.joinDir(repositoryDir, DIR_RESOURCES));
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public File getVolatileDir() {
        return new File(IOUtil.joinDir(repositoryDir, DIR_VOLATILE));
    }

    /**
     * _more_
     *
     * @param dirName _more_
     *
     * @return _more_
     */
    public File getPluginResourceDir(String dirName) {
        File f = new File(IOUtil.joinDir(getResourceDir(), dirName));
        f.mkdirs();

        return f;
    }

    /**
     * Add repository info to the StringBuffer
     *
     * @param sb the StringBuffer
     */
    public void addInfo(StringBuffer sb) {
        sb.append(HtmlUtils.formEntry("Home Directory:",
                                      getRepositoryDir().toString()));
        sb.append(HtmlUtils.formEntry("Storage Directory:",
                                      getStorageDir().toString()));

        /**
         *  Don't do this since it can take quite a while
         * String usage = getDiskUsage();
         * if (usage != null) {
         *   sb.append(HtmlUtils.formEntry("Disk Usage:", usage));
         * }
         */

        File file = getStorageManager().getRepositoryDir();
        sb.append(HtmlUtils.formEntry(msgLabel("File System"),
                                      "Total: "
                                      + (file.getTotalSpace() / 1000000)
                                      + " (MB)" + " Free: "
                                      + (file.getFreeSpace() / 1000000)
                                      + " (MB)" + " Usable: "
                                      + (file.getUsableSpace() / 1000000)
                                      + " (MB)"));
    }


    /**
     * Add a directory to the okay to read from list
     *
     * @param dir  the directory
     */
    public void addOkToReadFromDirectory(File dir) {
        if ( !okToReadFromDirs.contains(dir)) {
            okToReadFromDirs.add(dir);
        }
    }



    /**
     * Add a directory to the okay to write to list
     *
     * @param dir  the directory
     */
    public void addOkToWriteToDirectory(File dir) {
        if ( !okToWriteToDirs.contains(dir)) {
            okToWriteToDirs.add(dir);
        }
        addOkToReadFromDirectory(dir);
    }



    /**
     * Localize the path
     *
     * @param path  the path
     *
     * @return  the localized path
     */
    public String localizePath(String path) {
        String repositoryDir = getRepositoryDir().toString();
        path = path.replace("%repositorydir%", repositoryDir);
        path = path.replace("${repositorydir}", repositoryDir);
        path = path.replace("%resourcedir%",
                            "/org/ramadda/repository/resources");

        return path;
    }

    /**
     * Get the system resource path
     *
     * @return  the path
     */
    public String getSystemResourcePath() {
        return "/org/ramadda/repository/resources";
    }



    /**
     * Get the upload directory
     *
     * @return  the upload directory
     */
    public File getUploadDir() {
        if (uploadDir == null) {
            uploadDir = getFileFromProperty(PROP_UPLOADDIR);
            addOkToWriteToDirectory(uploadDir);
        }

        return uploadDir;
    }

    /**
     * Get the repository directory
     *
     * @return _more_
     */
    public File getRepositoryDir() {
        return repositoryDir;
    }


    /**
     * Add a temporary directory to the list of scourable directories
     *
     * @param storageDir the directory to add
     */
    public void addTempDir(final TempDir storageDir) {
        tmpDirs.add(storageDir);
        Misc.runInABit(10000, new Runnable() {
            public void run() {
                scourTmpDir(storageDir, true);
            }
        });
    }

    /**
     * Make a temporary directory
     *
     * @param dir  the path
     *
     * @return  the directory
     */
    public TempDir makeTempDir(String dir) {
        return makeTempDir(dir, true);
    }

    /**
     * Make a temporary directory
     *
     * @param dir dir name
     * @param shouldScour should we scour this directory of old files
     *
     * @return  the temporary directory
     */
    public TempDir makeTempDir(String dir, boolean shouldScour) {
        TempDir tmpDir = new TempDir(IOUtil.joinDir(getTmpDir(), dir));
        makeDirRecursive(tmpDir.getDir());
        if (shouldScour) {
            addTempDir(tmpDir);
        }

        return tmpDir;
    }


    /**
     * _more_
     *
     * @param f _more_
     */
    public static void makeDirRecursive(File f) {
        f.mkdirs();
        if (f.exists()) {
            return;
        }

        throw new RuntimeException("Could not create directory:" + f);
    }


    /**
     * _more_
     *
     * @param f _more_
     */
    public static void makeDir(File f) {
        f.mkdir();
        if (f.exists()) {
            return;
        }

        throw new RuntimeException("Could not create directory:" + f);
    }

    /**
     * _more_
     *
     * @param f _more_
     */
    public static void makeDir(String f) {
        makeDir(new File(f));
    }


    /**
     * Get the temporary directory file
     *
     * @param tmpDir  the direcgtory
     * @param file    the file name
     *
     * @return  the file
     */
    public File getTmpDirFile(TempDir tmpDir, String file) {
        File f = new File(IOUtil.joinDir(tmpDir.getDir(), file));
        dirTouched(tmpDir, f);

        return checkReadFile(f);
    }


    /**
     * Get the icons directory file
     *
     * @param file  the filename
     *
     * @return  the file
     */
    public File getIconsDirFile(String file) {
        return new File(IOUtil.joinDir(getIconsDir(), file));
    }


    /**
     * Get the icons directory
     *
     * @return  the directory
     */
    private String getIconsDir() {
        if (iconsDir == null) {
            iconsDir = IOUtil.joinDir(htdocsDir, DIR_ICONS);
            makeDirRecursive(new File(iconsDir));
        }

        return iconsDir;
    }





    /**
     * Get the temporary directory
     *
     * @return  the directory
     */
    public File getTmpDir() {
        if (tmpDir == null) {
            tmpDir = getFileFromProperty(PROP_TMPDIR);
            addOkToWriteToDirectory(tmpDir);
        }

        return tmpDir;
    }

    /**
     * Get the scratch directory
     *
     * @return  the directory
     */
    public TempDir getScratchDir() {
        if (scratchDir == null) {
            scratchDir = makeTempDir(DIR_SCRATCH);
            scratchDir.setMaxAge(DateUtil.hoursToMillis(1));
        }

        return scratchDir;
    }

    /**
     * Get a unique scratch file
     *
     * @param file  the file name
     *
     * @return  a unique file
     */
    public File getUniqueScratchFile(String file) {
        File f = getScratchFile(file);
        while (f.exists()) {
            f = getScratchFile(getRepository().getGUID() + "_" + file);
        }

        return f;
    }


    /**
     * Get a scratch file
     *
     * @param file  the file suffix
     *
     * @return  a unique file
     */
    public File getScratchFile(String file) {
        return getTmpDirFile(getScratchDir(), file);
    }


    /**
     * Get the thumbnail directory
     *
     * @return  the directory
     */
    private TempDir getThumbDir() {
        if (thumbDir == null) {
            thumbDir = makeTempDir(DIR_THUMBNAILS);
            thumbDir.setMaxFiles(1000);
            thumbDir.setMaxSize(1000 * 1000 * 1000);
        }

        return thumbDir;
    }

    /**
     * Get a file in the thumbnail directory
     *
     * @param file  the file name
     *
     * @return  the file
     */
    public File getThumbFile(String file) {
        return getTmpDirFile(getThumbDir(), file);
    }


    /**
     * Get the thumbnail directory
     *
     * @param file  the filename
     *
     * @return  the directory
     */
    public File getThumbDir(String file) {
        File f = getTmpDirFile(getThumbDir(), file);
        makeDirRecursive(f);

        return f;
    }



    /**
     * Get an icon file
     *
     * @param file the file name
     *
     * @return  the File
     */
    public File getIconFile(String file) {
        return getTmpDirFile(getThumbDir(), file);
    }


    /**
     * _more_
     */
    @Override
    public void clearCache() {
        for (File file : cacheDir.listFiles()) {
            file.delete();
        }
    }

    /**
     * Get the cache directory
     *
     * @return  the directory
     */
    private TempDir getCacheDir() {
        if (cacheDir == null) {
            cacheDir = makeTempDir(DIR_CACHE);
            cacheDir.setMaxSize(1000 * 1000 * 1000);
        }

        return cacheDir;
    }


    /**
     * Get a cache file
     *
     * @param file  the file name
     *
     * @return  the File
     */
    public File getCacheFile(String file) {
        return getTmpDirFile(getCacheDir(), file);
    }


    /**
     * Get the log directory
     *
     * @return  the log directory
     */
    public File getLogDir() {
        if (logDir != null) {
            return logDir;
        }

        synchronized (PROP_LOGDIR) {
            //Check for race conditions
            if (logDir != null) {
                return logDir;
            }
            File tmpLogDir = getFileFromProperty(PROP_LOGDIR);
            if (getRepository().isReadOnly()) {
                //                System.out.println("RAMADDA: skipping log4j");
                logDir = tmpLogDir;

                return logDir;
            }

            if ( !getLogManager().isLoggingEnabled()) {
                //                System.out.println("RAMADDA: skipping log4j");
                logDir = tmpLogDir;

                return logDir;
            }

            File log4JFile = new File(tmpLogDir + "/" + "log4j.properties");
            //For now always write out the log from the jar
            //            System.out.println("RAMADDA: log4j file=" + log4JFile);
            if (true || !log4JFile.exists()) {
                try {
                    String c =
                        IOUtil.readContents(
                            "/org/ramadda/repository/resources/log4j.properties",
                            getClass());
                    String logDirPath = tmpLogDir.toString();
                    //Replace for windows
                    logDirPath = logDirPath.replace("\\", "/");
                    c          = c.replace("${ramadda.logdir}", logDirPath);
                    c = c.replace("${file.separator}", File.separator);
                    IOUtil.writeFile(log4JFile, c);
                } catch (Exception exc) {
                    System.err.println(
                        "RAMADDA: Error writing log4j properties:" + exc);

                    throw new RuntimeException(exc);
                }
            }
            try {
                //                System.err.println("RAMADDA: turning on log4j.debug");
                //                System.setProperty("log4j.debug","");
                /*
                System.out.println(
                    "RAMADDA: Configuring log4j with properties:" + log4JFile
                    + " (this may print out a stack trace)");
                */
                org.apache.log4j.PropertyConfigurator.configure(
                    log4JFile.toString());
            } catch (Exception exc) {
                System.err.println("RAMADDA: Error configuring log4j:" + exc);
                exc.printStackTrace();
            }
            logDir = tmpLogDir;

            return logDir;
        }
    }


    /**
     * Touch the temporary directory
     *
     * @param tmpDir  the temporary directory location
     * @param f  the file
     */
    public void dirTouched(final TempDir tmpDir, File f) {
        if (f != null) {
            f.setLastModified(new Date().getTime());
            //if the file is already there then don't scour
            if (f.exists()) {
                return;
            }
        }
        //Run this in 10 seconds
        if (tmpDir.getTouched()) {
            return;
        }
        tmpDir.setTouched(true);
        Misc.runInABit(10000, new Runnable() {
            public void run() {
                scourTmpDir(tmpDir);
            }
        });
    }


    /**
     * Scour the temporary directories
     */
    private void scourTmpDirs() {
        //Scour once an hour
        while (true) {
            List<TempDir> tmpTmpDirs = new ArrayList<TempDir>(tmpDirs);
            for (TempDir tmpDir : tmpTmpDirs) {
                scourTmpDir(tmpDir);
            }
            Misc.sleepSeconds(60 * 60);
        }
    }


    /**
     * Scour a temporary directory
     *
     * @param tmpDir  the temporary directory
     */
    protected void scourTmpDir(final TempDir tmpDir) {
        scourTmpDir(tmpDir, false);
    }

    /**
     * Force the scouring of the temporary directory
     *
     * @param tmpDir  the directory
     * @param force   true to force scouring
     */
    protected void scourTmpDir(final TempDir tmpDir, boolean force) {
        synchronized (tmpDir) {
            //            if ( !force && !tmpDir.haveChanged()) {
            //                return;
            //            }
            List<File> filesToScour = tmpDir.findFilesToScour();
            if (filesToScour.size() > 0) {
                logInfo("StorageManager: scouring " + filesToScour.size()
                        + " files from:" + tmpDir.getDir().getName());
            }


            List<File> notDeleted = IOUtil.deleteFiles(filesToScour);
            if (notDeleted.size() > 0) {
                logInfo("Unable to delete tmp files:" + notDeleted);
            }
            if (tmpDir.getFilesOk()) {
                //Now check for empty top level dirs and get rid of the
                for (File remainingFile : tmpDir.listFiles()) {
                    if ( !remainingFile.isDirectory()) {
                        continue;
                    }
                    if (remainingFile.listFiles().length == 0) {
                        remainingFile.delete();
                    }
                }
            }

        }
        tmpDir.setTouched(false);
    }



    /**
     * Get the Anonymous uploads directory
     *
     * @return  the directory
     */
    public String getAnonymousDir() {
        if (anonymousDir == null) {
            anonymousDir = IOUtil.joinDir(getStorageDir(),
                                          DIR_ANONYMOUSUPLOAD);
            makeDirRecursive(new File(anonymousDir));
        }

        return anonymousDir;
    }


    /**
     * Get a file location from a property
     *
     * @param property  the property
     *
     * @return  the location as a File
     */
    private File getFileFromProperty(String property) {
        String path = getRepository().getProperty(property, null);
        if (path == null) {
            throw new IllegalArgumentException("Directory property:"
                    + property + " not defined");
        }
        File f = new File(localizePath(path));
        makeDirRecursive(f);

        return f;
    }


    /**
     * Get the storage directory
     *
     * @return the storage directory path
     */
    public String getStorageDir() {
        if (storageDir == null) {
            storageDir = getFileFromProperty(PROP_STORAGEDIR);
            addOkToWriteToDirectory(storageDir);
        }

        return storageDir.toString();
    }


    /**
     * _more_
     *
     * @param processDir _more_
     *
     * @return _more_
     */
    public String getProcessDirEntryId(File processDir) {
        return getProcessDirEntryId(processDir.getName());
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param processDir _more_
     *
     * @return _more_
     */
    public String getProcessDirEntryUrl(Request request, File processDir) {
        String processEntryId = getProcessDirEntryId(processDir);

        String entryUrl =
            HtmlUtils.url(request.makeUrl(getRepository().URL_ENTRY_SHOW,
                                          ARG_ENTRYID, processEntryId));

        return entryUrl;
    }


    /**
     * _more_
     *
     * @param processId _more_
     *
     * @return _more_
     */
    public String getProcessDirEntryId(String processId) {
        return EntryManager.ID_PREFIX_SYNTH
               + ProcessFileTypeHandler.TYPE_PROCESS + ":/" + processId;
    }

    /**
     * _more_
     *
     * @param processId _more_
     *
     * @return _more_
     */
    public File getProcessDir(String processId) {
        File subDir = new File(IOUtil.joinDir(getProcessDir(), processId));
        if ( !subDir.exists()) {
            System.err.println("no process id dir:" + subDir);

            return null;
        }

        return subDir;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public File createProcessDir() {
        String processId = getRepository().getGUID();
        File   subDir = new File(IOUtil.joinDir(getProcessDir(), processId));
        makeDir(subDir);

        return subDir;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public File getProcessDir() {
        if (processDir == null) {
            processDir = getFileFromProperty(PROP_PROCESSDIR);
            addOkToWriteToDirectory(processDir);
        }

        return processDir;
    }

    /**
     * _more_
     *
     * @param file _more_
     *
     * @return _more_
     */
    public boolean isProcessFile(File file) {
        return IOUtil.isADescendent(getProcessDir(), file);

    }

    /**
     * Get the index directory
     *
     * @return the index directory path
     */
    public String getIndexDir() {
        if (indexDir == null) {
            indexDir = new File(IOUtil.joinDir(getRepositoryDir(),
                    DIR_INDEX));
            makeDirRecursive(indexDir);
        }

        return indexDir.toString();
    }

    /**
     * Get the plugins directory
     *
     * @return  the directory
     */
    public File getPluginsDir() {
        if (pluginsDir == null) {
            pluginsDir = getFileFromProperty(PROP_PLUGINSDIR);
        }

        return pluginsDir;
    }

    /**
     * Get the backups directory
     *
     * @return  the directory
     */
    public String getBackupsDir() {
        String dir = IOUtil.joinDir(getRepositoryDir(), DIR_BACKUPS);
        makeDirRecursive(new File(dir));

        return dir;
    }

    /**
     * Get a directory
     *
     * @param name the name
     *
     * @return  the path
     */
    public String getDir(String name) {
        String dir = IOUtil.joinDir(getRepositoryDir(), name);
        makeDirRecursive(new File(dir));

        return dir;
    }






    /**
     * Get a tmp file for the request
     *
     * @param request  the Request
     * @param name     the file name
     *
     * @return  a unique file
     */
    public File getTmpFile(Request request, String name) {
        return getTmpDirFile(getScratchDir(),
                             getRepository().getGUID() + FILE_SEPARATOR
                             + name);
    }

    /**
     * _more_
     *
     * @param tmpDir _more_
     * @param file _more_
     *
     * @return _more_
     */
    public File makeTmpFile(File tmpDir, String file) {
        File   f;
        int    cnt  = 0;
        String name = file;
        while (true) {
            f = new File(IOUtil.joinDir(tmpDir, name));
            if ( !f.exists()) {
                break;
            }
            cnt++;
            name = cnt + "_" + file;
        }

        return checkReadFile(f);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param name _more_
     *
     * @return _more_
     */
    public File getTmpFilePath(Request request, String name) {
        return new File(IOUtil.joinDir(getScratchDir().getDir(), name));
    }

    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public String getOriginalFilename(String name) {
        int idx = name.indexOf(FILE_SEPARATOR);
        if (idx >= 0) {
            name = name.substring(idx + FILE_SEPARATOR.length());
        }

        return name;
    }


    /**
     * Move a file to storage
     *
     * @param request  the Request
     * @param original the original file
     *
     * @return  the new File location
     *
     * @throws Exception  problem moving file
     */
    public File moveToStorage(Request request, File original)
            throws Exception {
        return moveToStorage(request, original, original.getName());
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param inputStream _more_
     * @param fileName _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public File moveToStorage(Request request, InputStream inputStream,
                              String fileName)
            throws Exception {
        File         f = getStorageManager().getTmpFile(request, fileName);
        OutputStream outputStream =
            getStorageManager().getFileOutputStream(f);
        IOUtil.writeTo(inputStream, outputStream);
        IOUtil.close(inputStream);
        IOUtil.close(outputStream);
        f = getStorageManager().moveToStorage(request, f);

        return f;
    }



    /**
     * Clean an entry id
     *
     * @param id  the id
     *
     * @return the cleaned name
     */
    private String cleanEntryId(String id) {
        return IOUtil.cleanFileName(id);
    }

    /**
     * Get the entry directory
     *
     * @param id   the entry id
     * @param createIfNeeded  true if we want to create a new one
     * @return  the directory
     */
    public File getEntryDir(String id, boolean createIfNeeded) {
        id = cleanEntryId(id);
        if (entriesDir == null) {
            entriesDir = getFileFromProperty(PROP_ENTRIESDIR);
            addOkToWriteToDirectory(entriesDir);
        }
        File entryDir = new File(IOUtil.joinDir(entriesDir, id));
        //The old way
        if (entryDir.exists()) {
            return entryDir;
        }

        String dir1 = "entry_" + ((id.length() >= 2)
                                  ? id.substring(0, 2)
                                  : "");
        String dir2 = "entry_" + ((id.length() >= 4)
                                  ? id.substring(2, 4)
                                  : "");
        entryDir = new File(IOUtil.joinDir(entriesDir,
                                           IOUtil.joinDir(dir1,
                                               IOUtil.joinDir(dir2, id))));
        //        System.err.println("entrydir:" + entryDir);
        if (createIfNeeded) {
            makeDirRecursive(entryDir);
        }

        return entryDir;
    }


    /**
     * Get the user directory
     *
     * @param id  the entry id
     * @param createIfNeeded true to create if necessary
     *
     * @return  the directory
     */
    public File getUserDir(String id, boolean createIfNeeded) {
        id = IOUtil.cleanFileName(id);
        if (usersDir == null) {
            usersDir = IOUtil.joinDir(getRepositoryDir(), DIR_USERS);
            makeDirRecursive(new File(usersDir));
        }

        String dir1 = "user_" + ((id.length() >= 2)
                                 ? id.substring(0, 2)
                                 : "");
        String dir2 = "user_" + ((id.length() >= 4)
                                 ? id.substring(2, 4)
                                 : "");
        File userDir = new File(IOUtil.joinDir(usersDir,
                           IOUtil.joinDir(dir1, IOUtil.joinDir(dir2, id))));
        if (createIfNeeded) {
            makeDirRecursive(userDir);
        }

        return userDir;
    }



    /**
     * Delete an entry directory
     *
     * @param id  the entry id
     */
    public void deleteEntryDir(final String id) {
        Misc.run(new Runnable() {
            public void run() {
                File dir = getEntryDir(id, false);
                if (dir.exists()) {
                    IOUtil.deleteDirectory(dir);
                }
            }
        });
    }


    /**
     * Move to a new entry directory
     *
     * @param entry    the Entry
     * @param original the original directory
     *
     * @return  the new directory
     *
     * @throws Exception  problem moving
     */
    public File moveToEntryDir(Entry entry, File original) throws Exception {
        File newFile = new File(IOUtil.joinDir(getEntryDir(entry.getId(),
                           true), original.getName()));
        moveFile(original, newFile);

        return newFile;
    }

    /**
     * Copy to a new entry directory
     *
     * @param entry    the Entry
     * @param original the original directory
     *
     * @return  the new directory
     *
     * @throws Exception  problem copying
     */
    public File copyToEntryDir(Entry entry, File original) throws Exception {
        return copyToEntryDir(entry, original, original.getName());
    }

    /**
     * Copy to an entry directory
     *
     * @param entry    the Entry
     * @param original the original directory
     * @param newName  the new name
     *
     * @return  the new location
     *
     * @throws Exception  problem copying
     */
    public File copyToEntryDir(Entry entry, File original, String newName)
            throws Exception {
        File entryDir = getEntryDir(entry.getId(), true);
        File newFile  = new File(IOUtil.joinDir(entryDir, newName));
        try {
            copyFile(original, newFile);
        } catch (Exception exc) {
            System.err.println("ERROR: StorageManager.copyToEntryDir: "
                               + exc);
            System.err.println("ERROR: Original file:" + original
                               + " exists:" + original.exists());
            System.err.println("ERROR: Entry dir:" + entryDir + " exists:"
                               + entryDir.exists());

            throw exc;
        }

        return newFile;
    }

    /**
     * Copy from one entry to another
     *
     * @param oldEntry  the old Entry
     * @param newEntry  the new Entry
     * @param filename  the filename
     *
     * @return  the new location
     *
     * @throws Exception  problem copying
     */
    public String copyToEntryDir(Entry oldEntry, Entry newEntry,
                                 String filename)
            throws Exception {
        File oldFile = new File(IOUtil.joinDir(getEntryDir(oldEntry.getId(),
                           true), filename));

        File newFile = new File(IOUtil.joinDir(getEntryDir(newEntry.getId(),
                           true), filename));

        //If it is a URL then copy it over
        if ( !oldFile.exists()) {
            URL         url        = new URL(filename);
            InputStream fromStream = getInputStream(filename);

            newFile = new File(IOUtil.joinDir(getEntryDir(newEntry.getId(),
                    true), IOUtil.getFileTail(url.getPath())));


            OutputStream toStream = getFileOutputStream(newFile);
            IOUtil.writeTo(fromStream, toStream);
            IOUtil.close(fromStream);
            IOUtil.close(toStream);

            return newFile.getName();
        }


        copyFile(oldFile, newFile);

        return newFile.getName();
    }

    /**
     * _more_
     *
     * @param path _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public File fetchUrl(String path) throws Exception {
        //Make sure its a url
        if ( !path.toLowerCase().startsWith("http")) {
            throw new IllegalArgumentException("Bad URL:" + path);
        }
        URL           url        = new URL(path);
        URLConnection connection = url.openConnection();
        InputStream   fromStream = connection.getInputStream();
        File          tmpFile    = getTmpFile(null, IOUtil.getFileTail(path));
        OutputStream  toStream   = getFileOutputStream(tmpFile);
        IOUtil.writeTo(fromStream, toStream);
        IOUtil.close(fromStream);

        return tmpFile;
    }



    /**
     * Copy a file
     *
     * @param oldFile  old file
     * @param newFile  new file
     *
     * @throws Exception  problem with copy
     */
    private void copyFile(File oldFile, File newFile) throws Exception {
        checkReadFile(oldFile);
        checkWriteFile(newFile);
        IOUtil.copyFile(oldFile, newFile);
    }


    public void writeFile(File oldFile, String contents) throws Exception {
        checkWriteFile(oldFile);
        IOUtil.writeFile(oldFile, contents);
    }



    /**
     * Move a file to storage
     *
     * @param request   the Request
     * @param original  the original file
     * @param targetName  the target name
     *
     * @return  the new location
     *
     * @throws Exception  problem moving
     */
    public File moveToStorage(Request request, File original,
                              String targetName)
            throws Exception {
        if (targetName == null) {
            targetName = original.getName();
        }
        String storageDir = getStorageDir();
        GregorianCalendar cal =
            new GregorianCalendar(RepositoryUtil.TIMEZONE_DEFAULT);
        cal.setTime(new Date());
        storageDir = IOUtil.joinDir(storageDir, "y" + cal.get(cal.YEAR));
        makeDir(storageDir);
        storageDir = IOUtil.joinDir(storageDir,
                                    "m" + (cal.get(cal.MONTH) + 1));
        makeDir(storageDir);
        storageDir = IOUtil.joinDir(storageDir,
                                    "d" + cal.get(cal.DAY_OF_MONTH));
        makeDir(storageDir);

        for (int depth = 0; depth < dirDepth; depth++) {
            int index = (int) (dirRange * Math.random());
            storageDir = IOUtil.joinDir(storageDir, "data" + index);
            makeDir(storageDir);
        }

        File newFile = new File(IOUtil.joinDir(storageDir, targetName));


        try {
            moveFile(original, newFile);
        } catch (Exception exc) {
            System.err.println("ERROR: StorageManager.moveToStorage:" + exc);
            System.err.println("ERROR: original:" + original + " exists:"
                               + original.exists());
            System.err.println("ERROR: new:" + newFile + " dir exists:"
                               + newFile.getParentFile().exists());

            throw exc;
        }

        return newFile;
    }



    /**
     * Move to anonymous storage
     *
     * @param request    the Request
     * @param original   the original file
     * @param prefix     a prefix
     *
     * @return the new location
     *
     * @throws Exception  problem moving to storage
     */
    public File moveToAnonymousStorage(Request request, File original,
                                       String prefix)
            throws Exception {
        String storageDir = getAnonymousDir();
        File[] files      = new File(storageDir).listFiles();
        long   size       = 0;
        for (int i = 0; i < files.length; i++) {
            if (files[i].isHidden()) {
                continue;
            }
            size += files[i].length();
        }

        double sizeThresholdGB =
            getRepository().getProperty(PROP_UPLOAD_MAXSIZEGB, 10.0);
        if (size + original.length() > sizeThresholdGB * 1000 * 1000 * 1000) {
            throw new IllegalArgumentException(
                "Anonymous upload area exceeded capacity");
        }

        String fileName = original.getName();
        fileName = HtmlUtils.urlEncode(fileName);
        String targetName = prefix + fileName;
        File   newFile    = new File(IOUtil.joinDir(storageDir, targetName));
        moveFile(original, newFile);

        return newFile;
    }


    /**
     * Copy a file to storage
     *
     * @param request   the Request
     * @param original  the original file
     * @param newName   the new name
     *
     * @return  the new location
     *
     * @throws Exception  problem copying to storage
     */
    public File copyToStorage(Request request, File original, String newName)
            throws Exception {
        return copyToStorage(request, getFileInputStream(original), newName);
    }

    /**
     * Copy a file to storage
     *
     * @param request   the Request
     * @param original  the original file input stream
     * @param newName   the new name
     *
     * @return  the new location
     *
     * @throws Exception  problem copying to storage
     */
    public File copyToStorage(Request request, InputStream original,
                              String newName)
            throws Exception {
        String targetName = newName;
        String storageDir = getStorageDir();

        GregorianCalendar cal =
            new GregorianCalendar(RepositoryUtil.TIMEZONE_DEFAULT);
        cal.setTime(new Date());

        storageDir = IOUtil.joinDir(storageDir, "y" + cal.get(cal.YEAR));
        makeDir(storageDir);
        storageDir = IOUtil.joinDir(storageDir,
                                    "m" + (cal.get(cal.MONTH) + 1));
        makeDir(storageDir);
        storageDir = IOUtil.joinDir(storageDir,
                                    "d" + cal.get(cal.DAY_OF_MONTH));
        makeDir(storageDir);


        for (int depth = 0; depth < dirDepth; depth++) {
            int index = (int) (dirRange * Math.random());
            storageDir = IOUtil.joinDir(storageDir, "data" + index);
            makeDir(storageDir);
        }

        File newFile = new File(IOUtil.joinDir(storageDir, targetName));
        IOUtil.copyFile(original, newFile);
        IOUtil.close(original);

        return newFile;
    }

    /**
     * Get the upload file path
     *
     * @param fileName  the filename
     *
     * @return  the new file
     */
    public File getUploadFilePath(String fileName) {
        return checkReadFile(new File(IOUtil.joinDir(getUploadDir(),
                getStorageFileName(fileName))));
    }


    /**
     * Get a storage filename
     *
     * @param fileName  the name
     *
     * @return  the storage filename
     */
    public String getStorageFileName(String fileName) {
        return repository.getGUID() + FILE_SEPARATOR + fileName;
    }


    /**
     * Get the filetail of an Entry
     *
     * @param entry  the Entry
     *
     * @return  the file tail
     */
    public String getFileTail(Entry entry) {
        String tail;
        if (entry.getIsLocalFile()) {
            tail = IOUtil.getFileTail(entry.getResource().getPath());
        } else {
            tail = getFileTail(entry.getResource().getPath());
        }

        return tail;
    }

    /**
     * Get the filetail of a filename
     *
     * @param fileName  the filename
     *
     * @return  the file tail
     */
    public static String getFileTail(String fileName) {
        return RepositoryUtil.getFileTail(fileName);
    }

    /**
     * Can the Entry be downloaded
     *
     * @param request  the Request
     * @param entry    the Entry
     *
     * @return  true if it can be downloaded
     *
     * @throws Exception  problem accessing the Entry
     */
    public boolean canDownload(Request request, Entry entry)
            throws Exception {
        Resource resource = entry.getResource();
        if ( !resource.isFile()) {
            return false;
        }
        File file = resource.getTheFile();

        //This is for the ftptypehandler where it caches the file
        if (resource.isRemoteFile()) {
            return true;
        }

        //Check if its in the storage dir or under of the harvester dirs
        if (isInDownloadArea(file)) {
            return true;
        }

        //Check if its under one of the local file dirs defined by the admin
        if (isLocalFileOk(file)) {
            return true;
        }


        return false;
    }


    /**
     * Get the fast resource path
     *
     * @param entry  the Entry
     *
     * @return  the path
     */
    public String getFastResourcePath(Entry entry) {
        String fastDir = getRepository().getProperty(PROP_FASTDIR,
                             (String) null);
        if ((fastDir == null) || !entry.isFile()) {
            return entry.getResource().getPath();
        }

        File f = entry.getTypeHandler().getFileForEntry(entry);
        if ( !f.exists()) {
            return entry.getResource().getPath();
        }
        //TODO: do this


        return f.toString();
    }


    /**
     * Remove a file associated with an entry
     *
     * @param entry  the Entry
     */
    public void removeFile(Entry entry) {
        removeFile(entry.getResource());
    }

    /**
     * Remove a resource
     *
     * @param resource the resource
     */
    public void removeFile(Resource resource) {
        if (resource.isStoredFile()) {
            deleteFile(resource.getTheFile());
        }
    }


    /**
     * Delete a file
     *
     * @param f  the file
     */
    public void deleteFile(File f) {
        f.delete();
    }



    /**
     * Is the file in a downloadable area?
     *
     * @param file  the file
     *
     * @return true if in downloadable area
     */
    public boolean isInDownloadArea(File file) {
        for (File dir : okToReadFromDirs) {
            if (IOUtil.isADescendent(dir, file)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check the local file
     *
     * @param file  the file
     *
     * @throws Exception  problem accessing file
     */
    public void checkLocalFile(File file) throws Exception {
        if ( !isLocalFileOk(file)) {
            System.err.println(
                "StorageManager.checkLocalFile bad file location:" + file);

            throw new AccessException(
                "The specified file is not under one of the allowable file system directories<br>These need to be set by the site administrator",
                null);
        }
    }


    /**
     * Is the local file okay?
     *
     * @param file  the file
     *
     * @return  true if ok
     */
    public boolean isLocalFileOk(File file) {
        boolean ok = false;
        //        System.err.println ("StorageManager: isLocalFileOk:" +file);
        for (File parent : getRepository().getLocalFilePaths()) {
            //            System.err.println ("      Checking:" +parent);
            if (IOUtil.isADescendent(parent, file)) {
                //                System.err.println ("      OK");
                return true;
            }
        }

        //        System.err.println ("      NOT OK");
        return false;
    }



    /**
     * Throw a bad file Exception
     *
     * @param f  the file
     */
    private void throwBadFile(File f) {
        throw new IllegalArgumentException(
            "The file:" + f
            + " is not under one of the allowable file system directories", null);
    }

    /**
     * Check if a file is writeable
     *
     * @param file  the file
     *
     * @return  the File if writable, else null
     */
    public File checkWriteFile(File file) {
        if (getRepository().isReadOnly()) {
            throw new IllegalArgumentException("Unable to write to file");
        }
        if (IOUtil.isADescendent(getRepositoryDir(), file)) {
            return file;
        }
        for (File dir : okToWriteToDirs) {
            if (IOUtil.isADescendent(dir, file)) {
                return file;
            }
        }
        throwBadFile(file);

        return null;
    }


    /**
     * Check if a file is readable
     *
     * @param file  the file
     *
     * @return  the file if readable, else null
     */
    public File checkReadFile(File file) {
        //check if its in an allowable area for access
        if (isLocalFileOk(file)) {
            return file;
        }

        //Check if its in the storage dir
        if (IOUtil.isADescendent(getRepositoryDir(), file)) {
            return file;
        }

        if (isInDownloadArea(file)) {
            return file;
        }

        if (getRepository().getParentRepository() != null) {
            getRepository().getParentRepository().getStorageManager()
                .checkReadFile(file);

            return file;
        }

        throwBadFile(file);

        return null;
    }


    /**
     * check the path
     *
     * @param path  the path
     *
     * @throws Exception if bad file or not readable
     */
    public void checkPath(String path) throws Exception {
        //Path can be a file, a URL, a file URL or a system resource
        File f = new File(path);
        if (f.exists()) {
            checkReadFile(f);

            return;
        }

        if (path.toLowerCase().trim().startsWith("file:")) {
            f = new File(path.substring("file:".length()));
            checkReadFile(f);

            return;
        }

        //Should be ok here. Its either a url or a system resource
    }



    /**
     * Read a system resource without checking permissions
     *
     * @param path  the resource path
     *
     * @return  the resource as a String
     *
     * @throws Exception  problem reading the resource
     */
    public String readUncheckedSystemResource(String path) throws Exception {
        //        checkPath(path);
        return IOUtil.readContents(path, getClass());
    }

    /**
     * Move a file
     *
     * @param from  from File
     * @param to    to File
     *
     * @throws Exception problem moving
     */
    public void moveFile(File from, File to) throws Exception {
        checkReadFile(from);
        checkWriteFile(to);
        try {
            IOUtil.moveFile(from, to);
        } catch (Exception exc) {
            //Handle the windows file move problem
            if (to.isDirectory()) {
                to = new File(
                    IOUtil.joinDir(to, IOUtil.getFileTail(from.toString())));
            }
            IOUtil.copyFile(from, to);
            if ( !from.delete()) {
                from.deleteOnExit();
            }
        }
    }


    /**
     * Read an unchecked system resource
     *
     * @param path  the resource path
     * @param dflt  the default return if resource not availalble
     *
     * @return  the resource or dflt
     *
     * @throws Exception  problem reading resource
     */
    public String readUncheckedSystemResource(String path, String dflt)
            throws Exception {
        //        checkPath(path);
        return IOUtil.readContents(path, getClass(), dflt);
    }


    /**
     * Read a system resource, checking permissions
     *
     * @param url  the URL of the resource
     *
     * @return  the resource
     *
     * @throws Exception  problem reading
     */
    public String readSystemResource(URL url) throws Exception {
        checkPath(url.toString());

        return IOUtil.readContents(url.toString(), getClass());
    }


    /**
     * Read a system resource from the file
     *
     * @param file  the File location
     *
     * @return  the resource
     *
     * @throws Exception  problem reading
     */
    public String readSystemResource(File file) throws Exception {
        return IOUtil.readInputStream(getFileInputStream(file));
    }


    /**
     * Read a system resource from the path
     *
     * @param path  the path
     *
     * @return  the resource
     *
     * @throws Exception  problem reading
     */
    public String readSystemResource(String path) throws Exception {
        InputStream stream = getInputStream(path);
        try {
            return IOUtil.readInputStream(stream);
        } finally {
            IOUtil.close(stream);
        }
    }


    /**
     * Get an InputStream for the path
     *
     * @param path  the path
     *
     * @return  the InputStream
     *
     * @throws Exception  problem getting stream
     */
    public InputStream getInputStream(String path) throws Exception {
        checkPath(path);
        return Utils.getInputStream(path, getClass());
    }

    /**
     * Get a FileInputStream for the path
     *
     * @param path  the path
     *
     * @return  the FileInputStream
     *
     * @throws Exception  problem opening stream
     */
    public InputStream getFileInputStream(String path) throws Exception {
        return getFileInputStream(new File(path));
    }

    /**
     * This creates a FileOutputStream. It does enforce that the file is under
     * the main ramadda directory and it DOES check for READONLY mode
     *
     * @param file The file to write to
     *
     * @return FileOutputStream
     *
     * @throws Exception If the file is not under the main ramadda dir
     */
    public InputStream getFileInputStream(File file) throws Exception {
        checkReadFile(file);

        return decrypt(file, getEncryptionPassword());
    }

    /**
     * This checks to ensure that the given file is under one of the
     * allowable places to write to. This includes the storage dir,
     * the entries dir, uploads dir and the tmp dir.  In general a server
     * process should be configured to only be allowed to have write access
     * to limited directories.
     *
     * @param file to check
     *
     * @return The fos
     *
     * @throws Exception if the file is not under an allowable dir
     */
    public OutputStream getFileOutputStream(File file) throws Exception {
        checkWriteFile(file);

        return getUncheckedFileOutputStream(file);
    }


    /**
     * This creates a FileOutputStream. It makes no check whether we're
     * allowed to write to the file, read-only mode, etc
     *
     * @param file The file to write to
     *
     * @return FileOutputStream
     *
     * @throws Exception _more_
     */
    public OutputStream getUncheckedFileOutputStream(File file)
            throws Exception {
        return encrypt(file, getEncryptionPassword());
    }


    /**
     * _more_
     *
     * @param file _more_
     *
     * @return _more_
     */
    public boolean shouldCrypt(File file) {
        //We only crypt files stored under the RAMADDA home dir
        if (IOUtil.isADescendent(getRepositoryDir(), file)) {
            return true;
        }

        return false;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getEncryptionPassword() {
        return encryptionPassword;
    }


    /**
     * _more_
     *
     * @param file _more_
     * @param key _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public InputStream decrypt(File file, String key) throws Exception {
        InputStream is = new FileInputStream(file);
        if ((key == null) || !shouldCrypt(file)) {
            return is;
        }
        DESKeySpec dks = new DESKeySpec(key.getBytes());
        String cipherSpec = getRepository().getProperty(PROP_ENCRYPT_CIPHER,
                                DFLT_CIPHER);
        SecretKey desKey =
            SecretKeyFactory.getInstance(cipherSpec).generateSecret(dks);
        Cipher cipher = Cipher.getInstance(cipherSpec);  // DES/ECB/PKCS5Padding for SunJCE
        cipher.init(Cipher.DECRYPT_MODE, desKey);

        return new CipherInputStream(is, cipher);
    }


    /**
     * _more_
     *
     * @param file _more_
     * @param key _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public OutputStream encrypt(File file, String key) throws Exception {
        OutputStream os = new FileOutputStream(file);
        if ((key == null) || !shouldCrypt(file)) {
            return os;
        }
        String cipherSpec = getRepository().getProperty(PROP_ENCRYPT_CIPHER,
                                DFLT_CIPHER);
        DESKeySpec dks = new DESKeySpec(key.getBytes());
        SecretKey desKey =
            SecretKeyFactory.getInstance(cipherSpec).generateSecret(dks);
        Cipher cipher = Cipher.getInstance(cipherSpec);  // DES/ECB/PKCS5Padding for SunJCE
        cipher.init(Cipher.ENCRYPT_MODE, desKey);

        return new CipherOutputStream(os, cipher);
    }

    /**
     * _more_
     *
     * @param is _more_
     * @param os _more_
     *
     * @throws Exception _more_
     */
    public static void doCopy(InputStream is, OutputStream os)
            throws Exception {
        IOUtil.writeTo(is, os);
        IOUtil.close(is);
        IOUtil.close(os);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param zipFile _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<File> unpackZipfile(Request request, String zipFile)
            throws Exception {
        List<File>     files = new ArrayList<File>();
        InputStream    fis   = getFileInputStream(zipFile);
        OutputStream   fos   = null;
        ZipInputStream zin   = getStorageManager().makeZipInputStream(fis);
        try {
            ZipEntry ze = null;
            while ((ze = zin.getNextEntry()) != null) {
                if (ze.isDirectory()) {
                    continue;
                }
                String path = ze.getName();
                String name = IOUtil.getFileTail(path);
                if (name.equals("MANIFEST.MF")) {
                    continue;
                }
                File f = getTmpFile(request, name);
                files.add(f);
                fos = getStorageManager().getFileOutputStream(f);
                try {
                    IOUtil.writeTo(zin, fos);
                } finally {
                    IOUtil.close(fos);
                }
            }
        } finally {
            IOUtil.close(fis);
            IOUtil.close(zin);
        }

        return files;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param f _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public File uncompressIfNeeded(Request request, File f) throws Exception {
        if (Utils.isCompressed(f.toString())) {
            File uncompressedFile =
                getTmpFile(request,
                           getFileTail(IOUtil.stripExtension(f.toString())));
            InputStream is = Utils.doMakeInputStream(f.toString(), true);
            if (IOUtil.writeTo(is, new FileOutputStream(uncompressedFile))
                    == 0) {
                IOUtil.close(is);

                return null;
            }
            IOUtil.close(is);
            f = uncompressedFile;
        }

        return f;
    }


    /**
     * _more_
     *
     * @param fis _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public ZipInputStream makeZipInputStream(InputStream fis)
            throws Exception {
        String encoding = getRepository().getProperty("ramadda.zip.encoding");
        if (encoding != null) {
            return new ZipInputStream(fis, Charset.forName(encoding));
        } else {
            return new ZipInputStream(fis);
        }
    }



}
