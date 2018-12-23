/*
* Copyright (c) 2008-2019 Geode Systems LLC
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

// $Id: StringUtil.java,v 1.53 2007/06/01 17:02:44 jeffmc Exp $


package org.ramadda.util;


/**
 */

public class JQuery {

    /**
     * _more_
     *
     * @param selector _more_
     *
     * @return _more_
     */
    public static String id(String selector) {
        return "#" + selector;
    }


    /**
     * _more_
     *
     * @param selector _more_
     *
     * @return _more_
     */
    public static String select(String selector) {
        return "$(" + HtmlUtils.squote(selector) + ")";
    }


    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    public static String selectId(String id) {
        return select(id(id));
    }



    /**
     * _more_
     *
     * @param selector _more_
     * @param func _more_
     * @param code _more_
     *
     * @return _more_
     */
    public static String call(String selector, String func, String code) {
        return select(selector) + "." + func + "(function(event) {" + code
               + "});\n";
    }

    /**
     * _more_
     *
     * @param selector _more_
     * @param code _more_
     *
     * @return _more_
     */
    public static String submit(String selector, String code) {
        return call(selector, "submit", code);
    }

    /**
     * _more_
     *
     * @param selector _more_
     * @param code _more_
     *
     * @return _more_
     */
    public static String change(String selector, String code) {
        return call(selector, "change", code);
    }

    /**
     * _more_
     *
     * @param selector _more_
     * @param code _more_
     *
     * @return _more_
     */
    public static String click(String selector, String code) {
        return call(selector, "click", code);
    }

    /**
     * _more_
     *
     * @param label _more_
     * @param id _more_
     * @param js _more_
     * @param code _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String button(String label, String id, Appendable js,
                                String code)
            throws Exception {
        String html = HtmlUtils.tag("button", HtmlUtils.id(id), label);
        js.append(
            JQuery.select(JQuery.id(id))
            + ".button().click(function(event){event.preventDefault();\n"
            + code + "\n});\n");

        return html;
    }

    /**
     * _more_
     *
     * @param label _more_
     * @param id _more_
     * @param js _more_
     * @param code _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String makeButton(String label, String id, Appendable js,
                                    String code)
            throws Exception {
        String html = HtmlUtils.tag("button", HtmlUtils.id(id), label);
        js.append(JQuery.select(JQuery.id(id))
                  + ".button().click(function(event){\n" + code + "\n});\n");

        return html;
    }

    /**
     * _more_
     *
     * @param selector _more_
     *
     * @return _more_
     */
    public static String buttonize(String selector) {
        return JQuery.select(selector)
               + ".button().click(function(event){});\n";
    }


    /**
     * _more_
     *
     * @param js _more_
     *
     * @return _more_
     */
    public static String ready(String js) {
        return "$(document).ready(function(){\n" + js + "\n});\n";

    }

}
