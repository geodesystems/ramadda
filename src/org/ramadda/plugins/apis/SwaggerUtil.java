/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.apis;


import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;


import ucar.unidata.util.Misc;

import java.util.ArrayList;
import java.util.List;


/**
 */
public class SwaggerUtil {


    /** _more_ */
    public static final String VERSION_API = "1.0.0";

    /** _more_ */
    public static final String VERSION_SWAGGER = "1.2";

    /** _more_ */
    public static final String ATTR_API_VERSION = "apiVersion";

    /** _more_ */
    public static final String ATTR_ALLOWABLEVALUES = "allowableValues";

    /** _more_ */
    public static final String ATTR_SWAGGER_VERSION = "swaggerVersion";


    /** _more_ */
    public static final String ATTR_PATH = "path";

    /** _more_ */
    public static final String ATTR_BASEPATH = "basePath";

    /** _more_ */
    public static final String ATTR_RESOURCEPATH = "resourcePath";

    /** _more_ */
    public static final String ATTR_DESCRIPTION = "description";

    /** _more_ */
    public static final String ATTR_DEFAULTVALUE = "defaultValue";

    /** _more_ */
    public static final String ATTR_SUMMARY = "summary";

    /** _more_ */
    public static final String ATTR_AUTHORIZATIONS = "authorizations";

    /** _more_ */
    public static final String ATTR_NOTES = "notes";

    /** _more_ */
    public static final String ATTR_APIS = "apis";

    /** _more_ */
    public static final String ATTR_OPERATIONS = "operations";

    /** _more_ */
    public static final String ATTR_METHOD = "method";

    /** _more_ */
    public static final String ATTR_NICKNAME = "nickname";

    /** _more_ */
    public static final String ATTR_PARAMETERS = "parameters";

    /** _more_ */
    public static final String ATTR_ = "";


    /** _more_ */
    public static final String ATTR_NAME = "name";

    /** _more_ */
    public static final String ATTR_REQUIRED = "required";

    /** _more_ */
    public static final String ATTR_TYPE = "type";

    /** _more_ */
    public static final String ATTR_PARAMTYPE = "paramType";

    /** _more_ */
    public static final String ATTR_RESPONSEMESSAGES = "responseMessages";

    /** _more_ */
    public static final String ATTR_CODE = "code";

    /** _more_ */
    public static final String ATTR_MESSAGE = "message";

    /** _more_ */
    public static final String ATTR_DEPRECATED = "deprecated";

    /** _more_ */
    public static final String ATTR_PRODUCES = "produces";


    /** _more_ */
    public static final String TYPE_INTEGER = "integer";

    /** _more_ */
    public static final String TYPE_LONG = "long";

    /** _more_ */
    public static final String TYPE_FLOAT = "float";

    /** _more_ */
    public static final String TYPE_DOUBLE = "double";

    /** _more_ */
    public static final String TYPE_STRING = "string";

    /** _more_ */
    public static final String TYPE_BYTE = "byte";

    /** _more_ */
    public static final String TYPE_BOOLEAN = "boolean";

    /** _more_ */
    public static final String TYPE_DATE = "date";

    /** _more_ */
    public static final String TYPE_DATETIME = "dateTime";




    /**
     * _more_
     *
     * @param mapItems _more_
     */
    public static void initVersionItems(List<String> mapItems) {
        mapItems.add(ATTR_API_VERSION);
        mapItems.add(JsonUtil.quote(VERSION_API));
        mapItems.add(ATTR_SWAGGER_VERSION);
        mapItems.add(JsonUtil.quote(VERSION_SWAGGER));
    }


    /**
     * _more_
     *
     * @param basePath _more_
     * @param resourcePath _more_
     * @param produces _more_
     * @param apis _more_
     *
     * @return _more_
     */
    public static List<String> createDocument(String basePath,
            String resourcePath, String[] produces, List<String> apis) {
        List<String> doc = new ArrayList<String>();
        doc.add(ATTR_BASEPATH);
        doc.add(JsonUtil.quote(basePath));
        doc.add(ATTR_RESOURCEPATH);
        doc.add(JsonUtil.quote(resourcePath));
        doc.add(ATTR_PRODUCES);
        doc.add(JsonUtil.list(Misc.toList(produces), true));
        doc.add(ATTR_APIS);
        doc.add(JsonUtil.list(apis));

        return doc;
    }


    /**
     * _more_
     *
     * @param path _more_
     * @param operations _more_
     *
     * @return _more_
     */
    public static List<String> createApi(String path,
                                         List<String> operations) {
        List<String> api = new ArrayList<String>();
        api.add(ATTR_PATH);
        api.add(JsonUtil.quote(path));
        api.add(ATTR_OPERATIONS);
        api.add(JsonUtil.list(operations));

        return api;
    }





