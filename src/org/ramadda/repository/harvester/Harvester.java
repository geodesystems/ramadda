/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.harvester;



import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.type.*;
import org.ramadda.util.FileWrapper;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.PatternHolder;
import org.ramadda.util.Utils;
import org.ramadda.util.geo.*;


import org.ramadda.util.sql.SqlUtil;


import org.w3c.dom.*;


import ucar.unidata.util.DateUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
//Use myxmlutil as it fixes how attributes are encoded
//import ucar.unidata.xml.XmlUtil;
import org.ramadda.util.MyXmlUtil;

import java.io.ByteArrayInputStream;

import java.io.File;
import java.io.InputStream;

import java.lang.reflect.*;



import java.net.*;


import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



import java.util.regex.*;

@SuppressWarnings("unchecked")
public abstract class Harvester extends RepositoryManager {
    public static boolean debug = false;
    public static final String FILE_PLACEHOLDER = ".placeholder";
    private static final boolean PRINT_DEBUG = false;
    private static final LogManager.LogId LOGID =
        new LogManager.LogId("org.ramadda.repository.harvester.Harvester");

    public static final String TAG_HARVESTER = "harvester";
    public static final String TAG_HARVESTERS = "harvesters";
    public static final String ATTR_EXTRA = "extra";
    public static final String ATTR_SLEEPUNIT = "sleepunit";
    public static final String ATTR_ALIASES = "aliases";
    public static final String ATTR_CLEANNAME = "cleanname";
    public static final String ATTR_TYPEPATTERNS = "typepatterns";
    public static final String UNIT_ABSOLUTE = "absolute";
    public static final String UNIT_MINUTE = "minute";
    public static final String UNIT_HOUR = "hour";
    public static final String UNIT_DAY = "day";
    public static final String ATTR_GENERATEMD5 = "generatemd5";
    public static final String ATTR_CLASS = "class";
    public static final String ATTR_ROOTDIR = "rootdir";
    public static final String ROOTDIR_DELIM = ";";
    public static final String ATTR_USER = "user";
    public static final String ATTR_MONITOR = "monitor";
    public static final String ATTR_ADDMETADATA = "addmetadata";
    public static final String ATTR_ADDSHORTMETADATA = "addshortmetadata";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_ACTIVEONSTART = "activeonstart";
    public static final String ATTR_TESTCOUNT = "testcount";
    public static final String ATTR_TESTMODE = "testmode";
    public static final String ATTR_SLEEP = "sleep";
    public static final String ATTR_NAMETEMPLATE = "nametemplate";
    public static final String ATTR_GROUPTEMPLATE = "grouptemplate";
    public static final String ATTR_TAGTEMPLATE = "tagtemplate";
    public static final String ATTR_DESCTEMPLATE = "desctemplate";
    public static final String ATTR_BASEGROUP = "basegroup";

    protected long startTime;
    protected long endTime;
    protected String baseGroupId = "";
    protected String groupTemplate = "";
    protected String nameTemplate = "${filename}";
    protected String descTemplate = "";
    protected String tagTemplate = "";
    protected Harvester parent;
    protected List<Harvester> children;
    private List<FileWrapper> rootDirs = new ArrayList<FileWrapper>();
    private String name = "";
    private Element element;
    private boolean monitor = false;
    private boolean active = false;
    private boolean generateMd5 = false;
    private boolean activeOnStart = false;
    private double sleepMinutes = 5;
    private String sleepUnit = UNIT_ABSOLUTE;
    private int timestamp = 0;
    private Hashtable extra;
    private boolean addMetadata = false;
    private boolean addShortMetadata = false;
    private String id;
    private boolean isEditable = false;
    private TypeHandler typeHandler;
    protected String typePatterns="";
    private String aliases = "";    
    private boolean cleanName = false;    
    private Hashtable<String,String> aliasMap;
    private StringBuffer error;
    protected StringBuffer status = new StringBuffer();
    protected List<String> otherMsgs = new ArrayList<String>();
    protected String currentStatus = "";
    private String userName;
    private User user;
    private boolean testMode = false;
    private int testCount = 100;
    protected String printTab = "";
    
    public Harvester(Repository repository) {
        super(repository);
        this.id = repository.getGUID();
    }

