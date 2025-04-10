/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.metadata;

import org.json.*;

import org.ramadda.repository.*;
import org.ramadda.repository.output.OutputType;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.database.*;
import org.ramadda.repository.util.FileWriter;
import org.ramadda.util.FormInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.JQuery;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.NamedBuffer;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import org.ramadda.util.sql.Clause;

import org.ramadda.util.sql.SqlUtil;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.lang.reflect.*;

import java.util.function.Consumer;

import java.net.*;

import java.sql.ResultSet;
import java.sql.Statement;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import java.util.regex.*;
import java.util.zip.*;

@SuppressWarnings("unchecked")
public class MetadataManager extends RepositoryManager {

    public static final boolean CHECK_UNIQUE_FALSE = false;
    public static final boolean CHECK_UNIQUE_TRUE = false;
    public static final boolean INHERITED_FALSE = false;
    public static final boolean INHERITED_TRUE = true;

    private static final String SUFFIX_SELECT = ".select.";

    private Object MUTEX_METADATA = new Object();

    public RequestUrl URL_METADATA_FORM = new RequestUrl(getRepository(),
                                              "/metadata/form",
                                              "Edit Properties");

    public RequestUrl URL_METADATA_LIST = new RequestUrl(getRepository(),
                                              "/metadata/list",
                                              "Property Listing");

    public RequestUrl URL_METADATA_VIEW = new RequestUrl(getRepository(),
                                              "/metadata/view",
                                              "Property View");

    public RequestUrl URL_METADATA_ADDFORM = new RequestUrl(getRepository(),
                                                 "/metadata/addform",
                                                 "Add Property");

    public RequestUrl URL_METADATA_ADD = new RequestUrl(getRepository(),
                                             "/metadata/add");

    public RequestUrl URL_METADATA_CHANGE = new RequestUrl(getRepository(),
                                                "/metadata/change");

    private Hashtable<String, Hashtable<String, String>> metadataTypeToTemplate =
        new Hashtable<String, Hashtable<String, String>>();

    protected Hashtable distinctMap = new Hashtable();

    private List<MetadataHandler> metadataHandlers =
        new ArrayList<MetadataHandler>();

    private Hashtable<Class, MetadataHandler> metadataHandlerMap =
        new Hashtable<Class, MetadataHandler>();

    protected Hashtable<String, MetadataType> typeMap = new Hashtable<String,
                                                            MetadataType>();

    protected Hashtable<String, MetadataHandler> handlerMap =
        new Hashtable<String, MetadataHandler>();

    private List<MetadataType> metadataTypes = new ArrayList<MetadataType>();

    private List<String> tableNames = new ArrayList<String>();

    public MetadataManager(Repository repository) {
        super(repository);
    }

    MetadataHandler dfltMetadataHandler;

    private HashSet notTypes;
    private List<String> notTypesList;

    public synchronized boolean metadataTypeOk(MetadataType type) {
	if(notTypes==null) {
	    notTypes=new HashSet();
	    notTypesList = new ArrayList<String>();
	    for(String stype: Utils.split(getRepository().getProperty("ramadda.metadata.nottypes",""),",",true,true)) {
		notTypes.add(stype);
		if (StringUtil.containsRegExp(stype)) {
		    notTypesList.add(stype);
		}
	    }
	}
	if(notTypes.contains(type.getId()) || notTypes.contains("category:" +type.getCategory())) {
	    return false;
	}
	for(String pattern: notTypesList) {
	    if(pattern.startsWith("category:")) {
		if(type.getCategory().matches(pattern.substring("category:".length()))) return false;
	    } else if(type.getId().matches(pattern)) {
		return false;

	    }

	}
	return true;

    } 

    private void initializeState() {
	metadataTypeToTemplate =  new Hashtable<String, Hashtable<String, String>>();
	distinctMap = new Hashtable();
	metadataHandlers =    new ArrayList<MetadataHandler>();
	metadataHandlerMap =    new Hashtable<Class, MetadataHandler>();
	typeMap = new Hashtable<String, MetadataType>();
	handlerMap =  new Hashtable<String, MetadataHandler>();
	metadataTypes = new ArrayList<MetadataType>();
	tableNames = new ArrayList<String>();
    }

    public MetadataType findType(String stringType) {
	return findType(stringType,true);
    }

    public MetadataType findType(String stringType, boolean makeDefault) {
	MetadataType type =  typeMap.get(stringType);
	if(type==null) {
	    if(!makeDefault) return null;
	    return new MetadataType(stringType,getDefaultMetadataHandler());
	}
	return type;
    }

    public MetadataType getType(Metadata metadata) {
	MetadataType type = metadata.getMetadataType();
	if(type==null) {
	    type= typeMap.get(metadata.getType());
	    metadata.setMetadataType(type);
	}
	return type;
    }

    public MetadataHandler findHandler(String stringType) {
        MetadataType type = findType(stringType);
        if (type == null) {
            return null;
        }

        return type.getHandler();
    }

    public String[] getMetadataAddLink(Request request, Entry entry, String metadataType) {
	MetadataType type =findType(metadataType);
	if(type==null) return null;
	return  new String[]{
	    request.entryUrl(URL_METADATA_ADDFORM,
			     entry, ARG_METADATA_TYPE,
			     metadataType), msg("Add") + " " + type.getName()};
    }

    public void addMetadataType(MetadataType type) {
        metadataTypes.add(type);
        typeMap.put(type.getId(), type);
        handlerMap.put(type.getId(), type.getHandler());
    }

    private List<License> licenses;

    private Hashtable<String, License> licenseMap;

    public void loadLicenses() throws Exception {
        licenses   = new ArrayList<License>();
        licenseMap = new Hashtable<String, License>();
        List<String> files = new ArrayList<String>();
        files.add(
            "/org/ramadda/repository/resources/metadata/spdxlicenses.json");
        files.add(
            "/org/ramadda/repository/resources/metadata/rightsstatementslicenses.json");	
        files.addAll(getPluginManager().getLicenseFiles());
        for (String file : files) {
            JSONObject obj       = new JSONObject(IOUtil.readContents(file));
            JSONArray  jlicenses = obj.optJSONArray("licenses");
	    if(jlicenses==null) continue;
            int        priority  = obj.optInt("priority", 100);
            for (int i = 0; i < jlicenses.length(); i++) {
                JSONObject jlicense = jlicenses.getJSONObject(i);
		if(jlicense.optBoolean("skip",false)) continue;
                License license = new License(getRepository(),
					      obj.optString("name", "Usage Descriptors"),
					      jlicense.optString("url",
								 obj.optString("url",
									       null)), jlicense, priority);
                licenses.add(license);
                licenseMap.put(license.getId(), license);
                licenseMap.put(license.getId().toLowerCase(), license);		
            }
        }
        Collections.sort(licenses);
    }

    public synchronized License getLicense(String licenseId) {
        License license=licenseMap.get(licenseId);
	if(license==null) {
	    for(License l:getLocalLicenses()) {
		if(l.getId().equals(licenseId)) return l;
	    }
	}
	return license;
    }

    public synchronized List<License> getLicenses() {
	List<License> extra = getLocalLicenses();
	if(extra.size()>0) {
	    extra.addAll(licenses);
	    return extra;
	}
        return licenses;
    }

    private synchronized List<License> getLocalLicenses() {
	try {
	    Request request =getRepository().getAdminRequest();
	    List<License> licenses= new ArrayList<License>();
	    for(Entry entry:getEntryManager().getEntriesWithType(request,
								 "type_usage_descriptor")) {
		String icon =  "";
		if(entry.isFile())  {
		    icon = getEntryManager().getEntryResourceUrl(request, entry) ;
		}
		License license = new License(entry.getStringValue(request,"usage_id",""),
					      entry.getName(),
					      (String)entry.getValue(request,"external_url"),
					      icon,
					      entry.getDescription());
		license.setFrom("Local RAMADDA Usage Descriptors");
		licenses.add(license);
	    }
	    return licenses;
	} catch(Exception exc) {
	    throw new RuntimeException(exc);
	}
    }    

    public List<String> getTypeResource(String id) {
        if (id.equals("licenses")) {
            List<String> resources = new ArrayList<String>();
            for (License license : licenses) {
                resources.add(license.getId() + ":" + license.getName());
            }

            return resources;
        }

        throw new IllegalArgumentException("Unknown resource:" + id);
    }

    private String wrapLicenseText(String text) {
        return HU.div(text, HU.cssClass("ramadda-license-text"));
    }

    public String getLicenseHtml(String id, String label,boolean includeId) throws Exception {
        License license = licenseMap.get(id);
        if (license == null) {
            return "NA:" + id;
        }

        return getLicenseHtml(license, label,includeId);
    }

    public Result processUsageDescriptors(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        getPageHandler().sectionOpen(request, sb, "Available Usage Descriptors",
                                     false);
	HU.addPageSearch(sb,".ramadda-licenses-license",null,"Find Descriptor");

        String from = "";
        int    cnt  = 0;
        for (License license : getLicenses()) {
            if ( !license.getFrom().equals(from)) {
                if (cnt > 0) {
                    sb.append("</div>");
                }
                from = license.getFrom();
                sb.append(HU.h2(from));
                sb.append(HU.open("div", "class", "ramadda-licenses-box"));
            }
            HU.div(sb,getLicenseHtml(license, null,true),HU.cssClass("ramadda-licenses-license"));
            cnt++;
        }
        sb.append("</div>");
        getPageHandler().sectionClose(request, sb);
	HU.script(sb,"Utils.initCopyable('.ramadda-copyable');");

        return new Result("Usage Descriptors", sb);
    }

