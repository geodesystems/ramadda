/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.ontology;


import org.ramadda.repository.*;
import org.ramadda.repository.util.AssociationInfo;

import org.ramadda.repository.util.EntryInfo;


import org.w3c.dom.*;
import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;


import java.util.List;
import java.util.Properties;


/**
 * Class description
 *
 *
 * @version        $version$, Thu, Nov 25, '10
 * @author         Enter your name here...
 */
public class OwlConverter extends ImportHandler {

    /** _more_ */
    static Properties names = new Properties();

    /**
     * _more_
     */
    public OwlConverter() {}


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param parent _more_
     * @param fileName _more_
     * @param stream _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public InputStream getStream(Request request, Entry parent,
                                 String fileName, InputStream stream, StringBuilder message)
            throws Exception {
        String  ext   = IOUtil.getFileExtension(fileName);
        boolean isOwl = ext.equals(".rdf") || ext.equals(".owl");

        if ( !isOwl) {
            return null;
        }
        StringBuffer xml = new StringBuffer(XmlUtil.XML_HEADER);
        xml.append("<entries>\n");
        List<AssociationInfo> links = new ArrayList<AssociationInfo>();
        StringBuffer associations = new StringBuffer(XmlUtil.XML_HEADER);
        HashSet<String>       seen    = new HashSet<String>();
        List<EntryInfo>       entries = new ArrayList<EntryInfo>();
        Hashtable<String, EntryInfo> entryMap = new Hashtable<String,
                                                    EntryInfo>();
        HashSet<String> processed = new HashSet<String>();
        processFile(fileName, "", entries, entryMap, links, seen);
        EntryInfo.appendEntries(xml, entries, entryMap, processed);
        AssociationInfo.appendAssociations(xml, links, seen);
        xml.append("</entries>\n");

        return new ByteArrayInputStream(xml.toString().getBytes());
    }





    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public static String getName(String name) {
        int idx = name.indexOf("#");
        if (idx > 0) {
            name = name.substring(idx + 1);
        }


        String newName = (String) names.get(name);
        if (newName != null) {
            return newName;
        }

        String[] ltrs = {
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
            "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"
        };
        for (String ltr : ltrs) {
            name = name.replaceAll(ltr, " " + ltr);
        }

        return name.trim();
    }



    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void convertSweetAll() throws Exception {
        Properties topLevelMap = new Properties();
        topLevelMap.load(
            IOUtil.getInputStream(
                "/org/ramadda/plugins/ontology/toplevel.properties",
                OwlConverter.class));
        names.load(
            IOUtil.getInputStream(
                "/org/ramadda/plugins/ontology/names.properties",
                OwlConverter.class));

        File         f   = new File("owl");
        StringBuffer xml = new StringBuffer(XmlUtil.XML_HEADER);
        xml.append("<entries>\n");

        List<AssociationInfo> links = new ArrayList<AssociationInfo>();
        StringBuffer associations = new StringBuffer(XmlUtil.XML_HEADER);
        HashSet<String>       seen    = new HashSet<String>();
        List<EntryInfo>       entries = new ArrayList<EntryInfo>();
        Hashtable<String, EntryInfo> entryMap = new Hashtable<String,
                                                    EntryInfo>();




        List<String> files =
            StringUtil.split(
                IOUtil.readContents(
                    "/org.ramadda.plugins.ontology/sweetfiles.txt",
                    OwlConverter.class), "\n", true, true);
        int             cnt             = 0;
        String          currentTopLevel = null;
        HashSet<String> processed       = new HashSet<String>();

        for (String file : files) {
            file = "owl/" + IOUtil.getFileTail(file);
            System.err.println("file:" + file);
            String filePrefix = IOUtil.getFileTail(file.toString());
            //if(cnt++>10) break;
            String group =
                IOUtil.stripExtension(IOUtil.getFileTail(file.toString()));
            String topLevelLabel = (String) topLevelMap.get(group);
            if (topLevelLabel != null) {
                xml.append(XmlUtil.tag("entry",
                                       XmlUtil.attrs("type",
                                           RdfUtil.TYPE_ONTOLOGY, "name",
                                           getName(topLevelLabel), "id",
                                           group)));
                xml.append("\n");
                processed.add(group);
                currentTopLevel = group;
            } else {
                if ( !group.startsWith(currentTopLevel)) {
                    System.err.println("?:" + group + " " + currentTopLevel);
                }
                String name = getName(group.replace(currentTopLevel, ""));
                xml.append(XmlUtil.tag("entry",
                                       XmlUtil.attrs("type",
                                           RdfUtil.TYPE_ONTOLOGY, "name",
                                           name, "id", group, "parent",
                                           currentTopLevel)));
                xml.append("\n");
                processed.add(group);
            }


            processFile(file, group, entries, entryMap, links, seen);
            EntryInfo.appendEntries(xml, entries, entryMap, processed);
            AssociationInfo.appendAssociations(xml, links, seen);
            xml.append("</entries>\n");
            IOUtil.writeFile("entries.xml", xml.toString());
        }
    }


