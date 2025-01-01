/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.phone;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.harvester.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.search.SearchManager;
import org.ramadda.repository.search.SearchProvider;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;
import org.ramadda.util.geo.Place;



import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;

import java.io.*;



import java.net.*;



import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;



/**
 */
@SuppressWarnings("unchecked")
public class PhoneHarvester extends Harvester {


    /** _more_ */
    public static final String CMD_LOGOUT = "logout";



    /** _more_ */
    public static final String[] CMDS_LOGIN = { "pass", "password", "login" };

    /** _more_ */
    public static final String[] CMDS_CD = { "cd", "go" };

    /** _more_ */
    public static final String[] CMDS_CLEAR = { "clear" };

    /** _more_ */
    public static final String[] CMDS_LS = { "ls", "list", "dir" };

    /** _more_ */
    public static final String[] CMDS_SEARCH = { "search", "find", };

    /** _more_ */
    public static final String[] CMDS_URL = { "url", "link" };

    /** _more_ */
    public static final String[] CMDS_GET = { "get", "more", "view" };

    /** _more_ */
    public static final String[] CMDS_COMMENTS = { "comments" };

    /** _more_ */
    public static final String[] CMDS_APPEND = { "+", "append", "add",
                                                 "cat" };

    /** _more_ */
    public static final String[] CMDS_PWD = { "pwd", "where", "ur" };

    //TODO:
    //tags <entry>
    //comments

    //edit_line := + (<create_line> | <tag_line> | <comment_line>)
    //create_line := (wiki|blog|folder|note|sms|mkdir) entry name _newline_ <text>
    //tag_line := tag <optional entry name> _newline_ <text>

    //entryid := this is always relative to the current working directory
    //it can be a child entry name or a path (e.g., child1/child2/child3
    //or a relatve path:
    // ../../child_name

    //tag <entry>
    //hello
    //there

    /** _more_ */
    public static final String[] CMDS_COPY = { "copy", "cp" };

    /** _more_ */
    public static final String[] CMDS_TAG = { "tag" };

    /** _more_ */
    public static final String[] CMDS_DELETE = { "delete", "rm" };


    /** _more_ */
    public static final String ATTR_TYPE = "type";

    /** _more_ */
    public static final String ATTR_FROMPHONE = "fromphone";


    /** _more_ */
    public static final String ATTR_TOPHONE = "tophone";

    /** _more_ */
    public static final String ATTR_PASSWORD_VIEW = "password_view";

    /** _more_ */
    public static final String ATTR_PASSWORD_ADD = "password_add";

    /** _more_ */
    public static final String ATTR_PASSWORD_EDIT = "password_edit";

    /** _more_ */
    public static final String ATTR_RESPONSE = "response";

    /** _more_ */
    public static final String ATTR_VOICEMESSAGE = "voicemessage";

    /** _more_ */
    public static final String ATTR_SHORTCUTS = "shortcuts";

    /** _more_ */
    public static final String ATTR_HELP = "help";

    /** _more_ */
    public static final String ATTR_ = "";


    /** _more_ */
    private String type = PhoneInfo.TYPE_SMS;

    /** _more_ */
    private String fromPhone;

    /** _more_ */
    private String toPhone;

    /** _more_ */
    private String passwordAdd;

    /** _more_ */
    private String passwordView;

    /** _more_ */
    private String passwordEdit;

    /** _more_ */
    private String response;


    /** _more_ */
    private String voiceMessage;

    /** _more_ */
    private String shortcuts;

    /** _more_ */
    private String help;

    /** _more_ */
    private Hashtable<String, String> phoneToEntry = new Hashtable<String,
                                                         String>();

    /** _more_ */
    private Hashtable<String, List<Entry>> phoneToEntries =
        new Hashtable<String, List<Entry>>();

    /** _more_ */
    private TTLCache<String, PhoneSession> sessions = new TTLCache<String,
                                                          PhoneSession>(24
                                                              * 60 * 60
                                                              * 1000);





    /**
     * _more_
     *
     * @param repository _more_
     * @param id _more_
     *
     * @throws Exception _more_
     */
    public PhoneHarvester(Repository repository, String id) throws Exception {
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
    public PhoneHarvester(Repository repository, Element node)
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
        fromPhone = normalizePhone(XmlUtil.getAttribute(element,
                ATTR_FROMPHONE, fromPhone));
        toPhone = normalizePhone(XmlUtil.getAttribute(element, ATTR_TOPHONE,
                toPhone));
        passwordView = XmlUtil.getAttribute(element, ATTR_PASSWORD_VIEW,
                                            passwordView);
        passwordEdit = XmlUtil.getAttribute(element, ATTR_PASSWORD_EDIT,
                                            passwordEdit);
        passwordAdd = XmlUtil.getAttribute(element, ATTR_PASSWORD_ADD,
                                           passwordAdd);
        response = XmlUtil.getAttribute(element, ATTR_RESPONSE, response);
        shortcuts = Utils.getAttributeOrTag(element, ATTR_SHORTCUTS,
                                            shortcuts);
        help = Utils.getAttributeOrTag(element, ATTR_HELP, help);
        voiceMessage = XmlUtil.getAttribute(element, ATTR_VOICEMESSAGE,
                                            voiceMessage);
        type = XmlUtil.getAttribute(element, ATTR_TYPE, type);
    }

