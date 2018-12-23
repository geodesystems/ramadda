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

package org.ramadda.plugins.fda;


import org.json.*;

import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;
import org.ramadda.repository.Entry;


import org.ramadda.repository.RepositoryUtil;
import org.ramadda.util.Json;
import org.ramadda.util.Utils;

import org.w3c.dom.Element;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;


import java.util.Date;
import java.util.List;

import java.util.TimeZone;


/**
 */
public class FdaFile extends CsvFile {

    /** _more_ */
    private byte[] bytes;



    //    https://api.fda.gov/drug/label.json?search=effective_time:[20090601+TO+20140731]&count=effective_time


    /**
     * ctor
     *
     * @param filename _more_
     *
     * @throws IOException _more_
     */
    public FdaFile(String filename) throws IOException {
        super(filename);
    }


    /**
     * _more_
     *
     * @param buffered _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    @Override
    public InputStream doMakeInputStream(boolean buffered)
            throws IOException {

        try {
            if (bytes == null) {
                System.err.println("FDA: " + getFilename());
                InputStream    source  = super.doMakeInputStream(buffered);
                String         json    = new String(IOUtil.readBytes(source));
                JSONObject     obj     =
                    new JSONObject(new JSONTokener(json));
                JSONArray      results = obj.getJSONArray("results");
                List<String[]> values  = new ArrayList<String[]>();
                boolean
                    hasTime            = true,
                    hasTerm            = false;
                StringBuilder buffer   = new StringBuilder();
                for (int i = 0; i < results.length(); i++) {
                    JSONObject result = results.getJSONObject(i);
                    if (i == 0) {
                        hasTime = result.has("time");
                        hasTerm = result.has("term");
                        if (hasTime) {
                            buffer.append(
                                "#fields=date[type=date format=yyyyMMdd label=Date],count[type=numeric label=Count chartable=true]\n");
                        } else if (hasTerm) {
                            buffer.append(
                                "#fields=term[type=string label=Term],count[type=numeric label=Count chartable=true]\n");
                        } else {
                            //??
                        }
                    }
                    String  count      = result.get("count").toString();
                    String  timeOrTerm = hasTime
                                         ? result.get("time").toString()
                                         : result.get("term").toString();
                    boolean quoteIt    = timeOrTerm.indexOf(",") >= 0;
                    if (quoteIt) {
                        buffer.append("\"");
                    }
                    buffer.append(timeOrTerm);
                    if (quoteIt) {
                        buffer.append("\"");
                    }
                    buffer.append(",");
                    buffer.append(count);
                    buffer.append("\n");
                }
                bytes = buffer.toString().getBytes();
            }

            return new ByteArrayInputStream(bytes);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
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
        PointFile.test(args, FdaFile.class);
    }

}
