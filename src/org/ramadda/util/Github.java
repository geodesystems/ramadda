// Copyright (c) 2008-2025 Geode Systems LLC
// SPDX-License-Identifier: Apache-2.0


package org.ramadda.util;


import org.json.*;


import ucar.unidata.util.DateUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.IOException;

import java.net.URL;

import java.text.SimpleDateFormat;
import java.util.TimeZone;


import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.*;



/**
 */
@SuppressWarnings("unchecked")
public class Github {



    

    public static List<Item>  fetch(SystemContext handler, Hashtable props) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        sdf.setTimeZone(Utils.TIMEZONE_DEFAULT);
        SimpleDateFormat rsdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmz");
        rsdf.setTimeZone(Utils.TIMEZONE_DEFAULT);	
	String user = Utils.getProperty(props,"user",(String)null);
	String owner = Utils.getProperty(props,"owner",(String)null);
	String repository = Utils.getProperty(props,"repository",(String)null);	
	int max = Utils.getProperty(props,"max",100);
	String since = Utils.getProperty(props,"since",(String)null);
	if(since!=null) {
	    since = rsdf.format(DateUtil.getRelativeDate(new Date(), since));	    
	}
	String until = Utils.getProperty(props,"until",(String)null);		
	if(until!=null) {
	    until = rsdf.format(DateUtil.getRelativeDate(new Date(), until));	    
	}

	Function<String,JSONArray> getJson = (url)->{
	    try {
		//Cache for 10 minutes
		String json = handler.getSystemContextCache(url,Utils.minutesToMillis(10));
		if(json==null) {
		    String token = handler.getSystemContextProperty("github.token",null);
		    json = IO.doHttpRequest("GET",new URL(url),null,
					    "Authorization",token!=null?"token "+token:null);
		    handler.putSystemContextCache(url,json);		    
		    //		    System.out.println(json);
		} else {
		    //		    System.err.println("got cache:" + json.substring(0,50));
		}
		
		return  new JSONArray(json);
	    } catch(Exception exc) {
		throw new RuntimeException(exc);
	    }
	};


	List<Item> results = new ArrayList<Item>();

	boolean decorate = Utils.getProperty(props,"decorate",true);
	boolean showAuthor = Utils.getProperty(props,"showAuthor",true);	
	String height = Utils.getProperty(props,"height","200");
	if(Utils.stringDefined(user)) {
	    String apiUrl = HtmlUtils.url("https://api.github.com/users/" + user+"/events/public","per_page","" + max);
            JSONArray a = getJson.apply(apiUrl);
	    
	    int cnt = 0;
            for (int itemIdx = 0; itemIdx < a.length(); itemIdx++) {
                JSONObject item = a.getJSONObject(itemIdx);
		if(!item.has("payload")) {
		    continue;
		}
		String login = null;
		String avatarUrl = null;
		if(item.has("actor")) {
		    JSONObject actor = item.getJSONObject("actor");
		    login = actor.optString("login",null);
		    avatarUrl = actor.optString("avatar_url",null);		    
		}

		JSONObject payload = item.getJSONObject("payload");
		if(payload.has("commits")) {
		    JSONArray commits = payload.getJSONArray("commits");
		    Date date=null;
		    String sdate = item.optString("created_at","");
		    if(sdate.length()>0) date = Utils.parseDate(sdate);
		    for (int commitIdx = 0; commitIdx < commits.length(); commitIdx++) {
			JSONObject commit = commits.getJSONObject(commitIdx);
			if(cnt>max) break;
			String message = commit.optString("message","").replaceAll("<","&lt;").replaceAll(">","&gt;");
			String url = commit.getString("url");
			url = url.replace("//api.","//").replace("/repos/","/").replace("/commits/","/commit/");
			String name = JsonUtil.readValue(commit,"author.name","NA");
			String authorUrl = login==null?null: "https://github.com/" + login;
			results.add(new Item(new User(name,login, authorUrl, avatarUrl), date, message,url));
		    }
		}
	    }
	} else 	if(Utils.stringDefined(owner) && Utils.stringDefined(repository)) {
	    String apiUrl = HtmlUtils.url("https://api.github.com/repos/" + owner+"/" + repository+"/commits","per_page","" + max);
	    if(since!=null) apiUrl+="&since=" + since;
	    if(until!=null) apiUrl+="&until=" + until;	    
            JSONArray a = getJson.apply(apiUrl);
	    int cnt = 0;
            for (int commitIdx = 0; commitIdx < a.length(); commitIdx++) {
		if(cnt>max) break;
                JSONObject item = a.getJSONObject(commitIdx);
                JSONObject commit = item.getJSONObject("commit");
                JSONObject committer = commit.getJSONObject("committer");		
                JSONObject author = item.getJSONObject("author");				

		String login = author.optString("login",null);
		String avatarUrl = author.optString("avatar_url",null);
		String authorUrl = author.getString("html_url");		
		String name = committer.getString("name");
		Date date = null;
		String sdate = committer.getString("date");
		if(sdate.length()>0) date = Utils.parseDate(sdate);
		String message = commit.getString("message").replaceAll("<","&lt;").replaceAll(">","&gt;");
		String url = item.getString("html_url");
		name= HtmlUtils.href(authorUrl, name);
		results.add(new Item(new User(name,login, authorUrl, avatarUrl), date, message,url));
	    }
	}
	