    /**
     * _more_
     *
     * @param summary _more_
     * @param notes _more_
     * @param nickname _more_
     * @param parameters _more_
     * @param responseMessages _more_
     *
     * @return _more_
     */
    public static List<String> createOperation(String summary, String notes,
            String nickname, List<String> parameters,
            List<String> responseMessages) {
        return createOperation(summary, notes, nickname, parameters,
                               responseMessages, null);
    }


    /**
     * _more_
     *
     * @param summary _more_
     * @param notes _more_
     * @param nickname _more_
     * @param parameters _more_
     * @param responseMessages _more_
     * @param produces _more_
     *
     * @return _more_
     */
    public static List<String> createOperation(String summary, String notes,
            String nickname, List<String> parameters,
            List<String> responseMessages, String[] produces) {
        List<String> operation = new ArrayList<String>();
        initOperation(operation, summary, notes, nickname);
        operation.add(ATTR_RESPONSEMESSAGES);
        operation.add(JsonUtil.list(responseMessages));
        operation.add(ATTR_PARAMETERS);
        operation.add(JsonUtil.list(parameters));

        if (produces != null) {
            operation.add(ATTR_PRODUCES);
            operation.add(JsonUtil.list(Misc.toList(produces), true));
        }

        return operation;
    }



    /**
     * _more_
     *
     * @param operation _more_
     * @param summary _more_
     * @param notes _more_
     * @param nickname _more_
     *
     * @return _more_
     */
    public static List<String> initOperation(List<String> operation,
                                             String summary, String notes,
                                             String nickname) {
        operation.add(ATTR_METHOD);
        operation.add(JsonUtil.quote("GET"));
        operation.add(ATTR_SUMMARY);
        operation.add(JsonUtil.quote(summary));
        operation.add(ATTR_NOTES);
        operation.add(JsonUtil.quote(notes));
        operation.add(ATTR_NICKNAME);
        operation.add(JsonUtil.quote(nickname));

        //Add dummy auths
        operation.add(ATTR_AUTHORIZATIONS);
        operation.add(JsonUtil.map(new ArrayList()));

        return operation;
    }




    /**
     * _more_
     *
     * @param name _more_
     * @param description _more_
     *
     * @return _more_
     */
    public static String getParameter(String name, String description) {
        return getParameter(name, description, null, false);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param description _more_
     * @param required _more_
     *
     * @return _more_
     */
    public static String getParameter(String name, String description,
                                      boolean required) {
        return getParameter(name, description, null, required);
    }




    /**
     * _more_
     *
     * @param name _more_
     * @param description _more_
     * @param dflt _more_
     * @param required _more_
     *
     * @return _more_
     */
    public static String getParameter(String name, String description,
                                      String dflt, boolean required) {
        return getParameter(name, description, dflt, required, TYPE_STRING);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param description _more_
     * @param dflt _more_
     * @param required _more_
     * @param type _more_
     *
     * @return _more_
     */
    public static String getParameter(String name, String description,
                                      String dflt, boolean required,
                                      String type) {
        return getParameter(name, description, dflt, required, type, null);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param description _more_
     * @param dflt _more_
     * @param required _more_
     * @param type _more_
     * @param values _more_
     *
     * @return _more_
     */
    public static String getParameter(String name, String description,
                                      String dflt, boolean required,
                                      String type, List<String> values) {
        List<String> mapItems = new ArrayList<String>();
        mapItems.add(ATTR_NAME);
        mapItems.add(JsonUtil.quote(name));
        mapItems.add(ATTR_DESCRIPTION);
        mapItems.add(JsonUtil.quote(description));
        mapItems.add(ATTR_TYPE);
        mapItems.add(JsonUtil.quote(type));
        if (dflt != null) {
            mapItems.add(ATTR_DEFAULTVALUE);
            mapItems.add(JsonUtil.quote(dflt));
        }

        mapItems.add(ATTR_REQUIRED);
        mapItems.add("" + required);
        mapItems.add(ATTR_PARAMTYPE);
        mapItems.add(JsonUtil.quote("query"));
        if (values != null) {
            mapItems.add(ATTR_ALLOWABLEVALUES);
            mapItems.add(JsonUtil.map(Utils.makeListFromValues("valueType",
                    JsonUtil.quote("LIST"), "values",
                    JsonUtil.list(values, true))));
        }

        return JsonUtil.map(mapItems);

    }


}
