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

package org.ramadda.repository.ftp;


import org.apache.ftpserver.*;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.listener.*;
import org.apache.ftpserver.usermanager.*;
import org.apache.ftpserver.usermanager.impl.*;
import org.apache.log4j.config.PropertyPrinter;


import org.ramadda.repository.Constants;
import org.ramadda.repository.Entry;
import org.ramadda.repository.EntryManager;



import org.ramadda.repository.Repository;
import org.ramadda.repository.RepositoryUtil;
import org.ramadda.repository.Request;
import org.ramadda.repository.Resource;
import org.ramadda.repository.Result;
import org.ramadda.repository.auth.Permission;
import org.ramadda.repository.auth.UserManager;
import org.ramadda.repository.type.TypeHandler;

import org.ramadda.util.Utils;

import ucar.unidata.util.IOUtil;


import ucar.unidata.util.StringUtil;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;


/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */

public class RepositoryFtplet extends DefaultFtplet {

    /** _more_ */
    public static final String CMD_ABOR = "ABOR";

    /** _more_ */
    public static final String CMD_CWD = "CWD";

    /** _more_ */
    public static final String CMD_DELE = "DELE";

    /** _more_ */
    public static final String CMD_LIST = "LIST";

    /** _more_ */
    public static final String CMD_MDTM = "MDTM";

    /** _more_ */
    public static final String CMD_MKD = "MKD";

    /** _more_ */
    public static final String CMD_NLST = "NLST";

    /** _more_ */
    public static final String CMD_PASS = "PASS";

    /** _more_ */
    public static final String CMD_PASV = "PASV";

    /** _more_ */
    public static final String CMD_PORT = "PORT";

    /** _more_ */
    public static final String CMD_PWD = "PWD";

    /** _more_ */
    public static final String CMD_QUIT = "QUIT";

    /** _more_ */
    public static final String CMD_RETR = "RETR";

    /** _more_ */
    public static final String CMD_RMD = "RMD";

    /** _more_ */
    public static final String CMD_RNFR = "RNFR";

    /** _more_ */
    public static final String CMD_RNTO = "RNTO";

    /** _more_ */
    public static final String CMD_SITE = "SITE";

    /** _more_ */
    public static final String CMD_SIZE = "SIZE";

    /** _more_ */
    public static final String CMD_STOR = "STOR";

    /** _more_ */
    public static final String CMD_TYPE = "TYPE";

    /** _more_ */
    public static final String CMD_USER = "USER";

    /** _more_ */
    public static final String CMD_ACCT = "ACCT";

    /** _more_ */
    public static final String CMD_APPE = "APPE";

    /** _more_ */
    public static final String CMD_CDUP = "CDUP";

    /** _more_ */
    public static final String CMD_HELP = "HELP";

    /** _more_ */
    public static final String CMD_MODE = "MODE";

    /** _more_ */
    public static final String CMD_NOOP = "NOOP";

    /** _more_ */
    public static final String CMD_REIN = "REIN";

    /** _more_ */
    public static final String CMD_STAT = "STAT";

    /** _more_ */
    public static final String CMD_STOU = "STOU";

    /** _more_ */
    public static final String CMD_STRU = "STRU";

    /** _more_ */
    public static final String CMD_SYST = "SYST";



    /** _more_ */
    public static final String PROP_ENTRYID = "ramadda.entryid";

    /** _more_ */
    public static final String NL = "\r\n";

    /** _more_ */
    FtpManager ftpManager;

    /** _more_ */
    private SimpleDateFormat sdf;

    /**
     * _more_
     *
     * @param ftpManager _more_
     */
    public RepositoryFtplet(FtpManager ftpManager) {
        this.ftpManager = ftpManager;
        sdf             = RepositoryUtil.makeDateFormat("MMM dd HH:mm");
    }

    /**
     * _more_
     *
     * @param ftpletContext _more_
     *
     * @throws FtpException _more_
     */
    public void init(FtpletContext ftpletContext) throws FtpException {
        super.init(ftpletContext);
    }

