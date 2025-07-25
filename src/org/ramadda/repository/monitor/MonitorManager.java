/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.monitor;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.database.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.sql.Clause;
import org.ramadda.util.sql.SqlUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


@SuppressWarnings("unchecked")
public class MonitorManager extends RepositoryManager implements EntryChecker {
    public static final String ARG_MONITOR_CHANGE = "monitorchange";
    public static final String ARG_MONITOR_ONLYNEW = "onlynew";
    public static final String ARG_MONITOR_CREATE = "monitorcreate";
    public static final String ARG_MONITOR_DELETE = "monitordelete";
    public static final String ARG_MONITOR_DELETE_CONFIRM =
        "monitordeletefconfirm";
    public static final String ARG_MONITOR_ENABLED = "monitor_enabled";
    public static final String ARG_MONITOR_FROMDATE = "monitor_fromdate";
    public static final String ARG_MONITOR_ID = "monitorid";
    public static final String ARG_MONITOR_NAME = "monitor_name";
    public static final String ARG_MONITOR_TODATE = "monitor_todate";
    public static final String ARG_MONITOR_TYPE = "monitortype";

    private List<EntryMonitor> monitors = new ArrayList<EntryMonitor>();
    private List<MonitorAction> actions = new ArrayList<MonitorAction>();

