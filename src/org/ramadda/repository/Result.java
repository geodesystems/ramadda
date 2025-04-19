/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository;

import org.ramadda.repository.auth.AuthorizationMethod;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import java.io.InputStream;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

/**
 * A class to hold a Result
 */
@SuppressWarnings("unchecked")
public class Result {

    /** OK response code */
    public static final int RESPONSE_OK = 200;

    /** _more_ */
    public static final int RESPONSE_PARTIAL = 206;

    /** Not found response code */
    public static final int RESPONSE_NOTFOUND = 404;

    /** Unauthorized error response code */
    public static final int RESPONSE_UNAUTHORIZED = 401;

    /** Internal error response code */
    public static final int RESPONSE_INTERNALERROR = 500;

    /** _more_ */
    public static final int RESPONSE_BLOCKED = 403;

    public static final int RESPONSE_BADRREQUEST = 400;    

    /** HTML mime type */
    public static final String TYPE_HTML = "text/html";

    /** the redirect URL */
    private String redirectUrl;

    /**  */
    public static final String TYPE_JS = "application/x-javascript";

    /** XML mime type */
    public static final String TYPE_XML = "text/xml";

    /** CSV mime type */
    public static final String TYPE_CSV = "text/csv";

    /** CSV mime type */
    public static final String TYPE_JSON = "text/json";

    /** content bytes */
    private byte[] byteContent;

    /** _more_ */
    private String stringContent;

    /** the title */
    private String title = "";

    /** default mime type */
    private String mimeType = "text/html";

    /** flag for decorate */
    private boolean shouldDecorate = true;

    /** properties */
    private Hashtable properties = new Hashtable();

    /** the input stream */
    private InputStream inputStream;

    /** cache flag */
    private boolean cacheOk = false;

    /** last modified date */
    private Date lastModified;

    /** default response code */
    private int responseCode = RESPONSE_OK;

    /** header arguments */
    private List<String> httpHeaderArgs;

    /** need to write flag */
    private boolean needToWrite = true;

    /** authorization method */
    private AuthorizationMethod authorizationMethod;

    /**  */
    private boolean okToAddCookies = true;

    /**
     * Default ctor
     */
    public Result() {}

    /**
     * Create a Result with the specified Authorization Method
     *
     * @param authorizationMethod  the auth method
     */
    public Result(AuthorizationMethod authorizationMethod) {
        this.authorizationMethod = authorizationMethod;
    }

    /**
     * Create a result with a redirection
     *
     * @param redirectUrl  the URL for redirection
     */
    public Result(RequestUrl redirectUrl) {
        this(redirectUrl.toString());
    }

    /**
     * Create a Result with a redirection url
     *
     * @param redirectUrl  the redirection url
     */
    public Result(String redirectUrl) {
        this.redirectUrl    = redirectUrl;
        this.shouldDecorate = false;
    }

    /**
     * Create a Result
     *
     * @param title  the title
     * @param content  the content
     * @param foo  test flag
     */
    public Result(String title, byte[] content, boolean foo) {
        this(title, content, TYPE_HTML);
    }

    /**
     * Create a result with the content
     *
     * @param content  the content
     */
    public Result(Appendable content) {
        this("", content);
    }

    /**
     * Create a Result with the title and content
     *
     * @param title   the title
     * @param content the content
     */
    public Result(String title, Appendable content) {
        this(title, (byte[]) null, TYPE_HTML);
        stringContent = content.toString();
    }

    /**
     * Create a Result with the content and mimetype
     *
     * @param content  the content
     * @param mime     the mime type
     */
    public Result(String content, String mime) {
        this("", (byte[]) null, mime);
        stringContent = content;
    }

    /**
     * Create a Result with the title, content and mimetype
     *
     * @param title    the title
     * @param content  the content
     * @param mimeType     the mime type
     */
    public Result(String title, Appendable content, String mimeType) {
        this(title, (byte[]) null, mimeType);
        stringContent = content.toString();
    }

    /**
     * _more_
     *
     * @param content _more_
     * @param mimeType _more_
     * @param decorate _more_
     */
    public Result(Appendable content, String mimeType, boolean decorate) {
        this("", (byte[]) null, mimeType);
        stringContent       = content.toString();
        this.shouldDecorate = decorate;
    }

