/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.type;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.database.*;

import org.ramadda.repository.map.*;

import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;

import org.ramadda.repository.output.WikiMacro;
import org.ramadda.repository.output.WikiConstants;
import org.ramadda.repository.search.SearchManager;
import org.ramadda.repository.search.SpecialSearch;
import org.ramadda.repository.util.DateArgument;

import org.ramadda.repository.util.FileWriter;
import org.ramadda.repository.util.RequestArgument;
import org.ramadda.repository.util.SelectInfo;

import org.ramadda.service.Service;
import org.ramadda.service.ServiceInput;
import org.ramadda.service.ServiceOutput;
import org.ramadda.util.NamedBuffer;
import org.ramadda.util.FileInfo;
import org.ramadda.util.FormInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.GroupedBuffers;
import org.ramadda.util.geo.GeoUtils;

import org.ramadda.util.IO;
import org.ramadda.util.JQuery;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.SelectionRectangle;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import org.ramadda.util.sql.Clause;
import org.ramadda.util.sql.SqlUtil;

import java.sql.Connection;
import java.net.URL;

import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.awt.geom.Rectangle2D;
import java.io.*;
import java.util.function.Supplier;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;

import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import java.util.TimeZone;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.function.BiConsumer;

/**
 * Provide the core services around the entry types.
 */
@SuppressWarnings("unchecked")
public class TypeHandler extends RepositoryManager {

    public static final XmlUtil XU = null;
    public enum CorpusType {
	LLM,
	SEARCH
    }

    public enum NewType {
	NEW,
	IMPORT,
	COPY,
	CHANGETYPE,
	NONE
    }    

    public static boolean debug = false;

    public static final int COPY_LIMIT = 5000;
    static int xcnt;
    public String myid = "typehandler-" + (xcnt++);

    public static final String FIELD_COLUMNS ="_columns";
    public static final String FIELD_HR = "_hr";
    public static final String FIELD_LABEL = "_label";    
    public static final String FIELD_NAME = "name";
    public static final String FIELD_DESCRIPTION = "description";    
    public static final String FIELD_SNIPPET = "snippet";
    public static final String FIELD_ORDER = "order";
    public static final String FIELD_ICON = "icon";
    public static final String FIELD_FILE = "file";
    public static final String FIELD_DATE = "date";
    public static final String FIELD_CREATEDATE = "createdate";
    public static final String FIELD_CHANGEDATE = "changedate";
    public static final String FIELD_FROMDATE = "fromdate";
    public static final String FIELD_TODATE = "todate";
    public static final String FIELD_DOWNLOADFILE ="downloadfile";

    private String DEFAULT_EDIT_FIELDS  =
        ARG_NAME+"," +  ARG_DESCRIPTION+"," +  ARG_RESOURCE+"," +  ARG_TAGS+"," +  ARG_DATE+"," +  ARG_LOCATION+"," + FIELD_COLUMNS+"," + FIELD_HR+","+FIELD_ORDER;

    private String[] FIELDS_ENTRY = {
        ARG_NAME, ARG_DESCRIPTION, ARG_RESOURCE,/* FIELD_LABEL+":" +HU.span("Metadata",""),*/ ARG_TAGS, ARG_DATE, ARG_LOCATION,FIELD_COLUMNS
	    };

    private String[] FIELDS_NOENTRY = {
        ARG_NAME, ARG_RESOURCE, ARG_DESCRIPTION,  /*ARG_LABEL+":" +HU.span("Metadata",""),*/ARG_TAGS, ARG_DATE, ARG_LOCATION,FIELD_COLUMNS
    };

    public static final String ID_DELIMITER = ":";
    public static final String TARGET_ATTACHMENT = "attachment";
    public static final String TARGET_CHILD = "child";
    public static final String TARGET_SIBLING = "sibling";
    public static final RequestArgument REQUESTARG_FROMDATE =
        new RequestArgument("ramadda.arg.fromdate");

    public static final RequestArgument REQUESTARG_TODATE =
        new RequestArgument("ramadda.arg.todate");

    public static final String CATEGORY_DEFAULT = "Information";
    public static final String TYPE_ANY = Constants.TYPE_ANY;
    public static final String TYPE_GUESS = "guess";
    public static final String TYPE_FINDMATCH = "findmatch";
    public static final String TYPE_FILE = Constants.TYPE_FILE;
    public static final String TYPE_GROUP = Constants.TYPE_GROUP;
    public static final String TYPE_HOMEPAGE = "homepage";
    public static final String TYPE_CONTRIBUTION = "contribution";
    public static final String TAG_COLUMN = "column";
    public static final String TAG_PROPERTY = "property";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_METADATA = "metadata";
    public static final String ATTR_CHILDTYPES = "childtypes";
    public static final String ATTR_PATTERN = "pattern";
    public static final String ATTR_NOTPATTERN = "notpattern";        
    public static final String ATTR_WIKI = "wiki";
    public static final String ATTR_BUBBLE = "bubble";
    public static final String ATTR_WIKI_INNER = "wiki_inner";
    public static final String TAG_CHILDREN = "children";
    public static final String ATTR_VALUE = "value";
    public static final String ATTR_CATEGORY = "category";
    public static final String ATTR_SUPERCATEGORY = "supercategory";
    public static final String TAG_TYPE = "type";
    public static final String TAG_METADATA = "metadata";
    public static final String ATTR_HANDLER = "handler";
    public static final int MATCH_UNKNOWN = 0;
    public static final int MATCH_TRUE = 1;
    public static final int MATCH_FALSE = 2;
    public static String DFLT_WIKI_HEADER =
        "{{name}}\n{{description box.class=\"entry-page-description\"}}";

    public static final String PROP_FIELD_FILE_PATTERN = "field_file_pattern";
    public static final String PROP_INGEST_LINKS = "ingestLinks";
    public static final String ALL = "-all-";
    public static final TwoFacedObject ALL_OBJECT = new TwoFacedObject(ALL,
								       ALL);
    public static final TwoFacedObject NONE_OBJECT =
        new TwoFacedObject("None", "");

    private static List<DateArgument> dateArgs;
    static int cnt = 0;
    int mycnt = cnt++;
    private String type;
    private boolean needsToInitialize = false;
    private Element typeNode;
    private TypeHandler parent;
    private List<TypeHandler> childrenTypes = new ArrayList<TypeHandler>();
    private String description="";
    private String dictionary=null;
    private String editHelp = null;
    private String newHelp = null;
    private String[] editFields;
    private String[] newFields;    
    private List<String> displayFields;
    private List<WikiMacro> wikiMacros;
    private Hashtable<String,WikiMacro> wikiMacrosMap;    
    private List<Utils.Macro> startDateMacros;
    private List<Utils.Macro> endDateMacros;    
    private String embedWiki;
    private String help = "";
    private String iconPath;
    private String mimeType = "unknown";
    Hashtable<String,Column> columnMap;
    private String category;
    private String superCategory = "";
    private Hashtable properties = new Hashtable();
    private SpecialSearch specialSearch;

    /**
     *   the pattern= attribute in types.xml. Used when trying to figure out what entry type
     *   to use for a file
     */
    private String filePattern;
    private String fileNotPattern;    
    private String geoPosition;
    private List<MetadataPattern> metadataPatterns;

    /**
     *   the field_file_pattern attribute in types.xml. Used when trying to figure out what entry type
     *   to use for a file and to set the entry values from
     */
    private Pattern fieldFilePattern;
    private List<String> fieldPatternNames;
    /** the wiki tag in types.xml. If defined then use this as the default html display for entries of this type */
    private String wikiTemplate;
    private String nameTemplate;
    private Hashtable<String,String> wikiText = new Hashtable<String,String>();
    private String defaultChildrenEntries;
    private String wikiTemplateInner;
    private DecimalFormat latLonFormat = new DecimalFormat("##0.00");
    private String defaultCategory;
    private String displayTemplatePath;

    private boolean flushedFromCache = false;
    /** Should users be shown this type when doing a New Entry... */
    private boolean forUser = true;
    private boolean canCreate = true;
    private boolean isSynthType = false;    
    private boolean adminOnly = false;
    private boolean isGroup = false;

    /** can be set for abstract types */
    private boolean includeInSearch = false;

    private Boolean canCache;

    /** Default metadata types to show in Edit->Add Property menu */
    private List<String> metadataTypes=new ArrayList<String>();

    /** This holds this types plus the parent types */
    private List<String> allMetadataTypes;

    /** The default child entry types to show in the File->New menu */
    private List<String> childTypes;

    private List<String[]> requiredMetadata = new ArrayList<String[]>();
    private List<Service> services = new ArrayList<Service>();
    private Entry synthTopLevelEntry;
    private int priority=999;
    private List<Action> actions = new ArrayList<Action>();
    private     Hashtable<String,Action> actionMap = new Hashtable<String,Action>();

    public TypeHandler(Repository repository) {
        super(repository);
    }

    public TypeHandler(Repository repository, Element entryNode) {
        this(repository);
        if (entryNode != null) {
            initTypeHandler(entryNode);
        }
    }

    public TypeHandler(Repository repository, String type) {
        this(repository, type, "");
    }

    public TypeHandler(Repository repository, String type,
                       String description) {
        this(repository, type, description, CATEGORY_DEFAULT);
    }

    public TypeHandler(Repository repository, String type,
                       String description, String category) {
        super(repository);
        this.type        = type;
        this.description = description;
        if (category != null) {
            this.category = category;
        }
    }

    public boolean isNew(NewType newType) {
	return newType==NewType.NEW;
    }

    public void setParentTypeHandler(TypeHandler parent) {
        this.parent = parent;
    }

    public static void printAttrs(Node node,boolean doParent) {
	if(node==null) return;
	if(doParent)
	    printAttrs(node.getParentNode(),doParent);
	NamedNodeMap attrs = node.getAttributes();
	if(attrs!=null) {
	    for (int i = 0; i < attrs.getLength(); i++) {
		Attr attr = (Attr) attrs.item(i);
		String name = attr.getNodeName();
		String value =  attr.getNodeValue();
		if(seenProps.contains(name)) continue;
		seenProps.add(name);
		System.out.println(name+"=\""+ value+"\"");
	    }
	}
    }

    public boolean getNeedsToInitialize() {
	return needsToInitialize;
    }

    public void initTypeHandler() {
	if(typeNode ==null) {
	    throw new IllegalArgumentException("Cannot initialize. No typehandler XML node");
	}
	initTypeHandler(typeNode);
    }