	return results;
    }


    public static class User {
	private String login;
	private String name;
	private String url;
	private String avatarUrl;
    
	public User(String name, String login, String  url, String avatarUrl) {
	    this.login = login;
	    this.name = name;
	    this.url =  url;
	    this.avatarUrl = avatarUrl;
	}


	/**
	   Set the Login property.

	   @param value The new value for Login
	**/
	public void setLogin (String value) {
	    login = value;
	}

	/**
	   Get the Login property.

	   @return The Login
	**/
	public String getLogin () {
	    return login;
	}

	/**
	   Set the Name property.

	   @param value The new value for Name
	**/
	public void setName (String value) {
	    name = value;
	}

	/**
	   Get the Name property.

	   @return The Name
	**/
	public String getName () {
	    return name;
	}


	/**
	   Get the AuthorUrl property.

	   @return The AuthorUrl
	**/
	public String getUrl () {
	    return url;
	}

	/**
	   Set the AvatarUrl property.

	   @param value The new value for AvatarUrl
	**/
	public void setAvatarUrl (String value) {
	    avatarUrl = value;
	}

	/**
	   Get the AvatarUrl property.

	   @return The AvatarUrl
	**/
	public String getAvatarUrl () {
	    return avatarUrl;
	}

    }


    public static class Item {
	private User user;
	private Date date;
	private String message;
	private String itemUrl;

	public Item(User user,  Date date, String message, String itemUrl) {
	    this.user = user;
	    this.date = date;
	    this.message = message;
	    this.itemUrl = itemUrl;
	}

	/**
	   Set the User property.

	   @param value The new value for User
	**/
	public void setUser (User value) {
	    user = value;
	}

	/**
	   Get the User property.

	   @return The User
	**/
	public User getUser () {
	    return user;
	}


	/**
	   Set the Date property.

	   @param value The new value for Date
	**/
	public void setDate (Date value) {
	    date = value;
	}

	/**
	   Get the Date property.

	   @return The Date
	**/
	public Date getDate () {
	    return date;
	}

	/**
	   Set the Message property.

	   @param value The new value for Message
	**/
	public void setMessage (String value) {
	    message = value;
	}

	/**
	   Get the Message property.

	   @return The Message
	**/
	public String getMessage () {
	    return message;
	}

	/**
	   Set the ItemUrl property.

	   @param value The new value for ItemUrl
	**/
	public void setItemUrl (String value) {
	    itemUrl = value;
	}

	/**
	   Get the ItemUrl property.

	   @return The ItemUrl
	**/
	public String getItemUrl () {
	    return itemUrl;
	}

    }

}
