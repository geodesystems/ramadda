/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.worldbank;


import org.json.*;


import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;


import org.ramadda.repository.RepositoryUtil;


import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;
import org.ramadda.util.text.*;

import org.w3c.dom.*;

import org.w3c.dom.Element;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;

import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import java.util.Properties;
import java.util.TimeZone;


/**
 */
public class WorldBankFile extends CsvFile {

    /** _more_ */
    private byte[] bytes;

    /**
     * ctor
     *
     * @param filename _more_
     *
     * @throws IOException _more_
     */
    public WorldBankFile(String filename) throws IOException {
        super(filename);
    }



    /**
     * Gets called when first reading the file. Parses the header
     *
     * @param visitInfo visit info
     *
     * @return the visit info
     *
     *
     * @throws Exception _more_
     */
    @Override
    public VisitInfo prepareToVisit(VisitInfo visitInfo) throws Exception {
        //http://api.worldbank.org/countries/ARE/indicators/2.1.6_SHARE.SOLAR?per_page=10&date=1960:2015

        if (bytes == null) {
            ByteArrayOutputStream bos  = new ByteArrayOutputStream();
            BufferedOutputStream  bbos = new BufferedOutputStream(bos);
            String json = new String(
                              IOUtil.readBytes(
                                  visitInfo.getRecordIO().getInputStream()));

            PrintWriter   writer     = new PrintWriter(bbos);
            JSONArray     obj        = new JSONArray(new JSONTokener(json));
            JSONArray     headerJson = obj.getJSONArray(0);
            StringBuilder header     = new StringBuilder("#fields=");
            for (int i = 0; i < headerJsonUtil.length(); i++) {
                String value = headerJsonUtil.getString(i);
            }
            for (int i = 1; i < obj.length(); i++) {
                JSONArray row = obj.getJSONArray(i);
            }
            writer.flush();
            writer.close();
            bytes = bos.toByteArray();
        }


        RecordIO recordIO = new RecordIO(new ByteArrayInputStream(bytes));
        visitInfo.setRecordIO(recordIO);
        putProperty(PROP_HEADER_STANDARD, "true");
        //        putProperty("picky", "false");
        super.prepareToVisit(visitInfo);

        return visitInfo;

    }

    /**
     * _more_
     */
    @Override
    public void doQuickVisit() {
        //noop

    }


    /**
     * _more_
     *
     * @param record _more_
     * @param field _more_
     * @param tok _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public double parseValue(TextRecord record, RecordField field, String tok)
            throws Exception {
        //        tok = tok.replaceAll(",", "");
        //TODO: what does D mean?
        //        if(tok.equals("(D)")|| tok.equals("(X)") || tok.equals("(NA)")) return Double.NaN;

        return Double.parseDouble(tok);
    }



    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        //        PointFile.test(args, WorldBankFile.class);

        String    json = IOUtil.readContents(args[0], WorldBankFile.class);
        JSONArray obj  = new JSONArray(new JSONTokener(json));
        json = null;
        JSONArray data = obj.getJSONArray(1);
        for (int i = 0; i < data.length(); i++) {
            JSONObject indicator = data.getJSONObject(i);
            String     id        = indicator.getString("id");
            String     name      = indicator.getString("name");
            String     note      = indicator.getString("sourceNote");
            System.out.println(id + ": " + name);
        }




    }


}
