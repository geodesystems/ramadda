/*
* Copyright (c) 2008-2025 Geode Systems LLC
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

package org.ramadda.bio.taxonomy;


import org.ramadda.util.Utils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
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
 * @version        $version$, Mon, Sep 8, '14
 * @author         Enter your name here...
 */
public class IngestNCBI {

    /** _more_ */
    static String[] fields = {
        null, null, "rank", "embl_code", "division", "inherited_div",
        "genetic_code", "inherited_gc", "mitochondrial_genetic_code",
        "inherited_mgc",
    };


    /** _more_ */
    static List<Node> nodes = new ArrayList<Node>();

    /** _more_ */
    static Hashtable<String, Node> nodeMap = new Hashtable<String, Node>();

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {

        String         line;
        BufferedReader br;
        int            lineCnt = 0;

        br = new BufferedReader(
            new InputStreamReader(new FileInputStream("citations.dmp")));
        while ((line = br.readLine()) != null) {
            List<String> toks = StringUtil.split(line, "|", true, false);
            if (toks.size() < 7) {
                continue;
            }
            List<String> ids = StringUtil.split(toks.get(6), "|", true,
                                   false);
            for (String id : ids) {
                getNode(id).addCitation(toks);
            }
        }

        /*
        names.dmp
        9606    |       Homo sapiens    |               |       scientific name |
        tax_id-- the id of node associated with this name
        name_txt-- name itself
        unique name-- the unique variant of this name if name not unique
        name class
        */
        Hashtable<String, String> names = new Hashtable<String, String>();
        lineCnt = 0;
        br = new BufferedReader(
            new InputStreamReader(new FileInputStream("names.dmp")));

        while ((line = br.readLine()) != null) {
            List<String> toks   = StringUtil.split(line, "|", true, false);
            Node         node   = getNode(toks.get(0));
            boolean      isBase = toks.get(2).equals("scientific name");
            if ((node.name == null) || isBase) {
                node.name = toks.get(1);
            }
            if ( !isBase) {
                node.addAlias(toks.get(3) + ":" + toks.get(1));
            }
            if ((lineCnt++ % 10000) == 0) {
                System.err.println("#lines: " + lineCnt);
            }
        }

        /*
          tax_id-- node id in GenBank taxonomy database
          parent tax_id-- parent node id in GenBank taxonomy database
          rank-- rank of this node (superkingdom, kingdom, ...)
          embl code-- locus-name prefix; not unique
          division id-- see division.dmp file
          inherited div flag  (1 or 0)-- 1 if node inherits division from parent
          genetic code id-- see gencode.dmp file
          inherited GC  flag  (1 or 0)-- 1 if node inherits genetic code from parent
          mitochondrial genetic code id-- see gencode.dmp file
          inherited MGC flag  (1 or 0)-- 1 if node inherits mitochondrial gencode from parent
          GenBank hidden flag (1 or 0)            -- 1 if name is suppressed in GenBank entry lineage
          hidden subtree root flag (1 or 0)       -- 1 if this subtree has no sequence data yet
          comments-- free-text comments and citations
         */

        System.err.println("Reading nodes.dmp");
        lineCnt = 0;
        br = new BufferedReader(
            new InputStreamReader(new FileInputStream("nodes.dmp")));
        while ((line = br.readLine()) != null) {
            List<String> toks = StringUtil.split(line, "|", true, false);
            Node         node = getNode(toks.get(0));
            node.toks = toks;
            node.rank = toks.get(2);
            if ( !toks.get(1).equals(toks.get(0))) {
                node.setParent(getNode(toks.get(1)));
            }
            if ((lineCnt++ % 10000) == 0) {
                System.err.println("#lines: " + (lineCnt - 1));
            }
        }
        Hashtable<String, Integer> rankCnt = new Hashtable<String, Integer>();
        System.err.println("Done ingesting");
        int        cnt   = 0;
        List<Node> roots = new ArrayList<Node>();
        for (Node node : nodes) {
            if (node.id.equals("9606")) {
                BufferedWriter xml = new BufferedWriter(
                                         new PrintWriter(
                                             new File("entries.xml")));
                xml.append("<entries>\n");
                System.err.println("Printing xml");
                node.printXml(xml, true, true, new HashSet<String>());
                xml.append("</entries>\n");
                System.err.println("Done printing xml");
                xml.flush();
                //                System.out.println(xml);
                System.err.println("Exiting");
                org.ramadda.util.Utils.exitTest(1);
            }
            if (true) {
                continue;
            }
            if (node.parent == null) {
                //                roots.add(node);
                //                System.err.println ("Root:" + node);
            }
            Integer i = rankCnt.get(node.rank);
            if (i == null) {
                i = Integer.valueOf(0);
            }
            rankCnt.put(node.rank, Integer.valueOf(i.intValue() + 1));
            if (node.rank.equals("species") || node.rank.equals("no rank")) {
                continue;
            }
            if (node.rank.equals("kingdom")) {
                System.err.println("Super:" + node + " parent:"
                                   + node.parent);
            }

            cnt++;
        }
        for (Node node : roots) {
            walkTree(node, 0);
        }


    }

