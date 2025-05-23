/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.metadata;

import org.ramadda.repository.*;

import org.ramadda.util.IO;
import org.ramadda.util.NamedValue;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.awt.Image;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import java.net.URL;
import java.net.URLConnection;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public class MetadataTypeBase extends RepositoryManager {

    private SimpleDateFormat sdf;

    public static final String TAG_TYPE = "type";

    public static final String TAG_ELEMENT = "element";

    public static final String TAG_TEMPLATE = "template";

    public static String ATTR_HELP = "help";

    public static final String ATTR_FILE = "file";

    public static final String ATTR_TAG = "tag";

    public static final String ATTR_TYPE = "type";

    public static final String ATTR_NAME = "name";

    public static final String ATTR_LABEL = "label";

    public static final String ATTR_SUFFIX = "suffix";

    public static final String ATTR_ENTRYTYPE = "entrytype";    

    public static final String ATTR_SEARCHABLE = "searchable";

    public static final String ATTR_SHOWINHTML = "showinhtml";

    public static final String TEMPLATETYPE_THREDDS = "thredds";

    public static final String TEMPLATETYPE_DIF = "dif";

    public static final String TEMPLATETYPE_ISO = "iso";

    public static final String TEMPLATETYPE_OAIDC = "oai_dc";

    public static final String TEMPLATETYPE_HTML = "html";

    private String name;

    private String label;

    private String suffixLabel;

    protected String entryType;

    private boolean showInHtml = true;

    private boolean isTitle = false;

    protected boolean hasFile = false;

    private Hashtable<String, String> templates = new Hashtable<String,
                                                      String>();

    List<MetadataElement> children = new ArrayList<MetadataElement>();

    private Hashtable<String,MetadataElement> map;

    MetadataHandler handler;

    private boolean searchable = false;

    private Hashtable<String, String> tags = new Hashtable<String, String>();

    public MetadataTypeBase(MetadataHandler handler) {
        super(handler.getRepository());
        this.handler = handler;
    }

    public String getId() {
        return name;
    }

    /**
     *  Set the Handler property.
     *
     *  @param value The new value for Handler
     */
    public void setHandler(MetadataHandler value) {
        this.handler = value;
    }

    /**
     *  Get the Handler property.
     *
     *  @return The Handler
     */
    public MetadataHandler getHandler() {
        return this.handler;
    }

    public String toString() {
        return name;
    }

    protected void checkFileXml(Request request, String templateType,
                                Entry entry, Metadata metadata,
                                Element parent)
            throws Exception {
        for (MetadataElement element : getChildren()) {
            if ( !element.getDataType().equals(element.TYPE_FILE)) {
                continue;
            }
            File f = getFile(entry, metadata, element);
            if (f == null) {
                continue;
            }
            String tail = getStorageManager().getFileTail(f.toString());
            String path =
                request.getAbsoluteUrl(handler.getRepository()
                    .getMetadataManager().URL_METADATA_VIEW) + "/" + tail;

            String url = HU.url(path, ARG_ELEMENT,
                                       element.getIndex() + "", ARG_ENTRYID,
                                       metadata.getEntryId(),
                                       ARG_METADATA_ID, metadata.getId());
            //TODO:
            if (templateType.equals(TEMPLATETYPE_THREDDS)) {
                XmlUtil.create(parent.getOwnerDocument(), "property", parent,
                               new String[] { "name", (element.getThumbnail()
                        ? "thumbnail"
                        : "attachment"), "value", url });
            }

        }
    }

    public String applyTemplate(Request request, String templateType,
                                Entry entry, Metadata metadata,
                                Element parent)
            throws Exception {
        checkFileXml(request, templateType, entry, metadata, parent);
        String template = getTemplate(templateType);
        if ((template == null) || (template.length() == 0)) {
            return null;
        }
        //Remove newlines??
        template = template.replaceAll("\n", "");
        template = getRepository().getPageHandler().applyBaseMacros(template);

        template = template.replace("${entry.id}", entry.getId());
        template = template.replace("${entry.name}", entry.getName());
        template = template.replace("${entry.name.cdata}",
                                    wrapCdata(entry.getName()));
        template = template.replace("${entry.description}",
                                    entry.getDescription());
        template = template.replace("${entry.description.cdata}",
                                    wrapCdata(entry.getDescription()));
        template = template.replace("${entry.publishdate}",
                                    formatDate(entry.getCreateDate()));
        template = template.replace("${entry.changedate}",
                                    formatDate(entry.getChangeDate()));

	StringBuilder sb = new StringBuilder();
	for(Utils.Macro macro: Utils.splitMacros(template)) {
	    if(macro.isText()) {
		sb.append(macro.getText());
		continue;
	    } 
	    MetadataElement element = getElement(macro.getId());
	    if(element==null) {
		System.err.println("Unknown metadata element id:" + macro.getId() +" in template:" + template);
		continue;
	    }
	    String value = metadata.getAttr(element.getIndex());
	    if(macro.getProperty("uselabel",false)) value = element.getLabel();
	    if(macro.getProperty("skipempty",false) && !stringDefined(value)) continue;
	    if(value==null) value="";
	    value = getRepository().getPageHandler().applyBaseMacros(value);		
	    value = value.replaceAll("[\\r\\n]+", " ").replaceAll("&","&amp;").replaceAll("<","&lt;").replaceAll(">","&gt;").replaceAll("\"", "&quot;");

	    if(macro.getProperty("encoded", false)) value = XmlUtil.encodeString(value);
	    if(macro.getProperty("cdata", false)) value = wrapCdata(value);
	    boolean addSearch = macro.getProperty("addSearch",false);
	    String prefix = macro.getProperty("prefix","");
	    String suffix = macro.getProperty("suffix","");		
	    if(addSearch) {
		value = HU.href(getHandler().getSearchUrl(request,  metadata.getMetadataType(),value),value);
	    }
	    if(prefix!=null) sb.append(prefix);
	    sb.append(value);
	    if(suffix!=null) sb.append(suffix);		
	}
	return sb.toString();
    }

    public String wrapCdata(String s) {
        return "<![CDATA[" + s + "]]>";
    }

    public String applyMacros(String template, MetadataElement element,
                              String value) {
        if (value == null) {
            value = "";
        }
        //        value = XmlUtil.encodeString(value);
        value = getRepository().getPageHandler().applyBaseMacros(value);
        value = value.replaceAll("[\\r\\n]+", " ").replaceAll("&",
                                 "&amp;").replaceAll("<",
                                     "&lt;").replaceAll(">",
                                         "&gt;").replaceAll("\"", "&quot;");

        //TODO: make faster

        String   label = element.getLabel(value);
        String   name  = element.getName();
        String[] keys  = {
            "attr" + element.getIndex(), name, name.toLowerCase(),
            name.replace(" ", "_"), name.toLowerCase().replace(" ", "_"),
            element.getId(),
        };
        Hashtable macros = new Hashtable();
        macros.put("name", name);
        //      template = template.replaceAll("\\${name}", name);

        for (String key : keys) {
            if (key == null) {
                continue;
            }
            //                System.err.println("key: " + key);
            //            template = template.replaceAll("\\${" + key + "}", value);
            //            template = template.replaceAll("\\${" + key + ".label}", label);
            //            template = template.replaceAll("\\${" + key + ".cdata}",
            //                                        "<![CDATA[" + value + "]]>");
            macros.put(key, value);
            macros.put(key + ".label", label);
            macros.put(key + ".cdata", wrapCdata(value));
            macros.put(key + ".encoded", XmlUtil.encodeString(value));
        }

        //        System.err.println ("Template:" + template);
        //        System.err.println ("Macros:" + macros);

        template = StringUtil.applyMacros(template, macros, false);
        template = template.replaceAll("\r\n\r\n", "<p>");
        template = template.replace("\n\n", "<p>");

        return template;
    }

    public boolean hasElements() {
        return getChildren().size() > 0;
    }

    public boolean isSimple() {
        if (getChildren().size() > 1) {
            return false;
        }

        return !getChildren().get(0).getDataType().equals(TYPE_GROUP);
    }

    public String getTag(String what) {
        return tags.get(what + ".tag");
    }

    public void init(Element node) throws Exception {
        setName(XmlUtil.getAttribute(node, ATTR_NAME, ""));
        setLabel(XmlUtil.getAttribute(node, ATTR_LABEL, (String) null));
        setSuffixLabel(Utils.getAttributeOrTag(node, ATTR_SUFFIX,
                (String) null));

	entryType = XmlUtil.getAttribute(node, ATTR_ENTRYTYPE, (String) null);
        setShowInHtml(XmlUtil.getAttribute(node, ATTR_SHOWINHTML, true));
        setSearchable(XmlUtil.getAttributeFromTree(node, ATTR_SEARCHABLE,  false));

        setIsTitle(XmlUtil.getAttribute(node, "istitle",false));

        NamedNodeMap nnm = node.getAttributes();
        if (nnm != null) {
            for (int i = 0; i < nnm.getLength(); i++) {
                Attr   attr     = (Attr) nnm.item(i);
                String attrName = attr.getNodeName();
                if (attrName.endsWith(".tag")) {
                    tags.put(attrName, attr.getNodeValue());
                }
            }
        }

        NodeList children = XmlUtil.getElements(node);
        for (int i = 0; i < children.getLength(); i++) {
            Element childNode = (Element) children.item(i);
            if (childNode.getTagName().equals(TAG_TEMPLATE)) {
                processTemplateTag(childNode);
            } else if (childNode.getTagName().equals(TAG_ELEMENT)) {}
            else if (childNode.getTagName().equals(ATTR_HELP)) {}
            else if (childNode.getTagName().equals("suffix")) {}
            else {
                logError("Unknown metadata xml tag:"
                         + XmlUtil.toString(childNode), null);
            }
        }

        List childrenElements = XmlUtil.findChildren(node, TAG_ELEMENT);
        int  lastIndex        = 0;
        for (int j = 0; j < childrenElements.size(); j++) {
            Element elementNode = (Element) childrenElements.get(j);
            int     index       = lastIndex + 1;
            if (XmlUtil.hasAttribute(elementNode,
                                     MetadataElement.ATTR_INDEX)) {
                index = XmlUtil.getAttribute(elementNode,
                                             MetadataElement.ATTR_INDEX,
                                             index);
            }
            lastIndex = index;
            MetadataElement element = new MetadataElement(getHandler(), this,
                                          lastIndex, elementNode);
            addElement(element);
        }

    }

    public void processTemplate(String templateType, String template) {
        templates.put(templateType, template);
    }

    public void processTemplateTag(Element childNode) throws Exception {
        String templateType = XmlUtil.getAttribute(childNode, ATTR_TYPE);
        if (XmlUtil.hasAttribute(childNode, ATTR_FILE)) {

            processTemplate(templateType,
                            XmlUtil.getAttribute(childNode, ATTR_FILE));
        } else {
            processTemplate(templateType, XmlUtil.getChildText(childNode));
        }
    }

    public List<MetadataElement> getChildren() {
        return children;
    }

    public MetadataElement getElement(int idx) {
	if(idx<0 || idx>=children.size()) return null;
	return children.get(idx);
    }

    public MetadataElement getElement(String id) {
	if(id==null) return null;
	if(map==null) {
	    Hashtable<String,MetadataElement> tmp = new Hashtable<String,MetadataElement>();
	    for(MetadataElement element: getChildren()) {
		String _id =element.getId();
		if(_id!=null) {
		    tmp.put(_id,element);
		    tmp.put(_id.toLowerCase(),element);
		}
		String name =element.getName();		

		tmp.put(name,element);
		tmp.put(name.toLowerCase(),element);
		tmp.put(name.replace(" ","_"),element);
		tmp.put(name.toLowerCase().replace(" ","_"),element);
		tmp.put("attr" + element.getIndex(), element);
	    }
	    map = tmp;
	}

        return map.get(id);
    }    

    public void addElement(MetadataElement element) {
        getChildren().add(element);
	if(element.isFileType()) hasFile = true;
    }
    public String getFileUrl(Request request, Entry entry,
			     Metadata metadata, boolean checkImage,String matchFile)
            throws Exception {
        for (MetadataElement element : getChildren()) {
            if ( !element.getDataType().equals(element.TYPE_FILE)) {
                continue;
            }
            String url = getFileUrl(request, entry, metadata, element, checkImage,
                                     matchFile);
            if (url != null) {
                return url;
            }
        }

        return null;
    }

    public String getFileUrl(Request request, Entry entry,
			     Metadata metadata, MetadataElement element,
			     boolean checkImage,
			     String matchFile)
            throws Exception {

        File f = getFile(entry, metadata, element);
        if (f == null) {
            String value = metadata.getAttr(element.getIndex());
            if ((value != null) && value.startsWith("http")) {
                return value;
            }

            return null;
        }

        String tail = getStorageManager().getFileTail(f.toString());
        if (matchFile != null) {
            if ( !matchFile.equals("*") && !Misc.equals(matchFile, tail)) {
                return null;
            }
            //hack, hack
            if (matchFile.equals("display")) {
                if ( !element.showAsAttachment()) {
                    return null;
                }
            }

        }
        if (checkImage && !Utils.isImage(f.toString())) return null;
	tail = tail.replaceAll(" ", "_");
	String path =
	    handler.getRepository().getMetadataManager()
	    .URL_METADATA_VIEW + "/" + tail;
	return HU.url(path, ARG_ELEMENT, element.getIndex() + "",
		      ARG_ENTRYID, metadata.getEntryId(),
		      ARG_METADATA_ID, metadata.getId());
    }

    public String getFileHtml(Request request, Entry entry,
                              Metadata metadata, MetadataElement element,
                              boolean forLink) {
        File f = getFile(entry, metadata, element);
        if ((f == null) || !f.exists() || f.isDirectory()) {
            return null;
        }

        String extra = (forLink
                        ? HU.cssClass(
                            "ramadda-thumbnail-image img-thumbnail")
                        : "");
        extra += HU.attrs("style", "max-width:100%;","loading","lazy");
        String tail = getStorageManager().getFileTail(f.toString());
        String path =
            Utils.concatString(handler.getRepository().getMetadataManager()
                .URL_METADATA_VIEW.toString(), "/", tail);

        if (Utils.isImage(f.toString())) {
            String img = HU.img(HU.url(path, ARG_ELEMENT,
                             element.getIndex() + "", ARG_ENTRYID,
                             metadata.getEntryId(), ARG_METADATA_ID,
                             metadata.getId()), (forLink
                    ? msg("Click to enlarge")
                    : ""), extra);

            if (forLink) {
                String bigimg = HU.img(HU.url(path,
                                    ARG_ELEMENT, element.getIndex() + "",
                                    ARG_ENTRYID, metadata.getEntryId(),
                                    ARG_METADATA_ID,
                                    metadata.getId()), "thumbnail", "");
                StringBuilder tmp = new StringBuilder();
                img = HU.div(img,
                                    HU.cssClass("ramadda-thumbnail"));
                img = HU.makePopup(tmp, img, bigimg,
                                          new NamedValue("at", "right top"),
                                          new NamedValue("header", true));
                img = HU.div(
                    img, HU.cssClass("ramadda-thumbnail")) + "\n"
                        + tmp;
            } else {
                img = Utils.concatString(img, "\n<br>\n<b>", tail, "</b>\n");
            }

            return img;
        } else if (f.exists()) {
            String name = getStorageManager().getFileTail(f.getName());

            return HU.href(HU.url(path, ARG_ELEMENT,
                    element.getIndex() + "", ARG_ENTRYID,
                    metadata.getEntryId(), ARG_METADATA_ID,
                    metadata.getId()), name);
        }

        return "";
    }

    public String[] getFileUrl(Request request, Entry entry,
                               Metadata metadata)
            throws Exception {
        for (MetadataElement element : getChildren()) {
            if ( !element.getDataType().equals(element.TYPE_FILE)) {
                continue;
            }
            String[] url = getFileUrl(request, entry, metadata, element);
            if (url != null) {
                return url;
            }
        }

        return null;
    }

    public String[] getFileUrl(Request request, Entry entry,
                               Metadata metadata, MetadataElement element)
            throws Exception {

        File f = getFile(entry, metadata, element);
        if (f == null) {
            /*            String value = metadata.getAttr(element.getIndex());
            if ((value != null) && value.startsWith("http")) {
                return value;
            }
            */

            return null;
        }

        //Get the partial file name
        String tail = getStorageManager().getFileTail(f.toString());
        tail = tail.replaceAll(" ", "_");
        String path =
            handler.getRepository().getMetadataManager().URL_METADATA_VIEW
            + "/" + tail;
        String url = HU.url(path, ARG_ELEMENT,
                                   element.getIndex() + "", ARG_ENTRYID,
                                   metadata.getEntryId(), ARG_METADATA_ID,
                                   metadata.getId());

        //Get the full file name
        return new String[] { IO.getFileTail(f.toString()), url,f.getName()};
    }

    private String formatDate(long t) {
        if (sdf == null) {
            sdf = RepositoryUtil.makeDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        }
        synchronized (sdf) {
            return sdf.format(new Date(t)) + "Z";
        }
    }

    public File getFile(Entry entry, Metadata metadata, int attr) {
	return getFile(entry,metadata,metadata.getAttr(attr));
    }

    public File getFile(Entry entry, Metadata metadata, String filename) {	
	//	System.err.println("GET FILE:" + filename);
        if (!Utils.stringDefined(filename)) {
            return null;
        }

        File f= new File(
            IOUtil.joinDir(
                getStorageManager().getEntryDir(
                    metadata.getEntryId(), false), filename));
	return f;
    }

    public File getFile(Entry entry, Metadata metadata,
                        MetadataElement element) {
        File f;
        if ( !entry.getIsLocalFile()) {
            f = getFile(entry, metadata, element.getIndex());
        } else {
            f = new File(metadata.getAttr(element.getIndex()));
        }

        if ((f == null) || !f.exists()) {
            return null;
        }

        return f;
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

    public String getLabel() {
        if (label != null) {
            return label;
        }

        return getName();
    }

    public void setLabel(String value) {
        label = value;
    }

    public String getSuffixLabel() {
        return suffixLabel;
    }

    public void setSuffixLabel(String value) {
        suffixLabel = value;
    }

    /**
     *  Set the ShowInHtml property.
     *
     *  @param value The new value for ShowInHtml
     */
    public void setShowInHtml(boolean value) {
        this.showInHtml = value;
    }

    /**
     *  Get the ShowInHtml property.
     *
     *  @return The ShowInHtml
     */
    public boolean getShowInHtml() {
        return this.showInHtml;
    }

    /**
     * Get the Template property.
     *
     *
     * @param type _more_
     * @return The Template
     */
    public String getTemplate(String type) {
        return templates.get(type);
    }

    /**
     * Set the Searchable property.
     *
     * @param value The new value for Searchable
     */
    public void setSearchable(boolean value) {
        this.searchable = value;
    }

    /**
     * Get the Searchable property.
     *
     * @return The Searchable
     */
    public boolean getSearchable() {
        return this.searchable;
    }

    /**
       Set the IsTitle property.

       @param value The new value for IsTitle
    **/
    public void setIsTitle (boolean value) {
	isTitle = value;
    }

    /**
       Get the IsTitle property.

       @return The IsTitle
    **/
    public boolean getIsTitle () {
	return isTitle;
    }

}
