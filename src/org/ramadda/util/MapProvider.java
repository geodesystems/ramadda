/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import java.util.Hashtable;

import java.util.List;


/**
 */

public interface MapProvider {

    /**
     * _more_
     *
     * @param sb _more_
     * @param width _more_
     * @param height _more_
     * @param pts _more_
     * @param props _more_
     */
    public void makeMap(StringBuilder sb, String width, String height,
                        List<double[]> pts, Hashtable<String, String> props);
}