    public String getLicenseHtml(License license, String label,boolean includeId)
            throws Exception {
        if (label == null) {
            label = license.getName();
        }
        String contents = " " + label + " ";
        if (Utils.stringDefined(license.getUrl())) {
            contents = HU.href(license.getUrl(), contents, "target=_other");
        }
	if(includeId)
	    contents+=HU.space(2)
		+ "ID: " + HU.span(license.getId(),
						 HU.attrs("class","ramadda-copyable","copy-message","Usage descriptor ID copied to clipboard"));

        String icon = license.getIcon();
        String text = license.getText();
        if (icon != null) {
            String extra = HU.image(icon,
                                    HU.attrs("width", "80", "border", "0"));
            if (Utils.stringDefined(license.getUrl())) {
                extra = HU.href(license.getUrl(), extra, "target=_other");
            }
            if (Utils.stringDefined(text)) {
                extra = HU.span(extra, "style='vertical-align:top;'")
                        + wrapLicenseText(text);
            }
            extra    = HU.div(extra);
            contents += extra;
        } else if (Utils.stringDefined(text)) {
            contents += "<br>" + wrapLicenseText(text);
        }
        if (license.getUrl() != null) {
            contents = HU.href(license.getUrl(), contents, "target=_other");
        }
        return contents;
    }

    public void addTemplate(String metadataType, String templateType,
                            String templateContents) {
        Hashtable<String, String> templatesForType =
            metadataTypeToTemplate.get(metadataType);
        if (templatesForType == null) {
            templatesForType = new Hashtable<String, String>();
            metadataTypeToTemplate.put(metadataType, templatesForType);

        }
        templatesForType.put(templateType, templateContents);
    }

    public String getTemplate(String metadataType, String templateType) {
        Hashtable<String, String> templatesForType =
            metadataTypeToTemplate.get(metadataType);
        if (templatesForType == null) {
            return null;
        }

        return templatesForType.get(templateType);
    }

    private void addMetadataTag(Appendable sb, String tag, String v) throws Exception {
	sb.append("<meta name=\"" + tag+"\" content=\"" + XmlUtil.encodeString(v) +"\">\n");
	//	System.err.println("meta tag:" + tag + " " + v);
    }

    public void addHtmlMetadata(Request request, Entry entry, Appendable sb,
				boolean showJsonLd, boolean showTwitterCard) throws Exception {
	//This is always a new list
	List<Metadata> inherited = getInheritedMetadata(request,  entry);
        List<Metadata> metadataList = getMetadata(request,entry);
	inherited.addAll(metadataList);
        List<String> keywords = null;

	List<String[]> thumbs = new ArrayList<String[]>();
	getFullThumbnailUrls(request, entry, thumbs,false);
	if(thumbs.size()==0 && entry.isImage()) {
	    thumbs.add(new String[]{getEntryManager().getEntryResourceUrl(request, entry),null});
	}

        for (Metadata md : inherited) {
            String type = md.getType();
	    //	    System.err.println("mtd:" + type +" " + md.getAttr1());
	    if (type.equals("enum_gcmdkeyword")
		|| type.equals("content.keyword")
		|| type.equals("enum_tag")
		|| type.equals("thredds.keyword")) {
                if (keywords == null) {
                    keywords = new ArrayList<String>();
                }
                keywords.add(md.getAttr1());
            }
	}

	addMetadataTag(sb,"og:title",entry.getName());
	if(thumbs.size()>0) {
	    String image = request.getAbsoluteUrl(thumbs.get(0)[0]);
	    addMetadataTag(sb, "og:image",image);
	}

        String snippet = getWikiManager().getRawSnippet(request, entry,true);
	if(snippet!=null) snippet = Utils.stripTags(snippet).replace("\n"," ").replace("\"","&quot;").trim();
	if (keywords != null) {
	    addMetadataTag(sb,"keywords",Utils.join(keywords,","));
        }
	if(stringDefined(snippet)) {
	    addMetadataTag(sb,"description",snippet);
	}

	if(showJsonLd) addJsonLD(request, entry, sb,inherited,keywords,snippet);
	if(showTwitterCard) addTwitterCard(request, entry, sb,snippet,thumbs);	
    }

    private SimpleDateFormat jsonLdSdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    public void addJsonLD(Request request, Entry entry,Appendable sb,List<Metadata> metadataList,  List<String> keywords, String snippet) throws Exception {
        sb.append("\n<script type=\"application/ld+json\">\n");
        List<String>   top          = new ArrayList<String>();
        top.add("@context");
        top.add(JsonUtil.quote("https://schema.org/"));
        top.add("@type");
        top.add(JsonUtil.quote("Dataset"));
        top.add("name");
        top.add(JsonUtil.quote(JsonUtil.cleanString(entry.getName())));
        top.add("url");
        top.add(
            JsonUtil.quote(
                request.entryUrl(getRepository().URL_ENTRY_SHOW, entry)));

	//50-5000
	if(stringDefined(snippet)&& snippet.length()>50) {
	    top.add("description");
	    top.add(JsonUtil.quote(JsonUtil.cleanString(snippet)));
	}
        if (entry.hasDate()) {
            top.add("temporalCoverage");
	    synchronized(jsonLdSdf) {
		if (entry.getStartDate() == entry.getEndDate()) {
		    top.add(
			    JsonUtil.quote(
					   jsonLdSdf.format(new Date(entry.getStartDate()))));
		} else {
		    top.add(
			    JsonUtil.quote(
					   jsonLdSdf.format(new Date(entry.getStartDate())) + "/"
					   + jsonLdSdf.format(new Date(entry.getEndDate()))));
		}
	    }

        }
        if (entry.isGeoreferenced(request)) {
            List<String> geo = new ArrayList<String>();
            geo.add("@type");
            geo.add(JsonUtil.quote("Place"));
            geo.add("geo");
            if (entry.hasAreaDefined(request)) {
                String box = entry.getSouth(request) + " " + entry.getWest(request) + " "
                             + entry.getNorth(request) + " " + entry.getEast(request);
                geo.add(JsonUtil.map(Utils.makeListFromValues("@type",
                        JsonUtil.quote("GeoShape"), "box",
                        JsonUtil.quote(box))));
            } else {
                geo.add(JsonUtil.map(Utils.makeListFromValues("@type",
                        JsonUtil.quote("GeoCoordinates"), "latitude",
                        JsonUtil.quote("" + entry.getLatitude(request)),
                        "longitude",
                        JsonUtil.quote("" + entry.getLongitude(request)))));
            }
            top.add("spatialCoverage");
            top.add(JsonUtil.map(geo));
        }

        if (entry.isFile()) {
            top.add("distribution");
	    String mimeType = getRepository().getMimeTypeFromSuffix(
								    IO.getFileExtension(entry.getResource().getPath()));
            top.add(JsonUtil.mapAndQuote(Utils.makeListFromValues("@type","DataDownload",
							"encodingFormat",mimeType,
							"contentUrl",
                    getEntryManager().getEntryResourceUrl(request, entry,
                        EntryManager.ARG_INLINE_DFLT, true, false))));
        }

        List<String> ids      = null;
        for (Metadata md : metadataList) {
            String type = md.getType();
            if (type.equals("content.license")) {
                top.add("license");
                top.add(JsonUtil.quote(md.getAttr1()));
            } else if (type.equals("doi_identifier")) {
                if (ids == null) {
                    ids = new ArrayList<String>();
                }
                ids.add(md.getAttr2());
            } else if (type.equals("metadata_author")) {
                List<String> ctor = new ArrayList<String>();
		ctor.add(JsonUtil.quote("@type"));
		ctor.add(JsonUtil.quote(md.getAttr1()));
                top.add("author");
                top.add(JsonUtil.map(ctor));
            } else if (type.equals("thredds.creator")) {
                List<String> ctor = new ArrayList<String>();
                ctor.add("@type");
                ctor.add(JsonUtil.quote("Organization"));
                ctor.add("name");
                ctor.add(JsonUtil.quote(md.getAttr1()));
                ctor.add("url");
                ctor.add(JsonUtil.quote(md.getAttr4()));
                ctor.add("contactPoint");
                ctor.add(JsonUtil.mapAndQuote(Utils.makeListFromValues("@type",
                        "ContactPoint", "email", md.getAttr3())));
                top.add("creator");
                top.add(JsonUtil.map(ctor));
            }
	}
        if (ids != null) {
            top.add("identifier");
            top.add(JsonUtil.list(ids, true));
        }
        if (keywords != null) {
            top.add("keywords");
            top.add(JsonUtil.list(keywords, true));
        }
        JsonUtil.map(sb, top);
        sb.append("\n</script>\n");
    }

    public void  addTwitterCard(Request request, Entry entry, Appendable sb,String snippet
				,List<String[]> mtdThumbs) throws Exception {
	List<String[]> thumbs = new ArrayList<String[]>();
	String title = null;
	String image = null;
	String alt = null;
	String creator = null;		
	String mtdSnippet = null;

        List<Metadata> mtd =
            findMetadata(request, entry,
			 new String[]{"twitter_card"},true);

	if(mtd!=null) {
	    for(Metadata m: mtd) {
		if(!stringDefined(creator)) 
		    creator = m.getAttr1();
		if(!stringDefined(title)) 
		    title = m.getAttr2();
		if(!stringDefined(alt)) 
		    alt = m.getAttr(5);    
		if(!stringDefined(mtdSnippet))
		    mtdSnippet = m.getAttr(3);
		if(thumbs.size()==0) {
		    String url  = getType(m).getFileUrl(request, entry,m,true,null);
		    if(url!=null) {
			thumbs.add(new String[]{url,null});
		    }
		}
	    }
	}

	if(stringDefined(mtdSnippet)) {
	    snippet = mtdSnippet.replace("\n"," ").trim();
	}

	if(thumbs.size()==0) {
	    thumbs = mtdThumbs;
	}

	if(thumbs.size()>0) {
	    image = request.getAbsoluteUrl(thumbs.get(0)[0]);
	    if(!stringDefined(alt))
		alt = thumbs.get(0)[1];
	}

	//If not defined in the metadata then use the entry's name
	if(!stringDefined(title)) 
	    title = entry.getName();

	String type = stringDefined(image)?"summary_large_image":"summary";
	addMetadataTag(sb,"twitter:card",type);
	if(stringDefined(creator))
	    addMetadataTag(sb,"twitter:creator",creator);
	if(stringDefined(image)) {
	    addMetadataTag(sb,"twitter:image", image);
	    if(stringDefined(alt)) {
		addMetadataTag(sb,"twitter:image:alt",alt);
	    }
	}

	addMetadataTag(sb,"twitter:title",title);
	if(stringDefined(snippet)) {
	    addMetadataTag(sb,"twitter:description",snippet);
	}
    }

