/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.media;


import org.json.*;

import org.ramadda.data.docs.*;

import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;

import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.data.services.RecordTypeHandler;

import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;

import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;
import org.ramadda.util.seesv.Seesv;

import org.w3c.dom.Element;

import java.io.*;

import java.net.URL;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 */
@SuppressWarnings("unchecked")
public class VegaTypeHandler extends ConvertibleTypeHandler {


    /** _more_ */
    private static int IDX = RecordTypeHandler.IDX_LAST + 1;



    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public VegaTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param properties _more_
     * @param requestProperties _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public RecordFile doMakeRecordFile(Request request, Entry entry,
                                       Hashtable properties,
                                       Hashtable requestProperties)
            throws Exception {
        return new VegaRecordFile(getRepository(), this, entry,
                                  getPathForRecordEntry(request,entry,  requestProperties));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param fromImport _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {}

    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
        if ( !tag.equals("vega_chart")) {
            //          return super.getWikiInclude(wikiUtil, request, originalEntry, entry, tag, props);
        }
        //          String width  = entry.getValue(request,IDX_WIDTH, "320");
        //          String height = entry.getValue(request,IDX_HEIGHT, "256");
        String vega = getStorageManager().readSystemResource(entry.getFile());
        StringBuilder sb = new StringBuilder();
        wikiUtil.handleVega(sb, vega, wikiUtil.getHandler());

        return sb.toString();
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    @Override
    public boolean shouldProcessResource(Request request, Entry entry) {
        return false;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param tabTitles _more_
     * @param tabContents _more_
     */
    @Override
    public void addToInformationTabs(Request request, Entry entry,
                                     List<String> tabTitles,
                                     List<String> tabContents) {
        super.addToInformationTabs(request, entry, tabTitles, tabContents);
        try {
            StringBuilder js = new StringBuilder();
            js.append("//\n//Generated JS\n//\n");
            StringBuilder sb = new StringBuilder();
            String vega =
                getStorageManager().readSystemResource(entry.getFile());
            String jsonId = "vegaJson" + HtmlUtils.blockCnt++;
            js.append("var " + jsonId + "=" + vega + "\n");
            js.append("var vegaFormatted = Utils.formatJson(" + jsonId
                      + ",3);\n");
            js.append("$('#" + jsonId + "').html(vegaFormatted);\n");
            sb.append("\n");
            sb.append(HtmlUtils.div("", HtmlUtils.attrs("id", jsonId)));
            sb.append("\n");
            sb.append(HtmlUtils.script(js.toString()));
            tabTitles.add("Vega Json");
            tabContents.add(sb.toString());
        } catch (Exception exc) {
            throw new IllegalArgumentException(exc);
        }
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Dec 8, '18
     * @author         Enter your name here...
     */
    public static class VegaRecordFile extends CsvFile {

        /** _more_ */
        private Repository repository;

        /** _more_ */
        private String dataUrl;

        /** _more_ */
        private Entry entry;

        /**
         * _more_
         *
         *
         * @param repository _more_
         * @param ctx _more_
         * @param entry _more_
         *
         * @throws IOException _more_
         */
        public VegaRecordFile(Repository repository, VegaTypeHandler ctx,
                              Entry entry, IO.Path path)
                throws IOException {
            super(path, ctx, null);
            this.repository = repository;
            this.entry      = entry;
        }



        /**
         * _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public List<String> getCsvCommands() throws Exception {

            boolean debug = false;
            List<String> fromEntry =
                (List<String>) entry.getTransientProperty("csvcommands");
            if (fromEntry != null) {
                return fromEntry;
            }
            //Check for values or for url
            if (debug) {
                System.err.println("VegaFile.getCsvCommands");
            }
            String vega = repository.getStorageManager().readSystemResource(
                              getFilename());
            JSONObject root     = new JSONObject(new JSONTokener(vega));
            JSONObject obj      = root.optJSONObject("data");
            JSONArray  array    = root.optJSONArray("data");
            boolean    haveData = false;
            dataUrl = null;
            if (obj != null) {
                JSONArray values = obj.optJSONArray("values");
                if (debug) {
                    System.err.println("\thas obj");
                }
                if (values != null) {
                    haveData = true;
                    if (debug) {
                        System.err.println("\thas data:");
                    }
                }
            } else if (array != null) {
                if (debug) {
                    System.err.println("\thas array");
                }
                for (int i = 0; i < array.length(); i++) {
                    JSONObject child = array.getJSONObject(i);
                    if (debug) {
                        System.err.println("\tchild:"
                                           + child.optString("name"));
                    }
                    String url = child.optString("url");
                    if (url != null) {
                        if (debug) {
                            System.err.println("\turl:" + url);
                        }
                        String type = JsonUtil.readValue(child,
                                          "format.type", null);
                        //If its a map the skip it
                        if ((type != null) && type.equals("topojson")) {
                            if (debug) {
                                System.err.println("\tskipping topojson");
                            }
                            continue;
                        }
                        //check for the examples url path
                        if (url.startsWith("data/")) {
                            url = "https://vega.github.io/vega/" + dataUrl;
                        }
                        if (debug) {
                            System.err.println("\thas url:" + url);
                        }
                        dataUrl = url;

                        break;
                    }
                    JSONArray values = child.optJSONArray("values");
                    if (values != null) {
                        if (debug) {
                            System.err.println("\thas values");
                        }
                        //TODO: get an example of a vega file with embedded data held in a data array
                        haveData = true;

                        break;
                    }
                }
            }

            List<String> args = new ArrayList<String>();
            if (dataUrl != null) {
                if (dataUrl.endsWith("json")) {
                    args.addAll((List<String>) Utils.makeListFromValues("-json", "",
                            "", "-addheader", "", "-print"));
                } else if (dataUrl.endsWith("csv")) {
                    args.addAll((List<String>) Utils.makeListFromValues("-addheader",
                            "", "-print"));
                } else if (dataUrl.endsWith("tsv")) {
                    args.addAll((List<String>) Utils.makeListFromValues("-tab",
                            "-addheader", "", "-print"));
                } else {
                    System.err.println("Unknown url in vega file:" + dataUrl);

                    return null;
                }
            } else if (haveData) {
                args.addAll((List<String>) Utils.makeListFromValues("-json",
                        "data.values", "*", "-addheader", "", "-print"));
            } else {
                System.err.println("Unknown data");
            }
            entry.putTransientProperty("csvcommands", args);

            return args;

        }



        /**
         * This makes the InputStream form either the entry's file (if it has embedded data)
         * or from the dataUrl that is extracted from the vega file in getCsvCommands
         *
         *
         * @param csvUtil _more_
         * @param buffered _more_
         *
         * @return _more_
         *
         *
         * @throws Exception _more_
         */
        @Override
        public InputStream doMakeInputStream(Seesv csvUtil,
                                             boolean buffered)
                throws Exception {
            if (dataUrl != null) {
                return IO.getInputStream(new URL(dataUrl));
            }
            IO.Path path = getNormalizedFilename();
            return IO.doMakeInputStream(path, buffered);
        }


    }
}
