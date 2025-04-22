/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.output;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.sql.SqlUtil;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import java.io.*;

import java.net.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import java.util.regex.*;

import java.util.zip.*;

import java.util.zip.*;

import javax.servlet.http.*;

/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class ZipFileOutputHandler extends OutputHandler {

    public static final OutputType OUTPUT_LIST =
        new OutputType("Zip File Listing", "zipfile.list",
                       OutputType.TYPE_FILE, "", ICON_ZIP);

    public ZipFileOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_LIST);
    }

    public ZipFileOutputHandler(Repository repository, Element element,boolean skip)
            throws Exception {
        super(repository, element);
    }

    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {

        if (state.entry == null) {
            return;
        }

        if ( !state.entry.isFile()) {
            return;
        }
        if ( !getRepository().getAccessManager().canAccessFile(request,
                state.entry)) {
            return;
        }
        String path = state.entry.getResource().getPath().toLowerCase();
        if (path.endsWith(".zip") || path.endsWith(".jar")
                || path.endsWith(".zidv") || path.endsWith(".kmz")) {
            links.add(makeLink(request, state.entry, OUTPUT_LIST));
        }

    }

    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
        if ( !getRepository().getAccessManager().canAccessFile(request,
                entry)) {
            throw new AccessException("Cannot access data", request);
        }
        String fileToFetch = request.getString(ARG_FILE, null);
        if (fileToFetch != null) {
            return fetchFile(request, entry, fileToFetch);
        }
        StringBuffer sb = new StringBuffer();
        getPageHandler().entrySectionOpen(request, entry, sb,
                                          "Zip File Listing");
        outputZipFile(entry, sb);
        getPageHandler().entrySectionClose(request, entry, sb);

        return makeLinksResult(request, msg("Zip File Listing"), sb,
                               new State(entry));

    }

    /**
     *
     * @param entry _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void outputZipFile(Entry entry, Appendable sb) throws Exception {
	if(entry.getResource().isS3()) {
	    sb.append("No zip file listing for S3 files");
	    return;
	}

        ZipFile zipFile = new ZipFile(entry.getResource().getPath());
        //        ZipInputStream zin     = new ZipInputStream(fis);
        //        InputStream fis = getStorageManager().getFileInputStream(entry.getResource().getPath());
	List<Node> dirs  = new ArrayList<Node>();
        Node root    = null;
        Node current = null;
        root    = new Node("Zip File Contents", true, -1);
        current = root;
        try {
            Enumeration zipEnum = zipFile.entries();
            while (zipEnum.hasMoreElements()) {
                ZipEntry ze = (ZipEntry) zipEnum.nextElement();
                String path = ze.getName();
                if (root == null) {
                    if (ze.isDirectory()) {
                        continue;
                    }
                }
                long size   = ze.getSize();
                Node node = new Node(path, ze.isDirectory(), ze.isDirectory()
                        ? -1
                        : size);
		Node parent=root;
		for(Node tmp: dirs) {
		    if(path.startsWith(tmp.path)) {
			parent = tmp;
			break;
		    }
		}

		if(ze.isDirectory()) {
		    //Add at beginning so we get the closest dir
		    dirs.add(0,node);
		}
                parent.addChild(node);
                if (ze.isDirectory()) {
                    current = node;
                }
	    }
            root.walk(getRepository(), entry, sb, 0);
        } finally {
            zipFile.close();
        }
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    private static class Node {

        /**  */
        long size = -1;

        /**  */
        String path;

        /**  */
        Node parent;

        /**  */
        List<Node> children;

        /**  */
        boolean isDir = false;

        /**
         *
         *
         * @param path _more_
         * @param isDir _more_
         * @param size _more_
         */
        public Node(String path, boolean isDir, long size) {
            this.path  = path;
            this.isDir = isDir;
            this.size  = size;
        }

        /**
         *
         * @param node _more_
         */
        public void addChild(Node node) {
            if (children == null) {
                children = new ArrayList<Node>();
            }
            children.add(node);
            node.parent = this;
        }

	public List<Node> sortChildren() {
	    if(children==null) return null;
	    List<Node> tmp = new ArrayList<Node>();
	    for(Node child:children) {
		if(child.isDir) tmp.add(child);
	    }
	    for(Node child:children) {
		if(!child.isDir) tmp.add(child);
	    }
	    return tmp;
	}

        /**
         *
         * @param repository _more_
         * @param entry _more_
         * @param sb _more_
         * @param level _more_
         *
         * @throws Exception _more_
         */
        public void walk(Repository repository, Entry entry, Appendable sb,
                         int level)
                throws Exception {
            String name = path;
            if (name.endsWith("/")) {
                name = name.substring(0, name.length() - 1);
            }
            name = IO.getFileTail(name);
            if ( !isDir) {
                String url = repository.URL_ENTRY_SHOW + "/" + name;
                url = HtmlUtils.url(url, ARG_ENTRYID, entry.getId(),
                                    ARG_FILE, path, ARG_OUTPUT,
                                    OUTPUT_LIST.getId());
                sb.append("<div>");
                String suffix = IO.getFileExtension(name).toLowerCase();

                String icon   = repository.getProperty("file.icon" + suffix);
		sb.append(repository.getIconImage((icon != null? icon:"fa-file"),"width",ICON_WIDTH));
                sb.append(" ");
                sb.append(HtmlUtils.href(url, name));
                sb.append(RepositoryManager.formatFileLength(size, true));
                sb.append("</div>");

                return;
            }
            StringBuilder sb2 = new StringBuilder();
            if (children != null) {
                for (Node child : sortChildren()) {
                    child.walk(repository, entry, sb2, level + 1);
                }
            }
            String div = HU.div(sb2.toString(),
                                HU.attrs("style",
                                         "margin-left:" + ((level + 1) * 8)
                                         + "px"));
            sb.append(HU.makeShowHideBlock(name, div, true));
        }
    }

    /**
     *
     * @param request _more_
     * @param entry _more_
     * @param fileToFetch _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result fetchFile(Request request, Entry entry, String fileToFetch)
            throws Exception {
        InputStream fis = getStorageManager().getFileInputStream(
                              entry.getResource().getPath());
        ZipInputStream zin = new ZipInputStream(fis);
        ZipEntry       ze  = null;
        while ((ze = zin.getNextEntry()) != null) {
            String path = ze.getName();
            if (ze.isDirectory()) {
                continue;
            }
            if (path.equals(fileToFetch)) {
                HttpServletResponse response =
                    request.getHttpServletResponse();
                String type = getRepository().getMimeTypeFromSuffix(
                                  IO.getFileExtension(path));
                response.setContentType(type);
                OutputStream output = response.getOutputStream();
                try {
                    IOUtil.writeTo(zin, output);
                } finally {
                    IO.close(output);
                    IO.close(zin);
                }

                return Result.makeNoOpResult();
            }
        }

        throw new IllegalArgumentException("Could not find file:"
                                           + fileToFetch);
    }

}