    public void addThumbnailUrl(Request request, Entry entry, String url,
                                String name,String...extra) {
        try {
	    if(name==null)
		name = IOUtil.getFileTail(url);
            InputStream is =
                getRepository().getStorageManager().getInputStream(url);
	    addThumbnailUrl(request, entry, is, name,extra);
        } catch (Exception exc) {
            System.err.println("Error fetching thumbnail:" + url);
        }
    }

    public void addThumbnailUrl(Request request, Entry entry, InputStream is,
                                String name,String...extra) {
        try {	    
            File f = getRepository().getStorageManager().getTmpFile(name);
            OutputStream fos =
                getRepository().getStorageManager().getFileOutputStream(f);
            try {
                IOUtil.writeTo(is, fos);
                f = getRepository().getStorageManager().moveToEntryDir(entry,
                        f);
                addMetadata(request,
                    entry,
                    new Metadata(
                        getRepository().getGUID(), entry.getId(),
                        findType(ContentMetadataHandler.TYPE_THUMBNAIL), false,
                        f.getName(),extra!=null && extra.length>0?extra[0]:null, null, null,null));
            } finally {
                IO.close(fos);
                IO.close(is);
            }
        } catch (Exception exc) {
	    getLogManager().logError("Error fetching thumbnail",exc);
        }
    }

    public boolean addThumbnail(Request request, Entry entry, boolean deleteExisting) throws Exception {
	JpegMetadataHandler jpegMetadataHandler = (JpegMetadataHandler) getHandler(JpegMetadataHandler.class);
	Metadata thumb = jpegMetadataHandler.getThumbnail(request, entry,null);
	if(thumb==null) return false;
	if(deleteExisting) {
	    List<Metadata> thumbs = findMetadata(request,entry,ContentMetadataHandler.TYPE_THUMBNAIL,false);
	    if(thumbs!=null) {
		for(Metadata oldThumb: thumbs) {
		    deleteMetadata(entry,oldThumb);
		}
	    }
	}
	addMetadata(request,entry,thumb,false);
	getEntryManager().updateEntry(request, entry);
	return true;
    }

    public void decorateEntry(Request request, Entry entry, Appendable sb,
                              boolean forLink)
            throws Exception {
	boolean fileOk = getAccessManager().canDoFile(request, entry);
        StringBuilder mine = new StringBuilder();
        for (Metadata metadata : getMetadata(request,entry)) {
            MetadataHandler handler = findMetadataHandler(metadata.getType());
            handler.decorateEntry(request, entry, mine, metadata, forLink,fileOk);
            if (forLink) {
                //Only do the first one so we don't get multiple thumbnails
                if (mine.length() > 0) {
                    break;
                }
            } else {
                if (mine.length() > 0) {
                    mine.append(HU.br());
                }
            }

        }
        sb.append(mine);
    }

    public void getTextCorpus(Request request, Entry entry, Appendable sb) throws Exception {
        for (Metadata metadata : getMetadata(request,entry)) {
            MetadataHandler handler = findMetadataHandler(metadata.getType());
            handler.getTextCorpus(entry, sb, metadata);
        }
    }

    public Metadata copyMetadata(Entry oldEntry, Entry newEntry,
                                 Metadata oldMetadata)
            throws Exception {
        MetadataHandler handler = findMetadataHandler(oldMetadata.getType());

        return handler.copyMetadata(oldEntry, newEntry, oldMetadata);
    }

    public  List<String> getThumbnailUrls(Request request, Entry entry,
					  List<String> urls,boolean...checkParent)
            throws Exception {
	List<String[]> tmp = new ArrayList<String[]>();
	getFullThumbnailUrls(request, entry, tmp,Utils.isTrue(checkParent,false));
	if(urls==null) urls = new ArrayList<String>();
	for(String[]tuple: tmp) {
	    urls.add(tuple[0]);
	}
	return urls;
    }

    public void getFullThumbnailUrls(Request request, Entry entry,
				     List<String[]> urls,boolean checkParent,boolean...mustBeInherited)
            throws Exception {
	int size = urls.size();

        for (Metadata metadata : getMetadata(request,entry)) {
	    if(mustBeInherited!=null && mustBeInherited.length>0 && mustBeInherited[0]) {
		if(!metadata.getInherited()) continue;
	    }
            MetadataHandler handler = findMetadataHandler(metadata.getType());
            handler.getThumbnailUrls(request, entry, urls, metadata);
        }
	if(checkParent && size==urls.size()) {
	    Entry parent = entry.getParentEntry();
	    if(parent!=null) {
		getFullThumbnailUrls(request, parent, urls,checkParent,true);
	    }
	}	    

    }

    public String[] getThumbnailUrl(Request request, Entry entry,boolean...checkParent)
            throws Exception {
        List<String[]> urls = new ArrayList<String[]>();
	getFullThumbnailUrls(request, entry, urls,Utils.isTrue(checkParent,false));
        if (urls.size() > 0) {
            return urls.get(0);
        }
        return null;
    }

    public String getMetadataUrl(Request request, Entry entry, String type)
            throws Exception {
	List<String> urls= getMetadataUrls(request, entry,type);
	if(urls==null) return null;
	return urls.get(0);
    }

    public List<String> getMetadataUrls(Request request, Entry entry, String type)
            throws Exception {
        List<Metadata> metadataList =  findMetadata(request, entry,type,true);
	if(metadataList==null || metadataList.size()==0) return null;
	List<String> urls =null;
	for(Metadata metadata:metadataList) {
	    String[]tuple=  getFileUrl(request, entry, metadata);
	    if(tuple==null) continue;
	    if(urls==null) urls = new ArrayList<String>();
	    urls.add(tuple[1]);
	}
	return urls;
    }

    public List<String[]> getFilelUrls(Request request, Entry entry)
            throws Exception {
        List<String[]> nameUrlPairs = new ArrayList<String[]>();
        for (Metadata metadata : getMetadata(request,entry)) {
            MetadataHandler handler = findMetadataHandler(metadata.getType());
            handler.getFileUrls(request, entry, nameUrlPairs, metadata);
        }

        return nameUrlPairs;
    }

    public List<String> getImageUrls(Request request, Entry entry)
            throws Exception {
        List<String> urls = new ArrayList<String>();
        List<String[]> metadataUrls =
            getMetadataManager().getFilelUrls(request, entry);
        for (String[] pair : metadataUrls) {
            //[0] is the filename,[1] is the url
            if (Utils.isImage(pair[0])) {
                urls.add(pair[1]);
            }
        }

        return urls;
    }

    public List<Metadata> findMetadata(Request request, Entry entry,
                                       String type, boolean checkInherited)
            throws Exception {
        return findMetadata(request, entry, new String[] { type },
                            checkInherited);

    }

    public List<Metadata> findMetadata(Request request, Entry entry,
                                       String[] type, boolean checkInherited)
            throws Exception {
        return findMetadata(request, entry, type, checkInherited, true);
    }

    public List<Metadata> findMetadata(Request request, Entry entry,
                                       String[] type, boolean checkInherited,
                                       boolean firstOk)
            throws Exception {
        List<Metadata> result = new ArrayList<Metadata>();
        findMetadata(request, entry, type, result, checkInherited, firstOk);
        return result;
    }

    public static boolean debug = false;

    public void debug(String msg) {
        if (debug) {
            logInfo(msg);
        }
    }

    public List<Metadata> getInheritedMetadata(Request request, Entry entry)
            throws Exception {
        List<Metadata> result = new ArrayList<Metadata>();
        //        this.debug  = true;
        //        EntryManager.debug  =true;
        Entry parent = getEntryManager().getParent(request, entry);
        //        EntryManager.debug  =false;
        if (parent == null) {
            //            debug("METADATA: getInheritedMetadata entry=" + entry.getName() + " parent is NULL");
        } else {
            //            debug("METADATA: getInheritedMetadata entry=" + entry.getName() + " parent:" + parent.getName());
            findInheritedMetadata(request, parent, result);
        }
        this.debug = false;

        return result;
    }

    private void findInheritedMetadata(Request request, Entry entry,
                                       List<Metadata> result)
            throws Exception {
        debug("METADATA: findInherited: entry=" + entry);
        if (entry == null) {
            return;
        }
        List<Metadata> metadataList = getMetadata(request,entry);
        debug("METADATA: findInheritedMetadata:" + metadataList);
        for (Metadata metadata : metadataList) {
            if ( !metadata.getInherited()) {
                continue;
            }
            result.add(metadata);
        }
        findInheritedMetadata(request,
                              getEntryManager().getParent(request, entry),
                              result);
    }

    private void findMetadata(Request request, Entry entry, String[] type,
                              List<Metadata> result, boolean checkInherited,
                              boolean firstTime)
            throws Exception {

        if (entry == null) {
            return;
        }
        if (debug) {
            System.out.println("metadata:" + type + " entry:" + entry);
        }
        for (Metadata metadata : getMetadata(request,entry)) {
            if (debug) {
                System.out.println("\ttype:" + metadata.getType() + " "
                                   + metadata.getInherited());
            }
            if ( !firstTime && !metadata.getInherited()) {
                if (debug) {
                    System.out.println("\tskip1");
                }

                continue;
            }
            if (type != null && type.length>0 && stringDefined(type[0])) {
                for (int i = 0; i < type.length; i++) {
		    if(metadata.getType()==null)
			System.err.println("NULL:" + metadata);
                    if (metadata.getType().equals(type[i])) {
                        result.add(metadata);

                        break;
                    }
                }
            } else {
                result.add(metadata);
            }
        }
	result = checkMetadata(request, entry, result);

	if (checkInherited) {
            findMetadata(request,
                         getEntryManager().getParent(request, entry), type,
                         result, checkInherited, false);
        }
    }

