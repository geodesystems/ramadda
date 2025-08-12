/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.projects.assets;

import org.ramadda.repository.*;
import org.ramadda.repository.search.SearchManager;
import org.ramadda.repository.type.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.WikiUtil;
import org.ramadda.util.NamedBuffer;
import org.ramadda.util.Utils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.seesv.Seesv;

import org.w3c.dom.*;
import org.json.*;

import java.util.LinkedHashMap;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


public class AssetCollectionTypeHandler extends AssetBaseTypeHandler   {
    public AssetCollectionTypeHandler(Repository repository, Element node)
	throws Exception {
        super(repository, node);
    }


}
