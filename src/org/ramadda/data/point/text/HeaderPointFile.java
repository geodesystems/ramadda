/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.point.text;


import org.ramadda.data.point.*;
import org.ramadda.data.record.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Station;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;



import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 */
@SuppressWarnings("unchecked")
public class HeaderPointFile extends CsvFile {


    /**
     * ctor
     *
     *
     *
     * @throws IOException On badness
     */
    public HeaderPointFile(IO.Path path) throws IOException {
        super(path);
    }


    /**
     * _more_
     *
     * @param visitInfo _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public VisitInfo prepareToVisit(VisitInfo visitInfo) throws Exception {

        String propertiesFile = "/"
                                + getClass().getName().replaceAll("\\.", "/")
                                + ".properties";
        Hashtable<String, String> props =
            Utils.getProperties(IOUtil.readContents(propertiesFile, ""));
        putProperty(PROP_SKIPLINES,
                    Misc.getProperty(props, PROP_SKIPLINES, "1"));
        super.prepareToVisit(visitInfo);

        List<String> headerLines = getHeaderLines();
        List<String> fields      = new ArrayList<String>();
        for (String tok : StringUtil.split(headerLines.get(0), ",")) {
            String fieldId = tok.toLowerCase().replace(" ", "_");
            String label = Misc.getProperty(props, fieldId + ".label",
                                            (String) null);
            if (label == null) {
                label = StringUtil.camelCase(tok);
            }

            String type = Misc.getProperty(props, fieldId + ".type",
                                           "double");
            if (type.equals(RecordField.TYPE_DATE)) {
                fields.add(makeField(fieldId,
                                     attrType(RecordField.TYPE_DATE),
                                     attrFormat(Misc.getProperty(props,
                                         fieldId + ".format",
                                         "yyyyMMdd")), attrLabel(label)));
            } else if (type.equals(RecordField.TYPE_STRING)) {
                fields.add(makeField(fieldId,
                                     attrType(RecordField.TYPE_STRING),
                                     attrLabel(label)));
            } else {
                boolean chartable = Misc.getProperty(props,
                                        fieldId + ".chartable", true);
                boolean searchable = Misc.getProperty(props,
                                         fieldId + ".searchable", true);
                String missing = Misc.getProperty(props,
                                     fieldId + ".missing", (String) null);


                fields.add(makeField(fieldId, (missing != null)
                        ? attrMissing(missing)
                        : "", attrLabel(label),
                              HtmlUtils.attr(
                                  ATTR_SCALE,
                                  Misc.getProperty(
                                      props, fieldId + ".scale",
                                      "1.0")), HtmlUtils.attr(
                                          ATTR_OFFSET,
                                          Misc.getProperty(
                                              props, fieldId + ".offset",
                                                  "0.0")), chartable
                        ? attrChartable()
                        : "", attrSearchable(),
                              attrUnit(Misc.getProperty(props,
                                  fieldId + ".unit", ""))));
            }
        }

        putProperty(PROP_FIELDS, makeFields(fields));

        return visitInfo;
    }

    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        PointFile.test(args, HeaderPointFile.class);
    }

}