    public Metadata findMetadata(Request request, Entry entry, String id)
            throws Exception {
        if (entry == null) {
            return null;
        }
        for (Metadata metadata : getMetadata(request,entry)) {
            if (metadata.getId().equals(id)) {
                return metadata;
            }
        }

        return null;
    }

    public List<Metadata> checkMetadata(Request request,Entry entry, List<Metadata> metadata) {
	//for now
	if(true) return metadata;

	if(metadata.size()==0) return metadata;
	List<Metadata> result =new ArrayList<Metadata>();
	Boolean canEdit = null;
	for(Metadata mtd: metadata) {
	    MetadataType type = getType(mtd);
	    if(type==null) continue;
	    if(!type.getCanView()) {
		if(canEdit==null) {
		    try {
			canEdit=new Boolean(getAccessManager().canDoEdit(request,entry));
		    } catch(Exception exc) {
			throw new RuntimeException(exc);
		    }
		}
		if(!canEdit) {
		    System.err.println("can't edit:" + entry.getName()+" type:" + type + " mtd:" + mtd.getAttr1());
		    continue;
		}
	    }
	    result.add(mtd);
	}
	return result;
    }	

    public static boolean debugGetMetadata = false;

    public List<Metadata> getMetadataList(Request request,Entry entry) {
	return entry.getMetadataList(request);
    }

    public List<Metadata> getMetadata(Request request,Entry entry) throws Exception {
        return getMetadata(request,entry, null);
    }

    public List<Metadata> getMetadata(Request request,Entry entry, String type)
            throws Exception {
        if (entry.isDummy()) {
            return (entry.getMetadataList(request) == null)
                   ? new ArrayList<Metadata>()
		: getMetadata(request,entry,entry.getMetadataList(request), type);
        }
        List<Metadata> metadataList = entry.getMetadataList(request);
	if(debugGetMetadata) System.err.println("getMetadata:" + entry +" list:" +metadataList);
        if (metadataList != null) {
            metadataList =  getMetadata(request,entry,metadataList, type);
	    if(debugGetMetadata) System.err.println("getMetadata 2 list:" +metadataList);
	    return metadataList;
        }

        List<Metadata> finalMetadataList = readMetadata(Clause.eq(Tables.METADATA.COL_ENTRY_ID, entry.getId()));
        metadataList = Metadata.sort(finalMetadataList);
	if(debugGetMetadata) System.err.println("getMetadata 3 list:" +metadataList);
	setMetadataList(entry,metadataList);
        metadataList = getMetadata(request,entry,metadataList, type);
	if(debugGetMetadata) System.err.println("getMetadata 4 list:" +metadataList);
	return metadataList;
    }

    private List<Metadata> readMetadata(Clause clause) throws Exception {
	final List<Metadata> finalMetadataList = new ArrayList();
        Statement stmt =
            getDatabaseManager().select(
					Tables.METADATA.COLUMNS, Tables.METADATA.NAME,
					clause,
					getDatabaseManager().makeOrderBy(Tables.METADATA.COL_TYPE));

        getDatabaseManager().iterate(stmt, new SqlUtil.ResultsHandler() {
            public boolean handleResults(ResultSet results) throws Exception {
                int             col     = 1;
                String          type    = results.getString(3);
                MetadataHandler handler = findMetadataHandler(type);
                DatabaseManager dbm     = getDatabaseManager();

                finalMetadataList.add(
                    handler.makeMetadata(
                        dbm.getString(results, col++),
                        dbm.getString(results, col++),
                        dbm.getString(results, col++),
                        results.getInt(col++) == 1,
                        dbm.getString(results, col++),
                        getAttrString(results, col++),
                        getAttrString(results, col++),
                        getAttrString(results, col++),
                        getAttrString(results, col++),
                        dbm.getString(results, col++)));
		return true;
            }
        });
	return finalMetadataList;
    }

    public List<Metadata> getMetadata(Request request,Entry entry,List<Metadata> metadata, String type)
            throws Exception {
        if (type == null) {
            return checkMetadata(request,entry,metadata);
        }
        List<Metadata> tmp = new ArrayList<Metadata>();
        for (Metadata m : metadata) {
            if (m.getType().equals(type)) {
                tmp.add(m);
            }
        }
	return checkMetadata(request, entry, tmp);
    }

    private String getAttrString(ResultSet results, int col)
            throws Exception {
        String s = getDatabaseManager().getString(results, col);
        if ((s != null) && (s.length() > Metadata.MAX_LENGTH)) {
            s = s.substring(0, Metadata.MAX_LENGTH - 1);
        }

        return s;
    }

    public List<Metadata> getInitialMetadata(Request request, Entry entry,
                                             Hashtable extra,
                                             boolean shortForm) {
        List<Metadata> metadataList = new ArrayList<Metadata>();
        for (MetadataHandler handler : getMetadataHandlers()) {
            try {
                handler.getInitialMetadata(request, entry, metadataList,
                                           extra, shortForm);
            } catch (Exception exc) {
                System.err.println(
                    "MetadataManager.getInitialMetadata error: " + exc);
            }
        }

        return metadataList;
    }

    public boolean addInitialMetadata(Request request, Entry entry,
                                      Hashtable extra, boolean shortForm)
            throws Exception {
        boolean changed = false;
        for (Metadata metadata :
                getInitialMetadata(request, entry, extra, shortForm)) {
            if (addMetadata(request,entry, metadata, true)) {
                changed = true;
            }
        }
        if (extra.size() > 0) {
            changed = true;
        }

        return changed;
    }

    public void addMetadataAlias(Request request,Entry entry, String value) throws Exception {
        addMetadata(request,entry, ContentMetadataHandler.TYPE_ALIAS,CHECK_UNIQUE_FALSE, value);
    }

    public void addKeyword(Request request, Entry entry, String value) throws Exception {
        addMetadata(request,entry, ContentMetadataHandler.TYPE_KEYWORD, CHECK_UNIQUE_TRUE, value);
    }

    public void addMetadataTag(Request request,Entry entry, String value) throws Exception {
        addMetadata(request,entry, ContentMetadataHandler.TYPE_TAG, CHECK_UNIQUE_FALSE, value);
    }

    public void addMetadata(Request request,Entry entry, String type,     boolean checkUnique, String ...values)
            throws Exception {
	addMetadata(request, entry, type, INHERITED_FALSE,checkUnique,values);

    }
    public void addMetadata(Request request,Entry entry, String type, boolean inherited,boolean checkUnique, String ...values)
            throws Exception {	

        addMetadata(request,entry,
                    new Metadata(getRepository().getGUID(), entry.getId(),
                                 findType(type), inherited, values[0],
				 values.length>1 && values[1]!=null?values[1]:Metadata.DFLT_ATTR,
				 values.length>2 &&values[2]!=null?values[2]:Metadata.DFLT_ATTR,
				 values.length>3 && values[3]!=null?values[3]:Metadata.DFLT_ATTR,
                                 Metadata.DFLT_EXTRA),checkUnique);
    }

    public boolean addMetadata(Request request,Entry entry, Metadata value) throws Exception {
        return addMetadata(request,entry, value, CHECK_UNIQUE_FALSE);
    }

    public boolean addMetadata(Request request, Entry entry, Metadata value, boolean checkUnique)
            throws Exception {
	//Pass in result=null so we get the actual list
        List<Metadata> metadata = getMetadata(null,entry);
        if (checkUnique && metadata.contains(value)) {
            return false;
        }
	entry.setMetadataChanged(true);
        metadata.add(value);
        return true;
    }

    public void addXmlMetadata(Request request, Entry entry,
			       FileWriter fileWriter, Document doc,
			       Element parent,boolean encode)
            throws Exception {
        List<Metadata> metadataList = getMetadata(request,entry);
        for (Metadata metadata : metadataList) {
            MetadataHandler metadataHandler = findMetadataHandler(metadata);
            if (metadataHandler == null) {
                continue;
            }
            metadataHandler.addMetadata(request, entry, fileWriter, metadata,
                                        parent,encode);

        }
    }

    public List<MetadataHandler> getMetadataHandlers() {
        return metadataHandlers;
    }

    public MetadataHandler findMetadataHandler(Metadata metadata)
            throws Exception {
        MetadataHandler handler = handlerMap.get(metadata.getType());
        if (handler != null) {
            return handler;
        }
	return getDefaultMetadataHandler();
    }

    public MetadataHandler getDefaultMetadataHandler() {
        if (dfltMetadataHandler == null) {
            dfltMetadataHandler = new MetadataHandler(getRepository(), null);
        }

        return dfltMetadataHandler;
    }

    public MetadataHandler findMetadataHandler(String type) throws Exception {
        MetadataHandler handler = handlerMap.get(type);
        if (handler != null) {
            return handler;
        }
        if (dfltMetadataHandler == null) {
            dfltMetadataHandler = new MetadataHandler(getRepository(), null);
        }

        return dfltMetadataHandler;
    }

    public MetadataHandler getHandler(Class c) throws Exception {
        MetadataHandler handler = metadataHandlerMap.get(c);
        if (handler == null) {
            Constructor ctor = Misc.findConstructor(c,
                                   new Class[] { Repository.class });
            if (ctor == null) {
                throw new IllegalStateException(
                    "Could not find constructor for MetadataHandler:"
                    + c.getName());
            }

            handler = (MetadataHandler) ctor.newInstance(new Object[] {
                getRepository() });

            metadataHandlers.add(handler);
            metadataHandlerMap.put(c, handler);
        }

        return handler;
    }

