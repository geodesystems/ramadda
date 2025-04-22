/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.metadata;

import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.repository.auth.Role;
import org.ramadda.repository.auth.User;
import org.ramadda.util.FormInfo;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.ImageUtils;
import org.ramadda.util.sql.SqlUtil;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlUtil;

import java.awt.Image;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import java.net.URL;

import java.sql.Statement;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

@SuppressWarnings("unchecked")
public class MetadataType extends MetadataTypeBase implements Comparable {

    public static final String RESTRICTIONS_NONE = "none";
    public static final String RESTRICTIONS_ADMIN = "admin";
    public static final String RESTRICTIONS_USER = "user";
    public static final String TAG_TYPE = "type";
    public static final String TAG_TEMPLATE = "template";
    public static final String TAG_HANDLER = "handler";
    public static final String ATTR_METADATATYPE = "metadatatype";
    public static final String ATTR_CLASS = "class";
    public static final String ATTR_HANDLER = "handler";
    public static final String ATTR_ID = "id";
    public static final String ATTR_PRIORITY = "priority";
    public static final String ATTR_ADMINONLY = "adminonly";
    public static final String ATTR_FORUSER = "foruser";
    public static final String ATTR_ENTRYTYPE = "entrytype";
    public static final String ATTR_DISPLAYCATEGORY = "displaycategory";
    public static final String ATTR_CATEGORY = "category";
    public static final String ATTR_DISPLAYGROUP = "displaygroup";
    public static final String ATTR_BROWSABLE = "browsable";
    public static final String ATTR_ = "";
    public static final String PROP_METADATA_LABEL = "metadata.label";
    public static String ARG_METADATAID = "metadataid";
    public static String ARG_METADATA_ACCESS = "metadataaccess";    
    private String id;
    private int priority = 1000;
    private int textLengthLimit=400;
    private List<Column> databaseColumns;
    private String displayCategory = "Properties";
    private String displayGroup = null;
    private String tag = null;
    private String category = "Properties";
    private boolean adminOnly = false;
    private boolean isGeo = false;
    private List<String> restrictions;
    private boolean makeSearchLink = false;
    private boolean addNot = false;
    private boolean canView = true;
    private boolean canDisplay = true;
    private boolean showLabel = true;                
    private boolean browsable = false;
    private boolean forUser = true;
    private String entryType = null;
    private String help = "";
    private String tagStyle="";

    public MetadataType(String id, MetadataHandler handler) {
        super(handler);
        this.id = id;
    }

    /**
     *
     * @param o _more_
     *
     * @return _more_
     */
    public int compareTo(Object o) {
        boolean ok = o instanceof MetadataType;
        if ( !ok) {
            return -1;
        }

        return this.getName().compareTo(((MetadataType) o).getName());
    }

    @Override
    public int hashCode() {
	return id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
	if(!(o instanceof MetadataType)) return false;
	return id.equals(((MetadataType)o).id);
    }    

    public boolean isForEntry(Entry entry) {
        if (entryType != null) {
            return entry.getTypeHandler().isType(entryType);
        }

        return getHandler().isForEntry(entry);
    }

    public boolean hasFile() {
	return hasFile;
    }

    public String toString() {
        return id;
    }

    public static List<MetadataType> parse(Element root,
                                           MetadataManager manager)
	throws Exception {
        List<MetadataType> types = new ArrayList<MetadataType>();
        parse(root, manager, types);

        return types;
    }

