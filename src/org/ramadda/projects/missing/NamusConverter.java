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
    static int imageCnt = 0;
    static final JsonUtil JU=null;
    static Hashtable emap = Utils.makeHashtable("American Indian / Alaska Native","native_american",
						"Multiple","multiple");
    public static void processFile(String file) throws Exception{
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
	JSONObject _address = _sighting.getJSONObject("address");
	JSONObject _geolocation = _sighting.getJSONObject("publicGeolocation");					
	JSONArray _images = root.getJSONArray("images");
	JSONArray _tribe = _desc.getJSONArray("tribeAssociations");
	StringBuilder sb = new StringBuilder();
	sb.append("<entry ");
	sb.append(XmlUtil.attr("type","type_missing_person"));
	sb.append(XmlUtil.attr("parent",""));
	sb.append(XmlUtil.attr("status","missing"));		
	String firstName = _id.getString("firstName");
	String middleName = _id.optString("middleName","");	
	String lastName = _id.getString("lastName");	
	sb.append(XmlUtil.attr("case_number",root.getString("idFormatted")));
	sb.append(XmlUtil.attr("first_name",firstName));
	sb.append(XmlUtil.attr("middle_name",middleName));	
	sb.append(XmlUtil.attr("last_name",lastName));
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
	sb.append("\n");
	sb.append(">\n");

	JSONArray _features = root.getJSONArray("physicalFeatureDescriptions");
	StringBuilder tmp = new StringBuilder();
	for(int i=0;i<_features.length();i++) {
	    tmp.append(_features.getJSONObject(i).getString("description"));
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

	String _thumb = thumb.replace("/","_");
	File f=new File("images/" + _thumb+".png");
	if(!f.exists()) {
	    FileOutputStream fos= new FileOutputStream(f);
	    String url = "https://www.namus.gov" +thumb;
	    System.err.println("copying thumb");
	    IO.writeFile(new URL(url),fos);
	    fos.close();
	}

	String fileId = _thumb+".png";
	sb.append("<metadata  inherited=\"false\" type=\"content.thumbnail\">");
	sb.append("<attr fileid=\"" + fileId +"\" index=\"1\" encoded=\"false\">");
	sb.append(XmlUtil.getCdata("thumbnail.png"));
	sb.append("</attr>");
	sb.append("</metadata>\n");

	String circ  = _circ.getString("circumstancesOfDisappearance");
	sb.append("\n<description>");
	circ="+callout-info\nNote: this is an example of RAMADDA's Missing Person entry type. The original data came from the [https://namus.nij.ojp.gov/ National Missing and Unidentified Persons System (NAMUS)]\n-callout\n" +circ;
	sb.append(XmlUtil.getCdata(circ));
	sb.append("</description>\n");
	    

	sb.append("/n</entry>\n");
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

    public static void main(String[]args) throws Exception{
	System.out.println("<entries>");
	for(String file: args) {
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
