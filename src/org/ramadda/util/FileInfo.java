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

package org.ramadda.util;


import java.io.File;



/**
 * Class FileInfo holds information about a file or directory
 *
 *
 */
public class FileInfo {

    /** tracks whether we have initialized ourselves */
    private boolean hasInitialized = false;

    /** The file */
    private File file;

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


    /**
     * ctor
     *
     * @param f the file
     */
    public FileInfo(File f) {
        this(f, f.isDirectory());

    }

    /**
     * ctor
     *
     * @param f the file
     * @param isDir is file a directory
     */
    public FileInfo(File f, boolean isDir) {
        this.isDir = isDir;
        file       = f;
    }

    /**
     * _more_
     */
    private void doInit() {
        time = file.lastModified();
        if ( !isDir) {
            size = file.length();
        } else {
            File[] files = file.listFiles();
            if (files != null) {
                fileCount = files.length;
                for (File child : files) {
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
            File[] files = this.file.listFiles();
            if (files != null) {
                newFileCount = files.length;
                for (File child : files) {
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
    public File getFile() {
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



}
