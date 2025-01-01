/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.client;


import org.ramadda.repository.RepositoryBase;
import org.ramadda.repository.RepositoryUtil;
import org.ramadda.repository.RequestUrl;
import org.ramadda.repository.util.ServerInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;

import org.ramadda.util.Utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ucar.unidata.ui.HttpFormEntry;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;



import java.io.FileOutputStream;
import java.io.InputStream;

import java.net.URL;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;



/**
 *
 *
 * @author RAMADDA Development Team
 */
public class RepositorySearch extends RepositoryClient {

    /** _more_ */
    private String output = "default.csv";

    /** _more_ */
    private String fields = "name,url";

    /** _more_ */
    private boolean download = false;

    /** _more_ */
    private boolean overwrite = false;

    /** _more_ */
    private boolean showMetadata = false;

    /**
     * _more_
     */
    public RepositorySearch() {}

    /**
     * _more_
     *
     * @param serverUrl _more_
     * @param user _more_
     * @param password _more_
     *
     * @throws Exception _more_
     */
    public RepositorySearch(URL serverUrl, String user, String password)
            throws Exception {
        super(serverUrl, user, password);
    }




    /**
     * _more_
     *
     * @param hostname _more_
     * @param port _more_
     * @param base _more_
     *
     * @throws Exception _more_
     */
    public RepositorySearch(String hostname, int port, String base)
            throws Exception {
        this(hostname, port, base, "", "");
    }

    /**
     * _more_
     *
     * @param hostname _more_
     * @param port _more_
     * @param base _more_
     * @param user _more_
     * @param password _more_
     *
     * @throws Exception _more_
     */
    public RepositorySearch(String hostname, int port, String base,
                            String user, String password)
            throws Exception {
        super(hostname, port, base, user, password);
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    private void doSearch(List<String> args) throws Exception {

        RequestUrl URL_ENTRY_SEARCH = new RequestUrl(this, "/search/do",
                                          "Search");

        List<String> argList = new ArrayList<String>();
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if (arg.equals("-text")) {
                argList.add(ARG_TEXT);
                argList.add(args.get(++i));
            } else if (arg.equals("-type")) {
                argList.add(ARG_TYPE);
                argList.add(args.get(++i));
            } else if (arg.equals("-daterange")) {
                argList.add("datadate.from");
                argList.add(args.get(++i));
                argList.add("datadate.to");
                argList.add(args.get(++i));
            } else if (arg.equals("-mindate")) {
                argList.add("datadate.from");
                argList.add(args.get(++i));
            } else if (arg.equals("-maxdate")) {
                argList.add("datadate.to");
                argList.add(args.get(++i));
            } else if (arg.equals("-mincreatedate")) {
                argList.add("createdate.from");
                argList.add(args.get(++i));
            } else if (arg.equals("-maxcreatedate")) {
                argList.add("createdate.to");
                argList.add(args.get(++i));
            } else if (arg.equals("-max")) {
                argList.add("max");
                argList.add(args.get(++i));
            } else if (arg.equals("-suffix")) {
                argList.add("filesuffix");
                argList.add(args.get(++i));

            } else if (arg.equals("-bounds")) {
                argList.add(ARG_AREA_NORTH);
                argList.add(args.get(++i));
                argList.add(ARG_AREA_WEST);
                argList.add(args.get(++i));
                argList.add(ARG_AREA_SOUTH);
                argList.add(args.get(++i));
                argList.add(ARG_AREA_EAST);
                argList.add(args.get(++i));
            } else if (arg.equals("-tag")) {
                argList.add("metadata.attr1.enum_tag");
                argList.add(args.get(++i));
            } else if (arg.equals("-keyword")) {
                argList.add("metadata.attr1.content.keyword");
                argList.add(args.get(++i));
            } else if (arg.equals("-variable")) {
                argList.add("metadata.attr1.thredds.variable");
                argList.add(args.get(++i));
            } else if (arg.equals("-fields")) {
                fields = args.get(++i);
                output = "default.csv";
            } else if (arg.equals("-overwrite")) {
                overwrite = true;
            } else if (arg.equals("-download")) {
                download = true;
                fields   = "name,size,url";
                output   = "default.csv";
            } else if (arg.equals("-help")) {
                usage("");
            } else if (arg.equals("-output")) {
                output = args.get(++i);
                if (output.equals("wget")) {
                    output = "bulk.wget";
                } else if (output.equals("curl")) {
                    output = "bulk.curl";
                } else if (output.equals("csv")) {
                    output = "default.csv";
                } else if (output.equals("metadata")) {
                    output       = "xml.xml";
                    showMetadata = true;
                } else if (output.equals("name")) {
                    output = "default.csv";
                    fields = "name";
                }
            } else {
                usage("Unknown arg:" + arg);
            }
        }

        argList.add("fields");
        argList.add(fields);

        //        argList.add(args[0]);
        //argList.add(args[1]);
        checkSession();
        argList.add(ARG_OUTPUT);
        argList.add(output);
        argList.add(ARG_SESSIONID);
        argList.add(getSessionId());
        String url = HtmlUtils.url(URL_ENTRY_SEARCH.getFullUrl(), argList);
        String xml = IOUtil.readContents(url, getClass());

        if (download) {
            handleDownload(xml);

            return;
        }

        if (showMetadata) {
            handleMetadata(xml);

            return;
        }

        System.out.println(xml);

    }


