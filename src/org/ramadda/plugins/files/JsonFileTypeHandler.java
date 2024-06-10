/**
Copyright (c) 2008-2023 Geode Systems LLC
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

import org.ramadda.util.GroupedBuffers;
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
public class JsonFileTypeHandler extends ConvertibleTypeHandler {


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public JsonFileTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param parentEntry _more_
     * @param entry _more_
     * @param formInfo _more_
     * @param baseTypeHandler _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void addSpecialToEntryForm(Request request, GroupedBuffers      sb,
                                      Entry parentEntry, Entry entry,
                                      FormInfo formInfo,
                                      TypeHandler baseTypeHandler, HashSet seen)
            throws Exception {
        super.addSpecialToEntryForm(request, sb, parentEntry, entry,
                                    formInfo, baseTypeHandler, seen);
	sb.append(formEntryTop(request, msgLabel("Json text"),
			       HtmlUtils.textArea(ARG_TEXT, "", 50, 60)));
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    @Override
    public String getUploadedFile(Request request) {
        try {
            if (request.defined(ARG_FILE)) {
                return request.getUploadedFile(ARG_FILE);
            }
            String name = request.getString(ARG_NAME, "").trim();
            if (name.length() == 0) {
                name = "file";
            }
            if ( !name.toLowerCase().endsWith(".json")) {
                name = name + ".json";
            }
            File         f   = getStorageManager().getTmpFile(request, name);
            OutputStream out = getStorageManager().getFileOutputStream(f);
            out.write(request.getString(ARG_TEXT, "").getBytes());
            out.flush();
            out.close();

            return f.toString();
        } catch (Exception exc) {
            throw new RuntimeException(exc);

        }
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

        if ( !tag.equals("json.view")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }

        if ( !entry.isFile()) {
            return "No Json file available";
        }
	//Limit the size
	if(entry.getResource().getFileSize()>1000*1000*5) {
	    return "";
	}
        StringBuilder sb = new StringBuilder();
        HU.importJS(sb, getPageHandler().makeHtdocsUrl("/media/json.js"));
	String json=null;
        try {
            String id = Utils.getGuid();
            json =
                getStorageManager().readEntry(entry);
            String formatted = JsonUtil.format(json, true);
            HtmlUtils.open(sb, "div", "id", id);
            HtmlUtils.pre(sb, formatted);
            HtmlUtils.close(sb, "div");
	    sb.append(HtmlUtils.importJS(getRepository().getHtdocsUrl("/jsonutil.js")));
            sb.append(HtmlUtils.script("RamaddaJsonUtil.init('" + id + "');"));
        } catch (Exception exc) {
            sb.append("Error formatting JSON: " + exc);
	    System.err.println("Error formatting JSON:"  + exc +"\n" +json);
            exc.printStackTrace();
        }

        return sb.toString();
    }



}
