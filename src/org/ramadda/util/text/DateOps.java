/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.text;


import org.apache.commons.codec.language.Soundex;


import org.json.*;

import org.ramadda.util.HtmlUtils;


import org.ramadda.util.IO;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.PatternProps;
import org.ramadda.util.Utils;

import org.ramadda.util.geo.Feature;
import org.ramadda.util.geo.GeoUtils;
import org.ramadda.util.geo.Place;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;

import java.net.URL;

import java.security.MessageDigest;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import java.util.regex.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import javax.script.*;



/**
 * Class description
 *
 *
 * @version        $version$, Fri, Jan 9, '15
 * @author         Jeff McWhirter
 */
@SuppressWarnings("unchecked")
public abstract class DateOps extends Processor {


    /**
     *
     */
    public DateOps() {}

    /**
     * @param col _more_
     */
    public DateOps(String col) {
        super(col);
    }

    /**
     *
     * @param cols _more_
     */
    public DateOps(List<String> cols) {
        super(cols);
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Mon, Oct 14, '19
     * @author         Enter your name here...
     */
    public static class DateConverter extends Converter {



        /**
         * @param col _more_
         */
        public DateConverter(String col) {
            super(col);
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            int col = getIndex(ctx);
            //Don't process the first row
            if (rowCnt++ == 0) {
                return row;
            }

            String s = row.get(col).toString();
            Date   d =  ctx.parseDate(s);
	    row.set(col, ctx.formatDate(d));
            return row;
        }

    }



    /**
     * Class description
     *
     *
     * @version        $version$, Mon, Oct 14, '19
     * @author         Enter your name here...
     */
    public static class DateAdder extends Converter {


	private int type;
	private String valueCol;
	private int valueIndex=-1;
	private GregorianCalendar cal;

        /**
         * @param col _more_
         */
        public DateAdder(TextReader ctx, String dateCol, String valueCol, String what) {
            super(dateCol);
	    this.valueCol = valueCol;
	    type = getDatePart(what);
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
		cal= new GregorianCalendar();
		cal.setTimeZone(ctx.getTimeZone());
		valueIndex = getIndex(ctx,valueCol);
                return row;
            }
	    int col = getIndex(ctx);

	    String s = row.getString(col);
	    Date dttm= ctx.parseDate(s);
	    int value = (int) Double.parseDouble(row.getString(valueIndex));
	    cal.setTime(dttm);
	    cal.add(type,value);
	    dttm = cal.getTime();
	    row.set(col, ctx.formatDate(dttm));
            return row;
        }

    }
    


    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Nov 19, '19
     * @author         Enter your name here...
     */
    public static class DateExtracter extends Converter {

        /** _more_ */
        private String whatLabel = "Hour";

        /** _more_ */
        private int what = GregorianCalendar.HOUR_OF_DAY;

	private GregorianCalendar cal;

        /**
         * @param col _more_
         * @param what _more_
         */
        public DateExtracter(String col,  String what) {
            super(col);
            whatLabel = StringUtil.camelCase(what);
	    this.what = getDatePart(what);
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        public Row processRow(TextReader ctx, Row row) {
            //Don't process the first row
            if (rowCnt++ == 0) {
		cal = new GregorianCalendar();
		cal.setTimeZone(ctx.getTimeZone());
                add(ctx, row, whatLabel);
                return row;
            }
            int col = getIndex(ctx);
            try {
                String            s   = row.get(col).toString();
                Date              d   = ctx.parseDate(s);
                cal.setTime(d);
		System.err.println("S:" + s+" D:" + d +" " + cal.get(what));
                String v = "NA";
                v = "" + cal.get(what);
                add(ctx, row, v);
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }

            return row;
        }

    }


    /**
     * Class description
     *
     *
     * @version        $version$, Mon, Oct 14, '19
     * @author         Enter your name here...
     */
    public static class DateBefore extends Converter {

        /* */

        /** _more_ */
        private int col;

        /* */

        /** _more_ */
        private Date date;

        /* */

        /** _more_ */
        private SimpleDateFormat sdf1;

