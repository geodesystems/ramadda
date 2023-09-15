/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.metadata;


import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.FormInfo;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.sql.SqlUtil;


import org.w3c.dom.*;


import ucar.unidata.ui.ImageUtils;
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


/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public class MetadataType extends MetadataTypeBase implements Comparable {

    /** _more_ */
    public static final String TAG_TYPE = "type";

    /** _more_ */
    public static final String TAG_TEMPLATE = "template";


    /** _more_ */
    public static final String TAG_HANDLER = "handler";


    /** _more_ */
    public static final String ATTR_METADATATYPE = "metadatatype";



    /** _more_ */
    public static final String ATTR_CLASS = "class";

    /** _more_ */
    public static final String ATTR_MAKEDATABASE = "makedatabase";


    /** _more_ */
    public static final String ATTR_HANDLER = "handler";

    /** _more_ */
    public static final String ATTR_ID = "id";

    /** _more_ */
    public static final String ATTR_PRIORITY = "priority";

    /** _more_ */
    public static final String ATTR_ADMINONLY = "adminonly";

    /** _more_ */
    public static final String ATTR_FORUSER = "foruser";

    /** _more_ */
    public static final String ATTR_ENTRYTYPE = "entrytype";



    /** _more_ */
    public static final String ATTR_DISPLAYCATEGORY = "displaycategory";

    /** _more_ */
    public static final String ATTR_CATEGORY = "category";

    /** _more_ */
    public static final String ATTR_DISPLAYGROUP = "displaygroup";


    /** _more_ */
    public static final String ATTR_BROWSABLE = "browsable";



    /** _more_ */
    public static final String ATTR_ = "";


    /** _more_ */
    public static final String PROP_METADATA_LABEL = "metadata.label";

    /** _more_ */
    public static String ARG_METADATAID = "metadataid";

    /** _more_ */
    private String id;

    /** _more_ */
    private int priority = 1000;

    /** _more_ */
    private boolean makeDatabaseTable = false;

    /** _more_ */
    private List<Column> databaseColumns;

    /** _more_ */
    private String displayCategory = "Properties";

    /** _more_ */
    private String displayGroup = null;

    /** _more_ */
    private String category = "Properties";


    /** _more_ */
    private boolean adminOnly = false;


    private boolean canView = true;        
    


    /** _more_ */
    private boolean browsable = false;

    /** _more_ */
    private boolean forUser = true;

    /** _more_ */
    private String entryType = null;

    /** _more_ */
    private String help = "";

    /**
     * _more_
     *
     *
     * @param id _more_
     * @param handler _more_
     */
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


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public boolean isForEntry(Entry entry) {
        if (entryType != null) {
            return entry.getTypeHandler().isType(entryType);
        }

        return getHandler().isForEntry(entry);
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return id;
    }


    /**
     * _more_
     *
     * @param root _more_
     * @param manager _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static List<MetadataType> parse(Element root,
                                           MetadataManager manager)
            throws Exception {
        List<MetadataType> types = new ArrayList<MetadataType>();
        parse(root, manager, types);

        return types;
    }

    /**
     * _more_
     *
     * @param root _more_
     * @param manager _more_
     * @param types _more_
     *
     * @throws Exception _more_
     */
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



            String          id           = XmlUtil.getAttribute(node,
                                               ATTR_ID);
            MetadataHandler handler      = manager.getHandler(c);
            MetadataType    metadataType = new MetadataType(id, handler);
            metadataType.help = Utils.getAttributeOrTag(node, ATTR_HELP, "");
            metadataType.init(node);
            handler.addMetadataType(metadataType);
            types.add(metadataType);
        }
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
        setAdminOnly(XmlUtil.getAttributeFromTree(node, ATTR_ADMINONLY,
                false));
	canView = XmlUtil.getAttributeFromTree(node, "canview", true);

        setForUser(XmlUtil.getAttributeFromTree(node, ATTR_FORUSER, true));
        entryType = XmlUtil.getAttributeFromTree(node, ATTR_ENTRYTYPE,
                (String) null);

        priority = XmlUtil.getAttributeFromTree(node, ATTR_PRIORITY,
                priority);

        setBrowsable(XmlUtil.getAttributeFromTree(node, ATTR_BROWSABLE,
                false));

        setDisplayCategory(XmlUtil.getAttributeFromTree(node,
                ATTR_DISPLAYCATEGORY, "Properties"));


        setDisplayGroup(XmlUtil.getAttributeFromTree(node, ATTR_DISPLAYGROUP,
                (String) null));
        setCategory(XmlUtil.getAttributeFromTree(node, ATTR_CATEGORY,
                handler.getHandlerGroupName()));

        makeDatabaseTable = XmlUtil.getAttributeFromTree(node,
                ATTR_MAKEDATABASE, false);
        /*
        if makeDatabaseTable) {
            initDatabase();
        }
        */
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


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void initDatabase() throws Exception {
        Statement statement = getDatabaseManager().createStatement();
        databaseColumns = new ArrayList<Column>();
        int          cnt       = 0;
        final String tableName = getDbTableName();
        System.err.println("Making db:" + tableName);
        TypeHandler typeHandler = new TypeHandler(getRepository()) {
            @Override
            public String getTableName() {
                return tableName;
            }
        };
        String dropTable = "DROP TABLE " + tableName;
        try {
            getDatabaseManager().executeAndClose(dropTable);
        } catch (Throwable exc) {
            //      System.err.println("Error dropping table:" + exc+"\n"+  dropTable);
            //      throw new RuntimeException(exc);
        }


        StringBuffer tableDef = new StringBuffer("CREATE TABLE " + tableName
                                    + " (\n");
        tableDef.append(
            "id varchar(200), entry_id varchar(200), type varchar(200), inherited int)");
        System.out.println("Creating metadata table:" + tableName);
        try {
            getDatabaseManager().executeAndClose(tableDef.toString());
        } catch (Throwable exc) {
            if (exc.toString().indexOf("already exists") < 0) {
                System.err.println("Error creating metadata db table:" + exc
                                   + "\n" + tableDef);

                throw new RuntimeException(exc);
            }
        }

        StringBuffer indexDef = new StringBuffer();
        indexDef.append("CREATE INDEX " + tableName + "_INDEX_" + "id"
                        + "  ON " + tableName + " (" + "id" + ");\n");
        indexDef.append("CREATE INDEX " + tableName + "_INDEX_" + "entry_id"
                        + "  ON " + tableName + " (" + "entry_id" + ");\n");
        indexDef.append("CREATE INDEX " + tableName + "_INDEX_" + "type"
                        + "  ON " + tableName + " (" + "type" + ");\n");

        try {
            getDatabaseManager().loadSql(indexDef.toString(), true, false);
        } catch (Throwable exc) {
            //TODO:
            System.err.println("Error creating metadata index:" + exc + "\n"
                               + indexDef);

            throw new RuntimeException(exc);
        }

        for (MetadataElement element : getChildren()) {
            String dataType   = element.getDataType();
            String columnName = element.getId();
            if (columnName == null) {
                columnName = element.getName();
            }
            if (columnName == null) {
                throw new RuntimeException(
                    "No name defined for metadata element:" + element);
            }
            columnName = getDbColumnName(columnName);
            System.out.println("\tcolumn:" + columnName + " type:"
                               + dataType);
            int size = XmlUtil.getAttribute(element.getXmlNode(),
                                            Column.ATTR_SIZE, 1000);
            if (dataType == null) {
                dataType = DataTypes.DATATYPE_STRING;
            }

            Column column = new Column(typeHandler, columnName, dataType,
                                       cnt);
            column.setSize(size);
            //false-> don't ignore errors
            try {
                column.createTable(statement, false);
            } catch (Throwable exc) {
                System.err.println("Error:" + exc);

                return;
            }
            databaseColumns.add(column);
            cnt++;
        }
        getDatabaseManager().closeAndReleaseConnection(statement);
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
        for (MetadataElement element : getChildren()) {
            if (element.getDataType().equals(element.DATATYPE_FILE)) {
                String fileArg = metadata.getAttr(element.getIndex());
                if ((fileArg == null) || (fileArg.length() == 0)) {
                    continue;
                }
                File sourceFile = new File(fileArg);
                if ( !sourceFile.exists()) {
                    if (initializer != null) {
                        sourceFile = initializer.getMetadataFile(entry,
                                fileArg);
                    }
                }
                if ((sourceFile == null) || !sourceFile.exists()) {
                    throw new IllegalArgumentException(
                        "Missing metadata file:" + fileArg);
                }
                if ( !entry.getIsLocalFile()) {
                    fileArg = getStorageManager().copyToEntryDir(entry,
                            sourceFile).getName();
                }
                metadata.setAttr(element.getIndex(), fileArg);
            }
        }
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param node _more_
     * @param metadata _more_
     * @param fileMap _more_
     * @param internal _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean processMetadataXml(Entry entry, Element node,
                                      Metadata metadata, Hashtable fileMap,
                                      boolean internal)
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
                    + fileMap);

                continue;
            }

            fileArg = fileArg.trim();
            if (fileArg.length() == 0) {
                continue;
            }
            //            System.err.println ("metadata file:"+ fileArg);
            String fileName = null;
            File   tmpFile  = (File) fileMap.get(fileArg);
            if (tmpFile == null) {
                File root = (File) fileMap.get("root");
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

            if ((tmpFile == null) && internal) {
                tmpFile = new File(fileArg);
            }

            if (tmpFile == null) {
                try {
                    //See if its a URL
                    URL testUrl = new URL(fileArg);
                    continue;
                } catch (Exception ignore) {
                    handler.getRepository().getLogManager().logError(
                        "No attachment uploaded file:" + fileArg);
                    handler.getRepository().getLogManager().logError(
                        "available files: " + fileMap);

                    return false;
                }
            }

            if ((tmpFile != null) && tmpFile.exists()) {
                File file = new File(tmpFile.toString());
                //                System.err.println("Copying:" + metadata.getAttr(element.getIndex()));
                fileName = getStorageManager().copyToEntryDir(entry, file,
                        metadata.getAttr(element.getIndex())).getName();
            }
            //            System.err.println("metadata for: " + entry +" new file:" + fileName);
            metadata.setAttr(element.getIndex(), fileName);
        }

        return true;

    }



    /**
     *  _more_
     *
     *  @param request _more_
     *  @param entry _more_
     *  @param id _more_
     *  @param suffix _more_
     * @param oldMetadata _more_
     *  @param newMetadata _more_
     *
     *
     * @return _more_
     *  @throws Exception _more_
     */
    public Metadata handleForm(Request request, Entry entry, String id,
                               String suffix, Metadata oldMetadata,
                               boolean newMetadata)
            throws Exception {
        boolean inherited = request.get(ARG_METADATA_INHERITED + suffix,
                                        false);
        Metadata metadata = new Metadata(id, entry.getId(), getId(),
                                         inherited);
        for (MetadataElement element : getChildren()) {
            String value = element.handleForm(request, entry, metadata,
                               oldMetadata, suffix);
            metadata.setAttr(element.getIndex(), value);
        }

        return metadata;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param templateType _more_
     * @param entry _more_
     * @param metadata _more_
     * @param parent _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
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
            root = XmlUtil.getRoot(new ByteArrayInputStream(xml.getBytes()));
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
        decorateEntry(request, entry, sb, metadata, forLink, false);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param metadata _more_
     * @param forLink _more_
     * @param isThumbnail _more_
     *
     * @throws Exception _more_
     */
    public void decorateEntry(Request request, Entry entry, Appendable sb,
                              Metadata metadata, boolean forLink,
                              boolean isThumbnail)
            throws Exception {
        for (MetadataElement element : getChildren()) {
            if ( !element.getDataType().equals(element.DATATYPE_FILE)) {
                continue;
            }
            if ( !element.showAsAttachment()) {
                continue;
            }
            if (element.getThumbnail() || isThumbnail) {
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


    /**
     * _more_
     *
     * @param oldEntry _more_
     * @param newEntry _more_
     * @param newMetadata _more_
     *
     * @throws Exception _more_
     */
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
                String url = getImageUrl(request, entry, metadata, null);
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
                    urls.add(new String[]{url,title});
                }
            }
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     * @param filter _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
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
            String url = getImageUrl(request, entry, metadata, filter);
            if (url != null) {
                return url;
            }
        }

        return null;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     * @param filter _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
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
            String url = getImageUrl(request, entry, metadata, filter);
            if (url != null) {
                return element;
            }
        }

        return null;
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
        int elementIndex = request.get(ARG_ELEMENT, 0) - 1;
        if ((elementIndex < 0) || (elementIndex >= getChildren().size())) {
            return new Result("", "Cannot process view");
        }
        MetadataElement element = getChildren().get(elementIndex);

        return processView(request, entry, metadata, element);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     * @param element _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
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
                    Image image = Utils.readImage(f.toString());
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






    /**
     * _more_
     *
     * @param request _more_
     * @param metadata _more_
     *
     * @return _more_
     */
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
        for (MetadataElement element : getChildren()) {
            String value = metadata.getAttr(element.getIndex());
            element.getTextCorpus(value, sb);
        }
    }

    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
    public String getTemplate(String type) {
        String template = super.getTemplate(type);
        if (template != null) {
            return template;
        }

        return getMetadataManager().getTemplate(id, type);
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
    public String[] getHtml(Request request, Entry entry, Metadata metadata)
            throws Exception {
        if ( !getShowInHtml()) {
            return null;
        }
        StringBuffer content = new StringBuffer();
        boolean smallDisplay = request.getString(ARG_DISPLAY,
                                   "").equals(DISPLAY_SMALL);
        String nameString   = getTypeLabel(metadata);
        String searchLink = "";
        String lbl          = smallDisplay
                              ? msg(nameString)
                              : msgLabel(nameString);

	boolean makeSearchLink =  !smallDisplay && getSearchable();



        String htmlTemplate = getTemplate(TEMPLATETYPE_HTML);
        if (htmlTemplate != null) {
            String html = getRepository().getPageHandler().applyBaseMacros(
                              htmlTemplate);
            for (MetadataElement element : getChildren()) {
                String value = metadata.getAttr(element.getIndex());
                if (value == null) {
                    value = "";
                }
                value = value.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\"", "&quot;");
                html  = applyMacros(html, element, value);
            }
	    if(makeSearchLink) {
		searchLink = handler.getSearchLink(request, metadata,"")
		    + HU.space(1);
		content.append(searchLink);
	    }
            content.append(html);
        } else {
            int                   cnt      = 1;
            boolean               didOne   = false;
            List<MetadataElement> children = getChildren();
            content.append(HU.formTable());


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

                    if ( !element.isGroup() && (children.size() == 1)) {
                        content.append(
                            HU.row(
                                HU.colspan(metadataHtml, 2)));
                    } else {
                        content.append(HU.formEntry(formInfo.label,
                                metadataHtml));
                    }
                    didOne = true;
                }
                cnt++;
                content.append("\n");
            }
            content.append(HU.formTableClose());
            if ( !didOne) {
                return null;
            }
        }

        return new String[] { lbl, content.toString() };
    }


    /**
     * _more_
     *
     * @param template _more_
     * @param element _more_
     * @param value _more_
     *
     * @return _more_
     */
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

    /**
     * _more_
     *
     * @return _more_
     */
    public String getHelp() {
        return help;
    }


    /**
     * _more_
     *
     * @param handler _more_
     * @param request _more_
     * @param formInfo _more_
     * @param entry _more_
     * @param metadata _more_
     * @param suffix _more_
     * @param forEdit _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
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
        String submit = HU.submit(msg("Add") + HU.space(1)
                                         + getName());
        String        cancel = HU.submit(msg("Cancel"), ARG_CANCEL);
        StringBuilder sb     = new StringBuilder();
        if (Utils.stringDefined(help)) {
            sb.append(HU.row(HU.colspan(HU.note(help),
                    3)));
            sb.append("\n");
        }

        if ( !forEdit) {
            //            sb.append(header(msgLabel("Add") + getName()));
        }
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
                sb.append(HU.formEntryTop(elementLbl, "\n" + widget,
                        suffixLabel));
            }
        }

        sb.append("\n");
        sb.append( HU.formEntry("",
				       HU.labeledCheckbox(
								ARG_METADATA_INHERITED + suffix, "true",
								metadata.getInherited(),"Inherited")));



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
	if(titles.size()>0) {
	    lbl = lbl + " " + Utils.join(titles," - ");
	} else {
	    if ((firstValue != null) && (firstValue.indexOf("<") < 0)) {
		lbl = lbl + " " + firstValue;
	    }
	}

        return new String[] { lbl, sb.toString() };
    }







    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    public boolean isType(String id) {
        return Misc.equals(this.id, id);
    }



    /**
     *  Get the ID property.
     *
     *  @return The Id
     */
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





    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getHasDatabaseTable() {
        return makeDatabaseTable;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public int getPriority() {
        return priority;
    }

}
