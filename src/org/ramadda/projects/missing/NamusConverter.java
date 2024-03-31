/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.projects.missing;



import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.JsonUtil;

import org.json.*;

import org.w3c.dom.*;


import ucar.unidata.xml.XmlUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;


import java.text.SimpleDateFormat;
import java.io.*;


import java.net.URL;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import java.util.List;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 */
public class NamusConverter {
    static boolean ok = true;
    static String BASEURL =  "https://www.namus.gov";
    static int imageCnt = 0;
    static final JsonUtil JU=null;
    static Hashtable emap = Utils.makeHashtable("American Indian / Alaska Native","native_american",
						"Multiple","multiple");
    public static void processFile(File file) throws Exception{
	String json = IO.readContents(file);
	if(json.length()==0) {
	    //	    System.err.println("empty file:" + file);
	    return;
	}
	JSONObject root = new JSONObject(json);
	JSONObject _id = root.getJSONObject("subjectIdentification");
	JSONObject _desc = root.getJSONObject("subjectDescription");
	JSONObject _circ = root.getJSONObject("circumstances");
	JSONObject _phys = root.getJSONObject("physicalDescription");
	JSONObject _sighting = root.getJSONObject("sighting");
	JSONArray _docs = root.optJSONArray("documents");	
	if(_docs!=null && _docs.length()>0) {
	    //	    System.err.println("DOCS:" + _docs);
	}
	JSONArray _images = root.optJSONArray("images");	
	JSONArray _notes = root.optJSONArray("notes");	
	JSONObject _address = _sighting.getJSONObject("address");
	JSONObject _geolocation = _sighting.getJSONObject("publicGeolocation");					
	JSONArray _tribe = _desc.getJSONArray("tribeAssociations");
	StringBuilder sb = new StringBuilder();
	String uid = HtmlUtils.getUniqueId("entry");
	sb.append("<entry ");
	sb.append(XmlUtil.attr("id",uid));
	sb.append(XmlUtil.attr("type","type_missing_person"));
	sb.append(XmlUtil.attr("parent",""));
	sb.append(XmlUtil.attr("status","missing"));		
	int maxAge = _id.getInt("currentMaxAge");
	int missingMinAge = _id.getInt("computedMissingMinAge");
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	Date now = new Date();
	Date bdate = new Date(now.getTime()-(Utils.yearsToMillis(maxAge)));
	sb.append(XmlUtil.attr("fromdate",sdf.format(bdate)));
	Date missingDate = new Date(now.getTime()-Utils.yearsToMillis(maxAge-missingMinAge));
	sb.append(XmlUtil.attr("date_missing",sdf.format(missingDate)));	
	String firstName = _id.getString("firstName");
	String middleName = _id.optString("middleName","");	
	String lastName = _id.getString("lastName");	
	String nickName = _id.optString("nicknames",null);
	sb.append(XmlUtil.attr("case_number",root.getString("idFormatted")));
	sb.append(XmlUtil.attr("first_name",firstName));
	sb.append(XmlUtil.attr("middle_name",middleName));	
	sb.append(XmlUtil.attr("last_name",lastName));
	if(nickName!=null)
	    sb.append(XmlUtil.attr("nickname",nickName));
	String name = firstName +" " + (Utils.stringDefined(middleName)?middleName+" ":"") +lastName;			       
	sb.append(XmlUtil.attr("name",name));

	if(_tribe.length()>0) {
	    sb.append(XmlUtil.attr("tribe_association",JU.readValue(_tribe.getJSONObject(0),"tribe.tribeName","")));
	}
	sb.append(XmlUtil.attr("missing_from_tribal_land",
			       JU.readValue(_sighting,"missingFromTribalLand.name","").toLowerCase()));

	sb.append(XmlUtil.attr("primary_residence_tribal_land",
			       JU.readValue(_sighting,"primaryResidenceOnTribalLand.name","").toLowerCase()));


	sb.append(XmlUtil.attr("missing_city",_address.getString("city")));
	sb.append(XmlUtil.attr("missing_state",JU.readValue(_address,"state.name","")));
	sb.append(XmlUtil.attr("missing_county",JU.readValue(_address,"county.displayName","")));

	JSONObject _coords =_geolocation.getJSONObject("coordinates");
	sb.append(XmlUtil.attr("latitude",""+_coords.getDouble("lat")));
	sb.append(XmlUtil.attr("longitude",""+_coords.getDouble("lon")));
	


	sb.append(XmlUtil.attr("height",""+_desc.getInt("heightFrom")));
	sb.append(XmlUtil.attr("weight",""+_desc.getInt("weightFrom")));	
	String sex = JsonUtil.readValue(_desc,"sex.name","unsure");
	sb.append(XmlUtil.attr("biological_sex",sex.toLowerCase()));
	String e = JsonUtil.readValue(_desc,"primaryEthnicity.name","");
	String _e =  e.toLowerCase();

	String ethnicity = Utils.getProperty(emap,e,Utils.getProperty(emap,_e,"unknown"));
	sb.append(XmlUtil.attr("race_ethnicity",ethnicity));


	sb.append("\n");
	sb.append(XmlUtil.attr("hair_color",JU.readValue(_phys,"hairColor.name","unknown").toLowerCase()));
	sb.append("\n");
	sb.append(XmlUtil.attr("left_eye_color",JU.readValue(_phys,"leftEyeColor.name","unknown").toLowerCase()));
	sb.append("\n");
	sb.append(XmlUtil.attr("right_eye_color",JU.readValue(_phys,"rightEyeColor.name","unknown").toLowerCase()));
	String headHair = _phys.optString("headHairDescription",null);
	if(Utils.stringDefined(headHair)) 
	    sb.append(XmlUtil.attr("head_hair_description",headHair));
	String facialHair = _phys.optString("facialHairDescription",null);
	if(Utils.stringDefined(facialHair)) 
	    sb.append(XmlUtil.attr("facial_hair_description",facialHair));	
	
	sb.append("\n");
	sb.append(">\n");

	JSONArray _features = root.getJSONArray("physicalFeatureDescriptions");
	StringBuilder tmp = new StringBuilder();
	for(int i=0;i<_features.length();i++) {
	    tmp.append(_features.getJSONObject(i).optString("description",""));
	    tmp.append("\n");
	}
	if(tmp.length()>0) {
	    sb.append("<distinctive_physical_features>");
	    sb.append(XmlUtil.getCdata(tmp.toString().trim()));
	    sb.append("</distinctive_physical_features>\n");
	}

	tmp = new StringBuilder();
	_features = root.getJSONArray("clothingAndAccessoriesArticles");
	for(int i=0;i<_features.length();i++) {
	    tmp.append(_features.getJSONObject(i).getString("description"));
	    tmp.append("\n");
	}
	if(tmp.length()>0) {
	    sb.append("<clothing_and_accessories>");
	    sb.append(XmlUtil.getCdata(tmp.toString().trim()));
	    sb.append("</clothing_and_accessories>\n");
	}


    

	String thumb = root.getString("hrefDefaultImageThumbnail");
	String _thumb = getFile(thumb,"png");
	String fileId = _thumb;
	sb.append("<metadata  inherited=\"false\" type=\"content.thumbnail\">");
	sb.append("<attr fileid=\"" + fileId +"\" index=\"1\" encoded=\"false\">");
	sb.append(XmlUtil.getCdata("thumbnail.png"));
	sb.append("</attr>");
	sb.append("</metadata>\n");


	if(_notes!=null) {
	    for(int i=0;i<_notes.length();i++) {
		JSONObject _note = _notes.getJSONObject(i);
		sb.append("<metadata  inherited=\"false\" type=\"missing_note\">");
		mtd(sb,1,_note.getString("description"));
		sb.append("</metadata>\n");
	    }
	}



	//	isowner,department,contact,role,address,county,state,phone,email,url,#
	JSONArray _agencies = root.getJSONArray("investigatingAgencies");
	for(int i=0;i<_agencies.length();i++) {
	    JSONObject obj = _agencies.getJSONObject(i);
	    JSONObject _agency =JU.readObject(obj,"selection.agency");	
	    if(_agency==null) continue;
	    JSONObject _contact =JU.readObject(obj,"selection.contact");	
	    sb.append("<metadata  inherited=\"false\" type=\"missing_agency\">");
	    int midx=1;
	    mtd(sb,midx++,obj.getBoolean("isCaseOwner"));
	    mtd(sb,midx++,obj.getString("name"));
	    if(_contact!=null) {
		mtd(sb,midx++,_contact.getString("firstName") +" " +_contact.getString("lastName"));
		mtd(sb,midx++,_contact.getString("jobTitle"));
	    }	else {
		mtd(sb,midx++,"");
		mtd(sb,midx++,"");
	    }
	    mtd(sb,midx++,_agency.getString("street1"));
	    mtd(sb,midx++,JU.readValue(_agency,"county.name",""));
	    mtd(sb,midx++,JU.readValue(_agency,"state.name",""));
	    mtd(sb,midx++,JU.readValue(_agency,"phone",""));	    	    
	    //dummy email and url
	    mtd(sb,midx++,"");
	    mtd(sb,midx++,"");	    
	    mtd(sb,midx++,obj.optString("caseNumber",""));
	    sb.append("</metadata>\n");
	}


	String circ  = _circ.optString("circumstancesOfDisappearance","");
	sb.append("\n<description>");
	circ="+callout-info\nNote: this is an example of RAMADDA's Missing Person entry type. The original data came from the [https://namus.nij.ojp.gov/ National Missing and Unidentified Persons System (NAMUS)]\n-callout\n" +circ;
	sb.append(XmlUtil.getCdata(circ));
	sb.append("\n</description>");
	sb.append("\n</entry>\n");

	if(_images!=null && _images.length()>0) {
	    String puid = HtmlUtils.getUniqueId("entry");
	    sb.append("<entry ");
	    sb.append(XmlUtil.attr("id",puid));
	    sb.append(XmlUtil.attr("parent",uid));
	    sb.append(XmlUtil.attr("type","media_photoalbum"));
	    sb.append(XmlUtil.attr("name","Photos"));
	    sb.append(">\n</entry>\n");
	    for(int i=0;i<_images.length();i++) {
		JSONObject _image = _images.getJSONObject(i);
		String imageEntryName = _image.optString("caption",name+" Image");
		String url = JU.readValue(_image,"files.original.href",null);
		String mime = JU.readValue(_image,"files.original.mimeType","jpg");		
		String suffix = "jpg";
		if(mime.indexOf("png")>=0) suffix="png";
		if(url==null) {
		    System.err.println("No original image:" + _image);
		}
		String filename = Utils.makeID(imageEntryName)+"."+ suffix;
		sb.append("<entry ");
		sb.append(XmlUtil.attr("parent",puid));
		sb.append(XmlUtil.attr("type","type_image"));
		sb.append(XmlUtil.attr("name",imageEntryName));
		sb.append(XmlUtil.attr("filename",filename));
		String _file = getFile(url,suffix);
		sb.append(XmlUtil.attr("file",_file));
		sb.append(">\n</entry>\n");
	    }
	}		

	System.out.println(sb);

	/*
	JSONArray vehicles  = root.getJSONArray("vehicles");
	if(vehicles.length()>0) {
	    System.err.println(vehicles);
	    for (int i = 0; i < vehicles.length(); i++) {
	    }
	}
	*/

    }

