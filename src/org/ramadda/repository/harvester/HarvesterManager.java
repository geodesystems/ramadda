/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.harvester;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.database.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.FileWrapper;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;
import org.ramadda.util.sql.Clause;
import org.ramadda.util.sql.SqlUtil;
import org.w3c.dom.*;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import org.ramadda.util.MyXmlUtil;
import java.io.File;
import java.lang.reflect.*;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

@SuppressWarnings("unchecked")
public class HarvesterManager extends RepositoryManager {
    public RequestUrl URL_HARVESTERS_LIST = new RequestUrl(this,
                                                "/harvester/list",
                                                "Harvesters");
    public RequestUrl URL_HARVESTERS_NEW = new RequestUrl(this,
                                               "/harvester/new");

    public RequestUrl URL_HARVESTERS_FORM = new RequestUrl(this,
                                                "/harvester/form");

    public RequestUrl URL_HARVESTERS_CHANGE = new RequestUrl(this,
                                                  "/harvester/change");

    private List<Harvester> harvesters = new ArrayList();
    private Hashtable harvesterMap = new Hashtable();
    List<TwoFacedObject> harvesterTypes = new ArrayList<TwoFacedObject>();

    private boolean haveInited= false;

    public HarvesterManager(Repository repository) {
        super(repository);
        addHarvesterType(PatternHarvester.class);
        addHarvesterType(WebHarvester.class);
        addHarvesterType(DirectoryHarvester.class);
        addHarvesterType(MonitorHarvester.class);
    }

    public void addHarvesterType(Class c) {
        try {
            Constructor ctor = Misc.findConstructor(c,
                                   new Class[] { Repository.class,
                    String.class });
            if (ctor == null) {
                throw new IllegalArgumentException(
                    "Could not find constructor for harvester:"
                    + c.getName());
            }
            Harvester dummy = (Harvester) ctor.newInstance(new Object[] {
                                  getRepository(),
                                  "" });
            harvesterTypes.add(new TwoFacedObject(dummy.getDescription(),
                    c.getName()));
        } catch (Exception exc) {
            logError("Error creating harvester: " + c.getName(), exc);
        }
    }

    protected Harvester findHarvester(String id) {
        if (id == null) {
            return null;
        }
        for (Harvester harvester : harvesters) {
            if (harvester.getId().equals(id)) {
                return harvester;
            }
        }

        return null;
    }

    public List<Harvester> getHarvesters() {
        return harvesters;
    }

    public List getHarvesters(Class c) {
        List<Harvester> harvesters = new ArrayList<Harvester>();
        for (Harvester harvester : getHarvesters()) {
            if (harvester.getClass().isAssignableFrom(c)) {
                if (harvester.getActiveOnStart()) {
                    harvesters.add(harvester);
                } else {}
            }
        }

        return harvesters;
    }

    /**
     * This starts up the harvesters in a thread
     *
     * @throws Exception On badness
     */
    public void initHarvesters() throws Exception {
        //If we are in read only mode then don't start the harvesters
        if (getRepository().isReadOnly()) {
            return;
        }
        Misc.run(this, "initHarvestersInThread");
    }

