/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.cdmdata;

import org.ramadda.repository.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.output.WikiConstants;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import org.w3c.dom.*;
import ucar.unidata.util.Misc;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;


/**
 *
 */
@SuppressWarnings("unchecked")
public class GridTypeHandler extends GenericTypeHandler {


    /**
     * Construct a new GridAggregationTypeHandler
     *
     * @param repository   the Repository
     * @param node         the defining Element
     * @throws Exception   problems
     */
    public GridTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    @Override
    public String getProperty(Entry entry, String name, String dflt) {
        if ( !name.equals("chart.wiki")) {
            return super.getProperty(entry, name, dflt);
        }
        String s =
            "\n+row\n+col-6\n" + ":center &nbsp;\n"
            + "{{display height=200  width=100% type=linechart "
            + " showMenu=false  showTitle=false}} " + "<br>"
            + "{{display height=200  width=100% type=linechart accept.handleEventMapClick=false "
            + " showMenu=false  showTitle=false}}" + "\n-col-6\n+col-6\n"
            + ":center Click on map to select new location\n"
            + "{{display  height=400   width=100% type=map "
            + " recordHighlightFillColor=red recordHighlightRadius=8 "
            + " showMenu=false  showTitle=false}}\n" + "-col-6\n-row\n";

        return s;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param type _more_
     * @param target _more_
     *
     * @throws Exception _more_
     */
    public void addToSelectMenu(Request request, Entry entry,
                                StringBuilder sb, String type, String target)
            throws Exception {

        CdmDataOutputHandler cdoh =
            (CdmDataOutputHandler) getRepository().getOutputHandler(
                CdmDataOutputHandler.class);
        cdoh.addToSelectMenu(request, entry, sb, type, target);
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param tag _more_NN
     * @param props _more_
     * @param topProps _more_
     *
     * @return _more_
     */
    @Override
    public String getUrlForWiki(Request request, Entry entry, String tag,
                                Hashtable props, List<String> topProps) {

        if (tag.equals(WikiConstants.WIKI_TAG_CHART)
                || tag.equals(WikiConstants.WIKI_TAG_DISPLAY)
                || tag.startsWith("display_")) {
            StringBuilder jsonbuf = new StringBuilder();
            if (Misc.equals(props.get("doGrid"), "true")
                    || Misc.equals(props.get("doHeatmap"), "true")) {
                List<String> args = (List<String>) Misc.toList(new String[] {
                    ARG_ENTRYID, entry.getId(), "max",
                    Utils.getProperty(props, "max", "200000"), "gridStride",
                    Utils.getProperty(props, "gridStride", "1"), "gridBounds",
                    Utils.getProperty(props, "gridBounds", ""), "gridLevel",
                    Utils.getProperty(props, "gridLevel", "1"), "gridField",
                    Utils.getProperty(props, "gridField", "")
                });
                if (props.get("timeStride") != null) {
                    args.add("timeStride");
                    args.add(Utils.getProperty(props, "timeStride", "1"));
                }
                args.add("gridTime");
                args.add(Utils.getProperty(props, "gridTime", "0"));
                jsonbuf.append(getRepository().getUrlBase()
                               + "/grid/gridjson?"
                               + HtmlUtils.args(args, false));



                CdmDataOutputHandler cdoh =
                    (CdmDataOutputHandler) getRepository().getOutputHandler(
                        CdmDataOutputHandler.class);
                try {
                    cdoh.getWikiTagAttrs(request, entry, tag, props,
                                         topProps);
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }

                return jsonbuf.toString();
            }


            jsonbuf.append(getRepository().getUrlBase() + "/grid/pointjson?"
                           + HtmlUtils.args(new String[] { ARG_ENTRYID,
                    entry.getId() }, false));
            // get the lat/lon from the request if there
            String latArg = "${latitude}";
            String lonArg = "${longitude}";
            if (request.defined(ARG_LOCATION_LATITUDE)) {
                //                latArg = request.getString(ARG_LOCATION_LONGITUDE, latArg)
                jsonbuf.append("&");
                jsonbuf.append(
                    HtmlUtils.arg(
                        "default_latitude",
                        request.getString(ARG_LOCATION_LATITUDE, latArg),
                        false));
            }
            if (request.defined(ARG_LOCATION_LONGITUDE)) {
                //                lonArg = request.getString(ARG_LOCATION_LONGITUDE, lonArg)
                jsonbuf.append("&");
                jsonbuf.append(
                    HtmlUtils.arg(
                        "default_longitude",
                        request.getString(ARG_LOCATION_LONGITUDE, lonArg),
                        false));
            }
            jsonbuf.append("&");
            jsonbuf.append(HtmlUtils.arg(ARG_LOCATION_LATITUDE, latArg,
                                         false));
            jsonbuf.append("&");
            jsonbuf.append(HtmlUtils.arg(ARG_LOCATION_LONGITUDE, lonArg,
                                         false));

            // add in the list of selected variables as well
            String    VAR_PREFIX = Constants.ARG_VARIABLE + ".";
            Hashtable args       = request.getArgs();
            for (Enumeration keys = args.keys(); keys.hasMoreElements(); ) {
                String arg = (String) keys.nextElement();
                if (arg.startsWith(VAR_PREFIX) && request.get(arg, false)) {
                    jsonbuf.append("&");
                    jsonbuf.append(VAR_PREFIX);
                    jsonbuf.append(arg.substring(VAR_PREFIX.length()));
                    jsonbuf.append("=true");
                }
            }


            if (request.defined(ARG_FROMDATE)) {
                jsonbuf.append("&");
                jsonbuf.append(
                    HtmlUtils.arg(
                        ARG_FROMDATE, request.getString(ARG_FROMDATE)));
            }

            if (request.defined(ARG_TODATE)) {
                jsonbuf.append("&");
                jsonbuf.append(HtmlUtils.arg(ARG_TODATE,
                                             request.getString(ARG_TODATE)));
            }

            return jsonbuf.toString();
        }

        return super.getUrlForWiki(request, entry, tag, props, topProps);

    }





}
