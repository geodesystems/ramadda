/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.mail;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;


import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.util.Date;
import java.util.List;

import javax.mail.*;
import javax.mail.internet.*;


/**
 *
 *
 */
public class MailTypeHandler extends GenericTypeHandler {

    /** _more_ */
    public static final String TYPE_MESSAGE = "mail_message";

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public MailTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * Gets called when the user has created a new entry from the File->New form or
     * when they have edited the entry. If the eml files were being harvested from a harvester
     * then also override TypeHandler.initializeNewEntry
     *
     * @param request _more_
     * @param entry _more_
     * @param parent _more_
     * @param newEntry _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeEntryFromForm(Request request, Entry entry,
                                        Entry parent, boolean newEntry)
            throws Exception {
        //If this is an edit then return
        if ( !newEntry) {
            return;
        }

        //If the file for the entry does not exist then return
        if ( !entry.isFile()) {
            return;
        }

        //Extract out the mail message metadata
        MimeMessage message = new MimeMessage(
                                  null,
                                  getStorageManager().getFileInputStream(
                                      entry.getFile().toString()));

        //If the user did not specify a name in the entry form then use the mail subject
        if (entryHasDefaultName(entry)) {
            String subject = message.getSubject();
            if (subject == null) {
                subject = "";
            }
            entry.setName(subject);
        }
        String       from     = InternetAddress.toString(message.getFrom());
        String       to =
            InternetAddress.toString(message.getAllRecipients());
        StringBuffer desc     = new StringBuffer();
        Object       content  = message.getContent();
        Date         fromDttm = message.getSentDate();
        Date         toDttm   = message.getReceivedDate();
        if (toDttm == null) {
            toDttm = fromDttm;
        }
        //Set the start and end date
        if (fromDttm != null) {
            entry.setStartDate(fromDttm.getTime());
        }
        if (toDttm != null) {
            entry.setEndDate(toDttm.getTime());
        }

        //Do more mail stuff
        processContent(request, entry, content, desc);

        //Now get the values (this would be the fromaddress and toaddress from types.xml
        Object[] values = getEntryValues(entry);
        values[0] = from;
        values[1] = to;

        //Set the description from the mail message
        if (entry.getDescription().length() == 0) {
            String description = desc.toString();
            if (description.length() > Entry.MAX_DESCRIPTION_LENGTH) {
                description = description.substring(0,
                        Entry.MAX_DESCRIPTION_LENGTH - 1);
            }
            System.err.println("desc length:" + description.length());
            entry.setDescription(description);
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param content _more_
     * @param desc _more_
     *
     * @throws Exception _more_
     */
    private void processContent(Request request, Entry entry, Object content,
                                StringBuffer desc)
            throws Exception {
        if (content instanceof MimeMultipart) {
            MimeMultipart multipart = (MimeMultipart) content;
            for (int i = 0; i < multipart.getCount(); i++) {
                MimeBodyPart part = (MimeBodyPart) multipart.getBodyPart(i);
                String       disposition = part.getDisposition();
                if (disposition == null) {
                    Object partContent = part.getContent();
                    if (partContent instanceof MimeMultipart) {
                        processContent(request, entry, partContent, desc);
                    } else {
                        String contentType =
                            part.getContentType().toLowerCase();
                        //Only ingest the text
                        if (contentType.indexOf("text/plain") >= 0) {
                            //                        System.err.println ("part content:" + partContent.getClass().getName());
                            desc.append(partContent);
                            desc.append("\n");
                        }
                    }

                    continue;
                }
                if (disposition.equalsIgnoreCase(Part.ATTACHMENT)
                        || disposition.equalsIgnoreCase(Part.INLINE)) {
                    if (part.getFileName() != null) {
                        InputStream inputStream = part.getInputStream();
                        File f = getStorageManager().getTmpFile(request,
                                     part.getFileName());
                        OutputStream outputStream =
                            getStorageManager().getFileOutputStream(f);
                        IOUtil.writeTo(inputStream, outputStream);
                        IOUtil.close(inputStream);
                        IOUtil.close(outputStream);
                        String fileName =
                            getStorageManager().copyToEntryDir(entry,
                                f).getName();
                        Metadata metadata =
                            new Metadata(
                                getRepository().getGUID(), entry.getId(),
                                ContentMetadataHandler.TYPE_ATTACHMENT,
                                false, fileName, null, null, null, null);
                        getMetadataManager().addMetadata(request,entry, metadata);
                    }
                }
            }
        } else if (content instanceof Part) {
            //TODO
            Part part = (Part) content;
        } else {
            //            System.err.println ("xxx content:" + content.getClass().getName());
            String contents = content.toString();
            desc.append(contents);
            desc.append("\n");
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param wikiTemplate _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getInnerWikiContent(Request request, Entry entry,
                                      String wikiTemplate)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        getPageHandler().entrySectionOpen(request, entry, sb, null);
        String from = entry.getStringValue(0, "");
        String to   = entry.getStringValue(1, "");
        sb.append(HtmlUtils.formTable());
        from = from.replace("<", "&lt;");
        from = from.replace(">", "&gt;");
        to   = to.replace("<", "&lt;");
        to   = to.replace(">", "&gt;");
        sb.append(HtmlUtils.formEntry(msgLabel("From"), from));
        sb.append(HtmlUtils.formEntry(msgLabel("To"), to));



        sb.append(HtmlUtils.formEntry(msgLabel("Date"),
                                      getDateHandler().formatDate(request,
                                          new Date(entry.getStartDate()),
                                          (String) null)));
        StringBuffer attachmentsSB = new StringBuffer();
        getMetadataManager().decorateEntry(request, entry, attachmentsSB,
                                           false);

        sb.append(HtmlUtils.formTableClose());
        sb.append(HtmlUtils.hr());
        String desc = entry.getDescription();
        desc = desc.replaceAll("\r\n\r\n", "\n<p>\n");
        sb.append(HtmlUtils.div(desc, HtmlUtils.cssClass("mail-body")));
        if (attachmentsSB.length() > 0) {
            sb.append(HtmlUtils.hr());
            sb.append(HtmlUtils.makeShowHideBlock(msg("Attachments"),
                    "<div class=\"description\">" + attachmentsSB + "</div>",
                    false));
        }

        getPageHandler().entrySectionClose(request, entry, sb);

        return sb.toString();
    }


}
