/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.metadata;


import org.ramadda.repository.*;
import org.ramadda.repository.map.MapInfo;
import org.ramadda.repository.map.MapManager;
import org.ramadda.repository.type.DataTypes;
import org.ramadda.util.ColorTable;
import org.ramadda.util.FormInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.ImageUtils;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;

import org.w3c.dom.*;


import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.awt.Image;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URL;
import java.net.URLConnection;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;


/**
 *
 * @author Jeff McWhirter
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public class MetadataElement extends MetadataTypeBase implements DataTypes {

    /** _more_ */
    public static final int THUMBNAIL_WIDTH = 600;

    /** _more_ */
    public static final String ARG_THUMBNAIL_SCALEDOWN =
        "metadata_thumbnail_scaledown";

    /** _more_ */
    public static final String ARG_THUMBNAIL_WIDTH =
        "metadata_thumbnail_width";

    public static final String ARG_THUMBNAIL_DELETE =
        "metadata_thumbnail_delete";    

    /** _more_ */
    public static final String ATTR_REQUIRED = "required";

    /** _more_ */
    public static final String ATTR_MAX = "max";

    /** _more_ */
    public static final String ATTR_ID = "id";

    /** _more_ */
    public static final String ATTR_ROWS = "rows";

    /** _more_ */
    public static final String ATTR_COLUMNS = "columns";

    /** _more_ */
    public static final String ATTR_DEPENDS = "depends";

    /** _more_ */
    public static final String ATTR_DATATYPE = "datatype";

    /** _more_ */
    public static final String ATTR_GROUP = "group";


    /** _more_ */
    public static final String ATTR_SUBNAME = "subname";

    /** _more_ */
    public static final String ATTR_DEFAULT = "default";

    /** _more_ */
    public static final String ATTR_VALUES = "values";


    /** _more_ */
    public static final String ATTR_SEARCHABLE = "searchable";

    /** _more_ */
    public static final String ATTR_THUMBNAIL = "thumbnail";

    /** _more_ */
    public static final String ATTR_INDEX = "index";

    /** _more_ */
    public static final String ATTR_ATTACHMENT = "attachment";


    /** _more_ */
    private String dataType = DATATYPE_STRING;


    /** _more_ */
    private boolean attachment = true;


    /** _more_ */
    private String id = null;

    /** _more_ */
    private String subName = "";

    /** _more_ */
    private int max = -1;

    /** _more_ */
    private int rows = 1;

    /** _more_ */
    private int columns = 60;

    /** _more_ */
    private List<HtmlUtils.Selector> values;    

    /** _more_ */
    private Hashtable<String, String> valueMap = new Hashtable<String,
                                                     String>();

    
    /** _more_ */
    private String dflt = "";

    /** _more_ */
    private boolean thumbnail = false;

    /** _more_ */
    private boolean required = false;

    /** _more_ */
    private boolean searchable = false;


    /** _more_ */
    private int index;

    /** _more_ */
    private MetadataTypeBase parent;

    /** _more_ */
    private String group;

    /** _more_ */
    private Element xmlNode;

    /**
     * _more_
     *
     * @param handler _more_
     * @param parent _more_
     * @param index _more_
     * @param node _more_
     *
     * @throws Exception _more_
     */
    public MetadataElement(MetadataHandler handler, MetadataTypeBase parent,
                           int index, Element node)
            throws Exception {
        super(handler);
        this.parent  = parent;
        this.index   = index;
        this.xmlNode = node;
        init(node);
    }


    /**
     * _more_
     *
     * @param node _more_
     *
     * @throws Exception _more_
     */
    public void init(Element node) throws Exception {
        super.init(node);

        subName = XmlUtil.getAttribute(node, ATTR_SUBNAME, "");
        id      = XmlUtil.getAttribute(node, ATTR_ID, (String) null);
	if(id==null && getName()!=null)
	    id = Utils.makeID(getName());
        max     = XmlUtil.getAttribute(node, ATTR_MAX, max);
        setDataType(XmlUtil.getAttribute(node, ATTR_DATATYPE,
                                         MetadataElement.DATATYPE_STRING));
	
        setRows(XmlUtil.getAttribute(node, ATTR_ROWS, 1));
        setColumns(XmlUtil.getAttribute(node, ATTR_COLUMNS, isEnumeration()?20:60));

        attachment = XmlUtil.getAttribute(node, ATTR_ATTACHMENT, true);
        setDefault(XmlUtil.getAttribute(node, ATTR_DEFAULT, ""));

        setGroup(XmlUtil.getAttribute(node, ATTR_GROUP, (String) null));
        setSearchable(XmlUtil.getAttribute(node, ATTR_SEARCHABLE, false));


        required = XmlUtil.getAttribute(node, ATTR_REQUIRED, false);
        setThumbnail(XmlUtil.getAttribute(node, ATTR_THUMBNAIL, false));

        if (dataType.equals(MetadataElement.DATATYPE_ENUMERATION)
                || dataType.equals(
                    MetadataElement.DATATYPE_ENUMERATIONPLUS)) {
            String delimiter = ":";
            String values    = XmlUtil.getAttribute(node, ATTR_VALUES, "");
            if (values.length() == 0) {
                values = getHandler().getEnumerationValues(this);
            }
            List<String> tmpValues = null;
            if (values.startsWith("resource:")) {
                tmpValues = getMetadataManager().getTypeResource(
                    values.substring("resource:".length()));
            } else if (values.startsWith("file:")) {
                //If it is a .properties file then the delimiter is =
                if (values.endsWith(".properties")) {
                    delimiter = "=";
                }
                String tagValues = getStorageManager().readSystemResource(
                                       values.substring(5));
                tmpValues = (List<String>) Utils.split(tagValues, "\n", true,
                        true);
            } else {
		if(values.startsWith("values:")) values = values.substring("values:".length());
                tmpValues = (List<String>) Utils.split(values, ",", true,
                        true);
            }

            List<HtmlUtils.Selector> enumValues = new ArrayList<HtmlUtils.Selector>();	    
            for (String tok : tmpValues) {
                //Check for comment line
                if (tok.startsWith("#")) {
                    continue;
                }
                int idx = tok.indexOf(delimiter);
                if (idx < 0) {
                    valueMap.put(tok, tok);
		    enumValues.add(new HtmlUtils.Selector(tok,tok));
                    continue;
                }
                String[] toks = Utils.split(tok, delimiter, 2);
                if (toks == null) {
                    valueMap.put(tok, tok);
                    enumValues.add(new HtmlUtils.Selector(tok,tok));
                    continue;
                }
                valueMap.put(toks[0], toks[1]);
		//        public Selector(String label, String id, String tooltip, String icon,int margin, int padding, boolean isHeader) {
                enumValues.add(new HtmlUtils.Selector(toks[1], toks[0],toks[0],null,0,0,false));
            }
            enumValues.add(0, new HtmlUtils.Selector("",""));
            setValues(enumValues);
        }
    }


    public MetadataTypeBase getParent() {
	return parent;
    }

    @Override
    public int hashCode() {
	if(parent!=null && id!=null) {
	    return id.hashCode() ^ parent.hashCode();
	}
	if(id!=null) {
	    return id.hashCode();
	}
	if(parent!=null) {
	    return parent.hashCode();
	}
	return 0;
    }

    @Override
    public boolean equals(Object o) {
	if(!(o instanceof MetadataElement)) return false;
	MetadataElement that = (MetadataElement)o;
	return Misc.equals(this.id,that.id) && Misc.equals(this.parent,that.parent);
    }    

    /**
     * _more_
     *
     * @return _more_
     */
    public Element getXmlNode() {
        return xmlNode;
    }

    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
    private boolean isString(String type) {
        return dataType.equals(DATATYPE_STRING)
               || dataType.equals(DATATYPE_WIKI)
               || dataType.equals(DATATYPE_EMAIL)
               || dataType.equals(DATATYPE_URL);
    }


    /**
     * _more_
     *
     * @param value _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private List<Metadata> getGroupData(String value) throws Exception {
        List<Metadata> result = new ArrayList<Metadata>();
        List<Hashtable<Integer, String>> entries =
            (List<Hashtable<Integer,
                            String>>) (((value != null)
                                        && (value.length() > 0))
                                       ? Repository.decodeObject(value)
                                       : null);
        if (entries == null) {
            return result;
        }
        for (Hashtable<Integer, String> map : entries) {
            Metadata metadata = new Metadata();
            result.add(metadata);
            for (Enumeration keys = map.keys(); keys.hasMoreElements(); ) {
                Integer index = (Integer) keys.nextElement();
                metadata.setAttr(index.intValue(), map.get(index));
            }
        }

        return result;
    }

    public boolean isFileType() {
        return dataType.equals(DATATYPE_FILE);
    }

    public boolean isPrivate(Request request, Entry entry,MetadataType type, Metadata metadata) {
        if (dataType.equals(DATATYPE_API_KEY) || dataType.equals(DATATYPE_PASSWORD)) {
	    return true;
	}

	return false;
    }


    public String getValueForExport(Request request, Entry entry,MetadataType type,
				    Metadata metadata) 
            throws Exception {
        if (isPrivate(request,entry,type,metadata)) {
	    return null;
	}
	int    index = this.getIndex();
	String value = metadata.getAttr(index);
	return value;
    }




    public MetadataHtml getHtml(Request request, Entry entry,
                                MetadataType type,
                                Metadata containerMetadata, String value,
                                int depth)
            throws Exception {

        if ((value == null) || dataType.equals(DATATYPE_SKIP)) {
            return null;
        }

        if (isPrivate(request,entry,type, containerMetadata)) {
	    return null;
	}


        if (dataType.equals(DATATYPE_FILE)) {
            //Don't show thumbnails
            if (getThumbnail()) {
                return null;
            }

            String url = getFileUrl(request, entry, containerMetadata, this,true,
                                     null);
            if (url != null) {
                return new MetadataHtml(
                    "",
                    HU.img(
                        url, HU.cssClass("ramadda-metadata-image")));
            }

            return null;
        }

        String tab = "";
        for (int i = 0; i < depth; i++) {
            tab = tab + "    ";
        }
        String html = null;
        if (getDataType().equals(DATATYPE_GROUP)) {
            List<Metadata> childMetadata = getGroupData(value);
            if (childMetadata.size() == 0) {
                return null;
            }
            List<StringBuffer> subEntries = new ArrayList<StringBuffer>();
            boolean            anyChildrenGroups = false;
            for (Metadata metadata : childMetadata) {
                List<MetadataHtml> formInfos   =
                    new ArrayList<MetadataHtml>();
                List<MetadataElement> children = getChildren();
                for (MetadataElement element : children) {
                    MetadataHtml formInfo =
                        element.getHtml(request, entry, type, metadata,
                                        metadata.getAttr(element.getIndex()),
                                        depth + 1);
                    if (formInfo == null) {
                        continue;
                    }
                    formInfo.isGroup =
                        element.getDataType().equals(DATATYPE_GROUP);
                    if (formInfo.isGroup) {
                        anyChildrenGroups = true;
                    }
                    formInfos.add(formInfo);
                }
                StringBuffer subEntrySB = null;
                for (MetadataHtml formInfo : formInfos) {
                    if ((formInfo.content.length() > 0)
                            || (children.size() > 1)) {
                        if (subEntrySB == null) {
                            subEntrySB = new StringBuffer();
                            subEntries.add(subEntrySB);
                        }
                        if (formInfo.isGroup) {
                            //                            subEntrySB.append("<tr valign=\"top\"><td></td><td>\n");
                        }
                        subEntrySB.append(HU.formEntry(formInfo.label,
                                formInfo.content));
                        if (formInfo.isGroup) {
                            //                            subEntrySB.append("</td></tr>\n");
                        }
                    }
                }
            }


            StringBuffer entriesSB      = new StringBuffer();
            boolean      haveSubEntries = subEntries.size() != 0;
            int          entryCnt       = 0;
            for (StringBuffer subEntrySB : subEntries) {
                entryCnt++;
                if (anyChildrenGroups || (children.size() > 1)) {
                    StringBuffer tmp = new StringBuffer();
                    tmp.append(HU.formTable());
                    tmp.append(subEntrySB);
                    tmp.append(HU.formTableClose());
                    entriesSB.append(HU.makeToggleInline(entryCnt
                            + ") " + subName, tmp.toString(), true));
                    entriesSB.append("<br>");
                } else {
                    entriesSB.append(HU.formTable());
                    entriesSB.append(subEntrySB);
                    entriesSB.append(HU.formTableClose());
                }
            }
            if (haveSubEntries) {
                String[]     toggle = HU.getToggle("", true);
                String       id     = toggle[0];
                String       link   = toggle[1];
                String       initJS = toggle[2];
                StringBuffer tmp    = new StringBuffer();
                tmp.append(HU.formTable());
                tmp.append(
                    "<tr valign=top><td width=1%>" + link + "</td><td>"
                    + HU.div(
                        entriesSB.toString(),
                        HU.id(id)
                        + HU.cssClass("ramadda-metadata-html")) + "</td></tr>");
                tmp.append(HU.formTableClose());
                if (initJS.length() > 0) {
                    tmp.append(HU.script(initJS));
                }
                html = tmp.toString();
                html = entriesSB.toString();
            } else {
                html = "none";
            }
        } else if (dataType.equals(DATATYPE_ENUMERATION)
                   || dataType.equals(DATATYPE_ENUMERATIONPLUS)) {
            String label = getLabel(value);
            html = label;
        } else if (dataType.equals(DATATYPE_ENTRY)) {
            html = "";
            if (value.length() > 0) {
                Entry theEntry =
                    getRepository().getEntryManager().getEntry(request,
                        value);
                if (theEntry != null) {
                    html = theEntry.getName();
                }
            }
        } else if (dataType.equals(DATATYPE_EMAIL)) {
	    if(stringDefined(value))
		html = HU.href("mailto:" + value, value);
        } else if (dataType.equals(DATATYPE_URL)) {
            if (Utils.stringDefined(value)) {
                html = HU.href(value, value);
            } else {
                html = "";
            }
        } else if (dataType.equals(DATATYPE_DOUBLE) || dataType.equals(DATATYPE_INT)) {
	    try {
		double d =Double.parseDouble(value);
		if(d == (int)d) {
		    int i = (int) d;
		    html = Utils.intFormatComma(i);
		} else {
		    html = Utils.formatComma(d);
		}
	    } catch(Exception ignore) {
		html = value;
	    }
	} else if(getDataType().equals(DATATYPE_LATLON)) {
	    List<String> toks = Utils.split(value,",",true,true);
	    html="";
	    if(toks.size()==2 && Utils.stringDefined(toks.get(0)) && Utils.stringDefined(toks.get(1))) {
		html=value;
		double lat = Utils.getDouble(toks.get(0));
		double lon = Utils.getDouble(toks.get(1));
		if(!Double.isNaN(lat) && !Double.isNaN(lon)) {
		    MapInfo map = new MapInfo(request, getRepository(),"200","200");
		    map.addMarker("",lat,  lon, null,"","");
		    map.center();
		    html+=getRepository().getMapManager().getHtmlImports(request);
		    html+=map.getHtml();
		}
	    } else {
		html="Undefined";
	    }
        } else {
            html = value;
        }
        //        System.err.println("   html = " + html);
        if (html != null) {
            String name = getName();
            if (name.length() > 0) {
                name = msgLabel(name);
            } else {
                name = HU.space(1);
            }

            //            sb.append(HU.formEntry(name, html));
            return new MetadataHtml(name, html);
        }

        return null;

    }



    /**
     * _more_
     *
     * @param value _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void getTextCorpus(String value, Appendable sb) throws Exception {
        if ((value == null) || dataType.equals(DATATYPE_SKIP)) {
            return;
        }
        //For now skip showing files
        if (dataType.equals(DATATYPE_FILE)) {
            return;
        }
        String name = getName();
        if (getDataType().equals(DATATYPE_GROUP)) {
            List<Metadata> childMetadata = getGroupData(value);
            if (childMetadata.size() == 0) {
                return;
            }
            for (Metadata metadata : childMetadata) {
                for (MetadataElement element : getChildren()) {
                    element.getTextCorpus(
                        metadata.getAttr(element.getIndex()), sb);
                }
            }

            return;
        } else if (dataType.equals(DATATYPE_ENUMERATION)
                   || dataType.equals(DATATYPE_ENUMERATIONPLUS)) {
            sb.append(getLabel(value));
        } else if (dataType.equals(DATATYPE_EMAIL)) {
            sb.append("email:" + value);
        } else if (dataType.equals(DATATYPE_URL)) {
            sb.append("url:" + value);
        } else {
            sb.append(value);
        }
        sb.append(" ");
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Mon, Sep 5, '11
     * @author         Enter your name here...
     */
    public static class MetadataHtml {

        /** _more_ */
        public String label;

        /** _more_ */
        public String content;

        /** _more_ */
        public boolean isGroup = false;

        /**
         * _more_
         *
         * @param label _more_
         * @param content _more_
         */
        public MetadataHtml(String label, String content) {
            this.label   = label;
            this.content = content;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String toString() {
            return "MetadataHtml:" + label + " " + content;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getLabel() {
            return label;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getHtml() {
            return content;
        }

    }


    /**
     * _more_
     *
     * @param value _more_
     *
     * @return _more_
     */
    public String getLabel(String value) {
        if (valueMap == null) {
            return value;
        }
        String label = valueMap.get(value);
        if (label == null) {
            label = value;
        }

        return label;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isGroup() {
        return getDataType().equals(DATATYPE_GROUP);
    }

    public String handleForm(Request request, Entry entry,
                             Metadata newMetadata, Metadata oldMetadata,
                             String suffix)
            throws Exception {

        String arg = ARG_METADATA_ATTR + getIndex() + suffix;

        if (getDataType().equals(DATATYPE_LATLON)) {
            return request.getString(arg + ".latitude", "") + ","
                   + request.getString(arg + ".longitude", "");
        }


        if (getDataType().equals(DATATYPE_BOOLEAN)) {
            boolean value = request.get(arg, false);

            return "" + value;
        }

        if (getDataType().equals(DATATYPE_API_KEY)) {
	    if(request.get(arg+"_regenerate",false)) {
		return Utils.getGuid();
	    }
	    return request.getString(arg,"");
	}


        if (getDataType().equals(DATATYPE_ENTRY)) {
            return request.getString(arg + "_hidden", "");
        }

        if (getDataType().equals(DATATYPE_GROUP)) {
            List<Hashtable<Integer, String>> entries =
                new ArrayList<Hashtable<Integer, String>>();
            int groupCnt = 0;
            while (true) {
                String subArg = arg + "_group" + groupCnt + "_";
                groupCnt++;
                if ( !request.exists(subArg + "_group")) {
                    break;
                }
                if (request.get(subArg + "_delete", false)) {
                    continue;
                }
                if (request.get(subArg + "_lastone", false)) {
                    if ( !request.get(subArg + "_new", false)) {
                        continue;
                    }
                }
                Hashtable<Integer, String> map = new Hashtable<Integer,
                                                     String>();
                for (MetadataElement element : getChildren()) {
                    String subValue = element.handleForm(request, entry,
                                          newMetadata, oldMetadata, subArg);
                    if (subValue == null) {
                        continue;
                    }
                    map.put(Integer.valueOf(element.getIndex()), subValue);
                }
                entries.add(map);
            }

            return Repository.encodeObject(entries);
        }


        String attr = request.getString(arg, "");
        if (request.defined(arg + "_select")) {
            attr = request.getString(arg + "_select", "");
        }
        if (request.defined(arg + "_input")) {
            attr = request.getString(arg + "_input", "");
        }

        //        newMetadata.setAttr(getIndex(), attr);

        if ( !getDataType().equals(DATATYPE_FILE)) {
            return attr;
        }



        String oldValue = ((oldMetadata == null)
                           ? ""
                           : oldMetadata.getAttr(getIndex()));


	if(request.get(ARG_THUMBNAIL_DELETE,false)) {
	    File f = getFile(entry,oldMetadata,oldValue);
	    if(f!=null && f.exists()) {
		f.delete();
	    }
	    return null;
	}		




        String url      = request.getString(arg + "_url", "");
        String theFile  = null;
        if (url.length() > 0) {
            String        tail       = IO.getFileTail(url);
            File tmpFile = getStorageManager().getTmpFile(tail);
            InputStream   fromStream = getStorageManager().getInputStreamFromUrl(request,url);
            OutputStream toStream =
                getStorageManager().getFileOutputStream(tmpFile);
            try {
                long bytes = IOUtil.writeTo(fromStream, toStream);
                if (bytes < 0) {
                    throw new IllegalArgumentException(
                        "Could not download url:" + url);
                }
            } catch (Exception ioe) {
                throw new IllegalArgumentException("Could not download url:"
                        + url);
            } finally {
                IO.close(toStream);
                IO.close(fromStream);
            }
            theFile = tmpFile.toString();
        } else {
            //          String name = request.getString("upload_name_"+i);
            //          String contents = request.getString("upload_file_"+i);

            String fileArg = request.getUploadedFile(arg);
            if (fileArg == null) {
                return oldValue;
            }
            theFile = fileArg;
        }

        if (getThumbnail() && request.get(ARG_THUMBNAIL_SCALEDOWN, false)) {
            Image image = ImageUtils.readImage(theFile);
	    if(image==null) {
		getSessionManager().addSessionMessage(request,"Error processing image:" + entry);
	    } else {
		int   width = request.get(ARG_THUMBNAIL_WIDTH, THUMBNAIL_WIDTH);
		if (image.getWidth(null) > width) {
		    image = ImageUtils.resize(image, width, -1);
		    ImageUtils.waitOnImage(image);
		    ImageUtils.writeImageToFile(image, theFile);
		}
	    }
        }


        theFile = getStorageManager().moveToEntryDir(entry,
						     new File(theFile)).getName();

        return theFile;
    }


    public String getForm(Request request, Entry entry, FormInfo formInfo,
                          Metadata metadata, String suffix, String value,
                          boolean forEdit)
            throws Exception {



        if (dataType.equals(DATATYPE_SKIP)) {
            return "";
        }
        String arg = ARG_METADATA_ATTR + getIndex() + suffix;

        if ((value == null) || (value.length() == 0)) {
            value = request.getString(arg, dflt);
        }



        if (isString(dataType)) {
            if (dataType.equals(DATATYPE_WIKI)) {
                /*
                String buttons =
                    getRepository().getWikiManager().makeWikiEditBar(request,
                        entry, arg) + HU.br();
                return buttons
                       + HU.textArea(arg, value, rows, columns,
                                            HU.id(arg));
                */
                StringBuilder sb = new StringBuilder();

		String wikiHeight="400px";
		if(rows>1){
		    wikiHeight = ((int)(1.3*rows))+"em";
		}
                entry.getTypeHandler().addWikiEditor(request, entry, sb,
						     formInfo, arg, value, null, false, 25000, true,"height",wikiHeight);

                return HU.div(sb.toString(),HU.style("width:1000px;"));
                //                      wikiText, null, false, 256000);
            } else {
                if (rows > 1) {
                    return HU.textArea(arg, value, rows, columns);
                }

                return HU.input(arg, value,
                                       HU.attr(HU.ATTR_SIZE,
                                           "" + columns));
            }
	} else if (dataType.equals(DATATYPE_NOEDIT)) {
	    return "Not editable " + HU.hidden(arg, value);
        } else if (dataType.equals(DATATYPE_LATLON)) {
            List<String> toks = Utils.splitUpTo(value, ",", 2);
            MapInfo map = getMapManager().createMap(request, entry, true,
                              null);
            String mapSelector = map.makeSelector(arg, true,
                                     new String[] { (toks.size() > 0)
                    ? toks.get(0)
                    : "", (toks.size() > 1)
                            ? toks.get(1)
                            : "" }, "", "");

            return mapSelector;
        } else if (dataType.equals(DATATYPE_COLORTABLE)) {
            List names =
                Utils.split(
                    "blues,blue_green_red,white_blue,blue_red,red_white_blue,blue_white_red,grayscale,inversegrayscale,rainbow,nice,blues,gray_scale,inverse_gray_shade,light_gray_scale,blue_green,blue_purple,green_blue,orange_red,purple_blue,purple_blue_green,purple_red,red_purple,yellow_green,yellow_green_blue,yellow_orange_brown,yellow_orange_red,oranges,purples,reds,greens,map_grays,bright38,precipitation,humidity,temperature,visad,inverse_visad,wind_comps,windspeed,dbz,dbz_nws,topographic", ",");
            names.add(0, new TwoFacedObject("--none--", ""));
            return HU.select(arg, names, value) + " "
                   + HU.href(getRepository().getUrlBase()
                                    + "/colortables", "View",
                                        "target=_colortables");
        } else if (dataType.equals(DATATYPE_BOOLEAN)) {
            return HU.checkbox(arg, "true",
                                      Misc.equals(value, "true"));
        } else if (dataType.equals(DATATYPE_ENTRY)) {
            return getRepository().getEntryManager().getEntryFormSelect(
									request, entry, arg, value,entryType);
        } else if (dataType.equals(DATATYPE_API_KEY)) {
	    String uid = HU.getUniqueId("apikey");
	    if(!Utils.stringDefined(value)) value=Utils.getGuid();
	    String regen =  HU.labeledCheckbox(arg+"_regenerate", "true",false,"Regenerate");
            return regen +HU.space(3) + HU.b("Key: ") +"<i>" + value+"</i>" +
		HU.hidden(arg, value);
        } else if (dataType.equals(DATATYPE_INT)) {
            return HU.input(arg, value, HU.SIZE_10);
        } else if (dataType.equals(DATATYPE_DOUBLE)) {
            return HU.input(arg, value, HU.SIZE_10);
        } else if (dataType.equals(DATATYPE_DATETIME)) {
            Date date;
            if (value != null) {
                date = parseDate(value);
            } else {
                date = new Date();
            }

            return getDateHandler().makeDateInput(request, arg, "", date);
        } else if (dataType.equals(DATATYPE_DATE)) {
            Date date;
            if (values != null) {
                date = parseDate(value);
            } else {
                date = new Date();
            }

            return getDateHandler().makeDateInput(request, arg, "", date,
                    null, false);
        } else if (dataType.equals(DATATYPE_ENUMERATION)) {
            return HU.select(arg, values, value);
        } else if (dataType.equals(DATATYPE_ENUMERATIONPLUS)) {
	    MetadataType mtdType = getMetadataManager().findType(metadata.getType());
            String[] va = getMetadataManager().getDistinctValues(request,
								 mtdType.getHandler(), mtdType,index);
	    
	    //Check for any values in the database
	    List<HtmlUtils.Selector> valuesToUse = values;    
	    if(va.length>0) {
		valuesToUse = new ArrayList<HtmlUtils.Selector>(values);
		for(String a: va) {
		    if(!HtmlUtils.Selector.contains(values,a)) {
			valuesToUse.add(new HtmlUtils.Selector(a,a));
		    }
		}
	    }
            boolean contains = HtmlUtils.Selector.contains(valuesToUse, value);
	    
            return HU.select(arg, valuesToUse, value) + HU.space(2)
                   + msgLabel("Or")
                   + HU.input(arg + "_input", (contains
                    ? ""
					       : value), HU.attrs("size",""+columns));
        } else if (dataType.equals(DATATYPE_FILE)) {
            String image = (forEdit
                            ? getFileHtml(request, entry, metadata, this,
                                          false)
                            : "");
            if (image == null) {
                image = "";
            } 


            String extra = "";
            if (getThumbnail()) {
                extra = "<br>"
                        + HU.checkbox(
                            ARG_THUMBNAIL_SCALEDOWN, "true",
                            true) + HU.space(1)
                                  + msg("Scale down image")
                                  + HU.space(2) + "Width: "
                                  + HU.input(
                                      ARG_THUMBNAIL_WIDTH, THUMBNAIL_WIDTH,
                                      HU.SIZE_5);
            }

            StringBuilder sb      = new StringBuilder();
            String        inputId = formInfo.getId() + "_" + arg;
            sb.append(image);
	    String space = HU.div("",HU.style("margin-bottom:0.5em;"));
	    if(Utils.stringDefined(image)) sb.append(space);
	    sb.append(HU.fileInput(arg, HU.SIZE_70 + HU.id(inputId)) 
		      +space +
		      HU.input(arg + "_url", "", HU.attrs("style","width:430px;","placeholder","Or download URL")) + extra);
	    sb.append(HU.br());
	    sb.append(HU.labeledCheckbox(ARG_THUMBNAIL_DELETE, "true",false,"Delete file"));
            HU.script(sb,
                             "Ramadda.initFormUpload("
                             + HU.comma(HU.squote(inputId)) + ");");

            //+"','" + formInfo.getId()+"_dnd"+"');"));
            return sb.toString();
        } else if (dataType.equals(DATATYPE_GROUP)) {
            StringBuffer   sb            = new StringBuffer();
            String         lastGroup     = null;
            int            groupCnt      = 0;
            List<Metadata> groupMetadata = getGroupData(value);
            boolean        hadAny        = groupMetadata.size() > 0;
            groupMetadata.add(new Metadata());
            StringBuffer entriesSB = new StringBuffer();
            for (Metadata subMetadata : groupMetadata) {
                StringBuffer groupSB = new StringBuffer();
                groupSB.append(HU.formTable());
                String subArg = arg + "_group" + groupCnt + "_";
                boolean lastOne = ((groupMetadata.size() > 1)
                                   && (groupCnt == groupMetadata.size() - 1));
                if (lastOne) {
                    String newCbx =
                        HU.checkbox(subArg + "_new", "true", false)
                        + " " + msg("Click here to add a new record")
                        + HU.hidden(subArg + "_lastone", "true");
                    //                    groupSB.append(HU.formEntry("",newCbx));
                } else if (hadAny) {
                    //                    groupSB.append(HU.formEntry(msgLabel("Delete"),HU.checkbox(subArg+"_delete","true",false)));
                }

                for (MetadataElement element : getChildren()) {
                    if ((element.getGroup() != null)
                            && !Misc.equals(element.getGroup(), lastGroup)) {
                        lastGroup = element.getGroup();
                        groupSB.append(
                            HU.row(
                                HU.colspan(header(lastGroup), 2)));
                    }

                    String elementLbl = element.getName();
                    if (elementLbl.length() > 0) {
                        elementLbl = msgLabel(elementLbl);
                    }
                    String subValue = subMetadata.getAttr(element.getIndex());
                    if (subValue == null) {
                        subValue = "";
                    }
                    String widget = element.getForm(request, entry, formInfo,
                                        metadata, subArg, subValue, forEdit);
                    if ((widget == null) || (widget.length() == 0)) {
                        continue;
                    }
                    groupSB.append(HU.formEntry(elementLbl, widget));
                    groupSB.append(HU.hidden(subArg + "_group",
                            "true"));
                }


                groupSB.append(HU.formTableClose());

                if (lastOne) {
                    String newCbx = HU.checkbox(subArg + "_new",
                                        "true",
                                        false) + HU.hidden(subArg
                                        + "_lastone", "true");

                    entriesSB.append(HU.makeShowHideBlock(newCbx
                            + " Add New " + subName, groupSB.toString(),
                                false));
                } else {
                    String deleteCbx = ( !hadAny
                                         ? ""
                                         : " - "
                                           + HU.checkbox(subArg
                                               + "_delete", "true",
                                                   false) + " "
                                                       + msg("delete"));
                    entriesSB.append(HU.makeShowHideBlock((groupCnt
                            + 1) + ") " + subName
                                 + deleteCbx, groupSB.toString(), true));
                }
                groupCnt++;
            }
            sb.append(
                HU.makeToggleInline(
                    "",
                    HU.div(
                        entriesSB.toString(),
                        HU.cssClass("ramadda-metadata-form")), true));

            return sb.toString();
        } else {
            System.err.println("Unknown data type:" + dataType);

            return "Unknown data type:" + dataType;
        }

    }


    /**
     * _more_
     *
     * @param value _more_
     *
     * @return _more_
     */
    private Date parseDate(String value) {
        return null;
    }


    /**
     *  Set the Type property.
     *
     *  @param value The new value for Type
     */
    public void setDataType(String value) {
        dataType = value;
    }

    /**
     *  Get the Type property.
     *
     *  @return The Type
     */
    public String getDataType() {
        return dataType;
    }

    public boolean isEnumeration() {
	return (dataType.equals(DATATYPE_ENUMERATION)
		|| dataType.equals(DATATYPE_ENUMERATIONPLUS));
    }

    public boolean isBoolean() {
	return dataType.equals(DATATYPE_BOOLEAN);
    }
    

    /**
     *  Set the Rows property.
     *
     *  @param value The new value for Rows
     */
    public void setRows(int value) {
        rows = value;
    }

    /**
     *  Get the Rows property.
     *
     *  @return The Rows
     */
    public int getRows() {
        return rows;
    }

    /**
     *  Set the Columns property.
     *
     *  @param value The new value for Columns
     */
    public void setColumns(int value) {
        columns = value;
    }

    /**
     *  Get the Columns property.
     *
     *  @return The Columns
     */
    public int getColumns() {
        return columns;
    }


    /**
     * Set the Values property.
     *
     * @param value The new value for Values
     */
    public void setValues(List<HtmlUtils.Selector> value) {
        values = value;
    }

    /**
     * Get the Values property.
     *
     * @return The Values
     */
    public List<HtmlUtils.Selector> getValues() {
        return values;
    }


    /**
     * Set the Dflt property.
     *
     * @param value The new value for Dflt
     */
    public void setDefault(String value) {
        dflt = value;
    }

    /**
     * Get the Dflt property.
     *
     * @return The Dflt
     */
    public String getDefault() {
        return dflt;
    }

    /**
     * Set the Thumbnail property.
     *
     * @param value The new value for Thumbnail
     */
    public void setThumbnail(boolean value) {
        this.thumbnail = value;
    }

    /**
     * Get the Thumbnail property.
     *
     * @return The Thumbnail
     */
    public boolean getThumbnail() {
        return this.thumbnail;
    }

    /**
     * Set the Index property.
     *
     * @param value The new value for Index
     */
    public void setIndex(int value) {
        this.index = value;
    }

    /**
     * Get the Index property.
     *
     * @return The Index
     */
    public int getIndex() {
        return this.index;
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
     *  Set the Group property.
     *
     *  @param value The new value for Group
     */
    public void setGroup(String value) {
        this.group = value;
    }

    /**
     *  Get the Group property.
     *
     *  @return The Group
     */
    public String getGroup() {
        return this.group;
    }

    /**
     * Set the Id property.
     *
     * @param value The new value for Id
     */
    public void setId(String value) {
        this.id = value;
    }

    /**
     * Get the Id property.
     *
     * @return The Id
     */
    public String getId() {
        return this.id;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean showAsAttachment() {
        return attachment;
    }


}
