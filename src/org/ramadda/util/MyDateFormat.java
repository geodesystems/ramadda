/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Date;
import java.text.ParseException;

public class MyDateFormat {
    String format;
    TimeZone timezone;
    SimpleDateFormat sdf;
    
    public MyDateFormat(String format) {
	this.format=format;
    }

    public MyDateFormat(String format,TimeZone timezone) {
	this(format);
	this.timezone = timezone;
    }

    public MyDateFormat(String format,String timezone) {
	this(format,TimeZone.getTimeZone(timezone));
    }


    public  String  format(Date date) {
	return getSimpleDateFormat().format(date);
    }

    public  SimpleDateFormat getSimpleDateFormat() {
	if(sdf==null) {
	    sdf = new SimpleDateFormat();
            if(timezone!=null)
		sdf.setTimeZone(timezone);
	    sdf.applyPattern(format);
	}
	return sdf;
    }
    

    public String getFormat() {
	return format;
    }
    public Date parse(String s) throws ParseException {
	if (format.equals("SSS")) {
	    if (s.indexOf("E") >= 0) {
		long l = ((long) Double.parseDouble(s)) * 1000;
		return new Date(l);
	    } else {
		long l = Long.parseLong(s);
		return new Date(l);
	    }
	} else if (format.equals("sss")) {
	    if (s.indexOf("E") >= 0) {
		double l = Double.parseDouble(s) * 1000;
		return new Date((long) l);
	    } else {
		long l = Long.parseLong(s) * 1000;
		return new Date(l);
	    }
	} else if (format.equals("seconds")) {
	    if (s.indexOf("E") >= 0) {
		double l = Double.parseDouble(s);
		return new Date((long) l);
	    } else {
		long l = Long.parseLong(s);
		return new Date(l);
	    }
	}
	return getSimpleDateFormat().parse(s);
    }

}