    private void updateMetadata(Metadata mtd)  throws Exception {
	getDatabaseManager().update(Tables.METADATA.NAME,
				    Clause.eq(Tables.METADATA.COL_ID, mtd.getId()),
				    Tables.METADATA.ARRAY,
				    new Object[]{
					mtd.getId(),
					mtd.getEntryId(),
					mtd.getType(),
					mtd.getInherited(),
					mtd.getAccess(),
					mtd.getAttr1(),
					mtd.getAttr2(),
					mtd.getAttr3(),
					mtd.getAttr4(),
					mtd.getExtra()
				    });

	//	COL_ID,COL_ENTRY_ID,COL_TYPE,COL_INHERITED,COL_ACCESS,COL_ATTR1,COL_ATTR2,COL_ATTR3,COL_ATTR4,COL_EXTRA
    }

    private void applySchemaChange(String type,String command,int index,String value) throws Exception {
	MetadataType mtdType = findType(type);
	if(mtdType==null) {
	    getLogManager().logInfoAndPrint("MetadataManager.applySchemaChange: could not find type:" + type);
	    return;
	}

        List<Metadata> list = readMetadata(Clause.eq(Tables.METADATA.COL_TYPE,type));

	for(Metadata metadata: list) {
	    int maxElements =  metadata.getExtraCount();
	    if(command.equals("delete")) {
		System.err.println("delete: #" + maxElements);
		for(int i=index+1;i<=maxElements;i++) {
		    String  v = metadata.getAttr(i);
		    System.err.println("\tvalue:["+i+"]=" + v);
		    metadata.setAttr(i-1,v);
		}
	    } else if(command.equals("insert")) {
		System.err.println("insert:" + value +" #:" + maxElements +  " " + metadata.getExtraMap());
		for(int i=maxElements;i>=index;i--) {
		    String  v = metadata.getAttr(i);
		    if(v==null) v="";
		    System.err.println("\tvalue:["+i+"]=" + v);
		    metadata.setAttr(i+1,v);
		}
		metadata.setAttr(index,value);
	    } else {
		getLogManager().logError("MetadataManager.applySchemaChange: unknown command:" + command);
		continue;
	    }
	    updateMetadata(metadata);
	}
    }

    public void applySchemaChanges() throws Exception {
	boolean haveAppliedVersion = false;
	for(String line:
		Utils.split(getRepository().getResource("/org/ramadda/repository/resources/metadatachange.txt"),"\n",true,true)) {
	    if(line.startsWith("#")) continue;
	    if(line.startsWith("version:")) {
		String version ="propertychange.version." +line.substring("version:".length()).trim();
		haveAppliedVersion = getRepository().getDbProperty(version,false);
		if(haveAppliedVersion) {
		    getLogManager().logInfoAndPrint("MetadataManager.applySchemaChange: have applied version:" + version);
		} else  {
		    getRepository().writeGlobal(version,true);
		    getLogManager().logInfoAndPrint("MetadataManager.applySchemaChange: applying version:" + version);
		}
		continue;
	    }
	    if(haveAppliedVersion) continue;
	    List<String> toks = Utils.split(line,",",true,true);
	    if(toks.size()<=1) continue;
	    String type = toks.get(0);
	    for(int i=1;i<toks.size();i++) {
		List<String>toks2 = Utils.split(toks.get(i),":",true,true);
		if(toks2.size()<2) {
		    continue;
		}
		getLogManager().logInfoAndPrint("MetadataManager.applySchemaChange: applying command to:" + type +" command:" + toks2);
		applySchemaChange(type,toks2.get(0),Integer.parseInt(toks2.get(1)),
				  toks2.size()>2?toks2.get(2):"");
	    }
	}
    }

    public void loadMetadataHandlers(PluginManager pluginManager)
            throws Exception {
	initializeState();

        HashSet seen = new HashSet();
        List<String> metadataDefFiles =
            getRepository().getPluginManager().getMetadataDefFiles();
        for (String file : metadataDefFiles) {
            try {
                file = getStorageManager().localizePath(file);
                if (seen.contains(file)) {
                    //              System.out.println("pluginManager seen:" + file);
                    continue;
                }
                seen.add(file);
                Element root = XmlUtil.getRoot(file, getClass());
                if (root == null) {
                    System.out.println(
                        "MetadataManager: no root element found in:" + file);
                    continue;
                }
                //              System.out.println("MetadataManager: processing:" + file); 
                MetadataType.parse(root, this);
                debug = false;
            } catch (Exception exc) {
                System.out.println("MetadataManager: error:" + file + " "
                                   + exc);
                logError("Error loading metadata handler file:" + file, exc);

                throw exc;
            }

        }
    }

    public Appendable addToSearchForm(Request request, Appendable sb)
            throws Exception {
        for (MetadataType type : metadataTypes) {
            if ( !type.getSearchable()) {
                continue;
            }
            type.getHandler().addToSearchForm(request, sb, type);
        }

        return sb;
    }

    public List<MetadataType> getMetadataTypes() {
        return metadataTypes;
    }

    public String[] getFileUrl(Request request, Entry entry,
                               Metadata metadata)
            throws Exception {
        MetadataType type = getType(metadata);

        return type.getFileUrl(request, entry, metadata);
    }

    public File getFile(Request request, Entry entry, Metadata metadata,
                        int attr)
            throws Exception {
        MetadataType type = getType(metadata);

        return type.getFile(entry, metadata, attr);
    }

    public List<Metadata> getMetadataFromClipboard(Request request)
            throws Exception {
        List<Metadata> metadata =
            (List<Metadata>) getSessionManager().getSessionProperty(request,
                PROP_METADATA);

        return metadata;
    }

    public void copyMetadataToClipboard(Request request,
                                        List<Metadata> metadataList)
            throws Exception {
        List<Metadata> copies = new ArrayList<Metadata>();
        for (Metadata metadata : metadataList) {
            copies.add(new Metadata(metadata));
        }
        getSessionManager().putSessionProperty(request, PROP_METADATA,
                copies);
    }

    public Appendable addToBrowseSearchForm(Request request, Appendable sb)
            throws Exception {
        StringBuilder      tmp      = new StringBuilder();
        List<String>       titles   = new ArrayList<String>();
        List<String>       contents = new ArrayList<String>();
        List               rows     = new ArrayList();
        List<MetadataType> sorted =
            new ArrayList<MetadataType>(metadataTypes);
        Collections.sort(sorted);
        for (MetadataType type : sorted) {
            if ( !type.getBrowsable()) {
                continue;
            }
            String link =
                HU
                    .href(request
                        .makeUrl(getRepository().getMetadataManager()
                            .URL_METADATA_LIST, ARG_METADATA_TYPE,
                                type.toString()), type.getLabel());

            rows.add("<li>" + link);
            //            type.getHandler().addToBrowseSearchForm(request, tmp, type, titles, contents);
        }
        List<List> lists = Utils.splitList(rows, 8);
        List       cols  = new ArrayList();
        for (List list : lists) {
            cols.add("<ul>" + Utils.join(list, "") + "</ul>");
        }
        HU.centerBlock(sb, HU.hrow(cols));
        //        HU.makeAccordion(sb, titles, contents);

        return sb;
    }

    public Result processMetadataTypes(Request request) throws Exception {
	StringBuilder sb = new StringBuilder();
	getPageHandler().sectionOpen(request, sb,"Metadata Types",false);
	sb.append(HU.center("Click on a button to copy the metadata ID"));
	HU.script(sb,"HtmlUtils.initPageSearch('.ramadda-type',null,'Find Type')");
	Hashtable<String,NamedBuffer> map = new Hashtable<String,NamedBuffer>();
	List<NamedBuffer> contents = new ArrayList<NamedBuffer>();
        for (MetadataType type : metadataTypes) {
            String        cat    = type.getCategory();
	    NamedBuffer buffer = map.get(cat);
	    if(buffer==null) {
		contents.add(buffer = new NamedBuffer(cat));
		map.put(cat,buffer);
	    }
	    String corpus = cat +" " + type.getId() + " " + type.getName();
	    buffer.append(HU.span(type.getName(),HU.attrs("title","Click to copy: " + type.getId(),
							  "data-corpus",corpus,"data-copy",type.getId(),
							  "style","margin:4px;","class","ramadda-button ramadda-type-id ramadda-type")));
	}
	for(NamedBuffer buffer: contents) {
	    HU.div(sb,buffer.getName(),HU.clazz("ramadda-form-header"));
	    sb.append(buffer.getBuffer().toString());
	}
	HU.script(sb,"Utils.initCopyable('.ramadda-type-id');");
	getPageHandler().sectionClose(request, sb);
	return new Result("Metadata Types",sb);
    }

    public void processMetadataXml(Request request,Entry entry, Element entryChild,
                                   Hashtable filesMap, EntryManager.INTERNAL isInternal)
            throws Exception {
        String          type    = XmlUtil.getAttribute(entryChild, ATTR_TYPE);
        MetadataHandler handler = findMetadataHandler(type);
        handler.processMetadataXml(request,entry, entryChild, filesMap, isInternal);
    }

    public void initNewEntry(Request request,Entry entry, EntryInitializer initializer)
            throws Exception {
        for (Metadata metadata : getMetadata(request,entry)) {
            MetadataHandler handler = findMetadataHandler(metadata.getType());
            handler.initNewEntry(metadata, entry, initializer);
        }
    }