    /**
     * Create a Result with the title, content and mimetype
     *
     * @param title    the title
     * @param content  the content
     * @param mimeType the mime type
     */
    public Result(String title, byte[] content, String mimeType) {
        this(title, content, mimeType, true);
    }

    /**
     * Create a result from the input stream
     *
     * @param inputStream  the input stream
     * @param mimeType     the mimetype
     */
    public Result(InputStream inputStream, String mimeType) {
        this("", inputStream, mimeType);
    }

    /**
     * Create a result from the input stream
     *
     * @param title        the title
     * @param inputStream  the input stream
     * @param mimeType     the mimetype
     */
    public Result(String title, InputStream inputStream, String mimeType) {
        this.title          = title;
        this.inputStream    = inputStream;
        this.mimeType       = mimeType;
        this.shouldDecorate = false;
    }

    /**
     * Create a result from the content
     *
     * @param title        the title
     * @param content      the content
     * @param mimeType     the mimetype
     * @param shouldDecorate  true if the page should be decorated
     */
    public Result(String title, byte[] content, String mimeType,
                  boolean shouldDecorate) {
        this.byteContent    = content;
        this.title          = title;
        this.mimeType       = mimeType;
        this.shouldDecorate = shouldDecorate;
    }

    /**
     *  Set the OkToAddCookies property.
     *
     *  @param value The new value for OkToAddCookies
     */
    public void setOkToAddCookies(boolean value) {
        okToAddCookies = value;
    }

    /**
     *  Get the OkToAddCookies property.
     *
     *  @return The OkToAddCookies
     */
    public boolean getOkToAddCookies() {
        return okToAddCookies;
    }

    /**
     * Make a NO-OP result
     *
     * @return the result
     */
    public static Result makeNoOpResult() {
        Result result = new Result();
        result.setNeedToWrite(false);

        return result;
    }

    /**
     * Set the NeedToWrite property.
     *
     * @param value The new value for NeedToWrite
     */
    public void setNeedToWrite(boolean value) {
        needToWrite = value;
    }

    /**
     * Get the NeedToWrite property.
     *
     * @return The NeedToWrite
     */
    public boolean getNeedToWrite() {
        return needToWrite;
    }

    /**
     * Put a property for this result
     *
     * @param name   property name
     * @param value   property value
     */
    public void putProperty(String name, Object value) {
        properties.put(name, value);
    }