    private static void parse(Element root, MetadataManager manager,
                              List<MetadataType> types)
	throws Exception {

        NodeList children = XmlUtil.getElements(root);
        if ((children.getLength() == 0)
	    && root.getTagName().equals(TAG_HANDLER)) {
            Class c = Misc.findClass(XmlUtil.getAttribute(root, ATTR_CLASS));
            MetadataHandler handler = manager.getHandler(c);
        }

        for (int i = 0; i < children.getLength(); i++) {
            Element node = (Element) children.item(i);
            if (node.getTagName().equals(TAG_HANDLER)) {
                parse(node, manager, types);
                continue;
            }

            if (node.getTagName().equals(TAG_TEMPLATE)) {
                String templateType = XmlUtil.getAttribute(node, ATTR_TYPE);
                String metadataType = XmlUtil.getAttribute(node,
							   ATTR_METADATATYPE);

                if (XmlUtil.hasAttribute(node, ATTR_FILE)) {
                    manager.addTemplate(
					metadataType, templateType,
					manager.getStorageManager().readSystemResource(
										       XmlUtil.getAttribute(node, ATTR_FILE)));

                } else {
                    manager.addTemplate(metadataType, templateType,
                                        XmlUtil.getChildText(node));
                }

                continue;
            }

            if (node.getTagName().equals(ATTR_HELP)) {
                continue;
            }

            if ( !node.getTagName().equals(TAG_TYPE)) {
                manager.logError("Unknown metadata xml tag:"
                                 + XmlUtil.toString(node), null);
            }

            Class c = Misc.findClass(XmlUtil.getAttributeFromTree(node,
								  ATTR_CLASS,
								  "org.ramadda.repository.metadata.MetadataHandler"));

            String          id           = XmlUtil.getAttribute(node, ATTR_ID);

            MetadataHandler handler      = manager.getHandler(c);

            MetadataType    metadataType = new MetadataType(id, handler);
            metadataType.help = Utils.getAttributeOrTag(node, ATTR_HELP, "");
            metadataType.tagStyle = Utils.getAttributeOrTag(node, "tagstyle","");
            metadataType.init(node);
	    //Is this type ok
	    if(!manager.metadataTypeOk(metadataType)) {
		continue;
	    }

            handler.addMetadataType(metadataType);
            types.add(metadataType);
	}
    }

    public void init(Element node) throws Exception {
        super.init(node);
        setAdminOnly(XmlUtil.getAttributeFromTree(node, ATTR_ADMINONLY,  false));
        isGeo = XmlUtil.getAttributeFromTree(node, "isgeo",false);
	restrictions=Utils.split(XmlUtil.getAttributeFromTree(node,"restrictions",RESTRICTIONS_NONE),",",true,true);
	canView = XmlUtil.getAttributeFromTree(node, "canview", true);
	addNot = XmlUtil.getAttributeFromTree(node, "addnot", false);
	makeSearchLink = XmlUtil.getAttributeFromTree(node, "makesearchlink", false);
	canDisplay = XmlUtil.getAttributeFromTree(node, "candisplay", true);    	
	showLabel = XmlUtil.getAttributeFromTree(node, "showlabel", true);    	
        setForUser(XmlUtil.getAttributeFromTree(node, ATTR_FORUSER, true));
        entryType = XmlUtil.getAttributeFromTree(node, ATTR_ENTRYTYPE,
						 (String) null);

        priority = XmlUtil.getAttributeFromTree(node, ATTR_PRIORITY,
						priority);

	textLengthLimit= XmlUtil.getAttributeFromTree(node, "textLengthLimit",textLengthLimit);
        setBrowsable(XmlUtil.getAttributeFromTree(node, ATTR_BROWSABLE,
						  false));

        setDisplayCategory(XmlUtil.getAttributeFromTree(node,
							ATTR_DISPLAYCATEGORY, "Properties"));

	tag = XmlUtil.getAttributeFromTree(node, "tag",  (String) null);

        setDisplayGroup(XmlUtil.getAttributeFromTree(node, ATTR_DISPLAYGROUP,
						     (String) null));
        setCategory(XmlUtil.getAttributeFromTree(node, ATTR_CATEGORY,
						 handler.getHandlerGroupName()));

    }

    /**  */
    public static final String TABLE_NAME_PREFIX = "metadata_";

    /**
     *
     * @return _more_
     */
    public String getDbTableName() {
        String id = SqlUtil.cleanName(this.id);

        return TABLE_NAME_PREFIX + id;
    }

    /**
     *
     * @param name _more_
     *
     * @return _more_
     */
    public String getDbColumnName(String name) {
        name = SqlUtil.cleanName(name);
        if (name.equals("type")) {
            name = "column_type";
        } else if (name.equals("output")) {
            name = "column_output";
        }

        return name;
    }

    public void initNewEntry(Metadata metadata, Entry entry,
                             EntryInitializer initializer)
	throws Exception {
        for (MetadataElement element : getChildren()) {
            if (element.getDataType().equals(element.DATATYPE_FILE)) {
                String fileArg = metadata.getAttr(element.getIndex());
                if ((fileArg == null) || (fileArg.length() == 0)) {
                    continue;
                }
                File sourceFile = new File(fileArg);
                if ( !sourceFile.exists()) {
                    if (initializer != null) {
                        sourceFile = initializer.getMetadataFile(entry,fileArg);
                    }
                }
                if ((sourceFile == null) || !sourceFile.exists()) {
		    continue;
		    //Don't error off here
		    //                    throw new IllegalArgumentException("Missing metadata file:" + fileArg);
                }
                if ( !entry.getIsLocalFile()) {
                    fileArg = getStorageManager().copyToEntryDir(entry,
								 sourceFile).getName();
                }
                metadata.setAttr(element.getIndex(), fileArg);
            }
        }
    }