    public Result processMetadataChange(Request request) throws Exception {
        synchronized (MUTEX_METADATA) {
            Entry entry = getEntryManager().getEntry(request);
            Entry parent = getEntryManager().getParent(request, entry);

            if (request.defined(ARG_ENTRY_TIMESTAMP)) {
                String formTimestamp = request.getString(ARG_ENTRY_TIMESTAMP, "0");
                String currentTimestamp = getEntryManager().getEntryTimestamp(entry);
                if ( !Misc.equals(formTimestamp, currentTimestamp)) {
                    StringBuilder sb        = new StringBuilder();
                    String        dateRange = "";
                    try {
			dateRange = getDateHandler().parseDate(formTimestamp) + ":"
			    + getDateHandler().parseDate(currentTimestamp);
                    } catch (Exception ignore) {}
                    sb.append(getPageHandler().showDialogError(
							       msgLabel(
								   "Error: The entry you are editing has been edited since the time you began the edit" + dateRange)));

		    return  processMetadataForm(request, entry, sb);
		}
	    }		

            boolean canEditParent = (parent != null)
                                    && getAccessManager().canDoEdit(request,
                                        parent);

            if (request.exists(ARG_METADATA_DELETE)) {
                Hashtable args = request.getArgs();
                for (Enumeration keys =
                        args.keys(); keys.hasMoreElements(); ) {
                    String arg = (String) keys.nextElement();
                    if ( !arg.startsWith(ARG_METADATA_ID + SUFFIX_SELECT)) {
                        continue;
                    }
		    String id =       request.getString(arg, BLANK);
		    if(!stringDefined(id)) continue;
		    Metadata metadata = findMetadata(request, entry,id);
		    if(metadata!=null) deleteMetadata(entry,metadata);
                }
            } else {
                List<Metadata> newMetadataList  = new ArrayList<Metadata>();
                List<Metadata> existingMetadata = getMetadata(request,entry);
                Hashtable<String, Metadata> map = new Hashtable<String,
                                                      Metadata>();
                for (Metadata metadata : existingMetadata) {
                    map.put(metadata.getId(), metadata);
                }

                for (MetadataHandler handler : metadataHandlers) {
                    handler.handleFormSubmit(request, entry, map,
                                             newMetadataList);
                }

                if ( !request.isAnonymous()
                        && request.exists(ARG_METADATA_CLIPBOARD_COPY)) {
                    List<Metadata> toCopy = new ArrayList<Metadata>();
                    for (Metadata metadata : newMetadataList) {
                        if (request.defined(ARG_METADATA_ID + SUFFIX_SELECT
                                            + metadata.getId())) {
                            toCopy.add(metadata);
                        }
                    }
                    copyMetadataToClipboard(request, toCopy);
                }

                if (canEditParent
                        && request.exists(ARG_METADATA_ADDTOPARENT)) {
                    List<Metadata> parentMetadataList = getMetadata(request,parent);
                    int            cnt                = 0;

                    for (Metadata metadata : newMetadataList) {
                        if (request.defined(ARG_METADATA_ID + SUFFIX_SELECT
                                            + metadata.getId())) {
                            Metadata newMetadata =
                                new Metadata(getRepository().getGUID(),
                                             parent.getId(), metadata);

                            if ( !parentMetadataList.contains(newMetadata)) {
                                insertMetadata(newMetadata);
                                cnt++;
                            }
                        }
                    }
                    setMetadataList(parent,null);
                    parent.getTypeHandler().metadataChanged(request, parent);

                    return new Result(request.makeUrl(URL_METADATA_FORM,
                            ARG_ENTRYID, parent.getId(), ARG_MESSAGE,
                            cnt + " "
                            +  "metadata items added"));

                }

                for (Metadata metadata : newMetadataList) {
                    getDatabaseManager().delete(Tables.METADATA.NAME,getClause(metadata));
                    insertMetadata(metadata);
                }
            }
	    metadataHasChanged(entry);
            entry.getTypeHandler().metadataChanged(request, entry);
            getRepository().checkModifiedEntries(request,Misc.newList(entry));

            return new Result(request.makeUrl(URL_METADATA_FORM, ARG_ENTRYID,
                    entry.getId()));
        }
    }

    public Result processMetadataSuggest(Request request) throws Exception {
        StringBuilder sb     = new StringBuilder();

        String        value  = request.getString("value", "").trim();
        Clause        clause = Clause.eq(Tables.METADATA.COL_TYPE,
                                         "enum_tag");
        if (value.length() > 0) {
            clause = Clause.and(clause,
                                Clause.like(Tables.METADATA.COL_ATTR1,
                                            value + "%"));
        }
        int limit = 30;
        String l =
            getDatabaseManager().makeOrderBy(Tables.METADATA.COL_ATTR1)
            + getDatabaseManager().getLimitString(0, limit);

        Statement stmt = getDatabaseManager().select(
                             SqlUtil.distinct(Tables.METADATA.COL_ATTR1),
                             Tables.METADATA.NAME, clause, l);
        List<String> values = (List<String>) Utils.makeListFromArray(
                                  SqlUtil.readString(
                                      getDatabaseManager().getIterator(stmt),
                                      1));
        if (values.size() > limit) {
            List<String> tmp = new ArrayList<String>();
            for (int i = 0; (i < values.size()) && (i < limit); i++) {
                tmp.add(values.get(i));
            }
            values = tmp;
        }
        sb.append(JsonUtil.list(values, true));

        return new Result("", sb, JsonUtil.MIMETYPE);
    }

    public Result processMetadataList(Request request) throws Exception {
        boolean doCloud = request.getString(ARG_TYPE, "list").equals("cloud");
        String  header;
        if (doCloud) {
            request.put(ARG_TYPE, "list");
            header = HU.href(request.getUrl(), msg("List"))
                     + HU.span(
                         "&nbsp;|&nbsp;",
                         HU.cssClass(
                             CSS_CLASS_SEPARATOR)) + HU.b(
                                 msg("Cloud"));
        } else {
            request.put(ARG_TYPE, "cloud");
            header = HU.b(msg("List"))
                     + HU.span(
                         "&nbsp;|&nbsp;",
                         HU.cssClass(
                             CSS_CLASS_SEPARATOR)) + HU.href(
                                 request.getUrl(), msg("Cloud"));
        }
        //Don't do cloud
        header = "";
        String metadataType     = request.getString(ARG_METADATA_TYPE, "");
        MetadataHandler handler = findMetadataHandler(metadataType);
        MetadataType    type    = handler.findType(metadataType,false);
        StringBuilder   sb      = new StringBuilder();
        if ( !request.responseAsJson()) {
            sb.append(HU.sectionOpen("Metadata: " + (type==null?"Undefined property":type.getLabel())));
            sb.append(HU.center(header));
        }
	if(type==null) {
	    if (request.responseAsJson()) {
		sb.append("{\"undefined\":true}");
	    }
	} else {
	    doMakeTagCloudOrList(request, metadataType, sb, doCloud, 0);
	}
        if (request.responseAsJson()) {
            request.setCORSHeaderOnResponse();
            return new Result("", sb, JsonUtil.MIMETYPE);
        }

        sb.append(HU.sectionClose());

        return getSearchManager().makeResult(request,
                                             msg(type.getLabel() + " Cloud"),
                                             sb);
    }

    private String makeElementJson(MetadataElement element,List<String>maps) {
	List<String>attrs = new ArrayList<String>();
	Utils.add(attrs,"id",JsonUtil.quote(element.getId()),"name",JsonUtil.quote(element.getLabel()),
		  "index",""+element.getIndex());

	Utils.add(attrs,"type",JsonUtil.quote(element.isEnumeration()?"enumeration":"string"));
	if(maps!=null) Utils.add(attrs,"values",JsonUtil.list(maps));
	return JsonUtil.map(attrs);
    }

    public List<MetadataElement> getSearchableElements(MetadataType type) {
	List<MetadataElement> searchableElements = new ArrayList<MetadataElement>();
	for(MetadataElement child:type.getChildren()) {
	    if(child.getSearchable()) {
		searchableElements.add(child);
	    }
	}
	//Default to the first one
	if(searchableElements.size()==0 && type.getChildren().size()>0)  {
	    searchableElements.add(type.getChildren().get(0));
	}
	return searchableElements;
    }