    public Harvester(Repository repository, String id) throws Exception {
        super(repository);
        this.id         = id;
        this.isEditable = true;
    }

    public Object getExtraAttribute(String key) {
	if(extra==null) return null;
	return extra.get(key);
    }

    public void putExtraAttribute(String key,Object value) {
	if(extra==null) extra = new Hashtable();
	extra.put(key,value);
    }

    public boolean getExtraAttribute(String key, boolean dflt) {
	return Utils.getProperty(extra,key,dflt);
    }
    public String getExtraAttribute(String key, String dflt) {
	return Utils.getProperty(extra,key,dflt);
    }    


    public String getDefaultTypeHandler() throws Exception {
	return TypeHandler.TYPE_FILE;
    }

    public TypeHandler getTypeHandler() throws Exception {
	return  getTypeHandler(getDefaultTypeHandler());
    }
    
    public TypeHandler getTypeHandler(String dflt) throws Exception {
        if (typeHandler == null) {
            this.typeHandler =
                repository.getTypeHandler(dflt);
        }
        return typeHandler;
    }
    
    public Harvester(Repository repository, Element element)
            throws Exception {
        this(repository);
        this.children = createHarvesters(repository, element);
        for (Harvester child : children) {
            child.parent = this;
        }
    }

    public abstract String getDescription();
    
