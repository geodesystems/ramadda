/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;
import java.util.Hashtable;
import java.util.List;

public interface MapProvider {
    public void makeMap(StringBuilder sb, String width, String height,
                        List<double[]> pts, Hashtable<String, String> props);
}
