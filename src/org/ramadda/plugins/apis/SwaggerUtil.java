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

package org.ramadda.plugins.apis;


import org.ramadda.util.Json;


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
        mapItems.add(Json.quote(VERSION_API));
        mapItems.add(ATTR_SWAGGER_VERSION);
        mapItems.add(Json.quote(VERSION_SWAGGER));
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
        doc.add(Json.quote(basePath));
        doc.add(ATTR_RESOURCEPATH);
        doc.add(Json.quote(resourcePath));
        doc.add(ATTR_PRODUCES);
        doc.add(Json.list(Misc.toList(produces), true));
        doc.add(ATTR_APIS);
        doc.add(Json.list(apis));

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
        api.add(Json.quote(path));
        api.add(ATTR_OPERATIONS);
        api.add(Json.list(operations));

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
        operation.add(Json.list(responseMessages));
        operation.add(ATTR_PARAMETERS);
        operation.add(Json.list(parameters));

        if (produces != null) {
            operation.add(ATTR_PRODUCES);
            operation.add(Json.list(Misc.toList(produces), true));
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
        operation.add(Json.quote("GET"));
        operation.add(ATTR_SUMMARY);
        operation.add(Json.quote(summary));
        operation.add(ATTR_NOTES);
        operation.add(Json.quote(notes));
        operation.add(ATTR_NICKNAME);
        operation.add(Json.quote(nickname));

        //Add dummy auths
        operation.add(ATTR_AUTHORIZATIONS);
        operation.add(Json.map());

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
        mapItems.add(Json.quote(name));
        mapItems.add(ATTR_DESCRIPTION);
        mapItems.add(Json.quote(description));
        mapItems.add(ATTR_TYPE);
        mapItems.add(Json.quote(type));
        if (dflt != null) {
            mapItems.add(ATTR_DEFAULTVALUE);
            mapItems.add(Json.quote(dflt));
        }

        mapItems.add(ATTR_REQUIRED);
        mapItems.add("" + required);
        mapItems.add(ATTR_PARAMTYPE);
        mapItems.add(Json.quote("query"));
        if (values != null) {
            mapItems.add(ATTR_ALLOWABLEVALUES);
            mapItems.add(Json.map("valueType", Json.quote("LIST"), "values",
                                  Json.list(values, true)));
        }

        return Json.map(mapItems);

    }


}