    public String applyMacros(String s, Date createDate, Date fromDate,
                              Date toDate, String filename) {
        if (fromDate == null) {
            fromDate = createDate;
        }
        if (toDate == null) {
            toDate = fromDate;
        }
        s = getEntryManager().replaceMacros(s, createDate, fromDate, toDate);
        String[] macros = { "filename", filename, "fileextension",
                            IO.getFileExtension(filename), };

        for (int i = 0; i < macros.length; i += 2) {
            String macro = "${" + macros[i] + "}";
            String value = macros[i + 1];
            s = s.replace(macro, value);
        }

        return s;
    }

    
    protected User getUser() throws Exception {
        if (user != null) {
            return user;
        }
        if ((userName == null) || (userName.trim().length() == 0)) {
            user = repository.getUserManager().getAdminUser();
        } else {
            user = repository.getUserManager().findUser(userName);
	    if (user == null) {
		user = repository.getUserManager().getAdminUser();
	    }
        }
        return user;
    }




    
    private Request request;


    
    protected Request getRequest() throws Exception {
        if (request == null) {
            request = new Request(getRepository(), getUser());
            request.setSessionId(getSessionManager().createSessionId());
            request.put(ARG_FROMHARVESTER, "true");
        }

        return request;
    }

    
    public Entry getBaseGroup() throws Exception {
        if (!Utils.stringDefined(baseGroupId)) {
            return null;
        }
        Request request = new Request(getRepository(), getUser());
        Entry   baseGroup = getEntryManager().getEntry(getRequest(), baseGroupId);
        if (baseGroup != null) {
            return baseGroup;
        }
	//	System.err.println("Harvester:" + getName() +" " + getId() +"  unable to find base group: "+ baseGroupId);
	return null;
    }



    
    protected void addBaseGroupSelect(String selectId, StringBuffer sb)
            throws Exception {
        Entry  baseGroup = getBaseGroup();
        String extra     = "";
        if (baseGroup == null) {
            extra = HtmlUtils.br()
                    + HtmlUtils.span(
                        msg("Required"),
                        HtmlUtils.cssClass(CSS_CLASS_REQUIRED_LABEL));
        }

        getRepository().getPageHandler().addEntrySelect(getRequest(),
                baseGroup, ATTR_BASEGROUP, sb, "Base Group", extra);
    }


    
    protected void init(Element element) throws Exception {
        rootDirs = new ArrayList<FileWrapper>();
        for (String dir :
                Utils.split(MyXmlUtil.getAttribute(element, ATTR_ROOTDIR, ""),
                            ROOTDIR_DELIM, true, true)) {
            rootDirs.add(FileWrapper.createFileWrapper(dir,true));
        }

        this.typeHandler =
            repository.getTypeHandler(MyXmlUtil.getAttribute(element,
                ATTR_TYPE, TypeHandler.TYPE_ANY));

	this.typePatterns= MyXmlUtil.getAttribute(element,ATTR_TYPEPATTERNS, "");
        aliases = MyXmlUtil.getAttribute(element, ATTR_ALIASES, aliases);
        cleanName = MyXmlUtil.getAttribute(element, ATTR_CLEANNAME, cleanName);		
	
        groupTemplate = MyXmlUtil.getAttribute(element, ATTR_GROUPTEMPLATE,
                                             groupTemplate);

	//Set this before we get the base group since we need the user
        this.userName = MyXmlUtil.getAttribute(element, ATTR_USER, userName);
        this.user     = null;

        this.baseGroupId = MyXmlUtil.getAttribute(element, ATTR_BASEGROUP, "");

        Entry baseGroup = getBaseGroup();
        if (baseGroup != null) {
            baseGroupId = baseGroup.getId();
        }

        nameTemplate = MyXmlUtil.getAttribute(element, ATTR_NAMETEMPLATE,
                                            nameTemplate);
	String extraXml =MyXmlUtil.getGrandChildText(element,ATTR_EXTRA,null);
	if(extraXml!=null) {
	    extraXml = new String(Utils.decodeBase64(extraXml));
	    extra = (Hashtable) getRepository().decodeObject(extraXml);
	}

        descTemplate = MyXmlUtil.getAttribute(element, ATTR_DESCTEMPLATE, "");
        tagTemplate = MyXmlUtil.getAttribute(element, ATTR_TAGTEMPLATE,
                                           tagTemplate);



        this.name     = MyXmlUtil.getAttribute(element, ATTR_NAME, "");
        this.monitor = MyXmlUtil.getAttribute(element, ATTR_MONITOR, monitor);


        this.addMetadata = MyXmlUtil.getAttribute(element, ATTR_ADDMETADATA,
                addMetadata);
        this.addShortMetadata = MyXmlUtil.getAttribute(element,
                ATTR_ADDSHORTMETADATA, addShortMetadata);
        this.activeOnStart = MyXmlUtil.getAttribute(element,
                ATTR_ACTIVEONSTART, getDefaultActiveOnStart());

        this.generateMd5 = MyXmlUtil.getAttribute(element, ATTR_GENERATEMD5,
                generateMd5);
        this.testCount = MyXmlUtil.getAttribute(element, ATTR_TESTCOUNT,
                testCount);
        this.testMode = MyXmlUtil.getAttribute(element, ATTR_TESTMODE,
                                             testMode);
        this.sleepUnit = MyXmlUtil.getAttribute(element, ATTR_SLEEPUNIT,
                sleepUnit);

        this.sleepMinutes = MyXmlUtil.getAttribute(element, ATTR_SLEEP,
                sleepMinutes);

    }

    
    public boolean getDefaultActiveOnStart() {
        return false;
    }

    
    public String getRunLink(Request request, boolean redirectToEdit) {
        if (getActive()) {
            return HtmlUtils
                .href(request
                    .makeUrl(getRepository().getHarvesterManager()
                        .URL_HARVESTERS_LIST, ARG_ACTION, ACTION_STOP,
                            ARG_HARVESTER_ID, getId(),
                            ARG_HARVESTER_REDIRECTTOEDIT,
			     "" + redirectToEdit), msg("Stop"),HU.cssClass("ramadda-button"));
        } else {
            return HtmlUtils
                .href(request
                    .makeUrl(getRepository().getHarvesterManager()
                        .URL_HARVESTERS_LIST, ARG_ACTION, ACTION_START,
                            ARG_HARVESTER_ID, getId(),
                            ARG_HARVESTER_REDIRECTTOEDIT,
			     "" + redirectToEdit), msg("Start"),HU.cssClass("ramadda-button"));
        }
    }



    public void addAliasesEditForm(Request request, Appendable sb) throws Exception {
	HU.formEntry(sb, msgLabel("Clean name"),
			       HtmlUtils.labeledCheckbox(ATTR_CLEANNAME, "true",
							 cleanName, "Clean up name from filename, etc"));
	sb.append(HU.formEntryTop(msgLabel("Aliases"),
				  HU.hbox(HU.textArea(ATTR_ALIASES,
						      aliases,
						      10,60)," Format:<pre>name:Name to use\ne.g.:\nco:Colorado</pre>")));
    }



    public String getName(String name, boolean isDir) {
	name = getAlias(name,name);
	if(cleanName && isDir) {
	    name =Utils.makeLabel(name);
	}
	return name;
    }

