/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository;

import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class RequestUrl {

    private RepositorySource repositorySource;

    private String path = "foo";

    private String basePath;

    private String label = null;

    private boolean haveInitialized = false;

    private boolean needsSsl = false;

    public RequestUrl(RepositorySource repositorySource, String path) {
        this(repositorySource, path, false);
    }

    public RequestUrl(RepositorySource repositorySource, String path, boolean needsSsl) {
        this.repositorySource = repositorySource;
        this.path             = path;
        this.needsSsl         = needsSsl;
        if (path.endsWith("*")) {
            basePath = path.substring(0, path.length() - 2);
        } else {
            basePath = path;
        }
    }

    private List<String>aliases = new ArrayList<String>();

    public RequestUrl(RepositorySource repositorySource, String path,  String label,
		      String...aliases) {
        this(repositorySource, path);
        this.label = label;
	for(String alias:aliases) this.aliases.add(alias);
    }

    public boolean matches(Request request) {
	String path = request.getRequestPath();
	if(path.endsWith(getPath())) return true;
	for(String alias: aliases)
	    if(path.endsWith(alias)) return true;
	return false;
    }

    /**
     * Create a list of RequestUrl's from the array
     *
     * @param urls  the array of RequestUrls
     *
     * @return  the array as a list
     */
    public static List<RequestUrl> toList(RequestUrl[] urls) {
        List<RequestUrl> l = new ArrayList<RequestUrl>();
        for (RequestUrl r : urls) {
            l.add(r);
        }

        return l;
    }

    public String getFullUrl(String suffix) {
        checkInit();
        if (needsSsl) {
            String url = getHttpsUrl(suffix);
            return url;
        }

        String url2 =
            getRepositoryBase().absoluteUrl(getRepositoryBase().getUrlBase()
                                            + path) + ((suffix != null)
                ? suffix
                : "");
        return url2;
    }

    public String getFullUrl() {
        return getFullUrl("");
    }

    private RepositoryBase getRepositoryBase() {
        return repositorySource.getRepositoryBase();
    }

    public String getHttpsUrl(String suffix) {
        return getRepositoryBase().getHttpsUrl(
            getRepositoryBase().getUrlBase() + path) + suffix;
    }

    public String getHttpsUrl() {
        return getHttpsUrl("");
    }

    public String getUrlPath() {
        checkInit();
        if (needsSsl) {
            return getHttpsUrl();
        }

        return getRepositoryBase().getUrlBase() + path;
    }

    private void checkInit() {
        if ( !haveInitialized) {
            getRepositoryBase().initRequestUrl(this);
            haveInitialized = true;
        }
    }

    private String myString;

    public String toString() {
        if (myString == null) {
            checkInit();
            myString = getUrlPath();
        }

        return myString;
    }

    public String getUrl(String collectionPath) {
        return getRepositoryBase().getUrlBase() + "/" + collectionPath + path;
    }

    public String getLabel() {
        return label;
    }

    public String getBasePath() {
        return basePath;
    }

    public String getPath() {
        return path;
    }

    public boolean equals(Object o) {
        if ( !(o instanceof RequestUrl)) {
            return false;
        }
        RequestUrl that = (RequestUrl) o;

        return this.path.equals(that.path);
    }

    /**
     * Set the NeedsSsl property.
     *
     * @param value The new value for NeedsSsl
     */
    public void setNeedsSsl(boolean value) {
        this.needsSsl = value;
    }

    /**
     * Get the NeedsSsl property.
     *
     * @return The NeedsSsl
     */
    public boolean getNeedsSsl() {
        return this.needsSsl;
    }

}
