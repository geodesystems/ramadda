/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/
package org.ramadda.util;
import java.util.ArrayList;
import java.util.List;

public class FormInfo {

    /** _more_ */
    private List<Constraint> constraints = new ArrayList<Constraint>();

    /** _more_ */
    private String formId;

    /** _more_ */
    private StringBuilder extraJS = new StringBuilder();

    private Object history;

    /**
     * _more_
     *
     * @param formId _more_
     */
    public FormInfo(String formId) {
        this.formId = formId;
    }



    /**
     * _more_
     *
     * @param js _more_
     */
    public void appendExtraJS(String js) {
        extraJS.append(js);
        extraJS.append("\n");
    }

    /**
     * _more_
     *
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void addToForm(Appendable sb) throws Exception {
        StringBuilder validateJavascript = new StringBuilder("");
        addJavascriptValidation(validateJavascript);
        String script = JQuery.ready(JQuery.submit(JQuery.id(formId),
						   //                                                   extraJS +
						   //                                                   "event.preventDefault();return;\n" +
						   validateJavascript.toString() + "\n" + extraJS));
        HtmlUtils.script(sb, script);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getId() {
        return formId;
    }


    /**
       Set the History property.

       @param value The new value for History
    **/
    public void setHistory (Object value) {
	history = value;
    }

    /**
       Get the History property.

       @return The History
    **/
    public Object getHistory () {
	return history;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public List<Constraint> getConstraints() {
        return constraints;
    }

    /**
     * _more_
     *
     * @param js _more_
     *
     * @throws Exception _more_
     */
    public void addJavascriptValidation(Appendable js) throws Exception {
        for (Constraint constraint : constraints) {
            constraint.addJavascriptValidation(js);
        }
    }

    /**
     * _more_
     *
     * @param label _more_
     * @param id _more_
     * @param length _more_
     */
    public void addMaxSizeValidation(String label, String id, int length) {
        constraints.add(new MaxLength(label, id, length));
    }

    /**
     * _more_
     *
     * @param label _more_
     * @param id _more_
     * @param length _more_
     */
    public void addMinSizeValidation(String label, String id, int length) {
        constraints.add(new MinLength(label, id, length));
    }


    /**
     * _more_
     *
     * @param label _more_
     * @param id _more_
     */
    public void addRequiredValidation(String label, String id) {
        constraints.add(new Required(label, id));
    }


    /**
     * _more_
     *
     * @param label _more_
     * @param id _more_
     * @param min _more_
     */
    public void addMinValidation(String label, String id, double min) {
        constraints.add(new Value(label, id, min, true));
    }

    /**
     * _more_
     *
     * @param label _more_
     * @param id _more_
     * @param max _more_
     */
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

        /** _more_ */
        public String label;

        /** _more_ */
        public String id;

        /**
         * _more_
         *
         * @param label _more_
         * @param id _more_
         */
        public Constraint(String label, String id) {
            this.label = label;
            this.id    = id;
        }

        /**
         * _more_
         *
         * @param js _more_
         *
         * @throws Exception _more_
         */
        public void addJavascriptValidation(Appendable js) throws Exception {}

        /**
         * _more_
         *
         * @param js _more_
         * @param message _more_
         */
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

        /** _more_ */
        double value;

        /** _more_ */
        boolean min = true;

        /**
         * _more_
         *
         * @param label _more_
         * @param id _more_
         * @param value _more_
         * @param min _more_
         */
        public Value(String label, String id, double value, boolean min) {
            super(label, id);
            this.value = value;
            this.min   = min;
        }

        /**
         * _more_
         *
         * @param js _more_
         *
         * @throws Exception _more_
         */
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

        /** _more_ */
        public int length;

        /**
         * _more_
         *
         * @param label _more_
         * @param id _more_
         * @param length _more_
         */
        public MaxLength(String label, String id, int length) {
            super(label, id);
            this.length = length;
        }

        /**
         * _more_
         *
         * @param js _more_
         *
         * @throws Exception _more_
         */
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

        /** _more_ */
        public int length;

        /**
         * _more_
         *
         * @param label _more_
         * @param id _more_
         * @param length _more_
         */
        public MinLength(String label, String id, int length) {
            super(label, id);
            this.length = length;
        }

        /**
         * _more_
         *
         * @param js _more_
         *
         * @throws Exception _more_
         */
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

        /**
         * _more_
         *
         * @param label _more_
         * @param id _more_
         */
        public Required(String label, String id) {
            super(label, id);
        }

        /**
         * _more_
         *
         * @param js _more_
         *
         * @throws Exception _more_
         */
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
