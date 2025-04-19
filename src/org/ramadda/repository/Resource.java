/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;
import java.io.File;
import java.net.URL;

/**
 * Holds a file, url, etc and a type
 *
 * @author RAMADDA Development Team
 */
public class Resource {
    public static final String TYPE_FILE = "file";
    public static final String TYPE_S3 = "s3";
    public static final String TYPE_LOCAL_DIRECTORY = "localdirectory";
    public static final String TYPE_LOCAL_FILE = "localfile";
    public static final String TYPE_STOREDFILE = "storedfile";
    public static final String TYPE_URL = "url";
    public static final String TYPE_REMOTE_FILE = "remotefile";
    public static final String TYPE_UNKNOWN = "unknown";
    private String path;
    private String type = TYPE_UNKNOWN;
    private File file;
    private long fileSize;
    private String md5;

    public Resource() {
        path = "";
    }

    public Resource(File file, String type) {
        this.file = file;
        this.path = file.toString();
        this.type = type;
    }

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

    public Resource(URL url) {
        type = TYPE_URL;
        path = url.toString();
    }

    public Resource(String path, String type) {
        this(path, type, null, -1);
    }

    public Resource(String path, String type,long fileSize) {
	this(path,type,null,fileSize);
    }

    public Resource(String path, String type, String md5, long fileSize) {
        this.path     = path;
        this.type     = type;
        this.md5      = md5;
        this.fileSize = fileSize;
        if ((fileSize < 0) && (path != null)) {
            this.fileSize = new File(path).length();
        }
    }

    public Resource(Resource that) {
        this.path     = that.path;
        this.type     = that.type;
        this.md5      = that.md5;
        this.fileSize = that.fileSize;
    }

    public void sanitize() {
	path = HtmlUtils.strictSanitizeString(path);
    }

    public boolean isDefined() {
        return (path != null) && (path.length() > 0);
    }

    public boolean isImage() {
        return isImage(path);
    }

    public static boolean isImage(String path) {
        return Utils.isImage(path);
    }

    public boolean isEditableImage() {
        if (path == null) {
            return false;
        }
        String file = path.toLowerCase();

        return file.endsWith(".jpg") || file.endsWith(".jpeg")
               || file.endsWith(".gif") || file.endsWith(".png");
    }

    public File getTheFile() {
        if (file == null) {
            file = new File(path);
        }

        return file;
    }

    public long getFileSize() {
	return getFileSize(false);
    }

    public long getFileSize(boolean force) {	
        if (!force && fileSize > 0) {
            return fileSize;
        }
        File file = getTheFile();
        if (file.exists()) {
            fileSize = file.length();
        }

        return fileSize;
    }

    public long getFileSizeRaw() {
        return fileSize;
    }

    public boolean hasResource() {
        return isFile() || isUrl();
    }

    public boolean isS3() {
	return type.equals(TYPE_S3);
    }

    public boolean isFile() {
        if (type.equals(TYPE_REMOTE_FILE)) {
            return true;
        }

        if (type.equals(TYPE_FILE) || type.equals(TYPE_STOREDFILE)
                || type.equals(TYPE_LOCAL_FILE)
                || type.equals(TYPE_REMOTE_FILE)) {
            return getTheFile().exists();
        }

	if(isS3()) {
	    return true;
	}

        return false;
    }

    public boolean isUnknown() {
        return type.equals(TYPE_UNKNOWN);
    }

    public boolean fileExists() {
        if (type.equals(TYPE_FILE) || type.equals(TYPE_STOREDFILE)
                || type.equals(TYPE_LOCAL_FILE)
                || type.equals(TYPE_REMOTE_FILE)) {
            return getTheFile().exists();
        }

        return false;
    }

    public boolean isFileType() {
        if (isS3() || type.equals(TYPE_FILE) || type.equals(TYPE_STOREDFILE)
                || type.equals(TYPE_LOCAL_FILE)) {
            return true;
        }

        return false;
    }

    public boolean isServerSideFile() {
        return type.equals(TYPE_LOCAL_FILE);
    }

    public boolean isStoredFile() {
        return type.equals(TYPE_STOREDFILE);
    }

    public boolean isUrl() {
        return type.equals(TYPE_URL);
    }

    public boolean isRemoteFile() {
        return type.equals(TYPE_REMOTE_FILE);
    }

    public String toString() {
        return path;
    }

    public void setPath(String value) {
        path = value;
        file = null;
    }

    public void setFile(File file, String type) {
	fileSize= file.length();
        setPath(file.toString());
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public String getPathName() {
	return new File(path).getName();
    }

    public void setType(String value) {
        type = value;
    }

    public String getType() {
        return type;
    }

    public void setFileSize(long value) {
        this.fileSize = value;
    }

    public void setMd5(String value) {
        md5 = value;
    }

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

}
