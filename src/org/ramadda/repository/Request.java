/*
* Copyright (c) 2008-2021 Geode Systems LLC
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


import org.ramadda.repository.auth.User;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.OutputType;
import org.ramadda.repository.output.PageStyle;
import org.ramadda.repository.util.RequestArgument;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;
import org.ramadda.util.SelectionRectangle;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.WrapperException;


import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.ArrayList;

import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 *
 *
 * @author RAMADDA Development Team
 */
public class Request implements Constants, Cloneable {

    //    public org.ramadda.util.HtmlTemplate template = null;


    /** _more_ */
    public static final RequestArgument[] AREA_NWSE = { REQUESTARG_NORTH,
            REQUESTARG_WEST, REQUESTARG_SOUTH, REQUESTARG_EAST };




    /** _more_ */
    private static int COUNTER = 0;

    /** _more_ */
    public int count = COUNTER++;


    /** _more_ */
    private Hashtable fileUploads;

    /** _more_ */
    private String urlPath;

    /** _more_ */
    private Hashtable parameters;

    /** _more_ */
    private Hashtable originalParameters;

    /** _more_ */
    private Hashtable extraProperties = new Hashtable();

    /** _more_ */
    private Repository repository;

    /** _more_ */
    private Hashtable httpHeaderArgs;

    /** _more_ */
    private String sessionId;

    /** _more_ */
    private OutputStream outputStream;

    /** _more_ */
    private User user;

    /** _more_ */
    private String ip;

    /** _more_ */
    //    private Entry collectionEntry;

    private Entry rootEntry;
    private Entry currentEntry;    

    /** _more_ */
    private HttpServletRequest httpServletRequest;

    /** _more_ */
    private HttpServletResponse httpServletResponse;

    /** _more_ */
    private HttpServlet httpServlet;


    /** _more_ */
    private StringBuilder prefixHtml = new StringBuilder();

    /** _more_ */
    private StringBuilder suffixHtml = new StringBuilder();


    /** _more_ */
    private boolean checkingAuthMethod = false;

    /** _more_ */
    private ApiMethod apiMethod;

    /** _more_ */
    private boolean isMobile = false;

    /** _more_ */
    private boolean isRobot = false;

    /** _more_ */
    private boolean makeAbsoluteUrls = false;

    /** _more_ */
    private String htmlTemplateId;

    /** _more_ */
    private PageStyle pageStyle;

    /** _more_ */
    private boolean sessionIdWasSet = false;

    /** _more_ */
    private boolean sessionHasBeenHandled = false;


    /**
     * ctor
     *
     * @param repository the repository
     * @param user _more_
     */
    public Request(Repository repository, User user) {
        this.repository = repository;
        this.user       = user;
        this.urlPath    = "";
        this.parameters = new Hashtable();
    }


    /**
     * ctor
     *
     * @param repository the repository
     * @param user _more_
     * @param path _more_
     */
    public Request(Repository repository, User user, String path) {
        this.repository = repository;
        this.user       = user;
        this.urlPath    = path;
        this.parameters = new Hashtable();
    }


    /**
     * ctor
     *
     * @param that _more_
     * @param path _more_
     */
    public Request(Request that, String path) {
        this.repository          = that.getRepository();
        this.user                = that.getUser();
        this.urlPath             = path;
        this.parameters          = new Hashtable();
        this.originalParameters  = new Hashtable();
        this.isMobile            = that.isMobile;
        this.isRobot             = that.isRobot;
        this.ip                  = that.ip;
        this.httpServletRequest  = that.httpServletRequest;
        this.httpServletResponse = that.httpServletResponse;
        this.httpHeaderArgs      = that.httpHeaderArgs;
    }


    /**
     * ctor
     *
     * @param repository the repository
     * @param urlPath _more_
     * @param parameters _more_
     */
    public Request(Repository repository, String urlPath,
                   Hashtable parameters) {
        this.repository         = repository;
        this.urlPath            = urlPath;
        this.parameters         = parameters;
        this.originalParameters = new Hashtable();
        originalParameters.putAll(parameters);
    }



