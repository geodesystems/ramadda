/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.ObjectPair;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.WrapperException;

import java.io.*;

import java.lang.reflect.*;

import java.net.*;

import java.security.*;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.jar.*;
import java.util.regex.*;



/**
 * Class PluginClassLoader. Loads plugin classes
 *
 * @version $Revision: 1.54 $
 */
@SuppressWarnings("unchecked")
public class MultiJarClassLoader extends ClassLoader {

    /** for url plugins */
    public static final String PLUGIN_PROTOCOL = "idvresource";

    /** Mapping from path name to class */
    private Hashtable<String, Class> loadedClasses = new Hashtable<String,
                                                         Class>();

    /** The jar file we are loading from */
    private List<JarFile> jarFiles = new ArrayList<JarFile>();


    /** Mapping of resource name to jar entry */
    Hashtable<String, String> canonicalNames = new Hashtable<String,
                                                   String>();

    /** For handling getResource */
    private URLStreamHandler urlStreamHandler;

    /** List of non class jar entry names */
    private List entryNames = new ArrayList();


    /** The parent class loader */
    private ClassLoader parent;

    /**
     * _more_
     *
     * @param parent _more_
     *
     * @throws IOException _more_
     */
    public MultiJarClassLoader(ClassLoader parent) throws IOException {
        super(parent);
        this.parent      = parent;
        urlStreamHandler = new URLStreamHandler() {
            protected URLConnection openConnection(URL u) throws IOException {
                return openURLConnection(u);
            }
        };
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void shutdown() throws Exception {
        loadedClasses = null;
    }

    /**
     * ctor
     *
     *
     * @param jarFilePath Where the jar file is
     *
     * @throws Exception _more_
     */
    public void addJar(String jarFilePath) throws Exception {
        JarFile jarFile = new JarFile(jarFilePath);
        //Check if we have already loaded this jar file
        //If so then close the old one and put the new one in the list
        boolean replacedJarFile = false;
        //        System.err.println ("add jar:" + jarFilePath);
        for (int i = 0; i < jarFiles.size(); i++) {
            JarFile oldJarFile = jarFiles.get(i);
            if (Misc.equals(jarFilePath, oldJarFile.getName())) {
                //                System.err.println ("\tOLD JAR");
                oldJarFile.close();
                jarFiles.set(i, jarFile);
                replacedJarFile = true;

                break;
            }
        }

        if ( !replacedJarFile) {
            //            System.err.println("\tNEW JAR");
            jarFiles.add(jarFile);
        }

        List entries = Misc.toList(jarFile.entries());
        //First load in the class files
        for (int i = 0; i < entries.size(); i++) {
            JarEntry entry = (JarEntry) entries.get(i);
            if (entry.isDirectory()) {
                continue;
            }
            String name = entry.getName();
            if (name.endsWith(".class")) {
                //System.err.println ("class:"+entry.getName());
                try {
                    Class c = loadClassFromJar(entry.getName());
                } catch (java.lang.LinkageError jlle) {
                    handleError("Error loading plugin class:"
                                + entry.getName(), jlle);
                }
            } else {
                defineResource(jarFilePath, entry);
                entryNames.add(entry.getName());
            }
        }
    }


    /**
     * _more_
     *
     * @param msg _more_
     * @param exc _more_
     */
    protected void handleError(String msg, Throwable exc) {
        throw new WrapperException(msg, exc);
    }

    /**
     * Close the jar file
     */
    public void closeJar() {
        try {
            for (JarFile jarFile : jarFiles) {
                jarFile.close();
            }
            jarFiles = new ArrayList<JarFile>();
        } catch (IOException exc) {}
    }


    /**
     * Get the list of (String) names of the non-class files in the jar
     *
     * @return List of jar entries
     */
    public List getEntryNames() {
        return entryNames;
    }

    /**
     * Create our own URLConnection for handling getResource
     *
     * @param u The url
     *
     * @return The connectio
     *
     * @throws IOException On badness
     */
    private URLConnection openURLConnection(final URL u) throws IOException {
        return new URLConnection(u) {
            public void connect() throws IOException {}

            public InputStream getInputStream() throws IOException {
                return getResourceAsStream(u.getFile());
            }
        };
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Feb 12, '11
     * @author         Enter your name here...
     */
    private static class MyJarEntry {

        /** _more_ */
        JarFile jarFile;

        /** _more_ */
        JarEntry jarEntry;

        /**
         * _more_
         *
         * @param jarFile _more_
         * @param jarEntry _more_
         */
        MyJarEntry(JarFile jarFile, JarEntry jarEntry) {
            this.jarFile  = jarFile;
            this.jarEntry = jarEntry;
        }
    }

    /**
     * Load in the class from the jar.
     *
     *
     * @param entryName Name of entry
     *
     * @return The class.
     */
    private Class loadClassFromJar(String entryName) {
        MyJarEntry jarEntry = null;
        try {
            //            System.err.println("   entry:" + entryName);
            jarEntry = findJarEntry(entryName);
            Class c = loadedClasses.get(entryName);
            if (c != null) {
                return c;
            }
            InputStream is =
                jarEntry.jarFile.getInputStream(jarEntry.jarEntry);
            final byte[] bytes = IOUtil.readBytes(is);
            is.close();
            c = loadClass(bytes);
            loadedClasses.put(c.getName(), c);
            loadedClasses.put(entryName, c);
            loadedClasses.put(jarEntry.jarEntry.getName(), c);
            checkClass(c);

            return c;
        } catch (Exception exc) {
            System.err.println("RAMADDA: Error loading class from:"
                               + jarEntry.jarFile + " class:" + entryName);
            exc.printStackTrace();

            throw new IllegalArgumentException("Could not load class:"
                    + entryName + "\n" + exc);
        }
    }


    /**
     * _more_
     *
     * @param c _more_
     *
     * @throws Exception _more_
     */
    public void checkClass(Class c) throws Exception {}

    /**
     * _more_
     *
     * @param filename _more_
     *
     * @return _more_
     */
    private MyJarEntry findJarEntry(String filename) {
        JarEntry jarEntry = null;
        for (JarFile jarFile : jarFiles) {
            jarEntry = jarFile.getJarEntry(filename);
            if (jarEntry != null) {
                return new MyJarEntry(jarFile, jarEntry);
            }
        }

        return null;
    }

    /**
     * Overwrite base class method to load in a class by name
     *
     * @param name class name
     *
     * @return The class
     *
     * @throws ClassNotFoundException On badness
     */
    public Class loadClass(String name) throws ClassNotFoundException {
        //Check if we have this class as a jar entry
        Class c = (Class) loadedClasses.get(name);
        if (c != null) {
            return c;
        }
        String fileName = StringUtil.replace(name, ".", "/");
        fileName += ".class";
        MyJarEntry jarEntry = findJarEntry(fileName);
        if (jarEntry != null) {
            return loadClassFromJar(jarEntry.jarEntry.getName());
        } else {
            return super.loadClass(name);
        }
    }


    /**
     * Check if this class is one we loaded from a plugin
     *
     *
     * @param name _more_
     * @return the class or null
     */
    public Class getClassFromPlugin(String name) {
        return (Class) loadedClasses.get(name);
    }



    /**
     * Associate the resource name with the jar entry
     *
     *
     * @param jarFilePath _more_
     * @param jarEntry THe entry
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String defineResource(String jarFilePath, JarEntry jarEntry)
            throws Exception {
        String entryName = jarEntry.getName();
        String name      = jarEntry.getName();
        canonicalNames.put("/" + name, entryName);
        canonicalNames.put(jarFilePath + "!" + name, entryName);
        name = "/" + name;
        canonicalNames.put(jarFilePath + "!" + name, entryName);
        String path;
        canonicalNames.put(path = PLUGIN_PROTOCOL + ":" + jarFilePath + "!"
                                  + name, entryName);
        canonicalNames.put(PLUGIN_PROTOCOL + ":" + name, entryName);

        return path;
    }


    /**
     * Get the actual name that is used in the jar file
     * The resource might have teh PLUGIN_PROTOCOL prepended to it, etc.
     *
     * @param resource the resource
     *
     * @return jar name
     */
    private String getCanonicalName(String resource) {
        return (String) canonicalNames.get(resource);
    }

    /**
     * Open the resource as a URL
     *
     * @param resource The resource
     *
     * @return The URL
     */
    public URL getResource(String resource) {
        String name = getCanonicalName(resource);
        if (name == null) {
            return super.getResource(resource);
        }
        try {
            return new URL(PLUGIN_PROTOCOL, "", -1, resource,
                           urlStreamHandler);
        } catch (Exception exc) {
            return null;
        }
    }


    /**
     * Open the resource as a istream if we have it
     *
     * @param resource The resource
     *
     * @return The istream
     */
    public InputStream getResourceAsStream(String resource) {
        String jarEntryName = getCanonicalName(resource);
        if (jarEntryName != null) {
            MyJarEntry jarEntry = findJarEntry(jarEntryName);
            try {
                return jarEntry.jarFile.getInputStream(
                    jarEntry.jarFile.getJarEntry(jarEntryName));
            } catch (Exception exc) {}
        }

        return null;
    }


    /**
     * Load class bytes
     *
     * @param bytes class bytes
     *
     * @return New class
     */
    private Class loadClass(byte[] bytes) {
        PermissionCollection pc = new Permissions();
        pc.add(new AllPermission());
        CodeSource codeSource = new CodeSource((URL) null,
                                    (java.security.cert.Certificate[]) null);
        ProtectionDomain pd = new ProtectionDomain(codeSource, pc);

        return defineClass((String) null, bytes, 0, bytes.length, pd);
    }
}
