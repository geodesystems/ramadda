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

    /** _more_ */
    private RepositorySource repositorySource;

    /** _more_ */
    private String path = "foo";

    /** _more_ */
    private String basePath;

    /** _more_ */
    private String label = null;

    /** _more_ */
    private boolean haveInitialized = false;

    /** _more_ */
    private boolean needsSsl = false;

    /**
     * _more_
     *
     *
     * @param repositorySource _more_
     * @param path _more_
     */
    public RequestUrl(RepositorySource repositorySource, String path) {
        this(repositorySource, path, false);
    }


    /**
     * _more_
     *
     * @param repositorySource _more_
     * @param path _more_
     * @param needsSsl _more_
     */
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

    /**
     * _more_
     *
     *
     * @param repositorySource _more_
     * @param path _more_
     * @param label _more_
     */
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



    /**
     * _more_
     *
     * @param suffix _more_
     *
     * @return _more_
     */
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

    /**
     * _more_
     *
     * @return _more_
     */
    public String getFullUrl() {
        return getFullUrl("");
    }


    /**
     * _more_
     *
     * @return _more_
     */
    private RepositoryBase getRepositoryBase() {
        return repositorySource.getRepositoryBase();
    }

    /**
     * _more_
     *
     * @param suffix _more_
     *
     * @return _more_
     */
    public String getHttpsUrl(String suffix) {
        return getRepositoryBase().getHttpsUrl(
            getRepositoryBase().getUrlBase() + path) + suffix;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getHttpsUrl() {
        return getHttpsUrl("");
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public String getUrlPath() {
        checkInit();
        if (needsSsl) {
            return getHttpsUrl();
        }

        return getRepositoryBase().getUrlBase() + path;
    }

    /**
     * _more_
     */
    private void checkInit() {
        if ( !haveInitialized) {
            getRepositoryBase().initRequestUrl(this);
            haveInitialized = true;
        }
    }


    /** _more_ */
    private String myString;

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        if (myString == null) {
            checkInit();
            myString = getUrlPath();
        }

        return myString;
    }


    /**
     * _more_
     *
     * @param collectionPath _more_
     *
     * @return _more_
     */
    public String getUrl(String collectionPath) {
        return getRepositoryBase().getUrlBase() + "/" + collectionPath + path;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public String getLabel() {
        return label;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getBasePath() {
        return basePath;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getPath() {
        return path;
    }

    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
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
