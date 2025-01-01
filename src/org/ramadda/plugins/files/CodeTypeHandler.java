/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.files;


import org.ramadda.data.docs.*;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.FormInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import java.io.*;

import java.util.Hashtable;
import java.util.HashSet;


/**
 *
 *
 */
public class CodeTypeHandler extends TypeHandler {


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public CodeTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }



    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {

        if ( !tag.equals("code.view")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }

        if ( !entry.isFile()) {
            return "No code file available";
        }
	//Limit the size
	if(entry.getResource().getFileSize()>1000*1000*5) {
	    return "";
	}


        StringBuilder sb = new StringBuilder();
	linkCSS(request, sb, getRepository().getHtdocsUrl("/lib/highlight/default.min.css"));
	linkJS(request, sb, getRepository().getHtdocsUrl("/lib/highlight/highlight.min.js"));
	//<script src=\"https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/languages/go.min.js\"></script>

        try {
            String id = Utils.getGuid();
            String code =   getStorageManager().readEntry(entry);
	    code = code.replace("<","&lt;").replace(">","&gt;");
	    sb.append("<div style='max-height:600px;overflow-y:auto;'><pre><code>");
	    sb.append(code);
	    sb.append("</code></pre></div>");
	    sb.append(HtmlUtils.script("hljs.highlightAll();\n"));
        } catch (Exception exc) {
            sb.append("Error formatting code: " + exc);
	    System.err.println("Error formatting code:"  + exc);
            exc.printStackTrace();
        }
        return sb.toString();
    }



}