    /**
     * _more_
     *
     * @param repository the repository
     * @param urlPath _more_
     * @param parameters _more_
     * @param httpServletRequest _more_
     * @param httpServletResponse _more_
     * @param httpServlet _more_
     */
    public Request(Repository repository, String urlPath,
                   Hashtable parameters,
                   HttpServletRequest httpServletRequest,
                   HttpServletResponse httpServletResponse,
                   HttpServlet httpServlet) {
        this(repository, urlPath, parameters);
        this.httpServletRequest  = httpServletRequest;
        this.httpServletResponse = httpServletResponse;
        this.httpServlet         = httpServlet;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public Request cloneMe() {
        return cloneMe(null);
    }

    /**
     * _more_
     *
     * @param repository the repository
     *
     * @return _more_
     */
    public Request cloneMe(Repository repository) {
        try {
            Request that = (Request) super.clone();
            that.parameters         = (this.parameters != null)
                                      ? new Hashtable(this.parameters)
                                      : new Hashtable();
            that.originalParameters = (this.originalParameters != null)
                                      ? new Hashtable(this.originalParameters)
                                      : new Hashtable();

            if (repository != null) {
                that.repository = repository;
            }

            return that;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Repository getRepository() {
        return repository;
    }




    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public String getSession(String key, String dflt) throws Exception {
        return (String) getRepository().getSessionManager()
            .getSessionProperty(this, key, dflt);
    }

    /**
     * _more_
     *
     * @param key _more_
     * @param sessionPrefix _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public String getStringOrSession(String key, String sessionPrefix,
                                     String dflt)
            throws Exception {
        if (defined(key)) {
            return getString(key);
        }

        return getSession(sessionPrefix + key, dflt);
    }


    /**
     * _more_
     *
     * @param key _more_
     * @param value _more_
     *
     * @throws Exception On badness
     */
    public void putSession(String key, String value) throws Exception {
        getRepository().getSessionManager().putSessionProperty(this, key,
                value);
    }

    /**
     * _more_
     *
     * @param key _more_
     * @param prefix _more_
     */
    public void putSessionIfDefined(String key, String prefix) {
        try {
            getRepository().getSessionManager().putSessionProperty(this,
                    prefix + key, getString(key, ""));
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isConnected() {
        try {
            OutputStream os = getHttpServletResponse().getOutputStream();
            InputStream  is = getHttpServletRequest().getInputStream();
            //            System.err.println(is.available());
            is.read();
            os.flush();
            //System.err.println(is.available());
        } catch (Exception exc) {
            System.err.println("bad");

            return false;
        }

        return true;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public boolean useFullUrl() {
        return get(ARG_FULLURL, false);
    }


    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     */
    public String checkUrl(String url) {
        if (useFullUrl() && !url.startsWith("http")) {
            return getAbsoluteUrl(url);
        }

        return url;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isPost() {
        if (httpServletRequest == null) {
            return false;
        }

        return httpServletRequest.getMethod().equals("POST");
    }


    /**
     * _more_
     *
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getReferer(String dflt) {
        String referer = httpServletRequest.getHeader("referer");
        if (referer == null) {
            referer = dflt;
        }

        return referer;
    }


    /**
     * _more_
     *
     * @param filename _more_
     */
    public void setReturnFilename(String filename) {
        filename = filename.replaceAll(" ", "_");
        httpServletResponse.setHeader("Content-disposition",
                                      "attachment; filename=" + filename);
    }



    /**
     * _more_
     *
     * @param theUrl _more_
     * @param entry _more_
     *
     * @return _more_
     */
    public String getEntryUrlPath(String theUrl, Entry entry) {
        String url  = theUrl;
        String name = entry.getFullName(true);
        try {
            name = name.replace("/", "_FORWARDSLASH_");
            name = java.net.URLEncoder.encode(name, "UTF-8");
            name = name.replace("_FORWARDSLASH_", "/");
            name = name.replace("?", "_");
            name = name.replace("'", "_");
            name = name.replace(":", "-");
            //A hack because the browser thinks this a zipped page
            if (name.endsWith(".gz")) {
                name = name.replace(".gz", "");
            } else if (name.endsWith(".tgz")) {
                name = name.replace(".tgz", "");
            }
        } catch (Exception ignore) {}
        url = url + "/" + name;

        return checkUrl(url);
    }





    /**
     * _more_
     *
     * @param theUrl _more_
     * @param entry _more_
     * @param arg _more_
     * @param args _more_
     *
     * @return _more_
     */
    public String entryUrlWithArg(RequestUrl theUrl, Entry entry, String arg,
                                  String... args) {
        return entryUrl(theUrl, entry, arg, false, args);
    }


    /**
     * _more_
     *
     * @param theUrl _more_
     * @param entry _more_
     * @param args _more_
     *
     * @return _more_
     */
    public String entryUrl(RequestUrl theUrl, Entry entry, String... args) {
        //handle some possible backwards compatability issues
        if (args.length == 1) {
            return entryUrlWithArg(theUrl, entry, args[0]);
        }
        String arg = ARG_ENTRYID;

        return entryUrl(theUrl, entry, arg, false, args);
    }

    /**
     * _more_
     *
     * @param theUrl _more_
     * @param entry _more_
     * @param arg _more_
     * @param fullPath _more_
     * @param args _more_
     *
     * @return _more_
     */
    private String entryUrl(RequestUrl theUrl, Entry entry, String arg,
                            boolean fullPath, String... args) {
        if (entry.getIsRemoteEntry()) {
            String id = repository.getEntryManager().getRemoteEntryInfo(
                            entry.getId())[1];
            if (id.length() == 0) {
                return entry.getRemoteServer().getUrl() + theUrl.getPath();
            }

            String url = HtmlUtils.url(entry.getRemoteServer().getUrl()
                                       + theUrl.getPath(), arg, id);
            url = HtmlUtils.url(url, args);

            return url;
        }

        String path = fullPath
                      ? getEntryUrlPath(theUrl.toString(), entry)
                      : makeUrlPath(theUrl);
        String url  = HtmlUtils.url(path, arg, entry.getId());
        url = HtmlUtils.url(url, args);

        return url;
    }



    /**
     * _more_
     *
     * @param theUrl _more_
     * @param args _more_
     *
     * @return _more_
     */
    public String makeUrl(RequestUrl theUrl, String... args) {
        return HtmlUtils.url(makeUrlPath(theUrl), args);
    }


    /**
     * _more_
     *
     * @param theUrl _more_
     *
     * @return _more_
     */
    public String makeUrlPath(RequestUrl theUrl) {
        if (makeAbsoluteUrls) {
            return getAbsoluteUrl(theUrl);
        }

        return getRepository().getUrlPath(this, theUrl);
    }


    /**
     * _more_
     *
     * @param theUrl _more_
     *
     * @return _more_
     */
    public String form(RequestUrl theUrl) {
        return HtmlUtils.form(makeUrl(theUrl));
    }


    /**
     * _more_
     *
     * @param theUrl _more_
     *
     * @return _more_
     */
    public String formPost(RequestUrl theUrl) {
        return HtmlUtils.formPost(makeUrl(theUrl));
    }


    /**
     * _more_
     *
     * @param theUrl _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public String formPost(RequestUrl theUrl, String extra) {
        return HtmlUtils.formPost(makeUrl(theUrl), extra);
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param theUrl _more_
     */
    public void formPostWithAuthToken(Appendable sb, RequestUrl theUrl) {
        formPostWithAuthToken(sb, theUrl, null);
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param theUrl _more_
     * @param extra _more_
     */
    public void formPostWithAuthToken(Appendable sb, RequestUrl theUrl,
                                      String extra) {
        Utils.append(sb, formPost(theUrl, extra));
        repository.addAuthToken(this, sb);
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param theUrl _more_
     */
    public void uploadFormWithAuthToken(Appendable sb, RequestUrl theUrl) {
        uploadFormWithAuthToken(sb, theUrl, null);
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param theUrl _more_
     * @param extra _more_
     */
    public void uploadFormWithAuthToken(Appendable sb, RequestUrl theUrl,
                                        String extra) {
        Utils.append(sb, HtmlUtils.uploadForm(makeUrl(theUrl), extra));
        try {
            sb.append("\n");
        } catch (Exception exc) {
            throw new IllegalArgumentException(exc);
        }
        repository.addAuthToken(this, sb);
    }




    /**
     * _more_
     *
     * @param theUrl _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public String form(RequestUrl theUrl, String extra) {
        return HtmlUtils.form(makeUrl(theUrl), extra);
    }

    /**
     * _more_
     *
     * @param theUrl _more_
     *
     * @return _more_
     */
    public String uploadForm(RequestUrl theUrl) {
        return uploadForm(theUrl, "");
    }


    /**
     * _more_
     *
     * @param theUrl _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public String uploadForm(RequestUrl theUrl, String extra) {
        return HtmlUtils.uploadForm(makeUrl(theUrl), extra);
    }




    /**
     *  Set the OutputStream property.
     *
     *  @param value The new value for OutputStream
     */
    public void setOutputStream(OutputStream value) {
        outputStream = value;
    }

    /**
     *  Get the OutputStream property.
     *
     *  @return The OutputStream
     */
    public OutputStream getOutputStream() {
        return outputStream;
    }



    /**
     * _more_
     *
     * @param uploads _more_
     */
    public void setFileUploads(Hashtable uploads) {
        fileUploads = uploads;
    }


    /**
     * _more_
     *
     * @param arg _more_
     *
     * @return _more_
     */
    public String getUploadedFile(String arg) {
        if (fileUploads == null) {
            return null;
        }

        return (String) fileUploads.get(arg);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getUrl() {
        return checkUrl(getRequestPath() + "?" + getUrlArgs());
    }


    /**
     * _more_
     *
     * @param exceptArgs _more_
     * @param exceptValues _more_
     *
     * @return _more_
     */
    public String getUrl(HashSet<String> exceptArgs,
                         HashSet<String> exceptValues) {
        return checkUrl(getRequestPath() + "?"
                        + getUrlArgs(exceptArgs, exceptValues));
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public String getUrl(RequestUrl request) {
        return makeUrl(request) + "?" + getUrlArgs();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getAbsoluteUrl() {
        return getAbsoluteUrl(getUrl());
    }


    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     */
    public String getAbsoluteUrl(RequestUrl url) {
        String path = repository.getUrlBase() + url.getPath();

        return getAbsoluteUrl(path);
    }

    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     */
    public String getAbsoluteUrl(String url) {
        int     port        = getServerPort();
        boolean alwaysHttps = repository.getProperty(PROP_ALWAYS_HTTPS,
                                  false);
        String protocol = (alwaysHttps)
                          ? "https"
                          : "http";
        if ((httpServletRequest != null) && !alwaysHttps) {
            String scheme = httpServletRequest.getScheme();
            if (scheme != null) {
                List<String> toks = Utils.split(scheme, "/", true, true);
                if (toks.size() > 0) {
                    protocol = toks.get(0);
                }
            }
        }
        //        System.err.println("Request.getAbsoluteUrl:" + protocol +" port:" + port);
        if (port == 80) {
            return protocol + "://" + getServerName() + url;
        } else {
            return protocol + "://" + getServerName() + ":" + port + url;
        }
    }


    /**
     * _more_
     *
     * @param except _more_
     *
     * @return _more_
     */
    public String getUrl(String except) {
        return getRequestPath() + "?" + getUrlArgs(except);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getUrlArgs() {
        return getUrlArgs((HashSet) null);
    }

    /**
     * _more_
     *
     * @param except _more_
     *
     * @return _more_
     */
    public String getUrlArgs(String except) {
        HashSet<String> tmp = new HashSet<String>();
        tmp.add(except);

        return getUrlArgs(tmp);
    }


    /**
     * _more_
     *
     *
     * @param exceptArgs _more_
     *
     * @return _more_
     */
    public String getUrlArgs(HashSet<String> exceptArgs) {
        return getUrlArgs(exceptArgs, null);
    }


    /**
     * _more_
     *
     * @param exceptArgs _more_
     * @param exceptValues _more_
     *
     * @return _more_
     */
    public String getUrlArgs(HashSet<String> exceptArgs,
                             HashSet<String> exceptValues) {
        return getUrlArgs(exceptArgs, exceptValues, null);
    }



    /**
     * _more_
     *
     * @param exceptArgs _more_
     * @param exceptValues _more_
     * @param exceptArgsPattern _more_
     *
     * @return _more_
     */
    public String getUrlArgs(HashSet<String> exceptArgs,
                             HashSet<String> exceptValues,
                             String exceptArgsPattern) {

        if (exceptArgs == null) {
            exceptArgs = new HashSet<String>();
        }
        //Just in case, never want to let slip the passwords
        exceptArgs.add(ARG_USER_PASSWORD);
        exceptArgs.add(ARG_USER_PASSWORD1);
        exceptArgs.add(ARG_USER_PASSWORD2);

        StringBuilder sb      = new StringBuilder();
        int           cnt     = 0;
        List<String>  theKeys = new ArrayList<String>();
        for (Enumeration keys = parameters.keys(); keys.hasMoreElements(); ) {
            theKeys.add((String) keys.nextElement());
        }
        Collections.sort(theKeys);

        for (String arg : theKeys) {
            if ((exceptArgs != null) && (exceptArgs.contains(arg))) {
                continue;
            }

            if ((exceptArgsPattern != null)
                    && arg.matches(exceptArgsPattern)) {
                continue;
            }
            //      System.out.println(arg+":" + exceptArgsPattern+":");

            Object value = parameters.get(arg);
            if ((exceptValues != null) && (exceptValues.contains(value))) {
                continue;
            }
            if (value instanceof List) {
                List l = (List) value;
                if (l.size() == 0) {
                    continue;
                }
                for (int i = 0; i < l.size(); i++) {
                    String svalue = (String) l.get(i);
                    if (svalue.length() == 0) {
                        continue;
                    }
                    if (cnt++ > 0) {
                        sb.append("&");
                    }
                    HtmlUtils.arg(sb, arg, svalue, true);
                }

                continue;
            }
            String svalue = value.toString();
            if (svalue.length() == 0) {
                continue;
            }
            if (cnt++ > 0) {
                sb.append("&");
            }
            try {
                HtmlUtils.arg(sb, arg, svalue, true);
            } catch (Exception exc) {  /*noop*/
            }
        }

        String s = sb.toString();

        return HtmlUtils.sanitizeString(s);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isEmbedded() {
        return get(ARG_EMBEDDED, false);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getPathEmbeddedArgs() {
        try {
            StringBuilder sb  = new StringBuilder();
            int           cnt = 0;
            for (Enumeration keys =
                    parameters.keys(); keys.hasMoreElements(); ) {
                String arg   = (String) keys.nextElement();
                Object value = parameters.get(arg);
                if (value instanceof List) {
                    List l = (List) value;
                    if (l.size() == 0) {
                        continue;
                    }
                    for (int i = 0; i < l.size(); i++) {
                        String svalue = (String) l.get(i);
                        if (svalue.equals(VALUE_BLANK)) {
                            svalue = "";
                        } else if (svalue.length() == 0) {
                            continue;
                        }
                        if (cnt++ > 0) {
                            sb.append("/");
                        }
                        sb.append(arg + ":" + encodeEmbedded(svalue));
                    }

                    continue;
                }
                String svalue = value.toString();
                if (svalue.equals(VALUE_BLANK)) {
                    svalue = "";
                } else if (svalue.length() == 0) {
                    continue;
                }
                if (cnt++ > 0) {
                    sb.append("/");
                }
                sb.append(arg + ":" + encodeEmbedded(svalue));
            }

            return sb.toString();
        } catch (Exception exc) {
            throw new WrapperException(exc);
        }
    }


    /**
     * _more_
     *
     *
     * @param o _more_
     *
     * @return _more_
     */
    public static String encodeEmbedded(Object o) {
        String s = o.toString();
        try {
            if (s.indexOf("/") >= 0) {
                s = "b64:" + Utils.encodeBase64(s).trim();
            }

            //            s = java.net.URLEncoder.encode(s, "UTF-8");
            return s;
        } catch (Exception exc) {
            throw new WrapperException(exc);
        }
    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String decodeEmbedded(String s) {
        try {
            if (s.startsWith("b64:")) {
                s = s.substring(4);
                //s = java.net.URLDecoder.decode(s, "UTF-8");     
                s = new String(Utils.decodeBase64(s));
            }

            return s;
        } catch (Exception exc) {
            throw new WrapperException(exc);
        }

    }


    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public String getFromPath(String key) {
        String path = getRequestPath();
        //Look for .../id:<id>
        String prefix = key + ":";
        int    idx    = path.indexOf(prefix);
        if (idx >= 0) {
            String value = path.substring(idx + prefix.length());
            idx = value.indexOf("/");
            if (idx >= 0) {
                value = value.substring(0, idx);
            }
            try {
                value = decodeEmbedded(value);
            } catch (Exception exc) {
                throw new WrapperException(exc);
            }

            return value;
        }

        return null;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public Hashtable getDefinedProperties() {
        Hashtable props = new Hashtable();
        for (Enumeration keys = parameters.keys(); keys.hasMoreElements(); ) {
            String arg   = keys.nextElement().toString();
            Object value = parameters.get(arg);
            if (value instanceof List) {
                if (((List) value).size() == 0) {
                    continue;
                }
                props.put(arg, value);

                continue;
            }
            if (value.toString().length() == 0) {
                continue;
            }
            props.put(arg, value);
        }

        return props;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public Hashtable getArgs() {
        return parameters;
    }

    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    public boolean equals(Object o) {
        if ( !o.getClass().equals(getClass())) {
            return false;
        }
        Request that = (Request) o;

        return this.urlPath.equals(that.urlPath)
               && Misc.equals(this.user, that.user)
               && this.originalParameters.equals(that.originalParameters);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public int hashCode() {
        return urlPath.hashCode() ^ Misc.hashcode(user)
               ^ originalParameters.hashCode();
    }

    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public boolean hasParameter(String key) {
        return parameters.containsKey(key);
    }


    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public Object remove(Object key) {
        Object v = parameters.get(key);
        parameters.remove(key);

        return v;
    }

    /**
     * _more_
     *
     * @param props _more_
     */
    public void putAll(Hashtable props) {
        parameters.putAll(props);
    }


    /**
     * _more_
     *
     * @param key _more_
     * @param value _more_
     */
    public void put(Object key, Object value) {
        put(key, value, true);
    }

    /**
     * _more_
     *
     * @param key _more_
     * @param value _more_
     * @param singular _more_
     */
    public void put(Object key, Object value, boolean singular) {
        Object existing = singular
                          ? null
                          : parameters.get(key);
        if (existing != null) {
            if (existing instanceof List) {
                ((List) existing).add(value);
            } else {
                List newList = new ArrayList();
                newList.add(existing);
                newList.add(value);
                parameters.put(key, newList);
            }
        } else {
            if (value == null) {
                parameters.remove(key);
            } else {
                parameters.put(key, value);
            }
        }
    }


    /**
     * _more_
     *
     * @param key _more_
     * @param value _more_
     */
    public void putMultiples(Object key, Object value) {
        Object existing = parameters.get(key);
        if ((existing != null) && (existing instanceof List)) {
            ((List) existing).add(value);
        } else {
            List l = new ArrayList();
            if (existing != null) {
                l.add(existing);
            }
            l.add(value);
            parameters.put(key, l);
        }
    }


    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public boolean exists(Object key) {
        Object result = getValue(key, (Object) null);

        return result != null;
    }

    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public boolean defined(String key) {
        if (key == null) {
            return false;
        }
        Object result = getValue(key, (Object) null);
        if (result == null) {
            return false;
        }
        if (result instanceof List) {
            return ((List) result).size() > 0;
        }
        if (result instanceof String) {
            String sresult = (String) result;
            if (sresult.trim().length() == 0) {
                return false;
            }
            //Check if its a macro that was not set
            if (sresult.equals("${" + key + "}")) {
                return false;
            }
        }

        return true;
    }


    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public boolean hasMultiples(String key) {
        Object result = getValue(key, (Object) null);
        if (result == null) {
            return false;
        }

        return (result instanceof List);
    }




    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public List get(String key, List dflt) {
        Object result = getValue(key, (Object) null);
        if (result == null) {
            return dflt;
        }
        if (result instanceof List) {
            return (List) result;
        }
        List tmp = new ArrayList();
        tmp.add(result);

        return tmp;
    }


    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getUnsafeString(String key, String dflt) {
        String result = (String) getValue(key, (String) null);
        if (result == null) {
            return dflt;
        }

        return result;
    }


    /** _more_ */
    private static Pattern checker;

    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     * @param patternString _more_
     *
     * @return _more_
     */
    public String getCheckedString(String key, String dflt,
                                   String patternString) {
        return getCheckedString(key, dflt, Pattern.compile(patternString));
    }


    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getEncodedString(String key, String dflt) {
        String s = getString(key, dflt);
        if (s != null) {
            s = Utils.encodeUntrustedText(s);
            //            s = HtmlUtils.entityEncode(s);
        }

        return s;
    }


    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getAnonymousEncodedString(String key, String dflt) {
        if ( !isAnonymous()) {
            return getString(key, dflt);
        }

        return getEncodedString(key, dflt);
    }




    /**
     * _more_
     *
     * @param sb _more_
     */
    public void appendMessage(Appendable sb) {
        try {
            if (defined(ARG_MESSAGE)) {
                String message = getUnsafeString(ARG_MESSAGE, "");
                //            message = HtmlUtils.entityEncode(getUnsafeString(ARG_MESSAGE, "");
                message = PageHandler.getDialogString(message);
                //Encode this to keep from a spoof attack
                message = HtmlUtils.entityEncode(message);
                sb.append(
                    repository.getPageHandler().showDialogNote(message));
                remove(ARG_MESSAGE);
            }
        } catch (java.io.IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }




    /**
     * _more_
     */
    public void setCORSHeaderOnResponse() {
        if (repository.isCORSOk()) {
            httpServletResponse.setHeader("Access-Control-Allow-Methods",
                                          "POST, GET, OPTIONS , PUT");
            httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");
        }
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isAdmin() {
        return getUser().getAdmin();
    }


    /**
     * _more_
     */
    public void ensureAdmin() {
        if ( !getUser().getAdmin()) {
            throw new IllegalArgumentException("Need to be an administrator");
        }
    }

    /**
     * _more_
     */
    public void ensureAuthToken() {
        String authToken    = getString(ARG_AUTHTOKEN, (String) null);
        String mySessionId  = getSessionId();
        String argSessionId = getString(ARG_SESSIONID, (String) null);
        if (mySessionId == null) {
            mySessionId = argSessionId;
        }

        //        System.err.println("ensureAuthToken authToken:" + authToken +" arg session:"+ argSessionId +" session id:"+ mySessionId);
        if ((authToken == null) && (argSessionId != null)) {
            if (argSessionId.equals(mySessionId)) {
                //                System.err.println("ensureAuthToken arg session id == session id");
                return;
            }
            //            System.err.println("ensureAuthToken arg session id != session id");
        }

        if ((authToken != null) && (mySessionId != null)) {
            //            System.err.println("checking auth token");
            String sessionAuth = RepositoryUtil.hashString(mySessionId);
            //            System.err.println ("Request.ensureAuthToken:" + sessionId + " hashed:" +  sessionAuth +" token:" + authToken);
            if (authToken.trim().equals(sessionAuth)) {
                return;
            }
            getRepository().getLogManager().logError(
                "Request.ensureAuthToken: authToken != sessionAuth :"
                + "\n\tsession:" + mySessionId + "\n\tauth token:"
                + authToken + "\n\tsession hashed:" + sessionAuth, null);
        }

        //If we are publishing anonymously then don't look for a auth token
        if (get(ARG_ANONYMOUS, false) && isAnonymous()) {
            return;
        }


        getRepository().getLogManager().logError(
            "Request.ensureAuthToken: something null:" + "\n\tsession:"
            + mySessionId + "\n\tauth token:" + authToken, null);

        throw new IllegalArgumentException("Bad authentication token");
    }


    /**
     * _more_
     *
     * @param arg _more_
     *
     * @return _more_
     */
    public boolean defined(RequestArgument arg) {
        for (String key : getArgs(arg)) {
            if (defined(key)) {
                return true;
            }
        }

        return false;
    }

    /**
     * _more_
     *
     * @param arg _more_
     * @param dflt _more_
     * @param values _more_
     *
     * @return _more_
     */
    public String getEnum(String arg, String dflt, String... values) {
        String value = getString(arg, "");
        for (String enumValue : values) {
            if (value.equals(enumValue)) {
                return value;
            }
        }

        return dflt;
    }

    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public String getString(String key) {
        return getString(key, "");
    }


    /**
     * _more_
     *
     * @param arg _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getString(RequestArgument arg, String dflt) {
        for (String key : getArgs(arg)) {
            String value = getString(key, (String) null);
            if (value != null) {
                return value;
            }
        }

        return dflt;
    }


    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getString(String key, String dflt) {
        if (checker == null) {
            //Don't run the checker for now
            //checker =  Pattern.compile(repository.getProperty(PROP_REQUEST_PATTERN));
        }

        return getCheckedString(key, dflt, checker);
    }


    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getSanitizedString(String key, String dflt) {
        if (checker == null) {
            //Don't run the checker for now
            //checker =  Pattern.compile(repository.getProperty(PROP_REQUEST_PATTERN));
        }

        String s = getCheckedString(key, dflt, checker);

        return HtmlUtils.sanitizeString(s);
    }


    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     * @param pattern _more_
     *
     * @return _more_
     */
    public String getCheckedString(String key, String dflt, Pattern pattern) {
        String v = (String) getValue(key, (String) null);
        if (v == null) {
            return dflt;
        }

        //If the user is anonymous then replace all "script" strings with "_script_"
        if (isAnonymous()) {
            v = v.replaceAll("([sS][cC][rR][iI][pP][tT])", "_$1_");
        }


        if (pattern != null) {
            Matcher matcher = pattern.matcher(v);
            if ( !matcher.find()) {
                throw new BadInputException("Incorrect input for:" + key
                                            + " value:" + v + ":");
            }
        }

        return v;
    }


    /**
     * _more_
     *
     * @param arg _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public double get(RequestArgument arg, double dflt) {
        for (String key : getArgs(arg)) {
            if (defined(key)) {
                return get(key, dflt);
            }
        }

        return dflt;
    }




    /**
     * _more_
     */
    public void clearUrlArgs() {
        parameters = new Hashtable();
    }



    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    private Object getValue(Object key, Object dflt) {
        if (parameters == null) {
            return dflt;
        }
        Object result = parameters.get(key);
        if (result == null) {
            result = getFromPath(key.toString());
        }
        if (result == null) {
            return dflt;
        }

        return result;
    }


    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    private String getValue(Object key, String dflt) {
        Object result = parameters.get(key);
        if (result == null) {
            result = getFromPath(key.toString());
        }
        if (result == null) {
            return dflt;
        }
        if (result instanceof List) {
            List l = (List) result;
            if (l.size() == 0) {
                return dflt;
            }

            return (String) l.get(0);
        }
        String s = result.toString();
        if (s.startsWith("${")) {
            String extra =
                (String) repository.getSessionManager().getSessionExtra(s);
            if (extra != null) {
                s = extra;
            }
        }

        return s;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public OutputType getOutput() {
        return getOutput(OutputHandler.OUTPUT_HTML.getId());
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isOutputDefined() {
        if (defined(ARG_OUTPUT)) {
            return true;
        }
        String accept = getHeaderArg("Accept");
        if (accept != null) {
            //TODO: iterate through the handlers
            if (accept.equals("application/json")) {
                put(ARG_OUTPUT, "json");

                return true;
            }
        }

        return false;
    }


    /**
     * _more_
     *
     * @param dflt _more_
     *
     * @return _more_
     */
    public OutputType getOutput(String dflt) {
        String     typeId     = getString(ARG_OUTPUT, dflt);
        OutputType outputType = repository.findOutputType(typeId);
        if (outputType != null) {
            return outputType;
        }

        return new OutputType(typeId, OutputType.TYPE_FEEDS);
    }


    /**
     * _more_
     *
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getId(String dflt) {
        return getString(ARG_ENTRYID, dflt);
    }


    /**
     * _more_
     *
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getIds(String dflt) {
        return getString(ARG_ENTRYIDS, dflt);
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getDateSelect(String name, String dflt) {
        String v = getUnsafeString(name, (String) null);
        if (v == null) {
            return dflt;
        }
        if (defined(name + ".time")) {
            v = v + " " + getUnsafeString(name + ".time", "");
        }

        //TODO:Check value
        return v;
    }

    /**
     * _more_
     *
     * @param from _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public double getLatOrLonValue(RequestArgument from, double dflt) {
        if ( !defined(from)) {
            return dflt;
        }
        String llString = (String) getString(from, "").trim();
        if ((llString == null) || (llString.length() == 0)
                || (llString.startsWith("${"))) {
            return dflt;
        }
        double result = Utils.decodeLatLon(llString);

        return result;
    }



    /**
     * Get the value for a latitude or longitude property
     *
     * @param from the the property
     * @param dflt  the default value
     *
     * @return  the decoded value or the default if not defined
     *
     */
    public double getLatOrLonValue(String from, double dflt) {
        //        System.err.println("getLatOrLonValue url arg:" + from);
        if ( !defined(from)) {
            //            System.err.println("\tnnot defined returning dflt:" + dflt);
            return dflt;
        }
        String llString = (String) getString(from, "").trim();
        //        System.err.println("\tllstring:" + llString);
        if ((llString == null) || (llString.length() == 0)
                || (llString.startsWith("${"))) {
            return dflt;
        }

        double result = Utils.decodeLatLon(llString);

        //        System.err.println("\tdecoded value:" + result);
        return result;
    }


    /**
     * _more_
     *
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getWhat(String dflt) {
        return getString(ARG_WHAT, dflt);
    }

    /*
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */

    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public int get(Object key, int dflt) {
        String result = (String) getValue(key, (String) null);
        if ((result == null) || (result.trim().length() == 0)) {
            return dflt;
        }

        return new Integer(result).intValue();
    }


    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public long get(Object key, long dflt) {
        String result = (String) getValue(key, (String) null);
        if ((result == null) || (result.trim().length() == 0)) {
            return dflt;
        }

        return new Long(result).longValue();
    }

    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public double get(Object key, double dflt) {
        String result = (String) getValue(key, (String) null);
        if ((result == null) || (result.trim().length() == 0)) {
            return dflt;
        }

        return new Double(result).doubleValue();
    }


    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws java.text.ParseException On badness
     */
    public Date get(Object key, Date dflt) throws java.text.ParseException {
        String result = (String) getValue(key, (String) null);
        if ((result == null) || (result.trim().length() == 0)) {
            return dflt;
        }

        return Utils.parseDate(result);
    }

    /**
     * _more_
     *
     * @param from _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public Date getDate(String from, Date dflt) throws Exception {
        if ( !defined(from)) {
            return dflt;
        }
        String dateString = (String) getDateSelect(from, "").trim();

        return repository.getDateHandler().parseDate(dateString);
    }


    /**
     * _more_
     *
     * @param from _more_
     * @param to _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws java.text.ParseException On badness
     */
    public Date[] getDateRange(String from, String to, Date dflt)
            throws java.text.ParseException {
        return getDateRange(from, to, ARG_RELATIVEDATE, dflt);
    }

    /**
     * _more_
     *
     * @param from _more_
     * @param to _more_
     * @param relativeArg _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws java.text.ParseException On badness
     */
    public Date[] getDateRange(String from, String to, String relativeArg,
                               Date dflt)
            throws java.text.ParseException {
        String fromDate = "";
        String toDate   = "";
        if (defined(from) || defined(to)) {
            fromDate = (String) getDateSelect(from, "").trim();
            toDate   = (String) getDateSelect(to, "").trim();
        } else if (defined(relativeArg)) {
            fromDate = (String) getDateSelect(relativeArg, "").trim();
            if (fromDate.equals("none")) {
                return new Date[] { null, null };
            }
            toDate = "now";
        } else if (dflt == null) {
            return new Date[] { null, null };
        }

        //        System.err.println("from:" + fromDate+": to:" + toDate+":");
        if ( !Utils.stringDefined(fromDate) && !Utils.stringDefined(toDate)) {
            return new Date[] { null, null };
        }

        if (dflt == null) {
            dflt = new Date();
        }
        Date[] range = Utils.getDateRange(fromDate, toDate, dflt);
	//	System.err.println("from:" + fromDate +" to:" + toDate + " fd:" + range[0] +" td:" + range[1]);

        //        System.err.println("dateRange:" + fromDate + " date:" + range[0]);
        return range;
    }


    /**
     * _more_
     *
     * @param key _more_
     * @param value _more_
     *
     * @return _more_
     */
    public boolean setContains(String key, Object value) {
        List list = get(key, (List) null);
        if (list == null) {
            Object singleValue = getValue(key, (Object) null);
            if (singleValue == null) {
                return false;
            }

            return singleValue.equals(value);
        }

        return list.contains(value);
    }


    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public boolean get(Object key, boolean dflt) {
        String result = (String) getValue(key, (String) null);
        if ((result == null) || (result.trim().length() == 0)) {
            return dflt;
        }

        return new Boolean(result).booleanValue();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public Enumeration keys() {
        return parameters.keys();
    }

    /**
     * Set the UrlPath property.
     *
     * @param value The new value for UrlPath
     */
    public void setUrlPath(String value) {
        urlPath = value;
    }


    /**
     * _more_
     *
     * @param value _more_
     */
    public void setRequestPath(String value) {
        urlPath = value;
    }

    /**
     * Get the UrlPath property.
     *
     * @return The UrlPath
     */
    public String getRequestPath() {
        return urlPath;
    }



    /**
     * Class BadInputException On badness
     *
     *
     * @author RAMADDA Development Team
     * @version $Revision: 1.3 $
     */
    public static class BadInputException extends RuntimeException {

        /**
         * _more_
         *
         * @param msg _more_
         */
        public BadInputException(String msg) {
            super(msg);
        }
    }

    /**
     * Set the HttpHeaderArgs property.
     *
     * @param value The new value for HttpHeaderArgs
     */
    public void setHttpHeaderArgs(Hashtable value) {
        httpHeaderArgs = value;
        //TODO: be smarter about this
        String ua = getUserAgent("").toLowerCase();
        isMobile = (ua.indexOf("iphone") >= 0)
                   || (ua.indexOf("android") >= 0)
                   || (ua.indexOf("mobile") >= 0)
                   || (ua.indexOf("blackberry") >= 0);
        isRobot = checkForRobot();
    }

    /**
     * Get the HttpHeaderArgs property.
     *
     * @return The HttpHeaderArgs
     */
    public Hashtable getHttpHeaderArgs() {
        return httpHeaderArgs;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean canAcceptGzip() {
        String accept = (String) httpHeaderArgs.get("Accept-Encoding");
        if (accept == null) {
            System.err.println("no accept:" + httpHeaderArgs);

            return false;
        }
        List<String> toks = Utils.split(accept, ",", true, true);
        for (String tok : toks) {
            if ( !tok.startsWith("gzip")) {
                continue;
            }
            if (Utils.splitUpTo(tok, ";", 2).get(0).trim().equals("gzip")) {
                return true;
            }
        }

        return false;
    }


    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public String getHeaderArg(String name) {
        if (httpHeaderArgs == null) {
            return null;
        }
        String arg = (String) httpHeaderArgs.get(name);
        if (arg == null) {
            arg = (String) httpHeaderArgs.get(name.toLowerCase());
        }

        return arg;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        String args = getUrlArgs();
        if (args.trim().length() > 0) {
            return urlPath + " url args:" + args;
        } else {
            return urlPath;
        }
    }


    public String getKeys() {
        StringBuilder sb      = new StringBuilder();
        for (Enumeration keys = parameters.keys(); keys.hasMoreElements(); ) {
            sb.append((String) keys.nextElement());
	    sb.append("\n");
        }
	return sb.toString();
    }

    public String getDebug() {
        StringBuilder sb      = new StringBuilder();
        for (Enumeration keys = parameters.keys(); keys.hasMoreElements(); ) {
	    Object key = keys.nextElement();
            sb.append((String) key);
	    sb.append("=");
	    sb.append(parameters.get(key));
	    sb.append("\n");
        }
	return sb.toString();
    }
    




    /**
     * Set the SessionId property.
     *
     * @param value The new value for SessionId
     */
    public void setSessionId(String value) {
        sessionId = value;
        if (value != null) {
            sessionIdWasSet = true;
        } else {
            sessionIdWasSet = false;
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getSessionIdWasSet() {
        return sessionIdWasSet;
    }


    /**
     * Get the SessionId property.
     *
     * @return The SessionId
     */
    public String getSessionId() {
        return sessionId;
    }






    /**
     * Set the User property.
     *
     * @param value The new value for User
     */
    public void setUser(User value) {
        user = value;
    }

    /**
     * Get the User property.
     *
     * @return The User
     */
    public User getUser() {
        return user;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isAnonymous() {
        if ((user == null) || user.getAnonymous()) {
            return true;
        }

        return false;
    }

    /**
     * Set the Ip property.
     *
     * @param value The new value for Ip
     */
    public void setIp(String value) {
        ip = value;
    }

    /**
     * Get the Ip property.
     *
     * @return The Ip
     */
    public String getIp() {
        if ((ip == null) && (repository != null)) {
            return repository.getIpAddress();
        }

        return ip;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getIpRaw() {
        return ip;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getUserAgent() {
        return getUserAgent(null);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isMobile() {
        return isMobile;
    }

    /**
     * _more_
     *
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getUserAgent(String dflt) {
        String value = getHeaderArg("User-Agent");
        if (value == null) {
            //            System.err.println("no user agent");
            return dflt;
        }

        return value;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getIsRobot() {
        return isRobot;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isRealRequest() {
        return httpServletResponse != null;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    private boolean checkForRobot() {
        String userAgent = getUserAgent();
        if (userAgent == null) {
            return false;
        }
        userAgent = userAgent.toLowerCase();

        //hack - Special case for Slack
        if (userAgent.indexOf("slack.com") >= 0) {
            return false;
        }

        return ((userAgent.indexOf("googlebot") >= 0)
                || (userAgent.indexOf("bot") >= 0)
                || (userAgent.indexOf("slurp") >= 0)
                || (userAgent.indexOf("spider") >= 0));

    }



    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isHeadRequest() {
        if (httpServletRequest != null) {
            return httpServletRequest.getMethod().equals("HEAD");
        }

        return false;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getServerName() {
        String serverName = null;
        if ( !repository.useFixedHostnameForAbsoluteUrls()) {
            serverName = getRequestHostname();
        }
        if ( !Utils.stringDefined(serverName)) {
            serverName = repository.getHostname().trim();
        }

        return serverName;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getRequestHostname() {
        String serverName = null;
        try {
            if (httpServletRequest != null) {
                serverName =
                    httpServletRequest.getHeader("HTTP_X_FORWARDED_SERVER");
                if (serverName == null) {
                    serverName = httpServletRequest.getServerName();
                }
            }
        } catch (Exception ignoreThis) {}

        return serverName;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public int getServerPort() {
        if ( !repository.useFixedHostnameForAbsoluteUrls()) {
            try {
                if (httpServletRequest != null) {
                    return httpServletRequest.getServerPort();
                }
            } catch (Exception ignoreThis) {}
        }

        return repository.getPort();
    }


    /**
     *  Get the HttpServletRequest property.
     *
     *  @return The HttpServletRequest
     */
    public HttpServletRequest getHttpServletRequest() {
        return httpServletRequest;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public HttpServletResponse getHttpServletResponse() {
        return httpServletResponse;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public javax.servlet.http.HttpServlet getHttpServlet() {
        return httpServlet;
    }

    /**
     * Set the PrefixHtml property.
     *
     * @param value The new value for PrefixHtml
     */
    public void appendPrefixHtml(String value) {
        prefixHtml.append(value);
    }

    /**
     * Get the PrefixHtml property.
     *
     * @return The PrefixHtml
     */
    public String getPrefixHtml() {
        return prefixHtml.toString();
    }

    /**
     * Set the SuffixHtml property.
     *
     * @param value The new value for SuffixHtml
     */
    public void appendSuffixHtml(String value) {
        suffixHtml.append(value);
    }

    /**
     * Get the SuffixHtml property.
     *
     * @return The SuffixHtml
     */
    public String getSuffixHtml() {
        return suffixHtml.toString();
    }


    /**
     * _more_
     *
     * @param key _more_
     * @param value _more_
     */
    public void putExtraProperty(Object key, Object value) {
        extraProperties.put(key, value);
    }

    public String getUniqueId(String prefix) {
	Integer base = (Integer) extraProperties.get("uniquebase");
	if(base==null) {
	    base = new Integer(0);
	    extraProperties.put("uniquebase", base);
	}
	base = base.intValue()+1;
	extraProperties.put("uniquebase", base);
	return prefix+base;
    }


    /**
     * _more_
     *
     * @param s _more_
     */
    public synchronized void appendHead0(String s) {
        StringBuilder head0 = (StringBuilder) getExtraProperty("head0");
        if (head0 == null) {
            head0 = new StringBuilder();
            putExtraProperty("head0", head0);
        }
        head0.append(s);
        head0.append("\n");
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getHead0() {
        StringBuilder head0 = (StringBuilder) getExtraProperty("head0");
        if (head0 == null) {
            return null;
        } else {
            return head0.toString();
        }
    }





    /**
     * _more_
     *
     * @param head _more_
     *
     * @param s _more_
     */
    public synchronized void appendHead(String s) {
        StringBuilder head = (StringBuilder) getExtraProperty("head");
        if (head == null) {
            head = new StringBuilder();
            putExtraProperty("head", head);
        }
        head.append(s);
        head.append("\n");
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getHead() {
        StringBuilder head = (StringBuilder) getExtraProperty("head");
        if (head == null) {
            return null;
        } else {
            return head.toString();
        }
    }


    /**
     * _more_
     *
     * @param key _more_
     */
    public void removeExtraProperty(Object key) {
        extraProperties.remove(key);
    }

    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public Object getExtraProperty(Object key) {
        return extraProperties.get(key);
    }

    /**
     * Get the Protocol property.
     *
     * @return The Protocol
     */
    public String getProtocol() {
        return httpServletRequest.getProtocol();
    }



    /**
     * Get the Secure property.
     *
     * @return The Secure
     */
    public boolean getSecure() {
        return httpServletRequest.isSecure();
    }

    /**
     * Set the CheckingAuthMethod property.
     *
     * @param value The new value for CheckingAuthMethod
     */
    public void setCheckingAuthMethod(boolean value) {
        this.checkingAuthMethod = value;
    }

    /**
     * Get the CheckingAuthMethod property.
     *
     * @return The CheckingAuthMethod
     */
    public boolean getCheckingAuthMethod() {
        return this.checkingAuthMethod;
    }

    /**
     * Set the ApiMethod property.
     *
     * @param value The new value for ApiMethod
     */
    public void setApiMethod(ApiMethod value) {
        this.apiMethod = value;
    }

    /**
     * Get the ApiMethod property.
     *
     * @return The ApiMethod
     */
    public ApiMethod getApiMethod() {
        return this.apiMethod;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean responseAsXml() {
        return getString(ARG_RESPONSE, "").equals(RESPONSE_XML);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean responseAsJson() {
        return getString(ARG_RESPONSE, "").equals(RESPONSE_JSON)
               || getString(ARG_OUTPUT).equals("json");
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean responseAsData() {
        return responseAsJson() || responseAsXml() || responseAsText();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean responseAsText() {
        return getString(ARG_RESPONSE, "").equals(RESPONSE_TEXT);
    }

    /**
     *  Set the HtmlTemplateId property.
     *
     *  @param value The new value for HtmlTemplateId
     */
    public void setHtmlTemplateId(String value) {
        htmlTemplateId = value;
    }

    /**
     *  Get the HtmlTemplateId property.
     *
     *  @return The HtmlTemplateId
     */
    public String getHtmlTemplateId() {
        if (htmlTemplateId != null) {
            return htmlTemplateId;
        }

        return getString(ARG_TEMPLATE, (String) null);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getLanguage() {
        if (exists(ARG_LANGUAGE)) {
            return getString(ARG_LANGUAGE, "");
        }
        User   user     = getUser();
        String language = user.getLanguage();

        return language;
    }

    /**
     * Set the PageStyle property.
     *
     * @param value The new value for PageStyle
     */
    public void setPageStyle(PageStyle value) {
        pageStyle = value;
    }

    /**
     * Get the PageStyle property.
     *
     *
     * @param entry _more_
     * @return The PageStyle
     */
    public PageStyle getPageStyle(Entry entry) {
        if (pageStyle == null) {
            pageStyle = repository.getPageHandler().doMakePageStyle(this,
                    entry);
        }

        return pageStyle;
    }

    /**
     * _more_
     *
     * @param file _more_
     * @param filename _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public Result returnFile(File file, String filename) throws Exception {
        setReturnFilename(filename);
        Result result = new Result();
        result.setNeedToWrite(false);
        OutputStream os = getHttpServletResponse().getOutputStream();
        InputStream fis =
            getRepository().getStorageManager().getFileInputStream(
                file.toString());
        IOUtil.writeTo(fis, os);
        IOUtil.close(os);
        IOUtil.close(fis);

        return result;
    }


    /**
     * _more_
     *
     * @param is _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public Result returnStream(InputStream is) throws Exception {
        Result result = new Result();
        result.setNeedToWrite(false);
        OutputStream os = getHttpServletResponse().getOutputStream();
        IOUtil.writeTo(is, os);
        IOUtil.close(os);
        IOUtil.close(is);

        return result;
    }


    /*
       Makes a result where the caller will be writing directly to the
       http output stream which is gotten by calling request.getOutputStream()
    */

    /**
     * _more_
     *
     * @param filename _more_
     * @param mimeType _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result getOutputStreamResult(String filename, String mimeType)
            throws Exception {
        getHttpServletResponse().setContentType(mimeType);
        setReturnFilename(filename);
        Result result = new Result(filename, (byte[]) null, mimeType);
        result.setNeedToWrite(false);

        return result;
    }



    /**
     * Set the MakeAbsoluteUrls property.
     *
     * @param value The new value for MakeAbsoluteUrls
     */
    public void setMakeAbsoluteUrls(boolean value) {
        makeAbsoluteUrls = value;
    }

    /**
     * Get the MakeAbsoluteUrls property.
     *
     * @return The MakeAbsoluteUrls
     */
    public boolean getMakeAbsoluteUrls() {
        return makeAbsoluteUrls;
    }


    /**
     *  Set the SessionHasBeenHandled property.
     *
     *  @param value The new value for SessionHasBeenHandled
     */
    public void setSessionHasBeenHandled(boolean value) {
        sessionHasBeenHandled = value;
    }

    /**
     *  Get the SessionHasBeenHandled property.
     *
     *  @return The SessionHasBeenHandled
     */
    public boolean getSessionHasBeenHandled() {
        return sessionHasBeenHandled;
    }

    /**
     *  Set the RootEntry property.
     *
     *  @param value The new value for RootEntry
     */
    public void setRootEntry(Entry value) {
        rootEntry = value;
    }

    /**
     *  Get the RootEntry property.
     *
     *  @return The RootEntry
     */
    public Entry getRootEntry() {
        if (rootEntry == null) {
            rootEntry = repository.getEntryManager().getRootEntry(this);
        }

        return rootEntry;
    }



    /**
       Set the CurrentEntry property.

       @param value The new value for CurrentEntry
    **/
    public void setCurrentEntry (Entry value) {
	currentEntry = value;
    }

    /**
       Get the CurrentEntry property.

       @return The CurrentEntry
    **/
    public Entry getCurrentEntry () {
	return currentEntry;
    }


    /**
     * _more_
     *
     * @param request The request
     *
     * @return _more_
     */
    public  SelectionRectangle getSelectionBounds() {
        String[] argPrefixes = { ARG_AREA, ARG_BBOX };
        double[] bbox = { Double.NaN, Double.NaN, Double.NaN, Double.NaN };
        for (String argPrefix : argPrefixes) {
            if (defined(argPrefix)) {
                List<String> toks =
                    Utils.split(getString(argPrefix, ""), ",",
				true, true);
                //n,w,s,e
                if (toks.size() == 4) {
                    for (int i = 0; i < 4; i++) {
                        bbox[i] = Double.parseDouble(toks.get(i));
                    }
                }
            }
        }

        for (int i = 0; i < 4; i++) {
            if (defined(AREA_NWSE[i])) {
                bbox[i] = get(AREA_NWSE[i], 0.0);
            }
        }

        return new SelectionRectangle(bbox[0], bbox[1], bbox[2], bbox[3]);

    }


    
    /**
     * _more_
     *
     *
     * @param request _more_
     * @return _more_
     */
    public List<String> getArgs(RequestArgument arg) {
	List<String> args = arg.getArgs();
	String argsProperty = arg.getProperty();
        if (args == null) {
            args = Utils.split(
			       getRepository().getProperty(argsProperty, ""));
        }

        return args;
    }




    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {}




}
