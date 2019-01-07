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
import org.ramadda.repository.util.FileWriter;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import org.w3c.dom.*;


import ucar.unidata.util.Misc;


import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;



/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class MetadataHandler extends RepositoryManager {


    /** _more_ */
    public static String ATTR_FORUSER = "foruser";



    /** _more_ */
    public static String ARG_METADATAID = "metadataid";

    /** _more_ */
    public static String ARG_ENTRYID = "entryid";

    /** _more_ */
    public static String ARG_ATTR1 = "attr1";

    /** _more_ */
    public static String ARG_ATTR2 = "attr2";

    /** _more_ */
    public static String ARG_ATTR3 = "attr3";

    /** _more_ */
    public static String ARG_ATTR4 = "attr4";


    /** _more_ */
    public static final String TYPE_SPATIAL_POLYGON = "spatial.polygon";

    /** _more_ */
    protected Hashtable<String, MetadataType> typeMap = new Hashtable<String,
                                                            MetadataType>();


    /** _more_ */
    private List<MetadataType> metadataTypes = new ArrayList<MetadataType>();

    /** _more_ */
    boolean forUser = true;




    /**
     * _more_
     *
     * @param repository _more_
     */
    public MetadataHandler(Repository repository) {
        super(repository);
    }


    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public MetadataHandler(Repository repository, Element node)
            throws Exception {
        super(repository);
    }






    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public boolean isForEntry(Entry entry) {
        return true;
    }


    /**
     * _more_
     *
     * @param oldEntry _more_
     * @param newEntry _more_
     * @param oldMetadata _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Metadata copyMetadata(Entry oldEntry, Entry newEntry,
                                 Metadata oldMetadata)
            throws Exception {
        Metadata newMetadata = new Metadata(getRepository().getGUID(),
                                            newEntry.getId(), oldMetadata);

        MetadataType type = getType(newMetadata.getType());
        if (type != null) {
            type.initializeCopiedMetadata(oldEntry, newEntry, newMetadata);
        }

        return newMetadata;
    }

    /**
     * _more_
     *
     * @param type _more_
     */
    public void addMetadataType(MetadataType type) {
        type.setHandler(this);
        metadataTypes.add(type);
        typeMap.put(type.getId(), type);
        getMetadataManager().addMetadataType(type);
    }



    /**
     * _more_
     *
     * @param entry _more_
     * @param node _more_
     * @param fileMap _more_
     * @param internal _more_
     *
     * @throws Exception _more_
     */
    public void processMetadataXml(Entry entry, Element node,
                                   Hashtable fileMap, boolean internal)
            throws Exception {
        forUser = XmlUtil.getAttribute(node, ATTR_FORUSER, true);

        String type = XmlUtil.getAttribute(node, ATTR_TYPE);
        //TODO: Handle the extra attributes
        String extra = XmlUtil.getGrandChildText(node, Metadata.TAG_EXTRA,
                           "");
        Metadata metadata = new Metadata(getRepository().getGUID(),
                                         entry.getId(), type,
                                         XmlUtil.getAttribute(node,
                                             ATTR_INHERITED, DFLT_INHERITED));
        int attrIndex = Metadata.INDEX_BASE - 1;
        while (true) {
            attrIndex++;
            if ( !XmlUtil.hasAttribute(node, ATTR_ATTR + attrIndex)) {
                break;
            }
            metadata.setAttr(attrIndex,
                             XmlUtil.getAttribute(node,
                                 ATTR_ATTR + attrIndex, ""));
        }
        metadata.setExtra(extra);

        NodeList children = XmlUtil.getElements(node);
        for (int i = 0; i < children.getLength(); i++) {
            Element childNode = (Element) children.item(i);
            if ( !childNode.getTagName().equals(Metadata.TAG_ATTR)) {
                continue;
            }
            int index = XmlUtil.getAttribute(childNode, Metadata.ATTR_INDEX,
                                             -1);
            String text = XmlUtil.getChildText(childNode);
            if (XmlUtil.getAttribute(childNode, "encoded", true)) {
                text = new String(RepositoryUtil.decodeBase64(text));
            }
            text = metadata.trimToMaxLength(text);
            metadata.setAttr(index, text);
        }

        MetadataType metadataType = findType(type);
        if (metadataType == null) {
            //            System.err.println("Unknown metadata type:" + type);
            throw new IllegalStateException("Unknown metadata type:" + type);
        }
        if ( !metadataType.processMetadataXml(entry, node, metadata, fileMap,
                internal)) {
            return;
        }
        getMetadataManager().addMetadata(entry, metadata);
    }

    /**
     * _more_
     *
     * @param metadata _more_
     * @param entry _more_
     * @param initializer _more_
     *
     * @throws Exception _more_
     */
    public void initNewEntry(Metadata metadata, Entry entry,
                             EntryInitializer initializer)
            throws Exception {
        MetadataType type = getType(metadata.getType());
        type.initNewEntry(metadata, entry, initializer);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param metadata _more_
     * @param forLink _more_
     *
     * @throws Exception _more_
     */
    public void decorateEntry(Request request, Entry entry, Appendable sb,
                              Metadata metadata, boolean forLink)
            throws Exception {
        MetadataType type = getType(metadata.getType());
        if (type == null) {
            return;
        }
        type.decorateEntry(request, entry, sb, metadata, forLink);
    }



    /**
     * _more_
     *
     * @param entry _more_
     * @param sb _more_
     * @param metadata _more_
     *
     * @throws Exception _more_
     */
    public void getTextCorpus(Entry entry, Appendable sb, Metadata metadata)
            throws Exception {
        MetadataType type = getType(metadata.getType());
        if (type == null) {
            return;
        }
        type.getTextCorpus(entry, sb, metadata);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param urls _more_
     * @param metadata _more_
     *
     * @throws Exception _more_
     */
    public void getThumbnailUrls(Request request, Entry entry,
                                 List<String> urls, Metadata metadata)
            throws Exception {
        MetadataType type = getType(metadata.getType());
        if (type == null) {
            return;
        }
        type.getThumbnailUrls(request, entry, urls, metadata);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param nameUrlPairs _more_
     * @param metadata _more_
     *
     * @throws Exception _more_
     */
    public void getFileUrls(Request request, Entry entry,
                            List<String[]> nameUrlPairs, Metadata metadata)
            throws Exception {
        MetadataType type = getType(metadata.getType());
        if (type == null) {
            return;
        }
        String[] nameUrl = type.getFileUrl(request, entry, metadata);
        if (nameUrl != null) {
            nameUrlPairs.add(nameUrl);
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result xxxprocessView(Request request, Entry entry,
                                 Metadata metadata)
            throws Exception {
        return new Result("", "Cannot process view");
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processView(Request request, Entry entry, Metadata metadata)
            throws Exception {
        MetadataType type = getType(metadata.getType());
        if (type == null) {
            return null;
        }

        return type.processView(request, entry, metadata);
    }



    /**
     * _more_
     *
     * @param cols _more_
     *
     * @return _more_
     */
    protected String formEntry(String[] cols) {
        if (cols.length == 2) {
            //            return HtmlUtils.rowTop(HtmlUtils.cols(cols[0])+"<td colspan=2>" + cols[1] +"</td>");
            //            return HtmlUtils.rowTop(HtmlUtils.cols(cols[0])
            //                                   + "<td xxcolspan=2>" + cols[1] + "</td>");
        }
        StringBuffer sb = new StringBuffer();

        sb.append(HtmlUtils.rowTop("<td colspan=2>" + cols[0] + "</td>"));
        for (int i = 1; i < cols.length; i += 2) {
            if (false && (i == 1)) {
                sb.append(
                    HtmlUtils.rowTop(
                        HtmlUtils.cols(cols[0])
                        + "<td class=\"formlabel\" align=right>" + cols[i]
                        + "</td>" + "<td>" + cols[i + 1]));
            } else {
                //                sb.append(HtmlUtils.rowTop("<td></td><td class=\"formlabel\" align=right>" + cols[i] +"</td>" +
                //                                          "<td>" + cols[i+1]));
                sb.append(
                    HtmlUtils.rowTop(
                        "<td class=\"formlabel\" align=right>" + cols[i]
                        + "</td>" + "<td>" + cols[i + 1]));
            }
        }

        return sb.toString();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected String getHandlerGroupName() {
        return "Properties";
    }

    /**
     * _more_
     *
     * @param id _more_
     * @param entryId _more_
     * @param type _more_
     * @param inherited _more_
     * @param attr1 _more_
     * @param attr2 _more_
     * @param attr3 _more_
     * @param attr4 _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public Metadata makeMetadata(String id, String entryId, String type,
                                 boolean inherited, String attr1,
                                 String attr2, String attr3, String attr4,
                                 String extra) {
        MetadataType metadataType = findType(type);
        Metadata metadata = new Metadata(id, entryId, type, inherited, attr1,
                                         attr2, attr3, attr4, extra);
        if (metadataType != null) {
            metadata.setPriority(metadataType.getPriority());
        }

        return metadata;
    }



    /**
     * _more_
     *
     * @param stringType _more_
     *
     * @return _more_
     */
    public MetadataType findType(String stringType) {
        return typeMap.get(stringType);
    }


    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
    public MetadataType getType(String type) {
        return typeMap.get(type);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param metadataList _more_
     * @param extra _more_
     * @param shortForm _more_
     */
    public void getInitialMetadata(Request request, Entry entry,
                                   List<Metadata> metadataList,
                                   Hashtable extra, boolean shortForm) {}



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param fileWriter _more_
     * @param metadata _more_
     * @param node _more_
     *
     * @throws Exception _more_
     */
    public void addMetadata(Request request, Entry entry,
                            FileWriter fileWriter, Metadata metadata,
                            Element node)
            throws Exception {
        MetadataType type = getType(metadata.getType());
        if (type == null) {
            getRepository().getLogManager().logWarning(
                "Unknown metadata type:" + metadata.getType());

            return;
        }

        Document doc = node.getOwnerDocument();
        Element metadataNode = XmlUtil.create(doc, TAG_METADATA, node,
                                   new String[] { ATTR_TYPE,
                metadata.getType(), ATTR_INHERITED,
                "" + metadata.getInherited() });
        for (MetadataElement element : type.getChildren()) {
            int    index = element.getIndex();
            String value = metadata.getAttr(index);
            if (value == null) {
                continue;
            }
            Element attrNode = XmlUtil.create(doc, Metadata.TAG_ATTR,
                                   metadataNode,
                                   new String[] { Metadata.ATTR_INDEX,
                    "" + index });
            //true means to base 64 encode the text
            attrNode.appendChild(XmlUtil.makeCDataNode(doc, value, true));
            if ((fileWriter != null)
                    && element.getDataType().equals(element.DATATYPE_FILE)) {
                File f = type.getFile(entry, metadata, element);
                if ((f == null) || !f.exists()) {
                    continue;
                }
                String fileName = repository.getGUID();
                //metadata.getId() +"_" + index;
                attrNode.setAttribute("fileid", fileName);
                InputStream fis =
                    getStorageManager().getFileInputStream(f.toString());
                fileWriter.writeFile(fileName, fis);
            }
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param xmlType _more_
     * @param entry _more_
     * @param metadata _more_
     * @param doc _more_
     * @param datasetNode _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    public boolean addMetadataToXml(Request request, String xmlType,
                                    Entry entry, Metadata metadata,
                                    Document doc, Element datasetNode)
            throws Exception {
        MetadataType type = getType(metadata.getType());
        if (type == null) {
            return false;
        }

        return type.addMetadataToXml(request, xmlType, entry, metadata,
                                     datasetNode);
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public String getLabel(String s) {
        if (s.length() == 0) {
            return "No label";
        }
        s = s.replace("_", " ");
        s = s.replace(".", " ");
        s = s.substring(0, 1).toUpperCase() + s.substring(1);

        return s;
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String[] getHtml(Request request, Entry entry, Metadata metadata)
            throws Exception {
        MetadataType type = getType(metadata.getType());
        if ((type == null) || !type.hasElements()) {
            return null;
        }

        return type.getHtml(request, entry, metadata);
    }

    /**
     * _more_
     *
     * @param metadata _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean isSimple(Metadata metadata) throws Exception {
        MetadataType type = getType(metadata.getType());
        if ((type == null) || !type.hasElements()) {
            return false;
        }

        return type.isSimple();
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     * @param forEdit _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String[] getForm(Request request, Entry entry, Metadata metadata,
                            boolean forEdit)
            throws Exception {
        MetadataType type = getType(metadata.getType());
        if ((type == null) || !type.hasElements()) {
            return null;
        }
        String suffix = "";
        if (metadata.getId().length() > 0) {
            suffix = "_" + metadata.getId();
        }

        return type.getForm(this, request, entry, metadata, suffix, forEdit);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void makeAddForm(Request request, Entry entry, Appendable sb)
            throws Exception {
        for (MetadataType type : metadataTypes) {
            makeAddForm(request, entry, type, sb);
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param metadata _more_
     *
     * @return _more_
     */
    public String getSearchUrl(Request request, Metadata metadata) {
        MetadataType type = findType(metadata.getType());
        if (type == null) {
            return null;
        }

        return type.getSearchUrl(request, metadata);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param type _more_
     * @param value _more_
     *
     * @return _more_
     */
    public String getSearchUrl(Request request, MetadataType type,
                               String value) {
        List args = new ArrayList();
        //        args.add(ARG_METADATA_TYPE + "_" + type.getId());
        //        args.add(type.toString());
        args.add(ARG_METADATA_ATTR1 + "_" + type.getId());
        args.add(value);

        return HtmlUtils.url(
            request.makeUrl(
                getRepository().getSearchManager().URL_ENTRY_SEARCH), args);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param metadata _more_
     *
     * @return _more_
     */
    public String getSearchLink(Request request, Metadata metadata) {
        return HtmlUtils.href(
            getSearchUrl(request, metadata),
            HtmlUtils.img(
                getRepository().getIconUrl(ICON_SEARCH_SMALL),
                "Search for entries with this metadata", " border=0 "));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param type _more_
     *
     * @throws Exception _more_
     */
    public void addToSearchForm(Request request, Appendable sb,
                                MetadataType type)
            throws Exception {
        boolean doSelect = true;
        String  argName  = ARG_METADATA_ATTR1 + "_" + type;
        if (doSelect) {
            String[] values = getMetadataManager().getDistinctValues(request,
                                  this, type);
            if ((values == null) || (values.length == 0)) {
                return;
            }

            List<TwoFacedObject> existingValues =
                trimValues((List<String>) Misc.toList(values));
            List<TwoFacedObject> selectList = new ArrayList<TwoFacedObject>();
            selectList.add(new TwoFacedObject("-" + msg("all") + "-", ""));
            MetadataElement element    = type.getChildren().get(0);
            List            enumValues = element.getValues();
            if (enumValues == null) {
                enumValues = new ArrayList();
            }
            if (enumValues != null) {
                for (TwoFacedObject o : existingValues) {
                    TwoFacedObject tfo = TwoFacedObject.findId(o.getId(),
                                             enumValues);
                    if (tfo != null) {
                        selectList.add(tfo);
                    } else {
                        selectList.add(o);
                    }
                }
                String value = request.getString(argName, "");
                String size  = (selectList.size() >= 4)
                               ? HtmlUtils.attr(HtmlUtils.ATTR_SIZE, "6")
                               : "";
                sb.append(HtmlUtils.formEntry(msgLabel(type.getLabel()),
                        HtmlUtils.select(argName, selectList, value,
                                         size + HtmlUtils.ATTR_MULTIPLE,
                                         100)));
            }
        } else {
            sb.append(HtmlUtils.formEntry(msgLabel(type.getLabel()),
                                          HtmlUtils.input(argName, "")));
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param type _more_
     * @param titles _more_
     * @param contents _more_
     *
     * @throws Exception _more_
     */
    public void addToBrowseSearchForm(Request request, StringBuffer sb,
                                      MetadataType type, List<String> titles,
                                      List<String> contents)
            throws Exception {

        boolean doSelect = true;
        String cloudLink =
            HtmlUtils.href(
                request.makeUrl(
                    getRepository().getMetadataManager().URL_METADATA_LIST,
                    ARG_METADATA_TYPE, type.toString()), HtmlUtils.img(
                        getRepository().getIconUrl(ICON_LIST),
                        "View Listing"));

        cloudLink = HtmlUtils.href(
            request.makeUrl(
                getRepository().getMetadataManager().URL_METADATA_LIST,
                ARG_METADATA_TYPE, type.toString()), msg("View Listing"));

        String url = request.makeUrl(
                         getRepository().getSearchManager().URL_ENTRY_SEARCH);
        String[] values = getMetadataManager().getDistinctValues(request,
                              this, type);
        if ((values == null) || (values.length == 0)) {
            return;
        }
        StringBuffer content = new StringBuffer();
        content.append(cloudLink);
        content.append(HtmlUtils.p());
        content.append(HtmlUtils.h3(msg("Search")));
        content.append("<div class=\"browseblock\">");
        int rowNum = 1;
        for (int i = 0; i < values.length; i++) {
            String browseUrl = HtmlUtils.url(url,
                                             ARG_METADATA_TYPE + "_"
                                             + type.getId(), type.getId(),
                                                 ARG_METADATA_ATTR1 + "_"
                                                 + type.getId(), values[i]);
            String value = values[i].trim();
            if (value.length() == 0) {
                value = "-blank-";
            }
            content.append(HtmlUtils.div(HtmlUtils.href(browseUrl, value),
                                         HtmlUtils.cssClass("listrow"
                                             + rowNum)));
            rowNum++;
            if (rowNum > 2) {
                rowNum = 1;
            }
        }
        content.append("</div>");

        titles.add(type.getLabel());
        contents.add(content.toString());
        sb.append(HtmlUtils.makeShowHideBlock(type.getLabel(),
                content.toString(), false));


    }

    /**
     * _more_
     *
     * @param l _more_
     *
     * @return _more_
     */
    protected List<TwoFacedObject> trimValues(List<String> l) {
        List<TwoFacedObject> values = new ArrayList();
        for (String s : l) {
            String label = s;
            if (label.length() > 50) {
                label = label.substring(0, 49) + "...";
            }
            values.add(new TwoFacedObject(label, s));
        }

        return values;
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param type _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void makeAddForm(Request request, Entry entry, MetadataType type,
                            Appendable sb)
            throws Exception {
        if (type == null) {
            return;
        }
        Metadata metadata = new Metadata(type);
        metadata.setEntry(entry);
        String[] html = getForm(request, entry, metadata, false);
        if (html == null) {
            return;
        }
        if (entry != null) {
            request.uploadFormWithAuthToken(
                sb, getMetadataManager().URL_METADATA_ADD);
            sb.append("\n");
            sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
            sb.append("\n");
            sb.append(HtmlUtils.formTable());
            sb.append("\n");
        } else {
            sb.append("\n");
            sb.append(HtmlUtils.formTable());
            sb.append("\n");
            sb.append(HtmlUtils.row(HtmlUtils.colspan(header(html[0]), 2)));
        }


        sb.append("\n");
        sb.append(html[1]);
        sb.append("\n");
        sb.append("\n");
        sb.append(HtmlUtils.formTableClose());
        sb.append("\n");

        if (entry != null) {
            sb.append(HtmlUtils.formClose());
            sb.append("\n");
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void makeSearchForm(Request request, StringBuffer sb)
            throws Exception {
        for (MetadataType type : metadataTypes) {
            //            makeAddForm(entry, types.get(i).toString(), sb);
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param metadataList _more_
     *
     * @throws Exception _more_
     */
    public void handleAddSubmit(Request request, Entry entry,
                                List<Metadata> metadataList)
            throws Exception {
        String id = getRepository().getGUID();
        handleForm(request, entry, id, "", null, metadataList, true);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param existingMetadata _more_
     * @param metadataList _more_
     *
     * @throws Exception _more_
     */
    public void handleFormSubmit(
            Request request, Entry entry,
            Hashtable<String, Metadata> existingMetadata,
            List<Metadata> metadataList)
            throws Exception {
        Hashtable args = request.getArgs();
        for (Enumeration keys = args.keys(); keys.hasMoreElements(); ) {
            String arg = (String) keys.nextElement();
            if ( !arg.startsWith(ARG_METADATAID + "_")) {
                continue;
            }
            String id     = request.getString(arg, "");
            String suffix = "_" + id;
            handleForm(request, entry, id, suffix, existingMetadata,
                       metadataList, false);
        }
    }


    /**
     * _more_
     *
     * @param element _more_
     *
     * @return _more_
     */
    public String getEnumerationValues(MetadataElement element) {
        return "";
    }

    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
    public boolean canHandle(String type) {
        return typeMap.get(type) != null;
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param id _more_
     * @param suffix _more_
     * @param existingMetadata _more_
     * @param metadataList _more_
     * @param newMetadata _more_
     *
     * @throws Exception _more_
     */
    public void handleForm(Request request, Entry entry, String id,
                           String suffix,
                           Hashtable<String, Metadata> existingMetadata,
                           List<Metadata> metadataList, boolean newMetadata)
            throws Exception {

        String type = request.getString(ARG_METADATA_TYPE + suffix, "");
        if ( !canHandle(type)) {
            return;
        }
        MetadataType metadataType = getType(type);
        if (metadataType == null) {
            return;
        }

        Metadata metadata = metadataType.handleForm(request, entry, id,
                                suffix, ((existingMetadata == null)
                                         ? null
                                         : existingMetadata.get(
                                             id)), newMetadata);
        if (metadata != null) {
            metadataList.add(metadata);
        }
    }




    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
    public String getFormHtml(String type) {
        return null;
    }







    /**
     *  Set the ForUser property.
     *
     *  @param value The new value for ForUser
     */
    public void setForUser(boolean value) {
        forUser = value;
    }

    /**
     *  Get the ForUser property.
     *
     *  @return The ForUser
     */
    public boolean getForUser() {
        return forUser;
    }



}
