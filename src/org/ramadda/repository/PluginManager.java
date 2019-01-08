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


import org.ramadda.repository.admin.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.harvester.*;
import org.ramadda.repository.search.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.MultiJarClassLoader;
import org.ramadda.util.MyTrace;
import org.ramadda.util.TempDir;


import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;



import ucar.unidata.util.PluginClassLoader;

import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;



import java.io.*;

import java.io.File;
import java.io.InputStream;



import java.lang.reflect.*;

import java.net.*;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import java.util.jar.*;

import java.util.regex.*;
import java.util.zip.*;


/**
 * This class loads and manages the plugins
 */
public class PluginManager extends RepositoryManager {

    //Uggh

    /** _more_ */
    public static final String PLUGIN_ALL =
        "/org/ramadda/repository/resources/plugins/allplugins.jar";

    /** _more_ */
    public static final String PLUGIN_CORE =
        "/org/ramadda/repository/resources/plugins/coreplugins.jar";

    /** _more_ */
    public static final String PLUGIN_GEO =
        "/org/ramadda/repository/resources/plugins/geoplugins.jar";

    /** _more_ */
    public static final String PLUGIN_BIO =
        "/org/ramadda/repository/resources/plugins/bioplugins.jar";


    /** _more_ */
    public static final String PLUGIN_MISC =
        "/org/ramadda/repository/resources/plugins/miscplugins.jar";


    //This should be a properties file but...

    /** _more_ */
    public static final String[] PLUGINS = { PLUGIN_CORE, PLUGIN_GEO,
                                             PLUGIN_MISC, PLUGIN_BIO };



    /** _more_ */
    private StringBuffer pluginFilesList = new StringBuffer();

    /** _more_ */
    private StringBuffer pluginsList = new StringBuffer();

    /** _more_ */
    private List<String> propertyFiles = new ArrayList<String>();

    /** _more_ */
    private List<String> templateFiles = new ArrayList<String>();

    /** _more_ */
    private List<String> sqlFiles = new ArrayList<String>();

    /** _more_ */
    private List<String> pluginFiles = new ArrayList<String>();

    /** _more_ */
    private List<MultiJarClassLoader> classLoaders =
        new ArrayList<MultiJarClassLoader>();

    /** _more_ */
    private MyClassLoader classLoader;

    /** _more_ */
    File tmpPluginsDir;

    /** _more_ */
    private Properties properties;

    /** _more_ */
    private List<String> allFiles = new ArrayList<String>();


    /** _more_ */
    private List<String> metadataDefFiles = new ArrayList<String>();

    /** _more_ */
    private List<String> typeDefFiles = new ArrayList<String>();


    /** _more_ */
    private List<String> apiDefFiles = new ArrayList<String>();

    /** _more_ */
    private List<String> outputDefFiles = new ArrayList<String>();


    /** _more_ */
    private List<String> classDefFiles = new ArrayList<String>();


    /** _more_ */
    private List<String> pythonLibs = new ArrayList<String>();




    /** _more_ */
    private Hashtable<String, String> htdocsMap = new Hashtable<String,
                                                      String>();

    /** _more_ */
    private List<String[]> docUrls = new ArrayList<String[]>();

    /** _more_ */
    private List<String[]> lastDocUrls = new ArrayList<String[]>();

    /** _more_ */
    private List<Class> adminHandlerClasses = new ArrayList<Class>();

    /** _more_ */
    private List<Class> specialClasses = new ArrayList<Class>();


    /** _more_ */
    private List<PageDecorator> pageDecorators =
        new ArrayList<PageDecorator>();

    /** _more_ */
    private List<ImportHandler> importHandlers =
        new ArrayList<ImportHandler>();

    /** Keeps track of files we've seen */
    private HashSet seenThings = new HashSet();

