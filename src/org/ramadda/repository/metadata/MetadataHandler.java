/**
Copyright (c) 2008-2024 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.metadata;


import org.ramadda.repository.*;
import org.ramadda.repository.util.FileWriter;
import org.ramadda.util.FormInfo;

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



@SuppressWarnings("unchecked")
public class MetadataHandler extends RepositoryManager {
    public static String ATTR_FORUSER = "foruser";
    public static String ARG_METADATAID = "metadataid";
    public static String ARG_ENTRYID = "entryid";
    public static String ARG_ATTR1 = "attr1";
    public static String ARG_ATTR2 = "attr2";
    public static String ARG_ATTR3 = "attr3";
    public static String ARG_ATTR4 = "attr4";
    public static final String TYPE_SPATIAL_POLYGON = "spatial.polygon";
    protected Hashtable<String, MetadataType> typeMap = new Hashtable<String,
                                                            MetadataType>();

    private List<MetadataType> metadataTypes = new ArrayList<MetadataType>();
    boolean forUser = true;

    
    public MetadataHandler(Repository repository) {
        super(repository);
    }


    
    public MetadataHandler(Repository repository, Element node) {
        super(repository);
    }






    
    public boolean isForEntry(Entry entry) {
        return true;
    }


    public String getTag(Request request, Metadata metadata) {
	String mtd = metadata.getAttr(1);
	MetadataType type = metadata.getMetadataType();
	String group =type.getDisplayGroup();
	if(group==null) group= type.getLabel();

	return HU.div(mtd,HU.attrs("style",type.getTagStyle(),
				   "metadata-group",group,
				   "metadata-label",type.getLabel(),
				   "class","metadata-tag","metadata-tag",mtd));
    }

    
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

    
    public void addMetadataType(MetadataType type) {
        type.setHandler(this);
        metadataTypes.add(type);
        typeMap.put(type.getId(), type);
        getMetadataManager().addMetadataType(type);
    }




    public void processMetadataXml(Request request,Entry entry, Element node,
                                   Hashtable filesMap, EntryManager.INTERNAL isInternal)
            throws Exception {
        forUser = XmlUtil.getAttribute(node, ATTR_FORUSER, true);

        String type = XmlUtil.getAttribute(node, ATTR_TYPE);
        //TODO: Handle the extra attributes
        String extra = XmlUtil.getGrandChildText(node, Metadata.TAG_EXTRA,
                           "");
        String id = getRepository().getGUID();
        if (isInternal==EntryManager.INTERNAL.NO) {
            id = XmlUtil.getAttribute(node, "id", id);
        }
        Metadata metadata = new Metadata(id, entry.getId(), getMetadataManager().findType(type),
                                         XmlUtil.getAttribute(node,
                                             ATTR_INHERITED, DFLT_INHERITED));
        String access = XmlUtil.getGrandChildText(node, "access", null);
	if(access!=null) {
	    metadata.setAccess(access);
	}
	metadata.setMetadataType(getMetadataManager().findType(type));

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
            int index = XmlUtil.getAttribute(childNode, Metadata.ATTR_INDEX, -1);
            String text = XmlUtil.getChildText(childNode);
            if (XmlUtil.getAttribute(childNode, "encoded", true)) {
                text = new String(Utils.decodeBase64(text));
            }
            text = metadata.trimToMaxLength(text);
            metadata.setAttr(index, text);
        }

        MetadataType metadataType = findType(type);
        if (metadataType == null) {
            //            System.err.println("Unknown metadata type:" + type);
            throw new IllegalStateException("Unknown metadata type:" + type);
        }
        if ( !metadataType.processMetadataXml(entry, node, metadata, filesMap,
					      isInternal)) {
            return;
        }
        getMetadataManager().addMetadata(request,entry, metadata);
    }

    
    public void initNewEntry(Metadata metadata, Entry entry,
                             EntryInitializer initializer)
            throws Exception {
        MetadataType type = getType(metadata.getType());
        type.initNewEntry(metadata, entry, initializer);
    }



    
    public void decorateEntry(Request request, Entry entry, Appendable sb,
                              Metadata metadata, boolean forLink, boolean fileOk)
            throws Exception {
        MetadataType type = getType(metadata.getType());
        if (type == null) {
            return;
        }
        type.decorateEntry(request, entry, sb, metadata, forLink,fileOk);
    }



    
    public void getTextCorpus(Entry entry, Appendable sb, Metadata metadata)
            throws Exception {
        MetadataType type = getType(metadata.getType());
        if (type == null) {
            return;
        }
        type.getTextCorpus(entry, sb, metadata);
    }


    
    public void getThumbnailUrls(Request request, Entry entry,
                                 List<String[]> urls, Metadata metadata)
            throws Exception {
        MetadataType type = getType(metadata.getType());
        if (type == null) {
            return;
        }
        type.getThumbnailUrls(request, entry, urls, metadata);
    }

    
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


    
    public Result xxxprocessView(Request request, Entry entry,
                                 Metadata metadata)
            throws Exception {
        return new Result("", "Cannot process view");
    }


    
    public Result processView(Request request, Entry entry, Metadata metadata)
            throws Exception {
        MetadataType type = getType(metadata.getType());
        if (type == null) {
            return null;
        }

        return type.processView(request, entry, metadata);
    }



    
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


    
    protected String getHandlerGroupName() {
        return "Properties";
    }

    
    public Metadata makeMetadata(String id, String entryId, String type,
                                 boolean inherited, String access,String attr1,
                                 String attr2, String attr3, String attr4,
                                 String extra) {

        MetadataType metadataType = findType(type);
        Metadata metadata = new Metadata(id, entryId, metadataType, inherited, attr1,
                                         attr2, attr3, attr4, extra);
	metadata.setAccess(access);
        if (metadataType != null) {
            metadata.setPriority(metadataType.getPriority());
        }

        return metadata;
    }



    
    public MetadataType findType(String stringType) {
        MetadataType type  = typeMap.get(stringType);
	if(type==null) {
	    //	    System.err.println("Could not find type:" + stringType);
	    return getMetadataManager().findType(stringType);
	}
	return type;
    }


    
    public MetadataType getType(String type) {
        return typeMap.get(type);
    }


    
    public void getInitialMetadata(Request request, Entry entry,
                                   List<Metadata> metadataList,
                                   Hashtable extra, boolean shortForm) {}



    
    public void addMetadata(Request request, Entry entry,
                            FileWriter fileWriter, Metadata metadata,
                            Element node,boolean encode)
            throws Exception {
        MetadataType type = getType(metadata.getType());
        if (type == null) {
            getRepository().getLogManager().logWarning(
                "Unknown metadata type:" + metadata.getType());

            return;
        }

	if(!type.getCanView()) return;


        Document doc = node.getOwnerDocument();
        Element metadataNode = XmlUtil.create(doc, TAG_METADATA, node,
                                   new String[] {
            "id", metadata.getId(), ATTR_TYPE, metadata.getType(),
            ATTR_INHERITED, "" + metadata.getInherited()
        });
	String access=metadata.getAccess();
	if(stringDefined(access)) {
            Element accessNode = XmlUtil.create(doc, "access", metadataNode);
	    accessNode.appendChild(XmlUtil.makeCDataNode(doc, access, false));
	}

        for (MetadataElement element : type.getChildren()) {
            int    index = element.getIndex();
            String value = element.getValueForExport(request, entry, type, metadata);
            if (value == null) {
                continue;
            }
            Element attrNode = XmlUtil.create(doc, Metadata.TAG_ATTR,
                                   metadataNode,
					      encode?
                                   new String[] { Metadata.ATTR_INDEX,
						  "" + index}:
                                   new String[] { Metadata.ATTR_INDEX,
						  "" + index,"encoded","false"});					      
            //true means to base 64 encode the text
            attrNode.appendChild(XmlUtil.makeCDataNode(doc, value, encode));
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

    
    public String getLabel(String s) {
        if (s.length() == 0) {
            return "No label";
        }
        s = s.replace("_", " ");
        s = s.replace(".", " ");
        s = s.substring(0, 1).toUpperCase() + s.substring(1);

        return s;
    }


    
    public String[] getHtml(Request request, Entry entry, Metadata metadata)
            throws Exception {
        MetadataType type = getType(metadata.getType());
        if ((type == null) || !type.hasElements()) {
            return null;
        }

        return type.getHtml(request, entry, metadata);
    }

    
    public boolean isSimple(Metadata metadata) throws Exception {
        MetadataType type = getType(metadata.getType());
        if ((type == null) || !type.hasElements()) {
            return false;
        }

        return type.isSimple();
    }


    
    public String[] getForm(Request request, FormInfo formInfo, Entry entry,
                            Metadata metadata, boolean forEdit)
            throws Exception {
        MetadataType type = getType(metadata.getType());
        if ((type == null) || !type.hasElements()) {
            return null;
        }
        String suffix = "";
        if (metadata.getId().length() > 0) {
            suffix = "_" + metadata.getId();
        }

        return type.getForm(this, request, formInfo, entry, metadata, suffix,
                            forEdit);
    }


    
    public void makeAddForm(Request request, Entry entry, Appendable sb)
            throws Exception {
        for (MetadataType type : metadataTypes) {
            makeAddForm(request, entry, type, sb);
        }
    }


    
    public String getSearchUrl(Request request, Metadata metadata) {
        MetadataType type = findType(metadata.getType());
        if (type == null) {
            return null;
        }

        return type.getSearchUrl(request, metadata);
    }

    
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

    public String getSearchLink(Request request, Metadata metadata, String text) {
        return HtmlUtils.href(
            getSearchUrl(request, metadata),
             text,
	    HU.attr("title","Search for entries with this metadata") +
	    HU.cssClass("metadata-search-link ramadda-clickable"));
    }
    
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
            List selectList = new ArrayList();
            selectList.add(new TwoFacedObject("-" + msg("all") + "-", ""));
            MetadataElement element    = type.getChildren().get(0);
            List            enumValues = element.getValues();
            if (enumValues == null) {
                enumValues = new ArrayList();
            }
            if (enumValues != null) {
                for (TwoFacedObject o : existingValues) {
                    Object selected = findId(o.getId(), enumValues);
                    if (selected != null) {
                        selectList.add(selected);
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


    private Object findId(Object lookFor, List enums) {
	for(Object o:enums) {
	    if(o instanceof TwoFacedObject) {
		if(((TwoFacedObject)o).getId().equals(lookFor))return o;
	    }
	    if(o instanceof HtmlUtils.Selector) {
		if(((HtmlUtils.Selector)o).getId().equals(lookFor))return o;
	    }	    
	    if(lookFor.equals(o)) return o;
	}
	return null;
    }


    
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
        int          rowNum = 1;
        List<String> rows   = new ArrayList<String>();
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
            rows.add(HtmlUtils.div(HtmlUtils.href(browseUrl, value),
                                   HtmlUtils.cssClass("listrow" + rowNum)));
            rowNum++;
            if (rowNum > 2) {
                rowNum = 1;
            }
        }

        List<List> lists = Utils.splitList(rows, 5);
        for (List row : lists) {
            content.append("<div class=\"browseblock\">");
            content.append(Utils.join(row, ""));
            content.append("</div>");
        }
        titles.add(type.getLabel());
        contents.add(content.toString());
        sb.append(HtmlUtils.makeShowHideBlock(type.getLabel(),
                content.toString(), false));


    }

    
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


    
    public void makeAddForm(Request request, Entry entry, MetadataType type,
                            Appendable sb)
            throws Exception {
        if (type == null) {
            return;
        }
        Metadata metadata = new Metadata(type);
        metadata.setEntry(entry);
        String   formId   = HU.getUniqueId("metadata_");
        FormInfo formInfo = new FormInfo(formId);
        String[] html     = getForm(request, formInfo, entry, metadata,
                                    false);
        if (html == null) {
            return;
        }
        if (entry != null) {
            request.uploadFormWithAuthToken(
                sb, getMetadataManager().URL_METADATA_ADD,
                HU.attr("name", "metadataform") + HU.id(formId));
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
            formInfo.addToForm(sb);
            sb.append(HtmlUtils.formClose());
            sb.append("\n");
        }
    }



    
    public void makeSearchForm(Request request, StringBuffer sb)
            throws Exception {
        for (MetadataType type : metadataTypes) {
            //            makeAddForm(entry, types.get(i).toString(), sb);
        }
    }



    
    public void handleAddSubmit(Request request, Entry entry,
                                List<Metadata> metadataList)
            throws Exception {
        String id = getRepository().getGUID();
        handleForm(request, entry, id, "", null, metadataList, true);
    }


    
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


    
    public String getEnumerationValues(MetadataElement element) {
        return "";
    }

    
    public boolean canHandle(String type) {
        return typeMap.get(type) != null;
    }




    
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




    
    public String getFormHtml(String type) {
        return null;
    }







    
    public void setForUser(boolean value) {
        forUser = value;
    }

    
    public boolean getForUser() {
        return forUser;
    }



}
