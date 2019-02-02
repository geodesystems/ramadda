/*
* Copyright (c) 2008-2019 Geode Systems LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*     http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.ramadda.repository.search;



import org.ramadda.repository.*;

import org.ramadda.repository.output.XmlOutputHandler;
import org.ramadda.repository.type.*;
import org.ramadda.repository.util.ServerInfo;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.sql.Clause;

import org.w3c.dom.*;

import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 */
public abstract class SearchProvider extends GenericTypeHandler {

    /** _more_ */
    public static final String CATEGORY_SCIENCE = "Science & Academic";

    /** _more_ */
    private String id;

    /** _more_ */
    private List<String> aliases = new ArrayList<String>();


    /** _more_ */
    private String name;

    /** _more_ */
    private String apiKey;


    /**
     * _more_
     *
     * @param repository _more_
     */
    public SearchProvider(Repository repository) {
        super(repository, "", "Search Provider");
    }

    /**
     * _more_
     *
     * @param repository _more_
     * @param id _more_
     */
    public SearchProvider(Repository repository, String id) {
        super(repository, id, "Search Provider");
        initId(id);
    }

    /**
     * _more_
     *
     * @param repository _more_
     * @param id _more_
     * @param name _more_
     */
    public SearchProvider(Repository repository, String id, String name) {
        super(repository, id, name);
        initId(id);
        this.name = ((name == null)
                     ? id
                     : name);
    }



