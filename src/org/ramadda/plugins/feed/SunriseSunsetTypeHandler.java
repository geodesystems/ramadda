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

package org.ramadda.plugins.feed;


import org.json.*;



import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.AtomUtil;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.RssUtil;
import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.net.URL;

import java.text.SimpleDateFormat;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import java.util.TimeZone;



/**
 */
public class SunriseSunsetTypeHandler extends GenericTypeHandler {

    /** _more_ */
    private TTLCache<String, Appendable> cache = new TTLCache<String,
                                                     Appendable>(60 * 60
                                                         * 1000);




    /** _more_ */
    private static final String URL =
        "https://api.sunrise-sunset.org/json?lat=${lat}&lng=${lon}&formatted=0";


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public SunriseSunsetTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {


        if (tag.equals("sunrisesunset")) {

            if ( !entry.hasLocationDefined()) {
                return "No location defined";
            }
            String     key = entry.getLatitude() + "-" + entry.getLongitude();
            Appendable sb       = cache.get(key);
            TimeZone   timeZone = getTimeZone(request, entry, 0);
            if (sb == null) {
                String url =
                    URL.replace("${lat}",
                                "" + entry.getLatitude()).replace("${lon}",
                                    "" + entry.getLongitude());
                String     json = IOUtil.readContents(url, this.getClass());
                JSONObject jsonObject = new JSONObject(new JSONTokener(json));
                if ( !Misc.equals(jsonObject.optString("status", ""), "OK")) {
                    return "Failed to read data: status="
                           + jsonObject.optString("status", "");
                }
                sb = new StringBuilder();
                cache.put(key, sb);
                sb.append(HtmlUtils.cssBlock(""));
                JSONObject       results =
                    jsonObject.getJSONObject("results");
                SimpleDateFormat dateFormat = new SimpleDateFormat();

                dateFormat.setTimeZone(timeZone);
                dateFormat.applyPattern("h:mm a z");


                Date sunrise = Utils.parseDate(results.optString("sunrise",
                                   "NA"));
                Date sunset = Utils.parseDate(results.optString("sunset",
                                  "NA"));
                int length  = results.optInt("day_length", 0);
                int hours   = length / 3600;
                int minutes = (length - hours * 3600) / 60;
                int seconds = (length - hours * 3600 - minutes * 60);
                sb.append(HtmlUtils.formTable());
                sb.append(HtmlUtils.formEntry("Current Time:", "${now}"));
                sb.append(HtmlUtils.formEntry("Sunrise/Sunset:",
                        dateFormat.format(sunrise) + " - "
                        + dateFormat.format(sunset)));
                String kudos = "(From "
                               + HtmlUtils.href("https://sunrise-sunset.org",
                                   "Sunrise-Sunset.org") + ")";
                sb.append(HtmlUtils.formEntry("Day Length:",
                        hours + ":" + minutes + ":" + seconds + "  "
                        + kudos));
                sb.append(HtmlUtils.formTableClose());

            }

            SimpleDateFormat dateFormat2 = new SimpleDateFormat();
            dateFormat2.setTimeZone(timeZone);
            dateFormat2.applyPattern("MMM d, yyyy h:mm a z");

            return sb.toString().replace("${now}",
                                         dateFormat2.format(new Date()));
        } else {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }
    }



    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        String url =
            "https://forecast.weather.gov/MapClick.php?lat=40.0157&lon=-105.2792&unit=0&lg=english&FcstType=dwml";
        String xml = Utils.readUrl(url);
    }

}