    /**
     * _more_
     *
     * @param session _more_
     * @param ftpRequest _more_
     * @param reply _more_
     *
     * @return _more_
     *
     * @throws FtpException _more_
     * @throws IOException _more_
     */
    public FtpletResult afterCommand(FtpSession session,
                                     FtpRequest ftpRequest, FtpReply reply)
            throws FtpException, IOException {
        return super.afterCommand(session, ftpRequest, reply);
    }

    /** _more_ */
    boolean didit = false;

    /**
     * _more_
     *
     * @param session _more_
     * @param ftpRequest _more_
     *
     * @return _more_
     *
     * @throws FtpException _more_
     * @throws IOException _more_
     */
    public FtpletResult beforeCommand(FtpSession session,
                                      FtpRequest ftpRequest)
            throws FtpException, IOException {
        try {
            if ( !didit) {
                //                didit = true;
                //                PrintWriter     pw = new PrintWriter(System.err);
                //                PropertyPrinter pp = new PropertyPrinter(pw);
                //                pp.print(pw);
            }
            ftpManager.logInfo("command:" + ftpRequest.getCommand() + " arg:"
                               + ftpRequest.getArgument());
            Request request = getRequest(session);
            Entry   group   = getGroup(request, session);
            if (group == null) {
                return handleError(session, ftpRequest, "No CWD");
            }
            String       cmd    = ftpRequest.getCommand();
            FtpletResult result = null;
            if ( !getRepository().getUserManager().isRequestOk(request)) {
                result = handleError(session, ftpRequest,
                                     "Cannot access repository");
            } else if (cmd.equals(CMD_LIST)) {
                result = handleList(request, group, session, ftpRequest);
            } else if (cmd.equals(CMD_NLST)) {
                result = handleList(request, group, session, ftpRequest);
            } else if (cmd.equals(CMD_MKD)) {
                result = handleMkd(request, group, session, ftpRequest);
            } else if (cmd.equals(CMD_STOR)) {
                result = handleStor(request, group, session, ftpRequest);
            } else if (cmd.equals(CMD_DELE)) {
                result = handleDele(request, group, session, ftpRequest);
            } else if (cmd.equals(CMD_RMD)) {
                result = handleRmd(request, group, session, ftpRequest);
            } else if (cmd.equals(CMD_SYST)) {
                result = handleSyst(request, group, session, ftpRequest);
            } else if (cmd.equals(CMD_PWD)) {
                result = handlePwd(request, group, session, ftpRequest);
            } else if (cmd.equals(CMD_CWD)) {
                result = handleCwd(request, group, session, ftpRequest);
            } else if (cmd.equals(CMD_SIZE)) {
                result = handleSize(request, group, session, ftpRequest);
            } else if (cmd.equals(CMD_RETR)) {
                result = handleRetr(request, group, session, ftpRequest);
            } else {
                ftpManager.logInfo("Not handling command: " + cmd);

                return super.beforeCommand(session, ftpRequest);
            }

            //            System.err.println("        done:" + ftpRequest.getCommand());
            return result;
            //          return FtpletResult.SKIP;
        } catch (java.net.ConnectException cexc) {
            //TODO: Maybe log once in a while?
            return FtpletResult.SKIP;
        } catch (Exception exc) {
            ftpManager.logError("Error handling ftp request:"
                                + ftpRequest.getCommand() + " arg:"
                                + ftpRequest.getArgument(), exc);

            return handleError(session, ftpRequest, "Error:" + exc);
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    private Repository getRepository() {
        return ftpManager.getRepository();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    private EntryManager getEntryManager() {
        return getRepository().getEntryManager();
    }

    /**
     * _more_
     *
     * @param session _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Request getRequest(FtpSession session) throws Exception {
        try {
            User   user = session.getUser();
            String name = ((user == null)
                           ? org.ramadda.repository.auth.UserManager
                               .USER_ANONYMOUS
                           : user.getName());

            return new Request(
                getRepository(),
                getRepository().getUserManager().findUser(name));
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     * _more_
     *
     *
     * @param request _more_
     * @param session _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Entry getGroup(Request request, FtpSession session)
            throws Exception {
        EntryManager entryManager = getEntryManager();
        String       id = (String) session.getAttribute(PROP_ENTRYID);
        if (id == null) {
            return entryManager.getRootEntry();
        }

        return (Entry) entryManager.getEntry(request, id);
    }


    /**
     * _more_
     *
     * @param session _more_
     * @param ftpRequest _more_
     * @param message _more_
     *
     * @return _more_
     *
     * @throws FtpException _more_
     * @throws IOException _more_
     */
    public FtpletResult handleError(FtpSession session,
                                    FtpRequest ftpRequest, String message)
            throws FtpException, IOException {

        return handleError(session, ftpRequest,
                           FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                           message);
    }

    /**
     * _more_
     *
     * @param session _more_
     * @param ftpRequest _more_
     * @param reply _more_
     * @param message _more_
     *
     * @return _more_
     *
     * @throws FtpException _more_
     * @throws IOException _more_
     */
    public FtpletResult handleError(FtpSession session,
                                    FtpRequest ftpRequest, int reply,
                                    String message)
            throws FtpException, IOException {
        //        System.err.println("error:" + message);
        session.write(new DefaultFtpReply(reply, message));

        return FtpletResult.SKIP;
    }





    /**
     * _more_
     *
     *
     * @param request _more_
     * @param group _more_
     * @param session _more_
     * @param ftpRequest _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public FtpletResult handlePwd(Request request, Entry group,
                                  FtpSession session, FtpRequest ftpRequest)
            throws Exception {
        StringBuffer result = new StringBuffer();
        if (group.isTopEntry()) {
            result.append("\"" + "/" + "\"");
        } else {
            String fullName = group.getFullName();
            int    idx      = fullName.indexOf("/");
            if (idx >= 0) {
                fullName = fullName.substring(idx);
            }
            result.append("\"" + fullName + "\"");
        }
        session.write(
            new DefaultFtpReply(
                FtpReply.REPLY_257_PATHNAME_CREATED, result.toString()));

        return FtpletResult.SKIP;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param session _more_
     * @param ftpRequest _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public FtpletResult handleSyst(Request request, Entry group,
                                   FtpSession session, FtpRequest ftpRequest)
            throws Exception {
        session.write(
            new DefaultFtpReply(
                FtpReply.REPLY_215_NAME_SYSTEM_TYPE, "UNIX Type: L8"));

        return FtpletResult.SKIP;
    }

    /**
     * _more_
     *
     *
     * @param request _more_
     * @param group _more_
     * @param session _more_
     * @param ftpRequest _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public FtpletResult handleList(Request request, Entry group,
                                   FtpSession session, FtpRequest ftpRequest)
            throws Exception {
        //dr-x------   3 user group            0 Oct 20 14:27 Desktop
        List<Entry> children = null;
        String      arg      = ftpRequest.getArgument();
        if (Utils.stringDefined(arg)) {
            children = findEntries(request, group, arg);
        }


        EntryManager entryManager = getEntryManager();

        if (children == null) {
            children = entryManager.getChildren(request, group);
        }

        StringBuffer result = new StringBuffer();
        for (Entry e : children) {
            String prefix;
            String name;
            String size;
            String w = "-";
            String permissions;
            if (e.isFile()) {
                size        = "" + e.getResource().getFileSize();
                name = getRepository().getStorageManager().getFileTail(e);
                permissions = "-rw-r--r-- 1";
            } else if (e.isGroup()) {
                name        = e.getName();
                prefix      = "d";
                size        = "0";
                permissions = "drwxr-xr-x 1";
            } else {
                continue;
            }

            result.append(permissions);
            result.append(" ");
            result.append(e.getUser().getId());
            result.append(" ");
            result.append(" ramadda ");
            result.append(" ");
            result.append(StringUtil.padLeft(size, 13, " "));
            result.append(" ");
            result.append(sdf.format(new Date(e.getCreateDate())));
            result.append(" ");
            result.append(name);
            result.append(NL);
        }
        session.write(
            new DefaultFtpReply(
                FtpReply.REPLY_150_FILE_STATUS_OKAY,
                " Here comes the directory listing.."));

        session.getDataConnection().openConnection().transferToClient(
            session, result.toString());
        session.write(
            new DefaultFtpReply(
                FtpReply.REPLY_226_CLOSING_DATA_CONNECTION,
                "Directory send OK."));

        session.getDataConnection().closeDataConnection();


        return FtpletResult.SKIP;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param session _more_
     * @param ftpRequest _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public FtpletResult handleMkd(Request request, Entry group,
                                  FtpSession session, FtpRequest ftpRequest)
            throws Exception {
        if ( !getRepository().getAccessManager().canDoAction(request, group,
                Permission.ACTION_NEW)) {
            return handleError(
                session, ftpRequest,
                FtpReply.REPLY_450_REQUESTED_FILE_ACTION_NOT_TAKEN,
                "You do not have the access to create a directory");
        }


        EntryManager entryManager = getEntryManager();

        entryManager.makeNewGroup(group, ftpRequest.getArgument(),
                                  request.getUser());
        session.write(
            new DefaultFtpReply(
                FtpReply.REPLY_257_PATHNAME_CREATED, "Directory created"));


        return FtpletResult.SKIP;
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param session _more_
     * @param ftpRequest _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public FtpletResult handleStor(Request request, Entry group,
                                   FtpSession session, FtpRequest ftpRequest)
            throws Exception {
        if ( !getRepository().getAccessManager().canDoAction(request, group,
                Permission.ACTION_NEW)) {
            return handleError(
                session, ftpRequest,
                FtpReply.REPLY_450_REQUESTED_FILE_ACTION_NOT_TAKEN,
                "You do not have the access to store the file");
        }


        String name = ftpRequest.getArgument();
        if (name.trim().length() == 0) {
            return handleError(session, ftpRequest, "Bad file name");
        }
        File newFile =
            getRepository().getStorageManager().getTmpFile(request, name);
        OutputStream fos =
            getRepository().getStorageManager().getFileOutputStream(newFile);
        try {
            System.err.println("transferring from client");
            session.getDataConnection().openConnection().transferFromClient(
                session, fos);
            System.err.println("done");
        } finally {
            IOUtil.close(fos);
        }

        newFile = getRepository().getStorageManager().moveToStorage(request,
                newFile);

        System.err.println("making entry");
        EntryManager entryManager = getEntryManager();
        Entry entry = entryManager.addFileEntry(request, newFile, group,
                          name, request.getUser());


        System.err.println("closing session");
        session.write(
            new DefaultFtpReply(
                FtpReply.REPLY_257_PATHNAME_CREATED, "File created"));


        return FtpletResult.SKIP;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param session _more_
     * @param ftpRequest _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public FtpletResult handleDele(Request request, Entry group,
                                   FtpSession session, FtpRequest ftpRequest)
            throws Exception {
        Entry entry = findEntry(request, group, ftpRequest.getArgument());
        if (entry == null) {
            return handleError(session, ftpRequest,
                               "Not a valid file:"
                               + ftpRequest.getArgument());
        }

        if (entry.isGroup()) {
            return handleError(session, ftpRequest, "Not a file");
        }

        if ( !getRepository().getAccessManager().canDoAction(request, entry,
                Permission.ACTION_DELETE)) {
            return handleError(
                session, ftpRequest,
                FtpReply.REPLY_450_REQUESTED_FILE_ACTION_NOT_TAKEN,
                "You do not have the access to delete the entry");
        }

        //TODO: Do we really want to support this?
        if (true) {
            return handleError(session, ftpRequest,
                               FtpReply.REPLY_202_COMMAND_NOT_IMPLEMENTED,
                               "Not implemented");
        }

        getEntryManager().makeNewGroup(group, ftpRequest.getArgument(),
                                       request.getUser());
        session.write(
            new DefaultFtpReply(
                FtpReply.REPLY_257_PATHNAME_CREATED, "Directory created"));


        return FtpletResult.SKIP;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param session _more_
     * @param ftpRequest _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public FtpletResult handleRmd(Request request, Entry group,
                                  FtpSession session, FtpRequest ftpRequest)
            throws Exception {
        Entry entry = findEntry(request, group, ftpRequest.getArgument());
        if (entry == null) {
            return handleError(session, ftpRequest,
                               "Not a valid file:"
                               + ftpRequest.getArgument());
        }

        if ( !entry.isGroup()) {
            return handleError(session, ftpRequest, "Not a directory");
        }

        if ( !getRepository().getAccessManager().canDoAction(request, entry,
                Permission.ACTION_DELETE)) {
            return handleError(
                session, ftpRequest,
                FtpReply.REPLY_450_REQUESTED_FILE_ACTION_NOT_TAKEN,
                "You do not have the access to delete the entry");
        }

        //TODO: Do we really want to support this?
        if (true) {
            return handleError(session, ftpRequest,
                               FtpReply.REPLY_202_COMMAND_NOT_IMPLEMENTED,
                               "Not implemented");
        }

        getEntryManager().makeNewGroup(group, ftpRequest.getArgument(),
                                       request.getUser());
        session.write(
            new DefaultFtpReply(
                FtpReply.REPLY_257_PATHNAME_CREATED, "Directory created"));


        return FtpletResult.SKIP;
    }



    /**
     * _more_
     *
     * @param group _more_
     * @param session _more_
     * @param request _more_
     * @param ftpRequest _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public FtpletResult handleRetr(Request request, Entry group,
                                   FtpSession session, FtpRequest ftpRequest)
            throws Exception {

        String entryName  = ftpRequest.getArgument();
        String outputType = null;
        String delimiter  = ".ramadda.output.";
        int    idx        = entryName.indexOf(delimiter);

        if (idx > 0) {
            outputType = entryName.substring(idx + delimiter.length());
            entryName  = entryName.substring(0, idx);
        }
        //      System.err.println ("outputType:" + outputType +" name:" + entryName);

        Entry entry = findEntry(request, group, entryName);
        if (entry == null) {
            return handleError(session, ftpRequest,
                               "Not a valid file:"
                               + ftpRequest.getArgument());
        }



        if ( !getRepository().getAccessManager().canAccessFile(request,
                entry)) {
            System.err.println("permission problem:" + request.getUser()
                               + " " + entry.getName());

            return handleError(session, ftpRequest,
                               "You don't have permission to get the file");
        }

        InputStream inputStream = null;

        try {
            if (outputType != null) {
                request.put(Constants.ARG_OUTPUT, outputType);
                Result result = getEntryManager().processEntryShow(request,
                                    entry);
                byte[] contents = result.getContent();
                inputStream = new ByteArrayInputStream(contents);
            } else {
                if ( !entry.isFile()) {
                    return handleError(session, ftpRequest,
                                       "Not a file: " + entryName);
                }
                File file = entry.getFile();
                inputStream =
                    getRepository().getStorageManager().getFileInputStream(
                        file);
            }

            session.write(
                new DefaultFtpReply(
                    FtpReply.REPLY_150_FILE_STATUS_OKAY,
                    "Opening binary mode connect."));
            session.getDataConnection().openConnection().transferToClient(
                session, inputStream);
            session.write(
                new DefaultFtpReply(
                    FtpReply.REPLY_226_CLOSING_DATA_CONNECTION,
                    "Closing data connection."));

            session.getDataConnection().closeDataConnection();
        } finally {
            IOUtil.close(inputStream);
        }

        return FtpletResult.SKIP;
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param session _more_
     * @param ftpRequest _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public FtpletResult handleSize(Request request, Entry group,
                                   FtpSession session, FtpRequest ftpRequest)
            throws Exception {

        String entryName = ftpRequest.getArgument();
        Entry  entry     = findEntry(request, group, entryName);
        ftpManager.logInfo("Folder:" + group.getName() + " name:"
                           + entryName);
        if (entry == null) {
            return handleError(session, ftpRequest,
                               "Not a valid file:"
                               + ftpRequest.getArgument());
        }



        if ( !getRepository().getAccessManager().canAccessFile(request,
                entry)) {
            System.err.println("permission problem:" + request.getUser()
                               + " " + entry.getName());

            return handleError(session, ftpRequest,
                               "You don't have permission to get the file");
        }

        if ( !entry.isFile()) {
            return handleError(session, ftpRequest,
                               "Not a file: " + entryName);
        }

        session.write(
            new DefaultFtpReply(
                FtpReply.REPLY_250_REQUESTED_FILE_ACTION_OKAY,
                "" + entry.getFile().length()));

        return FtpletResult.SKIP;

    }




    /**
     * _more_
     *
     * @param request _more_
     * @param parent _more_
     * @param name _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Entry findEntry(Request request, Entry parent, String name)
            throws Exception {
        List<Entry> result = findEntries(request, parent, name);
        if (result.size() > 0) {
            return result.get(0);
        }

        return null;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param parent _more_
     * @param name _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private List<Entry> findEntries(Request request, Entry parent,
                                    String name)
            throws Exception {
        EntryManager entryManager = getEntryManager();
        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
        }

        List<Entry> result = new ArrayList<Entry>();
        if (name.length() == 0) {
            result.add(entryManager.getRootEntry());

            return result;
        }
        if (name.startsWith("/")) {
            name = name.substring(1);
            if (name.length() == 0) {
                result.add(entryManager.getRootEntry());

                return result;
            }
            //            getRepository().getLogManager().logInfo("ftp: calling findEntryFrom name");

            result = entryManager.findDescendants(request,
                    entryManager.getRootEntry(), name);

            return result;
        } else {
            while (name.startsWith("..")) {
                if (parent.getParentEntry() == null) {
                    break;
                }
                parent = parent.getParentEntry();
                name   = name.substring(2);
                if (name.startsWith("/")) {
                    name = name.substring(1);
                }
            }
            if (name.equals("")) {
                result.add(parent);

                return result;
            }

            return entryManager.findDescendants(request, parent, name);
        }
    }


    /**
     * _more_
     *
     * @param group _more_
     * @param session _more_
     * @param request _more_
     * @param ftpRequest _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public FtpletResult handleCwd(Request request, Entry group,
                                  FtpSession session, FtpRequest ftpRequest)
            throws Exception {

        StringBuffer result       = new StringBuffer();

        String       subGroupName = ftpRequest.getArgument().trim();
        Entry        entry        = findEntry(request, group, subGroupName);
        if (entry == null) {
            return handleError(session, ftpRequest,
                               "Not a valid directory:" + subGroupName);
        }
        if ( !entry.isGroup()) {
            return handleError(session, ftpRequest,
                               "Not a valid directory:" + subGroupName);
        }
        Entry subGroup = (Entry) entry;

        session.setAttribute(PROP_ENTRYID, subGroup.getId());
        result.append("Directory successfully changed");
        session.write(
            new DefaultFtpReply(
                FtpReply.REPLY_250_REQUESTED_FILE_ACTION_OKAY,
                result.toString()));

        return FtpletResult.SKIP;
    }



    /**
     * _more_
     *
     * @param session _more_
     * @param ftpRequest _more_
     *
     * @return _more_
     *
     * @throws FtpException _more_
     * @throws IOException _more_
     */
    public FtpletResult onLogin(FtpSession session, FtpRequest ftpRequest)
            throws FtpException, IOException {
        return FtpletResult.DEFAULT;
    }

}
