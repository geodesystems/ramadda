/*
* Copyright (c) 2008-2019 Geode Systems LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*     http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.ramadda.repository.metadata;


import org.ramadda.repository.*;
import org.ramadda.repository.type.DataTypes;
import org.ramadda.util.ColorTable;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;


import org.w3c.dom.*;

import ucar.unidata.ui.ImageUtils;

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
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class MetadataElement extends MetadataTypeBase implements DataTypes {


    /** _more_ */
    public static final int THUMBNAIL_WIDTH = 400;

    /** _more_ */
    public static final String ARG_THUMBNAIL_SCALEDOWN =
        "metadata_thumbnail_scaledown";

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
    private List<TwoFacedObject> values;

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
        max     = XmlUtil.getAttribute(node, ATTR_MAX, max);
        setRows(XmlUtil.getAttribute(node, ATTR_ROWS, 1));
        setColumns(XmlUtil.getAttribute(node, ATTR_COLUMNS, 60));
        setDataType(XmlUtil.getAttribute(node, ATTR_DATATYPE,
                                         MetadataElement.DATATYPE_STRING));
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
            if (values.startsWith("file:")) {
                //If it is a .properties file then the delimiter is =
                if (values.endsWith(".properties")) {
                    delimiter = "=";
                }
                String tagValues = getStorageManager().readSystemResource(
                                       values.substring(5));
                tmpValues = (List<String>) StringUtil.split(tagValues, "\n",
                        true, true);
            } else {
                tmpValues = (List<String>) StringUtil.split(values, ",",
                        true, true);
            }

            List<TwoFacedObject> enumValues = new ArrayList<TwoFacedObject>();
            for (String tok : tmpValues) {
                //Check for comment line
                if (tok.startsWith("#")) {
                    continue;
                }
                int idx = tok.indexOf(delimiter);
                if (idx < 0) {
                    valueMap.put(tok, tok);
                    enumValues.add(new TwoFacedObject(tok));

                    continue;
                }
                String[] toks = StringUtil.split(tok, delimiter, 2);
                if (toks == null) {
                    valueMap.put(tok, tok);
                    enumValues.add(new TwoFacedObject(tok));

                    continue;
                }
                valueMap.put(toks[0], toks[1]);
                enumValues.add(new TwoFacedObject(toks[1], toks[0]));
            }
            enumValues.add(0, new TwoFacedObject(""));
            setValues(enumValues);
        }
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


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param type _more_
     * @param containerMetadata _more_
     * @param value _more_
     * @param depth _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public MetadataHtml getHtml(Request request, Entry entry,
                                MetadataType type,
                                Metadata containerMetadata, String value,
                                int depth)
            throws Exception {

        if ((value == null) || dataType.equals(DATATYPE_SKIP)) {
            return null;
        }

        if (dataType.equals(DATATYPE_FILE)) {
            //Don't show thumbnails
            if (getThumbnail()) {
                return null;
            }

            String url = getImageUrl(request, entry, containerMetadata, this,
                                     null);
            if (url != null) {
                return new MetadataHtml(
                    "",
                    HtmlUtils.img(
                        url, HtmlUtils.cssClass("ramadda-metadata-image")));
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
                        subEntrySB.append(HtmlUtils.formEntry(formInfo.label,
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
                    tmp.append(HtmlUtils.formTable());
                    tmp.append(subEntrySB);
                    tmp.append(HtmlUtils.formTableClose());
                    entriesSB.append(HtmlUtils.makeToggleInline(entryCnt
                            + ") " + subName, tmp.toString(), true));
                    entriesSB.append("<br>");
                } else {
                    entriesSB.append(HtmlUtils.formTable());
                    entriesSB.append(subEntrySB);
                    entriesSB.append(HtmlUtils.formTableClose());
                }
            }
            if (haveSubEntries) {
                String[]     toggle = HtmlUtils.getToggle("", true);
                String       id     = toggle[0];
                String       link   = toggle[1];
                String       initJS = toggle[2];
                StringBuffer tmp    = new StringBuffer();
                tmp.append(HtmlUtils.formTable());
                tmp.append(
                    "<tr valign=top><td width=1%>" + link + "</td><td>"
                    + HtmlUtils.div(
                        entriesSB.toString(),
                        HtmlUtils.id(id)
                        + HtmlUtils.cssClass("ramadda-metadata-html")) + "</td></tr>");
                tmp.append(HtmlUtils.formTableClose());
                if (initJS.length() > 0) {
                    tmp.append(HtmlUtils.script(initJS));
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
            html = HtmlUtils.href("mailto:" + value, value);
        } else if (dataType.equals(DATATYPE_URL)) {
            if (Utils.stringDefined(value)) {
                html = HtmlUtils.href(value, value);
            } else {
                html = "";
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
                name = HtmlUtils.space(1);
            }

            //            sb.append(HtmlUtils.formEntry(name, html));
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

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param newMetadata _more_
     * @param oldMetadata _more_
     * @param suffix _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    public String handleForm(Request request, Entry entry,
                             Metadata newMetadata, Metadata oldMetadata,
                             String suffix)
            throws Exception {

        String arg = ARG_METADATA_ATTR + getIndex() + suffix;

        if (getDataType().equals(DATATYPE_BOOLEAN)) {
            boolean value = request.get(arg, false);

            return "" + value;
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
                    map.put(new Integer(element.getIndex()), subValue);
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


        String url      = request.getString(arg + "_url", "");
        String theFile  = null;
        if (url.length() > 0) {
            String        tail       = IOUtil.getFileTail(url);
            File tmpFile = getStorageManager().getTmpFile(request, tail);
            URL           fromUrl    = new URL(url);
            URLConnection connection = fromUrl.openConnection();
            InputStream   fromStream = connection.getInputStream();
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
                IOUtil.close(toStream);
                IOUtil.close(fromStream);
            }
            theFile = tmpFile.toString();
        } else {
            String fileArg = request.getUploadedFile(arg);
            if (fileArg == null) {
                return oldValue;
            }
            theFile = fileArg;
        }

        if (getThumbnail() && request.get(ARG_THUMBNAIL_SCALEDOWN, false)) {
            Image image = Utils.readImage(theFile);
            if (image.getWidth(null) > THUMBNAIL_WIDTH) {
                image = ImageUtils.resize(image, THUMBNAIL_WIDTH, -1);
                ImageUtils.waitOnImage(image);
                ImageUtils.writeImageToFile(image, theFile);
            }
        }


        theFile = getStorageManager().moveToEntryDir(entry,
                new File(theFile)).getName();

        return theFile;
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param templateType _more_
     * @param entry _more_
     * @param metadata _more_
     * @param value _more_
     * @param parent _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getValueForXml(Request request, String templateType,
                                 Entry entry, Metadata metadata,
                                 String value, Element parent)
            throws Exception {

        if ( !dataType.equals(DATATYPE_GROUP)) {
            //            System.err.println("\traw;" + value);
            return value;
        }

        String template = getTemplate(templateType);
        if (template == null) {
            return null;
        }
        StringBuffer   xml           = new StringBuffer();
        List<Metadata> groupMetadata = getGroupData(value);
        for (Metadata subMetadata : groupMetadata) {
            xml.append(applyTemplate(request, templateType, entry,
                                     subMetadata, parent));
        }

        return xml.toString();
    }



    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     * @param suffix _more_
     * @param value _more_
     * @param forEdit _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getForm(Request request, Entry entry, Metadata metadata,
                          String suffix, String value, boolean forEdit)
            throws Exception {

        if (dataType.equals(DATATYPE_SKIP)) {
            return "";
        }
        String arg = ARG_METADATA_ATTR + getIndex() + suffix;

        value = (((value == null) || (value.length() == 0))
                 ? dflt
                 : value);
        if (isString(dataType)) {
            if (dataType.equals(DATATYPE_WIKI)) {
                String buttons =
                    getRepository().getWikiManager().makeWikiEditBar(request,
                        entry, arg) + HtmlUtils.br();

                return buttons
                       + HtmlUtils.textArea(arg, value, rows, columns,
                                            HtmlUtils.id(arg));
            } else {
                if (rows > 1) {
                    return HtmlUtils.textArea(arg, value, rows, columns);
                }

                return HtmlUtils.input(arg, value,
                                       HtmlUtils.attr(HtmlUtils.ATTR_SIZE,
                                           "" + columns));
            }
           
        } else if (dataType.equals(DATATYPE_COLORTABLE)) {
            List names=StringUtil.split("blues,blue_green_red,white_blue,blue_red,red_white_blue,blue_white_red,grayscale,inversegrayscale,rainbow,nice,blues,gray_scale,inverse_gray_shade,light_gray_scale,blue_green,blue_purple,green_blue,orange_red,purple_blue,purple_blue_green,purple_red,red_purple,yellow_green,yellow_green_blue,yellow_orange_brown,yellow_orange_red,oranges,purples,reds,greens,map_grays,bright38,precipitation,humidity,temperature,visad,inverse_visad,wind_comps,windspeed,dbz,dbz_nws,topographic",",");
            //            List<TwoFacedObject> names = ColorTable.getColorTableNames();
            //            names.add(0, new TwoFacedObject("--none--", ""));
            names.add(0, new TwoFacedObject("--none--", ""));
            return HtmlUtils.select(arg, names, value) +" " + HtmlUtils.href(getRepository().getUrlBase()+"/colortables","View","target=_colortables");
        } else if (dataType.equals(DATATYPE_BOOLEAN)) {
            return HtmlUtils.checkbox(arg, "true",
                                      Misc.equals(value, "true"));
        } else if (dataType.equals(DATATYPE_ENTRY)) {
            return getRepository().getEntryManager().getEntryFormSelect(
                request, entry, arg, value);
        } else if (dataType.equals(DATATYPE_INT)) {
            return HtmlUtils.input(arg, value, HtmlUtils.SIZE_10);
        } else if (dataType.equals(DATATYPE_DOUBLE)) {
            return HtmlUtils.input(arg, value, HtmlUtils.SIZE_10);
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
            return HtmlUtils.select(arg, values, value);
        } else if (dataType.equals(DATATYPE_ENUMERATIONPLUS)) {
            boolean contains = TwoFacedObject.contains(values, value);

            return HtmlUtils.select(arg, values, value) + HtmlUtils.space(2)
                   + msgLabel("Or")
                   + HtmlUtils.input(arg + "_input", (contains
                    ? ""
                    : value), HtmlUtils.SIZE_30);
        } else if (dataType.equals(DATATYPE_FILE)) {
            String image = (forEdit
                            ? getFileHtml(request, entry, metadata, this,
                                          false)
                            : "");
            if (image == null) {
                image = "";
            } else {
                image = "<br>" + image;
            }

            String extra = "";
            if (getThumbnail()) {
                extra = "<br>"
                        + HtmlUtils.checkbox(ARG_THUMBNAIL_SCALEDOWN, "true",
                                             true) + HtmlUtils.space(1)
                                                 + msg("Scale down image");
            }

            return HtmlUtils.fileInput(arg, HtmlUtils.SIZE_70) + image
                   + "<br>" + msgLabel("Or download URL")
                   + HtmlUtils.space(1)
                   + HtmlUtils.input(arg + "_url", "", HtmlUtils.SIZE_70)
                   + extra;
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
                groupSB.append(HtmlUtils.formTable());
                String subArg = arg + "_group" + groupCnt + "_";
                boolean lastOne = ((groupMetadata.size() > 1)
                                   && (groupCnt == groupMetadata.size() - 1));
                if (lastOne) {
                    String newCbx =
                        HtmlUtils.checkbox(subArg + "_new", "true", false)
                        + " " + msg("Click here to add a new record")
                        + HtmlUtils.hidden(subArg + "_lastone", "true");
                    //                    groupSB.append(HtmlUtils.formEntry("",newCbx));
                } else if (hadAny) {
                    //                    groupSB.append(HtmlUtils.formEntry(msgLabel("Delete"),HtmlUtils.checkbox(subArg+"_delete","true",false)));
                }

                for (MetadataElement element : getChildren()) {
                    if ((element.getGroup() != null)
                            && !Misc.equals(element.getGroup(), lastGroup)) {
                        lastGroup = element.getGroup();
                        groupSB.append(
                            HtmlUtils.row(
                                HtmlUtils.colspan(header(lastGroup), 2)));
                    }

                    String elementLbl = element.getName();
                    if (elementLbl.length() > 0) {
                        elementLbl = msgLabel(elementLbl);
                    }
                    String subValue = subMetadata.getAttr(element.getIndex());
                    if (subValue == null) {
                        subValue = "";
                    }
                    String widget = element.getForm(request, entry, metadata,
                                        subArg, subValue, forEdit);
                    if ((widget == null) || (widget.length() == 0)) {
                        continue;
                    }
                    groupSB.append(HtmlUtils.formEntry(elementLbl, widget));
                    groupSB.append(HtmlUtils.hidden(subArg + "_group",
                            "true"));
                }


                groupSB.append(HtmlUtils.formTableClose());

                if (lastOne) {
                    String newCbx = HtmlUtils.checkbox(subArg + "_new",
                                        "true",
                                        false) + HtmlUtils.hidden(subArg
                                        + "_lastone", "true");

                    entriesSB.append(HtmlUtils.makeShowHideBlock(newCbx
                            + " Add New " + subName, groupSB.toString(),
                                false));
                } else {
                    String deleteCbx = ( !hadAny
                                         ? ""
                                         : " - "
                                           + HtmlUtils.checkbox(subArg
                                               + "_delete", "true",
                                                   false) + " "
                                                       + msg("delete"));
                    entriesSB.append(HtmlUtils.makeShowHideBlock((groupCnt
                            + 1) + ") " + subName
                                 + deleteCbx, groupSB.toString(), true));
                }
                groupCnt++;
            }
            sb.append(
                HtmlUtils.makeToggleInline(
                    "",
                    HtmlUtils.div(
                        entriesSB.toString(),
                        HtmlUtils.cssClass("ramadda-metadata-form")), true));

            return sb.toString();
        } else {
            System.err.println("Unknown data type:" + dataType);

            return null;
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
    public void setValues(List<TwoFacedObject> value) {
        values = value;
    }

    /**
     * Get the Values property.
     *
     * @return The Values
     */
    public List<TwoFacedObject> getValues() {
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
