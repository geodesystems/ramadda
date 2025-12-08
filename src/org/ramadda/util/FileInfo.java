/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;

import java.io.File;
import java.util.Date;

/**
 * Class FileInfo holds information about a file or directory
 */
public class FileInfo {
    /** tracks whether we have initialized ourselves */
    private boolean hasInitialized = false;

    /** The file */
    private FileWrapper file;
    private String title;
    private String description;
    private long time;
    private long size = 0;
    private int fileCount = 0;
    private boolean isDir;

    public FileInfo(File f) {
	this(new FileWrapper.File(f));
    }

    public FileInfo(FileWrapper f) {
        this(f, f.isDirectory());

    }

    public FileInfo(FileWrapper f, boolean isDir) {
        this.isDir = isDir;
        file       = f;
    }

    public int getLevel() {
	return file.getLevel();
    }

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

    public int hashCode() {
        return file.hashCode();

    }

    public boolean equals(Object obj) {
        if ( !(obj instanceof FileInfo)) {
            return false;
        }
        FileInfo that = (FileInfo) obj;

        return this.file.equals(that.file);
    }

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

    public void reset() {
        time      = -1;
        size      = -1;
        fileCount = 0;
    }

    public FileWrapper getFile() {
        return file;
    }

    public long getTime() {
        return time;
    }

    public boolean exists() {
        return file.exists();
    }

    public void setDescription(String value) {
        description = value;
    }

    public String getDescription() {
        return description;
    }

    public void setTitle(String value) {
        title = value;
    }

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