    public void initTypeHandler(Element node) {
        try {
            setType(Utils.getAttributeOrTag(node, ATTR_DB_NAME, (type == null)
					    ? ""
					    : type));

	    /*
	      If there is supposed to be a parent type and we can't  find it then mark this TypeHandler
	      as in need of initialization. The Repository checks all of the type handlers after the initial load
	      and reinitializes the ones in need of initialization
	     */

	    needsToInitialize = false;
	    typeNode = null;
	    String superType = Utils.getAttributeOrTag(node, ATTR_SUPER,  (String) null);
            if (superType != null) {
                parent = getRepository().getTypeHandler(superType);
                if (parent == null) {
		    needsToInitialize = true;
		    typeNode = node;
		    return;
		    //                    throw new IllegalArgumentException("Cannot find parent type:" + superType);
                }
            }

            displayTemplatePath = Utils.getAttributeOrTag(node,
							  "displaytemplate", displayTemplatePath);

	    //printAttrs(node,true);
            iconPath = XmlUtil.getAttributeFromTree(node, "icon",  iconPath);
            priority    = XmlUtil.getAttributeFromTree(node, "priority", priority);
            description = Utils.getAttributeOrTag(node, "description", description);
            filePattern = Utils.getAttributeOrTag(node, ATTR_PATTERN, filePattern);

	    List nodes = XmlUtil.findChildren(node, "filename_metadata");
	    for (int i = 0; i < nodes.size(); i++) {
		Element pnode= (Element) nodes.get(i);
		if(metadataPatterns==null)
		    metadataPatterns= new ArrayList<MetadataPattern>();
		MetadataPattern mp = new MetadataPattern(
							 XU.getAttribute(pnode,"pattern",(String)null),
							 XU.getAttribute(pnode,"column",(String)null),
							 XU.getAttribute(pnode,"metadata_type",(String)null));
		metadataPatterns.add(mp);
	    }

	    //	    if(stringDefined(filePattern))System.err.println(filePattern);
            fileNotPattern = Utils.getAttributeOrTag(node, ATTR_NOTPATTERN, null);	    
            help     = Utils.getAttributeOrTag(node, "help", help);
            dictionary = Utils.getAttributeOrTag(node, "data_dictionary", null);
	    String help =  Utils.getAttributeOrTag(node, "help", null);
            editHelp = Utils.getAttributeOrTag(node, "edithelp", help);
	    if(editHelp!=null)
		editHelp = editHelp.replace("\\n","\n");
            newHelp = Utils.getAttributeOrTag(node, "newhelp", editHelp);	    
	    if(newHelp!=null)
		newHelp = newHelp.replace("\\n","\n");
            mimeType     = XmlUtil.getAttributeFromTree(node, "mimetype", mimeType);	    

	    superCategory = XmlUtil.getAttributeFromTree(node,
							 ATTR_SUPERCATEGORY, superCategory);

	    category = Utils.getAttributeOrTag(node, ATTR_CATEGORY, null);
	    if (category == null) {
		category = XmlUtil.getAttributeFromTree(node, ATTR_CATEGORY,
							CATEGORY_DEFAULT);
	    }

            String tmp = Utils.getAttributeOrTag(node,
						 PROP_FIELD_FILE_PATTERN, (String) null);
            if (tmp != null) {
                this.fieldPatternNames = new ArrayList<String>();
                this.filePattern = Utils.extractPatternNames(tmp,
							     fieldPatternNames);
                //                System.out.println("File pattern:" + filePattern);
                this.fieldFilePattern = Pattern.compile(this.filePattern);
            }

            defaultChildrenEntries = Utils.getAttributeOrTag(node,TAG_CHILDREN, defaultChildrenEntries);

            List metadataNodes = XmlUtil.findChildren(node, TAG_METADATA);
            for (int i = 0; i < metadataNodes.size(); i++) {
                Element metadataNode = (Element) metadataNodes.get(i);
                requiredMetadata.add(new String[] {
			XmlUtil.getAttribute(metadataNode, ATTR_ID),
			XmlUtil.getAttribute(metadataNode, "label",
					     (String) null) });
            }

            List serviceNodes = XmlUtil.findChildren(node,
						     Service.TAG_SERVICE);
            for (int i = 0; i < serviceNodes.size(); i++) {
                Element serviceNode = (Element) serviceNodes.get(i);
                services.add(new Service(getRepository(), serviceNode));
            }

            setProperties(node);

	    String metadata = getAttributeOrProperty(node,ATTR_METADATA);
	    if(metadata!=null) {
		for(String mtd: Utils.split(metadata,",",true,true)) {
		    if(!metadataTypes.contains(mtd)) metadataTypes.add(mtd);
		}
	    }
	    if(metadataTypes.size()==0) {
		metadataTypes = makeInitialMetadataTypes();
	    }  else {
		for(String dflt:new String[]{"content.thumbnail","content.alias"}) {
		    if(!metadataTypes.contains(dflt)) metadataTypes.add(dflt);
		}
	    }
            childTypes = Utils.split(Utils.getAttributeOrTag(node,
							     ATTR_CHILDTYPES, ""),",",true,true);

            nameTemplate = Utils.getAttributeOrTag(node, "nametemplate",null);
            wikiTemplate = Utils.trimLinesLeft(Utils.getAttributeOrTag(node, ATTR_WIKI,wikiTemplate,true));

	    List macros = XmlUtil.findChildrenRecurseUp(node,"wikimacro");
	    for (int i = 0; i < macros.size(); i++) {
		Element macro= (Element) macros.get(i);
		if(wikiMacros==null)  {
		    wikiMacrosMap = new Hashtable<String,WikiMacro>();
		    wikiMacros= new ArrayList<WikiMacro>();
		}

		WikiMacro m = new WikiMacro(macro);
		if(m.isOutput()) {
		    List<String> types = new ArrayList<String>();
		    types.add(getType());
		    getRepository().addOutputHandler(
						     new TemplateOutputHandler(getRepository(),
									       m.getName(),
									       m.getLabel(),
									       types,
									       m.getWikiText(),m.getIcon()));
		} 
		wikiMacrosMap.put(m.getName(),m);
		wikiMacros.add(m);
	    }

	    List wikis = XmlUtil.findChildrenRecurseUp(node,"wikis");
	    for (int i = 0; i < wikis.size(); i++) {
		Element wiki = (Element) wikis.get(i);
		String tag =XmlUtil.getAttribute(wiki,"tag");
		String text = XmlUtil.getChildText(wiki);
		if(text!=null)
		    wikiText.put(tag,Utils.trimLinesLeft(text));
		else
		    System.err.println("No text in wiki tag:" + XmlUtil.toString(wiki));
	    }

            wikiTemplateInner = Utils.trimLinesLeft(Utils.getAttributeOrTag(node, ATTR_WIKI_INNER, wikiTemplateInner));

            includeInSearch = Utils.getAttributeOrTag(node,
						      "includeInSearch", includeInSearch);

            forUser = Utils.getAttributeOrTag(node, ATTR_FORUSER,
					      XmlUtil.getAttributeFromTree(node, ATTR_FORUSER,
									   forUser));
            isSynthType = Utils.getAttributeOrTag(node, "issynth",
						  XmlUtil.getAttributeFromTree(node, "issynth",false));
            adminOnly = Utils.getAttributeOrTag(node, "adminonly",
						XmlUtil.getAttributeFromTree(node, "adminonly", false));
            isGroup = Utils.getAttributeOrTag(node, "isgroup",
					      XmlUtil.getAttributeFromTree(node, "isgroup", isGroup));

	    embedWiki = XmlUtil.getGrandChildText(node,"embedwiki",null);
            String tmpCanCache = Utils.getAttributeOrTag(node, "canCache",
							 XmlUtil.getAttributeFromTree(node,
										      "canCache", (String) null));

            if (tmpCanCache != null) {
                canCache = Boolean.valueOf(tmpCanCache.equals("tmpCanCache"));
            }

	    String dfltFields = getAttributeOrProperty(node, "fields");	    
	    String fields = getAttributeOrProperty(node, "editfields");	    
	    if(fields==null) fields=dfltFields;
	    if(fields!=null) {
		fields = fields.replace("_default",DEFAULT_EDIT_FIELDS);
		editFields = Utils.toStringArray(Utils.split(fields,",",true,true));
	    }
	    fields = getAttributeOrProperty(node, "newfields");	    
	    if(fields==null) fields=dfltFields;
	    if(fields!=null) {
		fields = fields.replace("_default",DEFAULT_EDIT_FIELDS);
		newFields = Utils.toStringArray(Utils.split(fields,",",true,true));
	    }
	    fields = getAttributeOrProperty(node, "displayfields");	    
	    if(fields==null) fields=dfltFields;
	    if(fields!=null) {
		displayFields = Utils.split(fields,",",true,true);
	    }	    

            if ( !Utils.stringDefined(description)) {
                setDescription(Utils.getAttributeOrTag(node,
						       ATTR_DB_DESCRIPTION, getType()));
            }

            String llf = getTypeProperty("location.format", (String) null);
            if (llf != null) {
                latLonFormat = new DecimalFormat(llf);
            }

	    //Action(String id, String label, String icon,boolean forUser,boolean canEdit,String category) {
	    if(parent==null) {
		addAction(new Action("entryllm","Entry LLM","/icons/chatbot.png",true,false,false,"view"));
	    }

            List actionNodes = XmlUtil.findChildren(node, "action");
            for (int i = 0; i < actionNodes.size(); i++) {
                Element actionNode = (Element) actionNodes.get(i);
		addAction(new Action(
				     XmlUtil.getAttribute(actionNode, "name"),
				     XmlUtil.getAttribute(actionNode, "label"),
				     XmlUtil.getAttribute(actionNode, "icon",ICON_EDIT),
				     XmlUtil.getAttribute(actionNode, "foruser","false").equals("true"),
				     XmlUtil.getAttribute(actionNode, "foradmin","false").equals("true"),
				     XmlUtil.getAttribute(actionNode, "canedit","false").equals("true"),
				     XmlUtil.getAttribute(actionNode, "category","file")));
            }
            List wikiViewNodes = XmlUtil.findChildren(node, "wikiview");
            for (int i = 0; i < wikiViewNodes.size(); i++) {
                Element actionNode = (Element) wikiViewNodes.get(i);
		addAction(new Action(
				     XmlUtil.getAttribute(actionNode, "name"),
				     XmlUtil.getAttribute(actionNode, "label"),
				     XmlUtil.getAttribute(actionNode, "icon", getIconProperty(ICON_WIKI)),
				     XmlUtil.getChildText(actionNode)));
            }	    

	    String  startDateTemplate = getTypeProperty("startdate.template",null);
	    if(startDateTemplate!=null)  {
		startDateMacros = Utils.splitMacros(startDateTemplate);
	    }
	    String  endDateTemplate = getTypeProperty("enddate.template",null);
	    if(endDateTemplate!=null)  {
		endDateMacros = Utils.splitMacros(endDateTemplate);
	    }	    

	    geoPosition = getTypeProperty("form.geoposition",null);

	    if(parent!=null) {
                parent.checkAncestorTypes(getType());
                parent.addChildTypeHandler(this);
	    }

        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    private String getAttributeOrProperty(Element node, String attr) {
	String value = XmlUtil.getAttributeFromTree(node, attr, null);	    
	if(value==null) {
	    value = getTypeProperty(attr,null);
	}
	return value;
    }

    public boolean applyEditCommand(Request request,Entry entry, String command,String...args) throws Exception {
	return false;
    }

    public boolean addThumbnail(Request request, Entry entry, boolean deleteExisting) throws Exception {
	if(!entry.isImage()) return false;
	return getRepository().getMetadataManager().addThumbnail(request,entry,deleteExisting);
    }

    public void addAction(Action action) {
	if(action.getId().equals("documentchat") ||
	   action.getId().equals("entryllm") ||
	   action.getId().equals("applyllm")) {
	    if(!getRepository().getLLMManager().isLLMEnabled()) {
		return;
	    }
	}
	actions.add(action);
	actionMap.put(action.id,action);
    }

    public Object getWikiProperty(Request request,Entry entry, String id, Hashtable props)  {
	return  entry.getValue(request,id,true);
    }

    public void getWikiTags(List<String[]> tags, Entry entry) {
        if(this.parent!=null) {
	    this.parent.getWikiTags(tags, entry);
	}
    }

    public List<WikiMacro>getWikiMacros() {
	List<WikiMacro> macros=null;
	if(parent!=null) {
	    macros = parent.getWikiMacros();
	}
	if(wikiMacros!=null) {
	    if(macros!=null)
		macros.addAll(wikiMacros);
	    else
		macros = wikiMacros;
	}
	if(macros!=null)
	    return new ArrayList<WikiMacro>(macros);
	return null;
    }

    public WikiMacro getWikiMacro(Entry entry, String name) {
	WikiMacro macro = null;
	if(wikiMacrosMap!=null) 
	    macro  = wikiMacrosMap.get(name);
	if(macro!=null) return macro;
	if(this.parent!=null) return this.parent.getWikiMacro(entry, name);
	return null;
    }

    public WikiMacro getWikiMacroTag(Entry entry, String tag) {
	if(wikiMacros!=null) {
	    for(WikiMacro macro: wikiMacros) {
		if(macro.hasTag(tag)) return macro;
	    }

	}
	if(this.parent!=null) return this.parent.getWikiMacroTag(entry, tag);
	return null;
    }

    public boolean hasSearchDisplayText(Request request, Entry entry) throws Exception {
	String name = getTypeProperty("search.wikimacro",null);
	if(name!=null) {
	    WikiMacro macro = getWikiMacro(entry,name);
	    if(macro!=null) {
		return true;
	    }
	}
	return false;
    }

    public String getEmbedWiki(Request request, Entry entry) {
	if(embedWiki!=null) return embedWiki;
        if (getParent() != null) return getParent().getEmbedWiki(request, entry);
	return "{{information details=true}} {{tabletree  message=\"\"}}";
    }

    public String getSearchDisplayText(Request request, Entry entry) throws Exception {
	String name = getTypeProperty("search.wikimacro",null);
	if(name!=null) {
	    WikiMacro macro = getWikiMacro(entry,name);
	    if(macro!=null) {
		String s= getWikiManager().wikifyEntry(request,entry,macro.getWikiText());
		return s;
	    }
	}
	return null;
    }

    public void childrenChanged(Entry entry,boolean isNew) {
    }

    private void checkAncestorTypes(String type) {
        if (type.equals(getType())) {
            throw new IllegalStateException(
					    "Detected cycle in type handlers:" + type);
        }
        if (getParent() != null) {
            getParent().checkAncestorTypes(type);
        }
    }

    public List<String> makeInitialMetadataTypes() {
	return Utils.split(EnumeratedMetadataHandler.TYPE_TAG + ","
			   + ContentMetadataHandler.TYPE_THUMBNAIL + ","
			   + ContentMetadataHandler.TYPE_ALIAS,",");
    }

    public List<String> getMetadataTypes() {
	if(allMetadataTypes==null) {
	    allMetadataTypes = new ArrayList<String>();
	    List<String> types = getMetadataTypesInner();
	    if(types!=null) Utils.addAllUnique(allMetadataTypes,types);
	    if(parent!=null) {
		List<String> parentTypes = parent.getMetadataTypes();
		if(parentTypes!=null) Utils.addAllUnique(allMetadataTypes,parentTypes);
	    }
	}
	return allMetadataTypes;
    }

    private List<String> getMetadataTypesInner() {
        if (metadataTypes == null) {
            metadataTypes = makeInitialMetadataTypes();
        }
	if(metadataTypes.size()==0) {
            metadataTypes = makeInitialMetadataTypes();
	}

        return metadataTypes;
    }

    public String getDescriptionCorpus(Entry entry) throws Exception {
	Appendable sb = new StringBuilder();
	//False->don't include the column values, don't include metadata
	getTextCorpus(entry, sb,false,false);
	return sb.toString();
    }

    public void getTextCorpus(Entry entry, Appendable sb, boolean...args) throws Exception {
        if (getParent() != null) {
            getParent().getTextCorpus(entry, sb,args);
        } else {
            sb.append(entry.getDescription());
            sb.append("\n");
	    if(args.length>=2 && args[1]) {
		getMetadataManager().getTextCorpus(getRepository().getAdminRequest(),entry,sb);
	    }
        }
    }

    /**
     * Called by lucene index to get non file contents
     *
     * @param entry _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */

    public void getTextContents(Entry entry, StringBuilder sb)
	throws Exception {}

    public boolean getCanCache(Entry entry) {
        if (canCache != null) {
            return canCache;
        }
        if (getParent() != null) {
            return getParent().getCanCache(entry);
        }

        return true;
    }

    public String getMediaUrl(Request request, Entry entry) throws Exception {
        List<Column> columns = getColumns();
        if (columns != null) {
            for (Column column : columns) {
                if (column.isMediaUrl()) {
                    String url = entry.getStringValue(request, column,null);
                    if (Utils.stringDefined(url)) {
                        return url;
                    }

                    break;
                }
            }
        }

        return getEntryResourceUrl(request, entry);
    }

    public String getJson(Request request) throws Exception {
        List<String> items = new ArrayList<String>();
        items.add("id");
        items.add(JsonUtil.quote(getType()));
        items.add("entryCount");
        int cnt = getEntryUtil().getEntryCount(this);
        items.add("" + cnt);
        items.add("label");
        items.add(JsonUtil.quote(getLabel()));
        items.add("includeInSearch");
        items.add(JsonUtil.quote("" + getIncludeInSearch()));
        items.add("isgroup");
        items.add("" + isGroup());
	String bubble = getBubbleTemplate(request, null);
	if(bubble!=null) {
	    items.add("mapwiki");
	    items.add(JU.quote(bubble));
	}

        List<String> cols    = new ArrayList<String>();
        List<Column> columns = getColumns();
        if (columns != null) {
            for (Column column : columns) {
		String json = column.getJson(request,this);
		if(json!=null)
		    cols.add(json);
            }
        }
        items.add("columns");
        items.add(JsonUtil.list(cols));

        String icon = request.getAbsoluteUrl(
					     getIconUrl(getIconProperty(ICON_FOLDER_CLOSED)));
        items.add("icon");
        items.add(JsonUtil.quote(icon));
        items.add("category");
        items.add(JsonUtil.quote(getCategory()));

        return JsonUtil.map(items);
    }

    public void addToJson(Request request, Entry entry, List<String> items,
                          List<String> attrs)
	throws Exception {}

    public String getUrlForWiki(Request request, Entry entry, String tag,
                                Hashtable props, List<String> topProps) {
        return null;
    }

    public String getWikiTemplate(Request request, Entry entry)
	throws Exception {
        if (wikiTemplate != null) {
            return wikiTemplate;
        }
        if (getParent() != null) {
            return getParent().getWikiTemplate(request, entry);
        }

        return null;
    }

    public String getWikiTemplate(Request request)
	throws Exception {
        if (wikiTemplate != null) {
            return wikiTemplate;
        }
        if (getParent() != null) {
            return getParent().getWikiTemplate(request);
        }

        return null;
    }

    public void addWikiProperties(Entry entry, WikiUtil wikiUtil, String tag,
                                  Hashtable props) {}

    public String getBubbleTemplate(Request request, Entry entry)
	throws Exception {
	return getBubbleTemplate(request, entry, true);
    }

    private String getBubbleTemplate(Request request, Entry entry, boolean checkMetadata)
	throws Exception {	

	if(entry!=null && checkMetadata) {
	    List<Metadata> metadataList =
		getMetadataManager().findMetadata(request, entry,
						  "content.mapbubble", true);
	    if (metadataList != null) {
		//type-1, apply to this-2, name pattern - 3, wiki-4
		Metadata theMetadata = null;
		for (Metadata metadata : metadataList) {
		    if (Misc.equals(metadata.getAttr(2), "false")) {
			if (metadata.getEntryId().equals(entry.getId())) {
			    continue;
			}
		    }
		    String types = metadata.getAttr(1);
		    if ((types == null) || (types.trim().length() == 0)) {
			theMetadata = metadata;

			break;
		    }
		    for (String type : Utils.split(types, ",", true, true)) {
			if (type.equals("file") && !entry.isGroup()) {
			    theMetadata = metadata;

			    break;
			}
			if (type.equals("folder") && entry.isGroup()) {
			    theMetadata = metadata;

			    break;
			}
			if (entry.getTypeHandler().isType(type)) {
			    theMetadata = metadata;

			    break;
			}
		    }
		}

		if (theMetadata != null) {
		    return theMetadata.getAttr(4);

		}
	    }
	}

	String bubbleTemplate = getProperty(entry, "bubble",null);
        if (bubbleTemplate == null) {
	     bubbleTemplate = getProperty(entry, "map.popup",null);
	}

        if (bubbleTemplate != null) {
            return bubbleTemplate;
        }
        if (getParent() != null) {
            return getParent().getBubbleTemplate(request, entry,false);
        }

        return null;
    }

    public String preProcessWikiText(Request request, Entry entry,
                                     String wikiText) {
        return wikiText;
    }

    public String getWikiTemplateInner() {
        return wikiTemplateInner;
    }

    public void  setWikiTemplate(String wiki) {
	wikiTemplate=wiki;
    }

    public int getTotalNumberOfValues() {
        int cnt = getNumberOfMyValues();
        if (parent != null) {
            cnt += parent.getTotalNumberOfValues();
        }

        return cnt;
    }

    public int getNumberOfMyValues() {
        return 0;
    }

    public Object[] getEntryValues(Entry entry) {
        Object[] values = entry.getValues();
        if (values == null) {
            values = this.makeEntryValues(new Hashtable());
            entry.setValues(values);
        }

        return values;
    }

    public int getValueIndex(String name) {
        for (Column column : getColumns()) {
            if (column.getName().equalsIgnoreCase(name)) {
                return column.getColumnIndex();
            }
        }

        return -1;
    }

    public void setEntryValue(Entry entry, int index, Object value) {
        getEntryValues(entry)[index] = value;
    }

    public int getValuesOffset() {
        if (parent != null) {
            return parent.getTotalNumberOfValues();
        }

        return 0;
    }

    public List<Comment> getComments(Request request, Entry entry)
	throws Exception {
        return null;
    }

    public   Entry.EntryHistory createHistory(Entry entry) {
	return new  Entry.EntryHistory(entry);
    }

    public int getDefaultQueryLimit(Request request, Entry entry) {
        return getRepository().getDefaultMaxEntries();
    }

    public void getTableNames(List<String> tableNames) {
        String tableName = getTableName();
        if (stringDefined(tableName) &&  !tableNames.contains(tableName)) {
            tableNames.add(tableName);
        }
        //        for(TypeHandler child: childrenTypes) {
        //            child.getTableNames(tableNames);
        //        }
        if (getParent() != null) {
            getParent().getTableNames(tableNames);
        }
    }

    public List<String> getDefaultChildrenTypes() {
	return childTypes;
    }

    public void getChildTypes(List<String> types) {
        if ( !types.contains(getType())) {
            types.add(getType());
        }
        for (TypeHandler child : childrenTypes) {
            child.getChildTypes(types);
        }
    }

    public List<TypeHandler> getChildrenTypes() {
	return childrenTypes;
    }

    public void addChildTypeHandler(TypeHandler child) {
        if ( !childrenTypes.contains(child)) {
            childrenTypes.add(child);
        }
    }

    public TypeHandler getParent() {
        return parent;
    }

    public TypeHandler getTypeHandlerForCopy(Entry entry) throws Exception {
	if(isSynthType()) {
	    if(entry.getTypeHandler().isSynthType()) {
		if(entry.isGroup()) {
		    return getRepository().getGroupTypeHandler();
		} else {
		    return getRepository().getFileTypeHandler();
		}
	    }
	    return entry.getTypeHandler();
	}
        return this;
    }

    public Resource getResourceForCopy(Request request, Entry oldEntry,
                                       Entry newEntry)
	throws Exception {
        Resource newResource = new Resource(oldEntry.getResource());
        if (newResource.isFile()) {
            String newFileName =
                getStorageManager().getFileTail(
						getStorageManager().getOriginalFilename(oldEntry.getResource().getTheFile().getName()));
            String newFile =
                getStorageManager().copyToStorage(
						  request, oldEntry.getTypeHandler().getResourceInputStream(
													    oldEntry), newFileName).toString();
	    newResource = new Resource(newFile, Resource.TYPE_STOREDFILE);
        }

        return newResource;
    }

    public Rectangle2D.Double getBounds(Request request,Entry entry, Rectangle2D.Double bounds) {
	if (entry.hasAreaDefined(request) || entry.hasLocationDefined(request)) {
	    if (bounds == null) {
		bounds = entry.getBounds(request);
	    } else {
		bounds.add(entry.getBounds(request));
	    }
	}
	List<Column> columns = getColumns();
	if (columns != null) {
	    for(Column column: columns) {
		if(!column.isLatLon()) continue;
		double[] latlon = column.getLatLon(request,entry);
		if(!Double.isNaN(latlon[0]) && !Double.isNaN(latlon[1])) {
		    if(bounds==null)
			bounds = new Rectangle2D.Double();
		    bounds.add(latlon[1],latlon[0]);
		}
	    }
	}
	return bounds;
    }

    public boolean addToMap(Request request, Entry entry, MapInfo map)
	throws Exception {
        if (parent != null) {
            return parent.addToMap(request, entry, map);
        }
	List<Column> columns = getColumns();
	if (columns == null) {
	    return true;
	}
	boolean didOne=false;
	for(Column column: columns) {
	    if(column.isLatLon()) {
		double[] latlon = column.getLatLon(request,entry);
		if(!Double.isNaN(latlon[0]) && !Double.isNaN(latlon[1])) {
		    String icon  = getTypeProperty("column.icon",getTypeProperty(column.getName()+".column.icon",null));
		    String info  = getMapManager().encodeText(getMapManager().makeInfoBubble(request,entry,column.getLabel(),"<hr class=ramadda-hr>"));

		    map.addMarker(entry.getId()+"_"+column.getName(),latlon[0], latlon[1], icon,
				  column.getName(),info);
		    didOne=true;
		}
	    }
	}
	if(didOne)	map.center();
	return true;
    }

    public boolean shouldShowPolygonInMap() {
        return false;
    }

    public boolean addToMapSelector(Request request, Entry entry, Entry forEntry, MapInfo map)
	throws Exception {
        return true;
    }

    public String getEntryText(Entry entry) {
        return entry.getDescription();
    }

    public String getExtraText(Entry entry) {
        return null;
    }

    public String getTextForWiki(Request request, Entry entry,
                                 Hashtable properties)
	throws Exception {
        return entry.getDescription();
    }

    public void childEntryChanged(Request request,Entry entry, boolean isNew)
	throws Exception {}

    public void metadataChanged(Request request, Entry entry)
	throws Exception {}

    public String getTypePermissionName(String type) {
	if(true) return null;
        if (type.equals(Permission.ACTION_TYPE1)) {
            return "Type specific 1";
        }

        return "Type specific 2";
    }

    public void handleNoEntriesHtml(Request request, Entry entry,
                                    Appendable sb) {
        if ( !Utils.stringDefined(entry.getDescription())
	     && getType().equals(TYPE_GROUP)) {
            Utils.append(sb,
                         HU.tag(HU.TAG_I, "",
				msg(LABEL_EMPTY_FOLDER)));
        }
    }

    public boolean shouldExportTable(String tableName) {
        return true;
    }

    public void initAfterDatabaseImport() throws Exception {}

    public void clearCache() {
        columnEnumValues = new Hashtable<String, HashSet>();
    }

    public InputStream getResourceInputStream(Entry entry) throws Exception {
        return new BufferedInputStream(
				       getStorageManager().getFileInputStream(getFileForEntry(entry)));
    }

    public Result getHtmlDisplay(Request request, Entry entry)
	throws Exception {
        if (parent != null) {
            return parent.getHtmlDisplay(request, entry);
        }

        return null;
    }

    public String getInnerWikiContent(Request request, Entry entry,
                                      String wikiTemplate)
	throws Exception {
        return null;
    }

    public Result processEntryAccess(Request request, Entry entry)
	throws Exception {
        return new Result("Error",
                          new StringBuilder("Entry access not defined"));
    }

    public String getEntryActionUrl(Request request, Entry entry,
                                    String action)
	throws Exception {
        return request.makeUrl(getRepository().URL_ENTRY_ACTION, ARG_ENTRYID,
                               entry.getId(), ARG_ACTION, action);

    }

    public Result processEntryAction(Request request, Entry entry)
	throws Exception {
        String action = request.getString("action", "");
	try {
	    if (action.equals("entryllm") || action.equals("documentchat")) {
		return getLLMManager().processDocumentChat(request,entry,action.equals("documentchat"));
	    }

	    if (action.equals("applyllm")) {
		return getLLMManager().applyLLM(request,entry);
	    }
	} catch(Exception exc) {
	    StringBuilder sb = new StringBuilder();
	    getPageHandler().entrySectionOpen(request, entry, sb, "Error applying LLM");
	    sb.append(getPageHandler().showDialogError(exc.getMessage()));
	    getLogManager().logError("Applying LLM:"+ entry,exc);
	    getPageHandler().entrySectionClose(request, entry, sb);
	    return new Result("LLM Error", sb);
	}

	Action a = actionMap.get(action);
	if(a!=null && a.wikiText!=null) {
	    StringBuilder sb = new StringBuilder();
	    sb.append(getWikiManager().wikifyEntry(request, entry,a.wikiText));
	    return getEntryManager().addEntryHeader(request, entry,
						    new Result(a.label,sb));
	}

        if (parent != null) {
            return parent.processEntryAction(request, entry);
        }

        StringBuilder sb = new StringBuilder();
        getPageHandler().entrySectionOpen(request, entry, sb, "");
        sb.append(getPageHandler().showDialogError("Unknown entry action"));
        getPageHandler().entrySectionClose(request, entry, sb);

        return new Result("Error", sb);
    }

    public void addToInformationTabs(Request request, Entry entry,
                                     List<String> tabTitles,
                                     List<String> tabContents) {}

    public boolean isDefaultHtmlOutput(Request request) {
        return Misc.equals(
			   OutputHandler.OUTPUT_HTML.getId(),
			   request.getString(ARG_OUTPUT, OutputHandler.OUTPUT_HTML.getId()));
    }

    public String applyMacros(Request request, Entry entry, List<Utils.Macro> macros,Object[]values,
			      Object value,String s) throws Exception {
	if(macros==null) return s;
	StringBuilder tmp = new StringBuilder();
	for(Utils.Macro macro: macros) {
	    if(macro.isText()) {
		tmp.append(macro.getText());
		continue;
	    }
	    if(macro.getId().equals("value")) {
		String dateFormat = macro.getProperty("dateFormat",null);
		String prefix = macro.getProperty("prefix",null);
		String suffix = macro.getProperty("suffix",null);
		String unit = macro.getProperty("unit",null);				
		boolean showMissing = macro.getProperty("showMissing",true);
		double scale =  macro.getProperty("scale",Double.NaN);
		double offset1 =  macro.getProperty("offset1",Double.NaN);
		double offset2 =  macro.getProperty("offset2",Double.NaN);				
		int decimals = macro.getProperty("decimals",-1);
		if(!Double.isNaN(offset1)) {
		    String sv = value!=null?value.toString():null;
		    double v = stringDefined(sv)?Double.parseDouble(sv):Double.NaN;
		    value = new Double(v+offset1);
		    s = value.toString();
		}

		if(!Double.isNaN(scale)) {
		    String sv = value!=null?value.toString():null;
		    double v = stringDefined(sv)?Double.parseDouble(sv):Double.NaN;
		    value = new Double(v*scale);
		    s = value.toString();
		}		    

		if(!Double.isNaN(offset2)) {
		    String sv = value!=null?value.toString():null;
		    double v = stringDefined(sv)?Double.parseDouble(sv):Double.NaN;
		    value = new Double(v+offset2);
		    s = value.toString();
		}

		if(!showMissing && value!=null) {
		    if(value instanceof Double) {
			double d = (Double) value;
			if(Double.isNaN(d)) continue;
		    }
		}

		if(decimals>=0) {
		    if(value instanceof Double) {
			double d = (Double) value;
			value  =new Double(Utils.decimals(d,decimals));
			s = value.toString();
		    }
		}		

		if(prefix!=null) tmp.append(prefix);

		if(dateFormat!=null) {
		    SimpleDateFormat sdf2 = RepositoryUtil.makeDateFormat(dateFormat);
		    Date d = (Date) value;
		    if(d!=null) tmp.append(sdf2.format(d));
		} else if(macro.getProperty("showAge",false)) {
		    Date d = (Date) value;
		    if(d!=null) {
			Date startDate = null;
			String otherDate = macro.getProperty("otherDate",null);
			if(otherDate!=null) {
			    Column otherColumn = findColumn(otherDate);
			    if(otherColumn!=null) {
				if(otherColumn.equals("startdate")) 
				    startDate  = new Date(entry.getStartDate());
				else if(otherColumn.equals("enddate")) 
				    startDate  = new Date(entry.getEndDate());
				else if(otherColumn.equals("createdate")) 
				    startDate  = new Date(entry.getCreateDate());
				else
				    startDate = (Date) entry.getValue(request,otherColumn);
			    } else {
				return "Could not find other column:" + otherColumn;
			    }
			} else {
			    startDate  = new Date(entry.getStartDate());
			}
			startDate = DateHandler.checkDate(startDate);
			if(startDate!=null) {
			    int years = DateHandler.getYearsBetween(startDate,d);
			    tmp.append(macro.getProperty("prefix",""));
			    tmp.append(years);
			    tmp.append(macro.getProperty("suffix"," years"));	
			}
		    }
		} else if(macro.getProperty("inchesToFeet",false)) {
		    if(value==null) value="0";
		    int i = Integer.parseInt(value.toString());
		    if(i==0) {
			tmp.append("NA");
		    } else {
			int feet= i/12;
			int inches = i%12;
			tmp.append(feet+"' ");
			tmp.append(inches+"\"");			    
			tmp.append(" ("+ i+" inches)");
		    }
		} else {
		    tmp.append(s);
		}

		if(unit!=null&& value!=null) {
		    boolean ok = true;
		    if(value instanceof Double) {
			double d = (Double) value;
			if(Double.isNaN(d)) ok  =false;
		    }
		    if(ok) {
			tmp.append(" ");
			tmp.append(unit);
		    }
		}

		if(suffix!=null) tmp.append(suffix);
	    } else {
		Column otherColumn = findColumn(macro.getId());
		if(otherColumn!=null && values!=null) {
		    //the true=> don't apply macros
		    tmp.append(otherColumn.formatValue(request,entry,values,true));
		    continue;
		}
		System.err.println("unknown macro:" + macro.getId());
	    }
	}
	return tmp.toString();
    }

    public Result xgetHtmlDisplay(Request request, Entry group,
				  List<Entry> children)
	throws Exception {

        if (parent != null) {
            return parent.xgetHtmlDisplay(request, group, children);
        }

        return null;
    }

    public Result getHtmlDisplay(Request request, Entry group,
                                 Entries children)
	throws Exception {
	if (parent != null) {
            return parent.getHtmlDisplay(request, group, children);
        }

        return null;
    }

    public String getInlineHtml(Request request, Entry entry)
	throws Exception {
        if (parent != null) {
            return parent.getInlineHtml(request, entry);
        }

        return null;
    }

    public boolean canBeCreatedBy(Request request) {
        if (parent != null) {
            return parent.canBeCreatedBy(request);
        }

        return true;
    }

    public boolean adminOnly() {
        if (adminOnly) {
            return true;
        }
        if (parent != null) {
            return parent.adminOnly();
        }

        return false;
    }

    public boolean canCreate(Request request) {
	if(!canCreate) return false;
        if (adminOnly()) {
            return request.isAdmin();
        }
        if (parent != null) {
            return parent.canCreate(request);
        }

        return true;

    }

    public boolean isSynthType() {
	if(isSynthType)  return true;
        if (parent != null) {
            return parent.isSynthType();
        }
        return false;
    }

    public List<Entry> getSynthEntryTreeForCopy(Request request, Entry entry) throws Exception {
	List<Entry> entries = new ArrayList<Entry>();
	SelectInfo info = new SelectInfo(request, entry);
	getSynthEntryTreeForCopy(request, info, entries, entry,entry);
	return entries;
    }

    public void getSynthEntryTreeForCopy(Request request, SelectInfo info,List<Entry> entries,
					 Entry rootEntry, Entry entry) throws Exception {
	if(entries.size()>COPY_LIMIT) {
	    return;
	}
	List<Entry> children = getEntryManager().getChildren(request, entry);
	entries.addAll(children);
	for(Entry child: children) {
	    if(entries.size()>COPY_LIMIT) {
		break;
	    }
	    getSynthEntryTreeForCopy(request, info, entries, entry,child);
	}
    }

    public List<String> getSynthIds(Request request, SelectInfo select, Entry mainEntry,
                                    Entry ancestor, String synthId)
	throws Exception {

        if (parent != null) {
            return parent.getSynthIds(request, select, mainEntry, ancestor, synthId);
        }

        throw new IllegalArgumentException(
					   "getSynthIds  not implemented for type:" + getType()
					   + " in class:" + getClass().getName());
    }

    public Entry makeSynthEntry(Request request, Entry parentEntry, String id)
	throws Exception {
        if (parent != null) {
            return parent.makeSynthEntry(request, parentEntry, id);
        }

	System.err.println("NO SYNTH ENTRY");
        throw new IllegalArgumentException(
					   "makeSynthEntry  not implemented: type=" + getType() + "\nclass:"
					   + getClass().getName());
    }

    public Entry getSynthTopLevelEntry() throws Exception {
        if (synthTopLevelEntry == null) {
            synthTopLevelEntry = doMakeSynthTopLevelEntry();
        }

        return synthTopLevelEntry;
    }

    public Entry doMakeSynthTopLevelEntry() throws Exception {
        Entry parentEntry = new Entry(this, true);
        parentEntry.setUser(getUserManager().getLocalFileUser());
        //Add metadata to hide the menubar

        getMetadataManager().addMetadata(getRepository().getAdminRequest(),
					 parentEntry,
					 new Metadata(
						      getRepository().getGUID(), parentEntry.getId(),
						      getMetadataManager().findType(ContentMetadataHandler.TYPE_PAGESTYLE), true, "", "true", "",
						      "", ""));

        parentEntry.putTransientProperty("showinbreadcrumbs", "false");
        parentEntry.setName(getLabel());
        parentEntry.setId(ID_PREFIX_SYNTH + getType());
        parentEntry.setParentEntry(getEntryManager().getRootEntry());

        return parentEntry;
    }

    public Entry makeSynthEntry(Request request, Entry parentEntry,
                                List<String> entryNames)
	throws Exception {
        if (parent != null) {
            return parent.makeSynthEntry(request, parentEntry, entryNames);
        }

        throw new IllegalArgumentException("makeSynthEntry  not implemented:"
                                           + getType() + " "
                                           + getClass().getName()+" names:" + entryNames);
    }

    /*
      What is the file cache  time
    */
    public int getCacheTime() {
	return getTypeProperty("file.cache",60*60);
    }

    /**
       Set the FlushedFromCache property.

       @param value The new value for FlushedFromCache
    **/
    public void setFlushedFromCache (boolean value) {
	flushedFromCache = value;
    }

    /**
       Get the FlushedFromCache property.

       @return The FlushedFromCache
    **/
    public boolean getFlushedFromCache () {
	return flushedFromCache;
    }

    public boolean getTypeProperty(String name, boolean dflt) {
        return getProperty((Entry) null, name, dflt);
    }

    public int getTypeProperty(String name, int dflt) {
        return getProperty((Entry) null, name, dflt);
    }

    protected void setTypeProperty(String name, String value) {
        properties.put(name, value);
    }

    public String getTypeProperty(String name, String dflt) {
        return getProperty((Entry) null, name, dflt);
    }

    public String getProperty(Entry entry, String name) {
        return getProperty(entry, name, null);
    }

    public String getProperty(Entry entry, String name, String dflt) {
	return getProperty(entry, name,dflt,true);
    }

    public String getProperty(Entry entry, String name, String dflt,boolean checkParent) {	
        String result = (String) properties.get(name);
        if (result != null) {
            return result;
        }
        if (checkParent && parent != null) {
            return parent.getProperty(entry, name, dflt);
        }

        return dflt;
    }

    public int getProperty(Entry entry, String name, int dflt) {
        String s = getProperty(entry, name, null);
        if (s == null) {
            return dflt;
        }

        return Integer.parseInt(s);
    }

    public boolean getProperty(Entry entry, String name, boolean dflt) {
        String s = getProperty(entry, name, Boolean.toString(dflt));

        return s.equals("true");
    }

    public void putProperty(String name, String value) {
        properties.put(name, value);
        if (parent != null) {
            //            parent.putProperty(name, value);
        }
    }

    public String getFormLabel(Entry parentEntry,Entry entry, String arg, String dflt) {
        return getProperty(entry, "form." + arg + ".label", dflt);
    }

    public boolean okToShowInForm(Entry entry, String arg) {
        return okToShowInForm(entry, arg, true);
    }

    public boolean nullDateOk(){
	return getTypeProperty("date.nullok",false);
    }

    public boolean okToShowInForm(Entry entry, String arg, boolean dflt) {
        String key   = "form." + arg + ".show";
        String value = getProperty(entry, key, "" + dflt);

        return value.equals("true");
    }

    public boolean okToShowInHtml(Entry entry, String arg, boolean dflt) {
        String key   = "html." + arg + ".show";
        String value = getProperty(entry, key, "" + dflt);
        return value.equals("true");
    }

    public boolean okToList(Entry entry, String arg, Hashtable props,
                            boolean dflt) {
        String key   = "list." + arg + ".show";
        String value = getProperty(entry, key, (String) null);
        if (value != null) {
            return value.equals("true");
        }
        if (props != null) {
            return Utils.getProperty(props, key, dflt);
        }

        return dflt;
    }

    public String getFormDefault(Entry entry, String arg, String dflt) {
        String prop = getProperty(entry, "form." + arg + ".default");
        if (prop == null) {
            return dflt;
        }

        return prop;
    }

    public Entry createEntry() {
	return createEntry(getRepository().getGUID());
    }

    public Entry createEntry(String id) {
        return new Entry(id, this);
    }

    public boolean returnToEditForm() {
        if (parent != null) {
            return parent.returnToEditForm();
        }

        return false;
    }

    public void initializeEntryFromXml(Request request, Entry entry,
                                       Element node,
                                       Hashtable<String, File> files)
	throws Exception {
        if (parent != null) {
            parent.initializeEntryFromXml(request, entry, node, files);
        }

    }

    public String convertIdsFromImport(String s, List<String[]> idList) {
        if ( !Utils.stringDefined(s)) {
            return s;
        }
        for (String[] tuple : idList) {
            String oldId = tuple[0];
            if ((oldId == null) || (oldId.length() == 0)) {
                continue;
            }
            String newId = tuple[1];
            s = s.replaceAll(oldId, newId);
        }

        return s;
    }

    public boolean convertIdsFromImport(Entry newEntry,
                                        List<String[]> idList) {
        boolean changed = false;

        if (getTypeProperty("convertidsinfile", false)) {
            changed = convertIdsFromImportInFile(newEntry, idList);
        }

        String desc = newEntry.getDescription();
        if ((desc != null) && (desc.length() > 0)) {
            String converted = convertIdsFromImport(desc, idList);
            if ( !converted.equals(desc)) {
                newEntry.setDescription(converted);

                changed = true;
            }
        }

        return changed;
    }

    public boolean convertIdsFromImportInFile(Entry newEntry,
					      List<String[]> idList) {
        if (idList.size() == 0) {
            return false;
        }

        if ( !newEntry.getResource().isFile()) {
            return false;
        }
        File f = newEntry.getResource().getTheFile();
        //Check that it is a stored file
        File storageDir = new File(getStorageManager().getStorageDir());
        if ( !IO.isADescendent(storageDir, f)) {
            return false;
        }
        try {
            String txt  = IO.readContents(f.toString());
            String orig = txt;
            for (String[] tuple : idList) {
                if (tuple[0].trim().length() == 0) {
                    continue;
                }
                txt = txt.replaceAll(tuple[0].trim(), tuple[1]);
            }
            if ( !orig.equals(txt)) {
                getStorageManager().writeFile(f, txt);
            }
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }

        return true;
    }

    public void addToEntryNode(Request request, Entry entry,
                               FileWriter fileWriter, Element node,boolean encode)
	throws Exception {
        if (parent != null) {
            parent.addToEntryNode(request, entry, fileWriter, node,encode);
        }
    }

    public boolean equals(Object obj) {
        if ( !(obj.getClass().equals(getClass()))) {
            return false;
        }

        return Misc.equals(type, ((TypeHandler) obj).getType());
    }

    public String getNodeType() {
        if (parent != null) {
            return parent.getNodeType();
        }

        return NODETYPE_ENTRY;
    }

    public boolean canChangeTo(TypeHandler newType) {
        return true;
    }

    public String getCorpus(Request request, Entry entry,CorpusType type) throws Exception {
	//	if(type!=CorpusType.LLM) return null;
	String path = entry.getResource().getPath();
	return  getSearchManager().extractCorpus(request, entry,path, null);
    }

    public Entry changeType(Request request, Entry entry) throws Exception {
        //Recreate the entry. This will fill in any extra entry type db tables
        Object[]     origValues =
            entry.getTypeHandler().getEntryValues(entry);
        List<Column> origColumns = entry.getTypeHandler().getColumns();
        List<Column> columns     = this.getColumns();
        entry = getEntryManager().getEntry(request, entry.getId());
        //Then initialize it, e.g., point data type will read the file and set the entry values, etc.
        initializeNewEntry(request, entry, NewType.CHANGETYPE);
        Object[] values = getEntryValues(entry);
        if ((origColumns != null) && (columns != null)) {
            for (int i = 0; i < origColumns.size(); i++) {
                if (i >= columns.size()) {
                    break;
                }
                Column origColumn = origColumns.get(i);
                Column column     = columns.get(i);
                if ( !origColumn.getName().equals(column.getName())) {
                    break;
                }
                if (origColumn.getOffset() != column.getOffset()) {
                    break;
                }
                values[origColumn.getOffset()] =
                    origValues[origColumn.getOffset()];
            }
        }
        //Now store the changes
        getEntryManager().updateEntry(request, entry);

        return entry;
    }

    public String getDefaultFilename() {
        return getTypeProperty("defaultFilename", "tmp.txt");
    }

    public String getType() {
        return type;
    }

    public void setType(String value) {
        this.type = value;
    }

    public boolean isType(String type) {
        if (this.type.equals(type)) {
            return true;
        }
        if (parent != null) {
            return parent.isType(type);
        }

        return false;
    }

    public final Entry createEntryFromDatabase(Connection connection,ResultSet results)
	throws Exception {
        return createEntryFromDatabase(connection,results, false);
    }

    public void initEntryHasBeenCalled(Entry entry) {}

    public void initializeEntryFromForm(Request request, Entry entry,
                                        Entry parent, boolean newEntry)
	throws Exception {
        if (this.parent != null) {
            this.parent.initializeEntryFromForm(request, entry, parent,
						newEntry);
        }
    }

    public void initializeEntryFromHarvester(Request request, Entry entry,
                                             boolean firstCall)
	throws Exception {
        if (firstCall) {
            initializeNewEntry(request, entry, NewType.NEW);
        }
        if (this.parent != null) {
            this.parent.initializeEntryFromHarvester(request, entry, false);
        }
    }

    public void addInitialMetadata(Request request, Entry entry,boolean force) throws Exception {
	if(this.parent!=null) {
	    this.parent.addInitialMetadata(request, entry,force);
	}

    }

    public void initializeEntryFromDatabase(Entry entry) throws Exception {
        if (parent != null) {
            parent.initializeEntryFromDatabase(entry);
        }
    }

    public void doFinalEntryInitialization(Request request, Entry entry,
                                           boolean fromImport) {
        //Clear the column value cache?

        if (fromImport) {
            return;
        }
        if (request == null) {
            return;
        }
        try {
            //Check if there is a default set of children entries
            if (defaultChildrenEntries != null) {
                Element root = XmlUtil.getRoot(defaultChildrenEntries);
                List<Entry> newEntries =
                    getEntryManager().processEntryXml(request, root, entry,
						      new Hashtable<String, File>(), new StringBuilder());
            }

            if (requiredMetadata.size() == 0) {
                return;
            }
            Hashtable<String, Metadata> existingMetadata =
                new Hashtable<String, Metadata>();
            List<Metadata> metadataList = new ArrayList<Metadata>();
            for (String[] idLabel : requiredMetadata) {
                MetadataHandler handler =
                    getMetadataManager().findMetadataHandler(idLabel[0]);
                if (handler != null) {
                    handler.handleForm(request, entry,
                                       getRepository().getGUID(), "",
                                       existingMetadata, metadataList, true);

                }
            }
            for (Metadata metadata : metadataList) {
                getMetadataManager().insertMetadata(metadata);
            }

            //            getEntryManager().setBoundsFromChildren(request, entry.getParentEntry());
            //            getEntryManager().setTimeFromChildren(request, entry.getParentEntry(), null);

        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    public void entryChanged(Entry entry) throws Exception {
        if (parent != null) {
            parent.entryChanged(entry);
        }
    }

    public void entryDeleted(String id) throws Exception {
        if (parent != null) {
            parent.entryDeleted(id);
        }
    }

    public boolean anySuperTypesOfThisType() {
        Class       myClass = getClass();
        TypeHandler parent  = this.parent;

        while (parent != null) {
	    //            if (myClass.isAssignableFrom(parent.getClass().isAssignableFrom(myClass)) {
            if (myClass.isAssignableFrom(parent.getClass())) {
                return true;
            }
            parent = parent.getParent();
        }

        return false;
    }

    public String getFilePattern() {
        return filePattern;
    }

    public boolean canHandleResource(String fullPath, String name) {
        if (filePattern == null) {
            return false;
        }

	if(fileNotPattern!=null) {
            if (name.matches(fileNotPattern)) {
		return false;
	    }

	}

        //If the pattern has file delimiters then use the whole path
        if (filePattern.indexOf("/") >= 0) {
            if (fullPath.matches(filePattern)) {
                return true;
            }
        } else {
            if (fieldFilePattern != null) {
                //                System.err.println ("checking pattern:" + filePattern +" name :" + name);
            }

            //Else, just use the name
            if (name.matches(filePattern)) {
                return true;
            } else if (name.toLowerCase().matches(filePattern)) {
                return true;
            } else {
                //                System.err.println ("no match");
            }
        }

        return false;
    }

    public String getDefaultEntryName(String path) {
        return IO.getFileTail(path);
    }

    public void initEntry(Entry entry) {
	Request request=getAdminRequest();
	if(nameTemplate!=null) {
	    String name =nameTemplate;
	    List<Column> columns = getColumns();
	    if (columns == null) {
		return;
	    }
	    for(Column column: columns) {
		String value = entry.getStringValue(request,column,null);
		if(value==null) value="";
		name= name.replace("${" + column.getName()+"}",value);
	    }
	    entry.setName(name);
	}
    }

    public final Entry createEntryFromDatabase(Connection connection,
					       ResultSet results, boolean abbreviated)
	throws Exception {
        DatabaseManager dbm   = getDatabaseManager();
        String entryId        =
            results.getString(Tables.ENTRIES.COL_NODOT_ID);
        Entry           entry = createEntry(entryId);
        Date createDate = dbm.getDate(results,
                                      Tables.ENTRIES.COL_NODOT_CREATEDATE);
        String parentId =
            results.getString(Tables.ENTRIES.COL_NODOT_PARENT_GROUP_ID);
        Resource resource =
            new Resource(getStorageManager()
			 .resourceFromDB(results
					 .getString(Tables.ENTRIES.COL_NODOT_RESOURCE)), results
			 .getString(Tables.ENTRIES
				    .COL_NODOT_RESOURCE_TYPE), results
			 .getString(Tables.ENTRIES
                                    .COL_NODOT_MD5), results
			 .getLong(Tables.ENTRIES
				  .COL_NODOT_FILESIZE));
        User user = getUserManager().findUser(
					      results.getString(Tables.ENTRIES.COL_NODOT_USER_ID),
					      true);
        int order = results.getInt(Tables.ENTRIES.COL_NODOT_ENTRYORDER);
        if (order == 0) {
            order = 999;
        }
	ResultSet r= results;
	//	DatabaseManager.debug=true;
	//	System.err.println("TypeHandler.initEntry:"+
	//			   dbm.getDate(r, Tables.ENTRIES.COL_NODOT_FROMDATE).getTime());

        entry.initEntry(r.getString(Tables.ENTRIES.COL_NODOT_NAME),
			r.getString(Tables.ENTRIES.COL_NODOT_DESCRIPTION),
			null/*parent*/, user, resource,
			r.getString(Tables.ENTRIES.COL_NODOT_DATATYPE),
			order, createDate.getTime(),
			DateHandler.getTime(dbm.getDate(r, Tables.ENTRIES.COL_NODOT_CHANGEDATE, createDate)),
			DateHandler.getTime(dbm.getDate(r, Tables.ENTRIES.COL_NODOT_FROMDATE,null)),
			DateHandler.getTime(dbm.getDate(r, Tables.ENTRIES.COL_NODOT_TODATE,null)),
			null);

	//	DatabaseManager.debug=false;
        entry.setSouth(r.getDouble(Tables.ENTRIES.COL_NODOT_SOUTH));
        entry.setNorth(r.getDouble(Tables.ENTRIES.COL_NODOT_NORTH));
        entry.setEast(r.getDouble(Tables.ENTRIES.COL_NODOT_EAST));
        entry.setWest(r.getDouble(Tables.ENTRIES.COL_NODOT_WEST));
        entry.setAltitudeTop(
			     r.getDouble(Tables.ENTRIES.COL_NODOT_ALTITUDETOP));
        entry.setAltitudeBottom(
				r.getDouble(Tables.ENTRIES.COL_NODOT_ALTITUDEBOTTOM));

	//Close the connection that gave us the above results because we don't want to
	//call findGroup below and potentially have to get another connection with this
	//one open
	if(connection!=null) {
	    getDatabaseManager().closeConnection(connection);
	}

        Entry parent = getEntryManager().findGroup(getRepository().getAdminRequest(), parentId);	
	entry.setParentEntry(parent);
        if ( !abbreviated) {
            initializeEntryFromDatabase(entry);
        }

        return entry;
    }

    public void addMetadataToXml(Entry entry, Element root,
                                 Appendable extraXml, String metadataType) {}

    public String processDisplayTemplate(Request request, Entry entry,
                                         String html)
	throws Exception {
        String name      = getEntryName(entry);
        String shortName = (name.length() > 40)
	    ? name.substring(0, 39) + "..."
	    : name;
        html = html.replace("${" + ARG_NAME + "}", name);
        html = html.replace("${name.short}", shortName);
        html = html.replace("${" + ARG_LABEL + "}", entry.getLabel());
        html = html.replace("${" + ARG_DESCRIPTION + "}",
                            entry.getDescription());
        html = html.replace("${" + ARG_CREATEDATE + "}",
                            getDateHandler().formatDate(request, entry,
							entry.getCreateDate()));
        html = html.replace("${" + ARG_CHANGEDATE + "}",
                            getDateHandler().formatDate(request, entry,
							entry.getChangeDate()));
        html = html.replace("${" + ARG_FROMDATE + "}",
                            getDateHandler().formatDate(request, entry,
							entry.getStartDate()));
        html = html.replace("${" + ARG_DATE + "}",
                            getDateHandler().formatDate(request, entry,
							entry.getStartDate()));	
        html = html.replace("${" + ARG_TODATE + "}",
                            getDateHandler().formatDate(request, entry,
							entry.getEndDate()));
        html = html.replace("${" + ARG_CREATOR + "}",
                            entry.getUser().getLabel());

        return html;
    }

    public final void getEntryContent(Request request, Entry entry,
				      boolean showDescription,
				      boolean showResource,
				      Hashtable props,
				      boolean forOutput,
				      Appendable buff)
	throws Exception {
	List<NamedBuffer> contents = new ArrayList<NamedBuffer>();
	NamedBuffer sb =  NamedBuffer.append(contents,"",null);
	request.put("addmap","true");
        OutputType    output = request.getOutput();
        if (displayTemplatePath != null) {
            String html = getRepository().getResource(displayTemplatePath);
            buff.append(processDisplayTemplate(request, entry, html));
	    return;
        }
        if (request.get(WikiConstants.ATTR_SHOWTITLE, true)) {
            HU.sectionHeader(buff, getPageHandler().getEntryHref(request,entry));
        }
	HashSet<String> seen = new HashSet<String>();

	List<String> fields = displayFields;
	String propFields = Utils.getProperty(props,"displayFields",null);
	if(propFields!=null) fields=Utils.split(propFields,",",true,true);

	if(fields!=null) {
	    TypeHandler typeHandler  =this;
	    for(String field: fields) {
		if(seen.contains(field)) continue;
		seen.add(field);
		if(field.startsWith("!")) {
		    seen.add(field.substring(1));
		} else if(field.equals("ark"))
		    addArkToHtml(request,typeHandler,entry,sb);
		else if(field.equals(ARG_TYPE))
		    addTypeToHtml(request,typeHandler,entry,sb);
		else if(field.equals("resource"))
		    addResourceToHtml(request,typeHandler,entry,sb);
		else if(field.equals("image"))
		    addImageToHtml(request,typeHandler,entry,sb);
		else if(field.equals("description"))
		    addDescriptionToHtml(request,typeHandler,entry,sb);
		else if(field.equals("createdate"))
		    addCreateDateToHtml(request,typeHandler,entry,sb);
		else if(field.equals("date"))
		    addDateToHtml(request,typeHandler,entry,sb);		
		else if(field.equals("owner"))
		    addOwnerToHtml(request,typeHandler,entry,sb);								
		//		else if(field.equals("altitude"))
		//		    addAltitudeToHtml(request,typeHandler,entry,sb);
		else if(field.equals("_columns")) {
		    addColumnsToHtml(request,typeHandler, entry, contents,seen);
		} else if(field.equals("_default")) {
		    getInnerEntryContent(entry, request, null, output,
					 showDescription, showResource, true,
					 props,seen,forOutput,contents);
		} else {
		    addColumnToHtml(request, typeHandler,entry,field, contents);
		}
	    }
	} else {
	    getInnerEntryContent(entry, request, null, output,
				 showDescription, showResource, true,
				 props,seen,forOutput,contents);
	}

	String macros = Utils.getProperty(props,"macros",null);
	if(macros!=null) {
	    for(String key: Utils.split(macros,",",true,true)) {
		String value = Utils.getProperty(props,key+".value",null);
		if(stringDefined(value)) {
		    String title = Utils.getProperty(props,key+".title","Properties");
		    contents.add(new NamedBuffer(title,value));
		}
	    }
	}

	applyContents(request, buff,contents);
    }

    public void applyContents(Request request, Appendable buff, List<NamedBuffer> contents)
	throws Exception {
	if(getTypeProperty("html.tabs",false) && contents.size()>1) {
	    List<NamedBuffer> nonEmptyContents = new ArrayList<NamedBuffer>();
	    for(NamedBuffer namedBuffer:contents) {
		if(namedBuffer.getBuffer().length()!=0)
		    nonEmptyContents.add(namedBuffer);
		else continue;
		if(!stringDefined(namedBuffer.getName())) {
		    namedBuffer.setName("Information");
		}
		StringBuilder sb = new StringBuilder();
		String buffString = namedBuffer.getBuffer().toString();
		//a hack
		boolean addTable = buffString.startsWith("<tr");
		if(addTable)
		    sb.append(HU.formTable());
		sb.append(buffString);
		if(addTable)
		    sb.append(HU.formTableClose());
		namedBuffer.setBuffer(sb);
	    }		      
	    HU.makeTabs(buff, nonEmptyContents);
	    return;
	}

	for(NamedBuffer namedBuffer:contents) {
	    String title   = namedBuffer.getName();
	    entryTableOpen(request,buff);
	    if(stringDefined(title)) {
		buff.append(HU.row(HU.col(HU.div(title," class=\"formgroupheader\" "), " colspan=2 ")));
	    }
	    buff.append(namedBuffer.getBuffer());
	    entryTableClose(request,buff);
	}

    }

    public void addColumnsToHtml(Request request, TypeHandler typeHandler,Entry entry,
				 List<NamedBuffer> contents,
				 HashSet<String> seen) throws Exception {
    }

    public void entryTableOpen(Request request, Appendable sb) throws Exception {
	sb.append("<table class=formtable width=100%>\n");
    }

    public void entryTableClose(Request request, Appendable sb) throws Exception {
	sb.append("</table>\n");
    }

    public void addEntryProperty(Request request, Appendable sb, String label,String value) throws Exception {
	label = msgLabel(label);
        if (request.isMobile()) {
	    sb.append(formEntry(request, label, value));
	    return;
	}
	sb.append("<tr valign=top><td class=\"ramadda-entry-property-label\">");
	sb.append(label);
	sb.append("</td><td>");
	sb.append(value);
	sb.append("</td></tr>");
    }

    public Column findColumn(String columnName) {
        return null;
    }

    public Column findColumn(int index) {
	return  null;
    }

    private static HashSet seenProps = new HashSet();

    protected void setProperties(Element entryNode) {
        //        boolean debug = type.equals("type_fred_series");
        boolean debug = false;

        if (debug) {
            System.err.println("set Properties");
        }
        List propertyNodes = XmlUtil.findChildren(entryNode, TAG_PROPERTY);

        for (int propIdx = 0; propIdx < propertyNodes.size(); propIdx++) {
            Element propertyNode = (Element) propertyNodes.get(propIdx);
            if (XmlUtil.hasAttribute(propertyNode, ATTR_VALUE)) {
                if (false) {
		    String name =  XmlUtil.getAttribute(propertyNode, ATTR_NAME);
		    if(!seenProps.contains(name)) {
			seenProps.add(name);
			System.out.println(name+"="+
					   XmlUtil.getAttribute(propertyNode, ATTR_VALUE));
		    }
                }
                putProperty(XmlUtil.getAttribute(propertyNode, ATTR_NAME),
                            XmlUtil.getAttribute(propertyNode, ATTR_VALUE));
            } else {
                putProperty(XmlUtil.getAttribute(propertyNode, ATTR_NAME),
                            XmlUtil.getChildText(propertyNode));
            }
        }
    }

    public Hashtable getProperties() {
        return properties;
    }

    public void getServiceInfos(Request request, Entry entry,
                                List<ServiceInfo> services) {
        for (OutputHandler handler : getRepository().getOutputHandlers()) {
            handler.getServiceInfos(request, entry, services);
        }
    }

    public boolean processCommandView(
				      org.ramadda.repository.harvester.CommandHarvester.CommandRequest request,
				      Entry entry,
				      org.ramadda.repository.harvester.CommandHarvester harvester,
				      List<String> args, Appendable sb, List<FileInfo> files)
	throws Exception {
        StringBuilder html = new StringBuilder();
        String url = request.getRequest().getAbsoluteUrl(
							 getRepository().getHtmlOutputHandler().getImageUrl(
													    request.getRequest(), entry, false));
        html.append(harvester.getEntryHeader(request, entry));
        if (isImage(entry)) {
            html.append(HU.img(url, ""));
        }
        html.append(entry.getDescription());
        sb.append(html);

        return false;
    }

    public boolean isGroup() {
        return isGroup;
    }

    private  void addActionLinks(Request request, Entry entry, OutputHandler.State state, List<Link> links)
	throws Exception {
	for (Action action : actions) {
	    if(action.canEdit) {
		if(!getAccessManager().canDoEdit(request, entry)) {
		    continue;
		}
	    }

	    if(action.forAdmin) {
		if (!request.isAdmin()) {
		    continue;
		}
	    }		
	    if(action.forUser) {
		if (request.getUser().getAnonymous()) {
		    continue;
		}
	    }
	    int type = OutputType.TYPE_FILE;			
	    if(action.category.equals("view")) type = OutputType.TYPE_VIEW;
	    else if(action.category.equals("edit")) type = OutputType.TYPE_EDIT;
	    else if(action.category.equals("feeds")) type = OutputType.TYPE_FEEDS;	    	    
	    links.add(new Link(getEntryActionUrl(request, entry,
						 action.id), action.icon, action.label,
			       type));
	}

    }

    public void getEntryLinks(Request request, Entry entry, OutputHandler.State state, List<Link> links)
	throws Exception {

        if (parent != null) {
            parent.getEntryLinks(request, entry, state, links);
	    addActionLinks(request, entry,state, links);
            return;
        }

        boolean isGroup = entry.isGroup();
        boolean canDoNew = isGroup
	    && getAccessManager().canDoNew(request, entry);

        if (canDoNew) {
            links.add(
		      new Link(
			       request.makeUrl(
					       getRepository().URL_ENTRY_FORM, ARG_GROUP,
					       entry.getId(), ARG_TYPE,
					       TYPE_GROUP), ICON_FOLDER_ADD, "New Folder",
			       OutputType.TYPE_FILE));
            links.add(
		      new Link(
			       request.makeUrl(
					       getRepository().URL_ENTRY_FORM, ARG_GROUP,
					       entry.getId(), ARG_TYPE, TYPE_FILE), ICON_ENTRY_ADD,
			       "New File", OutputType.TYPE_FILE));
            links.add(new Link(request.makeUrl(getRepository().URL_ENTRY_NEW,
					       ARG_GROUP, entry.getId()), ICON_NEW, LABEL_NEW_ENTRY,
			       OutputType.TYPE_FILE | OutputType.TYPE_TOOLBAR));
            links.add(makeHRLink(OutputType.TYPE_FILE));
            List<String> pastTypes =
                (List<String>) getSessionManager().getSessionProperty(
								      request, ARG_TYPE);
            HashSet seen   = new HashSet();
            boolean didone = addTypes(request, entry, links, childTypes,
                                      seen);
            didone |= addTypes(request, entry, links, pastTypes, seen);
            didone |= addTypesFromEntries(request, entry, links,
                                          state.getAllEntries(), seen);
            if (didone) {
                links.add(makeHRLink(OutputType.TYPE_FILE));
            }
        }

	addImportExportLinks(request, entry,links,canDoNew);

        links.add(0,
		  new Link(
			   HU.url(
				  getRepository().URL_ENTRY_LINKS.toString(),
				  new String[] { ARG_ENTRYID,
						 entry.getId() }), "fa-list",
			   "All Actions", OutputType.TYPE_OTHER));

        links.add(makeHRLink(OutputType.TYPE_FILE));

        if ( !canDoNew && isGroup
	     && getAccessManager().canDoUpload(request, entry)) {
            links.add(
		      new Link(
			       request.makeUrl(
					       getRepository().URL_ENTRY_UPLOAD, ARG_GROUP,
					       entry.getId()), ICON_UPLOAD, "Upload a File",
			       OutputType.TYPE_FILE
			       | OutputType.TYPE_TOOLBAR));
        }

	boolean canEdit = getAccessManager().canDoEdit(request, entry);
        if (canEdit) {
            links.add(
		      new Link(
			       request.entryUrl(getRepository().URL_ENTRY_FORM, entry),
			       ICON_EDIT, "Edit " + LABEL_ENTRY,
			       OutputType.TYPE_EDIT /* | OutputType.TYPE_TOOLBAR*/));

            //NOTE: Don't add the direct link because the auth token is added
            if (false && getEntryManager().isAnonymousUpload(entry)) {
                links.add(
			  new Link(
				   request.entryUrl(
						    getRepository().URL_ENTRY_CHANGE, entry,
						    ARG_JUSTPUBLISH, "true"), ICON_PUBLISH,
				   "Make " + LABEL_ENTRY + " Public",
				   OutputType.TYPE_EDIT
				   /*| OutputType.TYPE_TOOLBAR*/
				   ));
            }

	}
	if ((request.getUser() != null)
	    && !request.getUser().getAnonymous()) {
	    links.add(
		      new Link(
			       request.entryUrlWithArg(
						       getRepository().URL_ENTRY_COPY, entry,
						       ARG_FROM), ICON_MOVE, "Move/Copy/Link",
			       OutputType.TYPE_EDIT));
	}

	if(canEdit) {

            List<String> metadataTypes =
                entry.getTypeHandler().getMetadataTypes();
	    //	    if (metadataTypes.size() > 0) {
	    links.add(makeHRLink(OutputType.TYPE_EDIT));
	    //            }

            links.add(
		      new Link(
			       request.entryUrl(
						getMetadataManager().URL_METADATA_FORM,
						entry), ICON_METADATA_EDIT, "Edit Properties",
			       OutputType.TYPE_EDIT));

            links.add(
		      new Link(
			       request.entryUrl(
						getMetadataManager().URL_METADATA_ADDFORM,
						entry), ICON_METADATA_ADD, "Add Property...",
			       OutputType.TYPE_EDIT));

            if (metadataTypes.size() > 0) {
                links.add(makeHRLink(OutputType.TYPE_EDIT));
                for (String metadataType : metadataTypes) {
		    String []pair = getMetadataManager().getMetadataAddLink(request, entry, metadataType);
		    if(pair==null) continue;
                    links.add(new Link(pair[0],ICON_METADATA_ADD, pair[1], OutputType.TYPE_EDIT));
                }
                links.add(makeHRLink(OutputType.TYPE_EDIT));
            }

            if (getAccessManager().canSetAccess(request, entry)) {
                links.add(
			  new Link(
				   request.entryUrl(
						    getRepository().URL_ACCESS_FORM,
						    entry), ICON_ACCESS, "Permissions",
				   OutputType.TYPE_EDIT));
            }

            links.add(
		      new Link(
			       request.entryUrl(
						getRepository().URL_ENTRY_EXTEDIT,
						entry), "fas fa-sitemap", "Extended Edit",
			       OutputType.TYPE_EDIT));

            if (getRepository().getLogActivityToDatabase()) {
                links.add(
			  new Link(
				   request.entryUrl(
						    getRepository().URL_ENTRY_ACTIVITY,
						    entry), "fas fa-chart-line", "Entry Activity",
				   OutputType.TYPE_EDIT));
            }

        }

        if (getEntryManager().okToDelete(request, entry)) {
            links.add(
		      new Link(
			       request.entryUrl(
						getRepository().URL_ENTRY_DELETE,
						entry), ICON_DELETE, "Delete " + LABEL_ENTRY,
			       OutputType.TYPE_EDIT
			       /*| OutputType.TYPE_TOOLBAR*/
			       ));

        }

        Link downloadLink = getEntryDownloadLink(request, entry);
        if (downloadLink != null) {
            links.add(0,downloadLink);
        }

        if (getRepository().getCommentsEnabled()) {
            if (getRepository().isReadOnly()) {
                links.add(
			  new Link(
				   request.entryUrl(
						    getRepository().URL_COMMENTS_SHOW,
						    entry), ICON_COMMENTS, "View Comments",
				   OutputType.TYPE_VIEW));
            } else {
                links.add(
			  new Link(
				   request.entryUrl(
						    getRepository().URL_COMMENTS_SHOW,
						    entry), ICON_COMMENTS, "Add/View Comments",
				   OutputType.TYPE_TOOLBAR));
            }
        }

	addActionLinks(request, entry,state, links);	

    }

    private void addImportExportLinks(Request request, Entry entry,List<Link>links, boolean canDoNew)  throws Exception {
        if (getAccessManager().canDoExport(request, entry)) {
            links.add(new Link(HU.url(getRepository().URL_ENTRY_EXPORT.toString() + "/"
				      + IO.stripExtension(Entry.encodeName(getEntryName(entry))) + ".zip", new String[] {
					  ARG_ENTRYID,
					  entry.getId() }), ICON_EXPORT,
			       "Export", OutputType.TYPE_FILE));
	    Link l = new Link(HU.url(getRepository().URL_ENTRY_EXPORT.toString() + "/"
				     + IO.stripExtension(Entry.encodeName(getEntryName(entry))) + ".zip", new String[] {
					 ARG_EXPORT_SHALLOW,"true",
					 ARG_ENTRYID,
					 entry.getId() }), ICON_EXPORT,
			      "Shallow Export", OutputType.TYPE_FILE);
	    l.setTooltip("Just export this entry, not it's children");
            links.add(l);	    
	    l = new Link(HU.url(getRepository().URL_ENTRY_EXPORT.toString() + "/"
				+ IO.stripExtension(Entry.encodeName(getEntryName(entry))) + ".zip", new String[] {
				    ARG_EXPORT_DEEP,"true",
				    ARG_ENTRYID,
				    entry.getId() }), ICON_EXPORT,
			 "Deep Export", OutputType.TYPE_FILE);
	    l.setTooltip("Include entries this entry links to");
            links.add(l);	    

        }

        //Add an import link if they have the right privileges
        if (canDoNew) {
            links.add(
		      new Link(
			       request.makeUrl(
					       getRepository().URL_ENTRY_IMPORT, ARG_GROUP,
					       entry.getId()), ICON_IMPORT, "Import",
			       OutputType.TYPE_FILE));
            links.add(makeHRLink(OutputType.TYPE_FILE));
        }
    }

    private Link makeHRLink(int mask) {
        Link hr = new Link(true);
        hr.setLinkType(mask);

        return hr;
    }

    private boolean addTypesFromEntries(Request request, Entry entry,
                                        List<Link> links,
                                        List<Entry> entries,
                                        HashSet<String> seen)
	throws Exception {
        if (entries == null) {
            return false;
        }
        List<String> types = new ArrayList<String>();
        for (Entry e : entries) {
            String type = e.getTypeHandler().getType();
            if ( !seen.contains(type)) {
                if (e.getTypeHandler().canCreate(request)) {
                    types.add(type);
                }
            }
        }

        return addTypes(request, entry, links, types, seen);
    }

    private boolean addTypes(Request request, Entry entry, List<Link> links,
                             List<String> types, HashSet<String> seen)
	throws Exception {
        if (types == null) {
            return false;
        }
        boolean didone = false;
        for (String type : types) {
            if (type.equals(TYPE_FILE) || type.equals(TYPE_GROUP)) {
                continue;
            }
            if (seen.contains(type)) {
                continue;
            }
            seen.add(type);
            didone = true;
            TypeHandler typeHandler = getRepository().getTypeHandler(type);
	    if(typeHandler==null) {
		continue;
	    }
            String      icon        = typeHandler.getIconProperty(null);
            if (icon == null) {
                icon = ICON_ENTRY_ADD;
            }
            links.add(
		      new Link(
			       request.makeUrl(
					       getRepository().URL_ENTRY_FORM, ARG_GROUP,
					       entry.getId(), ARG_TYPE, type), icon,
			       getIconImage("fa-solid fa-plus") +HU.SPACE + typeHandler.getDescription(),
			       OutputType.TYPE_FILE));
        }

        return didone;
    }

    public String getIconProperty(String dflt) {
        String icon = iconPath;
        if (icon == null) {
            icon = getTypeProperty("icon", (String) null);
            if (icon == null) {
                icon = dflt;
            }
        }

        return icon;
    }

    public String getTypeIconUrl() {
        String icon = getIconProperty(null);
        if (icon != null) {
            icon = getIconUrl(icon);
        }

        return icon;
    }

    public boolean canDownload(Request request, Entry entry)
	throws Exception {
        if (parent != null) {
            return parent.canDownload(request, entry);
        }

        if ( !entry.isFile()) {
            return false;
        }

        return true;
    }

    public String getPathForEntry(Request request, Entry entry,boolean forReading)
	throws Exception {
        Resource resource = entry.getResource();
	String path;
	if(forReading && entry.isFile()) {
	    path =  getStorageManager().getEntryFile(entry).toString();
	} else {
	    path = entry.getResource().getPath();
	}
        if ( !Utils.stringDefined(path)) {
            path = getTypeProperty("fixed_url", (String) null);
        }
        if (path != null) {
            path = Utils.normalizeTemplateUrl(path).replace("${htdocs}",getStorageManager().getHtdocsDir());
        }

        return path;
    }

    public File getFileForEntry(Entry entry)  {
	try {
	    return getStorageManager().getEntryFile(entry);
	} catch(Exception exc) {
	    getLogManager().logError("TypeHandler.getFileForEntry:" + "entry:" + entry.getId() +" " + entry.getName(),exc);
	    return null;
	}
    }

    private HashSet seenIt = new HashSet();

    public Link getEntryDownloadLink(Request request, Entry entry)
	throws Exception {
        return getEntryDownloadLink(request, entry, "Download File");
    }

    public Link getEntryDownloadLink(Request request, Entry entry,
                                     String label)
	throws Exception {
        if ( !getAccessManager().canDownload(request, entry)) {
            return null;
        }
        String size = " ("
	    + formatFileLength(entry.getResource().getFileSize())
	    + ")";

        String tail = getStorageManager().getFileTail(entry);
        String fileTail = HU.urlEncodeExceptSpace(tail);

        Link link  = new Link(getEntryManager().getEntryResourceUrl(request,
								    entry), ICON_FETCH,
			      msg(label) + size,
			      OutputType.TYPE_FILE | OutputType.TYPE_IMPORTANT);
	link.setTooltip(tail);
	return link;
    }

    public void getInnerEntryContent(Entry entry, Request request,
				     TypeHandler typeHandler, OutputType output,
				     boolean showDescription, boolean showResource,
				     boolean linkToDownload, Hashtable props,HashSet<String> seen,
				     boolean forOutput,
				     List<NamedBuffer>  contents)
	throws Exception {

	if(seen==null) seen=new HashSet<String>();
        if (typeHandler == null) {
            typeHandler = this;
        }
        if (parent != null) {
            parent.getInnerEntryContent(entry, request, typeHandler,
					output, showDescription, showResource, linkToDownload,
					props,seen,forOutput,contents);
	    return;
        }

	String showDateS=Utils.getProperty(props,"showDate",null);
        if ((props != null) && Misc.equals(props.get("showBase"), "false")) {
	    if(showDateS!=null && showDateS.equals("true")) {
		if(contents.size()==0)  contents.add(new NamedBuffer(""));
		Appendable sb = contents.get(contents.size()-1);
		addDateToHtml(request,typeHandler,entry,sb);
	    }
            return;
        }

        boolean showDate = typeHandler.okToShowInHtml(entry, ARG_DATE, true);
        boolean showCreateDate = showDate   && typeHandler.okToShowInHtml(entry,
									  "createdate", true);
        boolean noBasic =Utils.getProperty(props,"noBasic",false);
        boolean justBasic =Utils.getProperty(props,"justBasic",false);
        boolean entryIsImage = isImage(entry);
        boolean showImage    = false;
        if (showResource && entryIsImage) {
            if (entry.getResource().isFile()
		&& getAccessManager().canDownload(request, entry)) {
                showImage = true;
            } else if (entry.getResource().isUrl()) {
                showImage = true;
            }
        }

	if(props!=null && !Utils.getProperty(props,"showImage",true)) {
	    showImage = false;
	    entryIsImage = false;
	}

	if(contents.size()==0)  contents.add(new NamedBuffer(""));
	Appendable sb = contents.get(contents.size()-1);
	if (showDescription) {
	    addDescriptionToHtml(request,typeHandler,entry,sb);
	}

	String createdDisplayMode =  getPageHandler().getCreatedDisplayMode();
	boolean showCreated = true;
	if (createdDisplayMode.equals("none")) {
	    showCreated = false;
	} else if (createdDisplayMode.equals("admin")) {
	    showCreated = request.getUser().getAdmin();
	} else if (createdDisplayMode.equals("user")) {
	    showCreated = !request.isAnonymous();
	} else if (createdDisplayMode.equals("all")) {
	    showCreated = true;
	} else {
	    showCreated = false;
	}

	if(noBasic){
	    showCreated=false;
	    showCreateDate=false;		
	}

	if (showResource && entryIsImage) {
	    if(!seen.contains("image"))
		addImageToHtml(request,typeHandler,entry,sb);
	}

	if(!seen.contains("resource"))
	    addResourceToHtml(request,typeHandler,entry,sb);

	if (!noBasic && (forOutput ||typeHandler.okToShowInHtml(entry, ARG_TYPE, true))) {
	    if(!seen.contains(ARG_TYPE))
		addTypeToHtml(request,typeHandler,entry,sb);
	}

	if(forOutput|| okToShowInForm(entry, "ark", false)) {
	    if(!seen.contains("ark"))
		addArkToHtml(request,typeHandler,entry,sb);
	}

	if (forOutput||showCreateDate) {
	    if(!seen.contains("createdate"))
		addCreateDateToHtml(request,typeHandler,entry,sb);
	}

	if (forOutput||showCreated) {
	    if(!seen.contains("owner"))
		addOwnerToHtml(request,typeHandler,entry,sb);
	}
	if(justBasic) return;

	if (forOutput||showDate) {
	    if(!seen.contains("date"))
		addDateToHtml(request,typeHandler,entry,sb);
	}

	//	if(!seen.contains("altitude"))
	//	    addAltitudeToHtml(request,typeHandler,entry,sb);
    }

    public void addTypeToHtml(Request request, TypeHandler typeHandler,Entry entry,Appendable sb) throws Exception {
	String icon = getPageHandler().getEntryIconImage(request,entry);
	String label =icon + HU.space(1)+ 
	    getFileTypeDescription(request,  entry);
	String typeLink="";
	if(request.getExtraProperty("isinfo")!=null) 
	    typeLink = HU.href(getRepository().getUrlPath("/entry/types.html?type=" + getType()),
			       HU.getIconImage("fas fa-database"),HU.attrs("title","View data type"));

	label+=HU.space(2) + typeLink;
	addEntryProperty(request, sb,"Kind",label);
    }

    public void addResourceToHtml(Request request, TypeHandler typeHandler,Entry entry,Appendable sb) throws Exception {

	Resource resource      = entry.getResource();
	String   resourceLink  = resource.getPath();

	String   resourceLabel = null;
	if (resource.isUrl()) {
	    resourceLabel = "URL";
	} else if (resource.isFileType()) {
	    resourceLabel = "File";
	} else {
	    resourceLabel = "Resource (" + resource.getType() + ")";
	}
	if (resourceLink.length() > 0) {
	    if (entry.getResource().isUrl()) {
		try {
		    resourceLink = typeHandler.getPathForEntry(request, entry,false);
		    resourceLink = HU.href(resourceLink,
					   resourceLink);
		} catch (Exception exc) {
		    sb.append("Error:" + exc);
		}
	    } else if (entry.getResource().isFile()) {
		resourceLink =
		    getStorageManager().getFileTail(resourceLink);
		//Not sure why we were doing this but it screws up chinese characters
		//                    resourceLink =
		//                        HU.urlEncodeExceptSpace(resourceLink);
		resourceLabel = "File";
		if (getAccessManager().canDownload(request, entry)) {
		    String url = getEntryResourceUrl(request, entry, false);
		    resourceLink =    HU.href(url,resourceLink) +
			HU.space(1)+
			HU.href(url,HU.img(getIconUrl(ICON_DOWNLOAD),"Download", ""));
		} else {
		    resourceLink = resourceLink + HU.space(2)
			+ "(" + msg("restricted") + ")";
		}
	    }
	    if (entry.getResource().getFileSize() > 0) {
		resourceLink =
		    resourceLink + HU.space(2)
		    + formatFileLength(entry.getResource().getFileSize());
	    }
	    addEntryProperty(request, sb, resourceLabel, resourceLink);
	    /**** not now
	    if (!request.getUser().getAnonymous()) {
		File corpus = getSearchManager().getCorpusFile(request, entry);
		if(corpus.exists()) {
		    String length =  formatFileLength(corpus.length());
		    addEntryProperty(request, sb, "Text corpus size",length);
		}
	    }
	    */



	}
    }

    public void addDescriptionToHtml(Request request, TypeHandler typeHandler,Entry entry,Appendable sb) throws Exception {
	String desc = entry.getDescription();
	if ((desc != null) && (desc.length() > 0)
	    && ( !isWikiText(desc))) {
	    addEntryProperty(request, sb, "Description",
			     getEntryManager().getEntryText(request, entry, desc));
	}
    }

    public void addImageToHtml(Request request, TypeHandler typeHandler,Entry entry,Appendable sb) throws Exception {
	String width = "600";
	if (request.isMobile()) {
	    width = "250";
	}
	String img    = null;
	String imgUrl = null;
	if (entry.getResource().isFile()
	    && getAccessManager().canDownload(request, entry)) {
	    imgUrl = getEntryResourceUrl(request, entry,false,true);
	    img    = HU.img(imgUrl, "", HU.attrs("width", width,"style","max-width:100%;","align","center"));
	} else if (entry.getResource().isUrl()) {
	    try {
		imgUrl = typeHandler.getPathForEntry(request, entry,false);
		img    = HU.img(imgUrl, "", HU.attrs("width", width,"style","max-width:100%;","align","center"));
	    } catch (Exception exc) {
		sb.append("Error getting path:" + entry.getResource()
			  + " " + exc);
	    }
	}
	if (img != null) {
	    String outer = HU.href(imgUrl, img, HU.cssClass("popup_image"));
	    sb.append(HU.col(outer, " colspan=2 "));
	    getWikiManager().addImagePopupJS(request, null, sb,
					     new Hashtable());
	}

    }

    public void addColumnToHtml(Request request,
				  TypeHandler typeHandler,
				  Entry entry,
				  String columnName,
				  List<NamedBuffer> contents) throws Exception {
    }

    public void addArkToHtml(Request request, TypeHandler typeHandler,Entry entry,Appendable sb) throws Exception {
	String ark = getPageHandler().getArk(request, entry,false);
	if(ark!=null) {
	    addEntryProperty(request, sb,"ARK ID",ark);
	}
    }

    public void addCreateDateToHtml(Request request, TypeHandler typeHandler,Entry entry,Appendable sb) throws Exception {
	addEntryProperty(request, sb,"Created",
			 getDateHandler().formatDate(request,
						     entry, entry.getCreateDate()));

	if (entry.getCreateDate() != entry.getChangeDate()) {
	    addEntryProperty(request, sb,"Modified",
			     getDateHandler().formatDate(
							 request, entry, entry.getChangeDate()));

	}
    }

    public void addOwnerToHtml(Request request, TypeHandler typeHandler,Entry entry,Appendable sb) throws Exception {
	typeHandler.addUserSearchLink(request, entry, sb);
    }

    public void addAltitudeToHtml(Request request, TypeHandler typeHandler,Entry entry,Appendable sb) throws Exception {
	if (entry.hasAltitude() && (entry.getAltitude() != 0)) {
	    addEntryProperty(request, sb,"Elevation","" + entry.getAltitude());
	}
    }

    public void addDateToHtml(Request request, TypeHandler typeHandler,Entry entry,Appendable sb) throws Exception {
	boolean hasDataDate = false;
	if (Math.abs(entry.getCreateDate() - entry.getStartDate())   > 60000) {
	    hasDataDate = true;
	} else if (Math.abs(entry.getCreateDate() - entry.getEndDate())
		   > 60000) {
	    hasDataDate = true;
	}
	if(!hasDataDate) return;

	if (entry.getEndDate() != entry.getStartDate()) {
	    String startDate =  getFieldHtml(request,  entry,  null,"startdate",false);
	    String endDate = getDateHandler().formatDate(request,   entry, entry.getEndDate());
	    //	    addEntryProperty(request, sb, getFormLabel(null,null,"startdate","Start Date")),startDate);
	    //addEntryProperty(request, sb,msgLabel(getFormLabel(null,null,"enddate","End Date")),endDate);
	    addEntryProperty(request, sb,getFormLabel(null,null,"date","Date"),
			     startDate +" - "+ endDate);
	} else {
	    StringBuilder dateSB = new StringBuilder();
	    String startDate =  getFieldHtml(request,  entry,  null,"startdate",false);
	    dateSB.append(startDate);
	    if (typeHandler.okToShowInForm(entry, ARG_TODATE)
		&& (entry.getEndDate() != entry.getStartDate())) {
		dateSB.append(" - ");
		dateSB.append(getDateHandler().formatDate(request,
							  entry, entry.getEndDate()));
	    }
	    Entry parentEntry = entry==null?null:entry.getParentEntry();
	    String formLabel = getFormLabel(parentEntry,entry, ARG_DATE, "Date");
	    addEntryProperty(request, sb,formLabel, dateSB.toString());
	}
    }

    public void addUserSearchLink(Request request, Entry entry, Appendable sb) throws Exception  {
	if (!okToShowInHtml(entry, "owner", true)) return;
	String userSearchLink =
	    HtmlUtils
	    .href(HtmlUtils
		  .url(request
		       .makeUrl(getRepository()
				.URL_USER_PROFILE), ARG_USER_ID,
		       entry.getUser().getId()), entry
		  .getUser()
		  .getLabel(), "title=\"View user profile\"");

	String linkMsg = "Search for entries of this type created by the user";
	String userLinkId = HU.getUniqueId("userlink_");
	userSearchLink = HtmlUtils
	    .href(getSearchManager().URL_SEARCH_TYPE + "/"
		  + entry.getTypeHandler().getType() + "?"
		  + ARG_USER_ID + "=" + entry.getUser().getId()
		  + "&" + SearchManager.ARG_SEARCH_SUBMIT
		  + "=true", entry.getUser().getLabel(), HtmlUtils
		  .id(userLinkId) + HtmlUtils
		  .cssClass("entry-type-search") + HtmlUtils
		  .attr(HtmlUtils
			.ATTR_ALT, msg(linkMsg)) + HtmlUtils
		  .attr(HtmlUtils
			.ATTR_TITLE, linkMsg));
	String overview = getUserManager().getUserAvatar(request,entry.getUser(),true,40,null);
	if(overview!=null) {
	    userSearchLink+="<br>" + overview;

	}
	addEntryProperty(request, sb,"Created by", userSearchLink);

	if(entry.hasAreaDefined(request)) {
	    addEntryProperty(request, sb,"Bounds",entry.getBoundsString(request,true));
	} else 	if(entry.hasLocationDefined(request)) {
	    addEntryProperty(request, sb,"Location",
			     "Latitude: " + entry.getLatitude(request) +" Longitude: " + entry.getLongitude(request));
	}
	//	seen.add("altitude");
	addAltitudeToHtml(request,this,entry,sb);
    }

    public String formatLocation(double lat, double lon) {
        if (latLonFormat != null) {
            synchronized (latLonFormat) {
                return latLonFormat.format(lat) + "/"
		    + latLonFormat.format(lon);
            }
        }

        return Misc.format(lat) + "/" + Misc.format(lon);
    }

    public static boolean isWikiText(String desc) {
        if (desc == null) {
            return false;
        }
        desc = desc.trim();

        return (desc.startsWith("<wiki_inner>")
                || desc.startsWith(WIKI_PREFIX));
    }

    public String getEntryResourceHref(Request request, Entry entry)
	throws Exception {
        if ( !getAccessManager().canDownload(request, entry)) {
            /*
	      if(!entry.isGroup() && !seenIt.contains(entry.getId())) {
	      seenIt.add(entry.getId());
	      getLogManager().logInfoAndPrint("cannot download:" + entry);
	      Resource resource = entry.getResource();
	      getLogManager().logInfoAndPrint("\tresource:" + resource +
	      " type:" + resource.getType() +
	      " exists:" +  resource.getTheFile().exists() +
	      " the file:" + resource.getTheFile());

	      }
            */
            return null;
        }
        String size = " ("
	    + formatFileLength(entry.getResource().getFileSize())
	    + ")";

        String fileTail = getStorageManager().getFileTail(entry);
        fileTail = HU.urlEncodeExceptSpace(fileTail);

        return HU.href(
		       getEntryManager().getEntryResourceUrl(request, entry),
		       HU.img(getRepository().getIconUrl(ICON_FETCH)) + " "
		       + entry.getName());
    }

    public String getEntryResourceUrl(Request request, Entry entry)
	throws Exception {
        return getEntryResourceUrl(request, entry,
                                   EntryManager.ARG_INLINE_FALSE,false);
    }

    public String getEntryResourceUrl(Request request, Entry entry,
                                      boolean inline)
	throws Exception {
	return getEntryResourceUrl(request, entry,inline,false);
    }

    public String getEntryResourceUrl(Request request, Entry entry,
                                      boolean inline,boolean addTimestamp)
	throws Exception {
	String url  = getEntryManager().getEntryResourceUrl(request, entry, inline,
							    EntryManager.ARG_FULL_DFLT, EntryManager.ARG_ADDPATH_DFLT);
	if(addTimestamp)
	    url = HU.url(url,"timestamp",""+entry.getChangeDate());
	return url;
    }

    public boolean okToSetNewNameDefault() {
        return !getTypeProperty("name.raw",false);
    }

    public List<Entry> handleBulkUpload(Request request, Entry parent, String bulkFile,String column,
					HashSet<String> seen,String pattern,String notPattern) throws Exception {
	List<Entry> entries = new ArrayList<Entry>();
	request.remove(ARG_NAME);
	for(String siteId:Utils.split(getStorageManager().readFile(bulkFile),"\n",true,true)) {
	    if(siteId.startsWith("#")) continue;
	    if(seen.contains(siteId)) continue;
	    if(pattern!=null && !siteId.matches(pattern)) continue;
	    if(notPattern!=null && siteId.matches(pattern)) continue;	    
	    seen.add(siteId);
	    String      newId          = getRepository().getGUID();
	    Entry entry = createEntry(newId);
	    entry.clearDate();
	    entry.setParentEntry(parent);
	    initializeEntryFromForm(request, entry, parent, true);
	    Date date  = new  Date();
	    entry.setCreateDate(date.getTime());
	    entry.setChangeDate(date.getTime());
	    if(!entry.getTypeHandler().getTypeProperty("date.nullok",false)) {
		if(DateHandler.isNullDate(entry.getStartDate()))
		    entry.setStartDate(date.getTime());
		if(DateHandler.isNullDate(entry.getEndDate()))
		    entry.setEndDate(date.getTime());		
	    }

	    /*	    getEntryManager().initEntry(entry, "","",parent,
		    request.getUser(),					
		    new Resource(), "", entry.getEntryOrder(),
		    date.getTime(),
		    date.getTime(), date.getTime(),
		    date.getTime(), values);
	    */
	    entry.setValue(column, siteId);
	    entries.add(entry);
	}
	return entries;
    }

    public void initializeColumns(Request request, Entry entry,
				  NewType newType) throws Exception {
	if(!isNew(newType)) return;
	List<Column> columns = entry.getTypeHandler().getColumns();
	if (columns != null) {
	    String fileName = getStorageManager().getFileTail(entry);
	    for(Column column: columns) {
		column.initializeNew(request,entry,fileName);
	    }
	}
    }

    public void initializeNewEntry(Request request, Entry entry,
                                   NewType newType)
	throws Exception {

        if (parent != null) {
            parent.initializeNewEntry(request, entry, newType);
        } else 	{
	    initializeColumns(request, entry, newType);
	}

	if(metadataPatterns!=null     && entry.getResource().isFile()) {
	    String fileName = getStorageManager().getOriginalFilename(entry.getFile().getName());
	    Date startDate = null,endDate = null; 
	    for(MetadataPattern mp: metadataPatterns) {
		Matcher matcher = mp.pattern.matcher(fileName);
		if ( !matcher.find()) {
		    //		    System.err.println("filename pattern: no match:" + mp.spattern +" file:" + fileName);
		    continue;
		}

		String v1 = matcher.group(1);
		String v2 = matcher.groupCount()>1?matcher.group(2):null;
		String v3 = matcher.groupCount()>2?matcher.group(3):null;		
		//		System.err.println("filename pattern: match:" + v1 + " " + v2 +" " + v3);
		if(mp.column!=null) {
		    try {
			if(mp.column.equals("date")) {
			    startDate= endDate  = Utils.parseDate(v1);
			    continue;
			}
			if(mp.column.equals("startdate")) {
			    startDate=  Utils.parseDate(v1);
			    continue;
			} 		    
			if(mp.column.equals("enddate")) {
			    endDate  = Utils.parseDate(v1);
			    continue;
			} 
			entry.setValue(mp.column,v1);
		    } catch(Exception exc) {
			String message = "Error extracting metadata from filename:" + fileName +
			    " for entry:" + entry.getName() +" id:" + entry.getId() +
			    " Error: " + exc;
			getSessionManager().addSessionMessage(request, message);
			getLogManager().logError(message,exc);
		    }
		    continue;
		}
		MetadataType type = getMetadataManager().findType(mp.metadataType);
		if(type==null) {
		    System.err.println("filename pattern: could not find metadata:" + mp.metadataType);
		    continue;
		}
		Metadata mtd  =
                    new Metadata(getRepository().getGUID(), entry.getId(),
                                 type, false,
				 v1, v2, v3, null, null);
                getMetadataManager().addMetadata(request,entry, mtd);
	    }
	    if (startDate != null) 
		entry.setStartDate(startDate.getTime());
	    if (endDate != null) 
		entry.setEndDate(endDate.getTime());		
	}

        //Check if we're supposed to extract urls from the file
        if (getTypeProperty(PROP_INGEST_LINKS, "false").equals("true")
	    && entry.getResource().isFile()) {
            String contents = IOUtil.readContents(
						  getStorageManager().getFileInputStream(
											 entry.getFile()));
            for (String url :
		     Utils.extractPatterns(contents,
					   "(https?://[^\"' \\),]+)")) {
                getMetadataManager().addMetadata(request,entry,
						 new Metadata(getRepository().getGUID(),
							      entry.getId(),
							      getMetadataManager().findType(ContentMetadataHandler.TYPE_URL),
							      false,
							      url, url, "", "", ""));
            }
        }

        //Do we extract metadata from the file path
        if (fieldFilePattern != null) {
            String  path    = entry.getResource().getPath();
            Matcher matcher = fieldFilePattern.matcher(path);
            if ( !matcher.find()) {
                matcher = fieldFilePattern.matcher(path.toLowerCase());
                if ( !matcher.find()) {
                    //                System.err.println("no match:"  + entry.getResource().getPath());
                    return;
                }
            }
            Object[] values    = getEntryValues(entry);
            String   year      = null;
            String   month     = null;
            String   day       = null;
            String   julianDay = null;

            //            System.err.println("match:" + entry.getResource().getPath());
            for (int i = 0; i < fieldPatternNames.size(); i++) {
                String columnName = fieldPatternNames.get(i);
                String value      = matcher.group(i + 1);
                Column column     = getColumn(columnName);
                if (column == null) {
                    if (columnName.equals("year")) {
                        year = value;

                        continue;
                    }
                    if (columnName.equals("month")) {
                        month = value;

                        continue;
                    }
                    if (columnName.equals("day")) {
                        day = value;

                        continue;
                    }
                    if (columnName.equals("julian_day")) {
                        julianDay = value;

                        continue;
                    }
                    System.err.println("Unknown column:" + columnName);
                    continue;
                }
                column.setValue(entry, values, value);
            }

            if ((year != null) && entry.sameDate()) {
                Date date = null;
                if (month != null) {
                    if (day != null) {
                        date = new SimpleDateFormat("yyyy-MM-dd").parse(year
									+ "-" + month + "-" + day);
                    } else {
                        date = new SimpleDateFormat("yyyy-MM").parse(year
								     + "-" + month);
                    }
                } else if (julianDay != null) {
                    GregorianCalendar gc = new GregorianCalendar();
                    gc.set(GregorianCalendar.YEAR, Integer.parseInt(year));
                    gc.set(GregorianCalendar.DAY_OF_YEAR,
                           Integer.parseInt(julianDay));
                    date = gc.getTime();
                }
                if (date != null) {
                    entry.setStartDate(date.getTime());
                    entry.setEndDate(date.getTime());
                }
            }
        }

        if (newType==NewType.NEW) {
	    String inheritLocation = getTypeProperty("inheritlocationfromtype",null);
	    if(inheritLocation!=null
	       && !entry.hasLocationDefined(null)  &&
	       !entry.hasAreaDefined(null)) {
		Entry parent = entry.getParentEntry();
		while(parent!=null) {
		    if(inheritLocation.equals("*") ||
		       parent.getTypeHandler().isType(inheritLocation)){
			if(parent.hasLocationDefined(null) ||
			   parent.hasAreaDefined(null)) {
			    entry.setLocation(parent);
			    break;
			}
		    }

		    parent = parent.getParentEntry();
		}
	    }

            //Now run the services
            for (Service service : services) {
		applyService(request, entry,service);
	    }
        }

    }

    public void applyService(Request request, Entry entry, Service service) {
	if ( !service.isEnabled()) {
	    return;
	}
	try {
	    //		    System.err.println("service:" + service);
	    File workDir = getStorageManager().createProcessDir();
	    ServiceInput serviceInput = new ServiceInput(workDir, entry);
	    long t1 = System.currentTimeMillis();
	    //	    System.err.println("apply service:" + entry.getName() +" service:" + service.getLabel());
	    ServiceOutput output =
		service.evaluate(getRepository().getAdminRequest(),null,
				 serviceInput, null);
	    long t2 = System.currentTimeMillis();
	    //	    System.err.println("Time:" + (t2-t1));
	    if(output==null) {
		getSessionManager().addSessionMessage(request, "Error processing service:" + service.getLabel()+
						      " for entry:" + entry.getName() +" id:" + entry.getId() +
						      " Error: no service output"); 
		return;
	    }
	    if ( !output.isOk()) {
		getSessionManager().addSessionMessage(request, "Error processing service:" + service.getLabel()+
						      " for entry:" + entry.getName() +" id:" + entry.getId() +
						      " Error: service output is not ok:\n " + output.getResults()); 
		return;
	    }

	    //Defer to the entry's type handler
	    //                    System.err.println("calling handleServiceResults:"  + entry.getTypeHandler());
	    entry.getTypeHandler().handleServiceResults(request,
							entry, service, output);
	} catch (Exception exc) {
	    getSessionManager().addSessionMessage(request, "Error calling service:" + service.getLabel() + " Error: "+ exc.getMessage());
	    getLogManager().logError(
				     "ERROR: TypeHandler calling service:" + service.getLabel()
				     + "\n", exc);
	}
    }

    public void handleServiceResults(Request request, Entry entry,
                                     Service service, ServiceOutput output)
	throws Exception {
        if (parent != null) {
            parent.handleServiceResults(request, entry, service, output);
            return;
        }

        String namePattern = service.getNamePattern();
        if (namePattern != null) {
            String results = output.getResults();
            String name    = StringUtil.findPattern(results, namePattern);
            if (name != null) {
                entry.setName(name);
            }
	    //            System.err.println("name= " + name);
        }

	//	getLogManager().logSpecial("handle service: " + service);

        String descriptionPattern = service.getDescriptionPattern();
        if (descriptionPattern != null) {
            String results = output.getResults();
            String description = StringUtil.findPattern(results,
							descriptionPattern);
            if (description != null) {
                entry.setDescription(description);
            }
	    //            System.err.println("description= " + description);
        }

        List<Entry> entries = output.getEntries();
        if (entries.size() == 0) {
	    //	    getLogManager().logSpecial("handle service no entries ");
            return;
        }

        String target = service.getTarget();
        if (target == null) {
            //            System.err.println("TypeHandler: No target for service:" + service);
	    //	    getLogManager().logSpecial("handle service no target");
            return;
        }
	//	getLogManager().logSpecial("Service: " + target);
        if (target.equals(TARGET_ATTACHMENT)) {

            for (Entry serviceEntry : entries) {
                String fileName = getStorageManager().copyToEntryDir(entry,
								     serviceEntry.getFile()).getName();

                String mtype = isImage(serviceEntry)
		    ? ContentMetadataHandler.TYPE_THUMBNAIL
		    : ContentMetadataHandler.TYPE_ATTACHMENT;
                Metadata metadata = new Metadata(getRepository().getGUID(),
						 entry.getId(), getMetadataManager().findType(mtype),
						 false,
						 fileName, null, null, null, null);
		//		getLogManager().logSpecial("service: added attachment:" + metadata);
                getMetadataManager().addMetadata(request,entry, metadata);
            }
        } else if (target.equals(TARGET_SIBLING)
                   || target.equals(TARGET_CHILD)) {
            for (Entry serviceEntry : entries) {
                File   f    = serviceEntry.getFile();
                String name = f.getName();
                f = getStorageManager().copyToStorage(request, f,
						      getStorageManager().getStorageFileName(f.getName()));

                TypeHandler typeHandler = null;
                if (service.getTargetType() != null) {
                    typeHandler = getRepository().getTypeHandler(
								 service.getTargetType());
                }
                if (typeHandler == null) {
                    typeHandler = getEntryManager().findDefaultTypeHandler(request,
									   f.toString());
                }
                Entry parent = target.equals(TARGET_CHILD)
		    ? entry
		    : entry.getParentEntry();
                Entry newEntry = getEntryManager().addFileEntry(request, f,
								parent, null, name, "", request.getUser(),
								typeHandler, null);

                getAuthManager().addAuthToken(request);
                getAssociationManager().addAssociation(request, entry,
						       newEntry, "derived", "derived");
            }
        } else {
            System.err.println("Unknown target:" + target);
        }
    }

    public boolean isImage(Entry entry) {
        if (isType("type_image")) {
            return true;
        }
        if (entry.getResource().isImage()) {
            return true;
        }

        return false;
    }

    public boolean isDescriptionWiki(Entry entry) {
        return getProperty(entry, "form.description.iswiki", false);
    }

    public String getUploadedFile(Request request) {
        return request.getUploadedFile(ARG_FILE);
    }

    public void initializeCopiedEntry(Entry newEntry, Entry oldEntry)
	throws Exception {
        if (parent != null) {
            parent.initializeCopiedEntry(newEntry, oldEntry);
        }
    }

    public List<TwoFacedObject> getListTypes(boolean longName) {
        return new ArrayList<TwoFacedObject>();

    }

    public Result processList(Request request, String what) throws Exception {
        return new Result("Error",
                          new StringBuilder(msgLabel("Unknown listing type")
                                            + what));
    }

    public String getEntriesTableName() {
        return Tables.ENTRIES.NAME;
    }

    public String getTableName() {
        return "";
    }

    private String cleanQueryString(String s) {
        s = s.replace("\r\n", " ");
        s = StringUtil.stripAndReplace(s, "'", "'", "'dummy'");

        return s;
    }

    public Statement select(Request request, String what, Clause clause,
                            String extra)
	throws Exception {
        List<Clause> clauses = new ArrayList<Clause>();
        clauses.add(clause);

        return select(request, what, clauses, extra);
    }

    public Statement select(Request request, String what,
                            List<Clause> clauses, String extra)
	throws Exception {

        clauses = new ArrayList<Clause>(clauses);

        //We do the replace because (for some reason) any CRNW screws up the pattern matching
        String       whatString   = cleanQueryString(what);
        String       extraString  = cleanQueryString(extra);

        List<String> myTableNames = new ArrayList<String>();
	myTableNames.add(getEntriesTableName());
        getTableNames(myTableNames);

        List<String> tableNames = (List<String>) Misc.toList(new String[] {
		Tables.ENTRIES.NAME,
		Tables.METADATA.NAME,
		Tables.USERS.NAME,
		Tables.ASSOCIATIONS.NAME });
        tableNames.addAll(myTableNames);
        HashSet seenTables = new HashSet();

        List    tables     = new ArrayList();
        boolean didEntries = false;
        boolean didMeta    = false;

        int     cnt        = 0;
        for (String tableName : tableNames) {
            String pattern = ".*[, =\\(]+" + tableName + "\\..*";
            if (Clause.isColumnFromTable(clauses, tableName)
		|| whatString.matches(pattern)
		|| (extraString.matches(pattern))) {
                tables.add(tableName);
                if (tableName.equals(Tables.ENTRIES.NAME)) {
                    didEntries = true;
                } else if (tableName.equals(Tables.METADATA.NAME)) {
                    didMeta = true;
                } else if (myTableNames.contains(tableName)) {
                    seenTables.add(tableName);
                }
            }
            cnt++;
        }

        if (didMeta) {
            tables.add(Tables.METADATA.NAME);
            didEntries = true;
        }

        int metadataCnt = 0;

        while (true) {
            String subTable = Tables.METADATA.NAME + "_" + metadataCnt;
            metadataCnt++;
            if ( !Clause.isColumnFromTable(clauses, subTable)) {
                break;
            }
            tables.add(Tables.METADATA.NAME + " " + subTable);
        }

        if (didEntries) {
            List<String> typeList = (List<String>) request.get(ARG_TYPE,
							       new ArrayList());
	    getDatabaseManager().addTypeClause(getRepository(),request, typeList,clauses);
        }

        if (isOrSearch(request)) {
            Clause clause = Clause.or(clauses);
            clauses = new ArrayList<Clause>();
            clauses.add(clause);
        }

        //The join
        if (didEntries) {
            for (String otherTableName : myTableNames) {
                if (seenTables.contains(otherTableName)
		    && !Tables.ENTRIES.NAME.equalsIgnoreCase(
							     otherTableName)) {
                    clauses.add(0, Clause.join(Tables.ENTRIES.COL_ID,
					       otherTableName + ".id"));
                }
            }
        }

        //        System.err.println("clauses:" + clauses);
        int max = request.get(ARG_MAX, getRepository().getDefaultMaxEntries());
        return getDatabaseManager().select(what, tables, Clause.and(clauses),
                                           extra, max);

    }

    public void addWidgetHelp(Request request,Entry entry,Appendable formBuffer,Column column,Object[]values) throws Exception {
    }

    public void addNewEntryPageHeader(Request request, Entry group,Appendable sb) throws Exception {

	String msg = "+callout-info\n";
	msg+="You are adding a new " + getDescription() + " to the entry: " + HU.italics(group.getName());

	if (Utils.equals(getType(),TYPE_FILE)) {
	    msg+="<br>RAMADDA will try to guess at the new entry type based on the file name";
	}

	msg+="\n-callout\n";
	sb.append(getWikiManager().wikify(request, msg));

    }

    public void addToEntryFormHeader(Request request, Appendable sb,Entry entry) throws Exception {
	String header = getTypeProperty(entry==null?"form.header.new":"form.header", (String) null);
	if(header==null) header = getTypeProperty("form.header", (String) null);
	if(header!=null) {
	    header = header.replace("\\n","\n");
	    sb.append(getWikiManager().wikify(request, header));
	}
	if(entry==null) {
	    if(stringDefined(newHelp)) {
		sb.append(getWikiManager().wikify(request, HU.div(newHelp,HU.cssClass("ramadda-form-help"))));
	    }	
	} else {
	    if(stringDefined(editHelp)) {
		sb.append(getWikiManager().wikify(request, HU.div(editHelp,HU.cssClass("ramadda-form-help"))));
	    }
	}

    }

    public void addToEntryForm(Request request, Appendable sb,
                               Entry parentEntry, Entry entry,
                               FormInfo formInfo)
	throws Exception {

        try {
	    HashSet seen = new HashSet();
	    GroupedBuffers buffers = new GroupedBuffers(getTypeProperty("form.basic.group","Basic"));
            addBasicToEntryForm(request, buffers, parentEntry, entry, formInfo, this,seen);
	    if(!seen.contains(FIELD_COLUMNS)) {
		addSpecialToEntryForm(request, buffers, parentEntry, entry, formInfo,this, seen);
	    }

            if ((entry != null) && request.getUser().getAdmin()
		&& okToShowInForm(entry, "owner", true)) {
                String ownerInputId = Utils.getGuid();
		buffers.setGroup("Admin");
                buffers.append(formEntry(request, msgLabel("Owner"),
					 HU.input(ARG_USER_ID,
						  ((entry != null)
						   ? entry.getUser().getId()
						   : ""), HU.SIZE_20
						  + HU.attr("id", ownerInputId)) + " "
					 + msg("Optionally specify an owner")));
                HU.script(
			  buffers,
			  HU.call(
				  "HU.initInteractiveInput",
				  HU.squote(ownerInputId),
				  HU.squote(
					    getRepository().getUrlBase() + "/user/search")));
            }

	    List<Appendable>contents = buffers.getBuffers();
	    List<String>groups = buffers.getGroups();
	    if(getTypeProperty("form.tabs",false) && contents.size()>1) {
		sb.append(HU.formTableClose());
		List<Appendable> tabContents = new ArrayList<Appendable>();
		for(Appendable a: contents) {
		    StringBuilder tmp = new StringBuilder();
		    tmp.append(HU.formTable("ramadda-entry-edit",true));
		    tmp.append(a);
		    tmp.append(HU.formTableClose());
		    tabContents.add(tmp);
		}
		HU.makeTabs(sb,groups,tabContents);
		sb.append(HU.formTable("ramadda-entry-edit",true));
	    } else {
		for(int i=0;i<contents.size();i++) {
		    String group = groups.get(i);
		    if(i>0 &&stringDefined(group))  {
			sb.append(HU.row(HU.colspan(
						    HU.div(group, " class=\"formgroupheader\" "),
						    2)));
		    }
		    sb.append(contents.get(i).toString());
		}
	    }

        } catch (Exception exc) {
            StringBuilder tmp = new StringBuilder();
            tmp.append(
		       getPageHandler().showDialogError(
							"An error has occurred:" + exc));

            if ((request.getUser() != null) && request.getUser().getAdmin()) {
                Throwable inner = LogUtil.getInnerException(exc);
                tmp.append(
			   HU.pre(
				  HU.entityEncode(LogUtil.getStackTrace(exc)),
				  "style='max-height:300px;overflow-y:auto;'"));
            }
            sb.append(HU.formEntry("", tmp.toString()));
        }

    }

    public void addColumnToEntryForm(Request request, Column column,
                                     Appendable formBuffer, Entry parentEntry,Entry entry,
                                     Object[] values, Hashtable state,
                                     FormInfo formInfo,
                                     TypeHandler sourceTypeHandler)
	throws Exception {
        if ( !column.getShowInForm()) {
            return;
        }

        if ( !sourceTypeHandler.okToShowInForm(entry, column.getName(), true)) {
            return;
        }

	String group = column.getEditGroup();
        if ((group != null) && (state.get(group) == null)) {
	    if(formBuffer instanceof GroupedBuffers) {
		((GroupedBuffers)formBuffer).setGroup(group);
	    } else {
		formBuffer.append(
				  HU.row(
					 HU.colspan(
						    HU.div(group, " class=\"formgroupheader\" "),
						    2)));
	    }
	    state.put(group, group);
        }
	if(group!=null && geoPosition!=null && Utils.equals("group:"+group, geoPosition)) {
	    addSpatialToEntryForm(request, formBuffer, parentEntry, entry, formInfo);
	}

        boolean canEdit    = sourceTypeHandler.getEditable(column);
        boolean canDisplay = sourceTypeHandler.getCanDisplay(column);
        boolean hasValue = entry!=null?entry.getStringValue(request,column,null) != null:false;
        if ((entry != null) && hasValue && !canEdit) {
            if (canDisplay) {
                StringBuilder tmpSb = new StringBuilder();
                column.formatValue(request, entry, tmpSb, Column.OUTPUT_HTML,
                                   values, false);
                formBuffer.append(HU.formEntry(column.getDisplayLabel()
					       + ":", tmpSb.toString()));
            }
        } else if (canEdit) {
            addColumnToEntryForm(request, parentEntry, entry, column, formBuffer, values,
                                 state, formInfo, sourceTypeHandler);
        }

    }

    public void addColumnToEntryForm(Request request, Entry parentEntry, Entry entry,
                                     Column column, Appendable formBuffer,
                                     Object[] values, Hashtable state,
                                     FormInfo formInfo,
                                     TypeHandler sourceTypeHandler)
	throws Exception {

	if(parent!=null) {
	    parent.addColumnToEntryForm(request, parentEntry, entry,
					column,  formBuffer,
					values, state,
					formInfo,
					sourceTypeHandler);
	    return;
	}
	if(geoPosition!=null && Utils.equals(column.getName(), geoPosition)) {
	    addSpatialToEntryForm(request, formBuffer, parentEntry, entry, formInfo);
	}	

	String subGroup = column.getSubGroup();
	if(subGroup!=null) {
	    HU.formEntry(formBuffer, HU.div(subGroup,HU.clazz("ramadda-entry-subgroup")));
	}

	if(sourceTypeHandler.canShowColumn(column)) {
	    column.addToEntryForm(request, parentEntry, entry, formBuffer, values, state,
				  formInfo, sourceTypeHandler);
	}
    }

    public boolean canShowColumn(Column column) {
	return getTypeProperty("column." + column.getName()+".show",true);
    }

    public String getFormWidget(Request request, Entry entry, Column column,
                                String widget)
	throws Exception {
        if (true) {
            return widget;
        }

        return HU.hbox(widget, getFormHelp(request, entry, column));
    }

    public String getFormHelp(Request request, Entry entry, Column column)
	throws Exception {
        return HU.inset(column.getSuffix(), 5);
    }

    public void addSpecialToEntryForm(Request request, GroupedBuffers sb,
                                      Entry parentEntry, Entry entry,
                                      FormInfo formInfo,
                                      TypeHandler sourceTypeHandler,HashSet seen)
	throws Exception {
        if (parent != null) {
            parent.addSpecialToEntryForm(request, sb, parentEntry, entry,
                                         formInfo, sourceTypeHandler, seen);

            return;
        }
    }

    public void addSpatialToEntryForm(Request request, Appendable sb,
                                      Entry parentEntry, Entry entry,
                                      FormInfo formInfo)
	throws Exception {

        MapOutputHandler mapOutputHandler =
            (MapOutputHandler) getRepository().getOutputHandler(
								MapOutputHandler.OUTPUT_MAP.getId());
        if (okToShowInForm(entry, ARG_LOCATION, false)) {
            String lat = request.getString(ARG_LATITUDE,"");
            String lon = request.getString(ARG_LONGITUDE,"");	    
            if (entry != null) {
                if (entry.hasNorth()) {
                    lat = "" + entry.getNorth(request);
                }
                if (entry.hasWest()) {
                    lon = "" + entry.getWest(request);
                }
            }
            String locationWidget = msgLabel(getFormLabel(parentEntry,entry, "latitude","Latitude")) + " "
		+ HU.input(
			   ARG_LOCATION_LATITUDE, lat,
			   HU.SIZE_10) + "  "
		+ msgLabel(getFormLabel(parentEntry,entry, "longitude","Longitude")) + " "
		+ HU.input(
			   ARG_LOCATION_LONGITUDE, lon,
			   HU.SIZE_10);

            String[] nwse = new String[] { lat, lon };
            //            sb.append(formEntry(request, msgLabel("Location"),  locationWidget));
            MapInfo map = getMapManager().createMap(request, entry, true,
						    getMapManager().getMapProps(request, entry,
										null));
	    getMapManager().initMapSelector(request, this,parentEntry, entry, map);

            String mapSelector = map.makeSelector(ARG_LOCATION, true, nwse,
						  "", "");
	    String help = getTypeProperty("form.location.help",null);
	    if(stringDefined(help)) {
		sb.append(formEntry(request,"",TypeHandler.wrapHelp(help)));
	    }

            sb.append(formEntry(request, msgLabel(getFormLabel(parentEntry,entry,"location","Location")), mapSelector));

        } else if (okToShowInForm(entry, ARG_AREA)) {
            addAreaWidget(request, parentEntry, entry, sb, formInfo);
        }

        if (okToShowInForm(entry, ARG_ALTITUDE, false)) {
            String altitude = "";
            if ((entry != null) && entry.hasAltitude()) {
                altitude = "" + Misc.format(entry.getAltitude());
            }
            sb.append(formEntry(request, "Altitude:",
                                HU.input(ARG_ALTITUDE, altitude,
					 HU.SIZE_10)));
        } else if (okToShowInForm(entry, ARG_ALTITUDE_TOP, false)) {
            String altitudeTop    = "";
            String altitudeBottom = "";
            if (entry != null) {
                if (entry.hasAltitudeTop()) {
                    altitudeTop = "" + Misc.format(entry.getAltitudeTop());
                }
                if (entry.hasAltitudeBottom()) {
                    altitudeBottom =
                        "" + Misc.format(entry.getAltitudeBottom());
                }
            }
            sb.append(formEntry(request, "Altitude Range:",
                                HU.input(ARG_ALTITUDE_BOTTOM,
					 altitudeBottom,
					 HU.SIZE_10) + " - "
				+ HU.input(ARG_ALTITUDE_TOP,
					   altitudeTop,
					   HU.SIZE_10) + " "
				+ msg("meters")));
        }

    }

    public void addAreaWidget(Request request, Entry parentEntry,
			      Entry entry, Appendable sb,
                              FormInfo formInfo)
	throws Exception {
        String[]                  nwse  = null;
        Hashtable<String, String> props = null;

        if (entry != null) {
            props = getMapManager().getMapProps(request, entry, props);
            nwse  = new String[] { entry.hasNorth()
                                   ? "" + entry.getNorth(request)
                                   : "", entry.hasWest()
				   ? "" + entry.getWest(request)
				   : "", entry.hasSouth()
				   ? "" + entry.getSouth(request)
				   : "", entry.hasEast()
				   ? "" + entry.getEast(request)
				   : "", };

        }  
        String extraMapStuff = "";
        MapInfo map = getRepository().getMapManager().createMap(request,
								entry, "600", "300", true, props);
	getMapManager().initMapSelector(request, this,parentEntry, entry, map);
        String mapSelector = map.makeSelector(ARG_AREA, true, nwse, "", "")
	    + extraMapStuff;
	String help = getTypeProperty("form.location.help",null);
	if(stringDefined(help)) {
	    sb.append(formEntry(request,"",TypeHandler.wrapHelp(help)));
	}

        sb.append(formEntry(request, msgLabel(getFormLabel(parentEntry,entry,"location","Location")), mapSelector));
    }

    public void addDateToEntryForm(Request request, GroupedBuffers sb,
                                   Entry parentEntry,Entry entry)
	throws Exception {
	String group = getTypeProperty("form.date.group",null);
	if(group!=null)
	    sb.setGroup(group);

        String dateHelp = " (e.g., 2007-12-11 00:00:00)";
        /*        String fromDate = ((entry != null)
		  ? getDateHandler().formatDate(request,entry,entry.getStartDate())
		  : BLANK);
		  String toDate = ((entry != null)
		  ? getDateHandler().formatDate(request, entry, entry.getEndDate())
		  : BLANK);*/

        String  timezone  = ((entry == null)
                             ? null
                             : getEntryUtil().getTimezone(request, entry));

        Date[]  dateRange = getDefaultDateRange(request, entry);
        Date    fromDate  = dateRange[0];
        Date    toDate    = dateRange[1];

        boolean showTime  = okToShowInForm(entry, "time", true);
        if (okToShowInForm(entry, ARG_DATE)) {
            String setTimeCbx = "";
            /*
	      if (okToShowInForm(entry, "settimerange") && entry != null && entry.isGroup()) {
	      setTimeCbx = HU.checkbox(
	      ARG_SETTIMEFROMCHILDREN, "true",
	      false) + " "
	      + msg("Set time range from children");
	      }
            */

	    String help = getProperty(entry,"form.date.help",null);
	    if(help!=null)
		sb.append(HU.formEntry("",wrapHelp(help)));
            if ( !okToShowInForm(entry, ARG_TODATE)) {
                sb.append(
			  formEntry(
				    request,
				    msgLabel(getFormLabel(parentEntry,entry, ARG_DATE, "Date")),
				    getDateHandler().makeDateInput(
								   request, ARG_FROMDATE, "entryform", fromDate,
								   timezone, showTime) + " " + setTimeCbx));

            } else {
                sb.append(
			  formEntry(
				    request,
				    msgLabel(
					     getFormLabel(parentEntry,
							  entry, ARG_DATE,
							  "Date Range")), getDateHandler()
                                    .makeDateInput(
						   request, ARG_FROMDATE, "entryform",
						   fromDate, timezone,
						   showTime) + HU.space(1)
				    + HtmlUtils
				    .img(getIconUrl(
                                                    ICON_RANGE)) + HtmlUtils
				    .space(1) +
				    //                        " <b>--</b> " +
				    getDateHandler().makeDateInput(request, ARG_TODATE,
								   "entryform", toDate, timezone,
								   showTime) + HU.space(2) + " " + setTimeCbx));
            }

        }
    }

    public Date[] getDefaultDateRange(Request request, Entry entry) {
        Date fromDate = ((entry != null)
                         ? new Date(entry.getStartDate())
                         : null);
        Date toDate   = ((entry != null)
                         ? new Date(entry.getEndDate())
                         : null);

        return new Date[] { fromDate, toDate };
    }

    public void addBasicToEntryForm(Request request, GroupedBuffers sb,
                                    Entry parentEntry, Entry entry,
                                    FormInfo formInfo,
                                    TypeHandler sourceTypeHandler,
				    HashSet seen)
	throws Exception {

        Hashtable state = new Hashtable();
        String theSize = HU.SIZE_70;
	String size = theSize;
        boolean forUpload = (entry == null)
	    && getType().equals(TYPE_CONTRIBUTION);

        if (forUpload) {
            sb.append(formEntry(request, msgLabel("Your Name"),
                                HU.input(ARG_CONTRIBUTION_FROMNAME,
					 "", size)));
            sb.append(formEntry(request, msgLabel("Your Email"),
                                HU.input(ARG_CONTRIBUTION_FROMEMAIL,
					 "", size)));
        }

        Object[] values = entry==null?null:entry.getValues();
	String[] editFields = getEditFields();
	String[] newFields = getNewFields();	
        String[] whatList = entry==null?newFields:editFields;
	if(whatList==null) whatList = editFields;
	if(whatList==null) whatList = entry == null  ? FIELDS_NOENTRY: FIELDS_ENTRY;

        for (String what : whatList) {
            if (what.equals("quit")) {
		seen.add(FIELD_COLUMNS);
		break;
	    }
            if (what.equals(FIELD_HR)) {
		sb.append("<tr><td colspan=2><hr class=ramadda-hr></td></tr>");
		continue;
	    }

            if (what.startsWith(FIELD_LABEL)) {
		String label  =what.substring(FIELD_LABEL.length()+1);
		HU.formEntry(sb, HU.center(HU.b(label)));
		continue;
	    }

	    if(seen.contains(what)) continue;
	    seen.add(what);
	    size = getProperty(entry, "form." + what+".size",theSize);
            if (what.equals(FIELD_COLUMNS)) {
		addSpecialToEntryForm(request, sb, parentEntry, entry, formInfo, this,seen);
		continue;
	    }

	    if(what.equals(FIELD_DOWNLOADFILE)) {
		sb.append(formEntry(request, "",
				    HU.labeledCheckbox(ARG_DOWNLOAD_FILE,
						       "true",
						       request.get(ARG_DOWNLOAD_FILE,false),
						       "Download file")));
		continue;
	    }

	    if(what.equals(FIELD_ORDER)) {
		if(!okToShowInForm(entry, what)) continue;
		sb.append(formEntry(request, msgLabel("Order"),
				    HU.input(ARG_ENTRYORDER,
					     ((entry != null)
					      ? entry.getEntryOrder()
					      : 999), HU.SIZE_5) + " 1-N"));
		continue;
	    }		

            if (what.equals(ARG_TAGS)) {
                if ( !getTypeProperty("form.tags.show", true)) {
                    continue;
                }
                StringBuilder tags = new StringBuilder();
                for (int i = 0; i < 3; i++) {
                    tags.append(
				HU.input(
					 ARG_TAGS, "",
					 HU.SIZE_15 + HU.cssClass("metadata-tag-input")));
                }

                if (entry != null) {
                    List<Metadata> metadataList =
                        getMetadataManager().findMetadata(request, entry,
							  new String[] { "enum_tag",
									 "content.keyword" }, false);
                    if ((metadataList != null) && (metadataList.size() > 0)) {
                        for (Metadata metadata : metadataList) {
                            String mtd = metadata.getAttr(1);
                            HU.div(tags, mtd,
                                   HU.cssClass("metadata-tag")
                                   + HU.attr("metadata-tag", mtd)
                                   + HU.attr("metadata-id",
                                             metadata.getId()));
                            tags.append(HU.hidden("metadata_state_"
						  + metadata.getId(), "true",
						  HU.id(metadata.getId())));
                        }
                    }
                }
                HU.script(tags,
			  "Ramadda.initFormTags("
			  + HU.squote(formInfo.getId()) + ");");
                sb.append(formEntry(request, msgLabel("Tags"),
                                    tags.toString()));
		continue;
            }

            if (what.equals(ARG_NAME)) {
                if ( !forUpload && okToShowInForm(entry, ARG_NAME)) {
		    String   domId = "entrynameinput";
                    formInfo.addMaxSizeValidation("Name", domId,
						  Entry.MAX_NAME_LENGTH);
                    String nameDefault = request.getString(ARG_NAME,
							   getFormDefault(entry, ARG_NAME,
									  ""));
		    boolean showLabel = getProperty(entry,"form.name.showlabel",false);
		    String label = getFormLabel(parentEntry,entry, FIELD_NAME,"Name");
		    String input = 
			HU.input(ARG_NAME, ((entry != null)
					    ? entry.getName()
					    : nameDefault), size
				 + HU.attr("autofocus","true")
				 + HU.attr("placeholder", showLabel?"":label)
				 + HU.id(domId));

		    if(showLabel) {
			HU.formEntry(sb,  label+":",input);
		    } else {
			HU.formEntry(sb,  input);
		    }
                } else {
                    String nameDefault = getFormDefault(entry, ARG_NAME,
							null);
                    if (nameDefault != null) {
                        sb.append(HU.hidden(ARG_NAME, nameDefault));
                    }
                }

                continue;
            }

            if (what.equals(ARG_DESCRIPTION)) {
                if (okToShowInForm(entry, ARG_DESCRIPTION)) {
                    String desc = "";
                    int rows = getProperty(
					   entry, "form.description.rows",
					   getRepository().getProperty(
								       "ramadda.edit.rows", 8));
                    boolean isWiki = isDescriptionWiki(entry);
                    if (entry != null) {
                        desc = entry.getDescription();
                    }
		    Entry.EntryHistory entryHistory = (Entry.EntryHistory) formInfo.getHistory();
		    if(entryHistory!=null) {
			desc = entryHistory.getDescription();
		    }

                    if (desc.length() > 100) {
                        rows = rows * 2;
                    }
		    String group = getTypeProperty("form.description.group",null);
		    if(group!=null)
			sb.setGroup(group);

                    if (isWiki) {
			sb.append("<tr><td colspan=2>");
                        addWikiEditor(request, entry, sb, formInfo,
                                      ARG_DESCRIPTION, desc, "",
                                      false, Entry.MAX_DESCRIPTION_LENGTH,
                                      true);
			sb.append("</td></tr>");
                    } else {
                        desc = desc.trim();
                        boolean isTextWiki = isWikiText(desc);
                        if (desc.startsWith(WIKI_PREFIX)) {
                            desc = desc.substring(
						  WIKI_PREFIX.length()).trim();
                        }
                        String        cbxId  = "iswiki";
                        String        textId = ARG_DESCRIPTION;
                        StringBuilder tmpSB  = new StringBuilder();
                        /*
			  For some reason the FA icon inside the label gets shifted over
			  So wrap it in a span with margin spacing
                        */
			String img = HU.span(HU.getIconImage("fa-brands fa-wikipedia-w"),HU.style("margin-left:12px;"));

                        String prefix = "";

                        if (getTypeProperty("form.description.showwiki",
                                            true)) {
                            String cbx = HU.labeledCheckbox(ARG_ISWIKI,
							    "true", isTextWiki,
							    HU.id(cbxId), img);
                            cbx = HU.span(cbx,
                                          HU.cssClass("ramadda-clickable")
                                          + HU.title("Toggle Wiki Editor"));

                            String wikiId = addWikiEditor(request, entry,
							  tmpSB, formInfo,
							  ARG_WIKITEXT, desc, null,
							  false,
							  Entry.MAX_DESCRIPTION_LENGTH,
							  isTextWiki);
                            HU.open(tmpSB, "div",
				    HU.attrs("style",
					     !isTextWiki
					     ? ""
					     : "display:none;", "id",
					     textId + "_block"));
                            tmpSB.append(HU.textArea(ARG_DESCRIPTION,
						     desc, rows, HU.attr("placeholder","Description")+HU.id(textId)));
                            HU.script(tmpSB,
				      "WikiUtil.initWikiEditor("
				      + HU.squote((entry
						   == null)
						  ? ""
						  : entry.getId()) + ","
				      + HU.squote(wikiId) + ","
				      + HU.squote(textId) + ","
				      + HU.squote(cbxId) + ");");

                            HU.close(tmpSB, "div");
                            prefix = cbx + HU.space(2);
                        } else {
                            tmpSB.append(HU.textArea(ARG_DESCRIPTION,
						     desc, rows, HU.id(textId)));
                        }

                        String label =
                            getTypeProperty("form.description.label", "");
                        String edit = prefix + HU.b(label) + "<br>"
			    + tmpSB.toString();
                        sb.append(HU.row(HU.td(edit, "colspan=2")));
                    }
                }

                continue;
            }

            if (what.equals(ARG_RESOURCE)) {
                boolean showFile = okToShowInForm(entry, ARG_FILE);
                boolean showLocalFile = showFile
		    && request.getUser().getAdmin()
		    && okToShowInForm(entry,
				      ARG_SERVERFILE);
                boolean showUrl          = (forUpload
                                            ? false
                                            : okToShowInForm(entry, ARG_URL));

                boolean showResourceForm = okToShowInForm(entry,
							  ARG_RESOURCE);

                if (showResourceForm) {
                    boolean showDownload = showFile
			&& okToShowInForm(entry,
					  ARG_RESOURCE_DOWNLOAD);
                    List<String> tabTitles  = new ArrayList<String>();
                    List<String> tabContent = new ArrayList<String>();
                    String urlLabel = getFormLabel(parentEntry,entry, ARG_URL, "URL");
                    String fileLabel = getFormLabel(parentEntry,entry, ARG_FILE, "File");
                    if (showFile) {
                        String inputId   = formInfo.getId() + "_fileinput";
                        String fileAttrs = HU.id(inputId) + size;
                        String accept = getTypeProperty("file.accept",
							(String) null);
                        if (accept != null) {
                            fileAttrs += HU.attr("accept", accept);
                        }
                        String formContent = HU.fileInput(ARG_FILE, fileAttrs);
                        tabTitles.add(msg(fileLabel));

			boolean showDnd =getTypeProperty("form.dnd.show",true);
                        if (entry == null) {
			    if(showDnd) {
				String icon = HU.img("fas fa-upload");
				formContent += HU.div(HU.div(
							     icon + " " + msg("Or drag files here"),
							     HU.cssClass("ramadda-file-dnd-label")), HU
						      .cssClass(
								"ramadda-file-dnd-target") + HU
						      .id(inputId + "_dnd"));
			    }
			}

			formContent +=HU.script(HU.call("Ramadda.initFormUpload",
							HU.squote(inputId),
							(entry != null)
							? "null"
							: HU.squote(inputId + "_dnd"),entry!=null?"false":"true"));
                        tabContent.add(formContent);
                    }
                    if (showUrl) {
                        String url = "";
                        if ((entry != null) && entry.getResource().isUrl()) {
                            url = entry.getResource().getPath();
                        }

                        String clear =  stringDefined(url)?
			    HU.span(HU.labeledCheckbox(ARG_CLEAR_RESOURCE,"true",false,  "Delete URL"),HU.clazz("ramadda-important")):"";
                        String download = !showDownload
			    ? ""
			    : HU.space(1)
			    + HU.labeledCheckbox(
						 ARG_RESOURCE_DOWNLOAD,
						 "true",false,
						 "Download");
			String label="";
			if(entry != null && entry.isFile()) {
			    label = HU.div("Note: entering a URL will result in the existing file being deleted");
			}
                        String formContent = HU.input(ARG_URL, url,
						      HU.SIZE_80) + label+
			    HU.div(download + HU.space(2) + clear);
                        tabTitles.add(urlLabel);
			//                        tabContent.add(HU.inset(formContent, 8));
                        tabContent.add(formContent);
                    }

                    if (showLocalFile) {
                        StringBuilder localFilesSB = new StringBuilder();
                        localFilesSB.append(HU.formTable());
                        localFilesSB.append(
					    HU.formEntry(
							 msgLabel("File or directory"),
							 HU.input(ARG_SERVERFILE, "", size)
							 + " "
							 + msg("Note: If a directory then all files will be added")));
                        localFilesSB.append(
					    HU.formEntry(
							 msgLabel("Pattern"),
							 HU.input(
								  ARG_SERVERFILE_PATTERN, "",
								  HU.SIZE_10)));
                        localFilesSB.append(HU.formTableClose());
                        if (okToShowInForm(entry, "filesonserver", true)) {
                            tabTitles.add(msg("Files on Server"));
			    //                            tabContent.add(HU.inset(localFilesSB.toString(), 8));
                            tabContent.add(localFilesSB.toString());
                        }
                    }

		    StringBuilder extras = new StringBuilder();
		    extras.append("<div class=ramadda-entry-edit-options>");
                    getFileExtras(request, entry,extras);
		    extras.append("</div>");
		    String extra =
                        HU.makeShowHideBlock(msg("Upload Options"),
					     extras.toString(),
					     false);
                    if (forUpload/* || !showDownload*/) {
                        extra = "";
                    }

                    if ( !okToShowInForm(entry, "resource.extra")) {
                        extra = "";
                    }
                    if (entry == null) {
                        if (tabTitles.size() > 1) {
                            HU.formEntry(sb,
                                         OutputHandler.makeTabs(tabTitles,
								tabContent, true) + extra);
                        } else if (tabTitles.size() == 1) {
			    //HU.formEntry(sb, tabTitles.get(0) + ":",tabContent.get(0) + extra);
			    sb.append("<tr><td colspan=2>");
			    sb.append(HU.b(msgLabel(tabTitles.get(0))) + HU.space(1) +
				      tabContent.get(0) + HU.div(extra,""));
			    sb.append("</td></tr>");

                        }
                    } else {
                        if (showFile
			    && Utils.stringDefined(
						   entry.getResource().getPath())) {
                            String resource = request.getUser().getAdmin()
				? entry.getResource().getPath()
				: getStorageManager().getFileTail(entry);
			    sb.append(HU.formTableClose());
			    sb.append(HU.formTable());

                            HU.formEntry(sb,msgLabel("Resource"), resource);
			    sb.append(HU.formTableClose());
			    sb.append(HU.formTable());

                            /*
                              sb.append(
                              formEntry(
                              request, msgLabel("Resource"),
                              getStorageManager().getFileTail(
                              entry)));
                            */
			}
                        if (tabTitles.size() > 1) {
                            HU.formEntry(sb,
                                         HU.b("New Resource:<br>")
                                         + OutputHandler.makeTabs(tabTitles,
								  tabContent, true) + extra);
                        } else if (tabTitles.size() == 1) {
                            HU.formEntry(sb,
                                         HU.b(tabTitles.get(0) + ":<br>")
                                         + tabContent.get(0) + extra);
                        }

                        /*
			  if (tabTitles.size() > 1) {
			  sb.append(formEntryTop(request,
			  msgLabel("New Resource"),
			  OutputHandler.makeTabs(tabTitles,
			  tabContent, true) + extra));
			  } else if (tabTitles.size() == 1) {
			  sb.append(formEntry(request,
			  tabTitles.get(0) + ":",
			  tabContent.get(0) + extra));
			  }
                        */
                    }

                    continue;
                }

                if (what.equals(ARG_CATEGORY)) {

                    if ( !hasDefaultCategory()
			 && okToShowInForm(entry, ARG_CATEGORY, false)) {
                        String selected = "";
                        if (entry != null) {
                            selected = entry.getCategory();
                        } 
                       List   types  = getRepository().getDefaultCategorys();
                        String widget = ((types.size() > 1)
                                         ? HU.select(
						     ARG_CATEGORY_SELECT, types,
						     selected) + HU.space(1)
					 + msgLabel("Or")
                                         : "") + HU.input(
							  ARG_CATEGORY);
                        sb.append(formEntry(request, msgLabel("Data Type"),
                                            widget));
                    }

                }

                continue;
            }

            if (what.equals(ARG_DATE)) {
                addDateToEntryForm(request, sb, parentEntry,entry);
                continue;
            }
            if (what.equals(ARG_LOCATION)) {
                if(geoPosition==null)
		    addSpatialToEntryForm(request, sb, parentEntry, entry, formInfo);
                continue;
            }

	    Column column = getColumn(what);
	    if(column!=null) {
		String key = "column_"+column.getName();
		if(!seen.contains(key)) {
		    seen.add(key);
		    addColumnToEntryForm(request, column, sb, parentEntry,entry, values,
					 state, formInfo, this);
		}
		continue;
	    }
	    //	    System.err.println("Unknown edit field:" + what);
        }

        if (entry == null) {
            for (String[] idLabel : requiredMetadata) {
                MetadataHandler handler =
                    getMetadataManager().findMetadataHandler(idLabel[0]);
                if (handler != null) {
                    if (idLabel[1] != null) {
                        request.putExtraProperty(
						 MetadataType.PROP_METADATA_LABEL, idLabel[1]);
                    }
                    handler.makeAddForm(request, null,
                                        handler.findType(idLabel[0]), sb);
                    request.removeExtraProperty(
						MetadataType.PROP_METADATA_LABEL);
                    sb.append("<tr><td colspan=2><hr></td></tr>");
                }
            }
        }
    }

    public String[] getEditFields() {
	if(editFields!=null && editFields.length>0) return editFields;
        if (parent != null) {
	    return parent.getEditFields();
	}
	return null;
    }

    public String[] getNewFields() {
	if(newFields!=null && newFields.length>0) return newFields;
        if (parent != null) {
	    return parent.getNewFields();
	}
	return null;
    }

    public boolean downloadUrlAndSaveAsEntryFile(Request request,Entry entry,URL url,String fileName) {
	try {
	    InputStream inputStream = IO.getInputStream(url);
	    File file =  getStorageManager().copyToStorage(request, inputStream,fileName);
	    inputStream.close();
	    entry.setResource(new Resource(file,Resource.TYPE_STOREDFILE));
	} catch(Exception exc) {
	    String entryUrl = getEntryManager().getEntryUrl(request,entry);
	    getSessionManager().addSessionMessage(request,"Error downloading file:" +
						  HU.href(entryUrl,fileName,
							  HU.attrs("target","_entry")) +"<br>&nbsp;&nbsp;" +exc.getMessage());
	    return false;
	}
	return true;
    }

    public static void addExtra(Appendable extras, String label, String contents) {
	if(!Utils.stringDefined(contents)) return;
	try {
	    extras.append("<tr valign=top><td width=1% style='white-space:nowrap;' align=right>");
	    extras.append(HU.b(label));
	    extras.append("</td><td>");
	    //		extras.append(HU.openInset(0, 30, 0, 0));
	    extras.append(contents);
	    extras.append("</td></tr>");
	    //		extras.append(HU.closeInset());
	} catch(Exception exc) {
	    throw new RuntimeException(exc);
	}
    }				

    public void getFileExtras(Request request, Entry entry, Appendable extras)
	throws Exception {
	String space = HU.space(3);

	String uploadFlags = getTypeProperty("upload.flags",null);
	if(uploadFlags!=null) {
	    for(String pair:Utils.split(uploadFlags,",",true,true)) {
		List<String> toks = Utils.splitUpTo(pair,":",3);
		extras.append(HU.labeledCheckbox(toks.get(0), "true", toks.size()==3?toks.get(2).equals("true"):false, toks.size()>=2?toks.get(1):toks.get(0)));
		extras.append("<br>");
	    }
	}

        String unzipWidget =
            HU.labeledCheckbox(ARG_FILE_UNZIP, "true", true, "Unzip archive")+
	    space +
            HU.labeledCheckbox(ARG_FILE_PRESERVEDIRECTORY, "true", false,
			       "Make folders from archive");

	unzipWidget+=HU.space(2)+ HU.input(ARG_ZIP_PATTERN,"",
					   HU.attrs("placeholder","Match pattern","title","If set then only process files that match this pattern"));
        String addMetadata =
            HU.labeledCheckbox(ARG_METADATA_ADD, "true",
			       Misc.equals(getFormDefault(entry, ARG_METADATA_ADD, "false"),
					   "false"), "Add properties") + space+
	    HU.labeledCheckbox(ARG_METADATA_ADDSHORT, "true", false,
			       "Just spatial/temporal properties");



        List datePatterns = new ArrayList();
        datePatterns.add(new TwoFacedObject("", BLANK));
        for (int i = 0; i < DateUtil.DATE_PATTERNS.length; i++) {
            datePatterns.add(DateUtil.DATE_FORMATS[i]);
        }

        String makeNameWidget = HU.labeledCheckbox(ARG_MAKENAME,
						   "true", true, "Make name from filename");
	String dateFormatWidget = HU.hbox(HU.textArea(ARG_DATE_PATTERN,request.getString(ARG_DATE_PATTERN,""),
						      3,50),
					  HU.space(1),
					  "Date formats to use to extract date from filename. e.g.:" +
					  HU.pre("yyyyMMdd\nyyyy-MM-dd\nyyyy_MM_dd\nyyyyMMddHHmm\nyyyy_MM_dd_HHmm\nyyyy-MM-dd_HHmm",HU.style("max-height:50px;overflow-y:auto;")));

        String extraMore = "";
        if ((entry == null) && getType().equals(TYPE_FILE)) {
            extraMore = HU.labeledCheckbox(ARG_TYPE_GUESS, "true",
					   true, "Figure out the type from the file name");
        }

	extras.append("<table>");
	if(entry != null && entry.isFile()) {
	    String file = getStorageManager().getOriginalFilename(entry.getResource().getTheFile().getName());
	    addExtra(extras,
		     "",HU.span(HU.labeledCheckbox(ARG_DELETEFILE,"true", false, "Delete existing file"),HU.clazz("ramadda-important"))
		     +" --- " + file);
	}

	addExtra(extras,"Entry type:",extraMore);
	addExtra(extras,"Zip files:",unzipWidget);	

	String images =	    HU.labeledCheckbox(ARG_STRIPEXIF, "true",
					       request.get(ARG_STRIPEXIF,false),
					       "Strip metadata from images (e.g., lat/lon)");

	HU.formEntry(extras,"",HU.formHelp("Image processing"));
	addExtra(extras,"Images:",images);
	String ocr = getOcrForm(request, entry);
	if(stringDefined(ocr)) {
	    addExtra(extras,"OCR:",ocr);
	} 

	String extract = getLLMManager().getNewEntryExtract(request);
	if(stringDefined(extract))  {
	    HU.formEntry(extras,"",getLLMManager().getLLMWarning());
	    addExtra(extras,"Use LLM to:",extract);
	}


	HU.formEntry(extras,"",HU.formHelp("Metadata processing"));
	addExtra(extras,"Metadata:",addMetadata);
	addExtra(extras,"Entry name:",makeNameWidget);
	if(GeoUtils.reverseGeocodeEnabled()) {
	    String geocode = HU.labeledCheckbox(ARG_REVERSEGEOCODE, "true",
						request.get(ARG_REVERSEGEOCODE,false),
						"Set name from location");
	    addExtra(extras,"Geocode:",geocode);
	}

	HU.formEntry(extras,"",HU.formHelp("Advanced"));
	getEntryManager().makeTypePatternsInput(request, ARG_TYPEPATTERNS,
						extras,request.getString(ARG_TYPEPATTERNS,""));

	addExtra(extras,"Date Format:",dateFormatWidget);	
	if(entry==null)
	    addExtra(extras,"",HU.labeledCheckbox(ARG_TESTNEW,"true", request.get(ARG_TESTNEW,false),"Test the upload"));

	extras.append("</table>");	

    }

    public String getOcrForm(Request request, Entry entry) throws Exception {
	String ocr =  null;
	if(getRepository().getSearchManager().isImageIndexingEnabled()) {
	    String space = HU.space(3);
	    ocr =  HU.labeledCheckbox(ARG_DOOCR, "true", request.get(ARG_DOOCR,false),"Extract text from images if needed");
	    ocr+="<br>";
	    ocr += HU.labeledCheckbox(ARG_DOOCR_CONDITIONAL, "true",request.get(ARG_DOOCR_CONDITIONAL,false) ,
					      "Don't do OCR if there is any text in the document");
	}
	return ocr;
    }

    public String getWikiEditorSidebar(Request request, Entry entry)
	throws Exception {
        return "";
    }

    public void addToSelectMenu(Request request, Entry entry,
                                StringBuilder sb, String type, String target)
	throws Exception {}

    public void addToWikiToolbar(Request request, Entry entry,
                                 StringBuilder buttons, String textAreaId) {}

    public void addReadOnlyWikiEditor(Request request, Entry entry,
                                      Appendable sb, String text)
	throws Exception {
        String dummyId = Utils.getGuid();
        addWikiEditor(request, entry, sb, null, dummyId, text, null, true, 0,
                      true);
    }

    public String addWikiEditor(Request request, Entry entry, Appendable sb,
                                FormInfo formInfo, String hiddenId,
                                String text, String label, boolean readOnly,
                                int length, boolean visible,String...args)
	throws Exception {
        String editorId = hiddenId + "_editor";
        if (text.startsWith(WIKI_PREFIX)) {
            if ( !isDescriptionWiki(entry)) {
                text = text.substring(WIKI_PREFIX.length()).trim();
            }
        }
        String sidebar = "";
        if ( !readOnly) {
            sidebar = getWikiEditorSidebar(request, entry);
            String buttons =
                getRepository().getWikiManager().makeWikiEditBar(request,
								 entry, editorId);
            if (stringDefined(label)) {
                sb.append(HU.b(msgLabel(label)));
                sb.append(HU.br());
            }

            HU.open(sb, "div",
		    HU.attrs("class", "wiki-editor", "style",
			     (visible
			      ? "display:block;"
			      : "display:none;"), "id",
			     editorId + "_block"));
            sb.append(buttons);
        }
        if ((length > 0) && (formInfo != null)) {
            formInfo.addMaxSizeValidation(label, hiddenId, length);
        }
        text = text.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
	String height=getTypeProperty("form.wikieditor.height","500px");
	if(readOnly) {
	    int cnt = Utils.split(text,"\n").size();
	    if(cnt<20)
		height=(cnt+3)+"em";
	}
	for(int i=0;i<args.length;i+=2) {
	    String key = args[i];
	    String value = args[i+1];
	    if(key.equals("height")) height=value;
	}
        String textWidget = HU.div(text,
				   HU.id(editorId)
				   + HU.style("height:"+height+";")
				   + HU.attr("class",
					     "ace_editor"));
        sb.append(HU.hidden(hiddenId, "", HU.id(hiddenId)));
        if (Utils.stringDefined(sidebar)) {
            sb.append(
		      HU.table(
			       HU.row(
				      HU.cols(
					      new String[] { HU.td(textWidget),
							     HU.td(sidebar,
								   " width=10%") }))));
        } else {
            sb.append(textWidget);
        }

        if (getWikiManager().initWikiEditor(request, sb)) {
            if ((formInfo != null) && !readOnly) {
                formInfo.appendExtraJS(
				       "WikiUtil.handleWikiEditorSubmit();\n");
            }
        }

	String authToken = request.getAuthToken();

        sb.append(HU.script(HU.call("new WikiEditor",
				    HU.squote((entry == null)
					      ? ""
					      : entry.getId()),
				    ((formInfo == null)
				     ? "null"
				     : HU.squote(formInfo.getId())),
				    HU.squote(editorId),
				    HU.squote(hiddenId),
				    JsonUtil.map("authToken",HU.squote(authToken)))));

        if ( !readOnly) {
            HU.close(sb, "div");
        }

        return editorId;
    }

    public boolean haveDatabaseTable() {
        return false;
    }

    public List<Column> getColumns() {
        return null;
    }

    public List<Column> getColumnsForPointJson() {
        return getColumns();
    }

    public String getFieldLabel(String field) {
	if(field.equals(FIELD_NAME)) return  "Name";
	else if(field.equals(FIELD_FILE)) return  "Size";
	else if(field.equals(FIELD_CREATEDATE)) return  "Created";
	else if(field.equals(FIELD_CHANGEDATE)) return  "Last Updated";			
	else if(field.equals(FIELD_FROMDATE)) return  "From Date";
	else if(field.equals(FIELD_TODATE)) return  "To Date";
	else {
	    Column column = getColumn(field);
	    if(column!=null) return column.getDisplayLabel();
	}
	return Utils.makeLabel(field);
    }

    public Column getColumn(String columnName) {
        List<Column> columns = getColumns();
        if (columns == null) {
            return null;
        }
	if(columnMap==null) {
	    Hashtable<String,Column> tmp= new Hashtable<String,Column>();
	    for(Column column: columns) {
		tmp.put(column.getName(),column);
	    }
	    columnMap = tmp;
	}
	return columnMap.get(columnName);
    }

    public String applyTemplate(Request request,Entry entry, String template) throws Exception {
	return applyTemplate(request,entry, template, false);
    }	

    public String applyTemplate(Request request, Entry entry, String template,boolean includeMetadata) throws Exception {		    
	List<Column> columns =getColumns();
	if (columns != null) {
	    for (Column column : columns) {
		String s = entry.getStringValue(request, column,null);
		if (s != null) {
		    template = template.replace("${"
						+ column.getName() + "}", s);
		}
	    }
	}
	if(includeMetadata) {
	    List<Metadata> existingMetadata = getMetadataManager().getMetadata(null,entry);
	    for(Metadata mtd: existingMetadata) {
		for(int i=1;i<10;i++) {
		    String v = mtd.getAttr(i);
		    if(v==null) break;
		    String key = mtd.getType()+"."+i;
		    template = template.replace("${" + key +"}",v);
		}
	    }
	}	    

	template = template.replace("${entryid}",entry.getId());
	template = template.replace("${entryname}",entry.getName());
	template = template.replace("${description}",entry.getDescription());		
	return template;
    }

    public List<String> getColumnEnumerationProperties(Column column,
						       String propertyValue, String delimiter)
	throws Exception {
        if (propertyValue.startsWith("resource:")) {
            return getMetadataManager().getTypeResource(
							propertyValue.substring("resource:".length()));
        }

        if (propertyValue.startsWith("file:")) {
            //replace any macros {name} is the type id without the leading type_
            propertyValue = propertyValue.replace("${type}",
						  type).replace("${name}", type.replace("type_", ""));
            propertyValue = getStorageManager().readSystemResource(
								   propertyValue.substring("file:".length()));
            delimiter = "\n";
        }
        List<String> tmp = Utils.split(propertyValue, delimiter,true,false);

        return tmp;
    }

    public final String getIconUrl(Request request, Entry entry)
	throws Exception {
        return null;
    }

    public String getNameSort(Entry entry) {
	return entry.getName();
    }

    public String getDisplayAttribute(Request request,Entry entry, String attribute) {
        List<Column> columns = getColumns();
        if (columns != null) {
            for (Column column : columns) {
                String s    = entry.getStringValue(request, column,null);
		if(s==null) continue;
                String attr = column.getDisplayAttribute(attribute, s);
                if (attr != null) {
                    return attr;
                }
            }
        }

        return null;
    }

    public String decorateValue(Request request, Entry entry, Column column,
                                String s) {
        if (parent != null) {
            return parent.decorateValue(request, entry, column, s);
        }

        return column.decorate(s);
    }

    public String getDictionary() {
	if(dictionary!=null)
	    return dictionary;
        if (parent != null) {
	    return parent.getDictionary();
	}
	return null;
    }

    public String getEntryIconUrl(Request request, Entry entry)
	throws Exception {
	//check if the entry has a field for the icon
        String field = getTypeProperty("icon.column",null);
        if (field!=null) {
	    String value = entry.getStringValue(request, field,null);
	    if(stringDefined(value)) {
		value = value.trim();
		String icon = getRepository().getProperty("icon." + value,   (String) null);

		if(icon==null)
		    icon = getRepository().getProperty(value,   (String) null);

		if(icon==null) {
		    value = value.toLowerCase();
		    icon = getRepository().getProperty("icon." + value,   (String) null);
		}
		if(stringDefined(icon)) return icon;
	    }
        }

        String icon = entry.getIcon();
        if (icon != null) {
            return getIconUrl(icon);
        }

        icon = getIconProperty(null);
        if (icon != null) {
            return getIconUrl(icon);
        }

        if (entry.isGroup()) {
            if (getAccessManager().hasPermissionSet(entry,
						    Permission.ACTION_VIEWCHILDREN)) {
                if ( !getAccessManager().canDoViewChildren(request, entry)) {
                    return getIconUrl(ICON_FOLDER_CLOSED_LOCKED);
                }
            }

            return getIconUrl(ICON_FOLDER_CLOSED);
        }
        Resource resource = entry.getResource();
        String   path     = resource.getPath();

        return getIconUrlFromPath(path);
    }

    public String getIconUrlFromPath(String path) throws Exception {
        String img = ICON_FILE;
        if (path != null) {
            String suffix = IO.getFileExtension(path.toLowerCase());
            String prop   = getRepository().getProperty("file.icon" + suffix);
            if (prop != null) {
                img = prop;
            }
        }

        return getIconUrl(img);
    }

    public String getLabelFromPath(String path) throws Exception {
        if (path != null) {
            String suffix = IO.getFileExtension(path.toLowerCase());
            String label  = getRepository().getProperty("file.label"
							+ suffix);
            if (label != null) {
                return label;
            }
        }

        return getTypeProperty("file.label", (String) null);
    }

    public String getUrlFromPath(String path) throws Exception {
        if (path != null) {
            String suffix = IO.getFileExtension(path.toLowerCase());
            String url    = getRepository().getProperty("file.url" + suffix);
            if (url != null) {
                return url;
            }
        }

        return getTypeProperty("file.url", (String) null);
    }

    public void addTextSearch(Request request, Appendable sb, String type) {
        try {
            String name           = (String) request.getString(ARG_TEXT, "");
            String searchMetaData = " ";
            /*HU.checkbox(ARG_SEARCHMETADATA,
	      "true",
	      request.get(ARG_SEARCHMETADATA,
	      false)) + " "
	      + msg("Search metadata");*/

            String searchExact = " "
		+ HU.labeledCheckbox(ARG_EXACT, "true",
				     request.get(ARG_EXACT, false),
				     "Match exactly");
            String extra = HU.p() + searchExact + searchMetaData;
            if (getDatabaseManager().supportsRegexp()) {
                extra = HU.labeledCheckbox(
					   ARG_ISREGEXP, "true",
					   request.get(ARG_ISREGEXP, false),
					   "Use regular expression");

                extra = HU.makeToggleInline(msg("More..."), extra,
					    false);
            } else {
                extra = "";
            }

            sb.append(formEntry(request, msgLabel("Text"),
                                HU.input(ARG_TEXT, name,
					 HU.id("searchinput")
					 + HU.SIZE_50
					 + " autocomplete='off' autofocus ") + " "
				+ extra));
            sb.append("<div id=searchpopup class=ramadda-popup></div>");
            sb.append(
		      HU.script(
				"Utils.searchSuggestInit('searchinput'," + ((type == null)
									    ? "null"
									    : "'" + type + "'") + ");"));
        } catch (java.io.IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public void addToSpecialSearchForm(Request request,
                                       Appendable formBuffer,
                                       HashSet<String> fieldsToShow)
	throws Exception {
        if (parent != null) {
            parent.addToSpecialSearchForm(request, formBuffer, fieldsToShow);
        }
    }

    public void addToSearchForm(Request request, List<String> titles,
                                List<String> contents, List<Clause> where,
                                boolean advancedForm, boolean showText)
	throws Exception {

        if (parent != null) {
            parent.addToSearchForm(request, titles, contents, where,
                                   advancedForm, showText);

            return;
        }

        String type = null;
        if (request.defined(ARG_TYPE)) {
            TypeHandler typeHandler = getRepository().getTypeHandler(request);
            type = typeHandler.getType();
            if ( !typeHandler.isAnyHandler()) {
                //                typeHandlers.clear();
                //                typeHandlers.add(typeHandler);
            }
        }

        /*
	  if(minDate==null || maxDate == null) {
	  Statement stmt = select(request,
	  SqlUtil.comma(
	  SqlUtil.min(Tables.ENTRIES.COL_FROMDATE),
	  SqlUtil.max(
	  Tables.ENTRIES.COL_TODATE)), where);

	  ResultSet dateResults = stmt.getResultSet();
	  if (dateResults.next()) {
	  if (dateResults.getDate(1) != null) {
	  if(minDate == null)
	  minDate = SqlUtil.getDateString("" + dateResults.getDate(1));
	  if(maxDate == null)
	  maxDate = SqlUtil.getDateString("" + dateResults.getDate(2));
	  }
	  }
	  }
	*/

        //        minDate = "";
        //        maxDate = "";

        StringBuilder basicSB    = new StringBuilder(HU.formTable());
        StringBuilder advancedSB = new StringBuilder(HU.formTable());

        if (showText) {
            addTextSearch(request, basicSB, type);
        }

        List<TypeHandler> typeHandlers = getRepository().getTypeHandlers();
        if (true || (typeHandlers.size() > 1)) {
            List tmp = new ArrayList();
            for (TypeHandler typeHandler : typeHandlers) {
                if ( !typeHandler.getForUser()) {
                    continue;
                }
                tmp.add(new TwoFacedObject(msg(typeHandler.getLabel()),
                                           typeHandler.getType()));
            }
            TwoFacedObject anyTfo = new TwoFacedObject(TYPE_ANY, TYPE_ANY);
            if ( !tmp.contains(anyTfo)) {
                tmp.add(0, anyTfo);
            }
            List typeList = request.get(ARG_TYPE, new ArrayList());
            typeList.remove(TYPE_ANY);

	    String typeId = HU.getUniqueId("type_");
            String typeSelect = HU.select(ARG_TYPE, tmp, typeList,
					  (advancedForm
					   ? " MULTIPLE SIZE=4 "
					   : "")+HU.attrs("id",typeId));
            String groupCbx = (advancedForm
                               ? HU.labeledCheckbox(ARG_TYPE_EXCLUDE, "true",
						    request.get(ARG_TYPE_EXCLUDE,
								false),"Exclude")
                               : "");
            basicSB.append(formEntry(request, msgLabel("Kind"),
                                     typeSelect + HU.SPACE +
                                     HU.SPACE + groupCbx));

	    String popupArgs = "{label:'Select entry type',makeButtons:false,after:true,single:false}";
	    basicSB.append(HU.script(HU.call("HtmlUtils.makeSelectTagPopup",
					     HU.quote("#"+typeId),
					     popupArgs)));
        } else if (typeHandlers.size() == 1) {
            basicSB.append(HU.hidden(ARG_TYPE,
				     typeHandlers.get(0).getType()));
            basicSB.append(
			   formEntry(
				     request, msgLabel("Kind"),
				     msg(typeHandlers.get(0).getDescription())));
        }

	basicSB.append(formEntry(request, msgLabel("Creator"),
				 HU.input(ARG_USER_ID,
					  request.getString(ARG_USER_ID,
							    ""))));

        for (DateArgument arg : DateArgument.SEARCH_ARGS) {
            addDateSearch(getRepository(), request, basicSB, arg, false);
        }

        if (advancedForm || request.defined(ARG_GROUP)) {
            String groupArg = (String) request.getString(ARG_GROUP, "");
            String searchChildren = " "
		+ HU.labeledCheckbox(ARG_GROUP_CHILDREN,
				     "true",
				     request.get(ARG_GROUP_CHILDREN,
						 false),"(Search sub-folders)");
            if (groupArg.length() > 0) {
                basicSB.append(HU.hidden(ARG_GROUP, groupArg));
                Entry group = getEntryManager().findGroup(request, groupArg);
                if (group != null) {
                    basicSB.append(formEntry(request, msgLabel("Folder"),
                                             group.getFullName() + "&nbsp;"
                                             + searchChildren));

                }
            } else {

            }
            advancedSB.append("\n");
        }

        if (advancedForm) {
            String             radio = getSpatialSearchTypeWidget(request);
            SelectionRectangle bbox  = request.getSelectionBounds();
            MapInfo map = getRepository().getMapManager().createMap(request,
								    null, true, null);

            String mapSelector = map.makeSelector(ARG_AREA, true,
						  bbox.getStringArray(), "", radio);
            basicSB.append(formEntry(request, msgLabel("Area"), mapSelector));
            basicSB.append("\n");
            addSearchField(request, ARG_FILESUFFIX, basicSB);
        }

        /*
	  if (collection != null) {
	  basicSB.append(formEntry(request,msgLabel("Collection"),
	  collectionSelect));
	  }*/

        basicSB.append(HU.formTableClose());
        advancedSB.append(HU.formTableClose());

        titles.add(msg("Type, date, space"));
        contents.add(basicSB.toString());

    }

    public static String wrapHelp(String help) {
	return HU.div(help,HU.cssClass("ramadda-form-help"));
    }

    public static void addDateSearch(Repository repository, Request request,
                                     Appendable basicSB, DateArgument arg,
                                     boolean showTime)
	throws Exception {

        List dateTypes = new ArrayList();
        dateTypes.add(new TwoFacedObject(msg("Contained by range"),
                                         DATE_SEARCHMODE_CONTAINEDBY));
        dateTypes.add(new TwoFacedObject(msg("Overlaps range"),
                                         DATE_SEARCHMODE_OVERLAPS));
        dateTypes.add(new TwoFacedObject(msg("Contains range"),
                                         DATE_SEARCHMODE_CONTAINS));

        String dateSelectValue;
        List   dateSelect = new ArrayList();
        dateSelect.add(new TwoFacedObject("Search Relative", "none"));
        dateSelect.add(new TwoFacedObject(msg("Past hour"), "-1 hour"));
        dateSelect.add(new TwoFacedObject(msg("Past 3 hours"), "-3 hours"));
        dateSelect.add(new TwoFacedObject(msg("Past 6 hours"), "-6 hours"));
        dateSelect.add(new TwoFacedObject(msg("Past 12 hours"), "-12 hours"));
        dateSelect.add(new TwoFacedObject(msg("Past day"), "-1 day"));
        dateSelect.add(new TwoFacedObject(msg("Past week"), "-7 days"));
        dateSelect.add(new TwoFacedObject(msg("Past 2 weeks"), "-14 days"));
        dateSelect.add(new TwoFacedObject(msg("Past month"), "-1 month"));
        dateSelect.add(new TwoFacedObject(msg("Past 6 months"), "-6 month"));	
        dateSelect.add(new TwoFacedObject(msg("Past year"), "-1 year"));	

        if (request.exists(arg.getRelative())) {
            dateSelectValue = request.getString(arg.getRelative(), "");
        } else {
            dateSelectValue = "none";
        }

        String dateSelectInput = HU.select(arg.getRelative(),
					   dateSelect, dateSelectValue);
        String minDate = request.getDateSelect(arg.getFrom(), (String) null);
        String maxDate = request.getDateSelect(arg.getTo(), (String) null);

        String dateTypeValue = request.getString(arg.getMode(),
						 DATE_SEARCHMODE_DEFAULT);
        String dateTypeInput = HU.select(arg.getMode(), dateTypes,
					 dateTypeValue);

        String noDataMode = request.getString(ARG_DATE_NODATAMODE, "");
        String noDateInput = HU.labeledCheckbox(ARG_DATE_NODATAMODE,
						VALUE_NODATAMODE_INCLUDE,
						noDataMode.equals(VALUE_NODATAMODE_INCLUDE),
						"Include entries with no data times");
        String dateExtra;
        if (false && arg.getHasRange()) {
            dateExtra = HU.makeToggleInline(msg("More..."),
					    HU.br() + HU.formTable(new String[] {
						    msgLabel("Search for data whose time is"), dateTypeInput,
						    msgLabel("Search relative"), dateSelectInput, "",
						    noDateInput
						}), false);
        } else {
	    /*
	      dateExtra = HU.makeToggleInline(msg("More..."),
	      HU.br()+
	      HU.b(msgLabel("Search relative"))+HU.br()+
	      dateSelectInput, false);
	    */
            dateExtra =dateSelectInput;
        }

        String fromField = repository.getDateHandler().makeDateInput(request,
								     arg.getFrom(), "searchform", null, null,
								     showTime);
        String toField = repository.getDateHandler().makeDateInput(request,
								   arg.getTo(), "searchform", null, null, showTime);
        /*
	  basicSB.append(RepositoryManager.formEntryTop(request,
	  msgLabel(arg.getLabel()),
	  + HU.space(1)
	  + HU.img(repository.getIconUrl(ICON_RANGE))
	  + HU.space(1)
	  +  + dateExtra));
        */

        HU.formEntry(basicSB, HU.b(msgLabel(arg.getLabel())));

        basicSB.append(RepositoryManager.formEntryTop(request,
						      msgLabel("From"), fromField));
        basicSB.append(RepositoryManager.formEntryTop(request,
						      msgLabel("To"), toField));
        basicSB.append(RepositoryManager.formEntryTop(request, "",
						      dateExtra));

    }

    public static String getSpatialSearchTypeWidget(Request request) {
	//	String dflt = VALUE_AREA_OVERLAPS
	String value = request.getString(ARG_AREA_MODE,VALUE_AREA_CONTAINS);
        String radio = HU.labeledRadio(ARG_AREA_MODE, VALUE_AREA_CONTAINS,
                                       value.equals( VALUE_AREA_CONTAINS),
				       "Contained by") 
	    + HU.space(1) +
	    HU.labeledRadio(ARG_AREA_MODE, VALUE_AREA_OVERLAPS,
			    value.equals(VALUE_AREA_OVERLAPS),"Overlaps");
        return radio;
    }

    public void addSearchField(Request request, String what, Appendable sb) {
        if (what.equals(ARG_FILESUFFIX)) {
            Utils.append(sb,
                         formEntry(request, msgLabel("File Suffix"),
                                   HU.input(ARG_FILESUFFIX, "",
					    " size=\"8\" ")));
        }
    }

    public boolean isAnyHandler() {
        return getType().equals(TypeHandler.TYPE_ANY);
    }

    public List<Clause> assembleWhereClause(Request request)
	throws Exception {
        return assembleWhereClause(request, new StringBuilder());
    }

    public List<Clause> assembleWhereClause(Request request,
                                            Appendable searchCriteria)
	throws Exception {

        //        Misc.printStack("Assemble where clause", 10);

        if (parent != null) {
            return parent.assembleWhereClause(request, searchCriteria);
        }

        List<Clause> where    = new ArrayList<Clause>();
        List         typeList = request.get(ARG_TYPE, new ArrayList());
        typeList.remove(TYPE_ANY);
        if (typeList.size() > 0) {
            if (request.get(ARG_TYPE_EXCLUDE, false)) {
                addCriteria(request, searchCriteria, "Entry Type!=",
                            StringUtil.join(",", typeList));
            } else {
                addCriteria(request, searchCriteria, "Entry Type=",
                            StringUtil.join(",", typeList));
            }
        }

        if (request.defined(ARG_RESOURCE)) {
            addCriteria(request, searchCriteria, "Resource=",
                        request.getString(ARG_RESOURCE, ""));
            String resource = request.getString(ARG_RESOURCE, "");
            resource = getStorageManager().resourceFromDB(resource);
            DatabaseManager.addOrClause(Tables.ENTRIES.COL_RESOURCE, resource, where);
        }

        if (request.defined(ARG_CATEGORY)) {
            addCriteria(request, searchCriteria, "Category=",
                        request.getString(ARG_CATEGORY, ""));
            DatabaseManager.addOrClause(Tables.ENTRIES.COL_DATATYPE,
					request.getString(ARG_CATEGORY, ""), where);
        }

        if (request.defined(ARG_USER_ID)) {
            addCriteria(request, searchCriteria, "User=",
                        request.getString(ARG_USER_ID, ""));
            DatabaseManager.addOrClause(Tables.ENTRIES.COL_USER_ID,
					request.getString(ARG_USER_ID, ""), where);
        }

        if (request.defined(ARG_FILESUFFIX)) {
            addCriteria(request, searchCriteria, "File Suffix=",
                        request.getString(ARG_FILESUFFIX, ""));
            List<Clause> clauses = new ArrayList<Clause>();
            for (String tok :
		     (List<String>) Utils.split(
						request.getString(ARG_FILESUFFIX, ""), ",", true,
						true)) {
                clauses.add(Clause.like(Tables.ENTRIES.COL_RESOURCE,
                                        "%" + tok));
            }
            if (clauses.size() == 1) {
                where.add(clauses.get(0));
            } else {
                where.add(Clause.or(clauses));
            }
        }

        if (request.defined(ARG_GROUP)) {
            String  groupId = (String) request.getString(ARG_GROUP,
							 "").trim();

            boolean doNot   = groupId.startsWith("!");
            if (doNot) {
                groupId = groupId.substring(1);
            }
            if (groupId.endsWith("%")) {
                Entry group = getEntryManager().findGroup(request,
							  groupId.substring(0, groupId.length() - 1));
                if (group != null) {
                    addCriteria(request, searchCriteria, "Folder=",
                                group.getName());
                }
                where.add(Clause.like(Tables.ENTRIES.COL_PARENT_GROUP_ID,
                                      groupId));
            } else {
                List<String> toks = Utils.split(groupId, "|", true, true);
                if (toks.size() > 1) {
                    List<Clause> ors = new ArrayList<Clause>();
                    for (String tok : toks) {
                        ors.add(Clause.eq(Tables.ENTRIES.COL_PARENT_GROUP_ID,
                                          tok));
                    }
                    where.add(Clause.or(ors));
                } else {
                    Entry group = getEntryManager().findGroup(request);
                    if (group == null) {
                        throw new IllegalArgumentException(
							   msgLabel("Could not find folder") + groupId);
                    }
                    addCriteria(request, searchCriteria, "Folder" + (doNot
								     ? "!="
								     : "="), group.getName());
                    String searchChildren =
                        (String) request.getString(ARG_GROUP_CHILDREN,
						   (String) null);
                    if (Misc.equals(searchChildren, "true")) {
                        Clause sub = (doNot
                                      ? Clause.notLike(
						       Tables.ENTRIES.COL_PARENT_GROUP_ID,
						       group.getId() + Entry.IDDELIMITER
						       + "%")
                                      : Clause.like(
						    Tables.ENTRIES.COL_PARENT_GROUP_ID,
						    group.getId() + Entry.IDDELIMITER
						    + "%"));
                        Clause equals = (doNot
                                         ? Clause
					 .neq(Tables.ENTRIES
					      .COL_PARENT_GROUP_ID, group
					      .getId())
                                         : Clause
					 .eq(Tables.ENTRIES
					     .COL_PARENT_GROUP_ID, group
					     .getId()));
                        where.add(Clause.or(sub, equals));
                    } else {
                        if (doNot) {
                            where.add(
				      Clause.neq(
						 Tables.ENTRIES.COL_PARENT_GROUP_ID,
						 group.getId()));
                        } else {
                            where.add(
				      Clause.eq(
						Tables.ENTRIES.COL_PARENT_GROUP_ID,
						group.getId()));
                        }
                    }
                }
            }
        }

        List<Clause> dateClauses = new ArrayList<Clause>();
        for (DateArgument arg : getDateArgs()) {
            Date[] dateRange = request.getDateRange(arg.getFrom(),
						    arg.getTo(), arg.getRelative(),
						    new Date());
            if ((dateRange[0] != null) || (dateRange[1] != null)) {
                Date date1 = dateRange[0];
                Date date2 = dateRange[1];
                if (arg.forCreateDate() || arg.forChangeDate()) {
                    String column = arg.forCreateDate()
			? Tables.ENTRIES.COL_CREATEDATE
			: Tables.ENTRIES.COL_CHANGEDATE;
                    if (date1 != null) {
                        addCriteria(request, searchCriteria,
                                    msg(arg.getLabel()) + ">=", date1);
                        dateClauses.add(Clause.ge(column, date1));
                    }
                    if (date2 != null) {
                        addCriteria(request, searchCriteria,
                                    msg(arg.getLabel()) + "<=", date2);
                        dateClauses.add(Clause.le(column, date2));
                    }
                    continue;
                }

                if (date1 == null) {
                    date1 = date2;
                }
                if (date2 == null) {
                    date2 = date1;
                }

                String dateSearchMode = request.getString(arg.getMode(),
							  DATE_SEARCHMODE_DEFAULT);
                if (dateSearchMode.equals(DATE_SEARCHMODE_OVERLAPS)) {
                    addCriteria(request, searchCriteria, "To&nbsp;Date&gt;=",
                                date1);
                    addCriteria(request, searchCriteria,
                                "From&nbsp;Date&lt;=", date2);
                    dateClauses.add(Clause.le(Tables.ENTRIES.COL_FROMDATE,
					      date2));
                    dateClauses.add(Clause.ge(Tables.ENTRIES.COL_TODATE,
					      date1));
                } else if (dateSearchMode.equals(
						 DATE_SEARCHMODE_CONTAINEDBY)) {
                    addCriteria(request, searchCriteria,
                                "From&nbsp;Date&gt;=", date1);
                    addCriteria(request, searchCriteria, "To&nbsp;Date&lt;=",
                                date2);
                    dateClauses.add(Clause.ge(Tables.ENTRIES.COL_FROMDATE,
					      date1));
                    dateClauses.add(Clause.le(Tables.ENTRIES.COL_TODATE,
					      date2));
                } else {
                    //DATE_SEARCHMODE_CONTAINS
                    addCriteria(request, searchCriteria,
                                "From&nbsp;Date&lt;=", date1);
                    addCriteria(request, searchCriteria, "To&nbsp;Date&gt;=",
                                date2);
                    dateClauses.add(Clause.le(Tables.ENTRIES.COL_FROMDATE,
					      date1));
                    dateClauses.add(Clause.ge(Tables.ENTRIES.COL_TODATE,
					      date2));
                }
            }

            String noDataMode = request.getString(ARG_DATE_NODATAMODE, "");
            if (noDataMode.equals(VALUE_NODATAMODE_INCLUDE)
		&& (dateClauses.size() > 0)) {
                Clause dateClause = Clause.and(dateClauses);
                dateClauses = new ArrayList<Clause>();
                Clause allEqualClause =
                    Clause.and(
			       Clause.join(
					   Tables.ENTRIES.COL_CREATEDATE,
					   Tables.ENTRIES.COL_FROMDATE), Clause.join(
										     Tables.ENTRIES.COL_FROMDATE,
										     Tables.ENTRIES.COL_TODATE));

                dateClauses.add(allEqualClause);
                dateClauses.add(Clause.or(dateClause, allEqualClause));
                addCriteria(request, searchCriteria, "Include no data times",
                            "");
            }

        }

        if (dateClauses.size() > 1) {
            where.add(Clause.and(dateClauses));
        } else if (dateClauses.size() == 1) {
            where.add(dateClauses.get(0));
        }

        boolean contains = !(request.getString(
					       ARG_AREA_MODE, VALUE_AREA_OVERLAPS).equals(
											  VALUE_AREA_OVERLAPS));

        String[] areaCols = { Tables.ENTRIES.COL_NORTH,
                              Tables.ENTRIES.COL_WEST,
                              Tables.ENTRIES.COL_SOUTH,
                              Tables.ENTRIES.COL_EAST };
        boolean[]    areaLE = { true, false, false, true };
        String[] areaNames  = { "North", "West", "South", "East" };
        Clause       areaClause;
        List<Clause> areaClauses = new ArrayList<Clause>();
        List<SelectionRectangle> rectangles =
            getEntryUtil().getSelectionRectangles(
						  request.getSelectionBounds());
        for (SelectionRectangle rectangle : rectangles) {
            List<Clause> areaExpressions = new ArrayList<Clause>();

            if ( !contains) {
                if (rectangle.hasNorth()) {
                    areaClause = Clause.le(Tables.ENTRIES.COL_SOUTH,
                                           rectangle.getNorth());
                    areaExpressions.add(
					Clause.and(
						   getSpatialDefinedClause(
									   Tables.ENTRIES.COL_NORTH), areaClause));
                }
                if (rectangle.hasSouth()) {
                    areaClause = Clause.ge(Tables.ENTRIES.COL_NORTH,
                                           rectangle.getSouth());
                    areaExpressions.add(
					Clause.and(
						   getSpatialDefinedClause(
									   Tables.ENTRIES.COL_SOUTH), areaClause));
                }

                if (rectangle.hasWest()) {
                    areaClause = Clause.ge(Tables.ENTRIES.COL_EAST,
                                           rectangle.getWest());
                    areaExpressions.add(
					Clause.and(
						   getSpatialDefinedClause(Tables.ENTRIES.COL_EAST),
						   areaClause));
                }
                if (rectangle.hasEast()) {
                    areaClause = Clause.le(Tables.ENTRIES.COL_WEST,
                                           rectangle.getEast());
                    areaExpressions.add(
					Clause.and(
						   getSpatialDefinedClause(Tables.ENTRIES.COL_WEST),
						   areaClause));
                }
            } else {
                double[] values = rectangle.getValues();
                for (int i = 0; i < 4; i++) {
                    if (Double.isNaN(values[i])) {
                        continue;
                    }
                    double areaValue = values[i];
                    areaClause = areaLE[i]
			? Clause.le(areaCols[i], areaValue)
			: Clause.ge(areaCols[i], areaValue);
                    areaExpressions.add(
					Clause.and(
						   getSpatialDefinedClause(areaCols[i]),
						   areaClause));
                }
            }
            if (areaExpressions.size() > 0) {
                areaClauses.add(Clause.and(areaExpressions));
            }
        }

        if (areaClauses.size() == 1) {
            //            System.err.println("Single:" + areaClauses.get(0));
            where.add(areaClauses.get(0));
        } else if (areaClauses.size() > 1) {
            //            System.err.println("Multiple:" + areaClauses);
            where.add(Clause.or(areaClauses));
        }

        Hashtable args        = request.getArgs();
        String metadataPrefix = ARG_METADATA_ATTR1 + "_";
        Hashtable<String, List<Metadata>> typeMap = new Hashtable<String,
	    List<Metadata>>();
        List<String> types = new ArrayList<String>();
        for (Enumeration keys = args.keys(); keys.hasMoreElements(); ) {
            String arg = (String) keys.nextElement();
            if ( !arg.startsWith(metadataPrefix)) {
                continue;
            }
            if ( !request.defined(arg)) {
                continue;
            }
            String type = arg.substring(ARG_METADATA_ATTR1.length() + 1);
            List[] urlArgs = new List[] {
		request.get(ARG_METADATA_ATTR1 + "_" + type,
			    new ArrayList<String>()),
		request.get(ARG_METADATA_ATTR2 + "_" + type,
			    new ArrayList<String>()),
		request.get(ARG_METADATA_ATTR3 + "_" + type,
			    new ArrayList<String>()),
		request.get(ARG_METADATA_ATTR4 + "_" + type,
			    new ArrayList<String>()) };

            int index = 0;
            while (true) {
                boolean  ok         = false;
                String[] valueArray = { "", "", "", "" };
                for (int valueIdx = 0; valueIdx < urlArgs.length;
		     valueIdx++) {
                    if (index < urlArgs[valueIdx].size()) {
                        ok = true;
                        valueArray[valueIdx] =
                            (String) urlArgs[valueIdx].get(index);
                    }
                }
                if ( !ok) {
                    break;
                }
                index++;

                Metadata metadata = new Metadata(getMetadataManager().findType(type),
						 valueArray[0],
						 valueArray[1], valueArray[2],
						 valueArray[3], "");

                metadata.setInherited(request.get(ARG_METADATA_INHERITED
						  + "." + type, false));
                List<Metadata> values = typeMap.get(type);
                if (values == null) {
                    typeMap.put(type, values = new ArrayList<Metadata>());
                    types.add(type);
                }
                values.add(metadata);
            }
        }

        List<Clause> metadataAnds = new ArrayList<Clause>();
        for (int typeIdx = 0; typeIdx < types.size(); typeIdx++) {
            String         type     = types.get(typeIdx);
            List<Metadata> values   = typeMap.get(type);
            List<Clause>   attrOrs  = new ArrayList<Clause>();
            String         subTable = Tables.METADATA.NAME + "_" + typeIdx;
            for (Metadata metadata : values) {
                String       tmp      = "";
                List<Clause> attrAnds = new ArrayList<Clause>();
                for (int attrIdx = 1; attrIdx <= 4; attrIdx++) {
                    String attr = metadata.getAttr(attrIdx);
                    if (attr.trim().length() > 0) {
                        attrAnds.add(Clause.eq(subTable + ".attr" + attrIdx,
					       attr));
                        tmp = tmp + ((tmp.length() == 0)
                                     ? ""
                                     : " &amp; ") + attr;
                    }
                }

                Clause attrClause = Clause.and(attrAnds);
                attrOrs.add(attrClause);
                MetadataHandler handler =
                    getRepository().getMetadataManager().findMetadataHandler(
									     type);
                MetadataType metadataType = handler.findType(type);
                if (metadataType != null) {
                    addCriteria(request, searchCriteria,
                                metadataType.getLabel() + "=", tmp);
                }
            }

            List<Clause> subClauses = new ArrayList<Clause>();
            subClauses.add(Clause.join(subTable + ".entry_id",
                                       Tables.ENTRIES.COL_ID));
            subClauses.add(Clause.eq(subTable + ".type", type));
            subClauses.add(Clause.or(attrOrs));
            metadataAnds.add(Clause.and(subClauses));
        }

        if (metadataAnds.size() > 0) {
            if (isOrSearch(request)) {
                where.add(Clause.or(metadataAnds));
                //                System.err.println ("metadata:" +Clause.or(metadataAnds));
            } else {
                where.add(Clause.and(metadataAnds));
                //                System.err.println ("metadata:" +Clause.and(metadataAnds));
            }
        }

        String[] textArgs = { ARG_NAME, ARG_DESCRIPTION };
        String[] columns = { Tables.ENTRIES.COL_NAME,
                             Tables.ENTRIES.COL_DESCRIPTION };

        for (int textIdx = 0; textIdx < textArgs.length; textIdx++) {
            String value = request.getString(textArgs[textIdx],
                                             (String) null);
            if ( !Utils.stringDefined(value)) {
                continue;
            }
            if ( !request.get(ARG_EXACT, false)) {
                value = "%" + value + "%";
                where.add(
			  getDatabaseManager().makeLikeTextClause(
								  columns[textIdx], value, false));
            } else {
                where.add(Clause.eq(columns[textIdx], value, false));
            }

        }

        String textToSearch = (String) request.getString(ARG_TEXT, "").trim();
        //A hook to allow the database manager do its own text search based on the dbms type
        if (textToSearch.length() > 0) {
            addTextDbSearch(request, textToSearch, searchCriteria, where);
        }

        return where;
    }

    private Clause getSpatialDefinedClause(String column) {
        return Clause.neq(column, Double.valueOf(Entry.NONGEO));
    }

    public void getChildrenEntries(Request request, Entry group,
                                   List<Entry> children, SelectInfo select)
	throws Exception {
        List<String> ids = getEntryManager().getChildIds(request, group, select);
        List<Entry> myEntries   = new ArrayList<Entry>();
        for (String id : ids) {
            Entry entry = getEntryManager().getEntry(request, id);
            if (entry == null) {
                continue;
            }
	    myEntries.add(entry);
        }
        children.addAll(postProcessEntries(request, myEntries));
    }

    public List<Entry> postProcessEntries(Request request,
                                          List<Entry> entries)
	throws Exception {
        return entries;
    }

    public void addTextDbSearch(Request request, String textToSearch,
                                Appendable searchCriteria, List<Clause> where)
	throws Exception {
        boolean doName = true;
        boolean doDesc = true;
        boolean doFile = true;
        if (textToSearch.startsWith("name:")) {
            doDesc       = false;
            doFile       = false;
            textToSearch = textToSearch.substring("name:".length());
        } else if (textToSearch.startsWith("description:")) {
            doName       = false;
            doFile       = false;
            textToSearch = textToSearch.substring("description:".length());
        } else if (textToSearch.startsWith("file:")) {
            doName       = false;
            doDesc       = false;
            textToSearch = textToSearch.substring("file:".length());
        }

        addTextDbSearch(request, textToSearch, searchCriteria, where, doName,
                        doDesc, doFile);
    }

    public void addTextDbSearch(Request request, String textToSearch,
                                Appendable searchCriteria,
                                List<Clause> where, boolean doName,
                                boolean doDesc, boolean doFile)
	throws Exception {

        DatabaseManager dbm = getDatabaseManager();
        textToSearch = textToSearch.replaceAll("%20", " ");
        List<Clause> textOrs = new ArrayList<Clause>();
        for (String textTok :
		 (List<String>) Utils.split(textToSearch, ",", true, true)) {
            boolean doLike   = false;
            boolean doRegexp = false;
            if (request.get(ARG_ISREGEXP, false)) {
                doRegexp = true;
                addCriteria(request, searchCriteria, "Text regexp:",
                            textToSearch);
            } else if ( !request.get(ARG_EXACT, false)) {
                addCriteria(request, searchCriteria, "Text like", textTok);
                List tmp = Utils.split(textTok, ",", true, true);
                //                textTok = "%" + StringUtil.join("%,%", tmp) + "%";
                doLike = true;
            } else {
                addCriteria(request, searchCriteria, "Text =", textToSearch);
            }
            //            System.err.println (doLike +" toks:" + nameToks);
            List<Clause> ands      = new ArrayList<Clause>();
            boolean searchMetadata = request.get(ARG_SEARCHMETADATA, false);
            searchMetadata = false;
            String[]     attrCols = { Tables.METADATA.COL_ATTR1  /*,
								   Tables.METADATA.COL_ATTR2,
								   Tables.METADATA.COL_ATTR3,
								   Tables.METADATA.COL_ATTR4*/
            };
            List<String> nameToks = Utils.splitWithQuotes(textTok);
            for (String nameTok : nameToks) {
                boolean nameDoLike = doLike;
                boolean doNot      = nameTok.startsWith("!");
                if (doNot) {
                    nameTok = nameTok.substring(1);
                }

                boolean doEquals = nameTok.startsWith("=");
                if (doEquals) {
                    nameTok    = nameTok.substring(1);
                    nameDoLike = false;
                }

                if (nameDoLike) {
                    nameTok = "%" + nameTok + "%";
                }
                List<Clause> ors     = new ArrayList<Clause>();
                List<Column> columns = getColumns();
                if (columns != null) {
                    for (Column column : columns) {
                        if (column.getCanSearch()
			    && column.getCanSearchText()
			    && column.isString()) {
                            ors.add(
				    dbm.makeLikeTextClause(
							   column.getFullName(), nameTok, doNot));
                        }
                    }
                }

                if (searchMetadata) {
                    List<Clause> metadataOrs = new ArrayList<Clause>();
                    for (String attrCol : attrCols) {
                        if (doRegexp) {
                            metadataOrs.add(
					    getDatabaseManager().makeRegexpClause(
										  attrCol, nameTok, doNot));
                        } else if (nameDoLike) {
                            metadataOrs.add(dbm.makeLikeTextClause(attrCol,
								   nameTok, doNot));
                        } else {
                            metadataOrs.add(Clause.eq(attrCol, nameTok,
						      doNot));
                        }
                    }
                    ors.add(
			    Clause.and(
				       Clause.or(metadataOrs),
				       Clause.join(
						   Tables.METADATA.COL_ENTRY_ID,
						   Tables.ENTRIES.COL_ID)));
                }
                if (doRegexp) {
                    if (doName) {
                        ors.add(
				getDatabaseManager().makeRegexpClause(
								      Tables.ENTRIES.COL_NAME, nameTok, doNot));
                    }
                    if (doDesc) {
                        ors.add(
				getDatabaseManager().makeRegexpClause(
								      Tables.ENTRIES.COL_DESCRIPTION, nameTok,
								      doNot));
                    }
                    if (doFile) {
                        ors.add(
				getDatabaseManager().makeRegexpClause(
								      Tables.ENTRIES.COL_RESOURCE, nameTok, doNot));
                    }
                } else if (nameDoLike) {
                    if (doName) {
                        ors.add(
				dbm.makeLikeTextClause(
						       Tables.ENTRIES.COL_NAME, nameTok, doNot));
                    }
                    if (doDesc) {
                        ors.add(
				dbm.makeLikeTextClause(
						       Tables.ENTRIES.COL_DESCRIPTION, nameTok,
						       doNot));
                    }
                    if (doFile) {
                        ors.add(
				dbm.makeLikeTextClause(
						       Tables.ENTRIES.COL_RESOURCE, nameTok, doNot));
                    }
                } else {
                    if (doName) {
                        ors.add(Clause.eq(Tables.ENTRIES.COL_NAME, nameTok,
                                          doNot));
                    }
                    if (doDesc) {
                        ors.add(Clause.eq(Tables.ENTRIES.COL_DESCRIPTION,
                                          nameTok, doNot));
                    }
                    if (doFile) {
                        ors.add(Clause.eq(Tables.ENTRIES.COL_RESOURCE,
                                          nameTok, doNot));
                    }

                }
                if (doNot) {
                    ands.add(Clause.and(ors));
                } else {
                    ands.add(Clause.or(ors));
                }
            }
            //            System.err.println("clauses:" + ands);
            if (ands.size() > 1) {
                //                System.err.println ("ands:" + ands);
                textOrs.add(Clause.and(ands));
            } else if (ands.size() == 1) {
                //                System.err.println ("ors:" + ands.get(0));
                textOrs.add(ands.get(0));
            }
        }
        //        System.err.println ("ors:" + textOrs.get(0));
        if (textOrs.size() > 1) {
            where.add(Clause.or(textOrs));
        } else if (textOrs.size() == 1) {
            where.add(textOrs.get(0));
        }

    }

    public boolean isOrSearch(Request request) {
        return request.getString("search.or", "false").equals("true");
    }

    public void setStatement(Entry entry, PreparedStatement stmt,
                             boolean isNew)
	throws Exception {}

    public void getInsertSql(boolean isNew,
                             List<TypeInsertInfo> typeInserts) {
        if (parent != null) {
            parent.getInsertSql(isNew, typeInserts);
        }
    }

    public void deleteEntry(Request request, Statement statement, Entry entry)
	throws Exception {
        if (parent != null) {
            parent.deleteEntry(request, statement, entry);
        }
    }

    public void deleteEntry(Request request, Statement statement, String id,
                            Entry parentEntry, Object[] values)
	throws Exception {
        if (parent != null) {
            parent.deleteEntry(request, statement, id, parentEntry, values);
        }
    }

    protected List getTablesForQuery(Request request) {
        return getTablesForQuery(request, new ArrayList());
    }

    protected List getTablesForQuery(Request request, List initTables) {
        if (parent != null) {
            parent.getTablesForQuery(request, initTables);
        }
        if ( !initTables.contains(Tables.ENTRIES.NAME)) {
            initTables.add(Tables.ENTRIES.NAME);
        }

        return initTables;
    }

    public Object convert(String columnName, String value) {
        if (parent != null) {
            return parent.convert(columnName, value);
        }

        return value;
    }

    public Object[] makeEntryValues(Hashtable map) {
        if (parent != null) {
            return parent.makeEntryValues(map);
        }

        return null;
    }

    public String[] getValueNames() {
        if (parent != null) {
            return parent.getValueNames();
        }

        return null;
    }

    protected boolean addOr(String column, String value, List list,
                            boolean quoteThem) {
        if ((value != null) && (value.trim().length() > 0)
	    && !value.toLowerCase().equals("all")) {
            list.add("(" + SqlUtil.makeOrSplit(column, value, quoteThem)
                     + ")");

            return true;
        }

        return false;
    }

    public String getFileTypeDescription(Request request, Entry entry) {
        try {
            String desc = msg(entry.getTypeHandler().getDescription());
            if ( !Utils.stringDefined(desc)) {
                desc = entry.getTypeHandler().getType();
            }

            if ( !entry.getTypeHandler().getType().equals(
							  TypeHandler.TYPE_FILE)) {
                String searchUrl =
                    HU.href(
			    getSearchManager().URL_SEARCH_TYPE + "/"
			    + entry.getTypeHandler().getType(), HU.getIconImage(ICON_SEARCH) + HU.space(1) +
			    desc,
                            HU.cssClass("entry-type-search")
                            + HU.attrs(HU.ATTR_TITLE, "Search for entries of this type"));

                return searchUrl;
            }
            String path = "";
            try {
                path = getPathForEntry(request, entry,false);
            } catch (Exception exc) {
                return "Error:" + exc;
            }
            String label = getLabelFromPath(path);
            String url   = getUrlFromPath(path);

            if ((label == null) && (url != null)) {
                desc = HU.href(url, desc,
			       HU.attr("target", "_help"));
            } else if (label != null) {
                if (url != null) {
                    //                desc = desc  +" (" + HU.href(url, label)+")";
                    desc = HU.href(url, msg(label),
				   HU.attr("target", "_help"));
                } else {
                    //                desc = desc  +" (" +label+")";
                    desc = msg(label);
                }
            }

            return desc;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    public String getTemplateContent(Request request, Entry entry,
                                     String property, String dflt)
	throws Exception {
        String template = getTypeProperty(property, (String) null);
        if (template != null) {
            return processDisplayTemplate(request, entry, template);
        }

        return dflt;
    }

    public int matchValue(String arg, Object value, Entry entry) {
        return MATCH_UNKNOWN;
    }

    public TwoFacedObject getCategory(Request request,Entry entry,String categoryType) {
        return new TwoFacedObject(description, this.type);
    }

    public String getMapInfoBubble(Request request, Entry entry)
	throws Exception {
        return getBubbleTemplate(request, entry);
    }

    public String getSimpleDisplay(Request request, Hashtable props,
                                   Entry entry)
	throws Exception {
        if (getParent() != null) {
	    return getParent().getSimpleDisplay(request, props, entry);
	}
        return null;
    }

    public String getWikiText(Request request, Entry entry, String tag) {
	String text = wikiText.get(tag);
	if(text==null && getParent() != null) {
	    text = getParent().getWikiText(request, entry,tag);
	}
	return text;
    }

    public void putWikiText(String tag, String text) {
	wikiText.put(tag,text);
    }

    public String toString() {
        return type + " " + description;
    }

    public void setDefaultCategory(String value) {
        defaultCategory = value;
    }

    public String getDefaultCategory() {
        return defaultCategory;
    }

    public boolean hasDefaultCategory() {
        return (defaultCategory != null) && (defaultCategory.length() > 0);
    }

    private Hashtable<String, HashSet> columnEnumValues =
        new Hashtable<String, HashSet>();

    public boolean getEditable(Column column) {
        if (getParent() != null) {
            return getParent().getEditable(column);
        }

        return column.getEditable();
    }

    public boolean getCanDisplay(Column column) {
        if (getParent() != null) {
            return getParent().getCanDisplay(column);
        }

        return column.getCanDisplay();
    }

    protected String getEnumValueKey(Column column, Entry entry) {
        return column.getName();
    }

    public String getEntryListName(Request request, Entry entry) {
        return getEntryName(entry);
    }

    protected void addEnumValue(Column column, Entry entry, String theValue)
	throws Exception {
        if ((theValue == null) || (theValue.length() == 0)) {
            return;
        }
        HashSet set = getEnumValuesInner(null, column, entry,false);
	if(set!=null) 
	    set.add(theValue);
	else {
	    //????
	}
    }
    public List<HtmlUtils.Selector> getEnumValues(Request request, Column column, Entry entry)
	throws Exception {
	boolean forSearch = request.get("forsearch",false);
        HashSet   set  = getEnumValuesInner(request, column,  entry,forSearch|| true);
	//If we get back null then the column should have values
	List<HtmlUtils.Selector> columnValues = column.getValues();
        List<HtmlUtils.Selector> tfos = new ArrayList<HtmlUtils.Selector>();
	if(!forSearch && columnValues!=null) {
	    for(HtmlUtils.Selector sel: columnValues) {
		if(!tfos.contains(sel)) tfos.add(sel);
	    }
	}

        List                 tmp  = new ArrayList();
	if(set!=null)
	    tmp.addAll(set);

        for (String s : (List<String>) Misc.sort(tmp)) {
            String label = s;
            if (s.length() == 0) {
                label = "&lt;blank&gt;";
            } else {
		label = column.getEnumLabel(label);
	    }

	    HtmlUtils.Selector sel = new HtmlUtils.Selector(label, s);
	    if(!tfos.contains(sel)) {
		tfos.add(sel);
	    }
        }

	/*
	if(column.toString().toLowerCase().indexOf("offense")>=0) {
	    for(HtmlUtils.Selector sel: tfos)
		System.out.println(sel.getLabel() +" ");
	    System.out.println("");
	}
	*/

        return tfos;
    }

    private HashSet getEnumValuesInner(Request request, Column column,
                                       Entry entry, boolean getFromDatabase)
	throws Exception {
        Clause clause = getEnumValuesClause(column, entry);
	boolean hasEnumValuesClause = clause!=null;
	boolean didClause= false;
        if (request != null) {
            List<Clause> ands = new ArrayList<Clause>();
            for (Column otherCol : getColumns()) {
                if ( !otherCol.getCanSearch() || !otherCol.isEnumeration()) {
                    continue;
                }
                if (otherCol.equals(column)) {
                    continue;
                }
                String urlId = otherCol.getFullName();
                if (request.defined(urlId)) {
                    ands.add(Clause.eq(otherCol.getName(),
                                       request.getString(urlId, "")));
                }
            }
            if (ands.size() > 0) {
		didClause = true;
                if (clause == null) {
                    clause = Clause.and(ands);
                } else {
                    clause = Clause.and(clause, Clause.and(ands));
                }
                //                System.err.println("col:" + column + " Clause:" + clause);
            }
        }

	//If we have no other clauses and the column has values defined then pass back null
	//this tells the caller to get the values from the column
	if(!getFromDatabase && !didClause) {
	    List<HtmlUtils.Selector> tmp = column.getValues();
	    if(tmp!=null && tmp.size()>0) {
		return null;
	    }
	}
	boolean enumerationsSpecific = getProperty(null,"enumerations.specific","true",true).equals("true");
	Clause entryTypeClause = enumerationsSpecific?Clause.eq(GenericTypeHandler.COL_ENTRY_TYPE,this.getType()):null;
	if(!hasEnumValuesClause) {
	    clause = entryTypeClause==null?clause:(clause==null?entryTypeClause:Clause.and(clause,entryTypeClause));
	}
        //Use the clause string as part of the key
        String  key = getEnumValueKey(column, entry) + ((clause == null)
							? ""
							: "_" + clause);
        HashSet set = columnEnumValues.get(key);
	boolean debug = false;
	//	debug = column.toString().toLowerCase().indexOf("offense")>=0;

        if (set != null) {
            return set;
        }

        long t1 = System.currentTimeMillis();
        Statement stmt = getRepository().getDatabaseManager().select(
								     SqlUtil.distinct(column.getName()),
								     column.getTableName(), clause);
	if(debug) {
	    System.err.println(getType() +" column:" + column +" table:" + column.getTableName() +
			       "\n\tclause:" + clause +" set:" + set);
	}

        long t2 = System.currentTimeMillis();
        String[] values =
            SqlUtil.readString(
			       getRepository().getDatabaseManager().getIterator(stmt), 1);
        long t3 = System.currentTimeMillis();
	if(debug)
	    System.err.println("values:" + Utils.arrayToList(values));
	//	Utils.printTimes("enum values:"+ column +" times:",t1,t2,t3);
        set = new HashSet();
	if(column.isMultiEnumeration())   {
	    for(String value: values) {
		set.addAll(Utils.split(value,column.getDelimiter()));
	    }
	} else {
	    set.addAll(Misc.toList(values));
	}
        columnEnumValues.put(key, set);

        return set;
    }

    public Clause getEnumValuesClause(Column column, Entry entry)
	throws Exception {
        return null;
    }

    public String getFieldHtml(Request request, Entry entry, Hashtable props,
                               String name, boolean raw)
	throws Exception {
        if (name.equals("startdate")) {
            Date   dttm = new Date(entry.getStartDate());
            String fmt  = props!=null?(String) props.get("format"):null;
	    String s;
            if (fmt == null) {
                s= getDateHandler().formatDate(request, entry, entry.getStartDate());
            } else {
		s=Utils.makeDateFormat(
				       fmt,
				       (String) Utils.getNonNull(
								 props.get("timezone"), "UTC")).format(dttm);
	    }
	    if(startDateMacros!=null) {
		s = applyMacros(request,entry, startDateMacros,null, dttm,s);
	    }
	    return s;
        }
        if (name.equals("enddate")) {
            Date   dttm = new Date(entry.getEndDate());
            String fmt  = props!=null?(String) props.get("format"):null;
	    String s;
            if (fmt == null) {
                s= getDateHandler().formatDate(request, entry, entry.getStartDate());
            } else {
		s=Utils.makeDateFormat(fmt,
				       (String) Utils.getNonNull(props.get("timezone"), "UTC")).format(dttm);
	    }
	    if(endDateMacros!=null) {
		s = applyMacros(request,entry, endDateMacros,null,dttm,s);
	    }
	    return s;
        }

        if (name.equals("altitude")) {
            return "" + entry.getAltitude();
        }
        if (name.equals("latitude")) {
            return "" + entry.getLatitude(request);
        }
        if (name.equals("longitude")) {
            return "" + entry.getLongitude(request);
        }

        //TODO: support name, desc, etc.
        return null;
    }

    public void setCategory(String value) {
        this.category = value;
    }

    public void setSuperCategory(String value) {
        this.superCategory = value;
    }

    public String getCategory() {
        if (Misc.equals(this.category, CATEGORY_DEFAULT)
	    && (parent != null)) {
            return parent.getCategory();
        }

        return this.category;
    }

    public String getSuperCategory() {
        if ((this.superCategory.length() == 0) && (parent != null)) {
            return parent.getSuperCategory();
        }

	if (superCategory.equals("Basic") || superCategory.equals("")) {
	    return  "General";
	}

        return this.superCategory;
    }

    public void setIncludeInSearch(boolean value) {
        includeInSearch = value;
    }

    public boolean getIncludeInSearch() {
        return includeInSearch;
    }

    public void setCanCreate(boolean v) {
        this.canCreate = v;
    }

    public void setForUser(boolean v) {
        this.forUser = v;
    }

    public boolean getForUser() {
        if ( !forUser) {
            return false;
        } else {
            //Don't inherit the for user
            return true;
        }

        /*
          if (getParent() != null) {
          return getParent().getForUser();
          }
          return true;
        */
    }

    public boolean entryHasDefaultName(Entry entry) {
        return Misc.equals(getStorageManager().getFileTail(entry),
                           entry.getName());
    }

    public TimeZone getTimeZone(Request request, Entry entry, int index)
	throws Exception {
        TimeZone timeZone = null;
        String   timezone = null;
        if (entry != null) {
            if (index >= 0) {
                timezone = entry.getStringValue(request,index, "");
            }
            if ( !Utils.stringDefined(timezone)) {
                timezone = getEntryUtil().getTimezone(request, entry);
            }
        }
        if (Utils.stringDefined(timezone)) {
            timeZone = TimeZone.getTimeZone(timezone);
        } else {
            timeZone = RepositoryUtil.TIMEZONE_DEFAULT;
        }

        return timeZone;
    }

    public void setSpecialSearch(SpecialSearch value) {
        specialSearch = value;
    }

    public SpecialSearch getSpecialSearch() {
        if (specialSearch == null) {
            specialSearch = new SpecialSearch(this);
        }

        return specialSearch;
    }

    public String getEntryName(Entry entry) {
        return entry.getName();
    }

    private List<DateArgument> getDateArgs() {
        if (dateArgs == null) {
            List<DateArgument> tmp = new ArrayList<DateArgument>();
            for (DateArgument arg : DateArgument.SEARCH_ARGS) {
                tmp.add(arg);
            }
            List<String> from = Utils.split(
					    getRepository().getProperty(
									"ramadda.arg.date.from", ""), ",",
                                            true, true);
            List<String> to = Utils.split(
					  getRepository().getProperty(
								      "ramadda.arg.date.to", ""), ",", true,
                                          true);
            List<String> mode = Utils.split(
					    getRepository().getProperty(
									"ramadda.arg.date.mode", ""), ",",
                                            true, true);
            List<String> relative = Utils.split(
						getRepository().getProperty(
									    "ramadda.arg.date.relative",
									    ""), ",", true, true);
            for (int i = 0; i < from.size(); i++) {
                tmp.add(new DateArgument(DateArgument.TYPE_DATA, from.get(i),
                                         to.get(i), mode.get(i),
                                         relative.get(i)));
            }
            dateArgs = tmp;
        }

        return dateArgs;
    }

    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
	throws Exception {
        return null;
    }

    public static void addPropertyTags(Hashtable properties,
                                       Appendable inner) {
        for (Enumeration keys = properties.keys(); keys.hasMoreElements(); ) {
            String arg   = (String) keys.nextElement();
            String value = (String) properties.get(arg);
            Utils.append(inner,
                         XmlUtil.tag(TypeHandler.TAG_PROPERTY,
                                     XmlUtil.attrs(ATTR_NAME, arg,
						   TypeHandler.ATTR_VALUE, value)));
        }
    }

    public void initMapAttrs(Entry entry, MapInfo mapInfo,
                             StringBuilder sb) {}

    public String getLabel() {
        if ( !Utils.stringDefined(description)) {
            return getType();
        }

        return description;
    }

    public int getPriority() {
        return priority;
    }

    public void  setPriority(int p) {
        priority = p;
    }    

    public String getDescription() {
        return description;
    }

    public void setDescription(String d) {
        description = d;
    }

    public String getHelp() {
        return help;
    }

    public String getMimeType() {
	return mimeType;
    }

    public void setHelp(String d) {
        help = d;
    }

    public static void main(String[] args) throws Exception {
        String pattern = ".*\\.ggp$";
        System.err.println(args[0].toLowerCase().matches(pattern));
    }

    public interface Entries extends Supplier<List<Entry>> {
	public List<Entry> get();
    }

    public static class Action {
	private String id;
	private String label;
	private String icon;
	private boolean forUser;
	private boolean forAdmin;
	private boolean canEdit;
	private String category = "view";
	private String wikiText;
	Action(String id, String label, String icon,boolean forUser,boolean forAdmin, boolean canEdit,String category) {
	    this.id = id;
	    this.label = label;
	    this.icon = icon;
	    this.forUser = forUser;
	    this.forAdmin = forAdmin;
	    this.canEdit = canEdit;
	    this.category=category;
	}

	Action(String id, String label, String icon,String wikiText) {
	    this.id=id;
	    this.label = label;
	    this.icon = icon;
	    this.wikiText = wikiText;
	}

	public String getId() {
	    return id;
	}

    }

    public static class MetadataPattern {
	String spattern;
	Pattern pattern;
	String column;
	String metadataType;
	MetadataPattern(String spattern,String column, String metadataType) {
	    this.spattern = spattern;
	    pattern =  Pattern.compile(this.spattern);
	    this.column = column;
	    this.metadataType = metadataType;
	}
    }

}
