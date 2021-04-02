/*
* Copyright (c) 2008-2021 Geode Systems LLC
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

package org.ramadda.util.text;


import org.ramadda.util.Utils;
import org.ramadda.util.geo.*;

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
    public ShapefileProvider(Hashtable props) {
        addPoints = Utils.getProperty(props, "addPoints", true);
        addShapes = Utils.getProperty(props, "addShapes", false);

    }


    /**
     * _more_
     *
     * @param info _more_
     * @param s _more_
     *
     * @throws Exception _more_
     */
    public void tokenize(TextReader info, String s) throws Exception {}


    /**
     * _more_
     *
     *
     * @param csvUtil _more_
     * @param textReader _more_
     *
     * @throws Exception _more_
     */
    public void initialize(CsvUtil csvUtil, TextReader textReader)
            throws Exception {
        List<String> files = csvUtil.getInputFiles();
        if (files.size() == 0) {
            return;
        }
        String path = files.get(0);
        FeatureCollection fc = FeatureCollection.getFeatureCollection(path,
                                   textReader.getInputStream());

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
