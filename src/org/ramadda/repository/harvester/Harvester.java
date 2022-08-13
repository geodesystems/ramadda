/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.harvester;



import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.type.*;
import org.ramadda.util.FileWrapper;
import org.ramadda.util.HtmlUtils;
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


/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public abstract class Harvester extends RepositoryManager {

    /** _more_ */
    public static final String FILE_PLACEHOLDER = ".placeholder";


    /** _more_ */
    private static final boolean PRINT_DEBUG = false;

    /** _more_ */
    private final LogManager.LogId LOGID =
        new LogManager.LogId("org.ramadda.repository.harvester.Harvester");


    /** _more_ */

    public static final String TAG_HARVESTER = "harvester";

    /** _more_ */
    public static final String TAG_HARVESTERS = "harvesters";

    /** _more_ */
    public static final String ATTR_SLEEPUNIT = "sleepunit";

    public static final String ATTR_ALIASES = "aliases";
    public static final String ATTR_CLEANNAME = "cleanname";

    public static final String ATTR_TYPEPATTERNS = "typepatterns";

    /** _more_ */
    public static final String UNIT_ABSOLUTE = "absolute";

    /** _more_ */
    public static final String UNIT_MINUTE = "minute";

    /** _more_ */
    public static final String UNIT_HOUR = "hour";

    /** _more_ */
    public static final String UNIT_DAY = "day";


    /** _more_ */
    public static final String ATTR_GENERATEMD5 = "generatemd5";

    /** _more_ */
    public static final String ATTR_CLASS = "class";

    /** _more_ */
    public static final String ATTR_ROOTDIR = "rootdir";

    /** _more_ */
    public static final String ROOTDIR_DELIM = ";";

    /** _more_ */
    public static final String ATTR_USER = "user";

    /** _more_ */
    public static final String ATTR_MONITOR = "monitor";

    /** _more_ */
    public static final String ATTR_ADDMETADATA = "addmetadata";

    /** _more_ */
    public static final String ATTR_ADDSHORTMETADATA = "addshortmetadata";

    /** _more_ */
    public static final String ATTR_NAME = "name";

    /** _more_ */
    public static final String ATTR_ACTIVEONSTART = "activeonstart";

    /** _more_ */
    public static final String ATTR_TESTCOUNT = "testcount";

    /** _more_ */
    public static final String ATTR_TESTMODE = "testmode";

    /** _more_ */
    public static final String ATTR_SLEEP = "sleep";


    /** _more_ */
    public static final String ATTR_NAMETEMPLATE = "nametemplate";

    /** _more_ */
    public static final String ATTR_GROUPTEMPLATE = "grouptemplate";

    /** _more_ */
    public static final String ATTR_TAGTEMPLATE = "tagtemplate";

    /** _more_ */
    public static final String ATTR_DESCTEMPLATE = "desctemplate";

    /** _more_ */
    public static final String ATTR_BASEGROUP = "basegroup";



    /** _more_ */
    protected long startTime;

    /** _more_ */
    protected long endTime;



    /** _more_ */
    protected String baseGroupId = "";

    /** _more_ */
    protected String groupTemplate = "";

    /** _more_ */
    protected String nameTemplate = "${filename}";

    /** _more_ */
    protected String descTemplate = "";

    /** _more_ */
    protected String tagTemplate = "";



    /** _more_ */
    protected Harvester parent;

    /** _more_ */
    protected List<Harvester> children;

    /** _more_ */
    private List<FileWrapper> rootDirs = new ArrayList<FileWrapper>();

    /** _more_ */
    private String name = "";

    /** _more_ */
    private Element element;

    /** _more_ */
    private boolean monitor = false;

    /** _more_ */
    private boolean active = false;

    /** _more_ */
    private boolean generateMd5 = false;

    /** _more_ */
    private boolean activeOnStart = false;

    /** _more_ */
    private double sleepMinutes = 5;

    /** _more_ */
    private String sleepUnit = UNIT_ABSOLUTE;

    /** _more_ */
    private int timestamp = 0;

    /** _more_ */
    private boolean addMetadata = false;


    /** _more_ */
    private boolean addShortMetadata = false;

    /** _more_ */
    private String id;

    /** _more_ */
    private boolean isEditable = false;

    /** _more_ */
    private TypeHandler typeHandler;

    private String typePatterns="";

    private String aliases = "";    

    private boolean cleanName = false;    

    private Hashtable<String,String> aliasMap;




    /** _more_ */
    private StringBuffer error;

    /** _more_ */
    protected StringBuffer status = new StringBuffer();

    /** _more_ */
    protected String currentStatus = "";

    /** _more_ */
    private String userName;

    /** _more_ */
    private User user;

    /** _more_ */
    private boolean testMode = false;

    /** _more_ */
    private int testCount = 100;


    /** _more_ */
    protected String printTab = "";

    /**
     * _more_
     *
     * @param repository _more_
     */
    public Harvester(Repository repository) {
        super(repository);
        this.id = repository.getGUID();
    }

    /**
     * _more_
     *
     * @param repository _more_
     * @param id _more_
     *
     * @throws Exception _more_
     */
    public Harvester(Repository repository, String id) throws Exception {
        super(repository);
        this.id         = id;
        this.isEditable = true;
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public TypeHandler getTypeHandler() throws Exception {
        if (typeHandler == null) {
            this.typeHandler =
                repository.getTypeHandler(TypeHandler.TYPE_FILE);
        }

        return typeHandler;
    }


    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     *
     * @throws Exception _more_
     */
    public Harvester(Repository repository, Element element)
            throws Exception {
        this(repository);
        this.children = createHarvesters(repository, element);
        for (Harvester child : children) {
            child.parent = this;
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public abstract String getDescription();

    /**
     * _more_
     *
     * @param s _more_
     * @param createDate _more_
     * @param fromDate _more_
     * @param toDate _more_
     * @param filename _more_
     *
     * @return _more_
     */
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
                            IOUtil.getFileExtension(filename), };

        for (int i = 0; i < macros.length; i += 2) {
            String macro = "${" + macros[i] + "}";
            String value = macros[i + 1];
            s = s.replace(macro, value);
        }

        return s;
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected User getUser() throws Exception {
        if (user != null) {
            return user;
        }
        if ((userName == null) || (userName.trim().length() == 0)) {
            user = repository.getUserManager().getDefaultUser();
        } else {
            user = repository.getUserManager().findUser(userName);
        }
        if (user == null) {
            user = repository.getUserManager().getDefaultUser();
        }

        return user;
    }




    /** _more_ */
    private Request request;


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Request getRequest() throws Exception {
        if (request == null) {
            request = new Request(getRepository(), getUser());
            request.setSessionId(getSessionManager().createSessionId());
            request.put(ARG_FROMHARVESTER, "true");
        }

        return request;
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
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



    /**
     * _more_
     *
     * @param selectId _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
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


    /**
     * _more_
     *
     * @param element _more_
     *
     * @throws Exception _more_
     */
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
        this.baseGroupId = MyXmlUtil.getAttribute(element, ATTR_BASEGROUP, "");

        Entry baseGroup = getBaseGroup();
        if (baseGroup != null) {
            baseGroupId = baseGroup.getId();
        }

        nameTemplate = MyXmlUtil.getAttribute(element, ATTR_NAMETEMPLATE,
                                            nameTemplate);
        descTemplate = MyXmlUtil.getAttribute(element, ATTR_DESCTEMPLATE, "");
        tagTemplate = MyXmlUtil.getAttribute(element, ATTR_TAGTEMPLATE,
                                           tagTemplate);



        this.name     = MyXmlUtil.getAttribute(element, ATTR_NAME, "");
        this.monitor = MyXmlUtil.getAttribute(element, ATTR_MONITOR, monitor);

        this.userName = MyXmlUtil.getAttribute(element, ATTR_USER, userName);
        this.user     = null;

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

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getDefaultActiveOnStart() {
        return false;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param redirectToEdit _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
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
        sb.append(HtmlUtils.formEntry(msgLabel("Harvester name"),
                                      HtmlUtils.input(ARG_NAME, name,
                                          HtmlUtils.SIZE_40)));

        makeRunSettings(request, sb);

    }


    public static class PatternType {
	PatternHolder pattern;
	TypeHandler type;
	public PatternType(PatternHolder pattern, TypeHandler type) {
	    this.pattern = pattern;
	    this.type =type;
	}
    }


    public List<PatternType> getTypePatterns() throws Exception {
	List<PatternType> p = new ArrayList<PatternType>();
	if(Utils.stringDefined(typePatterns)) {
	    for(String line:Utils.split(typePatterns,"\n",true,true)) {
		List<String> toks = Utils.splitUpTo(line,":",2);
		if(toks.size()!=2)  continue;
		TypeHandler typeHandler = getRepository().getTypeHandler(toks.get(0));
		if(typeHandler!=null) {
		    p.add(new PatternType(new PatternHolder(toks.get(1)), typeHandler));
		}
	    }
	}
	return p;
    }

    public void makeTypePatternsInput(Request request, Appendable sb) 
	throws Exception {
	String uid = HU.getUniqueId("select_");
	String textid = HU.getUniqueId("text_");	
	String attrs =HU.style("max-width:200px;") + HU.id(uid);
	List items = Utils.makeList(new TwoFacedObject("Add type",""));
	String select = getRepository().makeTypeSelect(items, request,"noop",attrs,
						       false,"",false,null,false);
	String textArea = HtmlUtils.textArea(ATTR_TYPEPATTERNS, typePatterns, 
					     5, 60,HU.id(textid));
        sb.append(HtmlUtils.formEntryTop(msgLabel("Type Patterns"),
					 HU.hbox(
						 textArea,
						 select+
						 "<br>Form:<pre>entry type:pattern</pre>")));
	HU.importJS(sb,getRepository().getPageHandler().makeHtdocsUrl("/harvester.js"));
        HU.script(sb, "HtmlUtils.initTypeMenu(" +HU.comma(HU.squote(uid),HU.squote(textid))+");\n");
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     */
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
            runWidgets.append(HtmlUtils.checkbox(ATTR_TESTMODE, "true",
                    testMode) + HtmlUtils.space(1) + msg("Test mode"));
            runWidgets.append(HtmlUtils.space(3) + msgLabel("Count")
                              + HtmlUtils.input(ATTR_TESTCOUNT,
                                  "" + testCount, HtmlUtils.SIZE_5));
            runWidgets.append(HtmlUtils.br());
            widgetCnt++;
        }
        if (showWidget(ATTR_ACTIVEONSTART)) {
            runWidgets.append(HtmlUtils.checkbox(ATTR_ACTIVEONSTART, "true",
                    activeOnStart) + HtmlUtils.space(1)
                                   + msg("Active on startup"));
            runWidgets.append(HtmlUtils.br());
            widgetCnt++;
        }

        if (showWidget(ATTR_MONITOR)) {
            runWidgets.append(HtmlUtils.checkbox(ATTR_MONITOR, "true",
                    monitor) + HtmlUtils.space(1) + msg("Run continually"));

            runWidgets.append(HtmlUtils.br() + HtmlUtils.space(5));
            runWidgets.append(msgLabel("Every") + HtmlUtils.space(1)
                              + HtmlUtils.input(ATTR_SLEEP, "" + minutes,
                                  HtmlUtils.SIZE_5) + HtmlUtils.space(1)
                                      + sleepType + sleepLbl);
            runWidgets.append(HtmlUtils.br());
            widgetCnt++;
        }
        if (showWidget(ATTR_GENERATEMD5)) {
            runWidgets.append(HtmlUtils.checkbox(ATTR_GENERATEMD5, "true",
                    generateMd5) + HtmlUtils.space(1)
                                 + msg("Generate MD5 Checksum"));
            widgetCnt++;
        }

        String widgetText = runWidgets.toString();
        if (widgetCnt > 2) {
            widgetText = HtmlUtils.makeShowHideBlock(msg("Run Settings"),
                    widgetText, false);
        }
        sb.append(HtmlUtils.formEntryTop("", widgetText));
    }


    /**
     * _more_
     *
     * @param arg _more_
     *
     * @return _more_
     */
    public boolean showWidget(String arg) {
        return true;
    }

    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    public boolean equals(Object o) {
        if ( !getClass().equals(o.getClass())) {
            return false;
        }

        return this.id.equals(((Harvester) o).id);
    }



    /**
     * _more_
     *
     * @param element _more_
     *
     * @throws Exception _more_
     */
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
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getContent() throws Exception {
        Document doc  = MyXmlUtil.makeDocument();
        Element  root = doc.createElement(TAG_HARVESTER);
        applyState(root);
        String xml =  MyXmlUtil.toString(root);
	return xml;
    }

    /**
     * _more_
     *
     * @param content _more_
     *
     * @throws Exception _more_
     */
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


    /**
     * _more_
     *
     * @param type _more_
     * @param filepath _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry processFile(TypeHandler type, String filepath)
            throws Exception {
        return null;
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
     * @param repository _more_
     * @param root _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
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



    /**
     * _more_
     *
     * @return _more_
     */
    public List<FileWrapper> getRootDirs() {
        return rootDirs;
    }


    /**
     * _more_
     *
     * @param timestamp _more_
     *
     * @return _more_
     */
    public boolean canContinueRunning(int timestamp) {
        return getRepository().getActive() && getActive()
               && (timestamp == getCurrentTimestamp());
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getCurrentTimestamp() {
        return timestamp;
    }

    /**
     * _more_
     *
     * @param msg _more_
     */
    public void logStatus(String msg) {
        status.append("[<i>" + getDateHandler().formatDate(new Date())
                      + "</i>]: " + msg + "<br>");
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
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
            error.append("Error: " + exc
                         + HtmlUtils.makeShowHideBlock("Stack",
                             LogUtil.getStackTrace(exc), false));
        }
        setActive(false);
    }


    /**
     * _more_
     *
     * @param message _more_
     * @param exc _more_
     */
    public void logHarvesterError(String message, Throwable exc) {
        System.err.println("ERROR:" + getName() + " " + message);
        exc.printStackTrace();
        getRepository().getLogManager().logError(LOGID,
                getName() + " " + message, exc);
        appendError(message);
    }


    /**
     * _more_
     *
     * @param message _more_
     */
    public void logHarvesterInfo(String message) {
        if (PRINT_DEBUG) {
            System.err.println(printTab + message);
        }
        getLogManager().logInfo(LOGID, getName() + ": " + printTab + message);
    }




    /**
     * _more_
     */
    public void clearCache() {}


    /**
     * _more_
     *
     * @return _more_
     */
    public StringBuffer getError() {
        return error;
    }

    /**
     * _more_
     *
     * @param e _more_
     */
    public void appendError(String e) {
        if (this.error == null) {
            this.error = new StringBuffer();
        }
        this.error.append(e);
        this.error.append("<br>");
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getExtraInfo() throws Exception {
        return currentStatus + "<br>" + status;
    }

    /**
     * _more_
     */
    public void resetStatus() {
        status        = new StringBuffer();
        currentStatus = "";
    }


    /**
     * _more_
     *
     *
     * @param timestamp _more_
     * @throws Exception _more_
     */
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

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void runHarvester() throws Exception {
        //noop
    }


    /**
     * _more_
     */
    protected void doPause() {
        double minutes = getSleepMinutes();
        if (minutes < 1) {
            Misc.sleep((long) (1000 * 60 * minutes));

            return;
        }
        Misc.pauseEvery((int) minutes);
    }

    /**
     * Set the Active property.
     *
     * @param value The new value for Active
     */
    public void setActive(boolean value) {
        active = value;
    }

    /**
     * Get the Active property.
     *
     * @return The Active
     */
    public boolean getActive() {
        return active;
    }



    /**
     * Set the Monitor property.
     *
     * @param value The new value for Monitor
     */
    public void setMonitor(boolean value) {
        monitor = value;
    }

    /**
     * Get the Monitor property.
     *
     * @return The Monitor
     */
    public boolean getMonitor() {
        return monitor;
    }

    /**
     *  Set the AddMetadata property.
     *
     *  @param value The new value for AddMetadata
     */
    public void setAddMetadata(boolean value) {
        addMetadata = value;
    }

    /**
     *  Get the AddMetadata property.
     *
     *  @return The AddMetadata
     */
    public boolean getAddMetadata() {
        return addMetadata;
    }


    /**
     *  Set the AddMetadata property.
     *
     *  @param value The new value for AddMetadata
     */
    public void setAddShortMetadata(boolean value) {
        addShortMetadata = value;
    }

    /**
     *  Get the AddMetadata property.
     *
     *  @return The AddMetadata
     */
    public boolean getAddShortMetadata() {
        return addShortMetadata;
    }


    /**
     * _more_
     *
     * @param user _more_
     */
    public void setUser(User user) {
        if (user != null) {
            userName = user.getId();
        } else {
            userName = null;
        }
    }

    /**
     *  Set the User property.
     *
     *  @param value The new value for User
     */
    public void setUserName(String value) {
        this.userName = value;
    }

    /**
     *  Get the User property.
     *
     *  @return The User
     */
    public String getUserName() {
        return this.userName;
    }




    /**
     * Set the SleepMinutes property.
     *
     * @param value The new value for SleepMinutes
     */
    public void setSleepMinutes(double value) {
        sleepMinutes = value;
    }

    /**
     * Get the SleepMinutes property.
     *
     * @return The SleepMinutes
     */
    public double getSleepMinutes() {
        return sleepMinutes;
    }

    /**
     * Set the Name property.
     *
     * @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * Get the Name property.
     *
     * @return The Name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the IsEditable property.
     *
     * @param value The new value for IsEditable
     */
    public void setIsEditable(boolean value) {
        isEditable = value;
    }

    /**
     * Get the IsEditable property.
     *
     * @return The IsEditable
     */
    public boolean getIsEditable() {
        return isEditable;
    }


    /**
     * _more_
     *
     * @param element _more_
     * @param attr _more_
     *
     * @return _more_
     */
    public List<String> split(Element element, String attr) {
        if ( !MyXmlUtil.hasAttribute(element, attr)) {
            return new ArrayList<String>();
        }

        return Utils.split(MyXmlUtil.getAttribute(element, attr), ",", true,
                           true);
    }



    /**
     * Set the ActiveOnStart property.
     *
     * @param value The new value for ActiveOnStart
     */
    public void setActiveOnStart(boolean value) {
        activeOnStart = value;
    }

    /**
     * Get the ActiveOnStart property.
     *
     * @return The ActiveOnStart
     */
    public boolean getActiveOnStart() {
        return activeOnStart;
    }


    /**
     * Set the GenerateMd5 property.
     *
     * @param value The new value for GenerateMd5
     */
    public void setGenerateMd5(boolean value) {
        generateMd5 = value;
    }

    /**
     * Get the GenerateMd5 property.
     *
     * @return The GenerateMd5
     */
    public boolean getGenerateMd5() {
        return generateMd5;
    }




    /**
     * _more_
     *
     * @param msg _more_
     */
    public void debug(String msg) {
        if (getTestMode()) {
            logHarvesterInfo(msg);
            msg = msg.replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
            msg = msg.replace("\n", "<br>");
            status.append(msg);
            status.append(HtmlUtils.br());
        }
    }


    /**
     * Set the TestMode property.
     *
     * @param value The new value for TestMode
     */
    public void setTestMode(boolean value) {
        testMode = value;
    }

    /**
     * Get the TestMode property.
     *
     * @return The TestMode
     */
    public boolean getTestMode() {
        return testMode;
    }


    /**
     * Set the TestCount property.
     *
     * @param value The new value for TestCount
     */
    public void setTestCount(int value) {
        testCount = value;
    }

    /**
     * Get the TestCount property.
     *
     * @return The TestCount
     */
    public int getTestCount() {
        return testCount;
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public boolean defined(String s) {
        if ((s != null) && (s.length() > 0)) {
            return true;
        }

        return false;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return "harvester:" + getName();
    }

}
