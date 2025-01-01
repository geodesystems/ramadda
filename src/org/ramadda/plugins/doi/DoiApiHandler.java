/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.doi;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.harvester.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.HtmlUtils;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;


import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.*;


/**
 * Provides a top-level API
 *
 */
public class DoiApiHandler extends RepositoryManager implements RequestHandler {


    /** _more_ */
    public static final String ARG_DOI = "doi";

    /**
     *
     * @param repository _more_
     *
     * @throws Exception _more_
     */
    public DoiApiHandler(Repository repository) throws Exception {
        super(repository);
    }


    public Result processArkAccess(Request request) throws Exception {
	//https://ramadda.org/ark:/19157/foo
	String path = request.getRequestPath();
	int index  = path.indexOf("ark:/");
	if(index<0)  throw new IllegalArgumentException("Bad ark path format:" + path);
	String id = path.substring(index+"ark:/".length());
	index  = id.indexOf("/");
	if(index<0)  throw new IllegalArgumentException("Bad ark path format:" + path);
	id = id.substring(index);
	id = StringUtil.findPattern(id,"/?([^/\\?]+).*");
	if(id==null)  throw new IllegalArgumentException("Bad ark path format:" + path);

	id = id.replace("_","-");
	return new Result(request.makeUrl(getRepository().URL_ENTRY_SHOW,
					  ARG_ENTRYID, id));
    }



    /**
     * handle the request
     *
     * @param request request
     *
     * @return result
     *
     * @throws Exception on badness
     */
    public Result processDoiSearch(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(HtmlUtils.p());
        if ( !request.defined(ARG_DOI)) {
            makeForm(request, sb);

            return new Result("", sb);
        }

        List<Entry> entries = getEntryManager().getEntriesFromMetadata(request,
                          DoiMetadataHandler.TYPE_DOI,
                          request.getString(ARG_DOI, ""), 2);
        if (entries.size() == 0) {
            sb.append("Could not find DOI:" + request.getString(ARG_DOI, ""));
            sb.append(HtmlUtils.p());
            makeForm(request, sb);

            return new Result("", sb);
        }
	Entry entry = entries.get(0);
        request.put(ARG_ENTRYID, entry.getId());

        return getEntryManager().processEntryShow(request);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     */
    private void makeForm(Request request, StringBuffer sb) {
        String base = getRepository().getUrlBase();
        sb.append(HtmlUtils.formTable());
        sb.append(HtmlUtils.form(base + "/doi"));
        sb.append(HtmlUtils.formEntry("DOI",
                                      HtmlUtils.input(ARG_DOI,
                                          request.getString(ARG_DOI, ""))));

        sb.append(HtmlUtils.formEntry("", HtmlUtils.submit("Find DOI", "")));
        sb.append(HtmlUtils.formClose());
        sb.append(HtmlUtils.formTableClose());
    }

}