    /**
     * _more_
     *
     * @param xml _more_
     *
     * @throws Exception _more_
     */
    private void handleMetadata(String xml) throws Exception {
        Element root = XmlUtil.getRoot(xml);
        System.err.println(XmlUtil.toString(root));
        List children = XmlUtil.findChildren(root, "entry");
        if (children.size() == 0) {
            System.err.println("no results found:\n"
                               + XmlUtil.toString(root));
        }
        for (int i = 0; i < children.size(); i++) {
            Element entry = (Element) children.get(i);
            System.out.println("name: "
                               + XmlUtil.getAttribute(entry, "name", ""));
            System.out.println("path: "
                               + XmlUtil.getAttribute(entry, "path", ""));
            System.out.println("\tcreate date: "
                               + XmlUtil.getAttribute(entry, "createdate",
                                   ""));
            System.out.println("\tfrom date: "
                               + XmlUtil.getAttribute(entry, "fromdate", ""));
            System.out.println("\tto date: "
                               + XmlUtil.getAttribute(entry, "todate", ""));
            System.out.println("\tfile size: "
                               + XmlUtil.getAttribute(entry, "filesize",
                                   "0"));
            List mdts = XmlUtil.findChildren(entry, "metadata");
            for (int j = 0; j < mdts.size(); j++) {
                Element mdt  = (Element) mdts.get(j);
                String  type = XmlUtil.getAttribute(mdt, "type", "");
                System.out.println("\tmetadata: " + type);
                List attrs = XmlUtil.findChildren(mdt, "attr");
                for (int k = 0; k < attrs.size(); k++) {
                    Element attr = (Element) attrs.get(k);
                    String childText = new String(
                                           Utils.decodeBase64(
                                               XmlUtil.getChildText(attr)));
                    System.out.println("\t\tattr[" + k + "]=" + childText);
                }

            }
        }

    }


    /**
     * _more_
     *
     * @param csv _more_
     *
     * @throws Exception _more_
     */
    private void handleDownload(String csv) throws Exception {
        List<String> lines = Utils.split(csv, "\n", true, true);
        for (int i = 0; i < lines.size(); i++) {

            if (i == 0) {
                continue;
            }
            String line = lines.get(i);
            //            System.err.println("line:" + line);
            List<String> toks = Utils.splitUpTo(line, ",", 3);
            if (toks.size() != 3) {
                continue;
            }
            String name = toks.get(0);
            long   size = Long.parseLong(toks.get(1));
            File   f    = new File(name);
            if (f.exists()) {
                if ( !overwrite) {
                    System.err.println("Not overwriting:" + f);

                    continue;
                }
                System.err.println("Overwriting:" + f);
            }
            String url = toks.get(2);
            System.err.print("Downloading " + name + " size=" + size);
            InputStream inputStream = IOUtil.getInputStream(url, getClass());
            FileOutputStream fos    = new FileOutputStream(f);
            IOUtil.writeTo(inputStream, fos);
            IO.close(fos);
            IO.close(inputStream);
            System.err.println(" done");
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
        String repository = System.getenv(PROP_REPOSITORY);
        String user       = System.getenv(PROP_USER);
        String password   = System.getenv(PROP_PASSWORD);
        if (repository == null) {
            repository = "http://localhost/repository";
        }
        if (user == null) {
            user = "";
        }
        if (password == null) {
            password = "";
        }

        List<String> argList = new ArrayList<String>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-repos")) {
                repository = args[++i];
            } else if (args[i].equals("-user")) {
                user     = args[++i];
                password = args[++i];
            } else {
                argList.add(args[i]);
            }
        }


        try {
            RepositorySearch client =
                new RepositorySearch(new URL(repository), user, password);

            client.doSearch(argList);
            String[] msg = { "" };
            /*            if ( !client.isValidSession(true, msg)) {
                System.err.println("Error: invalid session:" + msg[0]);
                return;
                }*/
        } catch (Exception exc) {
            System.err.println("Error:" + exc);
            exc.printStackTrace();
            System.exit(1);
        }
        System.exit(0);

    }


    /**
     * _more_
     *
     * @param msg _more_
     */
    public static void usage(String msg) {
        System.err.println(msg);
        System.err.println(
            "Usage: search.sh -repository <server url> -user <user id> <password>\n\t-output <csv|curl|name|metadata|...>  \n\t-fields <comma separated list of fields to output - e.g. name, url, size, id, lat, lon, ....>\n\t-download -overwrite \n\t-max <max entries to show>\n\tSearch options:\n\t-text <search text>  \n\t-type <entry type>  \n\t-bounds <north> <west> <south> <east> \n\t-daterange <startdate yyyy-MM-dd> <todate yyyy-MM-dd> \n\t-mindate <date yyyy-MM-dd> \n\t-maxdate <date yyyy-MM-dd>\n\t-mincreatedate <date yyyy-MM-dd> \n\t-maxcreatedate <date yyyy-MM-dd>\n\t-suffix <file suffix>\n\t-variable <var name> \n\t-tag <tag> \n\t-keyword <keyword> ");
        System.err.println(
            "e.g., search.sh -repos http://ramadda.org/repository -text foo -max 5 -fields \"name,size,url\"");
        System.exit(1);
    }



}
