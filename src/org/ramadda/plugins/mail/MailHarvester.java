/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.mail;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.harvester.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.MailUtil;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.net.*;



import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.*;



/**
 */
@SuppressWarnings("unchecked")
public class MailHarvester extends Harvester {

    /** _more_ */
    public static final String ACTION_ATTACHMENTS = "attachments";

    /** _more_ */
    public static final String ACTION_EML = "eml";

    /** _more_ */
    public static final String MODE_UNREAD = "unread";

    /** _more_ */
    public static final String MODE_DELETE = "delete";


    /** _more_ */
    public static final String ATTR_IMAP_URL = "imapurl";

    /** _more_ */
    public static final String ATTR_FOLDER = "folder";

    /** _more_ */
    public static final String ATTR_FROM = "from";

    /** _more_ */
    public static final String ATTR_SUBJECT = "subject";

    /** _more_ */
    public static final String ATTR_BODY = "body";

    /** _more_ */
    public static final String ATTR_ACTION = "action";

    /** _more_ */
    public static final String ATTR_DELETEEMAIL = "delete_email";


    /** _more_ */
    public static final String ATTR_MODE = "mode";

    /** _more_ */
    public static final String ATTR_RESPONSE = "response";


    /** _more_ */
    private String imapUrl;

    /** _more_ */
    private String folder = "Inbox";

    /** _more_ */
    private String from;

    /** _more_ */
    private String subject;

    /** _more_ */
    private String body;

    /** _more_ */
    private String response;



    /** _more_ */
    private String action = ACTION_ATTACHMENTS;

    /** _more_ */
    private boolean delete = false;




    /**
     * _more_
     *
     * @param repository _more_
     * @param id _more_
     *
     * @throws Exception _more_
     */
    public MailHarvester(Repository repository, String id) throws Exception {
        super(repository, id);
    }


    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     *
     * @throws Exception _more_
     */
    public MailHarvester(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }



