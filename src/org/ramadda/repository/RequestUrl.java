/*
* Copyright (c) 2008-2018 Geode Systems LLC
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
    public RequestUrl(RepositorySource repositorySource, String path,
                      boolean needsSsl) {
        this.repositorySource = repositorySource;
        this.path             = path;
        this.needsSsl         = needsSsl;
        if (path.endsWith("*")) {
            basePath = path.substring(0, path.length() - 2);
        } else {
            basePath = path;
        }
    }

    /**
     * _more_
     *
     *
     * @param repositorySource _more_
     * @param path _more_
     * @param label _more_
     */
    public RequestUrl(RepositorySource repositorySource, String path,
                      String label) {
        this(repositorySource, path);
        this.label = label;
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
            return getHttpsUrl(suffix);
        }

        return getRepositoryBase().absoluteUrl(
            getRepositoryBase().getUrlBase() + path) + ((suffix != null)
                ? suffix
                : "");
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