    /**
     * _more_
     *
     * @param phone _more_
     *
     * @return _more_
     */
    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        phone = phone.replaceAll(" ", "");
        phone = phone.replaceAll("-", "");

        return phone;
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
        text = text.replaceAll("\n", "<br>");

        return text;
    }


    /**
     * _more_
     *
     * @param info _more_
     *
     * @return _more_
     */
    private PhoneSession getSession(PhoneInfo info) {
        PhoneSession session = sessions.get(info.getFromPhone());
        if (session == null) {
            session = makeSession(info, "somedummypassword");
        }

        return setSessionState(session);

    }


    /**
     * _more_
     *
     * @param info _more_
     *
     * @return _more_
     */
    public boolean canEdit(PhoneInfo info) {
        return getSession(info).getCanEdit();
    }





    /**
     * _more_
     *
     * @param request _more_
     * @param info _more_
     * @param msg _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean handleMessage(Request request, PhoneInfo info,
                                 Appendable msg)
            throws Exception {

        if ((fromPhone != null) && (fromPhone.length() > 0)) {
            if (info.getFromPhone().indexOf(normalizePhone(fromPhone)) < 0) {
                //                System.err.println ("handleMessage: skipping wrong from phone");
                return false;
            }
        }

        if ((toPhone != null) && (toPhone.length() > 0)) {
            if (info.getToPhone().indexOf(toPhone) < 0) {
                //                System.err.println ("handleMessage: skipping wrong to phone");
                return false;
            }
        }


        return handleMessageInner(request, info, msg);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param info _more_
     * @param msg _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean handleMessageInner(Request request, PhoneInfo info,
                                      Appendable msg)
            throws Exception {

        System.err.println("PhoneHarvester: handleMessage: from:"
                           + info.getFromPhone() + " to:" + info.getToPhone()
                           + " " + info.getMessage());
        Entry  baseGroup    = getBaseGroup();
        Entry  currentEntry = baseGroup;
        String pastEntry    = phoneToEntry.get(info.getFromPhone());
        if (pastEntry != null) {
            Entry entry = getEntryManager().getEntry(request, pastEntry,
                              false);
            if (entry != null) {
                currentEntry = entry;
            }
        }
        List<Entry> entries     = phoneToEntries.get(info.getFromPhone());

        String      message     = info.getMessage();
        String      name        = "SMS Message";
        String      toEntryName = null;
        int         spaceIndex  = message.indexOf(" ", 10);
        if (spaceIndex < 0) {
            spaceIndex = message.indexOf("\n");
        }
        if (spaceIndex > 0) {
            name = message.substring(0, spaceIndex);
        } else {
            //??
        }



        message = message.trim();


        String       type = null;
        String       tmp;
        StringBuffer descSB            = new StringBuffer();
        boolean      doAppend          = false;
        boolean      processedACommand = false;
        String       sessionKey        = info.getFromPhone();
        PhoneSession session           = getSession(info);
        String       lastMessage       = session.lastMessage;
        session.lastMessage = message;

        if (message.equalsIgnoreCase("knock knock")) {
            msg.append("Who's there?");

            return true;
        }

        if (lastMessage.equalsIgnoreCase("knock knock")) {
            msg.append(message + " who?");

            return true;
        }

        if (message.equals("help") || message.equals("?")) {
            msg.append(getHelp());

            return true;
        }



        List<String> urls = new ArrayList<String>();
        for (int i = 0; i < 10; i++) {
            if ( !request.defined("MediaUrl" + i)) {
                break;
            }
            String url = request.getString("MediaUrl" + i);
            url = java.net.URLDecoder.decode(url, "UTF-8");

            urls.add(url);
            System.err.println("url:" + url);
        }


        if (urls.size() > 0) {
            if ( !session.getCanAdd()) {
                msg.append("No permission to add\nEnter:\npass <password>");

                return true;
            }
            for (String url : urls) {
                String tail = IOUtil.getFileTail(url);
                File newFile = getStorageManager().getTmpFile(request, tail);

                InputStream fromStream =
                    getStorageManager().getInputStream(url);
                OutputStream toStream =
                    getStorageManager().getFileOutputStream(newFile);
                try {
                    IOUtil.writeTo(fromStream, toStream);
                } finally {
                    IOUtil.close(toStream);
                    IOUtil.close(fromStream);
                }
                type    = null;
                newFile = getStorageManager().moveToStorage(request, newFile);

                addEntry(info, currentEntry, type, msg, tail,
                         cleanUpText(message), newFile.toString());
            }

            return true;
        }



        List<String> lines = StringUtil.split(message, "\n");
        for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
            String line = lines.get(lineIdx);
            line = line.trim();
            String tline = line.toLowerCase();

            if (tline.equals(CMD_LOGOUT)) {
                session.logout();
                processedACommand = true;

                continue;
            }


            String remainder;
            //nist-constant search 

            if (shortcuts != null) {
                List<String> cmdToks = StringUtil.splitUpTo(line, " ", 2);
                String       command = cmdToks.get(0);
                String       prefix  = command.toLowerCase() + " ";
                for (String shortcut :
                        StringUtil.split(shortcuts, "\n", true, true)) {
                    if (shortcut.startsWith(prefix)) {
                        List<String> toks = StringUtil.splitUpTo(shortcut,
                                                " ", 3);
                        line = toks.get(1) + " ";
                        if (cmdToks.size() > 1) {
                            line += cmdToks.get(1);
                        }
                        if (toks.size() > 2) {
                            Hashtable props =
                                HtmlUtils.parseHtmlProperties(toks.get(2));
                            request.putAll(props);
                        }
                    }
                }
            }
            //            System.err.println ("PhoneHarvester: line=" + line);
            if ((remainder = checkCommand(CMDS_LOGIN, line)) != null) {
                session.logout();
                session.password = remainder;
                setSessionState(session);
                if ( !session.passwordOK) {
                    msg.append("Bad password\nEnter:\npass <password>");

                    return true;
                }
                msg.append("password OK\n");
                if (session.getCanAdd()) {
                    msg.append("you can add\n");
                }
                if (session.getCanEdit()) {
                    msg.append("you can edit\n");
                }

                processedACommand = true;

                continue;
            }

            if ( !session.getCanView()) {
                msg.append(
                    "Access is not allowed without a password\nEnter:\npass <password>");

                return true;
            }

            if ((remainder = checkCommand(CMDS_SEARCH, line)) != null) {
                StringBuilder text = new StringBuilder();
                List<String> toks = StringUtil.split(remainder, " ", true,
                                        true);
                int providerCnt = 0;
                for (int i = 0; i < toks.size(); i++) {
                    String tok = toks.get(i);
                    //                    System.err.println ("Tok:" + tok);
                    if (tok.startsWith("-")) {
                        if (tok.equals("-type")) {
                            if (i < toks.size() - 1) {
                                request.put(ARG_TYPE, toks.get(i + 1));
                                i++;
                            }
                        } else {
                            SearchProvider provider =
                                getSearchManager().getSearchProvider(
                                    tok.substring(1).toLowerCase());
                            if (provider != null) {
                                //                                System.err.println ("adding provider:" + provider.getId());
                                request.putMultiples(
                                    SearchManager.ARG_PROVIDER,
                                    provider.getId());
                            }
                        }
                    } else {
                        if (i > 0) {
                            text.append(" ");
                        }
                        text.append(tok);
                    }
                }
                //search boron
                //                System.err.println("text:" + text);
                request.put(ARG_TEXT, text.toString());
                entries =  getSearchManager().doSearch(request,
						       new org.ramadda.repository.util.SelectInfo(request));
                phoneToEntries.put(info.getFromPhone(), entries);
                String prefix = request.getString("prefix", (String) null);
                if (prefix != null) {
                    msg.append(prefix);
                    msg.append("\n");
                }
                if (entries.size() == 0) {
                    msg.append("Nothing found");
                } else if (entries.size() == 1) {
                    getEntryDetails(getRequest(), entries.get(0), msg);
                } else {
                    makeList(entries, msg, null,
                             request.get("details", false));
                }

                return true;
            }

            if ((remainder = checkCommand(CMDS_LS, line)) != null) {
                int cnt = 0;
                makeList((entries != null)
                         ? entries
                         : getEntryManager().getChildren(request,
                         currentEntry), msg, remainder, false);

                if (msg.toString().length() == 0) {
                    msg.append("No entries found");
                }
                msg.append("\n");
                processedACommand = true;

                continue;
            }

            if ((remainder = checkCommand(CMDS_CLEAR, line)) != null) {
                if ( !session.getCanEdit()) {
                    msg.append("clear is not allowed\nYou need edit access");

                    return true;
                }
                currentEntry = getEntry(request, remainder, currentEntry,
                                        entries, msg);
                if (currentEntry == null) {
                    return true;
                }
                currentEntry.setDescription("");
                List<Entry> tmpentries =
                    (List<Entry>) Misc.newList(currentEntry);
                getEntryManager().addNewEntries(getRequest(), tmpentries);
                processedACommand = true;

                continue;
            }


            if ((remainder = checkCommand(CMDS_TAG, line)) != null) {
                if ( !session.getCanEdit()) {
                    msg.append("clear is not allowed\nYou need edit access");

                    return true;
                }
                currentEntry = getEntry(request, remainder, currentEntry,
                                        entries, msg);
                if (currentEntry == null) {
                    return true;
                }
                lineIdx++;
                for (; lineIdx < lines.size(); lineIdx++) {
                    String tag = lines.get(lineIdx);
                    Metadata metadata =
                        new Metadata(getRepository().getGUID(),
                                     currentEntry.getId(),
                                     getMetadataManager().findType(EnumeratedMetadataHandler.TYPE_TAG),
                                     false, tag, "", "", "", null);
                    System.err.println("metadata:" + currentEntry + " "
                                       + metadata + " " + tag);
                    getMetadataManager().addMetadata(request,currentEntry, metadata);
                    getMetadataManager().insertMetadata(metadata);
                }

                return true;
            }



            if ((remainder = checkCommand(CMDS_URL, line)) != null) {
                currentEntry = getEntry(request, remainder, currentEntry,
                                        entries, msg);
                if (currentEntry == null) {
                    return true;
                }
                msg.append("entry:\n" + getEntryInfo(currentEntry));
                msg.append("\n");
                processedACommand = true;

                continue;
            }

            if ((remainder = checkCommand(CMDS_GET, line)) != null) {
                currentEntry = getEntry(request, remainder, currentEntry,
                                        entries, msg);
                if (currentEntry == null) {
                    return true;
                }

                getEntryDetails(getRequest(), currentEntry, msg);
                processedACommand = true;

                continue;
            }

            if ((remainder = checkCommand(CMDS_COMMENTS, line)) != null) {
                currentEntry = getEntry(request, remainder, currentEntry,
                                        entries, msg);
                if (currentEntry == null) {
                    return true;
                }
                List<org.ramadda.repository.Comment> comments =
                    getRepository().getCommentManager().getComments(
                        getRequest(), currentEntry);
                for (org.ramadda.repository.Comment comment : comments) {
                    msg.append(XmlUtil.encodeString("Comment:"
                            + comment.getSubject() + "\n"
                            + comment.getComment() + "\n"));
                }
                if (comments.size() == 0) {
                    msg.append("No comments available\n");
                }
                processedACommand = true;

                continue;
            }


            if ((remainder = checkCommand(CMDS_PWD, line)) != null) {
                currentEntry = getEntry(request, remainder, currentEntry,
                                        entries, msg);
                int         cnt       = 0;
                List<Entry> ancestors = new ArrayList<Entry>();
                ancestors.add(currentEntry);
                if ( !currentEntry.equals(baseGroup)) {
                    Entry theEntry = currentEntry;
                    while (ancestors.size() < 4) {
                        theEntry = theEntry.getParentEntry();
                        if (theEntry == null) {
                            break;
                        }
                        ancestors.add(theEntry);
                        if (theEntry.equals(baseGroup)) {
                            break;
                        }
                    }
                }
                String tab = "";
                for (int i = ancestors.size() - 1; i >= 0; i--) {
                    msg.append(tab);
                    tab = tab + "  ";
                    msg.append(getEntryName(ancestors.get(i)));
                    msg.append("\n");
                }
                msg.append(getEntryUrl(currentEntry));
                msg.append("\n");
                processedACommand = true;

                continue;
            }

            if ((remainder = checkCommand(CMDS_CD, line)) != null) {
                phoneToEntries.remove(info.getFromPhone());
                if (remainder.length() == 0) {
                    currentEntry = baseGroup;
                } else if (remainder.startsWith("/")) {
                    currentEntry = getEntry(request, remainder, baseGroup,
                                            entries, msg);
                } else if (remainder.startsWith("..")) {
                    boolean haveSeenBaseGroup = false;
                    for (String tok :
                            StringUtil.split(remainder, "/", true, true)) {
                        if (currentEntry.equals(baseGroup)) {
                            haveSeenBaseGroup = true;
                        }
                        if (tok.equals("..")) {
                            if (haveSeenBaseGroup) {
                                break;
                            }
                            currentEntry = currentEntry.getParentEntry();
                        } else {
                            Entry childEntry =
                                getEntryManager().findEntryWithName(request,
                                    currentEntry, tok);
                            if (childEntry == null) {
                                msg.append("Pad path:" + tok);

                                return true;
                            }
                            currentEntry = childEntry;
                        }
                    }
                } else {
                    currentEntry = getEntry(request, remainder, currentEntry,
                                            entries, msg);
                    if (currentEntry == null) {
                        return true;
                    }
                }
                phoneToEntry.put(info.getFromPhone(), currentEntry.getId());
                processedACommand = true;

                continue;
            }


            if ( !session.getCanAdd()) {
                msg.append("No permission to add\nEnter:\npass <password>");

                return true;
            }

            if ((remainder = checkCommand(CMDS_APPEND, line)) != null) {
                currentEntry = getEntry(request, remainder, currentEntry,
                                        entries, msg);
                doAppend          = true;
                processedACommand = true;
                lineIdx++;
                for (; lineIdx < lines.size(); lineIdx++) {
                    if (descSB.length() > 0) {
                        descSB.append("\n");
                    }
                    descSB.append(lines.get(lineIdx));
                }

                break;
            }


            if (type == null) {
                String[] cmds  = {
                    "folder", "mkdir", "wiki", "blog", "sms", "note"
                };
                String[] types = {
                    TypeHandler.TYPE_GROUP, TypeHandler.TYPE_GROUP,
                    "wikipage", "blogentry", "phone_sms", "notes_note"
                };
                boolean didOne = false;
                for (int i = 0; i < cmds.length; i++) {
                    if (tline.startsWith(cmds[i] + " ")) {
                        type   = types[i];
                        name   = line.substring(cmds[i].length()).trim();
                        didOne = true;

                        break;
                    }
                }
                if (didOne) {
                    lineIdx++;
                    for (; lineIdx < lines.size(); lineIdx++) {
                        if (descSB.length() > 0) {
                            descSB.append("\n");
                        }
                        descSB.append(lines.get(lineIdx));
                    }

                    break;
                }
            }

            msg.append("Unknown command:\n" + line);

            return true;
        }



        String cleanedInputText = cleanUpText(descSB.toString());


        Entry  parent           = currentEntry;
        if (doAppend) {
            getEntryManager().appendText(getRequest(), currentEntry,
                                         cleanedInputText);
            msg.append("appended to:\n" + getEntryInfo(currentEntry));

            return true;
        }
        if ( !defined(name)) {
            name = "SMS Entry";
        }
        name = name.trim();



        if (type == null) {
            if ( !processedACommand) {
                msg.append("No commands were given\n" + getHelp());
            }

            return true;
        }


        addEntry(info, currentEntry, type, msg, name, cleanedInputText, null);

        return true;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param msg _more_
     *
     * @throws Exception _more_
     */
    private void getEntryDetails(Request request, Entry entry, Appendable msg)
            throws Exception {
        String contents =
            entry.getTypeHandler().getTemplateContent(getRequest(), entry,
                "template.contents.sms", null);
        if (contents == null) {
            String theName =
                entry.getTypeHandler().getTemplateContent(getRequest(),
                    entry, "template.label.sms", entry.getName());
            contents = theName + "\n" + entry.getDescription();
        }
        contents = contents.replaceAll("<br>", "\n").replaceAll("<p>", "\n");
        msg.append(XmlUtil.encodeString(contents));
        msg.append("\n");
        msg.append(getEntryUrl(entry));
    }


    /**
     * _more_
     *
     * @param entries _more_
     * @param msg _more_
     * @param namePattern _more_
     * @param details _more_
     *
     * @throws Exception _more_
     */
    private void makeList(List<Entry> entries, Appendable msg,
                          String namePattern, boolean details)
            throws Exception {
        int cnt = 0;
        for (Entry child : entries) {
            if ((namePattern != null) && (namePattern.length() > 0)) {
                String childName = child.getName().trim();
                if (childName.indexOf(namePattern) < 0) {
                    continue;
                }
            }
            cnt++;
            msg.append("#" + cnt + " ");
            if (child.isGroup()) {
                msg.append(">");
                //msg.append(str);
            } else {
                msg.append(" ");
            }


            msg.append(getEntryName(child));
            msg.append("\n");
            if (details) {
                msg.append(
                    child.getTypeHandler().getTemplateContent(
                        getRequest(), child, "template.contents.sms", ""));
            }
        }


    }


    /**
     * _more_
     *
     * @param info _more_
     * @param currentEntry _more_
     * @param type _more_
     * @param msg _more_
     * @param name _more_
     * @param cleanedInputText _more_
     * @param resource _more_
     *
     * @throws Exception _more_
     */
    private void addEntry(PhoneInfo info, Entry currentEntry, String type,
                          Appendable msg, String name,
                          String cleanedInputText, String resource)
            throws Exception {
        if ( !currentEntry.isGroup()) {
            msg.append("ERROR: Not a folder:\n" + currentEntry.getName());

            return;
        }

        TypeHandler typeHandler = null;
        if (type != null) {
            typeHandler = getRepository().getTypeHandler(type);
        } else if (resource != null) {
            typeHandler = getEntryManager().findDefaultTypeHandler(null,resource);
        } else {
            typeHandler = getRepository().getTypeHandler("file");
        }
        Entry    entry  = typeHandler.createEntry(getRepository().getGUID());
        Date     date   = new Date();
        Object[] values = typeHandler.makeEntryValues(new Hashtable());
        if (type.equals("phone_sms")) {
            values[0] = info.getFromPhone();
            values[1] = info.getToPhone();
        } else if (type.equals("wikipage")) {
            values[0]        = cleanedInputText;
            cleanedInputText = "";
        }

        entry.initEntry(name, cleanedInputText, currentEntry, getUser(),
                        (resource != null)
                        ? new Resource(resource)
                        : new Resource(), "", Entry.DEFAULT_ORDER,
                                          date.getTime(), date.getTime(),
                                          date.getTime(), date.getTime(),
                                          values);


        Place place = org.ramadda.util.geo.GeoUtils.getLocationFromAddress(
									   info.getFromZip(),null);
        if (place != null) {
            entry.setLocation(place.getLatitude(), place.getLongitude(), 0);
        }

        List<Entry> entries = (List<Entry>) Misc.newList(entry);
        getEntryManager().addNewEntries(getRequest(), entries);
        msg.append("New entry:\n" + getEntryInfo(entry));

    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    private String getEntryUrl(Entry entry) {
        return getRepository().URL_ENTRY.getFullUrl() + "/" + entry.getId();
        //        return getRepository().URL_ENTRY_SHOW.getFullUrl() + "?entryid=" + entry.getId();
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    private String getEntryInfo(Entry entry) {
        return entry.getName() + "\n" + getEntryUrl(entry);
    }


    /**
     * _more_
     *
     * @param info _more_
     *
     * @return _more_
     */
    private boolean checkPhone(PhoneInfo info) {
        if (defined(fromPhone)) {
            if (info.getFromPhone().indexOf(fromPhone) < 0) {
                System.err.println(
                    "handleMessage: skipping wrong from phone");

                return false;
            }
        }

        if ((toPhone != null) && (toPhone.length() > 0)) {
            if (info.getToPhone().indexOf(toPhone) < 0) {
                System.err.println("handleMessage: skipping wrong to phone");

                return false;
            }
        }

        return true;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param info _more_
     * @param msg _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean handleVoice(Request request, PhoneInfo info,
                               StringBuffer msg)
            throws Exception {
        if (getVoiceResponse(info) == null) {
            return false;
        }
        System.err.println("handleVoice:" + fromPhone + ":"
                           + info.getFromPhone() + ": to phone:" + toPhone
                           + ":" + info.getToPhone());
        PhoneSession session = getSession(info);
        if ( !session.getCanEdit()) {
            throw new IllegalAccessException("No edit permissions");
        }

        Entry  baseGroup = getBaseGroup();
        Entry  parent    = baseGroup;
        String pastEntry = phoneToEntry.get(info.getFromPhone());
        if (pastEntry != null) {
            Entry entry = getEntryManager().getEntry(request, pastEntry,
                              false);
            if (entry != null) {
                parent = entry;
            }
        }

        String name = "Voice Message  - "
                      + getDateHandler().formatDate(request, new Date());
        String       type             = "phone_message";
        TypeHandler  typeHandler      = getRepository().getTypeHandler(type);
        Entry entry = typeHandler.createEntry(getRepository().getGUID());
        Date         date             = new Date();
        Object[]     values = typeHandler.makeEntryValues(new Hashtable());
        StringBuffer descSB = new StringBuffer(info.getTranscription());
        String       cleanedInputText = cleanUpText(descSB.toString());
        File voiceFile = fetchVoiceFile(request,
                                        new URL(info.getRecordingUrl()));

        if (voiceFile == null) {
            return false;
        }
        voiceFile = getStorageManager().moveToStorage(request, voiceFile);
        Resource resource = new Resource(voiceFile.toString(),
                                         Resource.TYPE_STOREDFILE);
        entry.initEntry(name, cleanedInputText, parent, getUser(), resource,
                        "", Entry.DEFAULT_ORDER, date.getTime(),
                        date.getTime(), date.getTime(), date.getTime(),
                        values);


        Place place = org.ramadda.util.geo.GeoUtils.getLocationFromAddress(
									   info.getFromZip(),null);
        if (place != null) {
            entry.setLocation(place.getLatitude(), place.getLongitude(), 0);
        }

        List<Entry> entries = (List<Entry>) Misc.newList(entry);
        getEntryManager().addNewEntries(getRequest(), entries);
        msg.append("New voice entry:\n" + getEntryUrl(entry));

        return true;
    }




    /**
     * _more_
     *
     * @return _more_
     */
    public String getDescription() {
        return "Phone and SMS Harvester";
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     */
    public void makeRunSettings(Request request, StringBuffer sb) {
        StringBuffer runWidgets = new StringBuffer();
        runWidgets.append(
            HtmlUtils.checkbox(
                ATTR_ACTIVEONSTART, "true",
                getActiveOnStart()) + HtmlUtils.space(1) + msg("Active"));
        sb.append(HtmlUtils.formEntryTop("", runWidgets.toString()));
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
        element.setAttribute(ATTR_FROMPHONE, fromPhone);
        element.setAttribute(ATTR_TOPHONE, toPhone);
        element.setAttribute(ATTR_PASSWORD_VIEW, passwordView);
        element.setAttribute(ATTR_PASSWORD_ADD, passwordAdd);
        element.setAttribute(ATTR_PASSWORD_EDIT, passwordEdit);
        element.setAttribute(ATTR_RESPONSE, response);

        if (shortcuts != null) {
            XmlUtil.create(ATTR_SHORTCUTS, element).appendChild(
                element.getOwnerDocument().createTextNode(shortcuts));
        }
        if (help != null) {
            XmlUtil.create(ATTR_HELP, element).appendChild(
                element.getOwnerDocument().createTextNode(help));
        }

        element.setAttribute(ATTR_VOICEMESSAGE, voiceMessage);
        element.setAttribute(ATTR_TYPE, type);
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
        fromPhone    = request.getString(ATTR_FROMPHONE, fromPhone);
        toPhone      = request.getString(ATTR_TOPHONE, toPhone);
        passwordView = request.getString(ATTR_PASSWORD_VIEW, passwordView);
        passwordAdd  = request.getString(ATTR_PASSWORD_ADD, passwordAdd);
        passwordEdit = request.getString(ATTR_PASSWORD_EDIT, passwordEdit);

        response     = request.getString(ATTR_RESPONSE, response);
        shortcuts    = request.getString(ATTR_SHORTCUTS, shortcuts);
        help         = request.getString(ATTR_HELP, help);
        voiceMessage = request.getString(ATTR_VOICEMESSAGE, voiceMessage);
        type         = request.getString(ATTR_TYPE, type);
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
        addBaseGroupSelect(ATTR_BASEGROUP, sb);


        String suffix = " no spaces, dashes, etc";
        sb.append(HtmlUtils.row(HtmlUtils.col("&nbsp;")));
        sb.append(
            HtmlUtils.row(
                HtmlUtils.colspan(
                    "Accept input when the following optional criteria is met",
                    2)));
        sb.append(HtmlUtils.formEntry(msgLabel("From Phone"),
                                      HtmlUtils.input(ATTR_FROMPHONE,
                                          fromPhone,
                                          HtmlUtils.SIZE_15) + suffix));
        sb.append(HtmlUtils.formEntry(msgLabel("To Phone"),
                                      HtmlUtils.input(ATTR_TOPHONE, toPhone,
                                          HtmlUtils.SIZE_15) + suffix));
        String msg1 =
            "  Entry \"any\" to allow for access without a password";

        sb.append(HtmlUtils.formEntry(msgLabel("View Password"),
                                      HtmlUtils.input(ATTR_PASSWORD_VIEW,
                                          passwordView,
                                          HtmlUtils.SIZE_15) + msg1));
        sb.append(HtmlUtils.formEntry(msgLabel("Add Password"),
                                      HtmlUtils.input(ATTR_PASSWORD_ADD,
                                          passwordAdd, HtmlUtils.SIZE_15)));
        sb.append(HtmlUtils.formEntry(msgLabel("Edit Password"),
                                      HtmlUtils.input(ATTR_PASSWORD_EDIT,
                                          passwordEdit, HtmlUtils.SIZE_15)));

        /*
          sb.append(HtmlUtils.formEntryTop(msgLabel("SMS Response"),
          HtmlUtils.textArea(ATTR_RESPONSE,
          response==null?"":response,5,60) +"<br>" + "Use ${url} for the URL to the created entry"));
        */


        sb.append(HtmlUtils.row(HtmlUtils.col("&nbsp;")));
        sb.append(
            HtmlUtils.row(
                HtmlUtils.colspan(
                    "Specify a voice response to handle voice message", 2)));

        sb.append(HtmlUtils.formEntryTop(msgLabel("Voice Message"),
                                         HtmlUtils.input(ATTR_VOICEMESSAGE,
                                             voiceMessage,
                                             HtmlUtils.SIZE_60)));

        sb.append(HtmlUtils.formEntryTop(msgLabel("Shortcuts"),
                                         "key command &lt;name=value&gt;<br>"
                                         + HtmlUtils.textArea(ATTR_SHORTCUTS,
                                             (shortcuts == null)
                                             ? ""
                                             : shortcuts, 5, 60)));

        sb.append(HtmlUtils.formEntryTop(msgLabel("Help Message"),
                                         HtmlUtils.textArea(ATTR_HELP,
                                             (help == null)
                                             ? ""
                                             : help, 5, 60)));
    }


    /**
     * _more_
     *
     *
     * @param timestamp _more_
     * @throws Exception _more_
     */
    protected void runInner(int timestamp) throws Exception {
        if ( !canContinueRunning(timestamp)) {
            //            return true;
        }
    }


    /**
     * _more_
     *
     * @param info _more_
     *
     * @return _more_
     */
    public String getVoiceResponse(PhoneInfo info) {
        if ( !checkPhone(info)) {
            return null;
        }
        if ((voiceMessage != null) && (voiceMessage.trim().length() == 0)) {
            return null;
        }

        return voiceMessage;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param url _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private File fetchVoiceFile(Request request, URL url) throws Exception {
        String tail    = "voicemessage.mp3";
        File   newFile = getStorageManager().getTmpFile(request, tail);
        url = new URL(url.toString() + ".mp3");
        URLConnection connection = url.openConnection();
        InputStream   fromStream = connection.getInputStream();
        OutputStream toStream =
            getStorageManager().getFileOutputStream(newFile);
        try {
            long bytes = IOUtil.writeTo(fromStream, toStream);
            if (bytes < 0) {
                System.err.println(
                    "PhoneHarvester: failed to read voice URL:" + url);

                return null;
            }
        } finally {
            IOUtil.close(toStream);
            IOUtil.close(fromStream);
        }

        return newFile;
    }

    /**
     * _more_
     *
     * @param cmds _more_
     * @param line _more_
     *
     * @return _more_
     */
    private String checkCommand(String[] cmds, String line) {
        String tline = line.toLowerCase();
        for (String cmd : cmds) {
            if (tline.equals(cmd) || tline.startsWith(cmd + " ")) {
                return line.substring(cmd.length()).trim();
            }
        }

        return null;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param line _more_
     * @param currentEntry _more_
     * @param entries _more_
     * @param msg _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Entry getEntry(Request request, String line, Entry currentEntry,
                           List<Entry> entries, Appendable msg)
            throws Exception {
        for (String name : StringUtil.split(line, "/", true, true)) {

            Entry childEntry = null;
            if (name.matches("\\d+")) {
                int index = Integer.parseInt(name);
                index--;
                List<Entry> children = (entries != null)
                                       ? entries
                                       : getEntryManager().getChildren(
                                           request, currentEntry);
                if ((index < 0) || (index >= children.size())) {
                    msg.append("Bad index:" + index);

                    return null;
                }
                childEntry = children.get(index);
            } else {
                childEntry = getEntryManager().findEntryWithName(request,
                        currentEntry, name);

            }
            if (childEntry == null) {
                msg.append("Could not find:\n" + name);

                return null;
            }
            currentEntry = childEntry;
        }

        return currentEntry;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    private String getHelp() {
        if (Utils.stringDefined(help)) {
            return help;
        }

        return "pass <password>\n" + "ls,go,ur,url,get <path>\n" + "append\n"
               + "new:\n" + "folder,note, blog <name>\n" + "<text>\n\n"
               + "http://ramadda.org/repository/phone/index.html";
    }


    /**
     * _more_
     *
     * @param info _more_
     * @param password _more_
     *
     * @return _more_
     */
    private PhoneSession makeSession(PhoneInfo info, String password) {
        PhoneSession session =
            setSessionState(new PhoneSession(info.getFromPhone(), password));
        sessions.put(info.getFromPhone(), session);

        return session;
    }

    /**
     * _more_
     *
     * @param session _more_
     *
     * @return _more_
     */
    private PhoneSession setSessionState(PhoneSession session) {

        String password = session.password;

        session.canView = false;
        session.canAdd  = false;
        session.canEdit = false;

        String  viewPassword      = passwordView;
        String  addPassword       = passwordAdd;
        String  editPassword      = passwordEdit;

        boolean somePasswordWasOK = false;
        //password, "any", blank
        if (defined(editPassword)) {
            session.canEdit = password.equals(editPassword);
            if (session.canEdit) {
                somePasswordWasOK = true;
            }
        }


        if (defined(addPassword)) {
            if (addPassword.equals("any")) {
                session.canAdd = true;
            } else {
                session.canAdd = password.equals(addPassword);
                if (session.canAdd) {
                    somePasswordWasOK = true;
                }
            }
        }

        if (defined(viewPassword)) {
            if (viewPassword.equals("any")) {
                session.canView = true;
            } else {
                session.canView = password.equals(viewPassword);
                if (session.canView) {
                    somePasswordWasOK = true;
                }
            }
        }

        session.passwordOK = somePasswordWasOK;

        if ( !session.canAdd) {
            session.canAdd = session.canEdit;
        }
        if ( !session.canView) {
            session.canView = session.canAdd;
        }

        return session;
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String getEntryName(Entry entry) throws Exception {
        String name = entry.getTypeHandler().getTemplateContent(getRequest(),
                          entry, "template.label.sms", entry.getName());
        if (name.length() > 50) {
            name = name.substring(0, 49) + "...";
        }

        return name;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public int getWeight() {
        int weight = 0;
        if (defined(fromPhone)) {
            weight += 2;
        }
        if (defined(toPhone)) {
            weight += 2;
        }
        if (defined(passwordView)) {
            weight++;
        }
        if (defined(passwordAdd)) {
            weight++;
        }
        if (defined(passwordEdit)) {
            weight++;
        }

        return weight;
    }




    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Aug 23, '13
     * @author         Enter your name here...
     */
    private static class PhoneSession {

        /** _more_ */
        boolean passwordOK = true;

        /** _more_ */
        String fromPhone;

        /** _more_ */
        String password;

        /** _more_ */
        boolean canView = false;

        /** _more_ */
        boolean canAdd = false;

        /** _more_ */
        boolean canEdit = false;

        /** _more_ */
        String lastMessage = "";

        /**
         * _more_
         *
         * @param fromPhone _more_
         * @param password _more_
         */
        PhoneSession(String fromPhone, String password) {
            this.fromPhone = fromPhone;
            this.password  = password;
        }

        /**
         * _more_
         */
        public void logout() {
            canView  = false;
            canAdd   = false;
            canEdit  = false;
            password = "";
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean getCanView() {
            return canView;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean getCanAdd() {
            return canAdd;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean getCanEdit() {
            return canEdit;
        }
    }



}