    public boolean processMetadataXml(Entry entry, Element node,
                                      Metadata metadata, Hashtable filesMap,
                                      EntryManager.INTERNAL isInternal)
	throws Exception {

        //don't check internal as I forgot what its intent was and it is keeping us from processing attachments
        //      if(!internal || entry.getIsRemoteEntry()) {
        if (entry.getIsRemoteEntry()) {
            //      System.err.println("\tinternal: " + internal  +" "  + entry.getIsRemoteEntry());
            return true;
        }
        NodeList elements = XmlUtil.getElements(node);
        for (MetadataElement element : getChildren()) {
            if ( !element.getDataType().equals(element.DATATYPE_FILE)) {
                continue;
            }
            String fileArg = null;
            Element attrNode = XmlUtil.findElement(elements,
						   Metadata.ATTR_INDEX,
						   "" + element.getIndex());
            if (attrNode == null) {
                fileArg = XmlUtil.getAttribute(node,
					       "attr" + element.getIndex(), (String) null);
                if (fileArg == null) {
                    System.err.println("Could not find attr node:"
                                       + XmlUtil.toString(node));

                    continue;
                }
            } else {
                fileArg = XmlUtil.getAttribute(attrNode, "fileid",
					       (String) null);
            }

            if (fileArg == null) {
                fileArg = metadata.getAttr(element.getIndex());
            }

            if (fileArg == null) {
                System.err.println(
				   "Metadata: Could not find file id for entry: "
				   + entry.getName() + " attr: "
				   + metadata.getAttr(element.getIndex()) + " files:"
				   + filesMap);

                continue;
            }

            fileArg = fileArg.trim();
            if (fileArg.length() == 0) {
                continue;
            }
            String fileName = null;
            File   tmpFile  = (File) filesMap.get(fileArg);
            if (tmpFile == null) {
                File root = (File) filesMap.get("root");
                if (root != null) {
                    File tmp = new File(root, fileArg);
                    if ( !tmp.exists()) {
                        tmp = new File(root, "." + fileArg);
                    }
                    if (tmp.exists()) {
                        tmpFile = tmp;
                    }
                }
            }

            if ((tmpFile == null) && isInternal==EntryManager.INTERNAL.YES) {
                tmpFile = new File(fileArg);
            }

            if (tmpFile == null) {
                try {
                    //See if its a URL
                    URL testUrl = new URL(fileArg);
                    continue;
                } catch (Exception ignore) {
                    return false;
                }
            }

	    File newFile=null;
            if (IO.exists(tmpFile)) {
		//If it is a template or remote then don't do anything with the file
		if(isInternal==EntryManager.INTERNAL.NO) {
		    metadata.setAttr(element.getIndex(), tmpFile.toString());
		    return true;
		} 

                File file = new File(tmpFile.toString());
                newFile = getStorageManager().copyToEntryDir(entry, file,
							     metadata.getAttr(element.getIndex()));
            } 
            metadata.setAttr(element.getIndex(), newFile==null?"":newFile.getName());
        }

        return true;

    }

    public Metadata handleForm(Request request, Entry entry, String id,
                               String suffix, Metadata oldMetadata,
                               boolean newMetadata)
	throws Exception {
        boolean inherited = request.get(ARG_METADATA_INHERITED + suffix,
                                        false);
        Metadata metadata = new Metadata(id, entry.getId(), this, inherited);
	metadata.setAccess(request.getString(ARG_METADATA_ACCESS+suffix,""));
        for (MetadataElement element : getChildren()) {
            String value = element.handleForm(request, entry, metadata,
					      oldMetadata, suffix);
            metadata.setAttr(element.getIndex(), value);
        }

        return metadata;
    }