    public void initHarvestersInThread() throws Exception {
	//Sleep a bit before we start up
	Misc.sleepSeconds(5);
        List<String> harvesterFiles =
            getRepository().getResourcePaths(PROP_HARVESTERS);
        boolean okToStart =
            getRepository().getProperty(PROP_HARVESTERS_ACTIVE, true);

        harvesters = new ArrayList<Harvester>();

        SqlUtil.Iterator iter = getDatabaseManager().getIterator(
                                    getDatabaseManager().select(
                                        Tables.HARVESTERS.COLUMNS,
                                        Tables.HARVESTERS.NAME,
                                        new Clause()));;
        ResultSet results;
        while ((results = iter.getNext()) != null) {
            String id            = results.getString(1);
            String origClassName = results.getString(2);
            String className     = origClassName;
            String content       = results.getString(3);

            Class  c             = null;

            //Hack, hack... 
            //handle package changes 
            className = className.replace("ucar.unidata.repository",
                                          "org.ramadda.repository");
            className = className.replace("ucar/unidata/repository",
                                          "org/ramadda/repository");

            className = className.replace("org.ramadda.geodata.data",
                                          "org.ramadda.geodata.cdmdata");
            className = className.replace("org/ramadda/geodata/data",
                                          "org/ramadda/geodata/cdmdata");
            className = className.replace("plugins.hipchat",
                                          "plugins.atlassian");
            try {
                c = Misc.findClass(className);
            } catch (ClassNotFoundException cnfe1) {
                className = className.replace("repository.",
                        "repository.harvester.");
                try {
                    c = Misc.findClass(className);
                } catch (ClassNotFoundException cnfe2) {
                    getRepository().getLogManager().logError(
                        "HarvesterManager: Could not load harvester class: "
                        + origClassName);
                    System.err.println("BAD:" + className);

                    continue;
                }
            }
            Constructor ctor = Misc.findConstructor(c,
                                   new Class[] { Repository.class,
                    String.class });
            Harvester harvester = (Harvester) ctor.newInstance(new Object[] {
                                      getRepository(),
                                      id });

            harvester.initFromContent(content);
            harvesters.add(harvester);
            harvesterMap.put(harvester.getId(), harvester);
        }

        try {
            for (String file : harvesterFiles) {
                Element root = MyXmlUtil.getRoot(file, getClass());
                if (root == null) {
                    continue;
                }
                try {
                    List<Harvester> newHarvesters =
                        Harvester.createHarvesters(getRepository(), root);
                    harvesters.addAll(newHarvesters);
                    for (Harvester harvester : newHarvesters) {
                        harvesterMap.put(harvester.getId(), harvester);
                    }
                } catch (Exception exc) {
                    logError("Error loading harvester file:" + file, exc);
                }
            }
        } catch (Exception exc) {
            logError("Error loading harvester file", exc);
        }

        for (Harvester harvester : harvesters) {
            for (FileWrapper rootDir : harvester.getRootDirs()) {
                getStorageManager().addOkToReadFromDirectory(rootDir.getFile());
            }
            if ( !okToStart) {
                harvester.setActive(false);
            } else if (harvester.getActiveOnStart()) {
                Misc.run(harvester, "run");
            }
        }

	haveInited= true;	

    }

