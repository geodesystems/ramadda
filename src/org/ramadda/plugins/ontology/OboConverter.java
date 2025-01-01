/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.ontology;


import org.ramadda.repository.*;


import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

import java.util.List;


/**
 * Class description
 *
 *
 * @version        $version$, Thu, Nov 25, '10
 * @author         Enter your name here...
 */
public class OboConverter extends ImportHandler {

    /** _more_ */
    private HashSet<String> tagMap = new HashSet<String>();

    /** _more_ */
    private String defaultNamespace = "";

    /**
     * ctor
     */
    public OboConverter() {}



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
                                 String fileName, InputStream stream)
            throws Exception {
        String ext = IOUtil.getFileExtension(fileName);
        if ( !ext.equals(".obo")) {
            return null;
        }
        String xml = processFile(fileName);

        return new ByteArrayInputStream(xml.getBytes());
    }



    /**
     * _more_
     *
     * @param line _more_
     *
     * @return _more_
     */
    public String[] getPair(String line) {
        return getPair(line, ":");
    }

    /**
     * _more_
     *
     * @param line _more_
     * @param token _more_
     *
     * @return _more_
     */
    public String[] getPair(String line, String token) {
        List<String> toks = StringUtil.splitUpTo(line, token, 2);
        if ( !tagMap.contains(toks.get(0))) {
            String tag = toks.get(0);
            tagMap.add(tag);
            String var = tag.toUpperCase();
            var = var.replaceAll("-", "_");
            if (var.indexOf(":") < 0) {
                //                System.out.println("public  final String OboUtil.TAG_" + var +"  = \"" + tag +"\";"); 
            }
        }
        if (toks.size() == 1) {
            return new String[] { toks.get(0).trim() };
        }

        return new String[] { toks.get(0).trim(), toks.get(1).trim() };
    }


    /**
     * _more_
     *
     * @param file _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    public String processFile(String file) throws Exception {
        List<String> lines = StringUtil.split(IOUtil.readContents(file,
                                 OboConverter.class), "\n", true, true);
        Term                    currentTerm = null;
        List<Term>              terms       = new ArrayList<Term>();
        Hashtable<String, Term> map         = new Hashtable<String, Term>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("[Term]")) {
                line = lines.get(++i);
                String[] pair = getPair(line);
                currentTerm = new Term(pair[1]);
                map.put(currentTerm.id, currentTerm);
                terms.add(currentTerm);

                continue;
            }

            if (line.startsWith("[Typedef]")) {
                currentTerm = null;

                continue;
            }
            String[] pair = getPair(line);
            if (pair[0].equals(OboUtil.TAG_DEFAULT_NAMESPACE)) {
                defaultNamespace = pair[1];

                continue;
            }

            if (currentTerm != null) {
                currentTerm.values.add(pair);
            }
        }

        HashSet<String> processed    = new HashSet<String>();
        StringBuffer    xml          = new StringBuffer(XmlUtil.XML_HEADER);
        StringBuffer    associations = new StringBuffer();
        xml.append("<entries>\n");

        for (Term term : terms) {
            process(term, xml, associations, processed, map);
        }
        System.err.println("# terms:" + terms.size());
        xml.append(associations);
        xml.append("</entries>\n");

        return xml.toString();
    }

    /**
     * _more_
     *
     * @param term _more_
     * @param xml _more_
     * @param associations _more_
     * @param processed _more_
     * @param map _more_
     *
     * @throws Exception _more_
     */
    private void process(Term term, Appendable xml, Appendable associations,
                         HashSet<String> processed,
                         Hashtable<String, Term> map)
            throws Exception {

        if (processed.contains(term.id)) {
            return;
        }
        processed.add(term.id);
        String namespace = term.getValue(OboUtil.TAG_NAMESPACE,
                                         defaultNamespace);
        String       parentId  = null;
        StringBuffer childTags = new StringBuffer();
        childTags.append(XmlUtil.tag("description", "",
                                     XmlUtil.getCdata(term.getDef())));

        //relationship: part_of TADS:0000501 ! adult male accessory gland

        for (String tuple : term.getValues(OboUtil.TAG_IS_A)) {
            String id        = getPair(tuple, "!")[0];
            Term   otherTerm = map.get(id);
            if (otherTerm == null) {
                System.out.println("    isa =  NULL " + id);

                continue;
            }
            process(otherTerm, xml, associations, processed, map);
            if (parentId == null) {
                parentId = otherTerm.id;
            } else {
                associations.append(XmlUtil.tag("association",
                        XmlUtil.attrs("from", term.id, "to", otherTerm.id,
                                      "type", "is_a")));

                System.err.println("Multiple isa:" + term.id);
            }
        }


        //relationship: part_of TADS:0000501 ! adult male accessory gland

        for (String tuple : term.getValues(OboUtil.TAG_RELATIONSHIP)) {
            tuple = getPair(tuple, "!")[0];
            List<String> toks      = StringUtil.split(tuple, " ", true, true);
            String       type      = toks.get(0);
            String       id        = toks.get(1);
            Term         otherTerm = map.get(id);
            if (otherTerm == null) {
                System.out.println("    relationship =  NULL " + id);

                continue;
            }
            process(otherTerm, xml, associations, processed, map);
            if (parentId == null) {
                parentId = otherTerm.id;
            } else {
                associations.append(XmlUtil.tag("association",
                        XmlUtil.attrs("from", term.id, "to", otherTerm.id,
                                      "type", "part_of")));

            }
        }

        //synonym: "Nucleus of the Solitary Tract principle cell" EXACT []
        HashSet<String> synonyms = new HashSet<String>();
        for (String tuple : term.getValues(OboUtil.TAG_SYNONYM)) {
            int    idx1  = tuple.indexOf("\"");
            int    idx2  = tuple.lastIndexOf("\"");
            String value = tuple;
            if ((idx1 >= 0) && (idx2 > idx1)) {
                value = tuple.substring(idx1 + 1, idx2);
            }

            synonyms.add(value);
            childTags.append(XmlUtil.tag("metadata",
                                         XmlUtil.attrs("type", "synonym",
                                             "attr1", value)));
            childTags.append("\n");
        }

        //        property_value: nif_obo_annot:createdDate "2007-09-05" xsd:string
        for (String tuple : term.getValues(OboUtil.TAG_PROPERTY_VALUE)) {
            String[] pair = getPair(tuple, " ");
            String   type = pair[0];
            int      idx  = type.lastIndexOf(":");
            if (idx >= 0) {
                type = type.substring(idx + 1);
            }

            tuple = pair[1];
            int idx1 = tuple.indexOf("\"");
            int idx2 = tuple.lastIndexOf("\"");
            if ((idx1 < 0) || (idx2 < 0)) {
                continue;
            }

            String value = tuple.substring(idx1 + 1, idx2);
            if (type.equals("synonym")) {
                if (synonyms.contains(value)) {
                    continue;
                }
                synonyms.add(value);
            }
            childTags.append(XmlUtil.tag("metadata",
                                         XmlUtil.attrs("type", "property",
                                             "attr1", type, "attr2", value)));
            childTags.append("\n");

        }

        if (parentId == null) {
            parentId = "";
        }
        xml.append(
            XmlUtil.tag(
                "entry",
                XmlUtil.attrs(
                    "type", RdfUtil.TYPE_CLASS, "name", term.getName(), "id",
                    term.id, "parent", parentId), childTags.toString()));

        xml.append("\n");
    }



    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        OboConverter oboConverter = new OboConverter();
        for (String file : args) {
            String entriesFile =
                IOUtil.stripExtension(IOUtil.getFileTail(file))
                + "entries.xml";
            String xml = oboConverter.processFile(file);
            IOUtil.writeFile(entriesFile, xml);
        }
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 25, '10
     * @author         Enter your name here...
     */
    public static class Term {

        /** _more_ */
        String id;

        /** _more_ */
        List<String[]> values = new ArrayList<String[]>();

        /**
         * _more_
         *
         * @param id _more_
         */
        public Term(String id) {
            this.id = id;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getName() {
            return getValue(OboUtil.TAG_NAME, getId());
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getId() {
            return id;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public String getDef() {
            return unquote(getValue(OboUtil.TAG_DEF, ""));
        }

        /**
         * _more_
         *
         * @param s _more_
         *
         * @return _more_
         */
        public String unquote(String s) {
            if (s == null) {
                return null;
            }
            s = s.trim();
            if ( !s.startsWith("\"")) {
                return s;
            }
            s = s.substring(1);
            int idx = s.indexOf("\"");
            if (idx >= 0) {
                s = s.substring(0, idx);
            }

            return s;
        }


        /**
         * _more_
         *
         * @param key _more_
         *
         * @return _more_
         */
        public String getValue(String key) {
            return getValue(key, null);
        }

        /**
         * _more_
         *
         * @param key _more_
         * @param dflt _more_
         *
         * @return _more_
         */
        public String getValue(String key, String dflt) {
            for (String[] tuple : values) {
                if (tuple[0].equals(key)) {
                    return tuple[1];
                }
            }

            return dflt;
        }

        /**
         * _more_
         *
         * @param key _more_
         *
         * @return _more_
         */
        public List<String> getValues(String key) {
            List<String> theValues = new ArrayList<String>();
            for (String[] tuple : values) {
                if (tuple[0].equals(key)) {
                    theValues.add(unquote(tuple[1]));
                }
            }

            return theValues;
        }

    }
}
