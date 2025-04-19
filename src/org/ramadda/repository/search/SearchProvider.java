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

public abstract class SearchProvider extends GenericTypeHandler {
    public static final String CATEGORY_SCIENCE = "Science & Academic";
    public static final String CAPABILITY_AREA = "area";
    private String id;
    private List<String> aliases = new ArrayList<String>();
    private String name;
    private String apiKey;

    public SearchProvider(Repository repository) {
        super(repository, "", "Search Provider");
    }

    public SearchProvider(Repository repository, String id) {
        super(repository, id, "Search Provider");
        initId(id);
    }

    public SearchProvider(Repository repository, String id, String name) {
        super(repository, id, name);
        initId(id);
        this.name = ((name == null)
                     ? id
                     : name);
    }

    public String getCapabilities() {
        return "";
    }

    private void initId(String s) {
        List<String> toks = Utils.split(s, ",");
        this.id = toks.get(0);
        toks.remove(0);
        this.aliases.addAll(toks);
    }

    public boolean identifiedBy(String id) {
        if (this.id.equals(id)) {
            return true;
        }
        if (this.aliases.contains(id)) {
            return true;
        }

        return false;
    }

    public boolean isEnabled() {
        return true;
    }

    public String getApiKey() {
        if (apiKey == null) {
            apiKey = getRepository().getProperty(getId() + ".api.key");
        }

        return apiKey;
    }

    public TypeHandler getLinkTypeHandler() throws Exception {
        return getRepository().getTypeHandler("link");
    }

    public String getId() {
        return id;
    }

    public void setId(String s) {
        id = s;
    }

    public String getFormSuffix() {
	return "";
    }

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

    public String getCategory() {
        return "External Search Providers";
    }

    public String getType() {
        return getClass().getSimpleName();
    }

    public String getSiteUrl() {
        return null;
    }

    public String getTooltip() {
        return getName();
    }    

    public String getSearchProviderIconUrl() {
        return "${root}/favicon.png";
    }

    public String getName() {
        if (name == null) {
            return getId();
        }

        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract List<Entry> getEntries(Request request,
                                           SelectInfo searchInfo)
     throws Exception;

    public static class RamaddaSearchProvider extends SearchProvider {

        public RamaddaSearchProvider(Repository repository, String id,
                                     String name) {
            super(repository, id, name);
        }

        @Override
        public String getCategory() {
            return "";
        }

        @Override
        public String getType() {
            return "ramadda";
        }

        @Override
        public String getSearchProviderIconUrl() {
            return "${root}/favicon.png";
        }

        public List<Entry> getEntries(Request request, SelectInfo searchInfo)
                throws Exception {
            return getEntryManager().searchEntries(request);
        }

    }

    public URLConnection getConnection(String url) throws Exception {
        URLConnection connection = new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", "ramadda");

        return connection;
    }

    public InputStream getInputStream(String url) throws Exception {
        return getConnection(url).getInputStream();
    }

    public static class RemoteSearchProvider extends SearchProvider {

        private ServerInfo serverInfo;

        public RemoteSearchProvider(Repository repository,
                                    ServerInfo serverInfo) {
            super(repository, serverInfo.getId(), serverInfo.getLabel());
            this.serverInfo = serverInfo;
        }

        @Override
        public String getType() {
            return "ramadda";
        }

	@Override
	public String getTooltip() {
	    return getName()+HU.NL+serverInfo.getUrl();
	}    

        @Override
        public String getCategory() {
            return "Remote Repositories";
        }

        @Override
        public String getSearchProviderIconUrl() {
            return "${root}/favicon.png";
        }

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

    public int getRequestLimit(Request request) {
        return request.get(ARG_MAX, 50);
    }

}