    public MonitorManager(Repository repository) {
        super(repository);
        repository.addEntryChecker(this);
        try {
            initActions();
            initMonitors();
	    Misc.run(this, "monitorLive");
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    public void monitorLive() {
	long delaySeconds = repository.getProperty("ramadda.monitor.live.sleepseconds",60*5);
	Misc.sleepSeconds(10);
	while(true) {
	    checkLiveMonitors();
	    Misc.sleepSeconds(delaySeconds);
	}

    }

    private void  checkLiveMonitors() {
	for(EntryMonitor monitor: getEntryMonitors(true)) {
	    if(!monitor.isLive() || !monitor.isActive()) continue;
	    try {
		monitor.setLastError("");
		monitor.setCurrentStatus("");
		monitor.checkLiveAction();
	    } catch(Throwable exc) {
		Throwable thr = LogUtil.getInnerException(exc);
		monitor.setLastError("Error processing monitor:" +monitor +" Error: " + thr.getMessage());
		getLogManager().logMonitorError("Error processing monitor:" +monitor,exc);
	    }
	}
    }

    public void addClass(Class c) throws Exception {
        MonitorAction action = (MonitorAction) c.getDeclaredConstructor().newInstance();
        actions.add(action);
    }

    private void initActions() throws Exception {
        actions.add(new DataAction());
        actions.add(new EmailAction());
        actions.add(new CopyAction());
        actions.add(new PublishAction());
        //        actions.add(new FtpAction());
	//        actions.add(new ExecAction());
        for (Class c :
                getRepository().getPluginManager().getSpecialClasses()) {
            if (MonitorAction.class.isAssignableFrom(c)) {
                addClass(c);
            }
        }

    }

    private void initMonitors() throws Exception {
        Statement stmt =
            getDatabaseManager().select(Tables.MONITORS.COL_MONITOR_ID+","+
					Tables.MONITORS.COL_ENCODED_OBJECT,
                                        Tables.MONITORS.NAME, new Clause(),
                                        " order by "
                                        + Tables.MONITORS.COL_NAME);
        SqlUtil.Iterator iter = getDatabaseManager().getIterator(stmt);
        ResultSet        results;

        while ((results = iter.getNext()) != null) {
            String id = results.getString(1);
            String xml = results.getString(2);
            try {
                //Uggh
                xml = xml.replaceAll(
                    "org.ramadda.repository.monitor.LdmAction",
                    "org.ramadda.geodata.cdmdata.LdmAction");
                xml = xml.replaceAll(
                    "org.ramadda.repository.monitor.TwitterAction",
                    "org.ramadda.repository.monitor.NoopAction");
                xml = xml.replace("hipchat.HipchatAction",
                                  "atlassian.HipchatAction");
                EntryMonitor monitor =
                    (EntryMonitor) Repository.decodeObject(xml);
                if (monitor != null) {
                    monitor.setRepository(getRepository());
                    monitors.add(monitor);
                } else {
                    /*
                      System.err.println ("could not create monitor:" + xml);
                      System.err.println ("messages:" + xmlEncoder.getErrorMessages());
                      for(Exception exc: (List<Exception>)xmlEncoder.getExceptions()) {
                      exc.printStackTrace();
                      }
                    */
                }
            } catch (Throwable ignore) {
                System.err.println("No monitor class found: " + "id:" + id +" error:" +ignore);
            }
        }
    }

    public List<EntryMonitor> getEntryMonitors() {
	return getEntryMonitors(false);

    }

    public List<EntryMonitor> getEntryMonitors(boolean clone) {	
	if(clone) {
	    List<EntryMonitor> l = new ArrayList<EntryMonitor>(monitors);
	    return l;
	}
        return monitors;
    }

    public Result processEntryListen(Request request) throws Exception {
        SynchronousEntryMonitor entryMonitor =
            new SynchronousEntryMonitor(getRepository(), request);
        synchronized (monitors) {
            monitors.add(entryMonitor);
        }
        synchronized (entryMonitor) {
            entryMonitor.wait();
            System.err.println("Done waiting");
        }
        Entry entry = entryMonitor.getEntry();
        if (entry == null) {
            System.err.println("No entry");

            return new Result(BLANK, new StringBuffer("No match"),
                              getRepository().getMimeTypeFromSuffix(".html"));
        }

        return getRepository().getOutputHandler(request).outputEntry(request,
                request.getOutput(), entry);
    }

    public void entriesModified(Request request, final List<Entry> entries) {
        handleEntriesChanged(entries, false);
    }

    /**
     *
     * @param entries _more_
     */
    public void entriesMoved(final List<Entry> entries) {}

    public void entriesDeleted(List<String> entryIds) {}

    public void entriesCreated(Request request, final List<Entry> entries) {
        handleEntriesChanged(entries, true);
    }

    private void handleEntriesChanged(final List<Entry> entries,
                                      final boolean isNew) {
        Misc.run(new Runnable() {
            public void run() {
                handleEntriesChangedInner(entries, isNew);
            }
        });
    }

    private void handleEntriesChangedInner(List<Entry> entries,
                                           boolean isNew) {
	boolean debug = false;
	//	debug = true;
        try {
            List<EntryMonitor> tmpMonitors;
            synchronized (monitors) {
                tmpMonitors = new ArrayList<EntryMonitor>(monitors);
            }
            for (Entry entry : entries) {
		if(debug) System.err.println("MonitorManager check entry: " + entry);
                for (EntryMonitor entryMonitor : tmpMonitors) {
                    if ( !isNew) {
                        if (entryMonitor.getOnlyNew()) {
                            continue;
                        }
                    }
                    entryMonitor.checkEntry(entry, isNew);
                }
            }
        } catch (Exception exc) {
            System.err.println("Error checking monitors:" + exc);
            exc.printStackTrace();
        }
    }

    private void deleteMonitor(EntryMonitor monitor) throws Exception {
        monitors.remove(monitor);
        if ( !monitor.getEditable()) {
            return;
        }
        getDatabaseManager().delete(Tables.MONITORS.NAME,
                                    Clause.eq(Tables.MONITORS.COL_MONITOR_ID,
                                        monitor.getId()));

    }

    private void insertMonitor(EntryMonitor monitor) throws Exception {
        String xml = Repository.encodeObject(monitor);
        getDatabaseManager().executeInsert(Tables.MONITORS.INSERT,
                                           new Object[] {
            monitor.getId(), monitor.getName(), monitor.getUser().getId(),
            monitor.getFromDate(), monitor.getToDate(), xml
        });
    }

    private void addNewMonitor(EntryMonitor monitor) throws Exception {
        monitors.add(monitor);
        if ( !monitor.getEditable()) {
            return;
        }
        insertMonitor(monitor);
    }

    public void updateMonitor(EntryMonitor monitor) throws Exception {
        if ( !monitor.getEditable()) {
            return;
        }
        getDatabaseManager().delete(Tables.MONITORS.NAME,
                                    Clause.eq(Tables.MONITORS.COL_MONITOR_ID,
                                        monitor.getId()));

        insertMonitor(monitor);
    }

    public Result processMonitorEdit(Request request, EntryMonitor monitor)
            throws Exception {

        if (request.exists(ARG_MONITOR_DELETE_CONFIRM)) {
            getAuthManager().ensureAuthToken(request);
            deleteMonitor(monitor);

            return new Result(
                request.makeUrl(
                    getAdmin().URL_ADMIN_MONITORS, ARG_MESSAGE,
                    "Monitor deleted"));
        }

        if (request.exists(ARG_MONITOR_CHANGE)) {
            getAuthManager().ensureAuthToken(request);
            monitor.applyEditForm(request);
            updateMonitor(monitor);

            return new Result(
                HU.url(
			      request.makeUrl(getAdmin().URL_ADMIN_MONITORS),
                    ARG_MONITOR_ID, monitor.getId()));
        }

        StringBuilder sb = new StringBuilder();
        sb.append(HU.sectionOpen(null, false));
        sb.append(HU.h2(msgLabel("Monitor") + " "
                               + monitor.getName()));
        request.formPostWithAuthToken(sb,
                                      getAdmin().URL_ADMIN_MONITORS,
                                      HU.attr(HU.ATTR_NAME,
                                          "monitorform"));
        sb.append(HU.hidden(ARG_MONITOR_ID, monitor.getId()));

        if (request.exists(ARG_MONITOR_DELETE)) {
            StringBuffer fb = new StringBuffer();
            fb.append(
                HU.buttons(
                    HU.submit("OK", ARG_MONITOR_DELETE_CONFIRM),
                    HU.submit(LABEL_CANCEL, ARG_CANCEL)));
            sb.append(
                getPageHandler().showDialogQuestion(
                    msg("Are you sure you want to delete the monitor?"),
                    fb.toString()));
            sb.append(HU.formClose());

            sb.append(HU.sectionClose());

            return getAdmin().makeResult(request,
					 msg("Monitor Delete"), sb);
        }

        StringBuffer buttons = new StringBuffer();
        buttons.append(HU.submit("Save", ARG_MONITOR_CHANGE));
        buttons.append(HU.space(2));
        buttons.append(HU.submit("Delete", ARG_MONITOR_DELETE));
	monitor.addButtons(request,buttons);
        sb.append(buttons);

        sb.append(HU.br());
        monitor.addToEditForm(request, sb);
        sb.append(buttons);

        sb.append(HU.formClose());

        sb.append(HU.sectionClose());

        return getAdmin().makeResult(request,
                                           msg("Edit Entry Monitor"), sb);
    }

    public Result processMonitorCreate(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        EntryMonitor monitor = new EntryMonitor(getRepository(),
                                   request.getUser(), "New Monitor", true);
        String        type   = request.getString(ARG_MONITOR_TYPE, "email");
        MonitorAction action = null;
        for (MonitorAction templateAction : actions) {
            if ( !templateAction.enabled(getRepository())) {
                continue;
            }
            if (templateAction.getActionName().equals(type)) {
                action = templateAction.cloneMe();
                action.setId(getRepository().getGUID());

                break;
            }
        }

        if (action == null) {
            throw new IllegalArgumentException("unknown action type:" + type);
        }

        if (action.adminOnly() && !request.getUser().getAdmin()) {
            throw new IllegalArgumentException(
                "You need to be an admin to add an " + type + " action");
        }

        monitor.addAction(action);
        addNewMonitor(monitor);

        return new Result(
            HU.url(
			  request.makeUrl(getAdmin().URL_ADMIN_MONITORS),
                ARG_MONITOR_ID, monitor.getId()));
    }

    public boolean canView(Request request, EntryMonitor monitor)
            throws Exception {
        if (request.getUser().getAdmin()) {
            return true;
        }

        return Misc.equals(monitor.getUser(), request.getUser());
    }

    public List<EntryMonitor> getEditableMonitors(Request request,
            List<EntryMonitor> monitors)
            throws Exception {
        List<EntryMonitor> result = new ArrayList<EntryMonitor>();
        for (EntryMonitor monitor : monitors) {
            if (monitor.getEditable() && canView(request, monitor)) {
                result.add(monitor);
            }
        }

        return result;
    }

    public Result processMonitors(Request request) throws Exception {

        if (request.getUser().getAnonymous()
                || request.getUser().getIsGuest()) {
            throw new IllegalArgumentException("Cannot access monitors");
        }
        StringBuilder sb = new StringBuilder();
        List<EntryMonitor> monitors = getEditableMonitors(request,
                                          getEntryMonitors());
        if (request.exists(ARG_MONITOR_ID)) {
            EntryMonitor monitor = EntryMonitor.findMonitor(monitors,
                                       request.getString(ARG_MONITOR_ID, ""));
            if (monitor == null) {
                throw new IllegalArgumentException(
                    "Could not find entry monitor");
            }
            if ( !monitor.getEditable()) {
                throw new IllegalArgumentException(
                    "Entry monitor is not editable");
            }
            if ( !canView(request, monitor)) {
                throw new IllegalArgumentException(
                    "You are not allowed to edit thr monitor");
            }

            return processMonitorEdit(request, monitor);
        }

        if (request.exists(ARG_MONITOR_CREATE)) {
            return processMonitorCreate(request);
        }

        sb.append(HU.sectionOpen(null, false));

        sb.append(HU.b("New Monitor:"));
        sb.append(HU.space(2));

        sb.append(HU.open(HU.TAG_TABLE));
        sb.append(HU.open(HU.TAG_TR));
        for (MonitorAction templateAction : actions) {
            if ( !templateAction.enabled(getRepository())) {
                continue;
            }
            if (templateAction.adminOnly() && !request.getUser().getAdmin()) {
                continue;
            }

            sb.append(HU.open(HU.TAG_TD));
            sb.append(HU.open(HU.TAG_DIV,
                                     HU.style("padding-left:10px;")));
            sb.append(request.form(getAdmin().URL_ADMIN_MONITORS));
            sb.append(HU.submit(templateAction.getActionLabel(),
                                       ARG_MONITOR_CREATE));
            sb.append(HU.hidden(ARG_MONITOR_TYPE,
                                       templateAction.getActionName()));
            sb.append(HU.formClose());
            sb.append(HU.close(HU.TAG_DIV));
            sb.append(HU.close(HU.TAG_TD));
        }

        sb.append(HU.close(HU.TAG_TR));
        sb.append(HU.close(HU.TAG_TABLE));

        sb.append(HU.br());

        sb.append(HU.open(HU.TAG_TABLE,
                                 HU.attrs(HU.ATTR_CELLPADDING,
                                     "4", HU.ATTR_CELLSPACING, "0")));
        if (monitors.size() > 0) {
            sb.append(HU.row(HU.cols("",
				     boldMsg("Monitor"),
				     boldMsg("Status"),
				     boldMsg("Action"),
				     boldMsg("Search Criteria"))));
        }
        for (EntryMonitor monitor : monitors) {
            sb.append(HU.open(HU.TAG_TR,
			      HU.attr(HU.ATTR_VALIGN,  "top")));
            sb.append(HU.open(HU.TAG_TD, HU.cssClass("ramadda-td")));
            sb.append(HU.button(HU.href(HU.url(request.makeUrl(getAdmin().URL_ADMIN_MONITORS),
					       ARG_MONITOR_ID, monitor.getId()), 
					"Edit",HU.title("Edit monitor"))));
            sb.append(HU.space(1));
            sb.append(HU.button(HU.href(HU.url(
					       request.makeUrl(getAdmin().URL_ADMIN_MONITORS),
					       ARG_MONITOR_DELETE, "true", ARG_MONITOR_ID,
					       monitor.getId()),  "Delete",HU.title("Delete monitor"))));
            sb.append(HU.close(HU.TAG_TD));
            sb.append(HU.col(monitor.getName(), HU.cssClass("ramadda-td")));
	    sb.append(HU.col(monitor.isActive()?"active":"inactive", HU.cssClass("ramadda-td")+
			     HU.style("text-align","center","background",!monitor.isActive()?"#efefef":"lightgreen")));
            sb.append(HU.col(monitor.getActionSummary(), HU.cssClass("ramadda-td")));
            sb.append(HU.col(monitor.getSearchSummary(), HU.cssClass("ramadda-td")));
            sb.append(HU.close(HU.TAG_TR));

	    monitor.addStatusLine(request,sb);
            if (stringDefined(monitor.getLastError())) {
                String msg = getPageHandler().showDialogError(monitor.getLastError());
                sb.append(HU.row(HU.colspan(HU.insetDiv(msg, 0,40,0,0),8)));
            }
            if (stringDefined(monitor.getCurrentStatus())) {
                String msg = getPageHandler().showDialogNote(monitor.getCurrentStatus());
                sb.append(HU.row(HU.colspan(msg, 5)));
            }	    

	    //Add the line
            sb.append(HU.open(HU.TAG_TR, HU.attrs("style","border-bottom:1px solid #000;")));
	    sb.append(HU.row(HU.colspan("", 5)));
            sb.append(HU.close(HU.TAG_TR));
        }
        sb.append(HU.close(HU.TAG_TABLE));

        sb.append(HU.sectionClose());

        return getAdmin().makeResult(request, "RAMADDA-Admin-Monitors", sb);
    }

}
