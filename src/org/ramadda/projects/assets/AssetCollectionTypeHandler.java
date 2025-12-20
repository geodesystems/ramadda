/**
   Copyright (c) 2008-2026 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.projects.assets;
import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.w3c.dom.*;

public class AssetCollectionTypeHandler extends AssetBaseTypeHandler   {
    public AssetCollectionTypeHandler(Repository repository, Element node)
	throws Exception {
        super(repository, node);
    }
}
