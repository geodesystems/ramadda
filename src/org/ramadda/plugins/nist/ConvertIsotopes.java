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

import java.util.Properties;


/**
 */
public class ConvertIsotopes {

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.load(
            IOUtil.getInputStream(
                "/org/ramadda/plugins/nist/elements.properties",
                ConvertIsotopes.class));
        String     json = IOUtil.readContents(args[0]);
        JSONObject top  = new JSONObject(new JSONTokener(json));
        JSONArray  cols = top.getJSONArray("data");
        System.out.println("<entries>");
        for (int i = 0; i < cols.length(); i++) {
            if (i > 5) {
                //                break;
            }
            JSONObject elt      = cols.getJSONObject(i);
            JSONArray  isotopes = elt.getJSONArray("isotopes");
            String     symbol   = elt.get("Atomic Symbol").toString();
            String     number   = elt.get("Atomic Number").toString();
            String     notes    = elt.has("Notes")
                                  ? elt.get("Notes").toString()
                                  : "";
            String     weight   = elt.has("Standard Atomic Weight")
                                  ? elt.get(
                                      "Standard Atomic Weight").toString()
                                  : "";
            for (int j = 0; j < isotopes.length(); j++) {
                JSONObject isotope = isotopes.getJSONObject(j);
                String     symbol2 = isotope.get("Atomic Symbol").toString();
                String     mass    = isotope.has("Mass Number")
                                     ? isotope.get("Mass Number").toString()
                                     : "";
                String     comp    = isotope.has("Isotopic Composition")
                                     ? isotope.get(
                                         "Isotopic Composition").toString()
                                     : "";
                String relmass = isotope.has("Relative Atomic Mass")
                                 ? isotope.get(
                                     "Relative Atomic Mass").toString()
                                 : "";
                String name = (String) props.get(symbol);
                if (name == null) {
                    name = symbol;
                }
                name = name + " " + symbol2 + "-" + mass;
                System.out.println(XmlUtil.tag("entry",
                        XmlUtil.attrs(new String[] {
                    "type", "type_nist_isotope", "name", name,
                    "atomic_symbol", symbol2, "atomic_number", number,
                    "standard_atomic_weight", weight, "mass_number", mass,
                    "isotopic_composition", comp, "relative_atomic_mass",
                    relmass, "notes", notes
                })));
            }
        }
        System.out.println("</entries>");
    }



}
