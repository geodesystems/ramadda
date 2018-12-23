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

package org.ramadda.geodata.ogc;


import com.jhlabs.map.proj.Projection;
import com.jhlabs.map.proj.ProjectionFactory;


import org.json.*;

import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.TwoFacedObject;

import java.awt.geom.*;

import java.io.InputStream;

import java.net.URL;

import java.util.ArrayList;

import java.util.Date;
import java.util.List;


/**
 * Class description
 *
 *
 * @version        $version$, Thu, Jul 31, '14
 * @author         Enter your name here...
 */
public class EsriProcessor {

    /** _more_ */
    private static final String URL =
        "?num=500&start=1&sortField=title&sortOrder=desc&q=group:%22{id}%22&f=json";

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        for (String id : args) {
            JSONTokener tokenizer = getTokenizer(id);
            JSONObject  obj       = new JSONObject(tokenizer);
            JSONArray   results   = obj.getJSONArray("results");
            for (int i = 0; i < results.length(); i++) {
                JSONObject result = results.getJSONObject(i);
                String     rid    = result.getString("id");
                String     name   = result.getString("name");
                String     title  = result.getString("title");
                String     type   = result.getString("type");
                String     thumb  = result.getString("thumbnail");
                String file =
                    "https://nga.maps.arcgis.com/sharing/rest/content/items/"
                    + rid + "/data";
                System.out.println("wget -O " + name + "  \"" + file + "\"");
            }
        }
    }

    /*

https://nga.maps.arcgis.com/sharing/rest/content/items/5df40867317e442ea886359fdfb67c4a/info/thumbnail/thumbnail.png
https://nga.maps.arcgis.com/sharing/rest/content/items/5df40867317e442ea886359fdfb67c4a/data


{
"id":"b1417eaaff304d158c18d407715e1cda",
"owner":"nga_public",
"created":1421069964000,
"modified":1421069964000,
"guid":"2BB8F20D-C209-4017-ACC7-BEEBE7497DD0",
"name":"WWHGD_Water_Resource_Lines_Nigeria.mpk",
"title":"WWHGD_Water_Resource_Lines_Nigeria",
"type":"Map Package",
"typeKeywords":[
            "2D",
            "ArcMap",
            "Map",
            "Map Package",
            "mpk",
            "SDE Feature Class"
         ],
         "description":"Nigeria Inland Waterways from the Digital Chart of the World (DCW). The Digital Chart of the World (ESRI 1993) is a global vector map at a resolution of 1:1 million that includes a layer of hydrographic features such as rivers and lakes.",
         "tags":[
            "Ebola",
            "WWHGD",
            "Human geography",
            "Geospatial",
            "United nations",
            "NGA",
            "Africa",
            "Water",
            "OHDR",
            "Water",
            "DivaGIS",
            "Land Use and Cover",
            "Transportation Use",
            "Water Supply and Control"
         ],
         "snippet":"Nigeria Inland Waterways from the Digital Chart of the World (DCW). The Digital Chart of the World (ESRI 1993) is a global vector map at a resolution of 1:1 million that includes a layer of hydrographic features such as rivers and lakes.",
         "thumbnail":"thumbnail/thumbnail.png",
         "documentation":null,
         "extent":[
            [
               2.675009434,
               4.30001257100002
            ],
            [
               14.683343806,
               13.891662619
            ]
         ],
         "spatialReference":"GCS_WGS_1984",
         "accessInformation":"Open Humanitarian Data Repository",
         "licenseInfo":null,
         "culture":"en-us",
         "properties":null,
         "url":null,
         "access":"public",
         "size":-1,
         "appCategories":[

         ],
         "industries":[

         ],
         "languages":[

         ],
         "largeThumbnail":null,
         "banner":null,
         "screenshots":[

         ],
         "listed":false,
         "numComments":0,
         "numRatings":0,
         "avgRating":0,
         "numViews":262
      },

    */


    /**
     * _more_
     *
     * @param url _more_
     *
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private static JSONTokener getTokenizer(String id) throws Exception {
        String url  = URL.replace("{id}", id);
        String json = IOUtil.readContents(url.toString(),
                                          EsriProcessor.class);
        JSONTokener tokenizer = new JSONTokener(json);

        return tokenizer;
    }




}
