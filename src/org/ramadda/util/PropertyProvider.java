/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;

public interface PropertyProvider {
    public String getProperty(String name, String dflt);
}
