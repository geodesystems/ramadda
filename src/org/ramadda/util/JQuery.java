/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;

public class JQuery {

    public static String id(String selector) {
        return "#" + selector;
    }

    public static String select(String selector) {
        return "$(" + HtmlUtils.squote(selector) + ")";
    }

    public static String selectId(String id) {
        return select(id(id));
    }

    public static String call(String selector, String func, String code) {
        return select(selector) + "." + func + "(function(event) {" + code
               + "});\n";
    }

    public static String submit(String selector, String code) {
        return call(selector, "submit", code);
    }

    public static String change(String selector, String code) {
        return call(selector, "change", code);
    }

    public static String click(String selector, String code) {
        return call(selector, "click", code);
    }

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

    public static String makeButton(String label, String id, Appendable js,
                                    String code)
            throws Exception {
        String html = HtmlUtils.tag("button", HtmlUtils.id(id), label);
        js.append(JQuery.select(JQuery.id(id))
                  + ".button().click(function(event){\n" + code + "\n});\n");

        return html;
    }

    public static String buttonize(String selector) {
        return JQuery.select(selector)
               + ".button().click(function(event){});\n";
    }

    public static String ready(String js) {
        return "$(document).ready(function(){\n" + js + "\n});\n";

    }

}
