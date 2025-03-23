/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/
// Copyright (c) 2008-2025 Geode Systems LLC
// SPDX-License-Identifier: Apache-2.0


package org.ramadda.util;


/**
 * Interface description
 *
 *
 * @author         Enter your name here...    
 */
public interface SystemContext {

    /**
     *
     * @param key _more_
     * @param value _more_
     */
    public void putSystemContextCache(String key, String value);

    /**
     *
     * @param key _more_
     * @param ttl _more_
      * @return _more_
     */
    public String getSystemContextCache(String key, long ttl);

    /**
     *
     * @param key _more_
     * @param dflt _more_
      * @return _more_
     */
    public String getSystemContextProperty(String key, String dflt);
}