    /**
     * _more_
     *
     * @param repository _more_
     */
    public PluginManager(Repository repository) {
        super(repository);
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void shutdown() throws Exception {
        for (MultiJarClassLoader classLoader : classLoaders) {
            classLoader.shutdown();
        }

        super.shutdown();
    }


    /**
     * _more_
     *
     * @param object _more_
     */
    public void markSeen(Object object) {
        seenThings.add(object);
    }

    /**
     * _more_
     *
     * @param object _more_
     *
     * @return _more_
     */
    public boolean haveSeen(Object object) {
        return haveSeen(object, true);
    }

    /**
     * _more_
     *
     * @param object _more_
     * @param andMark _more_
     *
     * @return _more_
     */
    public boolean haveSeen(Object object, boolean andMark) {
        boolean contains = seenThings.contains(object);
        if ( !contains && andMark) {
            markSeen(object);
        }

        return contains;
    }


    /**
     * _more_
     *
     * @param properties _more_
     *
     * @throws Exception On badness
     */
    public void init(Properties properties) throws Exception {
        this.properties = properties;
        if (classLoader == null) {
            classLoader = new MyClassLoader(getClass().getClassLoader());
            classLoaders.add(classLoader);
            Misc.addClassLoader(classLoader);
        }

        //Fix the old legacy allplugins.jar
        //If the allplugins was installed then copy the new one over
        File allPlugins =
            new File(IOUtil.joinDir(getStorageManager().getPluginsDir(),
                                    IOUtil.getFileTail(PLUGIN_ALL)));
        if (allPlugins.exists()) {
            getRepository().println(
                "RAMADDA: getting rid of old allplugins.jar file and replacing it with core and geo plugins");
            copyPlugin(PluginManager.PLUGIN_CORE);
            copyPlugin(PluginManager.PLUGIN_GEO);
            allPlugins.delete();
        }

        for (String plugin : PLUGINS) {
            if (new File(
                    IOUtil.joinDir(
                        getStorageManager().getPluginsDir(),
                        IOUtil.getFileTail(plugin))).exists()) {
                getRepository().println("RAMADDA: updating plugin file: "
                                        + IOUtil.getFileTail(plugin));
                copyPlugin(plugin);
            }
        }

        loadPlugins();
        apiDefFiles.addAll(0, getRepository().getResourcePaths(PROP_API));
        typeDefFiles.addAll(0, getRepository().getResourcePaths(PROP_TYPES));
        outputDefFiles.addAll(
            0, getRepository().getResourcePaths(PROP_OUTPUTHANDLERS));
        metadataDefFiles.addAll(
            0, getRepository().getResourcePaths(PROP_METADATA));
    }


    /**
     * _more_
     *
     * @throws Exception On badness
     */
    public void loadPlugins() throws Exception {
        //The false says not to scour
        TempDir tempDir = getStorageManager().makeTempDir("tmpplugins",
                              false);
        tmpPluginsDir = tempDir.getDir();
        File   dir   = getStorageManager().getPluginsDir();
        File[] files = dir.listFiles();
        Arrays.sort(files);
        List<File> plugins     = new ArrayList<File>();
        List<File> lastPlugins = new ArrayList<File>();
        for (int i = 0; i < files.length; i++) {
            if (files[i].toString().indexOf(".last.") >= 0) {
                lastPlugins.add(files[i]);
            } else {
                plugins.add(files[i]);
            }
        }
        Collections.sort(lastPlugins);
        plugins.addAll(lastPlugins);

        for (int i = 0; i < plugins.size(); i++) {
            File plugin = plugins.get(i);
            if (plugin.isDirectory()) {
                continue;
            }
            if (plugin.toString().endsWith("~")) {
                continue;
            }
            String pluginFile = plugin.toString();
            if (haveSeen(pluginFile)) {
                continue;
            }
            try {
                processPluginFile(pluginFile, pluginFilesList, classLoader,
                                  true);
            } catch (Exception exc) {
                System.err.println("RAMADDA: Error loading plugin:"
                                   + pluginFile);

                throw exc;
            }
        }
        loadPropertyFiles();

        //Now check for any special classes that need loading
        for (String classDefFile : classDefFiles) {
            for (String classToLoad :
                    StringUtil.split(
                        getStorageManager().readSystemResource(classDefFile),
                        "\n", true, true)) {
                try {
                    if (classToLoad.startsWith("#")) {
                        continue;
                    }
                    List<String> toks = StringUtil.split(classToLoad, ";",
                                            true, true);
                    if (toks.size() > 1) {
                        classToLoad = toks.get(0);
                        toks.remove(0);
                    } else {
                        toks = null;
                    }
                    Class c = Misc.findClass(classToLoad);
                    classLoader.checkSpecialClass(c, toks);
                } catch (Exception exc) {
                    System.err.println("Loading class: " + classToLoad
                                       + " from class file: " + classDefFile);

                    throw exc;
                }
            }

        }

    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void loadPluginsFinish() throws Exception {
        //Now check for the unknown plugin files
        List<String> remainder = new ArrayList<String>();
        for (String file : pluginFiles) {
            boolean didIt = false;
            for (AdminHandler adminHandler : getAdmin().getAdminHandlers()) {
                if (adminHandler.loadPluginFile(file)) {
                    didIt = true;

                    break;
                }
            }
            if ( !didIt) {
                remainder.add(file);
            }
        }
        pluginFiles = remainder;
    }

    /**
     * _more_
     *
     * @throws Exception On badness
     */
    public void loadPropertyFiles() throws Exception {
        for (String f : propertyFiles) {
            //            if (haveSeen(f)) {
            //                continue;
            //            }
            getRepository().loadProperties(properties, f);
        }
    }



    /**
     * _more_
     *
     * @param pluginFile _more_
     * @param pluginSB _more_
     * @param classLoader _more_
     * @param top _more_
     *
     * @throws Exception On badness
     */
    private void processPluginFile(String pluginFile, StringBuffer pluginSB,
                                   MultiJarClassLoader classLoader,
                                   boolean top)
            throws Exception {

        File tmpPluginFile = new File(pluginFile);
        if (pluginFile.toLowerCase().endsWith(".zip")) {
            ZipInputStream zin = getStorageManager().makeZipInputStream(
                                     new FileInputStream(pluginFile));
            ZipEntry ze = null;
            while ((ze = zin.getNextEntry()) != null) {
                if (ze.isDirectory()) {
                    continue;
                }
                String path = ze.getName();

                //Turn the path into a filename
                path = path.replaceAll("/", "_");
                File tmpFile = new File(IOUtil.joinDir(tmpPluginsDir, path));
                //Skip the manifest
                if (tmpFile.toString().indexOf("MANIFEST") >= 0) {
                    continue;
                }
                //Write out the zipped file and load it as a plugin
                OutputStream fos =
                    getStorageManager().getFileOutputStream(tmpFile);
                IOUtil.writeTo(zin, fos);
                IOUtil.close(fos);
                processPluginFile(tmpFile.toString(), pluginSB, classLoader,
                                  false);
            }
            zin.close();
        } else if (pluginFile.toLowerCase().endsWith(".jar")) {
            pluginSB.append(
                "<tr class=\"ramadda-plugin-row\"><td><b>Plugin file</b></td><td colspan=2><i>"
                + pluginFile + "</i> "
                + new Date(tmpPluginFile.lastModified()) + " Length:"
                + tmpPluginFile.length() + "</td></tr>");
            classLoader.addJar(pluginFile);
        } else {
            pluginSB.append(
                "<tr class=\"ramadda-plugin-row\"><td><b>Plugin file</b></td><td colspan=2><i>"
                + pluginFile + "</i>   "
                + new Date(tmpPluginFile.lastModified()) + " Length:"
                + tmpPluginFile.length() + "</td></tr>");
            checkFile(pluginFile, true);
        }
    }


    /**
     * _more_
     *
     * @param file _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected boolean reloadFile(String file) throws Exception {
        boolean contains = seenThings.contains(file);
        seenThings.remove(file);
        checkFile(file);

        return contains;
    }


    /**
     * _more_
     *
     * @param file _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected boolean checkFile(String file) throws Exception {
        return checkFile(file, false);
    }

    /**
     * _more_
     *
     * @param desc _more_
     * @param what _more_
     */
    private void pluginStat(String desc, Object what) {
        pluginsList.append("<tr><td></td><td><b>" + desc + "</b></td><td><i>"
                           + what + "</i></td></tr>");
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public Result adminPluginUpload(Request request) throws Exception {
        String pluginFile = request.getUploadedFile(ARG_PLUGIN_FILE);
        if ((pluginFile == null) || !new File(pluginFile).exists()) {
            return getAdmin().makeResult(
                request, "Administration",
                new StringBuffer("No plugin file provided"));
        }
        if (installPlugin(pluginFile)) {
            return getAdmin().makeResult(
                request, "Administration",
                new StringBuffer(
                    "Plugin has been re-installed.<br><b>Note: Reinstalling a plugin can lead to odd behavior. It is probably best to restart RAMADDA</b>"));
        } else {
            return getAdmin().makeResult(
                request, "Administration",
                new StringBuffer("Plugin installed"));
        }
    }


    /**
     * _more_
     *
     * @param pluginPath _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    public boolean installPlugin(String pluginPath) throws Exception {
        try {
            String newPluginFile = copyPlugin(pluginPath);
            boolean haveLoadedBefore =
                getPluginManager().reloadFile(newPluginFile);

            //            processPluginFile(newPluginFile, pluginFilesList, classLoader, true);
            //            loadPlugins();
            return haveLoadedBefore;
        } catch (Exception exc) {
            getLogManager().logError("Error installing plugin:" + pluginPath,
                                     exc);
        }

        return false;
    }



    /**
     * _more_
     *
     * @param pluginPath _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String copyPlugin(String pluginPath) throws Exception {
        //Remove any ..._file_ prefix
        String tail = RepositoryUtil.getFileTail(pluginPath);
        String newPluginFile =
            IOUtil.joinDir(getStorageManager().getPluginsDir(), tail);
        InputStream      inputStream = IOUtil.getInputStream(pluginPath);
        FileOutputStream fos         = new FileOutputStream(newPluginFile);
        IOUtil.writeTo(inputStream, fos);
        IOUtil.close(inputStream);
        IOUtil.close(fos);

        return newPluginFile;
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param sb _more_
     */
    public void addStatusInfo(Request request, StringBuffer sb) {
        StringBuffer formBuffer = new StringBuffer();
        request.uploadFormWithAuthToken(formBuffer,
                                        getAdmin().URL_ADMIN_PLUGIN_UPLOAD,
                                        "");
        formBuffer.append(msgLabel("Plugin File"));
        formBuffer.append(HtmlUtils.fileInput(ARG_PLUGIN_FILE,
                HtmlUtils.SIZE_60));
        formBuffer.append(HtmlUtils.submit("Upload new plugin file"));
        formBuffer.append(HtmlUtils.formClose());

        formBuffer.append(HtmlUtils.p());
        formBuffer.append(HtmlUtils.h2(msg("Main plugin files")));
        formBuffer.append(HtmlUtils.table(pluginFilesList.toString()));
        formBuffer.append(HtmlUtils.b(msg("Installed Plugins")));
        formBuffer.append(HtmlUtils.table(pluginsList.toString()));
        sb.append(formBuffer.toString());
    }


    /**
     * _more_
     *
     * @param file _more_
     * @param fromPlugin _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected boolean checkFile(String file, boolean fromPlugin)
            throws Exception {
        allFiles.add(file);

        if (file.indexOf("api.xml") >= 0) {
            if (fromPlugin) {
                pluginStat("Api", file);
            }
            apiDefFiles.add(file);
        } else if ((file.endsWith("types.xml"))
                   || (file.endsWith("type.xml"))) {
            if (fromPlugin) {
                pluginStat("Types", file);
            }
            typeDefFiles.add(file);
        } else if (file.indexOf("outputhandlers.xml") >= 0) {
            if (fromPlugin) {
                pluginStat("Output", file);
            }
            outputDefFiles.add(file);
        } else if (file.indexOf("classes.txt") >= 0) {
            if (fromPlugin) {
                pluginStat("Output", file);
            }
            classDefFiles.add(file);
        } else if (file.indexOf("metadata.xml") >= 0) {
            if (fromPlugin) {
                pluginStat("Metadata", file);
            }
            metadataDefFiles.add(file);
        } else if (file.endsWith(".py")) {
            if (fromPlugin) {
                pluginStat("Python", file);
            }
            pythonLibs.add(file);
        } else if (file.endsWith(".sql")) {
            if (fromPlugin) {
                pluginStat("Sql", file);
            }
            sqlFiles.add(file);
        } else if (file.endsWith("template.html")) {
            if (fromPlugin) {
                pluginStat("Template", file);
            }
            templateFiles.add(file);
        } else if (file.endsWith(".properties")
                   && (file.indexOf("htdocs") < 0)) {
            if (fromPlugin) {
                pluginStat("Properties", file);
                propertyFiles.add(file);
            }
        } else if (file.endsWith(".jar") && (file.indexOf("htdocs") < 0)) {
            try {
                File tmpFile = new File(IOUtil.joinDir(tmpPluginsDir,
                                   IOUtil.getFileTail(file)));
                IOUtil.writeTo(getStorageManager().getInputStream(file),
                               new FileOutputStream(tmpFile));
                processPluginFile(tmpFile.toString(), pluginFilesList,
                                  classLoader, false);
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        } else {
            boolean didIt = false;
            for (AdminHandler adminHandler : getAdmin().getAdminHandlers()) {
                if (adminHandler.loadPluginFile(file)) {
                    didIt = true;

                    break;
                }
            }
            if ( !didIt) {
                pluginFiles.add(file);

                return false;
            }
        }

        return true;
    }


    /**
     * Class MyClassLoader provides a hook into the MultiJarClassLoader routines
     *
     */
    private class MyClassLoader extends MultiJarClassLoader {

        /** _more_ */
        private HashSet seenClasses = new HashSet();

        /**
         * _more_
         *
         * @param parent _more_
         *
         * @throws Exception On badness
         */
        public MyClassLoader(ClassLoader parent) throws Exception {
            super(parent);
        }


        /**
         * _more_
         *
         * @param name _more_
         *
         * @return _more_
         *
         * @throws ClassNotFoundException On badness
         */
        public Class xxxloadClass(String name) throws ClassNotFoundException {
            try {
                Class clazz = super.loadClass(name);
                if (clazz != null) {
                    return clazz;
                }

                return clazz;
            } catch (ClassNotFoundException cnfe) {
                for (MultiJarClassLoader loader : classLoaders) {
                    Class clazz = loader.getClassFromPlugin(name);
                    if (clazz != null) {
                        return clazz;
                    }
                }

                throw cnfe;
            }
        }


        /*
        public void checkClass(Class c) throws Exception {
            this.checkSpecialClass(c,null);
            super.checkClass(c);
        }
        */


        /**
         * Check if this class is one of the special classes, e.g., ImportHandler, PageDecorator, etc.
         *
         * @param c the class
         * @param args _more_
         *
         * @throws Exception On badness
         */
        public void checkSpecialClass(Class c, List<String> args)
                throws Exception {
            String key = c.getName();
            if (args != null) {
                key += args.toString();
            }
            if (seenClasses.contains(key)) {
                //                System.out.println ("class: Seen it:" + c.getName());
                return;
            }
            seenClasses.add(key);


            if (ImportHandler.class.isAssignableFrom(c)) {
                //                System.out.println("class:" + c.getName());
                pluginStat("Import handler", c.getName());
                Constructor ctor = Misc.findConstructor(c,
                                       new Class[] { Repository.class });
                if (ctor != null) {
                    importHandlers.add(
                        (ImportHandler) ctor.newInstance(
                            new Object[] { getRepository() }));
                } else {
                    importHandlers.add((ImportHandler) c.newInstance());
                }

                return;
            }

            if (SearchProvider.class.isAssignableFrom(c)) {
                String className = c.getName();
                pluginStat("Search provider", className);
                String         extra    = null;
                SearchProvider provider = null;
                Constructor    ctor     = null;
                if ((args != null) && (args.size() > 0)) {
                    //                    System.err.println("args:" + args);
                    ctor = Misc.findConstructor(c,
                            new Class[] { Repository.class,
                                          List.class });
                    provider =
                        (SearchProvider) ctor.newInstance(new Object[] {
                            getRepository(),
                            args });
                } else {
                    ctor = Misc.findConstructor(c,
                            new Class[] { Repository.class });
                    if (ctor != null) {
                        provider =
                            (SearchProvider) ctor.newInstance(new Object[] {
                                getRepository() });
                    } else {
                        provider = (SearchProvider) c.newInstance();
                    }
                }
                getSearchManager().addPluginSearchProvider(provider);

                return;
            }

            if (UserAuthenticator.class.isAssignableFrom(c)) {
                //                System.out.println("class:" + c.getName());
                pluginStat("Authenticator", c.getName());
                Constructor ctor = Misc.findConstructor(c,
                                       new Class[] { Repository.class });
                if (ctor != null) {
                    getUserManager().addUserAuthenticator(
                        (UserAuthenticator) ctor.newInstance(
                            new Object[] { getRepository() }));

                } else {
                    getUserManager().addUserAuthenticator(
                        (UserAuthenticator) c.newInstance());
                }
            } else if (PageDecorator.class.isAssignableFrom(c)) {
                //                                System.out.println("class:" + c.getName());
                pluginStat("Page decorator", c.getName());
                PageDecorator pageDecorator = (PageDecorator) c.newInstance();
                pageDecorator.setRepository(getRepository());
                pageDecorators.add(pageDecorator);
            } else if (AdminHandler.class.isAssignableFrom(c)) {
                //                                System.out.println("class:" + c.getName());
                pluginStat("Admin handler", c.getName());
                adminHandlerClasses.add(c);
            } else if (Harvester.class.isAssignableFrom(c)) {
                //                                System.out.println("class:" + c.getName());
                pluginStat("Harvester", c.getName());
                getHarvesterManager().addHarvesterType(c);
            } else {
                specialClasses.add(c);
            }
        }

        /**
         * _more_
         *
         *
         * @param jarFilePath _more_
         * @param jarEntry _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        protected String defineResource(String jarFilePath, JarEntry jarEntry)
                throws Exception {
            String path = super.defineResource(jarFilePath, jarEntry);
            checkFile(path, true);




            String entryName = jarEntry.getName();
            int    idx       = entryName.indexOf("htdocs/");

            if (idx >= 0) {
                String htpath = entryName.substring(idx + "htdocs".length());
                htdocsMap.put(htpath, path);

                if (htpath.matches("/[^/]+/index.html")) {
                    try {
                        String contents =
                            getStorageManager().readSystemResource(path);
                        Pattern pattern =
                            Pattern.compile("(?s).*<title>(.*)</title>");
                        Matcher matcher = pattern.matcher(contents);
                        String category = StringUtil.findPattern(contents,
                                              "<category:([^>]+)>");
                        String title = htpath;
                        if (matcher.find()) {
                            title = matcher.group(1);
                        }
                        String url = htpath;
                        if (htpath.startsWith("/userguide")) {
                            docUrls.add(0, new String[] { url, title,
                                    (category == null)
                                    ? "Basics"
                                    : category });
                        } else {
                            docUrls.add(new String[] { url, title,
                                    category });
                        }
                    } catch (Exception exc) {
                        throw new RuntimeException(exc);
                    }
                }
            }

            return path;
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List<String[]> getDocUrls() {
        return docUrls;
    }

    /**
     * Get the MetadataDefFiles property.
     *
     * @return The MetadataDefFiles
     */
    public List<String> getMetadataDefFiles() {
        return metadataDefFiles;
    }


    /**
     * Get the TypeDefFiles property.
     *
     * @return The TypeDefFiles
     */
    public List<String> getTypeDefFiles() {
        return typeDefFiles;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<String> getAllFiles() {
        return allFiles;
    }


    /**
     * Get the ApiDefFiles property.
     *
     * @return The ApiDefFiles
     */
    public List<String> getApiDefFiles() {
        return apiDefFiles;
    }



    /**
     * Get the OutputDefFiles property.
     *
     * @return The OutputDefFiles
     */
    public List<String> getOutputDefFiles() {
        return outputDefFiles;
    }

    /**
     * Get the PythonLibs property.
     *
     * @return The PythonLibs
     */
    public List<String> getPythonLibs() {
        return pythonLibs;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<String> getPluginFiles() {
        return pluginFiles;
    }

    /**
     * Get the SqlFiles property.
     *
     * @return The SqlFiles
     */
    public List<String> getSqlFiles() {
        return sqlFiles;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List<PageDecorator> getPageDecorators() {
        return pageDecorators;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<ImportHandler> getImportHandlers() {
        return importHandlers;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public Hashtable<String, String> getHtdocsMap() {
        return htdocsMap;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<String> getTemplateFiles() {
        return templateFiles;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public List<Class> getAdminHandlerClasses() {
        return adminHandlerClasses;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<Class> getSpecialClasses() {
        return specialClasses;
    }
}
