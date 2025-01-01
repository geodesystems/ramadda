/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.biz;


import org.json.*;

import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;



import org.ramadda.repository.*;
import org.ramadda.repository.database.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.FormInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;

import org.ramadda.util.sql.*;
import org.ramadda.util.seesv.Row;
import org.ramadda.util.seesv.TextReader;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import java.io.*;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URL;
import java.net.URLConnection;

import java.sql.*;

import java.util.ArrayList;

import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.*;


/**
 *
 *
 */
public class EnigmaTable extends CsvFile {


    /** _more_ */
    private byte[] bytes;



    /**
     * _more_
     *
     * @param filename _more_
     *
     * @throws IOException _more_
     */
    public EnigmaTable(IO.Path path) throws IOException {
        super(path);
    }



    /**
     * _more_
     *
     * @param buffered _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public InputStream doMakeInputStream(boolean buffered) throws Exception {
        try {
            if (bytes == null) {
                StringBuilder     sb      = new StringBuilder();
                InputStream       source  = super.doMakeInputStream(buffered);
                String            json    = IOUtil.readContents(source);
                //                System.err.println("json:" + json);


                List<RecordField> fields  = getFields();
                JSONObject        obj = new JSONObject(new JSONTokener(json));
                JSONArray         results = JsonUtil.readArray(obj, "result");
                if (results != null) {
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject col = results.getJSONObject(i);
                        for (int colIdx = 0; colIdx < fields.size();
                                colIdx++) {
                            RecordField field = fields.get(colIdx);
                            String      v;
                            if (field.isTypeNumeric()) {
                                v = "" + col.optDouble(field.getName(),
                                        Double.NaN);
                            } else {
                                v = col.optString(field.getName(), "");
                            }
                            if (colIdx > 0) {
                                sb.append(",");
                            }
                            if (v.indexOf(",") >= 0) {
                                sb.append("\"");
                                sb.append(v);
                                sb.append("\"");
                            } else {
                                sb.append(v);
                            }
                        }
                        sb.append("\n");
                    }
                }
                //                System.err.println (sb);
                bytes = sb.toString().getBytes();
            }
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

            return bais;
        } catch (Exception exc) {
            exc.printStackTrace();

            throw new RuntimeException(exc);
        }
    }






}
