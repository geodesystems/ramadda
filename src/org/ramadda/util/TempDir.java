/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import ucar.unidata.util.IOUtil;

import java.io.*;

import java.net.*;

import java.util.ArrayList;
import java.util.Arrays;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import java.util.zip.*;


/**
 *
 * @version $Revision: 1.52 $
 */
public class TempDir {

    /** _more_ */
    private File dir;

    /** _more_ */
    boolean recurse = true;

    /** _more_ */
    boolean filesOk = true;

    /** _more_ */
    boolean dirsOk = true;

    /** _more_ */
    private long currentDirTime = 0;

    /** _more_ */
    private int maxFiles = -1;

    /** _more_ */
    private long maxSize = -1;

    /** _more_ */
    private long maxAge = -1;

    /** _more_ */
    private boolean touched = false;



    /**
     * _more_
     *
     * @param dir _more_
     */
    public TempDir(String dir) {
        this(new File(dir));
    }


    /**
     * _more_
     *
     * @param dir _more_
     */
    public TempDir(File dir) {
        this(dir, true);
    }

    /**
     * _more_
     *
     * @param dir _more_
     * @param recurse _more_
     */
    public TempDir(File dir, boolean recurse) {
        this.dir     = dir;
        this.recurse = recurse;
    }


    /**
     * _more_
     *
     * @param dir _more_
     * @param maxFiles _more_
     * @param maxSize _more_
     * @param maxAge _more_
     */
    public TempDir(File dir, int maxFiles, long maxSize, long maxAge) {
        this(dir);
        this.maxFiles = maxFiles;
        this.maxSize  = maxSize;
        this.maxAge   = maxAge;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public boolean haveChanged() {
        return currentDirTime != dir.lastModified();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        if (maxAge > 0) {
            return dir.toString() + " max age:" + (maxAge / 1000 / 3600)
                   + " hours";
        }

        return dir.toString();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public File[] listFiles() {
        return dir.listFiles();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<File> findFilesToScour() {
        List<File> results = new ArrayList<File>();

        List<File> allFiles;
        if (recurse) {
            allFiles = IOUtil.getFiles(dir, true);
        } else {
            allFiles = new ArrayList<File>();
            for (File f : dir.listFiles()) {
                allFiles.add(f);
            }
        }
        List<File> prunedFiles = new ArrayList<File>();
        for (File f : allFiles) {
            if ( !filesOk && f.isFile()) {
                continue;
            }
            if (f.isDirectory()) {
                continue;
            }
            prunedFiles.add(f);
        }

        allFiles = prunedFiles;
        //Sort files oldest first
        IOUtil.FileWrapper[] files =
            IOUtil.sortFilesOnAge(IOUtil.FileWrapper.toArray(allFiles,
                false));
        long now       = new Date().getTime();

        long totalSize = 0;
        int  numFiles  = 0;
        for (int i = 0; i < files.length; i++) {
            numFiles++;
        }

        if (maxSize > 0) {
            for (int i = 0; i < files.length; i++) {
                totalSize += files[i].length();
            }
        }
        //        System.err.println("max age:" + maxAge);
        for (int i = 0; i < files.length; i++) {
            boolean shouldScour = false;
            if ((maxSize > 0) && (totalSize > maxSize)) {
                shouldScour = true;
            }
            if (maxAge > 0) {
                long lastModified = files[i].lastModified();
                long age          = now - lastModified;
                if (age > maxAge) {
                    shouldScour = true;
                }
            }
            if (maxFiles > 0) {
                if (numFiles > maxFiles) {
                    shouldScour = true;
                }
            }

            if ( !shouldScour) {
                break;
            }
            long fileSize = files[i].length();
            results.add(files[i].getFile());
            totalSize -= files[i].length();
            numFiles--;
        }

        if (results.size() > 0) {
            //            System.err.println ("    found " + results.size() + " files to delete");
        }
        currentDirTime = dir.lastModified();

        return results;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List<File> findEmptyDirsToScour() {
        long       now                 = new Date().getTime();
        List<File> results             = new ArrayList<File>();
        List<File> filelessDirectories = IO.getFilelessDirectories(dir);
        //      System.err.println("FILELESS: "+ filelessDirectories);
        for (File sub : filelessDirectories) {
            boolean shouldScour = false;
            if (maxAge > 0) {
                long lastModified = sub.lastModified();
                long age          = now - lastModified;
                if (age > maxAge) {
                    shouldScour = true;
                }
            }
            if (shouldScour) {
                results.add(sub);
            }
        }

        return results;
    }




    /**
     * _more_
     *
     * @return _more_
     */
    public File getDir() {
        return dir;
    }


    /**
     *  Set the MaxFiles property.
     *
     *  @param value The new value for MaxFiles
     */
    public void setMaxFiles(int value) {
        this.maxFiles = value;
    }

    /**
     *  Get the MaxFiles property.
     *
     *  @return The MaxFiles
     */
    public int getMaxFiles() {
        return this.maxFiles;
    }

    /**
     *  Set the MaxBytes property.
     *
     *  @param value The new value for MaxBytes
     */
    public void setMaxSize(long value) {
        this.maxSize = value;
    }

    /**
     *  Get the MaxBytes property.
     *
     *  @return The MaxBytes
     */
    public long getMaxSize() {
        return this.maxSize;
    }

    /**
     * _more_
     *
     * @param value _more_
     */
    public void setMaxAgeMinutes(long value) {
        setMaxAge(1000 * 60 * value);
    }


    /**
     *  Set the MaxAge property.
     *
     *  @param value The new value for MaxAge
     */
    public void setMaxAge(long value) {
        this.maxAge = value;
    }

    /**
     *  Get the MaxAge property.
     *
     *  @return The MaxAge
     */
    public long getMaxAge() {
        return this.maxAge;
    }

    /**
     * Set the Touched property.
     *
     * @param value The new value for Touched
     */
    public void setTouched(boolean value) {
        touched = value;
    }

    /**
     * Get the Touched property.
     *
     * @return The Touched
     */
    public boolean getTouched() {
        return touched;
    }

    /**
     *  Set the FilesOk property.
     *
     *  @param value The new value for FilesOk
     */
    public void setFilesOk(boolean value) {
        filesOk = value;
    }

    /**
     *  Get the FilesOk property.
     *
     *  @return The FilesOk
     */
    public boolean getFilesOk() {
        return filesOk;
    }

    /**
     *  Set the DirsOk property.
     *
     *  @param value The new value for DirsOk
     */
    public void setDirsOk(boolean value) {
        dirsOk = value;
    }

    /**
     *  Get the DirsOk property.
     *
     *  @return The DirsOk
     */
    public boolean getDirsOk() {
        return dirsOk;
    }




}
