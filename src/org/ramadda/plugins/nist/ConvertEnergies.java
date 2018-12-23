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

package org.ramadda.plugins.nist;


import org.json.*;

import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;



import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 */
public class ConvertEnergies {

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        String     json = IOUtil.readContents(args[0]);
        JSONObject view = new JSONObject(new JSONTokener(json));
        JSONArray  cols = view.getJSONArray("ionization energies data");
        System.out.println("<entries>");
        for (int i = 0; i < cols.length(); i++) {
            JSONObject col = cols.getJSONObject(i);
            if ( !col.has("Element Name")) {
                continue;
            }
            String name = col.get("Element Name").toString();
            name = name + " Ionization Energy Data";

            String    number = col.get("Atomic Number").toString();
            String    shells = col.get("Ground Shells").toString();
            String    level  = col.get("Ground Level").toString();
            String    energy = col.get("Ionization Energy (eV)").toString();
            JSONArray refs   = col.getJSONArray("References");
            JSONArray urls   = col.getJSONArray("ReferencesURL");
            System.out.println("<entry type=\"type_nist_energy\" name=\""
                               + name + "\" " + " shells=\"" + shells + "\" "
                               + " level=\"" + level + "\" " + " energy=\""
                               + energy + "\" " + ">");
            for (int j = 0; j < refs.length(); j++) {
                String ref = refs.get(j).toString();
                String url = urls.get(j).toString();
                url = url.replace("<", "");
                url = url.replace(">", "");
                System.out.println("<metadata type=\"metadata_reference\" "
                                   + XmlUtil.attr("attr1", url)
                                   + XmlUtil.attr("attr2", ref) + "/>");
            }
            System.out.println("</entry>");
        }
        System.out.println("</entries>");
    }



}
