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


import org.ramadda.util.Utils;

import java.io.File;

import java.net.URL;




/**
 * Class Entry _more_
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class Resource {

    /** _more_ */
    public static final String TYPE_FILE = "file";

    /** _more_ */
    public static final String TYPE_LOCAL_DIRECTORY = "localdirectory";

    /** _more_ */
    public static final String TYPE_LOCAL_FILE = "localfile";

    /** _more_ */
    public static final String TYPE_STOREDFILE = "storedfile";

    /** _more_ */
    public static final String TYPE_URL = "url";

    /** _more_ */
    public static final String TYPE_REMOTE_FILE = "remotefile";

    /** _more_ */
    public static final String TYPE_UNKNOWN = "unknown";

    /** _more_ */
    private String path;

    /** _more_ */
    private String type = TYPE_UNKNOWN;

    /** _more_ */
    private File file;

    /** _more_ */
    private long fileSize = -1;

    /** _more_ */
    private String md5;

    /**
     * _more_
     */
    public Resource() {
        path = "";
    }


    /**
     * _more_
     *
     * @param file _more_
     * @param type _more_
     */
    public Resource(File file, String type) {
        this.file = file;
        this.path = file.toString();
        this.type = type;
    }



    /**
     * _more_
     *
     * @param path _more_
     */
    public Resource(String path) {
        this.path = path;
        if (new File(path).exists()) {
            type = TYPE_FILE;
        } else {
            try {
                new URL(path);
                type = TYPE_URL;
            } catch (Exception exc) {}
        }
    }


    /**
     * _more_
     *
     * @param url _more_
     */
    public Resource(URL url) {
        type = TYPE_URL;
        path = url.toString();
    }


    /**
     * _more_
     *
     * @param path _more_
     * @param type _more_
     */
    public Resource(String path, String type) {
        this(path, type, null, -1);
    }

    /**
     * _more_
     *
     * @param path _more_
     * @param type _more_
     * @param md5 _more_
     * @param fileSize _more_
     */
    public Resource(String path, String type, String md5, long fileSize) {
        this.path     = path;
        this.type     = type;
        this.md5      = md5;
        this.fileSize = fileSize;
        if ((fileSize < 0) && (path != null)) {
            fileSize = new File(path).length();
        }
    }

    /**
     * _more_
     *
     * @param that _more_
     */
    public Resource(Resource that) {
        this.path     = that.path;
        this.type     = that.type;
        this.md5      = that.md5;
        this.fileSize = that.fileSize;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isDefined() {
        return (path != null) && (path.length() > 0);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isImage() {
        return isImage(path);
    }

    /**
     * _more_
     *
     * @param path _more_
     *
     * @return _more_
     */
    public static boolean isImage(String path) {
        return Utils.isImage(path);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isEditableImage() {
        if (path == null) {
            return false;
        }
        String file = path.toLowerCase();

        return file.endsWith(".jpg") || file.endsWith(".jpeg")
               || file.endsWith(".gif") || file.endsWith(".png");
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public File getTheFile() {
        if (file == null) {
            file = new File(path);
        }

        return file;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public long getFileSize() {
        if (fileSize > 0) {
            return fileSize;
        }
        File file = getTheFile();
        if (file.exists()) {
            fileSize = file.length();
        }

        return fileSize;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isFile() {
        if (type.equals(TYPE_REMOTE_FILE)) {
            return true;
        }

        if (type.equals(TYPE_FILE) || type.equals(TYPE_STOREDFILE)
                || type.equals(TYPE_LOCAL_FILE)
                || type.equals(TYPE_REMOTE_FILE)) {
            return getTheFile().exists();
        }

        return false;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean fileExists() {
        if (type.equals(TYPE_FILE) || type.equals(TYPE_STOREDFILE)
                || type.equals(TYPE_LOCAL_FILE)
                || type.equals(TYPE_REMOTE_FILE)) {
            return getTheFile().exists();
        }

        return false;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isFileType() {
        if (type.equals(TYPE_FILE) || type.equals(TYPE_STOREDFILE)
                || type.equals(TYPE_LOCAL_FILE)) {
            return true;
        }

        return false;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isServerSideFile() {
        return type.equals(TYPE_LOCAL_FILE);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isStoredFile() {
        return type.equals(TYPE_STOREDFILE);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isUrl() {
        return type.equals(TYPE_URL);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isRemoteFile() {
        return type.equals(TYPE_REMOTE_FILE);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return path;
    }

    /**
     * Set the Path property.
     *
     * @param value The new value for Path
     */
    public void setPath(String value) {
        path = value;
        file = null;
    }

    /**
     * Get the Path property.
     *
     * @return The Path
     */
    public String getPath() {
        return path;
    }

    /**
     * Set the Type property.
     *
     * @param value The new value for Type
     */
    public void setType(String value) {
        type = value;
    }

    /**
     * Get the Type property.
     *
     * @return The Type
     */
    public String getType() {
        return type;
    }





    /**
     * Set the FileSize property.
     *
     * @param value The new value for FileSize
     */
    public void setFileSize(long value) {
        this.fileSize = value;
    }


    /**
     *  Set the Md5 property.
     *
     *  @param value The new value for Md5
     */
    public void setMd5(String value) {
        md5 = value;
    }

    /**
     *  Get the Md5 property.
     *
     *  @return The Md5
     */
    public String getMd5() {
        if (md5 == null) {
            //For now don't do this because big files take a long time
            //            File file = getTheFile();
            //            if(file.exists()) {
            //                md5 = IOUtil.getMd5(file.toString());
            //            }
        }

        return md5;
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        for (String arg : args) {
            long   t1  = System.currentTimeMillis();
            String md5 = ucar.unidata.util.IOUtil.getMd5(arg);
            long   t2  = System.currentTimeMillis();
            System.err.println("time:" + (t2 - t1));
        }
    }




}