    public boolean addMetadataToXml(Request request, String templateType,
                                    Entry entry, Metadata metadata,
                                    Element parent)
	throws Exception {

	if(!this.getCanView()) return false;

        String xml = applyTemplate(request, templateType, entry, metadata,
                                   parent);
        if ((xml == null) || (xml.length() == 0)) {
            return false;
        }
        xml = "<tmp>" + xml + "</tmp>";
        Element root = null;
        try {
	    long t1= System.currentTimeMillis();
            root = XmlUtil.getRoot(new ByteArrayInputStream(xml.getBytes()));
	    long t2= System.currentTimeMillis();
	    //	    System.err.println("time:" + (t2-t1));
        } catch (Exception exc) {
            throw new IllegalStateException("XML Error:" + exc
                                            + "\nCould not create xml:"
                                            + xml);
        }
        if (root == null) {
            throw new IllegalStateException("Could not create xml:" + xml);
        }
        NodeList children = XmlUtil.getElements(root);
        for (int i = 0; i < children.getLength(); i++) {
            Element node = (Element) children.item(i);
            node = (Element) parent.getOwnerDocument().importNode(node, true);
            parent.appendChild(node);
        }

        return true;
    }

    public void decorateEntry(Request request, Entry entry, Appendable sb,
                              Metadata metadata, boolean forLink,boolean fileOk)
	throws Exception {
        decorateEntry(request, entry, sb, metadata, forLink, false,fileOk);
    }

    public void decorateEntry(Request request, Entry entry, Appendable sb,
                              Metadata metadata, boolean forLink,
                              boolean isThumbnail,boolean fileOk)
	throws Exception {
        for (MetadataElement element : getChildren()) {
            if ( !element.getDataType().equals(element.DATATYPE_FILE)) {
                continue;
            }
            if ( !element.showAsAttachment()) {
                continue;
            }
            if (element.getThumbnail() || isThumbnail) {
		if(!fileOk) continue;
                String html = getFileHtml(request, entry, metadata, element,
                                          forLink);
                if (html != null) {
                    sb.append(HU.space(1));
                    sb.append(html);
                    sb.append(HU.space(1));
                } else {
                    String value = metadata.getAttr(element.getIndex());
                    if ((value != null) && value.startsWith("http")) {
                        sb.append(HU.space(1));
                        sb.append(HU.img(value, "",
					 HU.attr("loading", "lazy")));
                        sb.append(HU.space(1));
                    }
                }

                continue;
            }
            if ( !forLink) {
                String html = getFileHtml(request, entry, metadata, element,
                                          false);
                if (html != null) {
                    sb.append(HU.space(1));
                    sb.append(html);
                    sb.append(HU.space(1));
                }
            }
        }
    }

    public void initializeCopiedMetadata(Entry oldEntry, Entry newEntry,
                                         Metadata newMetadata)
	throws Exception {
        for (MetadataElement element : getChildren()) {
            if ( !element.getDataType().equals(element.DATATYPE_FILE)) {
                continue;
            }
            String oldFileName = newMetadata.getAttr(element.getIndex());
            String newFileName = getStorageManager().copyToEntryDir(oldEntry,
								    newEntry, oldFileName);
            newMetadata.setAttr(element.getIndex(), newFileName);
        }
    }

    public void getThumbnailUrls(Request request, Entry entry,
                                 List<String[]> urls, Metadata metadata)
	throws Exception {
	List<MetadataElement> elements =getChildren();
        for (MetadataElement element : elements) {
            if ( !element.getDataType().equals(element.DATATYPE_FILE)) {
                continue;
            }
            if (!element.showAsAttachment()) {
                continue;
            }
            if (element.getThumbnail()) {
                String url = getFileUrl(request, entry, metadata, true,null);
                if (url != null) {
		    String title = "";
		    //Look for the string title
		    for(MetadataElement titleElement: elements) {
			if ( !titleElement.getDataType().equals(element.DATATYPE_STRING)) {
			    continue;
			}
			title=metadata.getAttr(titleElement.getIndex());
			break;
		    }
		    String primary=metadata.getAttr3();
		    if(stringDefined(primary) && primary.equals("true")) {
			urls.add(0,new String[]{url,title});
		    } else {
			urls.add(new String[]{url,title});
		    }
                }
            }
        }
    }

    public String getDisplayImageUrl(Request request, Entry entry,
                                     Metadata metadata, String filter)
	throws Exception {
        for (MetadataElement element : getChildren()) {
            if ( !element.getDataType().equals(element.DATATYPE_FILE)) {
                continue;
            }
            if ( !element.showAsAttachment()) {
                continue;
            }
            if (element.getThumbnail()) {
                //???                continue;
            }
            String url = getFileUrl(request, entry, metadata, true, filter);
            if (url != null) {
                return url;
            }
        }

        return null;
    }