        /**
         * @param col _more_
         * @param sdf1 _more_
         * @param date _more_
         */
        public DateBefore(int col, SimpleDateFormat sdf1, Date date) {
            this.col  = col;
            this.sdf1 = sdf1;
            this.date = date;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            //Don't process the first row
            if (rowCnt++ == 0) {
                return row;
            }
            try {
                String s = row.get(col).toString().trim();
                if (s.length() == 0) {
                    return null;
                }
                Date d = sdf1.parse(s);
                if (d.getTime() > date.getTime()) {
                    return null;
                }

                return row;
            } catch (Exception exc) {
                exc.printStackTrace();

                throw new RuntimeException(exc);
            }
        }

    }


    /**
     * Class description
     *
     *
     * @version        $version$, Mon, Oct 14, '19
     * @author         Enter your name here...
     */
    public static class DateAfter extends Converter {

        /* */

        /** _more_ */
        private int col;

        /* */

        /** _more_ */
        private Date date;

        /* */

        /** _more_ */
        private SimpleDateFormat sdf1;

        /**
         * @param col _more_
         * @param sdf1 _more_
         * @param date _more_
         */
        public DateAfter(int col, SimpleDateFormat sdf1, Date date) {
            this.col  = col;
            this.sdf1 = sdf1;
            this.date = date;
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            //Don't process the first row
            if (rowCnt++ == 0) {
                return row;
            }
            try {
                String s = row.get(col).toString().trim();
                if (s.length() == 0) {
                    return null;
                }

                Date d = sdf1.parse(s);
                if (d.getTime() < date.getTime()) {
                    return null;
                }

                return row;
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

    }





    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Jan 26, '19
     * @author         Enter your name here...
     */
    public static class DateFormatter extends Converter {

        /**
         * @param cols _more_
         */
        public DateFormatter(List<String> cols) {
            super(cols);
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            //Don't process the first row
            if (rowCnt++ == 0) {
                if ( !ctx.getAllData()) {
                    return row;
                }
            }

            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                int    index = idx.intValue();
                String value = row.getString(index);
                Date   dttm  = null;
		dttm = ctx.parseDate(value);
                String toDate = ctx.formatDate(dttm);
                row.set(index, toDate);
            }
            return row;
        }

    }


    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class Elapsed extends Converter {


        /**  */
        private Date lastDate;

        /**  */
        private int index;

        /**
         * @param col _more_
         * @param from _more_
         */
        public Elapsed(String col) {
            super(col);
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            //Don't process the first row
            if (rowCnt++ == 0) {
                index = getIndex(ctx);
                row.add("elapsed");

                return row;
            }

            try {
                Date date = ctx.parseDate(row.get(index).toString());
                if (lastDate != null) {
                    row.add(date.getTime() - lastDate.getTime());
                } else {
                    row.add(0);
                }
                lastDate = date;
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }

            return row;
        }

    }

    public static int getDatePart(String what) {
	what      = what.toUpperCase();
	if (what.equals("ERA")) {
	    return GregorianCalendar.ERA;
	} else if (what.equals("YEAR")) {
	    return GregorianCalendar.YEAR;
	} else if (what.equals("MONTH")) {
	    return GregorianCalendar.MONTH;
	} else if (what.equals("DAY_OF_MONTH")) {
	    return GregorianCalendar.DAY_OF_MONTH;
	} else if (what.equals("DAY_OF_WEEK")) {
	    return GregorianCalendar.DAY_OF_WEEK;
	} else if (what.equals("WEEK_OF_MONTH")) {
	    return GregorianCalendar.WEEK_OF_MONTH;
	} else if (what.equals("DAY_OF_WEEK_IN_MONTH")) {
	    return GregorianCalendar.DAY_OF_WEEK_IN_MONTH;
	} else if (what.equals("AM_PM")) {
	    return GregorianCalendar.AM_PM;
	} else if (what.equals("HOUR")) {
	    return GregorianCalendar.HOUR;
	} else if (what.equals("HOUR_OF_DAY")) {
	    return GregorianCalendar.HOUR_OF_DAY;
	} else if (what.equals("MINUTE")) {
	    return GregorianCalendar.MINUTE;
	} else if (what.equals("SECOND")) {
	    return GregorianCalendar.SECOND;
	} else if (what.equals("MILLISECOND")) {
	    return GregorianCalendar.MILLISECOND;
	} else {
	    throw new IllegalArgumentException("Unknown date part:" + what);
	}
    }	



}