    public Result processNew(Request request) throws Exception {

        StringBuffer sb = new StringBuffer();
        if (request.exists(ARG_CANCEL)) {
            return new Result(request.makeUrl(URL_HARVESTERS_LIST));
        }

        getAuthManager().ensureAuthToken(request);
        List<Harvester> harvestersBeingCreated = new ArrayList<Harvester>();
        if (request.exists(ARG_HARVESTER_XMLFILE)) {
            String file = request.getUploadedFile(ARG_HARVESTER_XMLFILE);
            if ((file == null) || !new File(file).exists()) {
                return getAdmin().makeResult(
                    request, msg("New Harvester"),
                    new StringBuffer("You must specify a file"));
            }
            String xml = getStorageManager().readSystemResource(file);
            harvestersBeingCreated =
                Harvester.createHarvesters(getRepository(),
                                           MyXmlUtil.getRoot(xml));
            if (harvestersBeingCreated.size() == 0) {
                return getAdmin().makeResult(
                    request, msg("New Harvester"),
                    new StringBuffer("No harvesters defined"));
            }

            for (Harvester harvester : harvestersBeingCreated) {
                harvester.setId(getRepository().getGUID());
            }

        } else if (request.exists(ARG_NAME)) {
            String id = getRepository().getGUID();
            Class c = Misc.findClass(request.getString(ARG_HARVESTER_CLASS));
            Constructor ctor = Misc.findConstructor(c,
                                   new Class[] { Repository.class,
                    String.class });
            Harvester harvester = (Harvester) ctor.newInstance(new Object[] {
                                      getRepository(),
                                      id });
            harvester.setName(request.getString(ARG_NAME, ""));
            harvester.setUser(request.getUser());
            harvestersBeingCreated.add(harvester);
        }

        if (harvestersBeingCreated.size() > 0) {
            for (Harvester harvester : harvestersBeingCreated) {
                harvester.setIsEditable(true);
                harvesters.add(harvester);
                harvesterMap.put(harvester.getId(), harvester);

                getDatabaseManager().executeInsert(Tables.HARVESTERS.INSERT,
                        new Object[] { harvester.getId(),
                                       harvester.getClass().getName(),
                                       harvester.getContent() });
            }

            return new Result(request.makeUrl(URL_HARVESTERS_FORM,
                    ARG_HARVESTER_ID, harvestersBeingCreated.get(0).getId()));
        }

        sb.append(HU.sectionOpen(null, false));
        sb.append(HU.h2("Create new harvester"));
        request.formPostWithAuthToken(sb, URL_HARVESTERS_NEW);
        sb.append(HU.formTable());
        sb.append(HU.formEntry(msgLabel("Name"),
                                      HU.input(ARG_NAME, "",
                                          HU.SIZE_40)));
        String typeInput = HU.select(ARG_HARVESTER_CLASS,
                                            harvesterTypes);
        sb.append(HU.formEntry(msgLabel("Type"), typeInput));
        sb.append(HU.formEntry("",
                                      HU.submit("Create")
                                      + HU.space(1)
                                      + HU.submit(LABEL_CANCEL,
                                          ARG_CANCEL)));

        sb.append(HU.formClose());

        sb.append(HU.formEntry(HU.p(), ""));
        request.uploadFormWithAuthToken(sb, URL_HARVESTERS_NEW);

        sb.append(
            HU.row(
                HU.colspan(HU.b("Or upload xml file"), 2)));
        sb.append(
            HU.formEntry(
                msgLabel("File"),
                HU.fileInput(
                    ARG_HARVESTER_XMLFILE, HU.SIZE_70)));

        sb.append(HU.formEntry("",
                                      HU.submit("Upload")
                                      + HU.space(1)
                                      + HU.submit(LABEL_CANCEL,
                                          ARG_CANCEL)));
        sb.append(HU.formClose());
        sb.append(HU.formTableClose());

        sb.append(HU.sectionClose());

        return getAdmin().makeResult(request, msg("New Harvester"), sb);

    }

    public Result processForm(Request request) throws Exception {

        StringBuffer sb = new StringBuffer();
	if(!haveInited) {
	    sb.append(getPageHandler().showDialogNote("Harvesters have not been initialized yet"));
	    return getAdmin().makeResult(request, msg("RAMADDA-Admin-Harvesters"), sb);
	}

        Harvester harvester =
            findHarvester(request.getString(ARG_HARVESTER_ID));
        if (harvester == null) {
	    sb.append(getPageHandler().showDialogError("Could not find harvester:"+ request.getString(ARG_HARVESTER_ID)));
	    return getAdmin().makeResult(request, msg("RAMADDA-Admin-Harvesters"), sb);
        }

        if (request.get(ARG_HARVESTER_GETXML, false)) {
            String xml = harvester.getContent();
            xml = MyXmlUtil.tag(Harvester.TAG_HARVESTERS, "", xml);
            return new Result("",
                              new StringBuffer(MyXmlUtil.getHeader() + "\n"
                                  + xml), "text/xml");
        }

        if ( !harvester.getIsEditable()) {
	    sb.append(getPageHandler().showDialogError("Cannot edit  harvester:"+ request.getString(ARG_HARVESTER_ID)));
	    return getAdmin().makeResult(request, msg("RAMADDA-Admin-Harvesters"), sb);
        }
        if (request.exists(ARG_CANCEL)) {
            return new Result(request.makeUrl(URL_HARVESTERS_LIST));
        }

        makeFormHeader(request, harvester, sb);

	String space = HU.space(2);
        String xmlLink = HU.href(
                             HU.url(
                                 URL_HARVESTERS_FORM.toString()
                                 + "/harvester.xml", ARG_HARVESTER_GETXML,
                                     "true", ARG_HARVESTER_ID,
                                     harvester.getId()), msg("Download"));

        String buttons =
	    HU.submit("Save", ARG_CHANGE)
	    + space;

	buttons+=   HU.submit("Delete", ARG_DELETE)
	    + space
	    + HU.submit(LABEL_CANCEL, ARG_CANCEL);

	buttons+=HU.space(6);
	if(harvester.getActive()) {
	    buttons+=HU.submit("Save and stop",ARG_STOP);
	} else {
	    buttons+= HU.submit("Save and start",ARG_START);
	}
	


        sb.append(buttons);
	String helpLink = HU.href(getPageHandler().makeHtdocsUrl("/userguide/harvesters.html"),
				  msg("Help"), " target=_HELP style='font-weight:normal;'");

        sb.append(space);
        sb.append(helpLink);
        StringBuffer formSB = new StringBuffer();
        formSB.append(HU.formTable());
        harvester.createEditForm(request, formSB);
        formSB.append(HU.formTableClose());

        sb.append(formSB);
        sb.append(buttons);
        sb.append(space);
        sb.append(xmlLink);

        sb.append(HU.formClose());
        sb.append(HU.sectionClose());

        return getAdmin().makeResult(request, msg("Edit Harvester"), sb);
    }

