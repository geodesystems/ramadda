/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.seesv;

import org.ramadda.util.Utils;

import ucar.unidata.util.StringUtil;
import java.io.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;


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
            if (rowCnt++ == 0) {
                return row;
            }
            int col = getIndex(ctx);
            String s = row.get(col).toString();
	    if(Utils.stringDefined(s)) {
		Date  d = ctx.parseDate(s);
		row.set(col, ctx.formatDate(d));
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
    public static class DateAdder extends Converter {


        /**  */
        private int type;

        /**  */
        private String valueCol;

        /**  */
        private int valueIndex = -1;

        /**  */
        private GregorianCalendar cal;

        /**
         * @param ctx _more_
         * @param dateCol _more_
         * @param valueCol _more_
         * @param what _more_
         */
        public DateAdder(TextReader ctx, String dateCol, String valueCol,
                         String what) {
            super(dateCol);
            this.valueCol = valueCol;
            type          = getDatePart(what);
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                cal = new GregorianCalendar();
                cal.setTimeZone(ctx.getTimeZone());
                valueIndex = getIndex(ctx, valueCol);
                return row;
            }
            int    col   = getIndex(ctx);

            String s     = row.getString(col);
            Date   dttm  = ctx.parseDate(s);
            int    value =
                (int) Double.parseDouble(row.getString(valueIndex));
            cal.setTime(dttm);
            cal.add(type, value);
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

        /**  */
        private GregorianCalendar cal;

        /**
         * @param col _more_
         * @param what _more_
         */
        public DateExtracter(String col, String what) {
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
                String s = row.get(col).toString();
                Date   d = ctx.parseDate(s);
                cal.setTime(d);
                String v =  "" + cal.get(what);
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


    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class Diff extends Converter {


        /**  */
        private int index1;
        private int index2;	
	private String col1;
	private String col2;
	private String unit;
	private double divisor;

        /**
         * @param col _more_
         */
        public Diff(String col1,String col2, String unit) {
            super();
	    this.col1= col1;
	    this.col2= col2;
	    this.unit = unit;
	    if(unit.equals("milliseconds")) {
		divisor = 1;
	    } else if(unit.equals("seconds")) {
		divisor = 1000;
	    } else if(unit.equals("minutes")) {
		divisor = 60*1000;
	    } else if(unit.equals("hours")) {
		divisor = 60*60*1000;
	    } else if(unit.equals("days")) {
		divisor = 60*60*24*1000;				
	    } else {
		throw new IllegalArgumentException("-datediff: unknown unit:" + unit);
	    }
	    System.err.println(divisor);
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
		index1 = getIndex(ctx,col1);
		index2 = getIndex(ctx,col2);		
                row.add(Utils.makeLabel(unit));
                return row;
            }

            try {
                Date date1 = ctx.parseDate(row.get(index1).toString());
                Date date2 = ctx.parseDate(row.get(index2).toString());		
		double diff = (double)(date1.getTime()-date2.getTime());
		double value = diff/divisor;
		value = Utils.decimals(value,2);
		if(value==(int)value)
		    row.add(((int)value)+"");
		else
		    row.add(value+"");
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }

            return row;
        }

    }

    
    public static class DateFormatSetter extends Processor {
	boolean in;
	Seesv.Dater dater;

	public DateFormatSetter(boolean in,Seesv.Dater dater) {
	    this.in = in;
	    this.dater = dater;
	}

        public Row processRow(TextReader ctx, Row row) {
	    if(in) ctx.setInDater(dater);
	    else ctx.setOutDater(dater);	    
	    return row;
	}



    }


    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Jan 16, '15
     * @author         Enter your name here...
     */
    public static class CompareDate extends Converter {

        /** _more_ */
        private String op;

	private String scol1;
	private String scol2;
	private int col1;
	private int col2;	


        /**
         * @param indices _more_
         * @param name _more_
         * @param op _more_
         */
        public CompareDate(String col1, String col2, String op) {
	    this.scol1=  col1;
	    this.scol2=  col2;	    
            this.op   = op;
        }


        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
		col1 = getIndex(ctx, scol1);
		col2 = getIndex(ctx, scol2);		
		row.add(row.getString(col1) +" " + op +" " + row.getString(col2));
                return row;
            }
	    if(col1>= row.size()) return row;
	    if(col2>= row.size()) return row;	    
            Date dvalue1 = ctx.parseDate(row.getString(col1));
            Date dvalue2 = ctx.parseDate(row.getString(col2));	    
	    if(dvalue1==null || dvalue2==null) {
		row.add("NA");
		return row;
	    }
	    long value1 = dvalue1.getTime();
	    long value2 = dvalue2.getTime();	    
	    boolean value = true;
	    if(op.equals("<")) value  = value1 < value2;
	    else if(op.equals("<=")) value  = value1 <= value2;
	    else if(op.equals("=")) value  = value1 == value2;
	    else if(op.equals("!=")) value  = value1 != value2;	    
	    else if(op.equals(">")) value  = value1 > value2;
	    else if(op.equals(">=")) value  = value1 >= value2;    	    
	    else fatal(ctx,"Unknown operator:" + op);
            row.getValues().add(value + "");
            return row;
        }

    }
    




    /**
     *
     * @param what _more_
      * @return _more_
     */
    public static int getDatePart(String what) {
        what = what.toUpperCase();
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