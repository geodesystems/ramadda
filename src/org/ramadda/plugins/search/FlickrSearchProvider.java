/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.search;


import org.json.*;

import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.AtomUtil;


import org.ramadda.util.HtmlUtils;
import org.ramadda.util.SelectionRectangle;
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
 * Proxy that searches google
 *
 */
public class FlickrSearchProvider extends SearchProvider {

    /** _more_ */
    private static final String ID = "flickr";

    /** _more_ */
    private static final String xURL =
        "https://api.flickr.com/services/rest/?method=flickr.photos.search";

    /**  */
    private static final String URL =
        "https://www.flickr.com/services/rest/?method=flickr.photos.search&format=rest";

    /** _more_ */
    private static final String ARG_API_KEY = "api_key";

    /** _more_ */
    private static final String ARG_TEXT = "text";

    /** _more_ */
    private static final String ARG_MIN_TAKEN_DATE = "min_taken_date";

    /** _more_ */
    private static final String ARG_MAX_TAKEN_DATE = "max_taken_date";

    /** _more_ */
    private static final String ARG_TAGS = "tags";

    /** _more_ */
    private static final String ARG_BBOX = "bbox";



    /**
     * _more_
     *
     * @param repository _more_
     */
    public FlickrSearchProvider(Repository repository) {
        super(repository, ID, "Flickr Image Search");
    }



    /**
     *  @return _more_
     */
    @Override
    public String getCapabilities() {
        return CAPABILITY_AREA;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSiteUrl() {
        return "http://www.flickr.com/";
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSearchProviderIconUrl() {
        return "${root}/search/flickr.png";
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isEnabled() {
        return getApiKey() != null;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param searchInfo _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public List<Entry> getEntries(Request request, org.ramadda.repository.util.SelectInfo searchInfo)
            throws Exception {

        List<Entry> entries = new ArrayList<Entry>();
        //https://www.flickr.com/services/rest/?method=flickr.photos.search&api_key=c83a63910f7fd1cdaee7f507d41eb78a&text=colorado&per_page=20&page=2&format=rest

        String url = URL;
        url = HtmlUtils.url(url, ARG_API_KEY, getApiKey(), ARG_TEXT,
                            request.getString(ARG_TEXT, ""));
        int max  = request.get(ARG_MAX, 100);
        int skip = request.get(ARG_SKIP, 0);
        int page = 1;
        if (skip > 0) {
            page = skip / max;
        }
        url += "&"
               + HtmlUtils.arg("extras", "description,date_taken,geo,tags");
        url += "&" + HtmlUtils.arg("per_page", "" + max);
        url += "&" + HtmlUtils.arg("page", "" + page);
        SelectionRectangle rect = request.getSelectionBounds();
        if (rect.anyDefined()) {
            String bbox = rect.getWest(-180) + "," + rect.getSouth(-90) + ","
                          + rect.getEast(180) + "," + rect.getNorth(90);
            url += "&" + HtmlUtils.arg("bbox", bbox);
        }
        System.err.println(getName() + " url:" + url);

        URLConnection connection = new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", "ramadda");
        InputStream is  = connection.getInputStream();
        String      xml = IOUtil.readContents(is);
        //        System.out.println("xml:" + xml);

        Element root   = XmlUtil.getRoot(xml);
        Element photos = XmlUtil.findChild(root, "photos");
        if (photos == null) {
            return entries;
        }

        Entry       parent      = getSynthTopLevelEntry();
        TypeHandler typeHandler =
            getRepository().getTypeHandler("type_image");
        NodeList children = XmlUtil.getElements(photos, "photo");
        for (int childIdx = 0; childIdx < children.getLength(); childIdx++) {
            Element item   = (Element) children.item(childIdx);
            String  name   = XmlUtil.getAttribute(item, "title", "");
            String  id     = XmlUtil.getAttribute(item, "id", "");
            String  owner  = XmlUtil.getAttribute(item, "owner", "");
            String  server = XmlUtil.getAttribute(item, "server", "");
            String  farm   = XmlUtil.getAttribute(item, "farm", "");
            String  secret = XmlUtil.getAttribute(item, "secret", "");
            Date    dttm   = null;
            String date = XmlUtil.getAttribute(item, "datetaken",
                              (String) null);
            if (date != null) {
                dttm = Utils.parseDate(date);
            }
            if (dttm == null) {
                dttm = new Date();
            }
            double latitude = XmlUtil.getAttribute(item, "latitude",
                                  Double.NaN);
            double longitude = XmlUtil.getAttribute(item, "longitude",
                                   Double.NaN);

            Date fromDate = dttm,
                 toDate   = dttm;
            String urlTemplate =
                "https://farm${farm}.staticflickr.com/${server}/${id}_${secret}_${size}.jpg";
            String imageUrl = urlTemplate.replace("${farm}",
                                  farm).replace("${server}",
                                      server).replace("${id}",
                                          id).replace("${secret}", secret);

            String itemUrl = imageUrl.replace("${size}", "b");

            String pageUrl = "https://www.flickr.com/photos/" + owner + "/"
                             + id;

            String desc = "Photo courtesy of "
                          + HtmlUtils.href(pageUrl, "Flickr");
            String desc2 = XmlUtil.getGrandChildText(item, "description", "");
            if (desc2.length() > 0) {
                desc = desc + "<br>" + desc2;
            }

            Entry newEntry = new Entry(Repository.ID_PREFIX_SYNTH + getId()
                                       + TypeHandler.ID_DELIMITER
                                       + id, typeHandler);
            if ( !Double.isNaN(latitude) && (latitude != 0)
                    && (longitude != 0)) {
                newEntry.setLocation(latitude, longitude);
            }
            newEntry.setIcon("/search/flickr.png");
            entries.add(newEntry);

            Metadata thumbnailMetadata =
                new Metadata(getRepository().getGUID(), newEntry.getId(),
                             getMetadataManager().findType(ContentMetadataHandler.TYPE_THUMBNAIL), false,
                             imageUrl.replace("${size}", "t"), null, null,
                             null, null);
            getMetadataManager().addMetadata(request,newEntry, thumbnailMetadata);

            newEntry.initEntry(name, makeSnippet(desc), parent,
                               getUserManager().getLocalFileUser(),
                               new Resource(new URL(itemUrl)), "",
                               Entry.DEFAULT_ORDER, dttm.getTime(),
                               dttm.getTime(), fromDate.getTime(),
                               toDate.getTime(), null);
            getEntryManager().cacheSynthEntry(newEntry);
        }

        return entries;
    }



}
