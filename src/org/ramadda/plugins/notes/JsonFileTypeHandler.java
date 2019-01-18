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

package org.ramadda.plugins.notes;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.FormInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.Json;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import java.io.*;

import java.util.Hashtable;


/**
 *
 *
 */
public class JsonFileTypeHandler extends GenericTypeHandler {


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
    public void addSpecialToEntryForm(Request request, Appendable sb,
                                      Entry parentEntry, Entry entry,
                                      FormInfo formInfo,
                                      TypeHandler baseTypeHandler)
            throws Exception {
        super.addSpecialToEntryForm(request, sb, parentEntry, entry,
                                    formInfo, baseTypeHandler);
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
        StringBuilder sb = new StringBuilder();
        try {
            String id = Utils.getGuid();
            String formatted = Json.format(entry.getResource().getPath(),
                                           true);
            HtmlUtils.open(sb, "div", "id", id);
            HtmlUtils.pre(sb, formatted);
            HtmlUtils.close(sb, "div");
            sb.append(HtmlUtils.script("ramaddaJsonInit('" + id + "');"));
        } catch (Exception exc) {
            sb.append("Error formatting JSON: " + exc);
            exc.printStackTrace();
        }

        return sb.toString();
    }



}
