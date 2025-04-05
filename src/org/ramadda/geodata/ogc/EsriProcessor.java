/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.ogc;
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

public class EsriProcessor {
    private static final String URL =
        "?num=500&start=1&sortField=title&sortOrder=desc&q=group:%22{id}%22&f=json";

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

    private static JSONTokener getTokenizer(String id) throws Exception {
        String url  = URL.replace("{id}", id);
        String json = IOUtil.readContents(url.toString(),
                                          EsriProcessor.class);
        JSONTokener tokenizer = new JSONTokener(json);

        return tokenizer;
    }

}
