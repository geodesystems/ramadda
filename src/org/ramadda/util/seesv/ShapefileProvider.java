/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.seesv;

import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.geo.*;

import java.io.*;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

/**
 *
 * @author Jeff McWhirter
 */

public class ShapefileProvider extends DataProvider.BulkDataProvider {

    /** _more_ */
    private boolean addPoints = true;

    /** _more_ */
    private boolean addShapes = false;

    /**
     * _more_
     *
     * @param props _more_
     */
    public ShapefileProvider(Dictionary props) {
        addPoints = Utils.getProperty(props, "addPoints", true);
        addShapes = Utils.getProperty(props, "addShapes", false);

    }

    /**
     * _more_
     *
     * @param ctx _more_
     * @param s _more_
     *
     * @throws Exception _more_
     */
    public void tokenize(TextReader ctx, String s) throws Exception {}

    /**
     * _more_
     *
     *
     * @param seesv _more_
     * @param textReader _more_
     *
     * @throws Exception _more_
     */
    public void initialize(Seesv seesv, TextReader textReader)
            throws Exception {
        List<IO.Path> files = seesv.getInputFiles();
        if (files.size() == 0) {
            return;
        }
        IO.Path      path = files.get(0);
        InputStream is   = textReader.getInputStream();
        if (is == null) {
            is = IO.doMakeInputStream(path,true);
        }
        FeatureCollection fc = FeatureCollection.getFeatureCollection(path.getPath(),  is);

        List<DbaseDataWrapper> datum    = fc.getDatum();
        List<Feature>          features = (List<Feature>) fc.getFeatures();
        StringBuilder          sb       = new StringBuilder();
        int                    colCnt   = 0;
        Row                    header   = new Row();
        addRow(header);
        if (datum != null) {
            for (DbaseDataWrapper dbd : datum) {
                header.add(dbd.getName());
            }
        }
        if (addPoints) {
            header.add("latitude");
            header.add("longitude");
        }

        if (addShapes) {
            header.add("shapeType");
            header.add("shape");
        }

        int cnt = 0;
        for (int i = 0; i < features.size(); i++) {
            Row row = new Row();
            addRow(row);
            Feature feature = features.get(i);
            cnt++;
            colCnt = 0;
            if (datum != null) {
                for (DbaseDataWrapper dbd : datum) {
                    row.add("" + dbd.getData(i));
                }
            }

            if (addPoints) {
                float[] center = feature.getGeometry().getCenter();
                row.add(center[0]);
                row.add(center[1]);
            }

            if (addShapes) {
                row.add(feature.getGeometry().getGeometryType());
                String shape = feature.getGeometry().getCoordsString();
                shape = shape.replaceAll("\n", "").replaceAll(" ", "");
                row.add(shape);
            }
        }
    }

}
