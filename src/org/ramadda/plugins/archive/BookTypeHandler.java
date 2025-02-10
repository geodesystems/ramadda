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
	String isbn = entry.getStringValue(request,"isbn",null);
	if(!stringDefined(isbn)) return;

	try {
	    State state = new State();
	    try {
		processISBNOpenlibrary(request, entry,state,isbn);
	    } catch(Exception exc) {
		getSessionManager().addSessionMessage(request, "Error reading openlibrary:" + exc);
	    }
	    try {
		processISBNGoogle(request, entry,state,isbn);
	    } catch(Exception exc) {
		getSessionManager().addSessionMessage(request, "Error reading google books:" + exc);
	    }
	    if(stringDefined(state.name)&& !stringDefined(entry.getName())) entry.setName(state.name);
	    if(state.date!=null) {
		Date dttm = Utils.parseDate(state.date);
		if(dttm!=null) {
		    entry.setStartAndEndDate(dttm.getTime());
		}
	    }
	    if(state.description.length()>0) {
		String desc =  state.description.toString().replace("[","\\[").trim();
		if(!stringDefined(entry.getDescription())) entry.setDescription(desc);
	    }
	    if(state.authors!=null)
		entry.setValue("authors",state.authors);
	    if(state.publishers!=null)
		entry.setValue("publisher",state.publishers);
	    if(state.places!=null)
		entry.setValue("publication_place",state.places);
	    if(state.thumb!=null)  {
		getMetadataManager().addThumbnailUrl(request, entry,state.thumb,state.thumbName,state.thumbCredit);
	    }
	    if(state.cnt==0) {
		getSessionManager().addSessionMessage(request, "No ISBN information found");
	    }

	} catch(Exception exc) {
	    getLogManager().logError("BookType Handler processing  ISBN",exc);
	    getSessionManager().addSessionMessage(request, "There was an error reading the ISBN:" + exc);
	}
    }

    private static class State {
	int cnt=0;
	String name = null;
	StringBuilder description = new StringBuilder();
	String date;
	String thumb = null;
	String thumbName=null;
	String thumbCredit = null;	
	String authors = null;
	String publishers  =null;
	String places = null;
    }

    private void processISBNOpenlibrary(Request request, Entry entry, State state,String isbn) throws Exception {
	for(String _isbn: Utils.split(isbn,"\n",true,true)) {
	    String url = "https://openlibrary.org/api/books?bibkeys=ISBN:" + _isbn+"&format=json&jscmd=data";
	    IO.Result result = IO.doGetResult(new URL(url));
	    if(result.getError()) {
		getLogManager().logInfo("BookTypeHandler: error reading:"+ url);
		continue;
	    }
            JSONObject  obj   = new JSONObject(result.getResult());
	    Iterator<String> keys = obj.keys();
	    while (keys.hasNext()) {
		String key= keys.next();
		JSONObject o = obj.getJSONObject(key);
		state.name = o.optString("title",null);
		if(state.name==null) continue;
		state.cnt++;
		state.description.append(o.optString("notes",""));		
		String bookUrl = o.optString("url",null);
		if(stringDefined(bookUrl)) {
		    getMetadataManager().addMetadata(request, entry, "content.url",false,
						     bookUrl,
						     "openlibrary.org link");
		}

		JSONArray ebooks = o.optJSONArray("ebooks");
		if(ebooks!=null) {
		    for (int i = 0; i < ebooks.length(); i++) {
			JSONObject ebook = ebooks.getJSONObject(i);
			String purl = ebook.optString("preview_url",null);
			if(purl!=null) {
			    URL _url = new URL(purl);
			    getMetadataManager().addMetadata(request, entry, "content.url",false,
							     purl,
							     "Preview @ " + _url.getHost());
			}
		    }
		}

		state.date = o.optString("publish_date",null);
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

		if(state.authors==null)
		    state.authors = getNames(o,"authors");
		if(state.publishers==null)
		    state.publishers  =getNames(o,"publishers");
		if(state.places==null)
		    state.places = getNames(o,"publish_places");

		JSONObject cover= o.optJSONObject("cover");
		if(state.thumb!=null && cover!=null) {
		    state.thumb = cover.optString("large",null);
		    if(state.thumb==null)
			state.thumb = cover.optString("medium",null);
		    if(state.thumb==null)
			state.thumb = cover.optString("small",null);					    
		    if(state.thumb!=null)
			state.thumbCredit = "Credit: openlibrary.org";
		}


	    }


	}
    }

    private void processISBNGoogle(Request request, Entry entry, State state,String isbn) throws Exception {
	for(String _isbn: Utils.split(isbn,"\n",true,true)) {
	    String url = "https://www.googleapis.com/books/v1/volumes?q=isbn:" + isbn;
	    IO.Result result = IO.doGetResult(new URL(url));
	    if(result.getError()) {
		getLogManager().logInfo("BookTypeHandler: error reading:"+ url);
		continue;
	    }
            JSONObject  obj   = new JSONObject(result.getResult());
	    JSONArray items = obj.optJSONArray("items");
	    if(items==null) return;
	    for (int i = 0; i < items.length(); i++) {
		JSONObject item = items.getJSONObject(i);
		JSONObject volume = item.getJSONObject("volumeInfo");
		if(state.name==null)
		    state.name  = volume.optString("title",null);
		state.cnt++;
		if(state.description.length()>0) state.description.append("\n:p\n");
		state.description.append(volume.optString("description",""));
		if(state.date==null) 
		    state.date = volume.optString("publishedDate",null);
		if(state.authors==null)  {
		    List<String> a = JU.getStrings(volume.optJSONArray("authors"));
		    state.authors = Utils.join(a,"\n");
		}
		if(state.publishers==null)
		    state.publishers  =volume.optString("publisher",null);

		JSONObject cover= volume.optJSONObject("imageLinks");
		if(state.thumb==null && cover!=null) {
		    for(String k:new String[]{"thumbnail","medium","large","smallThumbnail"}) {
			if(state.thumb==null) {
			    state.thumb = cover.optString(k,null);
			}
		    }
		    if(state.thumb!=null) {
			state.thumbCredit = "Credit: Google Books";
			state.thumbName = "thumbnail.jpg";
		    }
		}
	    }


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
					"Initialize book from openlibrary.org and Google Books using ISBN"));
    }


}
