/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.archive;



import org.ramadda.repository.*;
import org.ramadda.repository.database.DatabaseManager;
import org.ramadda.repository.database.Tables;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.JsonUtil;
import org.json.*;

import java.util.Iterator;
import java.net.URL;
import org.ramadda.util.WikiUtil;
import ucar.unidata.util.StringUtil;
import org.ramadda.util.sql.Clause;
import org.w3c.dom.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;



import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;


@SuppressWarnings("unchecked")
public class BookTypeHandler extends GenericTypeHandler  {

    public BookTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {
	super.initializeNewEntry(request, entry, newType);
	if(!isNew(newType)) return;
	if(!request.get("initisbn",false)) return;
	try {
	    processISBN(request, entry);

	} catch(Exception exc) {
	    getLogManager().logError("BookType Handler processing  ISBN",exc);
	    getSessionManager().addSessionMessage(request, "There was an error reading the ISBN:" + exc);
	}
    }

    private void processISBN(Request request, Entry entry) throws Exception {
	String isbn = entry.getStringValue(request,"isbn",null);
	if(!stringDefined(isbn)) return;
	int cnt=0;
	for(String _isbn: Utils.split(isbn,"\n",true,true)) {
	    String url = "https://openlibrary.org/api/books?bibkeys=ISBN:" + _isbn+"&format=json&jscmd=data";
	    IO.Result result = IO.doGetResult(new URL(url));
	    if(result.getError()) {
		getLogManager().logInfo("BookTypeHandler: error reading:"+ url);
		System.err.println(result.getResult());
		getLogManager().logInfo("BookTypeHandler: error reading:"+ url);		
		continue;
	    }
            JSONObject  obj   = new JSONObject(result.getResult());
	    Iterator<String> keys = obj.keys();
	    while (keys.hasNext()) {
		String key= keys.next();

		JSONObject o = obj.getJSONObject(key);
		String title = o.optString("title",null);
		if(title==null) continue;
		cnt++;


		if(!stringDefined(entry.getName())) entry.setName(title);

		String notes = o.optString("notes",null);		
		if(!stringDefined(entry.getDescription())) entry.setDescription(notes);

		String date = o.optString("publish_date",null);
		if(stringDefined(date)) {
		    Date dttm = Utils.parseDate(date);
		    if(dttm!=null) {
		       entry.setStartAndEndDate(dttm.getTime());
		    }
		}
		JSONArray subjects = o.optJSONArray("subjects");
		if(subjects!=null) {
		    for (int i = 0; i < subjects.length(); i++) {
			String subject = subjects.getJSONObject(i).optString("name",null);
			if(stringDefined(subject)) 
			    getMetadataManager().addMetadata(request, entry, "archive_subject",false,subject);
		    }
		    
		}


		JSONObject ids= o.optJSONObject("identifiers");
		if(ids!=null) {
		    JSONArray lccn = ids.optJSONArray("lccn");
		    if(lccn!=null) {
			List<String> a = new ArrayList<String>();
			for (int i = 0; i < lccn.length(); i++) {
			    a.add(lccn.getString(i));
			}
			entry.setValue("lccn",Utils.join(a,"\n"));
		    }

			
		}

		entry.setValue("authors",getNames(o,"authors"));
		entry.setValue("publisher",getNames(o,"publishers"));
		entry.setValue("publication_place",getNames(o,"publish_places"));

		JSONObject cover= o.optJSONObject("cover");
		if(cover!=null) {
		    String thumb = cover.optString("large",null);
		    if(thumb==null)
			thumb = cover.optString("medium",null);
		    if(thumb==null)
			thumb = cover.optString("small",null);					    
		    if(thumb!=null) {
			getMetadataManager().addThumbnailUrl(request, entry,thumb,null,"Credit: openlibrary.org");
		    }
		}


	    }
	}
	if(cnt==0) {
	    getSessionManager().addSessionMessage(request, "No ISBN information found");
	}

    }

    private String getNames(JSONObject o,String key) {
	JSONArray places = o.optJSONArray(key);
	List<String> a = new ArrayList<String>();
	if(places!=null) {
	    for (int i = 0; i < places.length(); i++) {
		String s = places.getJSONObject(i).optString("name",null);
		if(s!=null)
		    a.add(s);
	    }
	}
	return Utils.join(a,"\n");
    }

    /**
       Add the lookup info checkbox
     */
    @Override
    public void addWidgetHelp(Request request,Entry entry,Appendable formBuffer,Column column,Object[]values) throws Exception {
	if(entry!=null || !column.getName().equals("isbn")) {
	    super.addWidgetHelp(request,entry, formBuffer, column, values);
	    return;
	}
	HU.formEntry(formBuffer, "",
		     HU.labeledCheckbox("initisbn","true", false,
					"Initialize book from openlibrary.org using ISBN"));
    }



}