    public MetadataElement getDisplayImageElement(Request request,
						  Entry entry, Metadata metadata, String filter)
	throws Exception {
        for (MetadataElement element : getChildren()) {
            if ( !element.getDataType().equals(element.DATATYPE_FILE)) {
                continue;
            }
            if ( !element.showAsAttachment()) {
                continue;
            }
            if (element.getThumbnail()) {
                //???                continue;
            }
            String url = getFileUrl(request, entry, metadata, true, filter);
            if (url != null) {
                return element;
            }
        }

        return null;
    }

    public Result processView(Request request, Entry entry, Metadata metadata)
	throws Exception {
        int elementIndex = request.get(ARG_ELEMENT, 0) - 1;
        if ((elementIndex < 0) || (elementIndex >= getChildren().size())) {
            return new Result("", "Cannot process view");
        }
        MetadataElement element = getChildren().get(elementIndex);

        return processView(request, entry, metadata, element);
    }

    public Result processView(Request request, Entry entry,
                              Metadata metadata, MetadataElement element)
	throws Exception {
        if ( !element.getDataType().equals(element.DATATYPE_FILE)) {
            return new Result("", "Cannot process view");
        }
        File f = getFile(entry, metadata, element);
        if (f == null) {
            return new Result("", "File does not exist");
        }
        String mimeType = handler.getRepository().getMimeTypeFromSuffix(f.toString());

        if (false && request.get(ARG_THUMBNAIL, false)) {
            File thumb = getStorageManager().getTmpFile(IO.getFileTail(f.toString()));
            if ( !thumb.exists()) {
                try {
                    Image image = ImageUtils.readImage(f.toString());
                    image = ImageUtils.resize(image, 100, -1);
                    ImageUtils.waitOnImage(image);
                    ImageUtils.writeImageToFile(image, thumb.toString());
                    f = thumb;
                } catch (Exception exc) {
                    getStorageManager().logError(
						 "Error creating thumbnail from file:" + f + " error:"
						 + exc, exc);
                }
            } else {
                f = thumb;
            }
        }

        InputStream inputStream = getStorageManager().getFileInputStream(f);
        Result      result      = new Result(inputStream, mimeType);
        result.setCacheOk(true);

        return result;
    }

    public String getSearchUrl(Request request, Metadata metadata) {
        if ( !getSearchable()) {
            return null;
        }

        List args = new ArrayList();
        args.add(ARG_METADATA_TYPE + "_" + getId());
        args.add(this.toString());

        for (MetadataElement element : getChildren()) {
            if ( !element.getSearchable()) {
                continue;
            }
            args.add(ARG_METADATA_ATTR + element.getIndex() + "_" + getId());
            args.add(metadata.getAttr(element.getIndex()));
        }

        //by default search on attr1 if none are set above
        if (args.size() == 2) {
            args.add(ARG_METADATA_ATTR1 + "_" + getId());
            args.add(metadata.getAttr1());
        }

        for (Object o : args) {
            if (o == null) {
                System.err.println("NULL: " + args);

                return null;
            }
        }

        try {
            return HU
                .url(request
		     .makeUrl(handler.getRepository().getSearchManager()
			      .URL_ENTRY_SEARCH), args);
        } catch (Exception exc) {
            System.err.println("ARGS:" + args);

            throw new RuntimeException(exc);
        }
    }

    public void getTextCorpus(Entry entry, Appendable sb, Metadata metadata)
	throws Exception {
        for (MetadataElement element : getChildren()) {
            String value = metadata.getAttr(element.getIndex());
            element.getTextCorpus(value, sb);
        }
    }

    public String getTemplate(String type) {
        String template = super.getTemplate(type);
        if (template != null) {
            return template;
        }

        return getMetadataManager().getTemplate(id, type);
    }

    public String getTypeLabel(Metadata metadata) throws Exception {
        String nameString = getName();
        for (MetadataElement element : getChildren()) {
            String value = metadata.getAttr(element.getIndex());
            if (value == null) {
                value = "";
            }
            nameString = nameString.replace("${attr" + element.getIndex()
                                            + "}", value);
        }

        return nameString;
    }