    private static String getFile(String url,String suffix) throws Exception {
	String _url = url.replace("/","_")+"." + suffix;
	File f=new File("images/" + _url);
	if(!f.exists()) {
	    FileOutputStream fos= new FileOutputStream(f);
	    String theUrl = BASEURL +url;
	    System.err.println("fetching  image:" + url);
	    IO.writeFile(new URL(theUrl),fos);
	    fos.close();
	}
	return _url;
    }



    private static void mtd(Appendable sb, int index,Object  contents) throws Exception {
	sb.append("<attr index=\""+ index+"\" encoded=\"false\">");
	sb.append(XmlUtil.getCdata(contents.toString()));
	sb.append("</attr>\n");
    }



    public static void main(String[]args) throws Exception{
	if(args.length!=1) {
	    System.err.println("Error: usage: NamusConverter <file of namus case ids>");
	    System.exit(1);
	}
	List<String> cases = Utils.split(IO.readInputStream(new FileInputStream(args[0])),"\n",true,true);
	String URL="https://www.namus.gov/api/CaseSets/NamUs/MissingPersons/Cases/";

	System.out.println("<entries>");
	for(int i=1;i<cases.size();i++) {
	    if(!ok) break;
	    String caseID = cases.get(i);
	    File file = new File(caseID+".json");
	    if(!file.exists()) {
		URL  url = new URL(URL+caseID);
		IO.writeFile(url,new FileOutputStream(file));
	    }
	    try {
		//		System.err.println("processing:" +file);
		processFile(file);
		//		if(true) break;
	    } catch(Exception exc) {
		System.err.println("Error reading file:" + file+ " error:" + exc.getMessage());
		exc.printStackTrace();
		System.exit(0);
	    }		
	}
	System.out.println("</entries>");
	System.exit(0);
    }

}
