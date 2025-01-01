/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.apis;


import org.ramadda.data.services.RecordConstants;


import org.ramadda.repository.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.repository.util.DateArgument;
import org.ramadda.service.Service;
import org.ramadda.service.ServiceArg;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;

import org.w3c.dom.Element;


import ucar.unidata.util.StringUtil;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;




/**
 */
public class SwaggerApiHandler extends RepositoryManager implements RequestHandler {


    /** _more_ */
    private static final SwaggerUtil SU = null;

    /** _more_ */
    public static final String BASE_PATH = "/swagger/api-docs";


    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @param props _more_
     *
     * @throws Exception _more_
     */
    public SwaggerApiHandler(Repository repository, Element node,
                             Hashtable props)
            throws Exception {
        super(repository);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param json _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result returnJson(Request request, StringBuffer json)
            throws Exception {
        //        request.setResultFilename("ramaddaswagger.json");
        Result result = new Result("", json, JsonUtil.MIMETYPE);
        request.setCORSHeaderOnResponse();

        return result;
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processApisRequest(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();

        return new Result("", sb);
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processSwaggerApiRequest(Request request) throws Exception {
        List<String> mapItems = new ArrayList<String>();
        SU.initVersionItems(mapItems);

        List<String> apis = new ArrayList<String>();
        int          cnt  = 0;
        apis.add(
            JsonUtil.map(
                Utils.makeListFromValues(
                    SU.ATTR_PATH, JsonUtil.quote("/point"),
                    SU.ATTR_DESCRIPTION, JsonUtil.quote("Point data API"))));

        apis.add(
            JsonUtil.map(
                Utils.makeListFromValues(
                    SU.ATTR_PATH, JsonUtil.quote("/gridaspoint"),
                    SU.ATTR_DESCRIPTION,
                    JsonUtil.quote("Grid point data API"))));

        apis.add(
            JsonUtil.map(
                Utils.makeListFromValues(
                    SU.ATTR_PATH, JsonUtil.quote("/gridsubset"),
                    SU.ATTR_DESCRIPTION, JsonUtil.quote("Grid subset API"))));

        for (OutputHandler outputHandler :
                getRepository().getOutputHandlers()) {
            if ( !(outputHandler instanceof ServiceOutputHandler)) {
                continue;
            }
            Service service =
                ((ServiceOutputHandler) outputHandler).getService();
            if ((service == null) || !service.isEnabled()) {
                continue;
            }

            String url = "/service/" + service.getId();
            apis.add(JsonUtil.map(Utils.makeListFromValues(SU.ATTR_PATH,
                    JsonUtil.quote(url), SU.ATTR_DESCRIPTION,
                    JsonUtil.quote(" API for " + service.getLabel()))));



        }

        for (TypeHandler typeHandler : getRepository().getTypeHandlers()) {
            if ( !typeHandler.getForUser()) {
                continue;
            }
            int entryCnt = getEntryUtil().getEntryCount(typeHandler);

            //Only show the types we have ??
            if (entryCnt == 0) {
                continue;
            }
            String url = "/type/" + typeHandler.getType();
            apis.add(JsonUtil.map(Utils.makeListFromValues(SU.ATTR_PATH,
                    JsonUtil.quote(url), SU.ATTR_DESCRIPTION,
                    JsonUtil.quote("Search API for '"
                                   + typeHandler.getLabel()
                                   + "' entry type"))));
        }
        mapItems.add(SU.ATTR_APIS);
        mapItems.add(JsonUtil.list(apis));
        StringBuffer sb = new StringBuffer();
        sb.append(JsonUtil.map(mapItems));

        return returnJson(request, sb);
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processSwaggerTypeRequest(Request request)
            throws Exception {
        List<String> toks = StringUtil.split(request.getRequestPath(), "/",
                                             true, true);
        String       type        = toks.get(toks.size() - 1);
        TypeHandler  typeHandler = getRepository().getTypeHandler(type);



        List<String> apis        = new ArrayList<String>();
        apis.add(getSearchApi(request, typeHandler));

        List<String> doc =
            SU.createDocument(request.getAbsoluteUrl(""),
                              getRepository().getUrlBase() + "/search/type/"
                              + type, new String[] { "application/json",
                "application/xml", "text/plain", "text/html" }, apis);

        return returnJson(request, new StringBuffer(JsonUtil.map(doc)));
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processSwaggerServiceRequest(Request request)
            throws Exception {
        List<String> toks = StringUtil.split(request.getRequestPath(), "/",
                                             true, true);
        String       type = toks.get(toks.size() - 1);
        Service service   = getRepository().getJobManager().getService(type);

        List<String> apis = new ArrayList<String>();
        apis.add(getServiceApi(request, service));

        List<String> doc =
            SU.createDocument(request.getAbsoluteUrl(""),
                              getRepository().URL_ENTRY_SHOW.toString(),
                              new String[] {}, apis);

        return returnJson(request, new StringBuffer(JsonUtil.map(doc)));
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param service _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String getServiceApi(Request request, Service service)
            throws Exception {
        List<String> parameters = new ArrayList<String>();
        parameters.add(SU.getParameter(ARG_OUTPUT,
                                       "Output type  -don't change",
                                       service.getId(), true));

        List<ServiceArg> args = new ArrayList<ServiceArg>();
        //TODO: We get everything including intermediate entries
        service.collectArgs(args);
        for (ServiceArg arg : args) {
            if (arg.isValueArg()) {
                continue;
            }
            String type = SU.TYPE_STRING;
            if (arg.isInt()) {
                type = SU.TYPE_INTEGER;
            } else if (arg.isFloat()) {
                type = SU.TYPE_FLOAT;
            }
            //TODO: enums
            if (arg.isFlag()) {
                if (arg.getGroup() != null) {
                    parameters.add(SU.getParameter(arg.getGroup(),
                            arg.getLabel(), null, arg.isRequired(), type));
                } else {
                    parameters.add(SU.getParameter(service.getUrlArg(null,
                            arg.getName()), arg.getLabel(), null,
                                            arg.isRequired(), type));
                }
            } else {
                String label  = arg.getLabel();
                String urlArg = service.getUrlArg(null, arg.getName());
                if (arg.isEntry()) {
                    label = "Entry ID";
                    //TODO: Not sure what to do here as this is most likely an intermediate arg
                    if (arg.isPrimaryEntry()) {
                        urlArg = ARG_ENTRYID;
                    } else {
                        continue;
                    }
                }
                parameters.add(SU.getParameter(urlArg, label, null,
                        arg.isPrimaryEntry() || arg.isRequired(), type));
            }
        }


        /*
        List<Column> columns = typeHandler.getColumns();
        if (columns != null) {
            for (Column column : columns) {
                if ( !column.getCanSearch()) {
                    continue;
                }

                String type = SU.TYPE_STRING;
                if (column.isEnumeration()) {
                    //TODO: list the enums
                } else if (column.isBoolean()) {
                    type = SU.TYPE_BOOLEAN;
                } else if (column.isDouble()) {
                    type = SU.TYPE_DOUBLE;
                } else if (column.isNumeric()) {
                    type = SU.TYPE_INTEGER;
                }
                parameters.add(SU.getParameter(column.getSearchArg(),
                        column.getLabel(), null, false, type));
            }
        }
        */

        List<String> operations = new ArrayList<String>();
        operations.add(
            JsonUtil.map(
			 SU.createOperation(
					    "API for " + service.getLabel(),
					    "API to call: " + service.getLabel(),
					    service.getId(), parameters,
					    new ArrayList<String>())));

        return JsonUtil.map(
			    SU.createApi(
					 getRepository().URL_ENTRY_SHOW.toString(), operations));
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param typeHandler _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String getSearchApi(Request request, TypeHandler typeHandler)
            throws Exception {
        List<String> parameters = new ArrayList<String>();
        addBaseSearchParameters(request, parameters);
        List<Column> columns = typeHandler.getColumns();
        if (columns != null) {
            for (Column column : columns) {
                if ( !column.getCanSearch()) {
                    continue;
                }
                String type = SU.TYPE_STRING;
                if (column.isEnumeration()) {
                    //TODO: list the enums
                } else if (column.isBoolean()) {
                    type = SU.TYPE_BOOLEAN;
                } else if (column.isDouble()) {
                    type = SU.TYPE_DOUBLE;
                } else if (column.isNumeric()) {
                    type = SU.TYPE_INTEGER;
                }
                parameters.add(SU.getParameter(column.getSearchArg(),
                        column.getLabel(), null, false, type));
            }
        }

        List<String> operations = new ArrayList<String>();
        operations.add(JsonUtil.map(SU.createOperation("Search API for '"
                + typeHandler.getLabel()
                + "' entry type", "API to search for entries of type "
                    + typeHandler.getLabel(), "search_"
                        + typeHandler.getType(), parameters, new ArrayList<String>())));

        return JsonUtil.map(
                SU.createApi(
                    getRepository().getUrlBase() + "/search/type/"
                    + typeHandler.getType(), operations));
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param parameters _more_
     *
     * @throws Exception _more_
     */
    private void addBaseSearchParameters(Request request,
                                         List<String> parameters)
            throws Exception {
        parameters.add(SU.getParameter(ARG_TEXT, "Search text"));
        parameters.add(SU.getParameter(ARG_NAME, "Search name"));
        parameters.add(SU.getParameter(ARG_DESCRIPTION,
                                       "Search description"));

        parameters.add(SU.getParameter(ARG_FROMDATE, "From date", null,
                                       false, SU.TYPE_DATETIME));
        parameters.add(SU.getParameter(ARG_TODATE, "To date", null, false,
                                       SU.TYPE_DATETIME));

        parameters.add(SU.getParameter(DateArgument.ARG_CREATE.getFromArg(),
                                       "Archive create date from", null,
                                       false, SU.TYPE_DATETIME));
        parameters.add(SU.getParameter(DateArgument.ARG_CREATE.getToArg(),
                                       "Archive create date to", null, false,
                                       SU.TYPE_DATETIME));

        parameters.add(SU.getParameter(DateArgument.ARG_CHANGE.getFromArg(),
                                       "Archive change date from", null,
                                       false, SU.TYPE_DATETIME));
        parameters.add(SU.getParameter(DateArgument.ARG_CHANGE.getToArg(),
                                       "Archive change date to", null, false,
                                       SU.TYPE_DATETIME));


        parameters.add(SU.getParameter(ARG_GROUP, "Parent entry"));
        parameters.add(SU.getParameter(ARG_FILESUFFIX, "File suffix"));


        parameters.add(SU.getParameter(ARG_MAXLATITUDE,
                                       "Northern bounds of search", null,
                                       false, SU.TYPE_FLOAT));
        parameters.add(SU.getParameter(ARG_MINLONGITUDE,
                                       "Western bounds of search", null,
                                       false, SU.TYPE_FLOAT));
        parameters.add(SU.getParameter(ARG_MINLATITUDE,
                                       "Southern bounds of search", null,
                                       false, SU.TYPE_FLOAT));
        parameters.add(SU.getParameter(ARG_MAXLONGITUDE,
                                       "Eastern bounds of search", null,
                                       false, SU.TYPE_FLOAT));

        parameters.add(SU.getParameter(ARG_MAX, "Max number of results",
                                       null, false, SU.TYPE_INTEGER));
        parameters.add(SU.getParameter(ARG_SKIP, "Number to skip", null,
                                       false, SU.TYPE_INTEGER));
    }






    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String getPointApi(Request request) throws Exception {
        List<String> parameters = new ArrayList<String>();

        parameters.add(SU.getParameter(ARG_ENTRYID, "Entry ID", null, true));

        List<String> formats = new ArrayList<String>();
        formats.add("points.json");
        formats.add("points.csv");
        parameters.add(SU.getParameter(ARG_OUTPUT,
                                       "Output type  -don't change",
                                       "points.product", true));


        parameters.add(SU.getParameter(RecordConstants.ARG_PRODUCT,
                                       "Product type", "points.json", true,
                                       SU.TYPE_STRING, formats));

        parameters.add(SU.getParameter(RecordConstants.ARG_ASYNCH,
                                       "Asynchronous", null, false,
                                       SU.TYPE_BOOLEAN));


        parameters.add(SU.getParameter(RecordConstants.ARG_SKIP,
                                       "Skip factor", null, false,
                                       SU.TYPE_INTEGER));

        parameters.add(SU.getParameter(ARG_MAXLATITUDE, "Northern bounds",
                                       null, false, SU.TYPE_FLOAT));
        parameters.add(SU.getParameter(ARG_MINLONGITUDE, "Western bounds",
                                       null, false, SU.TYPE_FLOAT));
        parameters.add(SU.getParameter(ARG_MINLATITUDE, "Southern bounds",
                                       null, false, SU.TYPE_FLOAT));
        parameters.add(SU.getParameter(ARG_MAXLONGITUDE, "Eastern bounds",
                                       null, false, SU.TYPE_FLOAT));

        /*
        parameters.add(SU.getParameter(ARG_AREA_NORTH, "Northern bounds"));
        parameters.add(SU.getParameter(ARG_AREA_WEST, "Western bounds"));
        parameters.add(SU.getParameter(ARG_AREA_SOUTH, "Southern bounds"));
        parameters.add(SU.getParameter(ARG_AREA_EAST, "Eastern bounds"));
        */

        List<String> operations = new ArrayList<String>();
        operations.add(
            JsonUtil.map(
			 SU.createOperation(
					    "Point data API", "API to access point data",
					    "pointdata", parameters, new ArrayList<String>())));

        return JsonUtil.map(
			    SU.createApi(
					 getRepository().getUrlBase() + "/entry/show",
					 operations));
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String getGridAsPointApi(Request request) throws Exception {
        List<String> parameters = new ArrayList<String>();

        parameters.add(SU.getParameter(ARG_ENTRYID, "Entry ID", null, true));

        List<String> formats = new ArrayList<String>();
        formats.add("csv");
        formats.add("kml");
        formats.add("json");


        parameters.add(SU.getParameter(ARG_OUTPUT, "Variable",
                                       "data.gridaspoint", true));


        parameters.add(
            SU.getParameter(
                ARG_VARIABLE, "Comma separated list of grid variables", null,
                true));
        parameters.add(SU.getParameter("format", "Format", null, true,
                                       SU.TYPE_STRING, formats));

        parameters.add(SU.getParameter(ARG_LOCATION_LATITUDE, "Latitude",
                                       null, true, SU.TYPE_FLOAT));
        parameters.add(SU.getParameter(ARG_LOCATION_LONGITUDE, "Longitude",
                                       null, true, SU.TYPE_FLOAT));

        parameters.add(SU.getParameter(ARG_FROMDATE, "From Date", null,
                                       false, SU.TYPE_DATETIME));
        parameters.add(SU.getParameter(ARG_TODATE, "To Date", null, false,
                                       SU.TYPE_DATETIME));

        List<String> operations = new ArrayList<String>();
        operations.add(
            JsonUtil.map(
			 SU.createOperation(
                        "Grid point data API",
                        "API to extract time series from gridded  data",
                        "gridaspointdata", parameters,
                        new ArrayList<String>())));

        return JsonUtil.map(
			    SU.createApi(
					 getRepository().getUrlBase() + "/entry/show",
					 operations));
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processSwaggerPointRequest(Request request)
            throws Exception {
        List<String> apis = new ArrayList<String>();
        apis.add(getPointApi(request));
        List<String> doc = SU.createDocument(request.getAbsoluteUrl(""),
                                             getRepository().getUrlBase()
                                             + "/point/data", new String[] {
                                                 "application/json",
                "text/csv" }, apis);

        return returnJson(request, new StringBuffer(JsonUtil.map(doc)));
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processSwaggerGridAsPointRequest(Request request)
            throws Exception {
        List<String> apis = new ArrayList<String>();
        apis.add(getGridAsPointApi(request));
        //This is the api from the geodata/cdmdata plugin
        List<String> doc = SU.createDocument(request.getAbsoluteUrl(""),
                                             getRepository().getUrlBase()
                                             + "/grid/json", new String[] {
                                                 "application/json" }, apis);

        return returnJson(request, new StringBuffer(JsonUtil.map(doc)));
    }




    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String getGridSubsetApi(Request request) throws Exception {
        List<String> parameters = new ArrayList<String>();

        parameters.add(SU.getParameter(ARG_ENTRYID, "Entry ID", null, true));


        parameters.add(SU.getParameter(ARG_OUTPUT,
                                       "Output id - don't change",
                                       "data.gridsubset", true));

        parameters.add(
            SU.getParameter(
                ARG_VARIABLE, "Comma separated list of grid variables", null,
                true));



        parameters.add(SU.getParameter(ARG_MAXLATITUDE,
                                       "Northern bounds of subset", null,
                                       false, SU.TYPE_FLOAT));
        parameters.add(SU.getParameter(ARG_MINLONGITUDE,
                                       "Western bounds of subset", null,
                                       false, SU.TYPE_FLOAT));
        parameters.add(SU.getParameter(ARG_MINLATITUDE,
                                       "Southern bounds of subset", null,
                                       false, SU.TYPE_FLOAT));
        parameters.add(SU.getParameter(ARG_MAXLONGITUDE,
                                       "Eastern bounds of subset", null,
                                       false, SU.TYPE_FLOAT));

        parameters.add(SU.getParameter(ARG_FROMDATE, "From Date", null,
                                       false, SU.TYPE_DATETIME));
        parameters.add(SU.getParameter(ARG_TODATE, "To Date", null, false,
                                       SU.TYPE_DATETIME));

        parameters.add(SU.getParameter("hstride", "Horizontal stride", null,
                                       false, SU.TYPE_INTEGER));

        List<String> operations = new ArrayList<String>();
        operations.add(
            JsonUtil.map(
			 SU.createOperation(
                        "Grid subset  API", "API to subset a grid",
                        "gridsubset", parameters, new ArrayList<String>(),
                        new String[] { "application/x-netcdf" })));

        return JsonUtil.map(
			    SU.createApi(
					 getRepository().getUrlBase() + "/entry/show",
					 operations));
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processSwaggerGridSubsetRequest(Request request)
            throws Exception {
        List<String> apis = new ArrayList<String>();
        apis.add(getGridSubsetApi(request));
        //This is the api from the geodata/cdmdata plugin
        List<String> doc =
            SU.createDocument(request.getAbsoluteUrl(""),
                              getRepository().getUrlBase() + "/grid/json",
                              new String[] { "application/x-netcdf" }, apis);

        return returnJson(request, new StringBuffer(JsonUtil.map(doc)));
    }






}
