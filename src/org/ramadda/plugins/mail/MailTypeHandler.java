/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.mail;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.FormInfo;
import org.ramadda.util.Utils;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.WikiUtil;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;


import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.util.LinkedHashMap;
import java.util.Date;
import java.util.Hashtable;
import java.util.Properties;
import java.util.HashSet;
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

    private static  int IDX=0;
    public static final int IDX_SUBJECT=IDX++;
    public static final int IDX_FROM=IDX++;        
    public static final int IDX_TO=IDX++;    
    private Session session;

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
	System.setProperty("mail.mime.address.strict", "false");
	Properties props = new Properties();
	props.setProperty("mail.mime.address.strict", "false");
	this.session = Session.getDefaultInstance(props);

    }

    private String clean(String s) {
	if(s!=null) {
	    s = s.replace("<", " ").replace(">", " ").trim();
	}
	return s;
    }

    private String clean2(String s) {
	if(s!=null) {
	    s = s.replace("<", "&lt;").replace(">", "&gt;");
	}
	return s;
    }    

    private void addMetadata(Request request,Entry entry,String email) throws Exception {
	if(!stringDefined(email)) return;
	Metadata metadata =
	    new Metadata(
			 getRepository().getGUID(), entry.getId(),
			 getMetadataManager().findType("email_address"),
			 false, email, null, null, null, null);
	getMetadataManager().addMetadata(request,entry, metadata);
    }



    @Override
    public void addToEntryForm(Request request, Appendable formBuffer,
                               Entry parentEntry, Entry entry,
                               FormInfo formInfo)
            throws Exception {
	if(entry==null) {
	    String cbx = HU.labeledCheckbox("processhtml",  "true",false,"Process HTML");
	    cbx += "&nbsp;" + HU.labeledCheckbox("addaddresses",  "true",true,"Add Addresses");
	    cbx += "&nbsp;" + HU.labeledCheckbox("wikify",  "true",false,"Wikify description");	    
	    formBuffer.append(HU.colspan(cbx, 2));
	}
	super.addToEntryForm(request, formBuffer, parentEntry,entry, formInfo);
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
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
	throws Exception {
	
	if(!isNew(newType)) return;
        //If the file for the entry does not exist then return
        if ( !entry.isFile()) {
            return;
        }

	boolean addProperties = request.get(ARG_METADATA_ADD,false) ||
	    request.get("fromharvester",false);
	boolean addAddresses = request.get("addaddresses",false);
	if(!addAddresses) addProperties=false;
        System.setProperty("mail.mime.address.strict", "false");

        //Extract out the mail message metadata
        MimeMessage message = new MimeMessage(this.session,
					      getStorageManager().getFileInputStream(
										     entry.getFile().toString()));

	String subject = clean(message.getSubject());
	if (subject == null) {
	    subject = "";
	}
        //If the user did not specify a name in the entry form then use the mail subject
        if (!stringDefined(entry.getDescription()) || entryHasDefaultName(entry)) {
            entry.setName(subject);
        }
        String       from     = clean(InternetAddress.toString(message.getFrom()));
	HashSet<String> seen =  new HashSet<String>();
	seen.add(from);
	if(addProperties) {
	    addMetadata(request,entry,from);
	}
	StringBuilder       toSB = new StringBuilder();
	Address[]addresses= message.getAllRecipients();
	if(addresses!=null) {
	    for(Address address:addresses) {
		//            toSB.append(InternetAddress.toString(address));
		String s = clean(address.toString());
		if(seen.contains(s)) continue;
		seen.add(s);
		if(addAddresses) {
		    toSB.append(s);
		    toSB.append("\n");
		}
		if(addProperties) {
		    addMetadata(request,entry,s);
		}
	    }
	}

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

	LinkedHashMap<String, String> fileMap =  new LinkedHashMap<String,String>();
        //Do more mail stuff
	boolean wikify = request.get("wikify",false);
	boolean processHtml = request.get("processhtml",false);
        processContent(request, entry, content, desc,fileMap,processHtml);

        //Now get the values (this would be the fromaddress and toaddress from types.xml
        Object[] values = getEntryValues(entry);
        values[IDX_SUBJECT] =subject;
	if(addAddresses)
	    values[IDX_FROM] = from;
	String to = Utils.clip(toSB.toString(),19900,"");
        values[IDX_TO] = toSB.toString();

        //Set the description from the mail message
        if (entry.getDescription().length() == 0) {
            String description = desc.toString();
	    if(processHtml) {
		for (String key : fileMap.keySet()) {
		    String file = fileMap.get(key);
		    description=description.replace(key,file);
		    String pattern = "<img src=\"(" + file+")\"([^>]+)>";
		    description = description.replaceAll(pattern,"\n{{image src=\"::$1\" $2}}\n");
		    
		}
		description = description.replace("<div><br></div>","<br>");
		description=description.replace("<br>","\n<br>\n");
		description=description.replace("</div>","\n</div>\n");		
		description=description.replaceAll("<div([^>]*)>","\n<div $1>\n");
		description = description.replaceAll("\n\n+","\n");
		//		System.out.println(description);
	    }
	    if(wikify) {
		description = "<wiki>\n+section title={{name}}\n" + description +"\n-section\n";
	    }

            if (description.length() > Entry.MAX_DESCRIPTION_LENGTH) {
                description = description.substring(0,
                        Entry.MAX_DESCRIPTION_LENGTH - 1);
            }
	    //            entry.setDescription(clean2(description));
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
                                StringBuffer desc,LinkedHashMap<String, String> fileMap, boolean processHtml)
            throws Exception {
        if (content instanceof MimeMultipart) {
            MimeMultipart multipart = (MimeMultipart) content;
            for (int i = 0; i < multipart.getCount(); i++) {
                MimeBodyPart part = (MimeBodyPart) multipart.getBodyPart(i);
                String       disposition = part.getDisposition();
                if (disposition == null) {
                    Object partContent = part.getContent();
                    if (partContent instanceof MimeMultipart) {
                        processContent(request, entry, partContent, desc,fileMap,processHtml);
                    } else {
                        String contentType =
                            part.getContentType().toLowerCase();
                        //Only ingest the text
			boolean ok = false;
			if(processHtml) {
			    ok = contentType.indexOf("text/html")>=0;
			} else {
			    ok = contentType.indexOf("text/plain")>=0;
			}
                        if (ok) {
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
			String filename = part.getFileName();
			String id = part.getContentID();
			if(id!=null) {
			    id = id.replace("<","").replace(">","");
			    String ext = IOUtil.getFileExtension(filename);
			    if(ext==null) ext="";
			    filename = id+ ext;
			    fileMap.put("cid:" +id,filename);			    
			}
                        File f = getStorageManager().getTmpFile(request,filename);

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
                                getMetadataManager().findType(ContentMetadataHandler.TYPE_ATTACHMENT),
                                false, fileName, null, null, null, null);
                        getMetadataManager().addMetadata(request,entry, metadata);
                    }
                }
            }
        } else if (content instanceof Part) {
            //TODO
            Part part = (Part) content;
        } else {
            String contents = content.toString();
            desc.append(contents);
            desc.append("\n");
        }
    }

    private String mailLink(String mail) {
	String url = getRepository().getUrlBase() + "/search/do?metadata_type_email_address=email_address&metadata_attr1_email_address=" + mail;
	String label = HU.getIconImage("fas fa-search","style","font-size:8pt") + "&nbsp;" + mail;
	label = HU.span(label,HU.style("white-space:nowrap;"));
	String link =  HU.href(url,label,HU.title("Search")+HU.style("text-decoration:none;"));
	link = HU.div(link,HU.attrs("class","ramadda-clickable-span","style","display:inline-block;border: 1px solid #ccc;margin-bottom:3px;padding: 1px;padding-right: 1px;padding-left: 1px;border-radius: 10px;padding-left: 8px;padding-right: 8px;"));
	return link;

    }


    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
        if ( !tag.startsWith("mail_")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }

        StringBuilder sb = new StringBuilder();
        if (tag.equals("mail_header")) {
	    sb.append(HtmlUtils.formTable());
	    String subject = clean2(entry.getStringValue(request,IDX_SUBJECT, ""));
	    String from = entry.getStringValue(request,IDX_FROM, "");
	    sb.append(HtmlUtils.formEntry(msgLabel("Subject"), subject));
	    sb.append(HtmlUtils.formEntry(msgLabel("From"), mailLink(from)));
	    StringBuilder to = new StringBuilder();
	    int toCnt = 0;
	    for(String tom:Utils.split(entry.getStringValue(request,IDX_TO, ""),"\n",true,true)) {
		to.append(mailLink(tom));
		to.append(" ");
		toCnt++;
	    }
	    String toHtml = to.toString();
	    if(toCnt>4) {
		toHtml = "+enlarge style=\"border:0px;border-bottom:var(--basic-border);\" height=\"100px\" \n" + toHtml +"\n-enlarge";
		toHtml = getRepository().getWikiManager().wikify(request, toHtml);
	    }
	    sb.append(HtmlUtils.formEntry(msgLabel("To"),toHtml));
	    sb.append(HtmlUtils.formEntry(msgLabel("Date"),
					  getDateHandler().formatDate(request,
								      new Date(entry.getStartDate()),
								      (String) null)));


	    sb.append(HtmlUtils.formTableClose());
	    return sb.toString();
	}

	if(tag.equals("mail_body")) {
	    String desc = clean2(entry.getDescription());
	    sb.append(HU.pre(desc,HU.style("white-space:pre-wrap;max-height:400px;overflow-y:auto;")));
	    return sb.toString();
	}
	if(tag.equals("mail_attachments")) {
	    StringBuffer attachmentsSB = new StringBuffer();
	    getMetadataManager().decorateEntry(request, entry, attachmentsSB,
					       false);
	    if (attachmentsSB.length() > 0) {
		sb.append(HtmlUtils.makeShowHideBlock(msg("Attachments"),
						      "<div class=\"description\">" + attachmentsSB + "</div>",
						      false));
	    }
	}
	    

        return sb.toString();
    }

}
