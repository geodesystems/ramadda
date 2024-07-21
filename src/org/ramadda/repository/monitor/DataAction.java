/**
   Copyright (c) 2008-2023 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.monitor;
import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.data.services.RecordTypeHandler;
import org.ramadda.data.services.PointOutputHandler;
import org.ramadda.data.services.PointEntry;

import org.ramadda.data.record.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.StringUtil;


import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import   org.mozilla.javascript.*;

public class DataAction extends MonitorAction {

    private boolean testMode = false;
    private Request currentRequest;

    private  String script="";
    private String entryIds="";

    private String execPath="";
    private String execTemplate="A data monitor action has occurred for entry: ${entryname}. View entry at ${entryurl}. Message: ${message}";    


    private String emails="";
    private String phoneNumbers="";    
    private String emailTemplate="A data monitor action has occurred for entry: ${entryname}. View entry at ${entryurl}. Message: ${message}";
    private String smsTemplate="";





    private double windowHours = 6;
    private LinkedHashMap<String,Long> lastMessageSent = new LinkedHashMap<String,Long>();
    

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
	checkLiveAction(request,monitor,false);
    }

    public void checkLiveAction(Request request,EntryMonitor monitor, boolean test) throws Throwable {	
	for(String id:Utils.split(entryIds,"\n",true,true)) {
	    if(id.startsWith("#")) continue;
	    Entry entry = monitor.getRepository().getEntryManager().getEntry(request, id);
	    if(entry==null) {
		monitor.getRepository().getLogManager().logMonitor(monitor +" could not find entry: " + id);
		continue;
	    }
	    checkLiveAction(request, monitor,entry,test);
	}
    }


    public void checkLiveAction(Request request, EntryMonitor monitor,Entry entry,boolean test) throws Throwable {
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


	currentRequest=request;
	testMode = test;

	try {
	    Context ctx =Context.enter();
	    Scriptable scope =  ctx.initSafeStandardObjects();
	    Script script = ctx.compileString(this.script, "code", 0, null);
	    scope.put("testMode", scope, test);
	    scope.put("action", scope, this);
	    scope.put("entry", scope, entry);
	    scope.put("record", scope, arecord[0]);	
            ctx.evaluateString(scope, this.script, "script", 1, null);
	    //	    script.exec(ctx, scope);
	} finally {
	    currentRequest=null;
	    testMode = false;
	}
    }
    

    public double getHoursSince(Object object)     {
	Date date;
	if(object instanceof BaseRecord) {
	    date = ((BaseRecord)object).getDate();
	} else {
	    date = (Date) object;
	}
	Date now=new Date();
	long diff = now.getTime()-date.getTime();
	return Utils.millisToHours(diff);
    }


    public void print(Object message)     {
	System.err.println(message.toString());
	testModeMessage(message.toString());
    }


    public void logMessage(Object message)     {
	monitor.getRepository().getLogManager().logMonitor(message.toString());
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

    public void testModeMessage(String msg) {
	if(testMode && currentRequest!=null) {
	    monitor.getRepository().getSessionManager().addSessionErrorMessage(currentRequest,
									       msg);
	}
    }
    public void trigger(Entry entry,String message,Object...what)  throws Throwable {
	if(!testMode && !canTrigger(entry)) {
	    return;
	}
	HashSet call = Utils.makeHashSet(what);
	testModeMessage("Data monitor triggered for:" + entry.getName());

	int sent = 0;


	if(what.length==0 || call.contains("program")) {
	    sent+=triggerExec(entry,message);
	}
	if(what.length==0 || call.contains("email")) {
	    sent+=triggerEmail(entry,message);
	}
	if(what.length==0 || call.contains("sms")) {
	    sent +=triggerSms(entry,message);
	}
	if(sent>0) {
	    Date now = new Date();
	    lastMessageSent.put(entry.getId(),now.getTime());
	    //Save this off so the last time gets saved
	    monitor.getRepository().getMonitorManager().updateMonitor(monitor);
	} else {
	    logMessage(monitor +" No message sent for entry: " + entry +" message: " + message);
	}
    }


    public int triggerExec(Entry entry,String message) throws Throwable {
	Repository repository = monitor.getRepository();
	if(!Utils.stringDefined(execPath)) {
	    return 0;
	}
	repository.addScriptPath(execPath);
	List<String>commands = new ArrayList<String>();
	commands.add(execPath);
	commands.add(entry.getId());	    
	Request request = repository.getAdminRequest();
	commands.add(HU.url(repository.getEntryManager().getFullEntryShowUrl(request),ARG_ENTRYID,entry.getId()));
	commands.add(applyTemplate(execTemplate,entry,message));
	String[]results = repository.runCommands(commands);
	if(Utils.stringDefined(results[0])) {
	    logMessage(monitor +" error calling program:" + execPath + " with entry:" + entry.getName() +"<br>Error:" + results[0]);
	    throw new RuntimeException("Error calling program:" + execPath + " with entry:" + entry.getName() +"<br>Error:" + results[0]);
	}
	testModeMessage("Program called:"+ execPath + " on entry:" + entry.getName());
	logMessage("Program called:"+ execPath + " on entry:" + entry.getName());
	if(Utils.stringDefined(results[1])) {
	    testModeMessage("Program returned:" + results[1]);
	}
	return 1;
    }




    public String applyTemplate(String template, Entry entry,String message)  {
	template = template.replace("${message}",message).replace("${entryname}",entry.getName()).replace("${entryid}",entry.getId());
	Request request = monitor.getRepository().getAdminRequest();
	String url = HU.url(monitor.getRepository().getEntryManager().getFullEntryShowUrl(request),ARG_ENTRYID,entry.getId());
	template = template.replace("${entryurl}",url);
	return template;
    }


    public int triggerEmail(Entry entry,String message)  throws Throwable {
	if(!Utils.stringDefined(emails)) {
	    return 0;
	}
	if(!monitor.getRepository().getMailManager().isEmailEnabled())  {
	    testModeMessage("Email not enabled");
	    logMessage(monitor +" unable to send email for entry:" + entry +" message:" + message);
	    return 0;
	}
	String contents = applyTemplate(emailTemplate,  entry, message);
	int cnt = monitor.getRepository().getMailManager().sendEmail(Utils.split(emails,"\n",true,true),monitor.getName(),contents,false);
	if(cnt==0) {
	    testModeMessage("No emails sent");
	    logMessage(monitor +" no emails sent");
	} else {
	    testModeMessage(cnt +" email messages sent");
	    logMessage(monitor +" sent " + cnt +" email messages for entry:" + entry +" message:" + message);
	}
	return cnt;
    }


    public int triggerSms(Entry entry,String message)  throws Throwable {
	if(!Utils.stringDefined(phoneNumbers)) {
	    return 0;
	}
	if(!monitor.getRepository().getMailManager().isSmsEnabled())  {
	    testModeMessage("SMS not enabled");
	    logMessage(monitor +" unable to send SMS for entry:" + entry +" message:" + message);
	    return 0;
	}
	String template=smsTemplate;
	if(!Utils.stringDefined(template)) template = emailTemplate;
	String contents = applyTemplate(template,  entry, message);
	int cnt=0;
	for(String phone: Utils.split(phoneNumbers,"\n",true,true)) {
	    if(phone.startsWith("#")) continue;
	    monitor.getRepository().getMailManager().sendTextMessage(null,phone,message);
	    logMessage(monitor +" sent SMS message for entry:" + entry +" to:"+ phone+ " message:" + message);
	    cnt++;
	}
	if(cnt==0) {
	    testModeMessage("No SMS messages sent");
	} else {
	    testModeMessage(cnt+" SMS messages sent");
	}
	return cnt;


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
	execPath = request.getString(getArgId("execpath"),execPath);
	execTemplate = request.getString(getArgId("exectemplate"),execTemplate);			
	if(request.get(getArgId("clearhistory"),false)) {
	    lastMessageSent = new LinkedHashMap<String,Long>();	
	}

	if(request.get(getArgId("test"),false)) {
	    try {
		checkLiveAction(request, monitor,true);
	    } catch(Throwable thr) {
		thr = LogUtil.getInnerException(thr);
		monitor.setLastError("Error running test: " + thr);
		monitor.getRepository().getSessionManager().addSessionErrorMessage(request,
										   "Error running test: " + thr);
		
	    }
	}


	
    }


    @Override
    public void addStatusLine(Request request, EntryMonitor monitor,Appendable sb) throws Exception {
	StringBuilder info = new StringBuilder();
	addEntryInfo(request,  monitor,info);
	if(info.length()>0) {
	    sb.append(HU.row(HU.colspan(HU.insetDiv(info.toString(), 0,40,0,0),8)));
	}
    }

    @Override
    public void addButtons(Request request, EntryMonitor monitor,Appendable sb) throws Exception {
	String cbx = 
	    HU.labeledCheckbox(getArgId("test"),"true",false,"Test Data Monitor");		
	sb.append(HU.space(3));
	sb.append(cbx);

    }

    @Override
    public void addToEditForm(Request request,EntryMonitor monitor, Appendable sb)
	throws Exception {

        sb.append(HU.div("Data Monitor Information",HU.cssClass("formgroupheader")));

        String helpLink =
            HU.href(monitor.getRepository().getUrlBase()
                           + "/userguide/datamonitor.html", "Help",
		    HU.attrs("class","ramadda-button", HU.ATTR_TARGET, "_help"));



	String top="";
	top+=helpLink;
	top+=HU.space(2);
	//	top+=cbx;
	sb.append(HU.div(top,"style='margin-top:5px;'"));

        sb.append(HU.formTable());
	StringBuilder entriesInfo = new StringBuilder();
	String clearCbx = HU.labeledCheckbox(getArgId("clearhistory"),"true",false,"Clear History");
	entriesInfo.append(clearCbx);
	entriesInfo.append("<br>One entry ID per line<br>");
	addEntryInfo(request,monitor,entriesInfo);
	sb.append(HU.colspan(HU.div("Enter delay between message sends",HU.cssClass("ramadda-form-help")),3));
	sb.append(HU.formEntry("Message Delay:",HU.input(getArgId("windowhours"),""+windowHours,
							 " size=\"10\" ")+" hours. Enter 0 to only send 1 message until this action is cleared"));


        sb.append(HU.colspan(HU.div("Select entries to monitor",HU.cssClass("ramadda-form-help")),3));
	String textAreaId = HU.getUniqueId("input_");
	HU.importJS(sb, monitor.getRepository().getPageHandler().getCdnPath("/wiki.js"));
	String buttons = OutputHandler.getSelect(request, textAreaId,
						 HU.span("Add entry id",HU.cssClass("ramadda-button")), true, "entryid", null,
						 false,false);


        sb.append(
		  HU.formEntryTop(
				  "Entry IDs:",
				  buttons+"<br>"+
				  HU.textArea(getArgId(ARG_ENTRYIDS), entryIds, 5, 60,HU.attrs("id",textAreaId)),
				  entriesInfo.toString()));

	List help = Utils.makeListFromValues("Javascript:","//log or print a message",HU.italics("action.logMessage('message');"),
					     HU.italics("action.print(record.getFields())"),
					     "//Access the field value",
					     HU.italics("record.getValue('field_name')"),
					     "//access hours between the current time and the time of the record",
					     HU.italics("action.getHoursSince(record.getDate())"),
					     "//trigger program, email or sms if enabled",
					     HU.italics("action.trigger(entry,'Some message');"),
					     "//just send email or sms",
					     HU.italics("action.triggger(entry,'Some message','email','sms');"),
					     "//just call program",
					     HU.italics("action.triggger(entry,'Some message','programl');"),
					     "e.g.:<pre>if(record.getValue('atmos_temp')>300) {\n\taction.trigger(entry,'Test trigger');\n}\n");
	String jsInfo = Utils.join(help,"<br>");
        sb.append(HU.colspan(HU.div("Specify Javascript to check data",HU.cssClass("ramadda-form-help")),3));
        sb.append(
		  HU.formEntryTop(
				  "Script:",
				  HU.textArea(
					      getArgId("script"), script, 15,
					      60),jsInfo.toString()));


	


	Repository repository= monitor.getRepository();

	sb.append(HU.colspan(HU.div("Path to program to execute. Will be called with arguments:<pre><i>program</i> &lt;entry id&gt; &lt;url to entry&gt; &lt;message&gt;",HU.cssClass("ramadda-form-help")),3));	
	sb.append(HU.formEntry("Program:",    HU.input(getArgId("execpath"), execPath, HU.SIZE_60)));
	sb.append(HU.formEntryTop(
				  "Message:",
				  HU.textArea(
						  getArgId("exectemplate"), execTemplate, 5,
						  60)));



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


    private void addEntryInfo(Request request, EntryMonitor monitor,StringBuilder entriesInfo) throws Exception {
	EntryManager em = monitor.getRepository().getEntryManager();
	for(String id:Utils.split(entryIds,"\n",true,true)) {
	    if(id.startsWith("#")) continue;
	    Entry entry = em.getEntry(request, id);
	    if(entry==null) {
		entriesInfo.append(HU.b("No entry: " + id+"<br>"));
	    } else {
		if(!(entry.getTypeHandler() instanceof RecordTypeHandler)) {
		    entriesInfo.append(HU.b("Entry: " + entry.getName()+" is not a point data type<br>"));
		} else {
		    String line = "Entry: " + entry.getName();
		    line = HU.href(em.getEntryUrl(request,entry),line,HU.attrs("target","entry"));
		    Long l = lastMessageSent.get(entry.getId());
		    if(l!=null) {
			Date date = new Date(l);
			line +="<br>Last trigger:" + date; 
			Date nextRun = new Date(date.getTime()+Utils.hoursToMillis(windowHours));
			line +="<br>Next trigger:" + nextRun;
		    }
		    entriesInfo.append(line+"<br>");
		}
		
	    }
	}
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
    public void setLastMessageSent (LinkedHashMap<String,Long> value) {
	lastMessageSent = value;
    }

    /**
       Get the LastMessageSent property.

       @return The LastMessageSent
    **/
    public LinkedHashMap<String,Long> getLastMessageSent () {
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

/**
Set the ExecPath property.

@param value The new value for ExecPath
**/
public void setExecPath (String value) {
	execPath = value;
}

/**
Get the ExecPath property.

@return The ExecPath
**/
public String getExecPath () {
	return execPath;
}

/**
Set the ExecTemplate property.

@param value The new value for ExecTemplate
**/
public void setExecTemplate (String value) {
	execTemplate = value;
}

/**
Get the ExecTemplate property.

@return The ExecTemplate
**/
public String getExecTemplate () {
	return execTemplate;
}





}