    /**
     * _more_
     *
     * @param file _more_
     * @param groupId _more_
     * @param entries _more_
     * @param entryMap _more_
     * @param links _more_
     * @param seen _more_
     *
     * @throws Exception _more_
     */
    private void processFile(String file, String groupId,
                             List<EntryInfo> entries,
                             Hashtable<String, EntryInfo> entryMap,
                             List<AssociationInfo> links,
                             HashSet<String> seen)
            throws Exception {

        String  filePrefix = IOUtil.getFileTail(file.toString());
        Element root = XmlUtil.getRoot(file.toString(), OwlConverter.class);
        if (root == null) {
            System.err.println("failed to read:" + file);

            return;
        }
        NodeList children = XmlUtil.getElements(root);
        for (int i = 0; i < children.getLength(); i++) {
            String  parent      = groupId;
            Element node        = (Element) children.item(i);
            String  tag         = node.getTagName();
            boolean okToProcess = false;
            if (tag.equals(RdfUtil.TAG_OWL_ONTOLOGY)) {
                continue;
            }
            if (tag.equals(RdfUtil.TAG_OWL_OBJECTPROPERTY)) {
                continue;
            }

            if (tag.equals(RdfUtil.TAG_OWL_CLASS)) {
                okToProcess = true;
            } else {
                int idx = tag.indexOf(":");
                if (idx >= 0) {
                    String[] toks = tag.split(":");
                    if (XmlUtil.hasAttribute(root, "xmlns:" + toks[0])) {
                        okToProcess = true;
                        parent = XmlUtil.getAttribute(root,
                                "xmlns:" + toks[0]) + "" + toks[1];
                        parent =
                            parent.replace("http://sweet.jpl.nasa.gov/2.1/",
                                           "");
                    }
                }
            }

            if ( !okToProcess) {
                System.err.println(" unknown:" + filePrefix + "::" + tag);
            }

            if (okToProcess) {
                String id;
                if (XmlUtil.hasAttribute(node, RdfUtil.ATTR_RDF_ABOUT)) {
                    id = XmlUtil.getAttribute(node, RdfUtil.ATTR_RDF_ABOUT,
                            "").trim();
                    id = id.replace("http://sweet.jpl.nasa.gov/2.1/", "");
                    if (id.startsWith("#")) {
                        id = filePrefix + id;
                    }
                } else if (XmlUtil.hasAttribute(node, RdfUtil.ATTR_RDF_ID)) {
                    id = XmlUtil.getAttribute(node, RdfUtil.ATTR_RDF_ID,
                            "").trim();
                    if (id.startsWith("#")) {
                        id = filePrefix + id;
                    } else {
                        id = filePrefix + "#" + id;
                    }
                } else {
                    continue;
                }

                String desc = null;
                seen.add(id);
                NodeList children2 = XmlUtil.getElements(node);
                for (int j = 0; j < children2.getLength(); j++) {
                    Element child     = (Element) children2.item(j);
                    String  childName = child.getTagName();
                    if (childName.equals(RdfUtil.TAG_RDFS_SUBCLASSOF)
                            || childName.equals(RdfUtil.TAG_OWL_DISJOINTWITH)
                            || childName.equals(
                                RdfUtil.TAG_OWL_EQUIVALENTCLASS)) {
                        if ( !XmlUtil.hasAttribute(child,
                                RdfUtil.ATTR_RDF_RESOURCE)) {
                            continue;
                        }
                        String resource = XmlUtil.getAttribute(child,
                                              RdfUtil.ATTR_RDF_RESOURCE);
                        resource = resource.replace(
                            "http://sweet.jpl.nasa.gov/2.1/", "");
                        if (resource.startsWith("#")) {
                            resource = filePrefix + resource;
                        }
                        links.add(new AssociationInfo(id, resource,
                                childName));
                    } else if (childName.equals(RdfUtil.TAG_RDFS_COMMENT)) {
                        desc = XmlUtil.getChildText(child);
                    } else if (childName.equals(RdfUtil.TAG_RDFS_LABEL)) {}
                    else {
                        //                            System.err.println("   ??:" + childName);
                    }
                }

                StringBuffer childTags = new StringBuffer();
                if (desc != null) {
                    childTags.append(XmlUtil.tag("description", "",
                            XmlUtil.getCdata(desc.toString())));
                }
                EntryInfo entryInfo = new EntryInfo(id, getName(id), parent,
                                          RdfUtil.TYPE_CLASS,
                                          childTags.toString());
                entries.add(entryInfo);
                entryMap.put(id, entryInfo);
            }
        }
    }





    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        /*
        OwlConverter processor = new OwlConverter();
        if (args.length > 0) {
            for (String file : args) {
                InputStream newStream = processor.getStream(file,
                                            new FileInputStream(file));
                IOUtil.writeFile(IOUtil.stripExtension(file) + "entries.xml",
                                 IOUtil.readInputStream(newStream));
            }

            return;
        }



        processor.convertSweetAll();
        */
    }



}