    /**
     * _more_
     *
     * @param node _more_
     * @param depth _more_
     */
    public static void walkTree(Node node, int depth) {
        if (node.rank.equals("no rank")) {
            System.err.println("Skipping:" + node);

            return;
        }


        if (depth > 5) {
            return;
        }
        for (int i = 0; i < depth; i++) {
            System.out.print(" ");
        }
        System.out.println(node);
        for (Node child : node.children) {
            walkTree(child, depth + 1);
        }
    }

    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    private static Node getNode(String id) {
        Node node = nodeMap.get(id);
        if (node == null) {
            node = new Node(id, null);
            nodeMap.put(id, node);
            nodes.add(node);
        }

        return node;
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Mon, Sep 8, '14
     * @author         Enter your name here...
     */
    public static class Node {

        /** _more_ */
        String id;

        /** _more_ */
        Node parent;

        /** _more_ */
        List<Node> children = new ArrayList<Node>();

        /** _more_ */
        List<List<String>> citations;

        /** _more_ */
        List<String> toks;

        /** _more_ */
        String name;

        /** _more_ */
        String rank;

        /** _more_ */
        List<String> aliases;

        /**
         * _more_
         *
         * @param id _more_
         * @param name _more_
         */
        public Node(String id, String name) {
            this.id   = id;
            this.name = name;
        }

        /**
         * _more_
         *
         * @param sb _more_
         * @param recurseUp _more_
         * @param doChildren _more_
         * @param seen _more_
         *
         * @throws Exception _more_
         */
        public void printXml(Appendable sb, boolean recurseUp,
                             boolean doChildren, HashSet<String> seen)
                throws Exception {
            if (seen.contains(id)) {
                return;
            }
            seen.add(id);
            if (recurseUp && (parent != null)) {
                parent.printXml(sb, recurseUp, true, seen);
            }

            List<String> attrs = new ArrayList<String>();
            attrs.add("name");
            attrs.add(name);
            attrs.add("id");
            attrs.add(id);
            if (parent != null) {
                attrs.add("parent");
                attrs.add(parent.id);
            }
            attrs.add("type");
            attrs.add("bio_taxonomy");
            StringBuffer contents = new StringBuffer();
            contents.append(XmlUtil.tag("description", "",
                                        XmlUtil.getCdata(toks.get(12))));

            for (int i = 0; i < fields.length; i++) {
                if (fields[i] == null) {
                    continue;
                }
                contents.append(XmlUtil.tag(fields[i], "", toks.get(i)));
            }


            if ((aliases != null) && (aliases.size() > 0)) {
                contents.append(
                    XmlUtil.tag(
                        "aliases", "",
                        XmlUtil.getCdata(StringUtil.join("\n", aliases))));
            }

            sb.append(
                XmlUtil.tag(
                    "entry", XmlUtil.attrs(Utils.toStringArray(attrs)),
                    contents.toString()));
            StringBuffer desc = new StringBuffer();


            if (doChildren) {
                for (Node child : children) {
                    child.printXml(sb, false, false, seen);
                }
            }
        }

        /**
         * _more_
         *
         * @param toks _more_
         */
        public void addCitation(List<String> toks) {
            if (citations == null) {
                citations = new ArrayList<List<String>>();
            }
            citations.add(toks);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public int printPath() {
            int indent = 0;
            if (parent != null) {
                indent = parent.printPath();
            }
            for (int i = 0; i < indent; i++) {
                System.out.print(" ");
            }
            System.out.println(this);

            return indent + 1;
        }

        /**
         * _more_
         *
         * @param node _more_
         */
        public void setParent(Node node) {
            parent = node;
            if ( !node.children.contains(this)) {
                node.children.add(this);
            }
        }


        /**
         * _more_
         *
         * @param name _more_
         */
        public void addAlias(String name) {
            if (aliases == null) {
                aliases = new ArrayList<String>();
            }
            aliases.add(name);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String toString() {
            return id + ":" + name + " (" + rank + ")";
        }


    }



}
