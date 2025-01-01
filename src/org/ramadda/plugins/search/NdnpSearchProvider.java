/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.search;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import org.w3c.dom.*;


import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 *
 */
public class NdnpSearchProvider extends OpenSearchProvider {

    /** _more_ */
    public static final String ID = "ndnp";

    /** _more_ */
    public static final String URL =
        "https://chroniclingamerica.loc.gov/search/pages/results/?andtext=${searchterms}&format=atom";


    /**
     * _more_
     *
     * @param repository _more_
     */
    public NdnpSearchProvider(Repository repository) {
        super(repository, ID, URL, "NDNP Chronicling America",
              "/search/neh.png");
    }

    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getCategory() {
        return CATEGORY_SCIENCE;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSearchProviderIconUrl() {
        return "${root}/favicon.png";
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param newEntry _more_
     * @param item _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initOpenSearchEntry(Request request, Entry newEntry,
                                    Element item)
            throws Exception {
        super.initOpenSearchEntry(request, newEntry, item);
        String url = newEntry.getResource().getPath();
        String img = url + "/thumbnail.jpg";
        Metadata thumbnailMetadata =
            new Metadata(getRepository().getGUID(), newEntry.getId(),
                         getMetadataManager().findType(ContentMetadataHandler.TYPE_THUMBNAIL), false, img,
                         null, null, null, null);
        getMetadataManager().addMetadata(request,newEntry, thumbnailMetadata);
    }



}
