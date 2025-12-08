/**
Copyright (c) 2008-2026 Geode Systems LLC
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

public class TempDir {
    private File dir;
    boolean recurse = true;
    boolean filesOk = true;
    boolean dirsOk = true;
    private long currentDirTime = 0;
    private int maxFiles = -1;
    private long maxSize = -1;
    private long maxAge = -1;
    private boolean touched = false;

    public TempDir(String dir) {
        this(new File(dir));
    }

    public TempDir(File dir) {
        this(dir, true);
    }

    public TempDir(File dir, boolean recurse) {
        this.dir     = dir;
        this.recurse = recurse;
    }

    public TempDir(File dir, int maxFiles, long maxSize, long maxAge) {
        this(dir);
        this.maxFiles = maxFiles;
        this.maxSize  = maxSize;
        this.maxAge   = maxAge;
    }

    public boolean haveChanged() {
        return currentDirTime != dir.lastModified();
    }

    public String toString() {
        if (maxAge > 0) {
            return dir.toString() + " max age:" + (maxAge / 1000 / 3600)
                   + " hours";
        }

        return dir.toString();
    }

    public File[] listFiles() {
        return dir.listFiles();
    }

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

    public File getDir() {
        return dir;
    }

    public void setMaxFiles(int value) {
        this.maxFiles = value;
    }

    public int getMaxFiles() {
        return this.maxFiles;
    }

    public void setMaxSize(long value) {
        this.maxSize = value;
    }

    public long getMaxSize() {
        return this.maxSize;
    }

    public void setMaxAgeMinutes(long value) {
        setMaxAge(1000 * 60 * value);
    }

    public void setMaxAge(long value) {
        this.maxAge = value;
    }

    public long getMaxAge() {
        return this.maxAge;
    }

    public void setTouched(boolean value) {
        touched = value;
    }

    public boolean getTouched() {
        return touched;
    }

    public void setFilesOk(boolean value) {
        filesOk = value;
    }

    public boolean getFilesOk() {
        return filesOk;
    }

    public void setDirsOk(boolean value) {
        dirsOk = value;
    }

    public boolean getDirsOk() {
        return dirsOk;
    }

}