    public void doMakeTagCloudOrList(Request request, String metadataType,
                                     Appendable sb, boolean doCloud,
                                     int threshold)
            throws Exception {

        boolean         doJson  = request.responseAsJson();
        MetadataHandler handler = findMetadataHandler(metadataType);
        MetadataType    type    = handler.findType(metadataType);
        if ((type == null) || (type.getChildren() == null)) {
            if (doJson) {
                sb.append(JsonUtil.list(new ArrayList<String>()));
            }
            return;
        }
	List<MetadataElement> searchableElements = getSearchableElements(type);
	List<String> jsonItems = new ArrayList<String>();

	for(MetadataElement element:searchableElements) {
	    if(!element.isEnumeration()) {
		if(doJson) {
		    jsonItems.add(makeElementJson(element,null));
		}
		continue;
	    }
	    List<HtmlUtils.Selector>      enumValues = element.getValues();
	    Hashtable<String, String> labels     = new Hashtable<String,  String>();
	    if (enumValues != null) {
		for (HtmlUtils.Selector sel: enumValues) {
		    labels.put((String) sel.getId(), (String) sel.getLabel());
		}
	    }
	    String[] values = getDistinctValues(request, handler, type,element.getIndex());
	    int[]    cnt    = new int[values.length];
	    int      max    = -1;
	    int      min    = 10000;
	    for (int i = 0; i < values.length; i++) {
		String value = values[i];
		cnt[i] = 0;
		Statement stmt = getDatabaseManager().select(
							     SqlUtil.count("*"), Tables.METADATA.NAME,
							     Clause.and(Clause.eq(Tables.METADATA.COL_TYPE,
										  type.getId()),
									Clause.eq(Tables.METADATA.COL_ATTR1,value)));
		ResultSet results = stmt.getResultSet();
		if ( !results.next()) {
		    continue;
		}
		cnt[i] = results.getInt(1);
		max    = Math.max(cnt[i], max);
		min    = Math.min(cnt[i], min);
		getDatabaseManager().closeAndReleaseConnection(stmt);
	    }
	    int    diff         = max - min;
	    double distribution = diff / 5.0;
	    if ( !doCloud) {
		List tuples = new ArrayList();
		for (int i = 0; i < values.length; i++) {
		    if (cnt[i] > threshold) {
			tuples.add(new Object[] {Integer.valueOf(cnt[i]),values[i] });
		    }
		}
		tuples = Misc.sortTuples(tuples, false);
		if (doJson) {
		    List<String> maps = new ArrayList<String>();
		    for (int i = 0; i < tuples.size(); i++) {
			Object[] tuple = (Object[]) tuples.get(i);
			String   value = (String) tuple[1];
			String   label = labels.get(value);
			if (label == null) {
			    label = value;
			}
			maps.add(JsonUtil.map(Utils.makeListFromValues("count",
								       tuple[0].toString(), "value",
								       JsonUtil.quote(value), "label",
								       JsonUtil.quote(label))));
		    }
		    jsonItems.add(makeElementJson(element,maps));

		} else {

		    List rows = new ArrayList();
		    for (int i = 0; i < tuples.size(); i++) {
			Object[] tuple = (Object[]) tuples.get(i);
			String   value = (String) tuple[1];
			String   label = value;
			if (value.trim().length() == 0) {
			    label = "----";
			}
			StringBuilder row = new StringBuilder();
			row.append("<tr><td>");
			row.append(tuple[0].toString());
			row.append("</td><td>");
			row.append(HU.href(handler.getSearchUrl(request,
								type, value), label));
			row.append("</td></tr>");
			rows.add(row);
		    }
		    List       cols  = new ArrayList();
		    List<List> lists = Utils.splitList(rows, 15);
		    for (List row : lists) {
			cols.add(HU.formTable()
				 + HU
				 .row(HU.cols(HU.b("# entries"),
					      HU.b(type
						   .getLabel())), "class=ramadda-table-header") + Utils
				 .join(row, "") + HU.formTableClose());
		    }
		    sb.append(
			      Utils.wrap(
					 cols,
					 "<div style='vertical-align:top;display:inline-block;margin:15px;'>",
					 "</div>"));
		}

	    } else {
		for (int i = 0; i < values.length; i++) {
		    if (cnt[i] <= threshold) {
			continue;
		    }
		    double percent = cnt[i] / distribution;
		    int    bin     = (int) (percent * 5);
		    String css     = "font-size:" + (12 + bin * 2);
		    String value   = values[i];
		    String ttValue = value.replace("\"", "'");
		    if (value.length() > 30) {
			value = value.substring(0, 29) + "...";
		    }
		    sb.append("<span style=\"" + css + "\">");
		    String extra = XmlUtil.attrs("alt",
						 "Count:" + cnt[i] + " "
						 + ttValue, "title",
                                                 "Count:" + cnt[i] + " "
                                                 + ttValue);
		    sb.append(HU.href(handler.getSearchUrl(request, type,
							   values[i]), value, extra));
		    sb.append("</span>");
		    sb.append(" &nbsp; ");
		}
	    }
	}
	if(doJson) {
	    List<String> obj = new ArrayList<String>();
	    Utils.add(obj,"metadataType",JsonUtil.quote(type.getId()),"metadataLabel",JsonUtil.quote(type.getLabel()));
	    Utils.add(obj,"addNot",""+type.getAddNot());
	    Utils.add(obj,"elements",JsonUtil.list(jsonItems));
	    sb.append(JsonUtil.map(obj));
	}
    }

    public Result processMetadataView(Request request) throws Exception {
        long  t1    = System.currentTimeMillis();
        Entry entry = getEntryManager().getEntry(request);
        if (entry == null) {
            Result result = getRepository().makeErrorResult(request,"No entry");
            result.setResponseCode(Result.RESPONSE_NOTFOUND);
            return result;
        }
	if(!getAccessManager().canDoFile(request, entry)) {
            Result result = getRepository().makeErrorResult(request,"No access");
            return result;
	}

        List<Metadata> metadataList = getMetadata(request,entry);
        Metadata metadata = findMetadata(request, entry,
                                         request.getString(ARG_METADATA_ID,
                                             ""));
        if (metadata == null) {
            String attachment = IO.getFileTail(request.getRequestPath());
            for (Metadata md : metadataList) {
                metadata = md;
                MetadataType metadataType =
                    getMetadataManager().findType(metadata.getType());
                if (metadataType == null) {
                    continue;
                }
                MetadataElement element =
                    metadataType.getDisplayImageElement(request, entry,
                        metadata, attachment);

                if (element != null) {
                    return metadataType.processView(request, entry, metadata,
                            element);
                }
            }

            return new Result("", "Could not find metadata");
        }
        long            t2      = System.currentTimeMillis();
        MetadataHandler handler = findMetadataHandler(metadata.getType());
        Result          result = handler.processView(request, entry,
                                     metadata);

        long            t3      = System.currentTimeMillis();
        //        result = getEntryManager().addEntryHeader(request,     request.getRootEntry(), result);
        long t4 = System.currentTimeMillis();

        //        Utils.printTimes("metadata", t1,t2,t3,t4);
        return result;
    }

    public Result processMetadataForm(Request request) throws Exception {
        Entry         entry = getEntryManager().getEntry(request);
        StringBuilder sb    = new StringBuilder();
        request.appendMessage(sb);

        return processMetadataForm(request, entry, sb);
    }

    public Result processMetadataForm(Request request, Entry entry,
                                      Appendable sb)
            throws Exception {
        boolean canEditParent = getAccessManager().canDoEdit(request,
                                    getEntryManager().getParent(request,
                                        entry));

	getEntryManager().addEntryEditHeader(request, entry,sb, URL_METADATA_FORM);
        List<Metadata> metadataList = getMetadata(request,entry);
        if (metadataList.size() != 0) {
	    sb.append(getWikiManager().getNewPropertyLinks(request,  entry,Utils.makeMap("fromEntry","true","showToggle","true","class","ramadda-button-small")));
	}

        if (metadataList.size() == 0) {
            sb.append(
                getPageHandler().showDialogNote(
                    msg("No properties defined for entry")));
            sb.append(msgLabel("Add new property"));
            makeAddList(request, entry, sb);
        } else {
            sb.append("\n");
            String   formId   = HU.getUniqueId("metadata_");
            FormInfo formInfo = new FormInfo(formId);
            request.uploadFormWithAuthToken(sb, URL_METADATA_CHANGE,
                                            HU.attr("name", "metadataform")
                                            + HU.id(formId));

            sb.append("\n");
            sb.append(HU.hidden(ARG_ENTRYID, entry.getId()));
	    sb.append(HU.hidden(ARG_ENTRY_TIMESTAMP,
				getEntryManager().getEntryTimestamp(entry)));
            sb.append("\n");
            String buttons = HU.buttons(
                                 HU.submit("Change"),
                                 HU.submit("Delete selected",
                                     ARG_METADATA_DELETE), HU.submit("Copy selected to clipboard",
                                         ARG_METADATA_CLIPBOARD_COPY));
	    String allId = HU.getUniqueId("all_");
	    String toggle  = HU.labeledCheckbox("all","",false,HU.attrs("id",allId),"Toggle all");
            sb.append(HU.leftRight(buttons,HU.inset(toggle,0,0,0,110)));
            List<String> titles   = new ArrayList<String>();
            List<String> contents = new ArrayList<String>();

            for (Metadata metadata : metadataList) {
                metadata.setEntry(entry);
                MetadataHandler metadataHandler =
                    findMetadataHandler(metadata);
                if (metadataHandler == null) {
                    continue;
                }
                String[] html = metadataHandler.getForm(request, formInfo,
                                    entry, metadata, true);
                if (html == null) {
                    continue;
                }

                String cbxId = "cbx_" + metadata.getId();
                String cbx =
                    HU.labeledCheckbox(
                        ARG_METADATA_ID + SUFFIX_SELECT + metadata.getId(),
                        metadata.getId(), false,
                        HU.id(cbxId) + " "
                        + HU.attr(
                            HU.ATTR_TITLE,
                            msg(
                            "Shift-click: select range; Control-click: toggle all")) +
			HU.cssClass("ramadda-metadata-select") +
			HU.attr(
                                HU.ATTR_ONCLICK,
                                HU.call(
                                    "HU.checkboxClicked",
                                    HU.comma(
                                        "event", HU.squote("cbx_"),
                                        HU.squote(cbxId)))),"Select");

                StringBuilder metadataEntry = new StringBuilder();
                HU.comment(metadataEntry, "Metadata part begin");
                metadataEntry.append(HU.formTable());
		//                metadataEntry.append(HU.formEntry("",cbx));
                metadataEntry.append("\n");
                metadataEntry.append(html[1]);
                HU.formTableClose(metadataEntry);
                HU.comment(metadataEntry, "Metadata part end");
                titles.add(html[0] + HU.span(cbx.toString(),HU.cssClass("accordion-toolbar") +HU.style("float:right;")));
                String content = HU.div(
                                     metadataEntry.toString(),
                                     HU.cssClass(
                                         "ramadda-metadata-form"));
                contents.add(content);
            }
            sb.append(HU.beginInset(10, 30, 10, 100));
            HU.makeAccordion(sb, titles, contents);
            sb.append(HU.endInset());
            sb.append(buttons);
	    HU.addFormChangeListener(sb,formId);

            HU.comment(sb, "Metadata form end");
            formInfo.addToForm(sb);
	    HU.script(sb,"HU.initToggleAll('" + allId +"','.ramadda-metadata-select');\n");
            sb.append(HU.formClose());
            sb.append("\n");

        }

        getPageHandler().entrySectionClose(request, entry, sb);

        return getEntryManager().makeEntryEditResult(request, entry,
                msg("Edit Properties"), sb);

    }

