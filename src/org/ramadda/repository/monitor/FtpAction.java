/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.monitor;


import org.apache.commons.net.ftp.*;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.BufferedInputStream;


import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.30 $
 */
public class FtpAction extends MonitorAction {

    /** _more_ */
    public static final String PROP_FTP_SERVER = "ftp.server";

    /** _more_ */
    public static final String PROP_FTP_DIRECTORY = "ftp.directory";

    /** _more_ */
    public static final String PROP_FTP_FILETEMPLATE = "ftp.filetemplate";

    /** _more_ */
    public static final String PROP_FTP_USER = "ftp.user";

    /** _more_ */
    public static final String PROP_FTP_PASSWORD = "ftp.password";


    /** _more_ */
    private String server = "";

    /** _more_ */
    private String directory = "";

    /** _more_ */
    private String fileTemplate = "${filename}";

    /** _more_ */
    private String user = "";

    /** _more_ */
    private String password = "";


    /**
     * _more_
     */
    public FtpAction() {}


    /**
     * _more_
     *
     * @param id _more_
     */
    public FtpAction(String id) {
        super(id);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getActionName() {
        return "ftp";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getActionLabel() {
        return "FTP Action";
    }

    /**
     * _more_
     *
     *
     * @param entryMonitor _more_
     * @return _more_
     */
    public String getSummary(EntryMonitor entryMonitor) {
        return "Put file via FTP";
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param monitor _more_
     */
    public void applyEditForm(Request request, EntryMonitor monitor) {
        super.applyEditForm(request, monitor);
        this.server = request.getString(getArgId(PROP_FTP_SERVER), "");
        this.user   = request.getString(getArgId(PROP_FTP_USER), "");
        this.directory = request.getString(getArgId(PROP_FTP_DIRECTORY), "");
        this.fileTemplate =
            request.getString(getArgId(PROP_FTP_FILETEMPLATE), "");
        this.password = request.getString(getArgId(PROP_FTP_PASSWORD), "");
    }


    /**
     * _more_
     *
     * @param monitor _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void addToEditForm(Request request,EntryMonitor monitor, Appendable sb)
            throws Exception {
        sb.append(HtmlUtils.formTable());
        sb.append(HtmlUtils.colspan("FTP Action", 2));


        sb.append(
            HtmlUtils.formEntry(
                monitor.getRepository().msgLabel("FTP Server"),
                HtmlUtils.input(
                    getArgId(PROP_FTP_SERVER), server, HtmlUtils.SIZE_60)));
        sb.append(
            HtmlUtils.formEntry(
                monitor.getRepository().msgLabel("FTP Directory"),
                HtmlUtils.input(
                    getArgId(PROP_FTP_DIRECTORY), directory,
                    HtmlUtils.SIZE_60)));
        String tooltip =
            "macros: ${from_day}  ${from_month} ${from_year} ${from_monthname}  <br>"
            + "${to_day}  ${to_month} ${to_year} ${to_monthname} <br> "
            + "${filename}  ${fileextension} etc";

        sb.append(
            HtmlUtils.formEntry(
                monitor.getRepository().msgLabel("File Name Template"),
                HtmlUtils.input(
                    getArgId(PROP_FTP_FILETEMPLATE), fileTemplate,
                    HtmlUtils.SIZE_60 + HtmlUtils.title(tooltip))));
        sb.append(
            HtmlUtils.formEntry(
                monitor.getRepository().msgLabel("User ID"),
                HtmlUtils.input(
                    getArgId(PROP_FTP_USER), user, HtmlUtils.SIZE_60)));
        sb.append(
            HtmlUtils.formEntry(
                monitor.getRepository().msgLabel("Password"),
                HtmlUtils.password(
                    getArgId(PROP_FTP_PASSWORD), password,
                    HtmlUtils.SIZE_20)));

        sb.append(HtmlUtils.formTableClose());
    }


    /**
     * _more_
     *
     *
     * @param monitor _more_
     * @param entry _more_
     */
    protected void entryMatched(EntryMonitor monitor, Entry entry) {
        FTPClient ftpClient = new FTPClient();
        try {
            Resource resource = entry.getResource();
            if ( !resource.isFile()) {
                return;
            }
            if (server.length() == 0) {
                return;
            }

            String passwordToUse =
                monitor.getRepository().getPageHandler().processTemplate(
                    password, false);
            ftpClient.connect(server);
            if (user.length() > 0) {
                ftpClient.login(user, password);
            }
            int reply = ftpClient.getReplyCode();
            if ( !FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
                monitor.handleError("FTP server refused connection:"
                                    + server, null);

                return;
            }
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();
            if (directory.length() > 0) {
                ftpClient.changeWorkingDirectory(directory);
            }

            String filename =
                monitor.getRepository().getEntryManager().replaceMacros(
                    entry, fileTemplate);

            InputStream is =
                new BufferedInputStream(monitor.getRepository()
                    .getStorageManager()
                    .getFileInputStream(new File(resource.getPath())));
            boolean ok = ftpClient.storeUniqueFile(filename, is);
            is.close();
            if (ok) {
                monitor.logInfo(this, "Wrote file:" + directory + " " + filename);
            } else {
                monitor.handleError("Failed to write file:" + directory + " "
                                    + filename, null);
            }
        } catch (Exception exc) {
            monitor.handleError("Error posting to FTP:" + server, exc);
        } finally {
            try {
                ftpClient.logout();
            } catch (Exception exc) {}
            try {
                ftpClient.disconnect();
            } catch (Exception exc) {}
        }
    }



    /**
     * Set the Server property.
     *
     * @param value The new value for Server
     */
    public void setServer(String value) {
        server = value;
    }

    /**
     * Get the Server property.
     *
     * @return The Server
     */
    public String getServer() {
        return server;
    }

    /**
     * Set the Directory property.
     *
     * @param value The new value for Directory
     */
    public void setDirectory(String value) {
        directory = value;
    }

    /**
     * Get the Directory property.
     *
     * @return The Directory
     */
    public String getDirectory() {
        return directory;
    }

    /**
     * Set the User property.
     *
     * @param value The new value for User
     */
    public void setUser(String value) {
        user = value;
    }

    /**
     * Get the User property.
     *
     * @return The User
     */
    public String getUser() {
        return user;
    }


    /**
     * Set the Tmp property.
     *
     * @param value The new value for Tmp
     */
    public void setTmp(byte[] value) {
        if (value == null) {
            password = null;
        } else {
            password = new String(Utils.decodeBase64(new String(value)));
        }
    }

    /**
     * Get the Tmp property.
     *
     * @return The Tmp
     */
    public byte[] getTmp() {
        if (password == null) {
            return null;
        }

        return Utils.encodeBase64(password).getBytes();
    }



}
