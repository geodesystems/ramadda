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

package org.ramadda.plugins.community;


import org.json.*;

import org.ramadda.repository.*;
import org.ramadda.repository.harvester.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

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
public class CobPermitHarvester extends Harvester {

    /** _more_ */
    private static String URL =
        "https://www-static.bouldercolorado.gov/docs/opendata/DevelopmentReviewClosed.GeoJSON";


    /**
     * _more_
     *
     * @param repository _more_
     * @param id _more_
     *
     * @throws Exception _more_
     */
    public CobPermitHarvester(Repository repository, String id)
            throws Exception {
        super(repository, id);
    }


    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     *
     * @throws Exception _more_
     */
    public CobPermitHarvester(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getDescription() {
        return "City of Boulder Permit Harvester";
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    @Override
    protected void runHarvester() throws Exception {
        Request request = getRequest();
        URL = "cob.json";
        System.err.println("COB Permits:" + URL);
        String     json     = IOUtil.readContents(URL);
        JSONObject top      = new JSONObject(new JSONTokener(json));
        JSONArray  features = top.getJSONArray("features");
        for (int i = 0; i < features.length(); i++) {
            if (i > 20) {
                //                break;
            }

            JSONObject feature    = features.getJSONObject(i);
            JSONObject props      = feature.getJSONObject("properties");

            String     caseNumber = props.getString("CASE_NUMBE");

            request.put(ARG_TYPE, "community_case");
            System.out.println("case:" + caseNumber);
            request.put("search.community_case.case_number", caseNumber);
            List[] array =
                getRepository().getEntryManager().getEntries(request);
            List<Entry> entries =
                new ArrayList<Entry>((List<Entry>) (array[0]));
            entries.addAll((List<Entry>) (array[1]));

            if (entries.size() > 0) {
                System.err.println("got one:" + entries.get(0));

                continue;
            }

            String[] attrs = {
                "case_number", "applicant", "address", "case_type", "contact",
                "email", "phone",
            };


            String[] keys = {
                "CASE_NUMBE", "APPLICANT_", "CASE_ADDRE", "CASE_TYPE",
                "STAFF_CONT", "STAFF_EMAI", "STAFF_PHON"
            };

            for (int keyIdx = 0; keyIdx < keys.length; keyIdx++) {
                if ( !props.has(keys[keyIdx])
                        || props.isNull(keys[keyIdx])) {}
                else {
                    //                    inner.append(XmlUtil.tag(attrs[keyIdx],"",XmlUtil.getCdata(props.getString(keys[keyIdx]))));
                }
            }
            //            inner.append(XmlUtil.tag("city","","Boulder"));
            //            inner.append(XmlUtil.tag("state","","CO"));

            String name = caseNumber + " - " + props.getString("APPLICANT_");
            //                        "type","community_case",
            //                        "name", name}));
            String     desc   = props.optString("CASE_DESCR", "");
            JSONObject geom   = feature.getJSONObject("geometry");
            double[]   coords = getCoords(geom);
        }
        System.err.println("COB Permits DONE:");
    }

    /**
     * _more_
     *
     * @param a _more_
     * @param nwse _more_
     */
    private static void getBounds(JSONArray a, double[] nwse) {
        double  lon   = a.getDouble(0);
        double  lat   = a.getDouble(1);
        boolean first = Double.isNaN(nwse[0]);
        nwse[0] = first
                  ? lat
                  : Math.max(nwse[0], lat);
        nwse[2] = first
                  ? lat
                  : Math.min(nwse[2], lat);
        nwse[1] = first
                  ? lon
                  : Math.min(nwse[1], lon);
        nwse[3] = first
                  ? lon
                  : Math.max(nwse[3], lon);
    }

    /**
     * _more_
     *
     * @param geom _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private static double[] getCoords(JSONObject geom) throws Exception {
        JSONArray coords   = geom.getJSONArray("coordinates");
        String    geomType = geom.optString("type", "");
        boolean   isPoly   = geomType.equals("Polygon");
        double[]  nwse = { Double.NaN, Double.NaN, Double.NaN, Double.NaN };
        for (int i = 0; i < coords.length(); i++) {
            JSONArray a1 = coords.getJSONArray(i);
            for (int j = 0; j < a1.length(); j++) {
                JSONArray a2 = a1.getJSONArray(j);
                if (isPoly) {
                    getBounds(a2, nwse);
                } else {
                    for (int k = 0; k < a2.length(); k++) {
                        JSONArray a3 = a2.getJSONArray(k);
                        getBounds(a3, nwse);
                    }
                }
            }
        }

        return nwse;
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        URL = "cob.json";
        System.err.println("COB Permits:" + URL);
        String json        = IOUtil.readContents(URL);
        JSONObject top = new JSONObject(new JSONTokener(json));
        JSONArray features = top.getJSONArray("features");
        Hashtable<String, List<String>> map = new Hashtable<String,
                                                  List<String>>();
        List<String> addrs = new ArrayList<String>();
        int          max   = 0;
        String       smax  = "";
        for (int i = 0; i < features.length(); i++) {
            if (i > 20) {
                //                break;
            }

            JSONObject feature    = features.getJSONObject(i);
            JSONObject props      = feature.getJSONObject("properties");

            String     caseNumber = props.getString("CASE_NUMBE");
            String     addr       = props.getString("CASE_ADDRE");
            addr = addr.replaceAll("DR$", "").trim();

            List<String> cases = map.get(addr);
            if (cases == null) {
                addrs.add(addr);
                cases = new ArrayList<String>();
                map.put(addr, cases);
            }
            cases.add(caseNumber);
            if (cases.size() > max) {
                max  = cases.size();
                smax = addr;
            }
        }
        for (String addr : addrs) {
            if (map.get(addr).size() > 6) {
                System.out.println(addr + ":" + map.get(addr));
            }
        }

    }



}