    public String getAlias(String n, String dflt) {
	if(n==null) return dflt;
	if(aliasMap==null) {
	    aliasMap = new Hashtable<String,String>();
	    for(String line: Utils.split(aliases,"\n",true,true)) {
		List<String> toks = Utils.splitUpTo(line,":",2);
		if(toks.size()==2) {
		    aliasMap.put(toks.get(0),toks.get(1));
		}
	    }
	}
	String alias = aliasMap.get(n);
	if(alias == null) {
	    if(Misc.equals(aliasMap.get("states"),"true")) {
		Place place = GeoResource.RESOURCE_STATES.getPlace(n.toLowerCase());
		if(place!=null) alias = place.getName();
	    }

	}
	if(alias==null) return dflt;
	return alias;
    }


    
    public void applyEditForm(Request request) throws Exception {
        this.request = null;
        getEntryManager().clearSeenResources();
        rootDirs = new ArrayList<FileWrapper>();
        for (String dir :
                Utils.split(request.getUnsafeString(ATTR_ROOTDIR, ""), "\n",
                            true, true)) {
	    FileWrapper fw = FileWrapper.createFileWrapper(dir);
            rootDirs.add(fw);
        }

        name = request.getString(ARG_NAME, name);

        typeHandler = repository.getTypeHandler(request.getString(ATTR_TYPE,
                ""));
	typePatterns = request.getString(ATTR_TYPEPATTERNS,typePatterns);
        aliases = request.getUnsafeString(ATTR_ALIASES,  aliases);
	aliasMap = null;
        cleanName = request.get(ATTR_CLEANNAME,  cleanName);		

        activeOnStart = request.get(ATTR_ACTIVEONSTART, false);
        generateMd5   = request.get(ATTR_GENERATEMD5, false);
        testCount     = request.get(ATTR_TESTCOUNT, testCount);
        testMode      = request.get(ATTR_TESTMODE, false);
        monitor       = request.get(ATTR_MONITOR, false);
        userName      = request.getString(ATTR_USER, userName);
        if (userName != null) {
            userName = userName.trim();
        }
        this.user        = null;
        addMetadata      = request.get(ATTR_ADDMETADATA, false);
        addShortMetadata = request.get(ATTR_ADDSHORTMETADATA, false);
        sleepMinutes     = request.get(ATTR_SLEEP, sleepMinutes);
        sleepUnit        = request.getString(ATTR_SLEEPUNIT, sleepUnit);
        if (sleepUnit.equals(UNIT_HOUR)) {
            sleepMinutes = sleepMinutes * 60;
        } else if (sleepUnit.equals(UNIT_DAY)) {
            sleepMinutes = sleepMinutes * 60 * 60;
        }
        nameTemplate = request.getString(ATTR_NAMETEMPLATE, nameTemplate);
        groupTemplate = request.getUnsafeString(ATTR_GROUPTEMPLATE,
                groupTemplate);

        baseGroupId = request.getUnsafeString(ATTR_BASEGROUP + "_hidden", "");

        descTemplate = request.getUnsafeString(ATTR_DESCTEMPLATE,
                descTemplate);

    }

    
    public void createEditForm(Request request, StringBuffer sb)
            throws Exception {
        sb.append(HtmlUtils.formEntry(msgLabel("Harvester name"),
                                      HtmlUtils.input(ARG_NAME, name,
                                          HtmlUtils.SIZE_40)));

        makeRunSettings(request, sb);

    }





    
    public void makeRunSettings(Request request, StringBuffer sb) {
        List<TwoFacedObject> tfos = new ArrayList<TwoFacedObject>();
        tfos.add(new TwoFacedObject(msg("Absolute (minutes)"),
                                    UNIT_ABSOLUTE));
        tfos.add(new TwoFacedObject(msg("Minutes"), UNIT_MINUTE));
        tfos.add(new TwoFacedObject(msg("Hourly"), UNIT_HOUR));
        //        tfos.add(new TwoFacedObject("Daily",UNIT_DAY));


        String minutes = "" + sleepMinutes;
        if (sleepUnit.equals(UNIT_HOUR)) {
            minutes = "" + (sleepMinutes / 60);
        } else if (sleepUnit.equals(UNIT_DAY)) {
            minutes = "" + (sleepMinutes / (60 * 60));
        }
        String sleepType = HtmlUtils.select(ATTR_SLEEPUNIT, tfos, sleepUnit);
        String sleepLbl =
            "<br>" + HtmlUtils.space(3)
            + "e.g., 30 minutes = on the hour and the half hour<br>"
            + HtmlUtils.space(3);

        if (sleepUnit.equals(UNIT_ABSOLUTE)) {
            sleepLbl += msg("Would run in") + " " + sleepMinutes + " "
                        + msg("minutes");
        } else {
            long sleepTime = Misc.getPauseEveryTime((int) sleepMinutes);
            Date now       = new Date();
            Date then      = new Date(now.getTime() + sleepTime);
            sleepLbl += msg("Would run at") + " "
                        + getDateHandler().formatDate(then);
        }

        StringBuffer runWidgets = new StringBuffer();
        int          widgetCnt  = 0;
        if (showWidget(ATTR_TESTMODE)) {
            runWidgets.append(HtmlUtils.labeledCheckbox(ATTR_TESTMODE, "true",
						 testMode, "Test mode"));
            runWidgets.append(HtmlUtils.space(3) + msgLabel("Count")
                              + HtmlUtils.input(ATTR_TESTCOUNT,
                                  "" + testCount, HtmlUtils.SIZE_5));
            runWidgets.append(HtmlUtils.br());
            widgetCnt++;
        }
        if (showWidget(ATTR_ACTIVEONSTART)) {
            runWidgets.append(HtmlUtils.labeledCheckbox(ATTR_ACTIVEONSTART, "true",
							activeOnStart, "Active on startup"));
            runWidgets.append(HtmlUtils.br());
            widgetCnt++;
        }

        if (showWidget(ATTR_MONITOR)) {
            runWidgets.append(HtmlUtils.labeledCheckbox(ATTR_MONITOR, "true",
							monitor, "Run continually"));

            runWidgets.append(HtmlUtils.br() + HtmlUtils.space(5));
            runWidgets.append(msgLabel("Every") + HtmlUtils.space(1)
                              + HtmlUtils.input(ATTR_SLEEP, "" + minutes,
                                  HtmlUtils.SIZE_5) + HtmlUtils.space(1)
                                      + sleepType + sleepLbl);
            runWidgets.append(HtmlUtils.br());
            widgetCnt++;
        }
        if (showWidget(ATTR_GENERATEMD5)) {
            runWidgets.append(HtmlUtils.labeledCheckbox(ATTR_GENERATEMD5, "true",
							generateMd5,
							"Generate MD5 Checksum"));
            widgetCnt++;
        }

        String widgetText = runWidgets.toString();
        if (widgetCnt > 2) {
            widgetText = HtmlUtils.makeShowHideBlock(msg("Run Settings"),
                    widgetText, false);
        }
        sb.append(HtmlUtils.formEntryTop("", widgetText));
    }


    
    public boolean showWidget(String arg) {
        return true;
    }

    
    public boolean equals(Object o) {
        if ( !getClass().equals(o.getClass())) {
            return false;
        }

        return this.id.equals(((Harvester) o).id);
    }



    
    public void applyState(Element element) throws Exception {
        element.setAttribute(ATTR_CLASS, getClass().getName());
        element.setAttribute(ATTR_NAME, name);
        element.setAttribute(ATTR_ACTIVEONSTART, activeOnStart + "");
        element.setAttribute(ATTR_GENERATEMD5, generateMd5 + "");
        element.setAttribute(ATTR_TESTMODE, testMode + "");
        element.setAttribute(ATTR_TESTCOUNT, testCount + "");
        element.setAttribute(ATTR_MONITOR, monitor + "");
        element.setAttribute(ATTR_USER, userName);
        element.setAttribute(ATTR_ADDMETADATA, addMetadata + "");
        element.setAttribute(ATTR_ADDSHORTMETADATA, addShortMetadata + "");
        element.setAttribute(ATTR_TYPE, getTypeHandler().getType());
        element.setAttribute(ATTR_TYPEPATTERNS, typePatterns);
        element.setAttribute(ATTR_ALIASES, aliases);
        element.setAttribute(ATTR_CLEANNAME, ""+cleanName);		
        element.setAttribute(ATTR_SLEEP, sleepMinutes + "");
        element.setAttribute(ATTR_SLEEPUNIT, sleepUnit);

        element.setAttribute(ATTR_TAGTEMPLATE, tagTemplate);
        element.setAttribute(ATTR_NAMETEMPLATE, nameTemplate);
        element.setAttribute(ATTR_GROUPTEMPLATE, groupTemplate);
        element.setAttribute(ATTR_BASEGROUP, baseGroupId);
        element.setAttribute(ATTR_DESCTEMPLATE, descTemplate);

        if (rootDirs != null) {
            element.setAttribute(ATTR_ROOTDIR,
                                 StringUtil.join(ROOTDIR_DELIM, rootDirs));
        }

	if(extra!=null) {
	    String xml = getRepository().encodeObject(extra);
	    xml = Utils.encodeBase64(xml);
	    Element extraNode = MyXmlUtil.create(element.getOwnerDocument(), ATTR_EXTRA, element);
	    extraNode.appendChild(MyXmlUtil.makeCDataNode(element.getOwnerDocument(),
							  xml,
							  false));

	}


    }

    
    public String getContent() throws Exception {
        Document doc  = MyXmlUtil.makeDocument();
        Element  root = doc.createElement(TAG_HARVESTER);
        applyState(root);
        String xml =  MyXmlUtil.toString(root);
	return xml;
    }

    
    public void initFromContent(String content) throws Exception {
        if ((content == null) || (content.trim().length() == 0)) {
            return;
        }
        content = content.replace("${fromdate}", "${from_date}");
        content = content.replace("${year}", "${from_year}");
        content = content.replace("${month}", "${from_month}");
        content = content.replace("${monthname}", "${from_monthname}");
        content = content.replace("${day}", "${from_day}");
        Element root =
            MyXmlUtil.getRoot(new ByteArrayInputStream(content.getBytes()));
        init(root);
    }


    
    public Entry processFile(TypeHandler type, String filepath)
            throws Exception {
        return null;
    }


    
    public String getId() {
        return id;
    }

    
    public void setId(String id) {
        this.id = id;
    }


    
    public static List<Harvester> createHarvesters(Repository repository,
            Element root)
            throws Exception {
        List<Harvester> harvesters = new ArrayList<Harvester>();
        List            children   = MyXmlUtil.findChildren(root,
                                         TAG_HARVESTER);
        for (int i = 0; i < children.size(); i++) {
            Element node = (Element) children.get(i);
            Class c = Misc.findClass(MyXmlUtil.getAttribute(node, ATTR_CLASS));
            Constructor ctor = Misc.findConstructor(c,
                                   new Class[] { Repository.class,
                    Element.class });
            Harvester harvester = (Harvester) ctor.newInstance(new Object[] {
                                      repository,
                                      node });
            harvesters.add(harvester);
            harvester.init(node);
        }

        return harvesters;
    }



    
    public List<FileWrapper> getRootDirs() {
        return rootDirs;
    }


    
    public boolean canContinueRunning(int timestamp) {
        return getRepository().getActive() && getActive()
               && (timestamp == getCurrentTimestamp());
    }

    
    public int getCurrentTimestamp() {
        return timestamp;
    }

    
    public void logStatus(String msg) {
        status.append("[<i>" + getDateHandler().formatDate(new Date())
                      + "</i>]: " + msg + "<br>");
    }


    
    public final void run() throws Exception {
        try {
            if (active) {
                logHarvesterError("Error: harvester is already running",
                                  null);

                return;
            }
            error = new StringBuffer();
            setActive(true);
            startTime = System.currentTimeMillis();
            runInner(++timestamp);
            endTime = System.currentTimeMillis();
        } catch (Throwable exc) {
            logHarvesterError("Error in harvester.run", exc);
            error.append(getStack(exc));
        }
        setActive(false);
    }