    public Result processApiStatus(Request request) throws Exception {
        Harvester harvester =
            findHarvester(request.getString(ARG_HARVESTER_ID));
	StringBuilder sb = new StringBuilder();
	if(harvester==null) {
	    sb.append("error:missing harvester");
	} else {
	    if(harvester.getActive()) sb.append("active");
	    else if(!harvester.getActive()) sb.append("inactive");	    
	}
	return new Result(sb.toString(), MIME_TEXT);
    }

    private void startHarvester(Request request, Harvester harvester) throws Exception {
	getEntryManager().clearSeenResources();
	harvester.clearCache();
	Misc.run(harvester, "run");
    }

    public Result processApiStart(Request request) throws Exception {
        Harvester harvester =
            findHarvester(request.getString(ARG_HARVESTER_ID));
	StringBuilder sb = new StringBuilder();
	if(harvester==null) {
	    sb.append("error:missing harvester");
	} else {
	    if(harvester.getActive()) sb.append("error:active");
	    else {
		harvester.applyApiStart(request);
		startHarvester(request, harvester);
		sb.append("running");
	    }
	}
	return new Result(sb.toString(), MIME_TEXT);
    }
    

    public Result processChange(Request request) throws Exception {
        Harvester harvester =
            findHarvester(request.getString(ARG_HARVESTER_ID));
        if (harvester == null) {
            throw new IllegalArgumentException("Could not find harvester");
        }
        getAuthManager().ensureAuthToken(request);
        if ( !harvester.getIsEditable()) {
            throw new IllegalArgumentException("Cannot edit harvester");
        }
        if (request.exists(ARG_CANCEL)) {
            return new Result(request.makeUrl(URL_HARVESTERS_LIST));
        }

        if (request.exists(ARG_DELETE_CONFIRM)) {
            harvesterMap.remove(harvester.getId());
            harvesters.remove(harvester);
            getDatabaseManager().delete(Tables.HARVESTERS.NAME,
                                        Clause.eq(Tables.HARVESTERS.COL_ID,
                                            harvester.getId()));

            return new Result(request.makeUrl(URL_HARVESTERS_LIST));
        }

        if (request.exists(ARG_DELETE)) {
            StringBuffer sb = new StringBuffer();
            makeFormHeader(request, harvester, sb);
            sb.append(
                getPageHandler().showDialogQuestion(
                    msg("Are you sure you want to delete the harvester"),
                    HU.buttons(
                        HU.submit("Yes", ARG_DELETE_CONFIRM),
                        HU.submit(LABEL_CANCEL, ARG_CANCEL_DELETE))));
            sb.append(HU.formClose());
            sb.append(HU.sectionClose());

            return getAdmin().makeResult(request, msg("Edit Harvester"), sb);
        }
        if (request.exists(ARG_CHANGE) || request.exists(ARG_START)) {
            harvester.applyEditForm(request);
            getDatabaseManager().delete(Tables.HARVESTERS.NAME,
                                        Clause.eq(Tables.HARVESTERS.COL_ID,
                                            harvester.getId()));
            getDatabaseManager().executeInsert(Tables.HARVESTERS.INSERT,
                    new Object[] { harvester.getId(),
                                   harvester.getClass().getName(),
                                   harvester.getContent() });

        }

	if(request.exists(ARG_START)) {
	    return new Result(harvester.getStartUrl(request));
	}
	if(request.exists(ARG_STOP)) {
	    return new Result(harvester.getStopUrl(request));
	}
        return new Result(request.makeUrl(URL_HARVESTERS_FORM,
                                          ARG_HARVESTER_ID,
                                          harvester.getId()));
    }