    /**
     * Get a property
     *
     * @param name  the property name
     *
     * @return  the property value or null
     */
    public Object getProperty(String name) {
        return properties.get(name);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public Object getProperty(String name, Object dflt) {
        Object prop = properties.get(name);
        if (prop == null) {
            return dflt;
        }

        return prop;
    }

    /**
     * Is this an HTML result?
     *
     * @return  true if no mimetype or mimetype is HTML
     */
    public boolean isHtml() {
        return (mimeType != null) && mimeType.equals(TYPE_HTML);
    }

    /**
     * Is this an XML result?
     *
     * @return  if mimetype is XML
     */
    public boolean isXml() {
        return mimeType.equals(TYPE_XML);
    }

    /**
     * Is this an CSV result?
     *
     * @return  true if mimetype is CSV
     */
    public boolean isCsv() {
        return mimeType.equals(TYPE_CSV);
    }

    /**
     * Set the Content property.
     *
     * @param value The new value for Content
     */
    public void setContent(byte[] value) {
        byteContent   = value;
        stringContent = null;
    }

    /**
     * _more_
     *
     * @param value _more_
     */
    public void setContent(String value) {
        stringContent = value;
        byteContent   = null;
    }

    /**
     * Get the Content property.
     *
     * @return The Content
     */
    public byte[] getContent() {
        if ((byteContent == null) && (stringContent != null)) {
            byteContent = stringContent.getBytes();
        }

        return byteContent;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getStringContent() {
        if ((stringContent == null) && (byteContent != null)) {
            stringContent = new String(byteContent);
        }

        return stringContent;
    }

    /**
     * Set the Title property.
     *
     * @param value The new value for Title
     */
    public void setTitle(String value) {
        title = value;
    }

    /**
     * Get the Title property.
     *
     * @return The Title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set the MimeType property.
     *
     * @param value The new value for MimeType
     */
    public void setMimeType(String value) {
        mimeType = value;
    }

    /**
     * Get the MimeType property.
     *
     * @return The MimeType
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Set the ShouldDecorate property.
     *
     * @param value The new value for ShouldDecorate
     */
    public void setShouldDecorate(boolean value) {
        shouldDecorate = value;
    }

    /**
     * Get the ShouldDecorate property.
     *
     * @return The ShouldDecorate
     */
    public boolean getShouldDecorate() {
        if (shouldDecorate && (mimeType != null) && (mimeType.length() > 0)
                && (mimeType.indexOf("html") < 0)) {
            return false;
        }

        return shouldDecorate;
    }

    /**
     * Set the InputStream property.
     *
     * @param value The new value for InputStream
     */
    public void setInputStream(InputStream value) {
        inputStream = value;
    }

    /**
     * Get the InputStream property.
     *
     * @return The InputStream
     */
    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     *  Set the RedirectUrl property.
     *
     *  @param value The new value for RedirectUrl
     */
    public void setRedirectUrl(String value) {
        redirectUrl = value;
    }

    /**
     *  Get the RedirectUrl property.
     *
     *  @return The RedirectUrl
     */
    public String getRedirectUrl() {
        return redirectUrl;
    }

    /**
     * Set the CacheOk property.
     *
     * @param value The new value for CacheOk
     */
    public void setCacheOk(boolean value) {
        cacheOk = value;
    }

    /**
     * Get the CacheOk property.
     *
     * @return The CacheOk
     */
    public boolean getCacheOk() {
        return cacheOk;
    }

    /**
     * Set the ResponseCode property.
     *
     * @param value The new value for ResponseCode
     */
    public void setResponseCode(int value) {
        responseCode = value;
    }

    /**
     * Get the ResponseCode property.
     *
     * @return The ResponseCode
     */
    public int getResponseCode() {
        return responseCode;
    }

    /**
     * Set the return filename for this Result
     *
     * @param filename  the filename
     */
    public void setReturnFilename(String filename) {
        setReturnFilename(filename, false);
    }

    /**
     *
     * @param filename _more_
     * @param inline _more_
     */
    public void setReturnFilename(String filename, boolean inline) {
        // filename = filename.replaceAll(" ", "_").replaceAll(",","_");
        if (inline) {
            addHttpHeader("Content-disposition", "filename=" + filename);
        } else {
            addHttpHeader("Content-disposition",
                          "attachment; filename=" + filename);
        }

    }

    /**
     * Add an HTTP header the the list of header args
     *
     * @param name   the header name
     * @param value  the header value
     */
    public void addHttpHeader(String name, String value) {
        if (httpHeaderArgs == null) {
            httpHeaderArgs = new ArrayList<String>();
        }
        httpHeaderArgs.add(name);
        httpHeaderArgs.add(value);
    }

    /**
     * Set the HttpHeaderArgs property.
     *
     * @param value The new value for HttpHeaderArgs
     */
    public void setHttpHeaderArgs(List<String> value) {
        httpHeaderArgs = value;
    }

    /**
     * Get the HttpHeaderArgs property.
     *
     * @return The HttpHeaderArgs
     */
    public List<String> getHttpHeaderArgs() {
        return httpHeaderArgs;
    }

    /**
     * Set the AuthorizationMethod property.
     *
     * @param value The new value for AuthorizationMethod
     */
    public void setAuthorizationMethod(AuthorizationMethod value) {
        this.authorizationMethod = value;
    }

    /**
     * Get the AuthorizationMethod property.
     *
     * @return The AuthorizationMethod
     */
    public AuthorizationMethod getAuthorizationMethod() {
        return this.authorizationMethod;
    }

    /**
     *  Set the LastModified property.
     *
     *  @param value The new value for LastModified
     */
    public void setLastModified(Date value) {
        this.lastModified = value;
    }

    /**
     *  Get the LastModified property.
     *
     *  @return The LastModified
     */
    public Date getLastModified() {
        return this.lastModified;
    }

}