    /**
     * _more_
     *
     * @param element _more_
     *
     * @throws Exception _more_
     */
    protected void init(Element element) throws Exception {
        super.init(element);
        imapUrl = XmlUtil.getAttribute(element, ATTR_IMAP_URL, imapUrl);
        folder  = XmlUtil.getAttribute(element, ATTR_FOLDER, folder);
        from    = XmlUtil.getAttribute(element, ATTR_FROM, from);
        action  = XmlUtil.getAttribute(element, ATTR_ACTION, action);
        delete = XmlUtil.getAttribute(element, ATTR_DELETEEMAIL,
                                      "false").equals("true");
        subject  = XmlUtil.getAttribute(element, ATTR_SUBJECT, subject);
        body     = XmlUtil.getAttribute(element, ATTR_BODY, body);
        response = XmlUtil.getAttribute(element, ATTR_RESPONSE, response);
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public String getDescription() {
        return "Email Harvester";
    }



    /**
     * _more_
     *
     * @param element _more_
     *
     * @throws Exception _more_
     */
    public void applyState(Element element) throws Exception {
        super.applyState(element);
        element.setAttribute(ATTR_IMAP_URL, imapUrl);
        element.setAttribute(ATTR_FOLDER, folder);
        element.setAttribute(ATTR_FROM, from);
        element.setAttribute(ATTR_ACTION, action);
        element.setAttribute(ATTR_DELETEEMAIL, "" + delete);
        element.setAttribute(ATTR_SUBJECT, subject);
        element.setAttribute(ATTR_BODY, body);
        element.setAttribute(ATTR_RESPONSE, response);
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    public void applyEditForm(Request request) throws Exception {
        super.applyEditForm(request);
        imapUrl = request.getString(ATTR_IMAP_URL, imapUrl);
        folder  = request.getString(ATTR_FOLDER, folder);
        from    = request.getString(ATTR_FROM, from);
        action  = request.getString(ATTR_ACTION, action);
        delete  = request.get(ATTR_DELETEEMAIL, false);
        subject = request.getString(ATTR_SUBJECT, subject);
        body    = request.getString(ATTR_BODY, body);
        if (getMailManager().isEmailEnabled()) {
            response = request.getString(ATTR_RESPONSE, response);
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void createEditForm(Request request, StringBuffer sb)
            throws Exception {
        super.createEditForm(request, sb);



        sb.append(HtmlUtils.formEntry("", ""));
        sb.append(HtmlUtils.formEntry("", "Email Information:"));
        sb.append(
            HtmlUtils.formEntry(
                msgLabel("Email URL"),
                HtmlUtils.input(ATTR_IMAP_URL, imapUrl, HtmlUtils.SIZE_60)
                + " "
                + "e.g. <i>imaps://&lt;email address&gt;:&lt;email password&gt;@&lt;email server&gt;</i>"));


        sb.append(HtmlUtils.formEntry(msgLabel("Folder"),
                                      HtmlUtils.input(ATTR_FOLDER, folder,
                                          HtmlUtils.SIZE_60)));

        sb.append(HtmlUtils.formEntry("&nbsp;", "&nbsp;"));
        sb.append(HtmlUtils.formEntry("", "Search Criteria:"));

        sb.append(HtmlUtils.formEntry(msgLabel("Sender Contains"),
                                      HtmlUtils.input(ATTR_FROM, from,
                                          HtmlUtils.SIZE_60)));
        sb.append(HtmlUtils.formEntry(msgLabel("Subject Contains"),
                                      HtmlUtils.input(ATTR_SUBJECT, subject,
                                          HtmlUtils.SIZE_60)));
        sb.append(HtmlUtils.formEntry(msgLabel("Body Contains"),
                                      HtmlUtils.input(ATTR_BODY, body,
                                          HtmlUtils.SIZE_60)));

        sb.append(HtmlUtils.formEntry("&nbsp;", "&nbsp;"));
        sb.append(HtmlUtils.formEntry("", "Process email:"));
        addBaseGroupSelect(ATTR_BASEGROUP, sb);
        List<TwoFacedObject> actions = new ArrayList<TwoFacedObject>();
        actions.add(new TwoFacedObject("Make entries from attachments",
                                       ACTION_ATTACHMENTS));
        actions.add(new TwoFacedObject("Import EML file", ACTION_EML));
        sb.append(HtmlUtils.formEntry(msgLabel("Action"),
                                      HtmlUtils.select(ATTR_ACTION, actions,
                                          action)));

        sb.append(
            HtmlUtils.formEntry(
                msgLabel(""),
                HtmlUtils.checkbox(ATTR_DELETEEMAIL, "true", delete) + " "
                + msg("Delete email after harvesting")));

        if (getMailManager().isEmailEnabled()) {
            sb.append(
                HtmlUtils.formEntryTop(
                    msgLabel("Email Response"),
                    HtmlUtils.textArea(ATTR_RESPONSE, (response == null)
                    ? ""
                    : response, 5, 60) + "<br>"
                    + "Use ${url} for the URL to the created entry"));
        }

    }



    /**
     * _more_
     *
     *
     * @param timestamp _more_
     * @throws Exception _more_
     */
    protected void runInner(int timestamp) throws Exception {
        int errorCount = 0;

        while (canContinueRunning(timestamp)) {
            long t1 = System.currentTimeMillis();
            logHarvesterInfo("Checking email");
            try {
                status = new StringBuffer();
                checkEmail();
                errorCount = 0;
            } catch (Exception exc) {
                logHarvesterError("Error in checkEmail", exc);
                if (errorCount++ > 10) {
                    logHarvesterError(
                        "Too many errors in checkEmail. Exiting.", null);

                    return;
                }
            }
            currentStatus = "Done";
            if ( !getMonitor()) {
                logHarvesterInfo("Ran one time only. Exiting loop");

                break;
            }
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(new Date());
            cal.add(cal.MINUTE, (int) getSleepMinutes());

            String msg = "Sleeping for " + getSleepMinutes()
                         + " minutes. Will run again at:" + cal.getTime();
            logHarvesterInfo(msg);
            status.append(msg + "<b>");
            doPause();
        }
    }



    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void checkEmail() throws Exception {

        currentStatus = "Checking mail";
        Properties props   = System.getProperties();
        Session    session = Session.getDefaultInstance(props);
        URLName    urlName = new URLName(imapUrl);
        Store      store   = session.getStore(urlName);
        if ( !store.isConnected()) {
            store.connect();
        }

        if ( !store.isConnected()) {
            logHarvesterError("Could not connect to email server", null);

            return;
        }

        Folder emailFolder = store.getFolder(folder);
        if ((emailFolder == null) || !emailFolder.exists()) {
            status.append("Invalid folder:" + folder);
            logHarvesterError("Invalid folder:" + folder, null);

            return;
        }
        emailFolder.open(Folder.READ_WRITE);

        int numMessages = emailFolder.getMessageCount();
        int cnt         = 0;
        int numSeen     = 0;
        int numPassed   = 0;
        //Go backwards to get the newest first
        status.append(numMessages + " messages in folder:" + folder + "<br>");
        for (int i = numMessages; i > 0; i--) {
            //Only read 100 messages
            if (cnt++ > 100) {
                break;
            }
            Message message = emailFolder.getMessage(i);

            if (message.isSet(Flags.Flag.SEEN)
                    || message.isSet(Flags.Flag.DELETED)) {
                numSeen++;

                //logHarvesterInfo ("message has been read:"  + message.getSubject());
                continue;
            }

            String messageSubject = message.getSubject();
            String messageFrom = InternetAddress.toString(message.getFrom());

            if ( !matches(subject, messageSubject)) {
                continue;
            }
            if ( !matches(from, messageFrom)) {
                continue;
            }
            Object       content = message.getContent();
            StringBuffer sb      = new StringBuffer();
            MailUtil.extractText(content, sb);
            String messageBody = sb.toString().trim();
            if ( !matches(body, messageBody)) {
                continue;
            }
            messageBody = cleanUpText(messageBody);
            //            System.err.println("*** Check Mail " + messageBody.length());
            //            System.err.println("Message:" + messageBody);
            numPassed++;
            List<Entry>     newEntries = new ArrayList<Entry>();
            List<EntryInfo> entryInfos = new ArrayList<EntryInfo>();
            if (action.equals(ACTION_EML)) {
                processEml(getBaseGroup(), message, newEntries, entryInfos);
            } else {
                processMessage(getBaseGroup(), message, content, messageBody,
                               newEntries, entryInfos);
            }


            if (newEntries.size() == 0) {
                //See if the first line is a URL
                URL url = null;
                try {
                    int index = messageBody.indexOf("\n");
                    if (index < 0) {
                        url = new URL(messageBody);
                    } else {
                        String firstLine = messageBody.substring(0,
                                               index).trim();
                        url         = new URL(firstLine);
                        messageBody = messageBody.substring(index);
                    }
                } catch (Exception ignore) {}

                Resource    resource    = null;
                String      type        = "notes_note";
                TypeHandler typeHandler = null;
                String      name        = message.getSubject();
                if (url != null) {
                    type = "link";
                    if (name == null) {
                        name = url.toString();
                    }
                    resource = new Resource(url);
                }
                if (name == null) {
                    name = "entry";
                }
                EntryInfo entryInfo = getEntryInfo(getRequest(),
                                          getBaseGroup(), name, messageBody);
                entryInfos.add(entryInfo);
                typeHandler = getRepository().getTypeHandler(type);
                Date now = new Date();
                Entry entry =
                    typeHandler.createEntry(getRepository().getGUID());
                entry.initEntry(entryInfo.name, entryInfo.text.toString(),
                                entryInfo.parentEntry, getUser(), resource,
                                "", Entry.DEFAULT_ORDER, now.getTime(),
                                now.getTime(), now.getTime(), now.getTime(),
                                null);

                entryInfo.newEntry = entry;
                newEntries.add(entry);
                Request request = getRequest();
                request.put(ARG_METADATA_ADD, "true");
                getEntryManager().addNewEntries(request, newEntries);
            }

            if (entryInfos.size() > 0) {
                StringBuffer result = new StringBuffer();
                if (Utils.stringDefined(response)) {
                    result.append(response);
                    result.append("\n");
                }
                for (EntryInfo entryInfo : entryInfos) {
                    Entry newEntry = entryInfo.newEntry;
                    String fullEntryUrl =
                        HtmlUtils.url(
                            getEntryManager().getFullEntryShowUrl(
                                getRequest()), ARG_ENTRYID, newEntry.getId());

                    String entryUrl =
                        HtmlUtils.url(
                            getRepository().URL_ENTRY_SHOW.toString(),
                            ARG_ENTRYID, newEntry.getId());

                    result.append(fullEntryUrl);
                    result.append("\n");

                    status.append("New entry: ");
                    status.append(HtmlUtils.href(entryUrl,
                            newEntry.getName()));
                    if (delete) {
                        status.append("  -- message has been deleted");
                        logHarvesterInfo("message has been deleted:"
                                         + message.getSubject());
                        message.setFlag(Flags.Flag.DELETED, true);
                    }
                    status.append(HtmlUtils.br());
                }

                if (getMailManager().isEmailEnabled()) {
                    if (Utils.stringDefined(response)) {
                        String to =
                            InternetAddress.toString(message.getFrom());
                        getRepository().getMailManager().sendEmail(to,
                                "RAMADDA harvested emails",
                                result.toString(), false);
                    }
                    HashSet seen = new HashSet();
                    for (EntryInfo info : entryInfos) {
                        for (String forward : info.forwards) {
                            if (seen.contains(forward)) {
                                continue;
                            }
                            status.append(HtmlUtils.br());
                            seen.add(forward);
                            getRepository().getMailManager().sendEmail(
                                forward, "RAMADDA email", result.toString(),
                                false);

                        }
                    }
                }
            }

        }
        int numNotPassed = cnt - numSeen - numPassed;

        if (numSeen > 0) {
            status.append(plural(numSeen, "1 message has already been seen",
                                 "messages have already been seen"));
            status.append("<br>");
        }
        if (numNotPassed > 0) {
            status.append(plural(numNotPassed,
                                 "1 message did not meet the criteria",
                                 "messages did not meet the criteria"));
            status.append("<br>");
        }
        if (numPassed > 0) {
            status.append(plural(numPassed,
                                 "1 message has been processed" + (delete
                    ? " and deleted"
                    : ""), "messages have been processed" + (delete
                    ? " and deleted"
                    : "")));
            status.append("<br>");
        }


    }


    /**
     * _more_
     *
     * @param cnt _more_
     * @param phrase1 _more_
     * @param phrase2 _more_
     *
     * @return _more_
     */
    private String plural(int cnt, String phrase1, String phrase2) {
        if (cnt == 1) {
            return phrase1;
        }

        return cnt + " " + phrase2;
    }

    /**
     * _more_
     *
     * @param parentEntry _more_
     * @param message _more_
     * @param content _more_
     * @param desc _more_
     * @param newEntries _more_
     * @param entryInfos _more_
     *
     * @throws Exception _more_
     */
    private void processMessage(Entry parentEntry, Message message,
                                Object content, String desc,
                                List<Entry> newEntries,
                                List<EntryInfo> entryInfos)
            throws Exception {

        Request request = getRequest();
        if (content instanceof MimeMultipart) {
            MimeMultipart multipart = (MimeMultipart) content;
            for (int i = 0; i < multipart.getCount(); i++) {
                MimeBodyPart part = (MimeBodyPart) multipart.getBodyPart(i);
                String       disposition = part.getDisposition();
                if (disposition == null) {
                    Object partContent = part.getContent();
                    if (partContent instanceof MimeMultipart) {
                        processMessage(parentEntry, message, partContent,
                                       desc, newEntries, entryInfos);
                    } else {}

                    continue;
                }
                if (disposition.equalsIgnoreCase(Part.ATTACHMENT)
                        || disposition.equalsIgnoreCase(Part.INLINE)) {
                    if (part.getFileName() != null) {
                        File f = getStorageManager().moveToStorage(request,
                                     part.getInputStream(),
                                     part.getFileName());
                        TypeHandler typeHandler =
                            getEntryManager().findDefaultTypeHandler(request,
                                f.toString());
                        if (typeHandler == null) {
                            typeHandler = getRepository().getTypeHandler(
                                TypeHandler.TYPE_FILE);
                        }
                        Resource resource = new Resource(f.toString(),
                                                Resource.TYPE_STOREDFILE);
                        Date now  = new Date();
                        Date date = message.getReceivedDate();
                        Object[] values =
                            typeHandler.makeEntryValues(new Hashtable());
                        Entry entry = typeHandler.createEntry(
                                          getRepository().getGUID());

                        String name;
                        if (message.getSubject() != null) {
                            name = message.getSubject();
                        } else {
                            name = part.getFileName();
                        }
                        EntryInfo entryInfo = getEntryInfo(request,
                                                  parentEntry, name,
                                                  desc.toString());
                        entry.initEntry(entryInfo.name,
                                        entryInfo.text.toString(),
                                        entryInfo.parentEntry, getUser(),
                                        resource, "", Entry.DEFAULT_ORDER,
                                        now.getTime(), now.getTime(),
                                        date.getTime(), date.getTime(),
                                        values);

                        List<Entry> entries =
                            (List<Entry>) Misc.newList(entry);
                        getEntryManager().addInitialMetadata(request,
                                entries, true, false);
                        getEntryManager().addNewEntries(getRequest(),
                                entries);
                        newEntries.add(entry);
                        entryInfo.newEntry = entry;
                        entryInfos.add(entryInfo);
                    }
                }
            }
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param parentEntry _more_
     * @param name _more_
     * @param desc _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private EntryInfo getEntryInfo(Request request, Entry parentEntry,
                                   String name, String desc)
            throws Exception {
        EntryInfo entryInfo      = new EntryInfo(parentEntry, name);
        Entry     theParentEntry = parentEntry;
        for (String line : StringUtil.split(desc.toString(), "\n")) {
            String lline = line.toLowerCase();
            //            System.err.println("email line:" + line);
            if (lline.startsWith("name:")) {
                entryInfo.name = line.substring("name:".length()).trim();
            } else if (lline.startsWith("fwd:")) {
                entryInfo.forwards.addAll(
                    StringUtil.split(
                        line.substring("fwd:".length()).trim(), ",", true,
                        true));
            } else if (lline.startsWith("forward:")) {
                entryInfo.forwards.addAll(
                    StringUtil.split(
                        line.substring("forward:".length()).trim(), ",",
                        true, true));
            } else if (lline.startsWith("to:") || lline.startsWith("at:")) {
                Entry        theFolder = theParentEntry;
                String       tmp       = line.substring(line.indexOf(":")
                                             + 1);
                List<String> toks = StringUtil.split(tmp, "/", true, true);
                for (String tok : toks) {
                    Entry folder =
                        getEntryManager().findEntryWithName(request,
                            theFolder, tok);
                    if (folder == null) {
                        System.err.println("could not find folder: " + line);

                        break;
                    }
                    if ( !folder.isGroup()) {
                        System.err.println("specified entry isn't a group: "
                                           + line);

                        break;
                    }
                    theFolder = folder;
                }
                theParentEntry = theFolder;
            } else if (lline.startsWith("tag:")) {
                String tag = line.substring("tag:".length()).trim();
            } else {
                entryInfo.text.append(line);
                entryInfo.text.append("<br>\n");
            }
        }
        entryInfo.parentEntry = theParentEntry;

        return entryInfo;
    }


    /**
     * _more_
     *
     * @param parentEntry _more_
     * @param message _more_
     * @param entries _more_
     * @param infos _more_
     *
     * @throws Exception _more_
     */
    private void processEml(Entry parentEntry, Message message,
                            List<Entry> entries, List<EntryInfo> infos)
            throws Exception {
        String       name         = message.getSubject();
        File f = getStorageManager().getTmpFile(getRequest(), name + ".eml");
        OutputStream outputStream =
            getStorageManager().getFileOutputStream(f);
        message.writeTo(outputStream);
        IOUtil.close(outputStream);
        f = getStorageManager().moveToStorage(getRequest(), f);
        TypeHandler typeHandler =
            getRepository().getTypeHandler(MailTypeHandler.TYPE_MESSAGE);
        Resource resource = new Resource(f.toString(),
                                         Resource.TYPE_STOREDFILE);
        Date     date   = new Date();
        Entry    entry  = typeHandler.createEntry(getRepository().getGUID());
        Object[] values = typeHandler.makeEntryValues(new Hashtable());
        entry.initEntry(name, "", parentEntry, getUser(), resource, "",
                        Entry.DEFAULT_ORDER, date.getTime(), date.getTime(),
                        date.getTime(), date.getTime(), values);
        typeHandler.initializeEntryFromForm(getRequest(), entry, parentEntry,
                                            true);
        entries.add(entry);
        infos.add(new EntryInfo(entry));
        getEntryManager().addNewEntries(getRequest(), entries);
    }





    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        //        imap://MYUSERNAME@gmail.com:MYPASSWORD@imap.gmail.com

        if (args.length != 1) {
            System.err.println("usage: username:password");

            return;
        }

        String url = "imaps://" + args[0] + "@imap.gmail.com:993";
        System.err.println("url:" + url);

        Properties props   = System.getProperties();
        Session    session = Session.getDefaultInstance(props);
        URLName    urlName = new URLName(url);
        System.err.println("protocol:" + urlName.getProtocol() + " port:"
                           + urlName.getPort() + " password:"
                           + urlName.getPassword());

        Store store = session.getStore(urlName);

        if ( !store.isConnected()) {
            store.connect();
        }
        Folder emailFolder = store.getFolder("Inbox");
        emailFolder.open(Folder.READ_WRITE);
        int numMessages = emailFolder.getMessageCount();
        int cnt         = 0;
        for (int i = numMessages; i > 0; i--) {
            Message message = emailFolder.getMessage(i);
            if (message == null) {
                continue;
            }
            String subject = message.getSubject();
            if ( !subject.equals("for ramadda")) {
                continue;
            }
            if (message.isSet(Flags.Flag.SEEN)) {
                System.err.println("Have seen:" + message.getSubject());

                continue;
            }


            //            System.err.println("subject:" + message.getSubject());
            Object       content = message.getContent();
            StringBuffer sb      = new StringBuffer();
            MailUtil.extractText(content, sb);
            //            System.err.println("text:" + sb);

            if (true) {
                return;
            }
            /*
              FileOutputStream fos = new FileOutputStream("test.eml");
              message.writeTo(fos);
              fos.close();
              if(true) break;
            */
            if (cnt++ > 100) {
                break;
            }
        }
    }

    /**
     * _more_
     *
     * @param pattern _more_
     * @param text _more_
     *
     * @return _more_
     */
    private boolean matches(String pattern, String text) {
        if ( !defined(pattern)) {
            return true;
        }

        boolean doNot = false;
        if (pattern.startsWith("!")) {
            pattern = pattern.substring(1);
            doNot   = true;
        }
        //TODO: do regexp here
        boolean matches = true;
        if (text.toLowerCase().indexOf(pattern.toLowerCase()) < 0) {
            matches = false;
        }
        if (doNot) {
            return !matches;
        }

        return matches;
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Aug 23, '13
     * @author         Enter your name here...
     */
    private static class EntryInfo {

        /** _more_ */
        Entry parentEntry;

        /** _more_ */
        Entry newEntry;

        /** _more_ */
        String name;

        /** _more_ */
        StringBuffer text = new StringBuffer();

        /** _more_ */
        List<String> forwards = new ArrayList<String>();



        /**
         * _more_
         *
         * @param parentEntry _more_
         * @param name _more_
         */
        public EntryInfo(Entry parentEntry, String name) {
            this.parentEntry = parentEntry;
            this.name        = name;
        }

        /**
         * _more_
         *
         * @param entry _more_
         */
        public EntryInfo(Entry entry) {
            this.newEntry = entry;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public String toString() {
            return "EntryInfo:" + name + " fwds:" + forwards;
        }


    }

    /**
     * _more_
     *
     * @param text _more_
     *
     * @return _more_
     */
    private String cleanUpText(String text) {
        text = Utils.encodeUntrustedText(text);

        return text;
    }


}