    private void makeFormHeader(Request request, Harvester harvester,
                                StringBuffer sb) {
        sb.append(HU.sectionOpen(null, false));
	String header = msgLabel("Harvester") + " " + harvester.getName();
	if(harvester.getActive()) {
	    header +=" (Active)";
	}
        sb.append(HU.h2(header));
        request.formPostWithAuthToken(sb, URL_HARVESTERS_CHANGE);
        sb.append(HU.hidden(ARG_HARVESTER_ID, harvester.getId()));
    }

    public Result processList(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        if (request.defined(ARG_ACTION)) {
            boolean returnXml = request.getString(ARG_RESPONSE,
                                    "").equals(RESPONSE_XML);
            String action       = request.getString(ARG_ACTION);
            String msg          = "";
            String returnStatus = CODE_OK;
            Harvester harvester =
                findHarvester(request.getString(ARG_HARVESTER_ID));
            if (harvester == null) {
                returnStatus = CODE_ERROR;
                msg = "Could not find harvester:"
                      + request.getString(ARG_HARVESTER_ID);
            } else {
                if (action.equals(ACTION_STOP)) {
                    harvester.setActive(false);
                    msg = "Harvester stopped:" + harvester;
                } else if (action.equals(ACTION_REMOVE)) {
                    harvester.setActive(false);
                    harvesters.remove(harvester);
                } else if (action.equals(ACTION_START)) {
                    if ( !harvester.getActive()) {
			startHarvester(request, harvester);
                        msg = "Harvester started:" + harvester;
                    } else {
                        returnStatus = CODE_ERROR;
                        msg          = "Harvester is already running";
                    }
                }
            }

            if (returnXml) {
                return new Result(MyXmlUtil.tag(TAG_RESPONSE,
                        MyXmlUtil.attr(ATTR_CODE, returnStatus),
                        msg), MIME_XML);
            }
            if (request.get(ARG_HARVESTER_REDIRECTTOEDIT, false)) {
                return new Result(request.makeUrl(URL_HARVESTERS_FORM,
                        ARG_HARVESTER_ID, harvester.getId()));
            }

            return new Result(request.makeUrl(URL_HARVESTERS_LIST));
        }
	if(!haveInited) {
	    sb.append(getPageHandler().showDialogNote("Harvesters have not been initialized yet"));
	} else {
	    //        sb.append(msgHeader("Harvesters"));
	    request.formPostWithAuthToken(sb, URL_HARVESTERS_NEW);
	    sb.append(HU.sectionOpen(null, false));
	    sb.append(HU.submit("New Harvester"));
	    sb.append(HU.formClose());

	    if (request.hasMessage()) {
		sb.append(messageNote(request.getMessage()));
	    }
	    sb.append(HU.center(getLogLink()));
	    makeHarvestersList(request, harvesters, sb);
	}
        sb.append(HU.sectionClose());

        return getAdmin().makeResult(request, msg("RAMADDA-Admin-Harvesters"), sb);
    }

    public String getLogLink() {
	return  HU.href(
			getAdmin().URL_ADMIN_LOG
			+ "?log=harvester.log", msg(
						    "Harvest Log"));
    }

