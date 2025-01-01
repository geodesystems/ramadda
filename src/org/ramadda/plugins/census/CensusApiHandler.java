/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.census;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.harvester.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;

import org.ramadda.util.Utils;
import org.ramadda.util.geo.GeoResource;
import org.ramadda.util.geo.Place;

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

    private static final String NAME_TEXT = "text";
    private static final String NAME_IGNORE = "ignore";    

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
        boolean asJson = request.getSanitizedString(ARG_OUTPUT, "").equals("json");
        String               text    = request.getSanitizedString(NAME_TEXT, "");
        List<CensusVariable> matches = new ArrayList<CensusVariable>();
	String  ignore = request.getSanitizedString(NAME_IGNORE,"margin of error");
        if (Utils.stringDefined(text)) {
            matches = processSearch(request, text,ignore);
        }

        int cnt = 0;
        if (asJson) {
            List<String> objs = new ArrayList<String>();
            for (CensusVariable var : matches) {
                objs.add(JsonUtil.mapAndQuote(Utils.makeListFromValues("id", var.getId(), "label",
							     var.getLabel(), "concept",
							     var.getConcept())));
                if (cnt++ > 500) {
                    break;
                }
            }
            String json = JsonUtil.list(objs);

            return new Result("", new StringBuilder(json), JsonUtil.MIMETYPE);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(
            HtmlUtils.importCss(
                ".ramadda-census-table {margin-left:10px;margin-right:10px;}\n"));



        getPageHandler().sectionOpen(request, sb, "RAMADDA Census Variables",false);
	sb.append(HU.center(HU.href(getRepository().getUrlBase() +"/census/states","View States List")));
	sb.append(HU.br());
        sb.append(HtmlUtils.form(""));
	sb.append(HU.submit("Search","search"));
	sb.append(HU.space(2));
        sb.append(HtmlUtils.input(NAME_TEXT, text.replaceAll("\"", "&quot;"),
                                  HU.attr("size","40")+HU.attr("placeholder","Search term")));
        sb.append(HtmlUtils.space(2));
        sb.append(HU.b("Ignore: ") + HtmlUtils.input(NAME_IGNORE, ignore,
						     HU.attr("size","40")));
        sb.append(HtmlUtils.formClose());
        StringBuilder table = new StringBuilder();
        for (CensusVariable var : matches) {
            String label = var.getLabel();


            if (cnt == 0) {
		table.append("<table table-height='400px' class='stripe ramadda-table' table-ordering=true><thead>");
		table.append("<tr><th>ID</th><th>Label</th><th>Concept</th></thead><tbody>");
            }
            cnt++;
	    



            label = label.replaceAll("!!", "<br>&nbsp;&nbsp;");
            String cellClass = HtmlUtils.cssClass("ramadda-census-table");
            String row = HtmlUtils.cols(
                             HtmlUtils.div(
                                 var.getId() + "&nbsp;&nbsp;",
                                 cellClass), HtmlUtils.div(
                                     label + "&nbsp;&nbsp;",
                                     cellClass), HtmlUtils.div(
                                         var.getConcept(), cellClass));
            table.append(HtmlUtils.rowTop(row));
            if (cnt >= 2000) {
                break;
            }
        }
        if (cnt > 0) {
            table.append("</tbody></table>");
	    sb.append(cnt +" matches<br>");
	    sb.append(table);
        } else {
	    if (Utils.stringDefined(text)) 
		sb.append("No match");
        }
        getPageHandler().sectionClose(request, sb);
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
        String        stateId = request.getSanitizedString("state", null);
        StringBuilder sb      = new StringBuilder();

        List<Place>   places  = null;
        if (stateId != null) {
            Place state = GeoResource.RESOURCE_STATES.getPlace(stateId);
            if (state == null) {
		getPageHandler().sectionOpen(request, sb, "Census Counties List",false);
                sb.append(getPageHandler().showDialogError("Unknown state:" + stateId));
            } else {
		getPageHandler().sectionOpen(request, sb, "Census Counties List for "+ state.getName(),false);
                places = new ArrayList<Place>();
                for (Place county :
                        GeoResource.RESOURCE_COUNTIES.getPlaces()) {
                    if (county.getFips().startsWith(state.getFips())) {
                        places.add(county);
                    }
                }
            }
        } else {
	    getPageHandler().sectionOpen(request, sb, "Census States List",false);
            places = GeoResource.RESOURCE_STATES.getPlaces();
        }
	sb.append(HU.center(HU.href(getRepository().getUrlBase() +"/census/variables","View Variables Form")
));
	sb.append(HU.br());
        if (places != null) {
	    Collections.sort(places);

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
        }
	getPageHandler().sectionClose(request, sb);
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
            for (CensusVariable var : processSearch(null, arg,null)) {
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
						      String text,String ignore)
            throws Exception {
        text = text.trim();
        List<CensusVariable> vars    = CensusVariable.getVariables();
        List<CensusVariable> matches = new ArrayList<CensusVariable>();
        for (String searchString : StringUtil.split(text, ",", true, true)) {
            matches = processSearchInner(request, searchString,ignore, vars);
	    /*
            System.err.println("Looked at:" + vars.size() + " for:"
                               + searchString + "   found: "
                               + matches.size());
	    */
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
							   String searchString, String ignore,List<CensusVariable> vars)
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
            nots.add(Boolean.valueOf(false));
        } else {
            for (String tok :
                    StringUtil.split(searchString, " ", true, true)) {
                boolean isNot = tok.startsWith("!");
                nots.add(Boolean.valueOf(isNot));
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

	    if(Utils.stringDefined(ignore)) {
		if(corpus.indexOf(ignore)>=0) continue;
		try {
		    if(corpus.matches(ignore)) continue;
		} catch(Exception badRegexp) {}
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
