/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.util;


import org.ramadda.repository.Constants;


/**
 * Holds the URL arguments for date range selection
 */
public class DateArgument implements Constants {


    /** _more_ */
    public static final int TYPE_CREATE = 0;

    /** _more_ */
    public static final int TYPE_CHANGE = 1;

    /** _more_ */
    public static final int TYPE_DATA = 2;

    /** _more_ */
    public static final DateArgument ARG_DATA = new DateArgument(TYPE_DATA,ARG_DATA_DATE, "Date");

    /** _more_ */
    public static final DateArgument ARG_CREATE =
        new DateArgument(TYPE_CREATE, ARG_CREATE_DATE, "Create Date");

    /** _more_ */
    public static final DateArgument ARG_CHANGE =
        new DateArgument(TYPE_CHANGE, ARG_CHANGE_DATE, "Change Date");

    /** _more_ */
    public static final DateArgument[] SEARCH_ARGS = { ARG_DATA, ARG_CREATE,
            ARG_CHANGE };


    /** _more_ */
    private int type;

    /** _more_ */
    private String label;

    /** _more_ */
    private String from;

    /** _more_ */
    private String to;

    /** _more_ */
    private String mode;

    /** _more_ */
    private String relative;

    /**
     * _more_
     *
     *
     * @param type _more_
     * @param suffix _more_
     * @param label _more_
     */
    public DateArgument(int type, String suffix, String label) {
        this.type  = type;
        this.label = label;
        from       = suffix + ".from";
        to         = suffix + ".to";
        mode       = suffix + ".mode";
        relative   = suffix + ".relative";
    }


    /**
     * _more_
     *
     * @param type _more_
     * @param from _more_
     * @param to _more_
     * @param mode _more_
     * @param relative _more_
     */
    public DateArgument(int type, String from, String to, String mode,
                        String relative) {
        this.type     = type;
        this.label    = from;
        this.from     = from;
        this.to       = to;
        this.mode     = mode;
        this.relative = relative;
    }

    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    public boolean equals(Object o) {
        if ( !(o instanceof DateArgument)) {
            return false;
        }
        DateArgument that = (DateArgument) o;

        return (this.type == that.type) && this.from.equals(that.from)
               && this.to.equals(that.to);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean forCreateDate() {
        return type == TYPE_CREATE;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean forChangeDate() {
        return type == TYPE_CHANGE;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean forDataDate() {
        return type == TYPE_DATA;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public String getFromArg() {
        return from;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getToArg() {
        return to;
    }



    /**
     *  Get the Type property.
     *
     *  @return The Type
     */
    public int getType() {
        return type;
    }



    /**
     *  Get the HasRange property.
     *
     *  @return The HasRange
     */
    public boolean getHasRange() {
        return forDataDate();
    }

    /**
     *  Get the Label property.
     *
     *  @return The Label
     */
    public String getLabel() {
        return label;
    }


    /**
     *  Get the From property.
     *
     *  @return The From
     */
    public String getFrom() {
        return from;
    }


    /**
     *  Get the To property.
     *
     *  @return The To
     */
    public String getTo() {
        return to;
    }


    /**
     *  Get the Mode property.
     *
     *  @return The Mode
     */
    public String getMode() {
        return mode;
    }



    /**
     *  Get the Relative property.
     *
     *  @return The Relative
     */
    public String getRelative() {
        return relative;
    }



}