    private List<Harvester> sortHarvesters(List<Harvester> harvesters) {
	HashSet seen = new HashSet();
	List<Harvester> sorted = new ArrayList<Harvester>();
	for(Harvester harvester: harvesters) {
	    if(harvester.getActive()) {
		seen.add(harvester.getId());
		sorted.add(harvester);
	    }
	}
	for(int i=harvesters.size()-1;i>=0;i--) {
	    Harvester harvester = harvesters.get(i);
	    if(!seen.contains(harvester.getId())) {
		seen.add(harvester.getId());
		sorted.add(harvester);
	    }
	}
	return sorted;
    }
    private void makeHarvestersList(Request request,
                                    List<Harvester> harvesters,
                                    StringBuffer sb)
            throws Exception {
	
        sb.append(HU.p());
        //        sb.append(HU.formTable());
        sb.append("<table cellpadding=4 cellspacing=0>");
        sb.append(HU.row(HU.cols("",
                HU.bold(msg("Name")),
				 HU.bold(msg("State")),
                HU.bold(msg("Action")), "", "")));

        int cnt = 0;
        for (Harvester harvester : sortHarvesters(harvesters)) {
            String removeLink =
                HU.href(request.makeUrl(URL_HARVESTERS_LIST,
                    ARG_ACTION, ACTION_REMOVE, ARG_HARVESTER_ID,
                    harvester.getId()), msg("Remove"));
            if (harvester.getIsEditable()) {
                removeLink = "";
            }

            String edit = "&nbsp;";
            if (harvester.getIsEditable()) {
                String icon =
                    edit =
		    HU.div(HU.href(request.makeUrl(URL_HARVESTERS_FORM,
							  ARG_HARVESTER_ID,
							  harvester.getId()),"Edit",
					  HU.cssClass("ramadda-clickable")),									

			   HU.cssClass("ramadda-button")); 
					  //getIconImage(ICON_EDIT, "title", msg("Edit")));
            }
            cnt++;
            String rowAttributes = HU.attr(HU.ATTR_VALIGN,
                                       HU.VALUE_TOP);

            rowAttributes += HU.cssClass(harvester.getActive()
                    ? "harvester-row harvester-active"
                    : "harvester-row");

            StringBuffer info  = new StringBuffer();
            StringBuffer error = harvester.getError();
            if ((error != null) && (error.length() > 0)) {
                info.append(HU.b(msg("Errors")));
		List<String> lines = Utils.split(error.toString(),"\n");
		lines.replaceAll(s -> {
			if(s.startsWith("***")) {
			    s = s.substring(3).trim();
			    if(s.length()==0) return "";
			    return "&bull; " + s+"";
			}
			return s;
		    });
		String errors = Utils.join(lines,"\n");
		errors = errors.replace("/div>\n","/div>");
		errors = errors.replace("/span>\n","/span>");		
                info.append("<div class=\"error-list\"><pre>" + errors
                            + "</div></pre>");
            }
            info.append(harvester.getExtraInfo());
            sb.append(HU.tag(HU.TAG_TR, rowAttributes,
                                    HU.cols(edit, harvester.getName(),
                                        (harvester.getActive()
                                         ? HU.bold(msg("Active"))
                                         : msg("Stopped")) + HU.space(
                                             2), harvester.getRunLink(
                                             request, false), removeLink,
                                                 info.toString())));
        }
        sb.append(HU.formTableClose());

    }

    public Result processFile(Request request) throws Exception {
        List<Harvester> harvesters  = getHarvesters();
        TypeHandler     typeHandler = getRepository().getTypeHandler(request);
        String          filepath    = request.getUnsafeString(ARG_FILE,
                                          BLANK);
        //Check to  make sure we can access this file
        if ( !getStorageManager().isInDownloadArea(new File(filepath))) {
            return new Result(BLANK,
                              new StringBuffer("Cannot load file:"
                                  + filepath), "text/plain");
        }
        for (Harvester harvester : harvesters) {
            Entry entry = harvester.processFile(typeHandler, filepath);
            if (entry != null) {
                getEntryManager().addNewEntry(request, entry);

                return new Result(BLANK, new StringBuffer("OK"),
                                  "text/plain");
            }
        }

        return new Result(BLANK, new StringBuffer("Could not create entry"),
                          "text/plain");
    }

}
