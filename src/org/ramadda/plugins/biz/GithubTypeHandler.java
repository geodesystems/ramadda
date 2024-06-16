/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.biz;


import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;
import org.ramadda.data.record.*;

import org.ramadda.data.services.PointTypeHandler;

import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.Github;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;


import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;

import java.io.*;

import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 */
@SuppressWarnings("unchecked")
public class GithubTypeHandler extends PointTypeHandler {

    //NOTE: This starts at 2 because the point type has a number of points field

    /** _more_ */
    private static int IDX_BASE = 2;

    /** _more_ */
    public static final int IDX_USER = IDX_BASE++;

    /** _more_ */
    public static final int IDX_OWNER= IDX_BASE++;

    /** _more_ */
    public static final int IDX_REPOSITORY = IDX_BASE++;


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public GithubTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getPathForEntry(Request request, Entry entry, boolean forRead)
            throws Exception {
	return "noop";
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param properties _more_
     * @param requestProperties _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public RecordFile doMakeRecordFile(Request request, Entry entry,
                                       Hashtable properties,
                                       Hashtable requestProperties)
            throws Exception {
        return new GithubRecordFile(request,getRepository(),
				    new IO.Path(getPathForEntry(request, entry, true)), entry);
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Jan 3, '19
     * @author         Enter your name here...
     */
    public static class GithubRecordFile extends CsvFile {

	private Request request;

        /** _more_ */
        private Entry entry;

        /** _more_ */
        private Repository repository;

        /**
         * _more_
         *
         *
         * @param repository _more_
         * @param entry _more_
         *
         * @throws IOException _more_
         */
        public GithubRecordFile(Request request,Repository repository, IO.Path path,
                             Entry entry)
                throws IOException {
            super(path);
	    this.request= request;
            this.repository = repository;
            this.entry      = entry;
	    putProperty(PROP_SKIPLINES, "1");
            putFields(new String[] {
		    makeField("date", attrType("date"), attrLabel("Date")
			      ,attrFormat("yyyy-MM-dd HH:mm")),
		    makeField("login", attrType("string"), attrLabel("Login")),
		    makeField("name", attrType("string"), attrLabel("Name")),
		    makeField("user_url", attrType("url"), attrLabel("User URL")),
		    makeField("avatar_url", attrType("image"), attrLabel("Avatar URL")),
		    makeField("item_url", attrType("url"), attrLabel("Commit URL")),
		    makeField("message", attrType("string"), attrLabel("Message"))});
        }


        /**
         * _more_
         *
         * @param buffered _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public InputStream doMakeInputStream(boolean buffered)
                throws Exception {
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	    sdf.setTimeZone(Utils.TIMEZONE_DEFAULT);
	    Hashtable props = Utils.makeHashtable(
						  "user",entry.getStringValue(request,IDX_USER, (String) null),
						  "owner",entry.getStringValue(request,IDX_OWNER, (String) null),
						  "repository",	entry.getStringValue(request,IDX_REPOSITORY, (String) null));		
	    List<Github.Item> items = Github.fetch(repository.getWikiManager(), props);
	    StringBuilder sb = new StringBuilder();
	    sb.append("date,login,name,user_url,avatar_url,item_url,message\n");
	    for(Github.Item item: items) {
		List cols = new ArrayList();
		cols.add(sdf.format(item.getDate()));
		cols.add(item.getUser().getLogin());
		cols.add(item.getUser().getName());
		cols.add(item.getUser().getUrl());
		cols.add(item.getUser().getAvatarUrl());
		cols.add(item.getItemUrl());
		cols.add(item.getMessage());
		Utils.columnsToString(sb,cols,",",true);
	    }
	    return new ByteArrayInputStream(sb.toString().getBytes());
        }


    }


}