    /**
     * _more_
     *
     * @param s _more_
     */
    private void initId(String s) {
        List<String> toks = StringUtil.split(s, ",");
        this.id = toks.get(0);
        toks.remove(0);
        this.aliases.addAll(toks);
    }

    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    public boolean identifiedBy(String id) {
        if (this.id.equals(id)) {
            return true;
        }
        if (this.aliases.contains(id)) {
            return true;
        }

        return false;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isEnabled() {
        return true;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getApiKey() {
        if (apiKey == null) {
            apiKey = getRepository().getProperty(getId() + ".api.key");
        }

        return apiKey;
    }



    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public TypeHandler getLinkTypeHandler() throws Exception {
        return getRepository().getTypeHandler("link");
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getId() {
        return id;
    }

    /**
     * _more_
     *
     * @param s _more_
     */
    public void setId(String s) {
        id = s;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getFormLabel() {
        String url = getSiteUrl();
        if (url != null) {
            return HtmlUtils.href(url, getName());
        }

        return getName();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getCategory() {
        return "External Search Providers";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getSiteUrl() {
        return null;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getSearchProviderIconUrl() {
        return "${root}/favicon.png";
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getName() {
        if (name == null) {
            return getId();
        }

        return name;
    }

    /**
     * _more_
     *
     * @param name _more_
     */
    public void setName(String name) {
        this.name = name;
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
    public abstract List<Entry> getEntries(Request request,
                                           SearchInfo searchInfo)
     throws Exception;

    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Mar 14, '14
     * @author         Enter your name here...
     */
    public static class RamaddaSearchProvider extends SearchProvider {


        /**
         * _more_
         *
         * @param repository _more_
         * @param id _more_
         * @param name _more_
         */
        public RamaddaSearchProvider(Repository repository, String id,
                                     String name) {
            super(repository, id, name);
        }


        /**
         * _more_
         *
         * @return _more_
         */
        @Override
        public String getCategory() {
            return "";
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
         * @param searchInfo _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        public List<Entry> getEntries(Request request, SearchInfo searchInfo)
                throws Exception {
            StringBuilder sb = new StringBuilder();
            List<Entry>[] repositoryResults =
                getEntryManager().getEntries(request, sb);
            searchInfo.addMessage(this, sb.toString());
            List<Entry> results = new ArrayList<Entry>();
            results.addAll(repositoryResults[0]);
            results.addAll(repositoryResults[1]);

            return results;
        }

    }

    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public URLConnection getConnection(String url) throws Exception {
        URLConnection connection = new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", "ramadda");

        return connection;
    }

    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public InputStream getInputStream(String url) throws Exception {
        return getConnection(url).getInputStream();
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Sun, Jun 28, '15
     * @author         Enter your name here...
     */
    public static class RemoteSearchProvider extends SearchProvider {

        /** _more_ */
        private ServerInfo serverInfo;

        /**
         * _more_
         *
         * @param repository _more_
         * @param serverInfo _more_
         */
        public RemoteSearchProvider(Repository repository,
                                    ServerInfo serverInfo) {
            super(repository, serverInfo.getId(), serverInfo.getLabel());
            this.serverInfo = serverInfo;
        }



        /**
         * _more_
         *
         * @return _more_
         */
        @Override
        public String getCategory() {
            return "Remote Repositories";
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
         * @return _more_
         */
        @Override
        public String getFormLabel() {
            return HtmlUtils.href(serverInfo.getUrl(), getName());
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
        public List<Entry> getEntries(Request request, SearchInfo searchInfo)
                throws Exception {
            final Entry parentEntry =
                new Entry(getRepository().getGroupTypeHandler(), true);
            String serverUrl = serverInfo.getUrl();
            parentEntry.setId(getEntryManager().getRemoteEntryId(serverUrl,
                    ""));
            getEntryManager().cacheEntry(parentEntry);
            parentEntry.setRemoteServer(serverInfo);
            parentEntry.setUser(getUserManager().getAnonymousUser());
            //            parentEntry.setParentEntry(tmpEntry);
            parentEntry.setName(serverUrl);


            List<Entry> entries = new ArrayList<Entry>();
            request = request.cloneMe();
            request.remove(SearchManager.ARG_PROVIDER);
            request.put(ARG_OUTPUT, XmlOutputHandler.OUTPUT_XML);
            String linkUrl = request.getUrlArgs();
            String remoteSearchUrl =
                serverUrl + getSearchManager().URL_ENTRY_SEARCH.getPath()
                + "?" + linkUrl;
            //            System.err.println("Remote URL:" + remoteSearchUrl);
            try {
                String entriesXml = getStorageManager().readSystemResource(
                                        new URL(remoteSearchUrl));
                Element  root     = XmlUtil.getRoot(entriesXml);
                NodeList children = XmlUtil.getElements(root);
                for (int i = 0; i < children.getLength(); i++) {
                    Element node = (Element) children.item(i);
                    //                    if (!node.getTagName().equals(TAG_ENTRY)) {continue;}
                    Entry entry =
                        getEntryManager().createEntryFromXml(request, node,
                            parentEntry, new Hashtable(), false, false);

                    //                            entry.setName("remote:" + entry.getName());
                    entry.setResource(
                        new Resource(
                            "remote:"
                            + XmlUtil.getAttribute(
                                node, ATTR_RESOURCE,
                                ""), Resource.TYPE_REMOTE_FILE));
                    String id = XmlUtil.getAttribute(node, ATTR_ID);
                    entry.setId(getEntryManager().getRemoteEntryId(serverUrl,
                            id));
                    entry.setRemoteServer(serverInfo);
                    entry.setRemoteUrl(serverUrl + "/entry/show?entryid="
                                       + id);
                    getEntryManager().cacheEntry(entry);
                    entries.add((Entry) entry);
                }
            } finally {}

            return entries;
        }

    }


    /**
     * _more_
     *
     * @param ids _more_
     *
     * @return _more_
     */
    public String makeSynthId(String... ids) {
        StringBuilder sb = new StringBuilder();
        sb.append(Repository.ID_PREFIX_SYNTH);
        sb.append(getId());
        for (String id : ids) {
            sb.append(TypeHandler.ID_DELIMITER);
            sb.append(id);
        }

        return sb.toString();
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public int getRequestLimit(Request request) {
        return request.get(ARG_MAX, 50);
    }

}
