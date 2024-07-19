/**
   Copyright (c) 2008-2023 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.monitor;
import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.data.services.RecordTypeHandler;
import org.ramadda.data.services.PointOutputHandler;
import org.ramadda.data.services.PointEntry;

import org.ramadda.data.record.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import ucar.unidata.util.StringUtil;


import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import   org.mozilla.javascript.*;

public class DataAction extends MonitorAction {

    private  String script="";
    private String entryIds="";
    private String emails="";
    private String phoneNumbers="";    
    private String emailTemplate="A data monitor action has occurred for entry: ${entryname}. View entry at ${entryurl}";
    private String smsTemplate="A data monitor action has occurred for entry: ${entryname}. View entry at ${entryurl}";    

    private double windowHours = 0;
    private Hashtable<String,Long> lastMessageSent = new Hashtable<String,Long>();
    

    /**
     * _more_
     */
    public DataAction() {}

    /**
     * _more_
     *
     * @param id _more_
     */
    public DataAction(String id) {
        super(id);
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public String getActionLabel() {
        return "Data Action";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getActionName() {
        return "data";
    }


    /**
     * _more_
     *
     *
     * @param entryMonitor _more_
     * @return _more_
     */
    public String getSummary(EntryMonitor entryMonitor) {
        return "Monitor data";
    }

    @Override
    public boolean doSearch() {
	return false;
    }

    @Override
    public boolean isLive(EntryMonitor monitor) {
	return true;
    }    

    private EntryMonitor monitor;
    
    @Override
    public void checkLiveAction(EntryMonitor monitor) throws Throwable {
	Request request = monitor.getRepository().getAdminRequest();
	for(String id:Utils.split(entryIds,"\n",true,true)) {
	    if(id.startsWith("#")) continue;
	    Entry entry = monitor.getRepository().getEntryManager().getEntry(request, id);
	    if(entry==null) {
		monitor.getRepository().getLogManager().logMonitor(monitor +" could not find entry: " + id);
		continue;
	    }
	    checkLiveAction(request, monitor,entry);
	}
    }

    public void checkLiveAction(Request request, EntryMonitor monitor,Entry entry) throws Throwable {
	this.monitor = monitor;
	if(!(entry.getTypeHandler() instanceof RecordTypeHandler)) {
	    logMessage(monitor +": skipping non point data entry :" + entry.getName());
	    return;
	}

	RecordTypeHandler typeHandler = (RecordTypeHandler) entry.getTypeHandler();
	PointOutputHandler poh = (PointOutputHandler) typeHandler.getRecordOutputHandler();
        List<PointEntry> pointEntries = new ArrayList<PointEntry>();
        pointEntries.add((PointEntry) poh.doMakeEntry(request, entry));
	VisitInfo visitInfo = new VisitInfo(VisitInfo.QUICKSCAN_NO);
	visitInfo.setLast(1);
	final BaseRecord[]arecord={null};
	RecordVisitor visitor = new RecordVisitor() {
		public boolean visitRecord(RecordFile file, VisitInfo visitInfo,
					   BaseRecord r) {
		    arecord[0] = r;
		    return true;
		}
	    };

	poh.getRecordJobManager().visitSequential(request, pointEntries,
						  visitor, visitInfo);

	if(arecord[0]==null) {
	    logMessage(monitor +": No data found");
	    return;
	}


	Context ctx =Context.enter();
	Scriptable scope =  ctx.initSafeStandardObjects();
	Script script = ctx.compileString(this.script, "code", 0, null);
	scope.put("action", scope, this);
	scope.put("entry", scope, entry);
	scope.put("record", scope, arecord[0]);	
	script.exec(ctx, scope);
    }
    

    public void print(String message)     {
	System.err.println(message);
    }


    public void logMessage(String message)     {
	monitor.getRepository().getLogManager().logMonitor(message);
    }
    
    public boolean canTrigger(Entry entry) throws Throwable {
	Date now = new Date();
	Long lastTime = lastMessageSent.get(entry.getId());
	if(lastTime!=null) {
	    if(windowHours<=0) {
		logMessage(monitor +": skipping trigger. no window defined");
		return false;
	    }
	    double  since = Utils.millisToHours(now.getTime()-lastTime);
	    if(since<windowHours) {
		now  = new Date(now.getTime()+Utils.hoursToMillis(since));
		logMessage(monitor +": skipping trigger. Next  trigger at:" + now);
		return false;
	    }
	}
	return true;
    }
	


    public void clear(Entry entry) throws Throwable {
	lastMessageSent.remove(entry.getId());
	//Save this off so the last time gets saved
	monitor.getRepository().getMonitorManager().updateMonitor(monitor);
    }

    public void trigger(Entry entry,String message,boolean...what)  throws Throwable {
	if(!canTrigger(entry)) {
	    return;
	}
	boolean sent = false;
	if(what.length==0 || what[0]) {
	    triggerEmail(entry,message);
	    sent = true;
	}
	if(what.length==0 || (what.length>1&&what[2])) {
	    triggerSms(entry,message);
	    sent = true;
	}
	if(sent) {
	    Date now = new Date();
	    lastMessageSent.put(entry.getId(),now.getTime());
	    //Save this off so the last time gets saved
	    monitor.getRepository().getMonitorManager().updateMonitor(monitor);
	} else {
	    logMessage(monitor +" No message sent for entry: " + entry +" message: " + message);
	}
    }

    public String applyTemplate(String template, Entry entry,String message)  {
	template = template.replace("${message}",message).replace("${entryname}",entry.getName()).replace("${entryid}",entry.getId());
	Request request = monitor.getRepository().getAdminRequest();
	String url = HU.url(monitor.getRepository().getEntryManager().getFullEntryShowUrl(request),ARG_ENTRYID,entry.getId());
	template = template.replace("${entryurl}",url);
	return template;
    }

    public void triggerEmail(Entry entry,String message)  throws Throwable {
	if(!monitor.getRepository().getMailManager().isEmailEnabled())  {
	    logMessage(monitor +" unable to send email for entry:" + entry +" message:" + message);
	    return;
	}
	String contents = applyTemplate(emailTemplate,  entry, message);
	logMessage(monitor +" sent email message for entry:" + entry +" message:" + message);
	monitor.getRepository().getMailManager().sendEmail(Utils.split(emails,"\n",true,true),monitor.getName(),contents,false);
    }


    public void triggerSms(Entry entry,String message)  throws Throwable {
	if(!monitor.getRepository().getMailManager().isSmsEnabled())  {
	    logMessage(monitor +" unable to send SMS for entry:" + entry +" message:" + message);
	    return;
	}
	String template=smsTemplate;
	if(!Utils.stringDefined(template)) template = emailTemplate;
	String contents = applyTemplate(template,  entry, message);
	for(String phone: Utils.split(phoneNumbers,"\n",true,true)) {
	    if(phone.startsWith("#")) continue;
	    monitor.getRepository().getMailManager().sendTextMessage(null,phone,message);
	    logMessage(monitor +" sent SMS message for entry:" + entry +" to:"+ phone+ " message:" + message);
	}

    }
    

    @Override
    public void applyEditForm(Request request, EntryMonitor monitor) {
        super.applyEditForm(request, monitor);
	entryIds = request.getString(getArgId(ARG_ENTRYIDS),entryIds);
	script = request.getString(getArgId("script"),script);
	emails = request.getString(getArgId("emails"),emails);	
	phoneNumbers = request.getString(getArgId("phonenumbers"),phoneNumbers);
	emailTemplate = request.getString(getArgId("emailtemplate"),emailTemplate);
	smsTemplate = request.getString(getArgId("smstemplate"),smsTemplate);		
	windowHours = request.get(getArgId("windowhours"),windowHours);
	if(request.get(getArgId("clearhistory"),false)) {
	    lastMessageSent = new Hashtable<String,Long>();	
	}
	
    }



    @Override
    public void addToEditForm(Request request,EntryMonitor monitor, Appendable sb)
	throws Exception {
        sb.append(HU.formTable());
        sb.append(HU.colspan(HU.div("Data Monitor Information",HU.cssClass("formgroupheader")), 2));

	sb.append(HU.formEntry("",HU.labeledCheckbox(getArgId("clearhistory"),"true",false,"Clear History")));



	StringBuilder entriesInfo = new StringBuilder("One entry ID per line<br>");
	List<Entry> entries = new ArrayList<Entry>();
	for(String id:Utils.split(entryIds,"\n",true,true)) {
	    if(id.startsWith("#")) continue;
	    
	    Entry entry = monitor.getRepository().getEntryManager().getEntry(request, id);
	    if(entry==null) {
		entriesInfo.append(HU.b("No entry: " + id+"<br>"));
	    } else {
		if(!(entry.getTypeHandler() instanceof RecordTypeHandler)) {
		    entriesInfo.append(HU.b("Entry: " + entry.getName()+" is not a point data type<br>"));
		} else {
		    entriesInfo.append("Entry: " + entry.getName()+"<br>");

		}
		
	    }
	}

        sb.append(HU.colspan(HU.div("Select entries to monitor",HU.cssClass("ramadda-form-help")),3));
        sb.append(
		  HU.formEntryTop(
				  "Entry IDs:",
				  HU.textArea(getArgId(ARG_ENTRYIDS), entryIds, 5, 60), entriesInfo.toString()));

        sb.append(HU.colspan(HU.div("Specify Javascript to check data",HU.cssClass("ramadda-form-help")),3));
        sb.append(
		  HU.formEntryTop(
				  "Script:",
				  HU.textArea(
					      getArgId("script"), script, 5,
					      60)));	


	sb.append(HU.colspan(HU.div("Enter delay between message sends",HU.cssClass("ramadda-form-help")),3));
	sb.append(HU.formEntry("Message Delay:",HU.input(getArgId("windowhours"),""+windowHours,
							 " size=\"10\" ")+" hours. Enter 0 to only send 1 message until this action is cleared"));
	


	Repository repository= monitor.getRepository();
	if(repository.getMailManager().isEmailEnabled())  {
	    sb.append(HU.colspan(HU.div("Enter emails to send",HU.cssClass("ramadda-form-help")),3));
	    sb.append(HU.formEntryTop(
				      "Emails:",
				      HU.textArea(
						  getArgId("emails"), emails, 5,
						  60)));

	    sb.append(HU.formEntryTop(
				      "Email Message:",
				      HU.textArea(
						  getArgId("emailtemplate"), emailTemplate, 5,
						  60)));
	} else {
	    sb.append(HU.formEntry("","Email is not configured"));
	}


	if(repository.getMailManager().isSmsEnabled()) {
	    sb.append(HU.colspan(HU.div("Enter phone numbers to text to",HU.cssClass("ramadda-form-help")),3));
	    sb.append(
		      HU.formEntryTop(
				      "Phone Numbers:",
				      HU.textArea(
						  getArgId("phonenumbers"), phoneNumbers, 5,
						  60)));		


	    
	    sb.append(
		      HU.formEntryTop(
				      "Text Message:",
				      HU.textArea(
						  getArgId("smstemplate"), smsTemplate, 5,
						  60)));
	} else {
	    sb.append(HU.formEntry("","SMS is not configured"));
	}



        sb.append(HU.formTableClose());
    }


    /**
     * _more_
     *
     *
     * @param monitor _more_
     * @param entry _more_
     * @param isNew _more_
     */
    @Override
    public void entryMatched(EntryMonitor monitor, Entry entry,
                             boolean isNew) {
    }


    /**
       Set the Script property.
       @param value The new value for Script
    **/
    public void setScript (String value) {
	script = value;
    }

    /**
       Get the Script property.

       @return The Script
    **/
    public String getScript () {
	return script;
    }


    /**
       Set the EntryIds property.

       @param value The new value for EntryIds
    **/
    public void setEntryIds (String value) {
	entryIds = value;
    }

    /**
       Get the EntryIds property.

       @return The EntryIds
    **/
    public String getEntryIds () {
	return entryIds;
    }

    /**
       Set the Emails property.

       @param value The new value for Emails
    **/
    public void setEmails (String value) {
	emails = value;
    }

    /**
       Get the Emails property.

       @return The Emails
    **/
    public String getEmails () {
	return emails;
    }

    /**
       Set the PhoneNumbers property.

       @param value The new value for PhoneNumbers
    **/
    public void setPhoneNumbers (String value) {
	phoneNumbers = value;
    }

    /**
       Get the PhoneNumbers property.

       @return The PhoneNumbers
    **/
    public String getPhoneNumbers () {
	return phoneNumbers;
    }


    /**
       Set the EmailTemplate property.

       @param value The new value for EmailTemplate
    **/
    public void setEmailTemplate (String value) {
	emailTemplate = value;
    }

    /**
       Get the EmailTemplate property.

       @return The EmailTemplate
    **/
    public String getEmailTemplate () {
	return emailTemplate;
    }

    /**
       Set the SmsTemplate property.

       @param value The new value for SmsTemplate
    **/
    public void setSmsTemplate (String value) {
	smsTemplate = value;
    }

    /**
       Get the SmsTemplate property.

       @return The SmsTemplate
    **/
    public String getSmsTemplate () {
	return smsTemplate;
    }

    public String toString() {
	return "Data Monitor";
    }

    /**
       Set the LastMessageSent property.

       @param value The new value for LastMessageSent
    **/
    public void setLastMessageSent (Hashtable<String,Long> value) {
	lastMessageSent = value;
    }

    /**
       Get the LastMessageSent property.

       @return The LastMessageSent
    **/
    public Hashtable<String,Long> getLastMessageSent () {
	return lastMessageSent;
    }

    /**
       Set the WindowHours property.

       @param value The new value for WindowHours
    **/
    public void setWindowHours (double value) {
	windowHours = value;
    }

    /**
       Get the WindowHours property.

       @return The WindowHours
    **/
    public double getWindowHours () {
	return windowHours;
    }




}