    public boolean isPrivate(Request request, Entry entry,Metadata metadata) {
	//Check for the missing type
	if(restrictions==null || restrictions.size()==0) {
	    return false;
	}

	if(request.isAdmin()) return false;
	if(isGeo && !request.geoOk(entry)) {
	    return true;
	}
	if(restrictions.contains(RESTRICTIONS_ADMIN) && !request.isAdmin()) {
	    return false;
	}
	if(restrictions.contains(RESTRICTIONS_USER) && request.isAnonymous()) {
	    return true;
	}
	List<Role> list = metadata.getAccessList();
	boolean debug = false;
	//	debug=true;
	if(list!=null && list.size()>0) {
	    if(debug)
		System.err.println("has roles:" + metadata.getAttr1());
	    User user = request.getUser();
	    for(Role role: list) {
		//		if(debug)    System.err.println("\trole:"+ role+" is:" + user.isRole(role));
		if(user.isRole(role)) {
		    if(debug)
			System.err.println("\tis role:" + metadata.getAttr1());
		    return false;
		}
	    }
	    if(debug)
		System.err.println("\t***  not ok");
	    return true;
	}
	return false;
    }

    public String[] getHtml(Request request, Entry entry, Metadata metadata)
	throws Exception {
        if ( !getShowInHtml()) {
            return null;
        }
	if(isPrivate(request, entry,metadata)) {
	    return null;
	}
	Hashtable props =
	    (Hashtable) request.getExtraProperty("wiki.props");

        StringBuffer content = new StringBuffer();
        boolean smallDisplay = request.getString(ARG_DISPLAY,
						 "").equals(DISPLAY_SMALL);
        String nameString   = getTypeLabel(metadata);
        String searchLink = "";
        String lbl          = !showLabel?"":
	    smallDisplay
	    ? msg(nameString)
	    : msgLabel(nameString);
	boolean showLabel = Utils.getProperty(props,"showLabel",true);

	List<MetadataElement> children = getChildren();
	boolean makeSearchLink =  !smallDisplay && (this.makeSearchLink ||getSearchable()) && children.size()>=1;
	if(makeSearchLink) {
	    if(children.size()==1) {
		MetadataElement element= children.get(0);
		if(element.isEnumeration() || element.isBoolean()) makeSearchLink = true;
		else makeSearchLink = this.makeSearchLink;
	    } else  {
		makeSearchLink = this.makeSearchLink;
	    }
	}

	makeSearchLink = Utils.getProperty(props,"addLink",makeSearchLink);

	String html = applyTemplate(request, TEMPLATETYPE_HTML,entry,metadata,null);
	int lengthLimit = Utils.getProperty(props,"textLengthLimit",textLengthLimit);
        if (html!=null) {
	    if(makeSearchLink) {
		html= handler.getSearchLink(request, metadata,html);
	    }
	    if(html.length()>lengthLimit) {
		if(Utils.getProperty(props,"checkTextLength",true)) {
		    html = HU.div(html,HU.cssClass("ramadda-bigtext"));
		}
	    }
            content.append(html);
        } else {
            int                   cnt      = 1;
            boolean               didOne   = false;
            content.append("<table>");
	    //            content.append(HU.formTable());
            for (MetadataElement element : children) {
                MetadataElement.MetadataHtml formInfo =
                    element.getHtml(request, entry, this, metadata,
                                    metadata.getAttr(cnt), 0);
                if (formInfo != null) {
                    String metadataHtml = formInfo.content;
		    if ((cnt > 1) && !Utils.stringDefined(metadataHtml)) {
                        cnt++;
                        continue;
                    }
		    if(cnt==1 && makeSearchLink) {
			metadataHtml = handler.getSearchLink(request, metadata,metadataHtml);
		    }

		    if(metadataHtml.length()>lengthLimit) {
			if(Utils.getProperty(props,"checkTextLength",true)) {
			    metadataHtml = HU.div(metadataHtml,HU.cssClass("ramadda-bigtext"));
			}
		    }
                    if (!showLabel || (!element.isGroup() && (children.size() == 1))) {
                        content.append(HU.row(HU.colspan(metadataHtml, 2)));
                    } else {
                        content.append(HU.formEntry(formInfo.label,  metadataHtml));
                    }
                    didOne = true;
                }
                cnt++;
                content.append("\n");
            }
            content.append("</table>");
	    //            content.append(HU.formTableClose());
            if ( !didOne) {
                return null;
            }
        }
        return new String[] { lbl, content.toString() };
    }

