/**
Copyright (c) 2008-2024 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.monitor;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import java.io.File;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;

import java.util.List;



@SuppressWarnings("unchecked")
public class EntryMonitor implements Constants {

    public static final HtmlUtils HU = null;


    /** _more_ */
    public static final LogManager.LogId LOGID =
        new LogManager.LogId("org.ramadda.repository.monitor.Monitor");


    /** _more_ */
    public static final String ARG_CLEARERROR = "monitor_clearerror";

    /** _more_ */
    private String lastError;

    /** _more_ */
    private String id;

    /** _more_ */
    private String name = "";

    /** _more_ */
    private Repository repository;


    /** _more_ */
    private String userId;

    /** _more_ */
    private User user;

    /** _more_ */
    private Request request;

    /** _more_ */
    private boolean enabled = true;

    /** _more_ */
    private boolean onlyNew = true;

    /** _more_ */
    private List<Filter> filters = new ArrayList<Filter>();

    /** _more_ */
    private List<MonitorAction> actions = new ArrayList<MonitorAction>();


    /** _more_ */
    private Date fromDate;

    /** _more_ */
    private Date toDate;

    /** _more_ */
    private boolean editable = true;



    /** _more_ */
    public static final String ARG_ADD_ACTION = "addaction";

    /** _more_ */
    public static final String ARG_DELETE_ACTION = "deleteaction";

    /** _more_ */
    public static final String ARG_DELETE_ACTION_CONFIRM =
        "deleteactionconfirm";




    /**
     * _more_
     */
    public EntryMonitor() {}


    /**
     * _more_
     *
     * @param repository _more_
     * @param user _more_
     * @param name _more_
     * @param editable _more_
     */
    public EntryMonitor(Repository repository, User user, String name,
                        boolean editable) {
        this.repository = repository;
        this.name       = name;
        this.editable   = editable;
        this.user       = user;
        if (user != null) {
            this.userId = user.getId();
        }
        this.id  = repository.getGUID();
        fromDate = new Date();
        toDate = new Date(fromDate.getTime()
                          + (long) DateUtil.daysToMillis(365*5));
    }





    /**
     * _more_
     *
     * @param monitors _more_
     * @param id _more_
     *
     * @return _more_
     */
    public static EntryMonitor findMonitor(List<EntryMonitor> monitors,
                                           String id) {
        for (EntryMonitor monitor : monitors) {
            if (Misc.equals(monitor.getId(), id)) {
                return monitor;
            }
        }

        return null;
    }





    /**
     * _more_
     *
     * @return _more_
     */
    public String getSearchSummary() {
        StringBuffer sb = new StringBuffer();
        if (filters.size() == 0) {
            return "None";
        }
        for (int i = 0; i < filters.size(); i++) {
            if (i > 0) {
                sb.append(" AND<br>");
            }
            sb.append(getSearchSummary(filters.get(i)));
        }

        return sb.toString();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getActionSummary() {
        StringBuffer sb = new StringBuffer();
        if (actions.size() == 0) {
            return "None";
        }
        for (int i = 0; i < actions.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(actions.get(i).getSummary(this));
        }

        return sb.toString();
    }

    public void logInfo(MonitorAction action, String message) {
        getRepository().getLogManager().logInfo(LOGID, getName() + ": "  + message);
    }

    public void logError(MonitorAction action, String message) {
	logError(action, message,null);
    }
    public void logError(MonitorAction action, String message,Exception exc) {	
        getRepository().getLogManager().logError(LOGID, getName() + ": "  + message,exc);
    }
    

    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    public void applyEditForm(Request request) throws Exception {

        if (request.get(ARG_CLEARERROR, false)) {
            lastError = "";
        }

        setName(request.getString(MonitorManager.ARG_MONITOR_NAME,
                                  getName()));
        setEnabled(request.get(MonitorManager.ARG_MONITOR_ENABLED, false));
        setOnlyNew(request.get(MonitorManager.ARG_MONITOR_ONLYNEW, false));
        Date[] dateRange =
            request.getDateRange(MonitorManager.ARG_MONITOR_FROMDATE,
                                 MonitorManager.ARG_MONITOR_TODATE,
                                 new Date());
        fromDate = dateRange[0];
        toDate   = dateRange[1];



        for (MonitorAction action : actions) {
            action.applyEditForm(request, this);
        }
        filters = new ArrayList();
        for (int i = 0; i < Filter.FIELD_TYPES.length; i++) {
            applyEditFilterField(request, Filter.FIELD_TYPES[i]);
        }

    }

    public MonitorAction getAction() {
	return actions.size()>0?actions.get(0):null;
    }


    public boolean isLive() {
	MonitorAction action = getAction();
	if(action==null) return false;
	return action.isLive(this);
    }


    public void checkLiveAction() throws Throwable {
	MonitorAction action = getAction();
	action.checkLiveAction(this);
    }
    

    public void addToEditForm(Request request, Appendable sb)
            throws Exception {
	MonitorAction theAction = actions.size()>0?actions.get(0):null;


        StringBuffer stateSB = new StringBuffer();

        stateSB.append(HU.formTable());
        stateSB.append(
            HU.formEntry(
                getRepository().msgLabel("Name"),
                HU.input(
                    MonitorManager.ARG_MONITOR_NAME, getName(),
                    HU.SIZE_70)));
	HU.formEntry(stateSB,
			    "",
			    HU.labeledCheckbox(
						      MonitorManager.ARG_MONITOR_ENABLED, "true",
						      getEnabled(),"Enabled"));

	boolean doSearch = theAction== null || theAction.doSearch();
	if(doSearch) {
	    HU.formEntry(stateSB,"",
				HU.labeledCheckbox(
							  MonitorManager.ARG_MONITOR_ONLYNEW, "true",
							  getOnlyNew(),
							  "Only check new entries"));
	}

        stateSB.append(
            HU.formEntry(
                getRepository().msgLabel("Valid Date Range"),
                getRepository().getDateHandler().makeDateInput(
                    request, MonitorManager.ARG_MONITOR_FROMDATE,
                    "monitorform", getFromDate()) + " "
                        + getRepository().msg("To") + " "
                        + getRepository().getDateHandler().makeDateInput(
                            request, MonitorManager.ARG_MONITOR_TODATE,
                            "monitorform", getToDate())));

        stateSB.append(HU.formTableClose());


        StringBuffer searchSB = new StringBuffer();
        addSearchToEditForm(request, searchSB);

        StringBuffer actionsSB = new StringBuffer();
        for (MonitorAction action : actions) {
            action.addToEditForm(request,this, actionsSB);
        }

        sb.append(HU.makeShowHideBlock("Settings", stateSB.toString(),
                true));

        if ((getLastError() != null) && (getLastError().length() > 0)) {
            StringBuffer errorSB = new StringBuffer();
            errorSB.append(HU.labeledCheckbox(ARG_CLEARERROR, "true", true,
						    "Clear error"));
            errorSB.append(getRepository().getPageHandler().showDialogError(getLastError()));
            sb.append(
                HU.makeShowHideBlock(
                    HU.span(
                        getRepository().msg("Error"),
                        HU.cssClass(
                            "errorlabel")), errorSB.toString(), true));
        }


	if(doSearch) {
	    sb.append(HU.makeShowHideBlock("Search Criteria",
						  searchSB.toString(), false));
	    sb.append(HU.makeShowHideBlock("Actions",
						  actionsSB.toString(), false));

	} else {
	    sb.append(actionsSB.toString());
	}



        sb.append(HU.p());

    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
	MonitorAction action = getAction();
	if(action!=null && !action.doSearch())
	    return "Monitor: " + name + " action:" + action;
        return "Monitor" + name + " filters:" + filters;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void addSearchToEditForm(Request request, StringBuffer sb)
            throws Exception {
        sb.append(HU.formTable());
        Hashtable<String, Filter> filterMap = new Hashtable<String, Filter>();
        for (Filter filter : filters) {
            filterMap.put(filter.getField(), filter);
        }

        for (int i = 0; i < Filter.FIELD_TYPES.length; i++) {
            addFilterField(Filter.FIELD_TYPES[i], filterMap, sb);
        }


        sb.append(HU.formTableClose());

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param what _more_
     *
     * @throws Exception _more_
     */
    private void applyEditFilterField(Request request, String what)
            throws Exception {
        boolean doNot = request.get(what + "_not", false);
        if (what.equals(ARG_AREA)) {
            double[] bbox = new double[] {
                                request.get(ARG_AREA + "_south",
                                            Entry.NONGEO),
                                request.get(ARG_AREA + "_north",
                                            Entry.NONGEO),
                                request.get(ARG_AREA + "_east", Entry.NONGEO),
                                request.get(ARG_AREA + "_west",
                                            Entry.NONGEO) };

            if ((bbox[0] != Entry.NONGEO) || (bbox[1] != Entry.NONGEO)
                    || (bbox[2] != Entry.NONGEO)
                    || (bbox[3] != Entry.NONGEO)) {
                addFilter(new Filter(what, bbox, doNot));
            }

            return;
        }



        if (what.equals(ARG_ANCESTOR)) {
            String ancestorId = request.getString(ARG_ANCESTOR + "_hidden",
                                    "");
            if (Utils.stringDefined(ancestorId)) {
                addFilter(new Filter(what, ancestorId, doNot));
            }

            return;
        }


        if ( !request.defined(what)) {
            return;
        }
        if (what.equals(ARG_FILESUFFIX)) {
            List<String> suffixes = Utils.split(request.getString(what, ""),
                                        ",", true, true);
            addFilter(new Filter(what, suffixes, doNot));
        } else if (what.equals(ARG_TEXT)) {
            addFilter(new Filter(what, request.getString(what, "").trim(),
                                 doNot));
        } else if (what.equals(ARG_USER)) {
            List<String> users = Utils.split(request.getString(what, ""),
                                             ",", true, true);
            addFilter(new Filter(what, users, doNot));
        } else if (what.equals(ARG_TYPE)) {
            List types = request.get(ARG_TYPE, new ArrayList());
            addFilter(new Filter(what, types, doNot));
        }
    }


    /**
     * _more_
     *
     * @param pattern _more_
     * @param s2 _more_
     *
     * @return _more_
     */
    public boolean nameMatch(String pattern, String s2) {
        boolean debug = false;
        if (StringUtil.containsRegExp(pattern)) {
            if (debug) {
                System.err.println("EntryMonitor string:" + s2 + " pattern:"
                                   + pattern + " matches:"
                                   + s2.matches(pattern));
            }

            return s2.matches(pattern);
        }

        String s1 = pattern;
        //TODO: We need to have a StringMatcher object
        if (s1.endsWith("%")) {
            s1 = s1.substring(0, s1.length() - 1);
            return s2.startsWith(s1);
        }
        if (s1.startsWith("%")) {
            s1 = s1.substring(1);
            return s2.endsWith(s1);
        }
        return s2.equals(s1);
    }



    /**
     * _more_
     *
     * @param what _more_
     * @param filterMap _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    private void addFilterField(String what,
                                Hashtable<String, Filter> filterMap,
                                StringBuffer sb)
            throws Exception {
        Filter  filter = filterMap.get(what);
        boolean doNot  = ((filter == null)
                          ? false
                          : filter.getDoNot());
        String notCbx = " " +HU.labeledCheckbox(what + "_not", "true", doNot, "Not");

        if (what.equals(ARG_FILESUFFIX)) {
            List<String> suffixes = ((filter == null)
                                     ? (List) new ArrayList()
                                     : (List) filter.getValue());
            sb.append(
                HU.formEntry(
                    getRepository().msgLabel("File Suffix"),
                    HU.input(
                        what, StringUtil.join(",", suffixes),
                        " size=\"60\" ") + notCbx));
        } else if (what.equals(ARG_TEXT)) {
            sb.append(HU.formEntry(getRepository().msgLabel("Text"),
                                          HU.input(what,
                                              ((filter == null)
                    ? ""
                    : filter.getValue()
                        .toString()), " size=\"60\" ") + notCbx));
        } else if (what.equals(ARG_USER)) {
            List<String> users = ((filter == null)
                                  ? (List) new ArrayList()
                                  : (List) filter.getValue());
            sb.append(HU.formEntry(getRepository().msgLabel("Users"),
                                          HU.input(what,
                                              StringUtil.join(",", users),
                                                  " size=\"60\" ") + notCbx));
        } else if (what.equals(ARG_ANCESTOR)) {
            String id = (String) ((filter == null)
                                  ? ""
                                  : filter.getValue());
            Entry group =
                (Entry) getRepository().getEntryManager().getEntry(getRequest(), id);
            getRepository().getPageHandler().addEntrySelect(getRequest(),
                    group, ARG_ANCESTOR, sb, "Ancestor Folder", " " + notCbx);
        } else if (what.equals(ARG_AREA)) {
            double[] values = ((filter == null)
                               ? new double[] { Entry.NONGEO, Entry.NONGEO,
                    Entry.NONGEO, Entry.NONGEO }
                               : (double[]) filter.getValue());
            String latLonForm = HU.makeLatLonBox(ARG_AREA, ARG_AREA,
                                    (values[0] != Entry.NONGEO)
                                    ? values[0]
                                    : Double.NaN, (values[1] != Entry.NONGEO)
                    ? values[1]
                    : Double.NaN, (values[2] != Entry.NONGEO)
                                  ? values[2]
                                  : Double.NaN, (values[3] != Entry.NONGEO)
                    ? values[3]
                    : Double.NaN);

            sb.append(HU.formEntry(getRepository().msgLabel("Area"),
                                          latLonForm));
        } else if (what.equals(ARG_TYPE)) {
            List<TypeHandler> typeHandlers =
                getRepository().getTypeHandlers();
            List         tmp   = new ArrayList();
            List<String> types = (List<String>) ((filter == null)
                    ? new ArrayList()
                    : filter.getValue());
            for (TypeHandler typeHandler : typeHandlers) {
                if (typeHandler.getType().equals(TYPE_ANY)) {
                    continue;
                }
                tmp.add(new TwoFacedObject(typeHandler.getLabel(),
                                           typeHandler.getType()));
            }
            String typeSelect = HU.select(ARG_TYPE, tmp, types,
                                    " MULTIPLE SIZE=4 ");
            sb.append(HU.formEntry(getRepository().msgLabel("Type"),
                                          typeSelect + notCbx));
        }
    }



    /**
     * _more_
     *
     * @param filter _more_
     *
     * @return _more_
     */
    private Entry getGroup(Filter filter) {
        try {
            Entry group = (Entry) filter.getProperty("ancestor");
            if (group != null) {
                return group;
            }
            group = (Entry) getRepository().getEntryManager().getEntry(getRequest(),
                    (String) filter.getValue());
            if (group != null) {
                filter.putProperty("ancestor", group);
            }

            return group;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }



    /**
     * _more_
     *
     * @param filter _more_
     *
     * @return _more_
     */
    private String getSearchSummary(Filter filter) {
        String desc  = "";
        String value = null;
        String what  = filter.getField();
        if (what.equals(ARG_FILESUFFIX)) {
            desc = "file suffix";
        } else if (what.equals(ARG_TEXT)) {
            desc = "name/description";
        } else if (what.equals(ARG_USER)) {
            desc = "user";
        } else if (what.equals(ARG_AREA)) {
            desc = "area";
        } else if (what.equals(ARG_ANCESTOR)) {
            desc = "ancestor";
            Entry group = getGroup(filter);
            value = ((group == null)
                     ? "_undefined_"
                     : group.getFullName());
        } else if (what.equals(ARG_TYPE)) {
            desc = "type";
        } else {
            desc = "Unknown";
        }

        if (value == null) {
            if (filter.getValue() instanceof List) {
                value = HU.quote(StringUtil.join("\" OR \"",
                        (List) filter.getValue()));
            } else {
                value = HU.quote(filter.getValue().toString());
            }
        }

        return HU.italics(desc) + " " + (filter.getDoNot()
                ? "!"
                : "") + "= (" + value + ")";
    }

    /**
     * _more_
     *
     * @param dummy _more_
     */
    public void setRepository(String dummy) {}


    /**
     * _more_
     *
     * @param repository _more_
     */
    protected void setRepository(Repository repository) {
        this.repository = repository;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Repository getRepository() {
        return repository;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getId() {
        return id;
    }

    /**
     * _more_
     *
     * @param id _more_
     */
    public void setId(String id) {
        this.id = id;
    }


    /**
     * _more_
     *
     * @param message _more_
     * @param exc _more_
     */
    public void handleError(String message, Exception exc) {
        lastError = message + "<br>" + exc + "<br>" + ((exc != null)
                ? LogUtil.getStackTrace(exc)
                : "");
        logError(null, message, exc);
    }


    /**
     * _more_
     *
     * @param filter _more_
     */
    public void addFilter(Filter filter) {
        synchronized (filters) {
            filters.add(filter);
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isActive() {
        if ( !getEnabled()) {
            return false;
        }
        //Must have at least one date
        if ((fromDate == null) && (toDate == null)) {
            return false;
        }
        Date now = new Date();

        if (fromDate != null) {
            if (now.getTime() < fromDate.getTime()) {
                return false;
            }
        }

        if (toDate != null) {
            if (now.getTime() > toDate.getTime()) {
                return false;
            }
        }

        return true;
    }



    public boolean checkEntry(Entry entry, boolean isNew) throws Exception {
        boolean debug = false;
        if (debug) {
            System.err.println("EntryMonitor.checkEntry:" + name + " entry:"
                               + entry);
        }
        if ( !isActive()) {
            if (debug) {
                System.err.println("\tnot active");
            }

            return false;
        }

        if (filters.size() == 0) {
            if (debug) {
                System.err.println("\tno filters");
            }

            return false;
        }

        //        System.err.println(getName() + " checking entry:" + entry.getName());

        if ( !okToView(entry)) {
            if (debug) {
                System.err.println("\t!ok to view");
            }

            //            System.err.println("can't view");
            return false;
        }



        for (Filter filter : filters) {
            boolean ok = checkEntry(filter, entry, isNew);
            if (debug) {
                System.err.println("\tChecking " + ok + " filter=" + filter);
            }
            if ( !ok) {
                if (debug) {
                    System.err.println("\tfilter not OK");
                }

                return false;
            }
        }

        if (debug) {
            System.err.println("\tentry matched");
        }
        entryMatched(entry, isNew);

        return true;
    }


    /**
     * _more_
     *
     *
     * @param filter _more_
     * @param entry _more_
     * @param isNew _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean checkEntry(Filter filter, Entry entry, boolean isNew)
            throws Exception {
        boolean ok    = false;
        String  field = filter.getField();
        Object  value = filter.getValue();
        boolean doNot = filter.getDoNot();
        if (field.equals(ARG_TYPE)) {
            List<String> types = (List<String>) value;
            ok = types.contains(entry.getTypeHandler().getType());
        } else if (field.equals(ARG_NAME)) {
            ok = nameMatch(value.toString(), entry.getName());
        } else if (field.equals(ARG_DESCRIPTION)) {
            ok = nameMatch(value.toString(), entry.getDescription());
        } else if (field.equals(ARG_FILESUFFIX)) {
            List<String> suffixes = (List<String>) value;
            String       path     = entry.getResource().getPath();
	    //	    System.err.println("suffixes:" + suffixes +" path:" + path);
            if (path != null) {
                for (String suffix : suffixes) {
                    if (IO.hasSuffix(path, suffix)) {
                        ok = true;
                        break;
                    }
                }
            }
        } else if (field.equals(ARG_TEXT)) {
            ok = nameMatch(value.toString(), entry.getDescription())
                 || nameMatch(value.toString(), entry.getName());
            if ( !ok) {
                ok = nameMatch(
                    value.toString(),
                    getRepository().getStorageManager().getOriginalFilename(
                        entry.getResource().toString()));
            }

        } else if (field.equals(ARG_ANCESTOR)) {
            Entry ancestor = getGroup(filter);
            if (ancestor == null) {
                return true;
            }
            if (ancestor != null) {
                Entry parent = entry.getParentEntry();
                while (parent != null) {
                    if (ancestor.equals(parent)) {
                        ok = true;

                        break;
                    }
                    parent = parent.getParentEntry();
                }
            }
        } else if (field.equals(ARG_AREA)) {
            //            System.err.println ("got area filter");
            double[] bbox    = (double[]) filter.getValue();
            boolean  okSouth = true,
                     okNorth = true,
                     okEast  = true,
                     okWest  = true;
            if (bbox[0] != Entry.NONGEO) {
                okSouth = entry.hasSouth() && (entry.getSouth(request) >= bbox[0]);
            }
            if (bbox[1] != Entry.NONGEO) {
                okNorth = entry.hasNorth() && (entry.getNorth(request) <= bbox[1]);
            }
            if (bbox[2] != Entry.NONGEO) {
                okEast = entry.hasEast() && (entry.getEast(request) <= bbox[2]);
            }
            if (bbox[3] != Entry.NONGEO) {
                okWest = entry.hasWest() && (entry.getWest(request) >= bbox[3]);
            }
            //            System.err.println (okWest +" " + okEast +" " +  okNorth +" " + okSouth);
            ok = okWest && okEast && okNorth && okSouth;
        } else if (field.equals(ARG_USER)) {
            List<String> users = (List<String>) value;
            ok = users.contains(entry.getUser().getId());
        } else if (field.equals(ARG_WAIT)) {
            ok = true;
        } else {
            int match = entry.getTypeHandler().matchValue(field, value,
                            entry);
            if (match == TypeHandler.MATCH_FALSE) {
                ok = false;
            } else if (match == TypeHandler.MATCH_TRUE) {
                ok = true;
            } else {
                System.err.println("unknown field:" + field);

                return true;
            }
        }
        if (doNot) {
            return !ok;
        }

        return ok;
    }



    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Request getRequest() throws Exception {
        if (request == null) {
	    request =  getRepository().getAdminRequest();
        }

        return request;
    }

    /**
     * _more_
     *
     * @param group _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean okToAddNew(Entry group) throws Exception {
        if (group == null) {
            return false;
        }

        return getRepository().getAccessManager().canDoNew(getRequest(),
                group);
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
    public boolean okToView(Entry entry) throws Exception {
        if (entry == null) {
            return false;
        }

        return getRepository().getAccessManager().canDoView(getRequest(),
                entry);
    }




    /**
     * _more_
     *
     * @param entry _more_
     * @param isNew _more_
     */
    protected void entryMatched(final Entry entry, final boolean isNew) {
        Misc.run(new Runnable() {
            public void run() {
                try {
                    entryMatchedInner(entry, isNew);
                } catch (Exception exc) {
                    handleError("Error handle entry matched", exc);
                }
            }
        });
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param isNew _more_
     */
    protected void entryMatchedInner(Entry entry, boolean isNew) {
        System.err.println(getName() + " matched entry: " + entry);
        for (MonitorAction action : actions) {
            action.entryMatched(this, entry, isNew);
        }
    }


    /**
     * _more_
     *
     * @param action _more_
     */
    public void addAction(MonitorAction action) {
        actions.add(action);
    }

    /**
     * Set the Filters property.
     *
     * @param value The new value for Filters
     */
    public void setFilters(List<Filter> value) {
        filters = value;
    }

    /**
     * Get the Filters property.
     *
     * @return The Filters
     */
    public List<Filter> getFilters() {
        return filters;
    }



    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public User getUser() throws Exception {
        if (user == null) {
            if (repository != null) {
                user = repository.getUserManager().findUser(userId, true);
            }
        }

        return user;
    }


    /**
     *  Set the UserId property.
     *
     *  @param value The new value for UserId
     */
    public void setUserId(String value) {
        userId = value;
    }

    /**
     *  Get the UserId property.
     *
     *  @return The UserId
     */
    public String getUserId() {
        return userId;
    }


    /**
     *  Set the Enabled property.
     *
     *  @param value The new value for Enabled
     */
    public void setEnabled(boolean value) {
        enabled = value;
    }

    /**
     *  Get the Enabled property.
     *
     *  @return The Enabled
     */
    public boolean getEnabled() {
        return enabled;
    }



    /**
     * Set the OnlyNew property.
     *
     * @param value The new value for OnlyNew
     */
    public void setOnlyNew(boolean value) {
        onlyNew = value;
    }

    /**
     * Get the OnlyNew property.
     *
     * @return The OnlyNew
     */
    public boolean getOnlyNew() {
        return onlyNew;
    }

    /**
     *  Set the FromDate property.
     *
     *  @param value The new value for FromDate
     */
    public void setFromDate(Date value) {
        fromDate = value;
    }

    /**
     *  Get the FromDate property.
     *
     *  @return The FromDate
     */
    public Date getFromDate() {
        return fromDate;
    }

    /**
     *  Set the ToDate property.
     *
     *  @param value The new value for ToDate
     */
    public void setToDate(Date value) {
        toDate = value;
    }

    /**
     *  Get the ToDate property.
     *
     *  @return The ToDate
     */
    public Date getToDate() {
        return toDate;
    }

    /**
     *  Set the Actions property.
     *
     *  @param value The new value for Actions
     */
    public void setActions(List<MonitorAction> value) {
        actions = value;
    }

    /**
     *  Get the Actions property.
     *
     *  @return The Actions
     */
    public List<MonitorAction> getActions() {
        return actions;
    }


    /**
     *  Set the Name property.
     *
     *  @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     *  Get the Name property.
     *
     *  @return The Name
     */
    public String getName() {
        return name;
    }


    /**
     *  Set the Editable property.
     *
     *  @param value The new value for Editable
     */
    public void setEditable(boolean value) {
        editable = value;
    }

    /**
     *  Get the Editable property.
     *
     *  @return The Editable
     */
    public boolean getEditable() {
        return editable;
    }




    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    public boolean equals(Object o) {
        if ( !(o instanceof EntryMonitor)) {
            return false;
        }
        EntryMonitor that = (EntryMonitor) o;

        return this.id.equals(that.id);
    }


    /**
     *  Set the LastError property.
     *
     *  @param value The new value for LastError
     */
    public void setLastError(String value) {
        this.lastError = value;
    }

    /**
     *  Get the LastError property.
     *
     *  @return The LastError
     */
    public String getLastError() {
        return this.lastError;
    }


}
