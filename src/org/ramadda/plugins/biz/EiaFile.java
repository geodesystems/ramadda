/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.biz;

import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;
import org.ramadda.util.IO;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;
import org.ramadda.repository.Entry;
import org.ramadda.repository.RepositoryUtil;
import org.w3c.dom.Element;
import ucar.unidata.xml.XmlUtil;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;


import org.json.*;

/**
 */
public class EiaFile extends CsvFile {

    /** _more_ */
    private StringBuilder buffer;


    /**
     * ctor
     *
     * @param filename _more_
     *
     * @throws IOException _more_
     */
    public EiaFile(IO.Path path) throws IOException {
        super(path);
    }


    /**
     * _more_
     *
     * @param buffered _more_
     *
     * @return _more_
     * @throws Exception _more_
     */
    @Override
    public InputStream doMakeInputStream(boolean buffered) throws Exception {
        try {
            if (buffer == null) {
                //                System.err.println("Reading EIA time series");
                buffer = new StringBuilder();
                Entry entry = (Entry) getProperty("entry");

                InputStream source = super.doMakeInputStream(buffered);
		String json = new String(IO.readBytes(source,10_000_000));
		IO.close(source);
		JSONObject obj = new JSONObject(json);

		JSONObject response = obj.getJSONObject("response");
		JSONObject request = obj.getJSONObject("request");
		JSONObject params = request.getJSONObject("params");				
		String valueField=params.getJSONArray("data").getString(0);
		JSONArray data = response.getJSONArray("data");

		for(int i=0;i<data.length();i++) {
		    JSONObject datum = data.getJSONObject(i);
		    String dttm=null;
		    try {
			dttm = ""+datum.getString("period");
		    } catch(Exception exc) {
			dttm = ""+datum.getInt("period");
		    }
		    if (dttm.indexOf("q") >= 0) {
                        dttm = dttm.replace("q1", "01").replace("q2",
								"04").replace("q3",
									      "07").replace("q4", "10");
                    }

		    if(i==0) {
			String format ="";
                        if (dttm.length() == 4) {
                            format = "yyyy";
                        } else if (dttm.length() == 6) {
                            format = "yyyyMM";
                        } else if (dttm.length() == 7) {
                            format = "yyyy-MM";			    
                        } else if (dttm.length() == 8) {
                            format = "yyyyMMdd";
			}
			if (entry != null) {
			    entry.setName(datum.optString("seriesDescription"));
			    entry.setDescription(response.getString("description"));
			}
			String unit = datum.optString("unit","");
			putFields(new String[] {
				makeField(FIELD_DATE, attrType("date"),
					  attrFormat(format)),
				makeField("value", attrUnit(unit), attrLabel("Value"),
					  attrChartable(), attrMissing(-999999.99)), });
		    }


                    double value = datum.optDouble(valueField, Double.NaN);
		    //                    if (value.equals("") || value.equals(".")) {
		    //                        value = "-999999.99";
		    //                    }
                    buffer.append(dttm).append(",").append(value).append(
									 "\n");
		}
		/*
                Element     root   = XmlUtil.getRoot(source);
                Element     series = XmlUtil.findChild(root, Eia.TAG_SERIES);
                series = XmlUtil.findChild(series, Eia.TAG_ROW);

                Element data = XmlUtil.findChild(series, Eia.TAG_DATA);
                String name = XmlUtil.getGrandChildText(series, Eia.TAG_NAME,
                                  "").trim();
                String desc = XmlUtil.getGrandChildText(series,
                                  Eia.TAG_DESCRIPTION, "").trim();


                String format = "yyyyMMdd";
                String unit = XmlUtil.getGrandChildText(series,
                                  Eia.TAG_UNITS, "").trim();
                List nodes = XmlUtil.findChildren(data, Eia.TAG_ROW);
                for (int i = 0; i < nodes.size(); i++) {
                    Element node = (Element) nodes.get(i);
                    String dttm = XmlUtil.getGrandChildText(node,
                                      Eia.TAG_DATE, "").trim().toLowerCase();

		}
		*/

	    }

            ByteArrayInputStream bais =
                new ByteArrayInputStream(buffer.toString().getBytes());

            return bais;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
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
        super.prepareToVisit(visitInfo);
        if (getProperty(PROP_FIELDS, (String) null) == null) {
            String format = "yyyy-MM-dd";
            putFields(new String[] {
                makeField(FIELD_DATE, attrType("date"), attrFormat(format)),
                makeField("value", attrLabel("Value"), attrChartable(),
                          attrMissing(-999999.99)), });
        }

        return visitInfo;
    }




    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        PointFile.test(args, EiaFile.class);
    }

}
