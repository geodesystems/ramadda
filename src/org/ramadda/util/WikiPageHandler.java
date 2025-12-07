/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/
// Copyright (c) 2008-2025 Geode Systems LLC
// SPDX-License-Identifier: Apache-2.0

package org.ramadda.util;

import java.util.HashSet;

import java.util.Hashtable;


public interface WikiPageHandler extends SystemContext {

    public String getHtdocsUrl(String path);

    public String getWikiLink(WikiUtil wikiUtil, String name, String label);

    public String getWikiImageUrl(WikiUtil wikiUtil, String image,
                                  Hashtable props);

    public String getWikiPropertyValue(WikiUtil wikiUtil, String property,
                                       String tag, String remainder,
                                       HashSet notTags);

    
    public boolean titleOk(WikiUtil wikiUtil);

    public boolean ifBlockOk(WikiUtil wikiUtil, String attrs);

}
