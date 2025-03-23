/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/
// Copyright (c) 2008-2025 Geode Systems LLC
// SPDX-License-Identifier: Apache-2.0

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