    public String getStack(Throwable exc) {
	return  "Error: " + exc.getMessage()+"<br>"+
	    HU.makeShowHideBlock("Stack",
				 LogUtil.getStackTrace(
						       LogUtil.getInnerException(exc)).trim(), false);
    }


    
    public void logHarvesterError(String message, Throwable exc) {
        System.err.println("ERROR:" + getName() + " " + message);
        exc.printStackTrace();
        getRepository().getLogManager().logError(LOGID,
                getName() + " " + message, exc);
        appendError(message);
    }


    
    public void logHarvesterInfo(String message) {
        if (PRINT_DEBUG) {
            System.err.println(printTab + message);
        }
        getLogManager().logInfo(LOGID, getName() + ": " + printTab + message);
    }




    
    public void clearCache() {}


    
    public StringBuffer getError() {
        return error;
    }

    
    public void appendError(String e) {
        if (this.error == null) {
            this.error = new StringBuffer();
        }
        this.error.append(e);
        this.error.append("<br>");
    }


    
    public String getExtraInfo() throws Exception {
        return currentStatus + "<br>" + status;
    }

    
    public void resetStatus() {
        status        = new StringBuffer();
	otherMsgs = new ArrayList<String>();
        currentStatus = "";
    }


    
    protected void runInner(int timestamp) throws Exception {
        while (canContinueRunning(timestamp)) {
            try {
                runHarvester();

                if ( !canContinueRunning(timestamp)) {
                    break;
                }
            } catch (Exception exc) {
                logHarvesterError("Error in harvester:" + this, exc);

                return;
            }
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
            logStatus(msg);
            doPause();
        }
    }

    
    protected void runHarvester() throws Exception {
        //noop
    }



    
    protected void doPause() {
        double minutes = getSleepMinutes();
        if (minutes < 1) {
	    status.append(new Date((long)(new Date().getTime()+1000 * 60 * minutes)));
            Misc.sleep((long) (1000 * 60 * minutes));
            return;
        }
        Utils.pauseEvery((int) minutes,status);
    }

    
    public void setActive(boolean value) {
        active = value;
    }

    
    public boolean getActive() {
        return active;
    }



    
    public void setMonitor(boolean value) {
        monitor = value;
    }

    
    public boolean getMonitor() {
        return monitor;
    }

    
    public void setAddMetadata(boolean value) {
        addMetadata = value;
    }

    
    public boolean getAddMetadata() {
        return addMetadata;
    }


    
    public void setAddShortMetadata(boolean value) {
        addShortMetadata = value;
    }

    
    public boolean getAddShortMetadata() {
        return addShortMetadata;
    }


    
    public void setUser(User user) {
        if (user != null) {
            userName = user.getId();
        } else {
            userName = null;
        }
    }

    
    public void setUserName(String value) {
        this.userName = value;
    }

    
    public String getUserName() {
        return this.userName;
    }




    
    public void setSleepMinutes(double value) {
        sleepMinutes = value;
    }

    
    public double getSleepMinutes() {
        return sleepMinutes;
    }

    
    public void setName(String value) {
        name = value;
    }

    
    public String getName() {
        return name;
    }

    
    public void setIsEditable(boolean value) {
        isEditable = value;
    }

    
    public boolean getIsEditable() {
        return isEditable;
    }


    
    public List<String> split(Element element, String attr) {
        if ( !MyXmlUtil.hasAttribute(element, attr)) {
            return new ArrayList<String>();
        }

        return Utils.split(MyXmlUtil.getAttribute(element, attr), ",", true,
                           true);
    }



    
    public void setActiveOnStart(boolean value) {
        activeOnStart = value;
    }

    
    public boolean getActiveOnStart() {
        return activeOnStart;
    }


    
    public void setGenerateMd5(boolean value) {
        generateMd5 = value;
    }

    
    public boolean getGenerateMd5() {
        return generateMd5;
    }




    
    public void debug(String msg) {
        if (debug || getTestMode()) {
	    if(debug) System.err.println(msg);
            logHarvesterInfo(msg);
            msg = msg.replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
            msg = msg.replace("\n", "<br>");
            status.append(msg);
            status.append(HtmlUtils.br());
        }
    }


    
    public void setTestMode(boolean value) {
        testMode = value;
    }

    
    public boolean getTestMode() {
        return testMode;
    }


    
    public void setTestCount(int value) {
        testCount = value;
    }

    
    public int getTestCount() {
        return testCount;
    }

    
    public boolean defined(String s) {
        if ((s != null) && (s.length() > 0)) {
            return true;
        }

        return false;
    }

    
    public String toString() {
        return "harvester:" + getName();
    }

}