    public Result processMetadataAddForm(Request request) throws Exception {
        StringBuilder sb    = new StringBuilder();
        Entry         entry = getEntryManager().getEntry(request);

        if (request.get(ARG_METADATA_CLIPBOARD_PASTE, false)) {
            getPageHandler().entrySectionOpen(request, entry, sb,
                    "Add Property");
            List<Metadata> clipboard = getMetadataFromClipboard(request);
            if ((clipboard == null) || (clipboard.size() == 0)) {
                sb.append(
                    getPageHandler().showDialogError("Clipboard empty"));
            } else {
                //TODO: file attachments
                for (Metadata copiedMetadata : clipboard) {
                    Metadata newMetadata =
                        new Metadata(getRepository().getGUID(),
                                     entry.getId(), copiedMetadata);
                    insertMetadata(newMetadata);
                }
		metadataHasChanged(entry);
                sb.append(
                    getPageHandler().showDialogNote(
                        "Metadata pasted from clipboard"));
            }

            sb.append(HU.sectionClose());

            return processMetadataForm(request, entry, sb);
        }

        if ( !request.exists(ARG_METADATA_TYPE)) {
            getEntryManager().addEntryEditHeader(request, entry,sb, URL_METADATA_ADDFORM);
            makeAddList(request, entry, sb);
        } else {
            String          type = request.getString(ARG_METADATA_TYPE,
                                       BLANK);
            MetadataHandler handler = findMetadataHandler(type);
            if (handler != null) {
                MetadataType metadataType = handler.findType(type);
                getPageHandler().entrySectionOpen(request, entry, sb,
                        msgLabel("Add Property") + metadataType.getLabel());
                handler.makeAddForm(request, entry, metadataType, sb);

            }
        }

        sb.append(HU.sectionClose());

        return getEntryManager().makeEntryEditResult(request, entry,
                msg("Add Property"), sb);
    }

    private void makeAddList(Request request, Entry entry, Appendable sb)
            throws Exception {
        List<String>   groups    = new ArrayList<String>();
        Hashtable      groupMap  = new Hashtable();

        List<Metadata> clipboard = getMetadataFromClipboard(request);
        if ((clipboard != null) && (clipboard.size() > 0)) {
            StringBuilder clipboardSB = new StringBuilder();
            Entry         dummyEntry  = new Entry();
            int           cnt         = 0;
            for (Metadata copied : clipboard) {
                MetadataHandler handler =
                    findMetadataHandler(copied.getType());
                MetadataType type  = handler.getType(copied.getType());
                String       label = type.getTypeLabel(copied);
                String       row   = label;
                clipboardSB.append(row);
                clipboardSB.append("<br>");
                cnt++;
            }

            request.uploadFormWithAuthToken(sb, URL_METADATA_ADDFORM);
            sb.append(HU.hidden(ARG_ENTRYID, entry.getId()));
            sb.append(HU.hidden(ARG_METADATA_CLIPBOARD_PASTE, "true"));
            sb.append(HU.submit("Copy from Clipboard"));
            sb.append(HU.formClose());
            sb.append(HU.makeShowHideBlock("Clipboard",
                    clipboardSB.toString(), false));
            sb.append(HU.p());
        }

        for (MetadataType type : metadataTypes) {
            if (type.getAdminOnly() && !request.getUser().getAdmin()) {
                continue;
            }
            if ( !type.getForUser()) {
                continue;
            }
            if ( !type.isForEntry(entry)) {
                continue;
            }
            String        name    = type.getCategory();
            StringBuilder groupSB = (StringBuilder) groupMap.get(name);
            if (groupSB == null) {
                groupMap.put(name, groupSB = new StringBuilder());
                groups.add(name);
            }
            //            request.uploadFormWithAuthToken(groupSB, URL_METADATA_ADDFORM);

	    groupSB.append(HU.open("span",HU.attrs("class","ramadda-metadata-add","data-corpus",type.getLabel())));
            groupSB.append(request.form(URL_METADATA_ADDFORM,HU.style("display:inline-block;margin:5px;")));
            groupSB.append(HU.hidden(ARG_ENTRYID, entry.getId()));
            groupSB.append(HU.hidden(ARG_METADATA_TYPE, type.getId()));
            groupSB.append(HU.submit("Add" + HU.space(1)
                                            + type.getLabel()));
            if (Utils.stringDefined(type.getHelp())) {
                groupSB.append(HU.space(1));
                groupSB.append(HU.span(HU.faIcon("fas fa-info"),HU.attrs("title",type.getHelp().trim(),"class","ramadda-clickable")));
            }
            groupSB.append(HU.formClose());
	    groupSB.append(HU.close("span"));
	    //            groupSB.append(HU.p());
            groupSB.append(NEWLINE);
        }

        List<String> titles = new ArrayList<String>();
        List<String> tabs   = new ArrayList<String>();

        for (String name : groups) {
            titles.add(name);
            tabs.add(HU.insetDiv(groupMap.get(name).toString(), 5, 10,
                                        5, 10));
        }
	String uid =  HU.getUniqueId("types");
	sb.append("<center>");
	HU.script(sb,"HtmlUtils.initPageSearch('.ramadda-metadata-add','.ramadda-metadata-group','Find Property')");
	sb.append("</center>");
	sb.append(HU.open("div",HU.attr("id",uid)));
	for(int i=0;i<titles.size();i++) {
	    sb.append(HU.open("div",HU.clazz("ramadda-metadata-group")));
	    sb.append(HU.div(titles.get(i),HU.attrs("style","margin-top:10px","class","ramadda-form-header xramadda-metadata-add")));
	    sb.append(tabs.get(i));
	    sb.append(HU.close("div"));
	}
	sb.append(HU.close("div"));

	//        HU.makeAccordion(sb, titles, tabs);
    }

    public Result processMetadataAdd(Request request) throws Exception {
        synchronized (MUTEX_METADATA) {
            Entry entry = getEntryManager().getEntry(request);
            if (request.exists(ARG_CANCEL)) {
                return new Result(request.makeUrl(URL_METADATA_ADDFORM,
                        ARG_ENTRYID, entry.getId()));
            }
            List<Metadata> newMetadata = new ArrayList<Metadata>();
            for (MetadataHandler handler : metadataHandlers) {
                handler.handleAddSubmit(request, entry, newMetadata);
            }

            for (Metadata metadata : newMetadata) {
                insertMetadata(metadata);
            }

	    metadataHasChanged(entry);
            getRepository().checkModifiedEntries(request,
						 Misc.newList(entry));

            return new Result(request.makeUrl(URL_METADATA_FORM, ARG_ENTRYID,
                    entry.getId()));

        }
    }

    public void  setMetadataList(Entry entry,List<Metadata> list) throws Exception {
	entry.setMetadata(list);
    }

    public void  metadataHasChanged(Entry entry) throws Exception {
	getEntryManager().metadataHasChanged(entry);
	entry.setMetadata(null);
    }

    public String[] getDistinctValues(Request request,
                                      MetadataHandler handler,
                                      MetadataType type)
            throws Exception {
	return getDistinctValues(request, handler,type,1);
    }

    public String[] getDistinctValues(Request request,
                                      MetadataHandler handler,
                                      MetadataType type,int attr)
            throws Exception {	
        Hashtable myDistinctMap = distinctMap;
	String key = type.getId()+"_"+attr;
        String[]  values        = (String[]) ((myDistinctMap == null)
                ? null
                : myDistinctMap.get(key));

        if (values == null) {
	    String col = attr==1?Tables.METADATA.COL_ATTR1:
		attr==2?Tables.METADATA.COL_ATTR2:
		attr==3?Tables.METADATA.COL_ATTR3:
		attr==4?Tables.METADATA.COL_ATTR4:
		Tables.METADATA.COL_ATTR1;		
            Statement stmt = getDatabaseManager().select(
                                 SqlUtil.distinct(col),
                                 Tables.METADATA.NAME,
                                 Clause.eq(
                                     Tables.METADATA.COL_TYPE, type.getId()));
            values =
                SqlUtil.readString(getDatabaseManager().getIterator(stmt), 1);

            if (myDistinctMap != null) {
                myDistinctMap.put(key, values);
            }
        }

        return values;
    }

    public void insertMetadata(Metadata metadata) throws Exception {
        distinctMap = null;
        DatabaseManager dbm = getDatabaseManager();
        String          lbl = metadata.getType();
        dbm.executeInsert(Tables.METADATA.INSERT, new Object[] {
            metadata.getId(),
	    metadata.getEntryId(),
	    metadata.getType(),
            Integer.valueOf(metadata.getInherited()
                        ? 1
                        : 0),
	    metadata.getAccess(),
            dbm.checkString(lbl, metadata.getAttr1(), Metadata.MAX_LENGTH),
            dbm.checkString(lbl, metadata.getAttr2(), Metadata.MAX_LENGTH),
            dbm.checkString(lbl, metadata.getAttr3(), Metadata.MAX_LENGTH),
            dbm.checkString(lbl, metadata.getAttr4(), Metadata.MAX_LENGTH),
            dbm.checkString(lbl, metadata.getExtra(), Metadata.MAX_LENGTH_EXTRA),
        });
    }

    private Clause getClause(Metadata metadata) {
	Clause clause = Clause.eq(Tables.METADATA.COL_ID,
				  metadata.getId());
	if(metadata.getEntry()!=null) clause=Clause.and(clause,
							Clause.eq(Tables.METADATA.COL_ENTRY_ID,
								  metadata.getEntry().getId()));
	return clause;
    }

    public void deleteMetadata(Entry entry,Metadata metadata) throws Exception {
	System.err.println("deleteMetadata");
	MetadataType type = getType(metadata);
	if(type!=null) {
	    List<String> files = type.getFiles(entry,metadata);
	    for(String filename: files) {
		File f = new File(IOUtil.joinDir(getStorageManager().getEntryDir(metadata.getEntryId(), false), filename));
		//		System.err.println("deleting metadata file:" + f +" " + f.exists());
		if(f.exists())
		    getStorageManager().deleteFile(f);
	    }
	}

        getDatabaseManager().delete(Tables.METADATA.NAME,getClause(metadata));
        entry.clearMetadata();
    }

    public Metadata getSortOrderMetadata(Request request, Entry entry,boolean inherited)
            throws Exception {
        if (entry == null) {
            return null;
        }
        List<Metadata> metadataList =
            findMetadata(request, entry,
                         new String[] { ContentMetadataHandler.TYPE_SORT },
                         inherited);
        if ((metadataList != null) && (metadataList.size() > 0)) {
            return metadataList.get(0);
        }

        return null;
    }

}
