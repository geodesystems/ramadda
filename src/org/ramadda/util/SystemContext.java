/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/
// Copyright (c) 2008-2026 Geode Systems LLC
// SPDX-License-Identifier: Apache-2.0

package org.ramadda.util;

public interface SystemContext {
    public void putSystemContextCache(String key, String value);
    public String getSystemContextCache(String key, long ttl);
    public String getSystemContextProperty(String key, String dflt);
}
