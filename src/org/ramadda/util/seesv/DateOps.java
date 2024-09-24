/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.seesv;

import org.ramadda.util.Utils;

import ucar.unidata.util.StringUtil;
import java.io.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
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


    public static final int[]DATE_COMPONENTS = {
	Calendar.MILLISECOND,
	Calendar.SECOND, 
	Calendar.MINUTE,
	Calendar.HOUR_OF_DAY,
	Calendar.DAY_OF_WEEK,
	Calendar.DAY_OF_MONTH,
	Calendar.MONTH};


    private static int OFFSET_BASE = -1000;
    private static int OFFSET_IDX = OFFSET_BASE;
    private static final int OFFSET_DAYS_IN_YEAR = OFFSET_IDX--;
    private static final int OFFSET_HOURS_IN_YEAR = OFFSET_IDX--;    
    private static final int OFFSET_MINUTES_IN_YEAR = OFFSET_IDX--;
    private static final int OFFSET_SECONDS_IN_YEAR = OFFSET_IDX--;
    private static final int DECADE= OFFSET_IDX--;        



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
        public DateConverter(List<String> cols) {
            super(cols);
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
            List<Integer> indices = getIndices(ctx);
            for (Integer col : indices) {
		if(!row.indexOk(col)) continue;
		String s = row.get(col).toString();
		if(Utils.stringDefined(s)) {
		    Date  d = ctx.parseDate(s);
		    row.set(col, ctx.formatDate(d));
		}
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
     * @version        $version$, Mon, Oct 14, '19
     * @author         Enter your name here...
     */
    public static class DateClear extends Converter {


        /**  */
        private int type;

        /**  */
        private GregorianCalendar cal;

        /**
         * @param ctx _more_
         * @param dateCol _more_
         * @param valueCol _more_
         * @param what _more_
         */
        public DateClear(TextReader ctx, String dateCol, 
                         String what) {
            super(dateCol);
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
                return row;
            }
            int    col   = getIndex(ctx);
            String s     = row.getString(col);
            Date   dttm  = ctx.parseDate(s);
            cal.setTime(dttm);
	    
	    for(int comp: DATE_COMPONENTS) {
		if(comp==Calendar.YEAR)
		    cal.set(Calendar.MONTH, Calendar.JANUARY);
		else if (comp==Calendar.MONTH)
		    cal.set(Calendar.MONTH, Calendar.JANUARY);
		else if(comp==Calendar.DAY_OF_MONTH)
		    cal.set(Calendar.DAY_OF_MONTH, 1);
		else
		    cal.set(comp,0);
		if(comp==type) break;
	    }

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
	    if(this.what == OFFSET_DAYS_IN_YEAR)
		whatLabel="Days in year";
	    else if(this.what == DECADE)
		whatLabel="Decade";	    
	    else  if(this.what == OFFSET_HOURS_IN_YEAR)
		whatLabel="Hours in year";
	    else if(this.what == OFFSET_MINUTES_IN_YEAR)
		whatLabel="Minutes in year";
	    else if(this.what == OFFSET_SECONDS_IN_YEAR)
		whatLabel="Seconds in year";	    	    
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
		if(this.what==DECADE) {
		    cal.setTime(d);
		    int year = cal.get(Calendar.YEAR);
		    int decade = (year / 10) * 10;
		    add(ctx, row, decade+"s");
		    return row;
		}

		if(this.what<=OFFSET_BASE) {
		    Date start = Utils.getStartOfYear(d);
		    long ms = d.getTime() - start.getTime();
		    long v=0;
		    if(this.what == OFFSET_DAYS_IN_YEAR) {
			v = ms / (1000 * 60 * 60*24);
		    }  else if(this.what == OFFSET_HOURS_IN_YEAR) {
			v = ms / (1000 * 60 * 60);
		    }   else if(this.what == OFFSET_MINUTES_IN_YEAR) {
			v = ms / (1000 * 60);
		    }   else if(this.what == OFFSET_SECONDS_IN_YEAR) {
			v = ms / (1000);
		    }
		    add(ctx, row, v+"");
		    return row;
		}
                cal.setTime(d);
                String v =  "" + cal.get(what);
                add(ctx, row, v);
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }

            return row;
        }

    }

    public static class FormatDateOffset extends Converter {

        /** _more_ */
        private int what = GregorianCalendar.HOUR_OF_DAY;
	private String name;


	private SimpleDateFormat sdf;
	private Date start;


        /**
         * @param col _more_
         * @param what _more_
         */
        public FormatDateOffset(String col, String what) {
            super(col);
	    start = Utils.getStartOfYear(new Date());
            this.what = getDatePart(what);
	    if(this.what == OFFSET_DAYS_IN_YEAR) {
		sdf = Utils.makeDateFormat("MMMM-dd");
		name="Month-Day";
	    }   else  if(this.what == OFFSET_HOURS_IN_YEAR) {
		sdf = Utils.makeDateFormat("MMMM-dd HH:mm");
		name="Month-Day-Hour";
	    }  else if(this.what == OFFSET_MINUTES_IN_YEAR) {
		sdf = Utils.makeDateFormat("MMMM-dd HH:mm");
		name="Month-Day-Hour-Minute";
	    }   else if(this.what == OFFSET_SECONDS_IN_YEAR) {
		sdf = Utils.makeDateFormat("MMMM-dd HH:mm:ss");
		name="Month-Day-Hour";
	    }	else {
		name="Month-Day-Hour";
		sdf = Utils.makeDateFormat("MMMM-dd HH:mm:ss");
	    }		
        }

        /**
         * @param ctx _more_
         * @param row _more_
         * @return _more_
         */
        public Row processRow(TextReader ctx, Row row) {
            //Don't process the first row
            if (rowCnt++ == 0) {
                add(ctx, row, name);
                return row;
            }
            int col = getIndex(ctx);
            try {
                int offset = Integer.parseInt(row.get(col).toString());
		long v=0;
		if(this.what == OFFSET_DAYS_IN_YEAR) {
		    v = Utils.daysToMillis(offset);
		}  else if(this.what == OFFSET_HOURS_IN_YEAR) {
		    v = Utils.hoursToMillis(offset);
		}   else if(this.what == OFFSET_MINUTES_IN_YEAR) {
		    v = Utils.minutesToMillis(offset);
		}   else if(this.what == OFFSET_SECONDS_IN_YEAR) {
		    v = Utils.secondsToMillis(offset);
		}
		Date newDate = new Date(start.getTime()+v);
		add(ctx, row, sdf.format(newDate));
		return row;
            } catch (Exception exc) {
		add(ctx, row, "");
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


        /** _more_ */
        private int col;

        /* */

        /** _more_ */
        private Date date;

        /* */


        /**
         * @param col _more_
         * @param sdf1 _more_
         * @param date _more_
         */
        public DateBefore(String col,  Date date) {
	    super(col);
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
		this.col  = getIndex(ctx);
                return row;
            }
            try {
                String s = row.get(col).toString().trim();
                if (s.length() == 0) {
                    return null;
                }
                Date d = ctx.parseDate(s);
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

        /** _more_ */
        private int col=-1;

        /* */

        /** _more_ */
        private Date date;

        public DateAfter(String col,  Date date) {
	    super(col);
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
		col = getIndex(ctx);
                return row;
            }
            try {
		if(!row.indexOk(col)) return row;
                String s = row.get(col).toString().trim();
                if (s.length() == 0) {
                    return null;
                }

                Date d = ctx.parseDate(s);
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


    public static class MsTo extends Converter {

        private int index;

	private String to;

        public MsTo(String col,String to) {
            super(col);
	    this.to = to;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            //Don't process the first row
            if (rowCnt++ == 0) {
                index = getIndex(ctx);
                row.add(to);
                return row;
            }

	    if(!row.indexOk(index)) return row;
	    double ms = row.getDouble(index);
	    row.add(""+Utils.millisTo((long)ms,to));
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
        what = what.toUpperCase().trim();
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
	} else if(what.equals("DAYS_IN_YEAR")) {
	    return  OFFSET_DAYS_IN_YEAR;
	} else if(what.equals("HOURS_IN_YEAR")) {
	    return  OFFSET_HOURS_IN_YEAR;	    
	} else if(what.equals("MINUTES_IN_YEAR")) {
	    return  OFFSET_MINUTES_IN_YEAR;	    
	} else if(what.equals("SECONDS_IN_YEAR")) {
	    return  OFFSET_SECONDS_IN_YEAR;	    
	} else if(what.equals("DECADE")) {
	    return  DECADE;
        } else {
            throw new IllegalArgumentException("Unknown date part:" + what);
        }
    }



}
