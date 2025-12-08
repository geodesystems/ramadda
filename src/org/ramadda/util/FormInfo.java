/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/
package org.ramadda.util;
import java.util.ArrayList;
import java.util.List;

public class FormInfo {

    private List<Constraint> constraints = new ArrayList<Constraint>();

    private String formId;

    private StringBuilder extraJS = new StringBuilder();

    private Object history;

    public FormInfo(String formId) {
        this.formId = formId;
    }

    public void appendExtraJS(String js) {
        extraJS.append(js);
        extraJS.append("\n");
    }

    public void addToForm(Appendable sb) throws Exception {
        StringBuilder validateJavascript = new StringBuilder("");
        addJavascriptValidation(validateJavascript);
        String script = JQuery.ready(JQuery.submit(JQuery.id(formId),
						   //                                                   extraJS +
						   //                                                   "event.preventDefault();return;\n" +
						   validateJavascript.toString() + "\n" + extraJS));
        HtmlUtils.script(sb, script);
    }

    public String getId() {
        return formId;
    }

    public void setHistory (Object value) {
	history = value;
    }

    public Object getHistory () {
	return history;
    }

    public List<Constraint> getConstraints() {
        return constraints;
    }

    public void addJavascriptValidation(Appendable js) throws Exception {
        for (Constraint constraint : constraints) {
            constraint.addJavascriptValidation(js);
        }
    }

    public void addMaxSizeValidation(String label, String id, int length) {
        constraints.add(new MaxLength(label, id, length));
    }

    public void addMinSizeValidation(String label, String id, int length) {
        constraints.add(new MinLength(label, id, length));
    }

    public void addRequiredValidation(String label, String id) {
        constraints.add(new Required(label, id));
    }

    public void addMinValidation(String label, String id, double min) {
        constraints.add(new Value(label, id, min, true));
    }

    public void addMaxValidation(String label, String id, double max) {
        constraints.add(new Value(label, id, max, false));
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Oct 31, '13
     * @author         Enter your name here...
     */
    public static class Constraint {

        public String label;

        public String id;

        public Constraint(String label, String id) {
            this.label = label;
            this.id    = id;
        }

        public void addJavascriptValidation(Appendable js) throws Exception {}

        public void error(Appendable js, String message) {
            Utils.append(js,
                         HtmlUtils.call("alert", HtmlUtils.squote(message)));
            Utils.append(js, "event.preventDefault();\n");
            Utils.append(js, "return;\n");
        }

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Oct 31, '13
     * @author         Enter your name here...
     */
    public static class Value extends Constraint {

        double value;

        boolean min = true;

        public Value(String label, String id, double value, boolean min) {
            super(label, id);
            this.value = value;
            this.min   = min;
        }

        @Override
        public void addJavascriptValidation(Appendable js) throws Exception {
            Utils.append(js,
                         "if(!GuiUtils.inputValueOk(" + HtmlUtils.squote(id)
                         + "," + value + "," + (min
						? "true"
						: "false") + ")) {\n");
            String message;
            if (min) {
                message = label + " is < " + value;
            } else {
                message = label + " is > " + value;
            }
            error(js, message);
            Utils.append(js, "}\n");
        }

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Oct 31, '13
     * @author         Enter your name here...
     */
    public static class MaxLength extends Constraint {

        public int length;

        public MaxLength(String label, String id, int length) {
            super(label, id);
            this.length = length;
        }

        @Override
        public void addJavascriptValidation(Appendable js) throws Exception {
            Utils.append(js,
                         "if(!GuiUtils.inputLengthOk(" + HtmlUtils.squote(id)
                         + "," + length + ")) {\n");
            String message = label + " is too long. Max length is " + length;
            error(js, message);
            Utils.append(js, "}\n");
        }

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Feb 17, '16
     * @author         Enter your name here...
     */
    public static class MinLength extends Constraint {

        public int length;

        public MinLength(String label, String id, int length) {
            super(label, id);
            this.length = length;
        }

        @Override
        public void addJavascriptValidation(Appendable js) throws Exception {
            Utils.append(js,
                         "if(!GuiUtils.inputLengthOk(" + HtmlUtils.squote(id)
                         + "," + length + ", true)) {\n");
            String message = label + " is too short. Minimum length is "
		+ length;
            error(js, message);
            Utils.append(js, "}\n");
        }

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Oct 31, '13
     * @author         Enter your name here...
     */
    public static class Required extends Constraint {

        public Required(String label, String id) {
            super(label, id);
        }

        @Override
        public void addJavascriptValidation(Appendable js) throws Exception {
            js.append("if(!GuiUtils.inputIsRequired(" + HtmlUtils.squote(id)
                      + ")) {\n");
            String message = label + " is required";
            error(js, message);
            js.append("}\n");
        }

    }

}
