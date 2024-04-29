/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/
// Copyright (c) 2008-2023 Geode Systems LLC
// SPDX-License-Identifier: Apache-2.0


package org.ramadda.util;


import java.util.HashSet;


import java.util.Hashtable;


/**
 */
public interface WikiPageHandler extends SystemContext {

    /**
     * _more_
     *
     * @param path _more_
     *
     * @return _more_
     */
    public String getHtdocsUrl(String path);

    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param name _more_
     * @param label _more_
     *
     * @return _more_
     */
    public String getWikiLink(WikiUtil wikiUtil, String name, String label);


    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param image _more_
     * @param props _more_
     *
     * @return _more_
     */
    public String getWikiImageUrl(WikiUtil wikiUtil, String image,
                                  Hashtable props);

    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param property _more_
     * @param tag _more_
     * @param remainder _more_
     * @param notTags _more_
     *
     * @return _more_
     */
    public String getWikiPropertyValue(WikiUtil wikiUtil, String property,
                                       String tag, String remainder,
                                       HashSet notTags);

    /**
     *
     * @param wikiUtil _more_
      * @return _more_
     */
    public boolean titleOk(WikiUtil wikiUtil);


    public boolean ifBlockOk(WikiUtil wikiUtil, String attrs);

}
