/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.search;



import org.ramadda.repository.*;

import org.ramadda.repository.util.SelectInfo;
import org.ramadda.repository.output.XmlOutputHandler;
import org.ramadda.repository.type.*;
import org.ramadda.repository.util.ServerInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
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

    /**  */
    public static final String CAPABILITY_AREA = "area";

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
     *
     * @return _more_
     */
    public String getCapabilities() {
        return "";
    }


    /**
     * _more_
     *
     * @param s _more_
     */
    private void initId(String s) {
        List<String> toks = Utils.split(s, ",");
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

    public String getFormSuffix() {
	return "";
    }


    /**
     * _more_
     *
     *
     * @param includeId _more_
     * @return _more_
     */
    public String getFormLabel(boolean includeId) {
        //For now just return the name
        if (true) {
            return getName();
        }
        String url = getSiteUrl();
        if (url != null) {
            return HtmlUtils.href(url, getName(), " class=black_href ")
                   + (includeId
                      ? " Id:" + getId()
                      : "");
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
     *
     * @return _more_
     */
    public String getType() {
        return getClass().getSimpleName();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getSiteUrl() {
        return null;
    }

    public String getTooltip() {
        return getName();
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
                                           SelectInfo searchInfo)
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
         *
         * @return _more_
         */
        @Override
        public String getType() {
            return "ramadda";
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
        public List<Entry> getEntries(Request request, SelectInfo searchInfo)
                throws Exception {
            return getEntryManager().searchEntries(request);
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
         *
         * @return _more_
         */
        @Override
        public String getType() {
            return "ramadda";
        }

	@Override
	public String getTooltip() {
	    return getName()+HU.NL+serverInfo.getUrl();
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
         *
         * @param includeId _more_
         * @return _more_
         */
        @Override
        public String getFormLabel(boolean includeId) {
	    if(true) {
		String prefix = stringDefined(serverInfo.getSlug())?serverInfo.getSlug()+"- ":"";
		return  prefix+
		    getName() + (includeId
				 ? " Id:" + getId()
				 : "");
	    }


            return HtmlUtils.href(serverInfo.getUrl(), getName(),
                                  " class=black_href ") + (includeId
                    ? " Id:" + getId()
                    : "");
        }

        public String getFormSuffix() {
            return HtmlUtils.href(serverInfo.getUrl(),
				  HU.faIcon("fa-solid fa-link","target","_other",
					    "class","ramadda-clickable","title",
					    "Go to repository: " + serverInfo.getUrl()));
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
	public List<Entry> getEntries(Request request, SelectInfo searchInfo)
                throws Exception {
            String serverUrl = serverInfo.getUrl();
            request = request.cloneMe();
            request.remove(SearchManager.ARG_PROVIDER);
            request.put(ARG_OUTPUT, XmlOutputHandler.OUTPUT_XML);
            String linkUrl = request.getUrlArgs();
            String remoteSearchUrl =
                serverUrl + getSearchManager().URL_ENTRY_SEARCH.getPath()
                + "?" + linkUrl;
	    if(Utils.stringDefined(serverInfo.getSearchRoot())) {
		remoteSearchUrl+="&ancestor=" +serverInfo.getSearchRoot();
	    }
	    //	    System.err.println("remote search url:" + remoteSearchUrl);
	    IO.Result result = IO.doGetResult(new URL(remoteSearchUrl));
	    if(result.getError()) {
		getLogManager().logSpecial("Error doing remote search:" + remoteSearchUrl+" " + result.getResult());
		return new ArrayList<Entry>();
	    }


            return getEntryManager().createRemoteEntries(request, serverInfo, result.getResult());
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