    @Override
    public String applyMacros(String template, MetadataElement element,
                              String value) {
        template = super.applyMacros(template, element, value);
        if (template != null) {
            while (template.indexOf("${id}") >= 0) {
                template = template.replace("${id}", id);
            }
        }

        return template;
    }

    public String getHelp() {
        return help;
    }

    public String[] getForm(MetadataHandler handler, Request request,
                            FormInfo formInfo, Entry entry,
                            Metadata metadata, String suffix, boolean forEdit)
	throws Exception {

        String lbl = (String) request.getExtraProperty(PROP_METADATA_LABEL);
        String firstValue = null;
	List<String> titles = new ArrayList<String>();
        if (lbl == null) {
            lbl = msgLabel(getName());
        }
        String submit = HU.submit("Add" + HU.space(1)
				  + getName());
        String        cancel = HU.submit(LABEL_CANCEL, ARG_CANCEL);
        StringBuilder sb     = new StringBuilder();
        if (Utils.stringDefined(help)) {
	    String _help = HU.div(help,HU.clazz("ramadda-form-help"));
	    sb.append(HU.row(HU.colspan(_help, 3)));
            sb.append("\n");
        }

        if ( !forEdit) {
            //            sb.append(header(msgLabel("Add") + getName()));
        }
	String clazz = 	"ramadda-metadata-widget ramadda-metadata-widget-"+HU.makeCssClass(id);

        String lastGroup = null;
        for (MetadataElement element : getChildren()) {
            if (forEdit && (firstValue == null || element.getIsTitle())) {
		String attr = metadata.getAttr(element.getIndex());
                MetadataElement.MetadataHtml metadataHtml =
                    element.getHtml(request, entry, this, metadata,   attr, 0);
                if (metadataHtml != null) {
		    if(firstValue==null)
			firstValue = metadataHtml.getHtml();
		} 
		if(element.getIsTitle()) {
		    if(element.isFileType())
			attr = getStorageManager().getFileTail(attr);
		    titles.add(attr);
		}		    
            }

            if ((element.getGroup() != null)
		&& !Misc.equals(element.getGroup(), lastGroup)) {
                lastGroup = element.getGroup();
                sb.append(HU.row(HU.colspan(header(lastGroup),
					    2)));
            }
            String elementLbl = msgLabel(element.getLabel());
            String widget =
                element.getForm(request, entry, formInfo, metadata, suffix,
                                metadata.getAttr(element.getIndex()),
                                forEdit);
            if ((widget == null) || (widget.length() == 0)) {}
            else {
                String suffixLabel = element.getSuffixLabel();
                if (suffixLabel == null) {
                    suffixLabel = "";
                }
		suffixLabel = HU.span(suffixLabel,HU.attrs("class","ramadda-metadata-widget-suffix"));
                sb.append(HU.formEntryTop(elementLbl, HU.span(widget+ suffixLabel,HU.cssClass(clazz))
					  ));
            }
        }

        sb.append("\n");
        sb.append( HU.formEntry("",
				HU.labeledCheckbox(
						   ARG_METADATA_INHERITED + suffix, "true",
						   metadata.getInherited(),"Inherited")));

	String msg ="<br>Comma separated list - e.g.: <i>admin</i>, <i>user</i>, <i>user:&lt;user id&gt;, <i>&lt;userrole&gt;</i>";
	msg+=" " +HU.href(getRepository().getUrlBase()+"/userguide/editing.html#property_access","Help",
			  HU.attrs("target","_help"));
	sb.append(HU.formEntry(msgLabel("Permissions"),
			       HU.input(ARG_METADATA_ACCESS+suffix,metadata.getAccess(),HU.attrs("size","40")) +
			       msg));

        String argtype = ARG_METADATA_TYPE + suffix;
        String argid   = ARG_METADATAID + suffix;
        sb.append(HU.hidden(argtype, getId())
                  + HU.hidden(argid, metadata.getId()));

        if ( !forEdit && (entry != null)) {
            sb.append("\n");
            sb.append(HU.formEntry("",
				   submit + HU.buttonSpace()
				   + cancel));
        }

        //Only show the value if its simple text
	if(showLabel) {
	    if(titles.size()>0) {
		lbl = lbl + " " + Utils.join(titles," - ");
	    } else {
		if ((firstValue != null) && (firstValue.indexOf("<") < 0)) {
		    lbl = lbl + " " + firstValue;
		}
	    }
	}

        return new String[] { lbl, sb.toString() };
    }

