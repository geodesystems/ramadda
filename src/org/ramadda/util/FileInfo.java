/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import java.io.File;
import java.util.Date;


/**
 * Class FileInfo holds information about a file or directory
 *
 *
 */
public class FileInfo {

    /** tracks whether we have initialized ourselves */
    private boolean hasInitialized = false;

    /** The file */
    private FileWrapper file;

    /** _more_ */
    private String title;

    /** _more_ */
    private String description;

    /** _more_ */
    private long time;

    /** _more_ */
    private long size = 0;

    /** _more_ */
    private int fileCount = 0;

    /** _more_ */
    private boolean isDir;


    public FileInfo(File f) {
	this(new FileWrapper.File(f));
    }


    /**
     * ctor
     *
     * @param f the file
     */
    public FileInfo(FileWrapper f) {
        this(f, f.isDirectory());

    }

    /**
     * ctor
     *
     * @param f the file
     * @param isDir is file a directory
     */
    public FileInfo(FileWrapper f, boolean isDir) {
        this.isDir = isDir;
        file       = f;
    }

    public int getLevel() {
	return file.getLevel();
    }

    /**
     * _more_
     */
    private void doInit() {
        time = file.lastModified();
        if ( !isDir) {
            size = file.length();
        } else {
            FileWrapper[] files = file.listFiles();
            if (files != null) {
                fileCount = files.length;
                for (FileWrapper child : files) {
                    size += child.length();
                }
            }
        }
        hasInitialized = true;
    }


    /**
     * override hashcode
     *
     * @return hashcode
     */
    public int hashCode() {
        return file.hashCode();

    }

    /**
     * _more_
     *
     * @param obj _more_
     *
     * @return _more_
     */
    public boolean equals(Object obj) {
        if ( !(obj instanceof FileInfo)) {
            return false;
        }
        FileInfo that = (FileInfo) obj;

        return this.file.equals(that.file);
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasChanged() {
        if ( !hasInitialized) {
            doInit();
            return true;
        }
        long newTime      = file.lastModified();
        long newSize      = 0;
        int  newFileCount = 0;

        if (isDir) {
            FileWrapper[] files = this.file.listFiles();
            if (files != null) {
                newFileCount = files.length;
                for (FileWrapper child : files) {
                    newSize += child.length();
                }
            }
        } else {
            newSize = file.length();
        }

        boolean changed = (newTime != time) || (newSize != size)
                          || (newFileCount != fileCount);
        time      = newTime;
        size      = newSize;
        fileCount = newFileCount;
        return changed;
    }

    /**
     * _more_
     */
    public void reset() {
        time      = -1;
        size      = -1;
        fileCount = 0;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public FileWrapper getFile() {
        return file;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public long getTime() {
        return time;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean exists() {
        return file.exists();
    }

    /**
     * Set the Description property.
     *
     * @param value The new value for Description
     */
    public void setDescription(String value) {
        description = value;
    }

    /**
     * Get the Description property.
     *
     * @return The Description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the Title property.
     *
     * @param value The new value for Title
     */
    public void setTitle(String value) {
        title = value;
    }

    /**
     * Get the Title property.
     *
     * @return The Title
     */
    public String getTitle() {
        return title;
    }

    public static void main(String[]args) throws Exception {
	File dir = new File(args[0]);
	System.err.println("modified:" + new Date(dir.lastModified()));
	ucar.unidata.util.Misc.sleepSeconds(10);
	System.err.println("modified:" + new Date(dir.lastModified()));
    }

}
