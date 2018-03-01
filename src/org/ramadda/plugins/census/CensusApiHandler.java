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

package org.ramadda.plugins.census;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.harvester.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.Place;

import org.ramadda.util.Utils;

import org.w3c.dom.*;

import ucar.unidata.ui.HttpFormEntry;

import ucar.unidata.util.IOUtil;


import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;


import java.net.*;

import java.util.ArrayList;

import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Provides a top-level API
 *
 */
public class CensusApiHandler extends RepositoryManager implements RequestHandler {

    /**
     *     ctor
     *
     *     @param repository the repository
     *
     *     @throws Exception on badness
     */
    public CensusApiHandler(Repository repository) throws Exception {
        super(repository);
    }




    /**
     * handle the request
     *
     * @param request request
     *
     * @return result
     *
     * @throws Exception on badness
     */
    public Result processVariableApi(Request request) throws Exception {
        boolean asJson = request.getString(ARG_OUTPUT, "").equals("json");
        String               text    = request.getString("text", "");

        List<CensusVariable> matches = new ArrayList<CensusVariable>();
        if (Utils.stringDefined(text)) {
            matches = processSearch(request, text);
        }

        int cnt = 0;
        if (asJson) {
            List<String> objs = new ArrayList<String>();
            for (CensusVariable var : matches) {
                objs.add(Json.mapAndQuote("id", var.getId(), "label",
                                          var.getLabel(), "concept",
                                          var.getConcept()));
                if (cnt++ > 500) {
                    break;
                }
            }
            String json = Json.list(objs);

            return new Result("", new StringBuilder(json), Json.MIMETYPE);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(HtmlUtils.sectionOpen("RAMADDA Census Variables"));
        sb.append(HtmlUtils.form(""));
        sb.append(HtmlUtils.input("text", text.replaceAll("\"", "&quot;"),
                                  80));
        sb.append(HtmlUtils.space(2));
        sb.append(HtmlUtils.formClose());

        sb.append(
            HtmlUtils.importCss(
                ".ramadda-census-table {margin-left:10px;margin-right:10px;}\n"));

        for (CensusVariable var : matches) {
            if (cnt == 0) {
                sb.append(
                    "<table border=1><tr><td><div class=ramadda-census-table><b>ID</b></div></td><td><div class=ramadda-census-table><b>Label</b></div></td><td><div class=ramadda-census-table><b>Concept</b></div></td>");
            }
            cnt++;
            String label = var.getLabel();
            label = label.replaceAll("!!", "<br>&nbsp;&nbsp;");
            String cellClass = HtmlUtils.cssClass("ramadda-census-table");
            String row = HtmlUtils.cols(
                             HtmlUtils.div(
                                 var.getId() + "&nbsp;&nbsp;",
                                 cellClass), HtmlUtils.div(
                                     label + "&nbsp;&nbsp;",
                                     cellClass), HtmlUtils.div(
                                         var.getConcept(), cellClass));
            sb.append(HtmlUtils.rowTop(row));
            if (cnt > 1000) {
                break;
            }
        }
        if (cnt > 0) {
            sb.append("</table>");
        } else {
            sb.append("No match");
        }
        sb.append(HtmlUtils.sectionClose());

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
    public Result processStatesApi(Request request) throws Exception {
        String        stateId = request.getString("state", (String) null);
        StringBuilder sb      = new StringBuilder();

        List<Place>   places;
        if (stateId != null) {
            Place state = Place.getPlace(stateId);
            sb.append(HtmlUtils.sectionOpen("Census Counties List for "
                                            + state.getName()));
            places = new ArrayList<Place>();
            for (Place county : Place.getPlacesFrom("counties.txt")) {
                if (county.getFips().startsWith(state.getFips())) {
                    places.add(county);
                }
            }

        } else {
            sb.append(HtmlUtils.sectionOpen("Census States List"));
            places = Place.getPlacesFrom("states.txt");
        }
        sb.append(HtmlUtils.formTable());
        sb.append(HtmlUtils.row(HtmlUtils.cols("Place", "ID", "FIPS")));
        for (Place place : places) {
            String name = place.getName();
            if (stateId == null) {
                name = HtmlUtils.href(getRepository().getUrlBase()
                                      + "/census/states?state="
                                      + place.getId(), name);
            }
            sb.append(HtmlUtils.row(HtmlUtils.cols(name, place.getId(),
                    place.getFips())));
        }
        sb.append(HtmlUtils.formTableClose());

        sb.append(HtmlUtils.sectionClose());

        return new Result("", sb);
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        for (String arg : args) {
            for (CensusVariable var : processSearch(null, arg)) {
                System.err.println("Var:" + var.getId() + " :: "
                                   + var.getLabel() + " :: "
                                   + var.getConcept());
            }
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param text _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private static List<CensusVariable> processSearch(Request request,
            String text)
            throws Exception {
        text = text.trim();
        List<CensusVariable> vars    = CensusVariable.getVariables();
        List<CensusVariable> matches = new ArrayList<CensusVariable>();
        for (String searchString : StringUtil.split(text, ",", true, true)) {
            matches = processSearchInner(request, searchString, vars);
            System.err.println("Looked at:" + vars.size() + " for:"
                               + searchString + "   found: "
                               + matches.size());
            vars = matches;
        }

        return matches;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param searchString _more_
     * @param vars _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private static List<CensusVariable> processSearchInner(Request request,
            String searchString, List<CensusVariable> vars)
            throws Exception {

        searchString = searchString.trim();
        List<CensusVariable> matches = new ArrayList<CensusVariable>();
        List<String>         toks    = new ArrayList<String>();
        List<Boolean>        nots    = new ArrayList<Boolean>();
        String prefix = StringUtil.findPattern(searchString,
                            "^(id|concept|label|category|cid|group):.*");
        boolean justConcept   = false;
        boolean justConceptId = false;
        boolean justId        = false;
        boolean justLabel     = false;
        if (prefix != null) {
            searchString = searchString.substring(prefix.length() + 1);
            System.err.println("prefix:" + prefix + " string:"
                               + searchString);
            if (prefix.equals("concept")) {
                justConcept = true;
            } else if (prefix.equals("cid")) {
                justConceptId = true;
            } else if (prefix.equals("id")) {
                justId = true;
            } else if (prefix.equals("label")) {
                justLabel = true;
            }
        }
        if (searchString.startsWith("\"") && searchString.endsWith("\"")) {
            searchString = searchString.replaceAll("\"", "");
            toks.add(searchString);
            nots.add(new Boolean(false));
        } else {
            for (String tok :
                    StringUtil.split(searchString, " ", true, true)) {
                boolean isNot = tok.startsWith("!");
                nots.add(new Boolean(isNot));
                if (isNot) {
                    tok = tok.substring(1);
                }
                toks.add(tok.toLowerCase());
            }
        }

        List<Pattern> patterns = new ArrayList<Pattern>();

        for (int i = 0; i < toks.size(); i++) {
            String tok = toks.get(i);




            if ( !StringUtil.containsRegExp(tok)) {
                patterns.add(null);
            } else {
                Pattern p = Pattern.compile(tok);
                patterns.add(p);
            }
        }


        int cnt = 0;
        for (CensusVariable var : vars) {
            String corpus = null;
            if (justConcept) {
                corpus = var.getConcept().toLowerCase();
            } else if (justConceptId) {
                corpus = var.getConceptId().toLowerCase();
            } else if (justId) {
                corpus = var.getId().toLowerCase();
            } else if (justLabel) {
                corpus = var.getLabel().toLowerCase();
            } else {
                corpus = var.getCorpus().toLowerCase();
            }


            boolean ok = true;
            for (int i = 0; (i < patterns.size()) && ok; i++) {
                String  tok   = toks.get(i);
                boolean isNot = nots.get(i);
                Pattern p     = patterns.get(i);
                if (p == null) {
                    if (corpus.indexOf(tok) < 0) {
                        ok = (isNot
                              ? true
                              : false);
                    }

                    continue;
                }
                ok = p.matcher(tok).matches();
                if (isNot) {
                    ok = !ok;
                }
            }
            if ( !ok) {
                continue;
            }
            matches.add(var);
            //            if(matches.size()>10) return;
        }

        return matches;

    }
}