    public List<String> getFiles(Entry entry, Metadata metadata)
	throws Exception {
	List<String> files = new ArrayList<String>();
        for (MetadataElement element : getChildren()) {
	    if(element.isFileType())
		files.add(metadata.getAttr(element.getIndex()));
	}
	return files;
    }

    public boolean isType(String id) {
        return Misc.equals(this.id, id);
    }

    /**
     *  Get the ID property.
     *
     *  @return The Id
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     *  Set the Category property.
     *
     *  @param value The new value for Category
     */
    public void setCategory(String value) {
        category = value;
    }

    /**
     *  Get the Category property.
     *
     *  @return The Category
     */
    public String getCategory() {
        return category;
    }

    /**
     *  Set the DisplayCategory property.
     *
     *  @param value The new value for DisplayCategory
     */
    public void setDisplayCategory(String value) {
        this.displayCategory = value;
    }

    /**
     *  Get the DisplayCategory property.
     *
     *  @return The DisplayCategory
     */
    public String getDisplayCategory() {
        return this.displayCategory;
    }

    public String getTag() {
	return tag;
    }

    /**
     *  Set the DisplayGroup property.
     *
     *  @param value The new value for DisplayGroup
     */
    public void setDisplayGroup(String value) {
        this.displayGroup = value;
    }

    /**
     *  Get the DisplayGroup property.
     *
     *  @return The DisplayGroup
     */
    public String getDisplayGroup() {
        if (displayGroup == null) {
            return getName();
        }

        return displayGroup;
    }

    /**
     *  Set the AdminOnly property.
     *
     *  @param value The new value for AdminOnly
     */
    public void setAdminOnly(boolean value) {
        this.adminOnly = value;
    }

    /**
     *  Get the AdminOnly property.
     *
     *  @return The AdminOnly
     */
    public boolean getAdminOnly() {
        return this.adminOnly;
    }

    /**
     * Set the Browsable property.
     *
     * @param value The new value for Browsable
     */
    public void setBrowsable(boolean value) {
        this.browsable = value;
    }

    /**
     * Get the Browsable property.
     *
     * @return The Browsable
     */
    public boolean getBrowsable() {
        return this.browsable;
    }

    public boolean getCanView() {
        return this.canView;
    }
    public boolean getAddNot() {
        return this.addNot;
    }    

    public boolean getCanDisplay() {
        return this.canDisplay;
    }

    public boolean getShowLabel() {
        return this.showLabel;
    }        

    /**
     * Set the ForUser property.
     *
     * @param value The new value for ForUser
     */
    public void setForUser(boolean value) {
        this.forUser = value;
    }

    /**
     * Get the ForUser property.
     *
     * @return The ForUser
     */
    public boolean getForUser() {
        return this.forUser;
    }

    public String getTagStyle() {
	return tagStyle;
    }

    public int getPriority() {
        return priority;
    }

    public static class Checker {
        List<String> onlyTypes = null;
        List<String> notTypes  = null;
        List<String> onlyTags = null;
	List<String> notTags = null;	
	public Checker(String types) {
	    if (types != null) {
		for (String type :Utils.split(types, ",", true, true)) {
		    boolean negate = false;
		    boolean tag=false;
		    if (type.startsWith("!")) {
			type=type.substring(1);
			negate=true;
		    }
		    if (type.startsWith("tag:")) {
			type=type.substring("tag:".length());
			tag=true;
		    }		    
		    if(tag) {
			if(negate) notTags = add(notTags,type);
			else onlyTags = add(onlyTags,type);
		    } else {
			if(negate) notTypes = add(notTypes,type);
			else onlyTypes = add(onlyTypes,type);
		    }
		}
	    }
	}

	private List<String> add(List<String> l,String t) {
	    if(l==null) l=new ArrayList<String>();
	    l.add(t);
	    return l;
	}

	public boolean typeOk(MetadataType type) {
	    if(type==null) return true;
            if (onlyTags != null) {
		String tag = type.getTag();
		if(tag==null) return false;
		if(!onlyTags.contains(tag)) return false;
	    }
            if (notTags != null) {
		String tag = type.getTag();
		if(tag!=null && notTags.contains(tag)) return false;
	    }	    

            if (onlyTypes != null) {
                if (!onlyTypes.contains(type.getId())) {
		    return false;
                }
            }

            if (notTypes != null) {
                if (notTypes.contains(type.getId())) {
		    return false;
                }
            }

	    return true;
	}

    }

}
