/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.util;

import org.ramadda.repository.Constants;

/**
 * Holds the URL arguments for date range selection
 */
public class DateArgument implements Constants {
    public static final int TYPE_CREATE = 0;
    public static final int TYPE_CHANGE = 1;
    public static final int TYPE_DATA = 2;
    public static final DateArgument ARG_DATA = new DateArgument(TYPE_DATA,ARG_DATA_DATE, "Date");
    public static final DateArgument ARG_CREATE =
        new DateArgument(TYPE_CREATE, ARG_CREATE_DATE, "Create Date");
    public static final DateArgument ARG_CHANGE =
        new DateArgument(TYPE_CHANGE, ARG_CHANGE_DATE, "Change Date");
    public static final DateArgument[] SEARCH_ARGS = { ARG_DATA, ARG_CREATE,
            ARG_CHANGE };

    private int type;
    private String label;
    private String from;
    private String to;
    private String mode;
    private String relative;

    public DateArgument(int type, String suffix, String label) {
        this.type  = type;
        this.label = label;
        from       = suffix + ".from";
        to         = suffix + ".to";
        mode       = suffix + ".mode";
        relative   = suffix + ".relative";
    }

    public DateArgument(int type, String from, String to, String mode,
                        String relative) {
        this.type     = type;
        this.label    = from;
        this.from     = from;
        this.to       = to;
        this.mode     = mode;
        this.relative = relative;
    }

    public boolean equals(Object o) {
        if ( !(o instanceof DateArgument)) {
            return false;
        }
        DateArgument that = (DateArgument) o;

        return (this.type == that.type) && this.from.equals(that.from)
               && this.to.equals(that.to);
    }

    public boolean forCreateDate() {
        return type == TYPE_CREATE;
    }

    public boolean forChangeDate() {
        return type == TYPE_CHANGE;
    }

    public boolean forDataDate() {
        return type == TYPE_DATA;
    }

    public String getFromArg() {
        return from;
    }

    public String getToArg() {
        return to;
    }

    public int getType() {
        return type;
    }

    public boolean getHasRange() {
        return forDataDate();
    }

    public String getLabel() {
        return label;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getMode() {
        return mode;
    }


    public String getRelative() {
        return relative;
    }

}
