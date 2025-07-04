/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.seesv;

import org.apache.commons.codec.language.Soundex;

import org.json.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.PatternProps;
import org.ramadda.util.Propper;
import org.ramadda.util.Utils;

import ucar.unidata.xml.XmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;
import java.net.*;

import java.util.Date;
import java.util.Enumeration;
import java.security.MessageDigest;
import java.security.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import java.util.regex.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import javax.script.*;

@SuppressWarnings("unchecked")
public abstract class Converter extends Processor {

    public Converter() {}

    public Converter(String col) {
        super(col);
    }

    public Converter(List<String> cols) {
        super(cols);
    }

    private static String getLabel(int n) {
	int d = (int) (n / 25.0);
	int r = n % 25;
	//	    System.err.println("d:" + d +" r:" + r);
	if (d != 0) {
	    return getLabel(d-1) + Utils.LETTERS[r];
	} else {
	    return Utils.LETTERS[r];
	}
    }

    public static class ConverterGroup extends Converter {

        public ConverterGroup() {}

    }

    public static class ColumnSelector extends Converter {

        public ColumnSelector(TextReader ctx, List<String> cols) {
            super(cols);
	    if(ctx.getUniqueHeader()) {
		HashSet seen = new HashSet();
		for(String s: cols) {
		    s = makeID(s);
		    if(seen.contains(s))
			throw new RuntimeException("Non unique header value:" + s);
		    seen.add(s);
		}
	    }
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            List<Integer> indices = getIndices(ctx);
            boolean       debug   = ctx.getDebug() && (rowCnt++ == 0);
            if (indices.size() == 0) {
                if (debug) {
                    ctx.printDebug("-columns", "No indices");
                }

                //                return row;
            }
            List<String> result = new ArrayList<String>();
            for (Integer idx : indices) {
                if (idx < row.size()) {
                    String s = row.getString(idx);
                    result.add(s);
                } else {
                    if (debug) {
                        ctx.printDebug("-columns", "Missing index:" + idx);
                    }
                }
            }

            return new Row(result);
        }
    }

    public static class Renamer extends Converter {
	List<String> names;

        public Renamer(List<String> cols,List<String> names) {
            super(cols);
	    this.names = names;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
	    if(rowCnt++==0) {
		List<Integer> indices = getIndices(ctx);
		for(int i=0;i<indices.size();i++) {
		    int index = indices.get(i);
		    if(row.indexOk(i)) {
			String name = i<names.size()?names.get(i):names.get(names.size()-1);
			row.set(index,name);
		    }
		}
		
	    }
	    return row;
	}
    }


    public static class Roller extends Converter {

        public Roller(TextReader ctx, List<String> cols) {
            super(cols);
        }

	List<Row> prev= new ArrayList<Row>();

        @Override
        public Row processRow(TextReader ctx, Row row) {
	    if(rowCnt++==0) return row;
            List<Integer> indices = getIndices(ctx);
	    Row templateRow = new Row();
	    for(int i=0;i<row.size();i++) {
		if(!row.indexOk(i)) continue;
		if(!indices.contains(i)) {
		    templateRow.add(row.get(i));
		}
	    }
	    int baseIndex = indices.get(0);
	    for(int i:indices) {
		if(!row.indexOk(i)) continue;
		Object v = row.get(i);
		Row newRow = new Row(templateRow);
		newRow.insert(baseIndex,v);
		prev.add(newRow);
	    }
	    if(prev.size()>0) {
		row = prev.get(0);
		prev.remove(0);
		return row;
	    }
	    return null;
        }

        public void finish(TextReader ctx) throws Exception {
	    if(prev.size()>0) {
		finishRows(ctx, prev);
	    }
	}

    }

    public static class Grabber extends Converter {
	int col=-1;
	String scol;
	String pattern;
	List<String> names;
	List<String> values;
        public Grabber(String scol, String pattern,
		       List<String> cols,List<String> names) {
            super(cols);
	    this.scol = scol;
	    this.pattern = pattern;
	    this.names = names;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
	    if(rowCnt++==0) {
		row.addAll(names);
		col = getIndex(ctx,scol);
		return row;
	    }
	    if(!row.indexOk(col)) return row;
	    String v = row.getString(col);
	    if(v.matches(pattern)) {
		values = new ArrayList<String>();
		List<Integer> indices = getIndices(ctx);
		for(int i:indices) {
		    if(row.indexOk(i)) values.add(row.getString(i));
		    else values.add("");
		}

		return null;
	    }
	    if(values!=null) {
		row.addAll(values);
	    }
	    return row;
        }
    }

    public static class NoHeader extends Converter {

        public NoHeader() {
        }

        public Row processRow(TextReader ctx, Row row) {
	    if(rowCnt++==0) return null;
	    return row;
        }
    }

    public static class FileNamePattern extends Converter {

	List<String> names;
	List<String> values;	
	String pattern;

        public FileNamePattern(TextReader ctx, String pattern, String names) {
	    this.names = Utils.split(names,",",true,true);
	    this.pattern=pattern;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
	    if(values==null) {
		String file = ctx.getInputFile();
		values= new ArrayList<String>();
		String[] matches=Utils.findPatterns(file,pattern);
		if(matches==null) throw new RuntimeException("No match on file name:" + file +" using pattern:" + pattern);
		for(String m: matches)
		    values.add(m);

	    }

	    if(rowCnt++==0) {
		row.addAll(names);
	    } else {
		row.addAll(values);
	    }
	    return row;
        }
    }

    public static class Editor extends Converter {
	private static String CLEAR = "\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b";
	private Row header;

	private BufferedReader br;

	private boolean done = false;

	private Hashtable<String,String> seen = new Hashtable<String,String>();

        public Editor(List<String> cols) {
            super(cols);
	    br = new BufferedReader(new InputStreamReader(System.in));
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
	    if(done) return row;
	    if(rowCnt++==0) {
		header = row;
		return row;
	    }

	    for(int col:getIndices(ctx)) {
		if(!header.indexOk(col) || !row.indexOk(col)) continue;
		String h = header.getString(col);
		String s = row.getString(col);
		System.err.print(CLEAR);
		String key =col+"_"+s;
		String seenIt = seen.get(key);
		if(seenIt!=null) {
		    row.set(col,seenIt);
		    continue;
		}
		String label = "#" + (rowCnt-1)+" " +h+"("+ s+"):";
		while(true) {
		    System.err.print(label);
		    try {
			String v= br.readLine();
			if(v.length()==0) break;
			if(v.length()==1 && v.charAt(0)==0x1b) {
			    System.err.print(CLEAR);
			    done = true;
			    return row;
			}
			row.set(col,v);
			seen.put(key,v);
			break;
		    } catch(Exception exc) {
			throw new RuntimeException(exc);
		    }
		}
	    }
	    return row;
        }
    }

    public static class Highlighter extends Converter {

	String prefix;
	boolean rainbow=false;

        public Highlighter(List<String> cols, String color) {
            super(cols);
	    if(color.equals("rainbow")) rainbow=true;
	    else if(color.equals("green")) prefix=Utils.ANSI_GREEN_BOLD;
	    else if(color.equals("yellow")) prefix=Utils.ANSI_YELLOW_BOLD;
	    else if(color.equals("blue")) prefix=Utils.ANSI_BLUE_BOLD;
	    else if(color.equals("purple")) prefix=Utils.ANSI_PURPLE_BOLD;
	    else if(color.equals("cyan")) prefix=Utils.ANSI_CYAN_BOLD;
	    else prefix=Utils.ANSI_RED_BOLD;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                return row;
            }
            List<Integer> indices = getIndices(ctx);
	    int cnt=0;
	    for(int idx:getIndices(ctx)) {
		if(row.indexOk(idx)) {
		    if(rainbow) {
			prefix = Utils.getAnsiColor(cnt++);
		    }

		    row.set(idx,Utils.ansi(prefix,row.getString(idx)));
		}
	    }
	    return row;
        }
    }

    public static class Backgrounder extends Converter {
	String prefix;

        public Backgrounder(List<String> cols, String color) {
            super(cols);
	    if(color.equals("green")) prefix=Utils.ANSI_GREEN_BACKGROUND;
	    else if(color.equals("yellow")) prefix=Utils.ANSI_YELLOW_BACKGROUND;
	    else if(color.equals("blue")) prefix=Utils.ANSI_BLUE_BACKGROUND;
	    else if(color.equals("purple")) prefix=Utils.ANSI_PURPLE_BACKGROUND;
	    else if(color.equals("cyan")) prefix=Utils.ANSI_CYAN_BACKGROUND;
	    else prefix=Utils.ANSI_RED_BACKGROUND;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                return row;
            }
            List<Integer> indices = getIndices(ctx);
	    for(int idx:getIndices(ctx)) {
		if(row.indexOk(idx)) {
		    row.set(idx,prefix+row.getString(idx)+Utils.ANSI_RESET);
		}
	    }
	    return row;
        }
    }

    public static class RowAppender  extends Converter {
	private int skip;
	private int count;
	private String delimiter;
	private List<Row> rows = new ArrayList<Row>();

        public RowAppender(int skip, int count, String delimiter) {
	    this.skip = skip;
	    this.count = count;
	    this.delimiter = delimiter;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
	    if(skip>0) {
		skip--;
		return row;
	    }
	    if(count<=0) return row;
	    rows.add(row);
	    count--;
	    if(count<=0) {
		Row newRow = null;
		for(Row r: rows) {
		    if(newRow==null) newRow = r;
		    else {
			for(int i=0;i<r.size();i++) {
			    newRow.set(i,newRow.getString(i)+delimiter+r.getString(i));
			}
		    }
		}
		return newRow;
	    }
	    return null;
        }
    }

    public static class Faker extends Converter {

        private String what;

        com.github.javafaker.Faker faker = new com.github.javafaker.Faker();

        private double v1 = Double.NaN;

        private double v2 = Double.NaN;

        private double v3 = Double.NaN;

        public Faker(TextReader ctx, String what, List<String> cols) {
            super(cols);
            List<String> toks = Utils.split(what, ":", true, true);
            this.what = toks.get(0);
            if (toks.size() > 1) {
                v1 = Double.parseDouble(toks.get(1));
            }
            if (toks.size() > 2) {
                v2 = Double.parseDouble(toks.get(2));
            }
            if (toks.size() > 3) {
                v3 = Double.parseDouble(toks.get(3));
            }
        }

        private boolean hasV1() {
            return !Double.isNaN(v1);
        }

        private boolean hasV2() {
            return !Double.isNaN(v2);
        }

        private boolean hasV3() {
            return !Double.isNaN(v3);
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            List<Integer> indices = getIndices(ctx);
            if (rowCnt++ == 0) {
                if (indices.size() == 0) {
                    row.add(what);
                }

                return row;
            }

            if (indices.size() == 0) {
                row.add(getFakerValue());
            } else {
                for (Integer idx : getIndices(ctx)) {
                    row.set(idx, getFakerValue());
                }
            }

            return row;
        }

        private String getFakerValue() {

            if (what.equals("boolean")) {
                return "" + faker.bool().bool();
            }
            if (what.equals("firstname")) {
                return faker.name().firstName();
            }
            if (what.equals("fullname")) {
                return faker.name().fullName();
            }
            if (what.equals("lastname")) {
                return faker.name().lastName();
            }
            if (what.equals("name")) {
                return faker.name().name();
            }
            if (what.equals("namewithmiddle")) {
                return faker.name().nameWithMiddle();
            }
            if (what.equals("prefix")) {
                return faker.name().prefix();
            }
            if (what.equals("suffix")) {
                return faker.name().suffix();
            }
            if (what.equals("title")) {
                return faker.name().title();
            }
            if (what.equals("username")) {
                return faker.name().username();
            }

            if (what.equals("address")) {
                return faker.address().streetAddress();
            }
            if (what.equals("city")) {
                return faker.address().city();
            }
            if (what.equals("country")) {
                return faker.address().country();
            }
            if (what.equals("state")) {
                return faker.address().state();
            }
            if (what.equals("stateabbr")) {
                return faker.address().stateAbbr();
            }
            if (what.equals("streetname")) {
                return faker.address().streetName();
            }
            if (what.equals("timezone")) {
                return faker.address().timeZone();
            }
            if (what.equals("zipcode")) {
                return faker.address().zipCode();
            }
            if (what.equals("latitude")) {
                return faker.address().latitude();
            }
            if (what.equals("longitude")) {
                return faker.address().longitude();
            }
            if (what.equals("countrycode")) {
                return faker.address().countryCode();
            }

            if (what.equals("asin")) {
                return faker.code().asin();
            }

            if (what.equals("ean13")) {
                return faker.code().ean13();
            }

            if (what.equals("ean8")) {
                return faker.code().ean8();
            }

            if (what.equals("gtin13")) {
                return faker.code().gtin13();
            }
            if (what.equals("gtin8")) {
                return faker.code().gtin8();
            }

            if (what.equals("imei")) {
                return faker.code().imei();
            }

            if (what.equals("isbn10")) {
                return faker.code().isbn10();
            }

            if (what.equals("isbn13")) {
                return faker.code().isbn13();
            }

            if (what.equals("isbngroup")) {
                return faker.code().isbnGroup();
            }

            if (what.equals("isbngs1")) {
                return faker.code().isbnGs1();
            }

            if (what.equals("isbnregistrant")) {
                return faker.code().isbnRegistrant();
            }

            if (what.equals("color")) {
                return faker.commerce().color();
            }

            if (what.equals("department")) {
                return faker.commerce().department();
            }

            if (what.equals("material")) {
                return faker.commerce().material();
            }

            if (what.equals("price")) {
                return faker.commerce().price();
            }

            if (what.equals("productname")) {
                return faker.commerce().productName();
            }

            if (what.equals("promotioncode")) {
                return faker.commerce().promotionCode();
            }

            if (what.equals("demonym")) {
                return faker.demographic().demonym();
            }

            if (what.equals("educationalattainment")) {
                return faker.demographic().educationalAttainment();
            }

            if (what.equals("maritalstatus")) {
                return faker.demographic().maritalStatus();
            }

            if (what.equals("race")) {
                return faker.demographic().race();
            }

            if (what.equals("sex")) {
                return faker.demographic().sex();
            }

            if (what.equals("bic")) {
                return faker.finance().bic();
            }

            if (what.equals("creditcard")) {
                return faker.finance().creditCard();
            }

            if (what.equals("iban")) {
                return faker.finance().iban();
            }

            if (what.equals("ssn")) {
                return faker.idNumber().ssnValid();
            }

            if (what.equals("digit")) {
                if (hasV1()) {
                    return "" + faker.number().digits((int) v1);
                }

                return "" + faker.number().digit();
            }

            if (what.equals("digits")) {
                return "" + faker.number().digits((int) v1);
            }

            if (what.equals("numberbetween")) {
                return "" + faker.number().numberBetween((long) v1,
							 (long) v2);
            }

            if (what.equals("randomdigit")) {
                return "" + faker.number().randomDigit();
            }

            if (what.equals("randomdigitnotzero")) {
                return "" + faker.number().randomDigitNotZero();
            }

            if (what.equals("randomdouble")) {
                return "" + faker.number().randomDouble((int) v1, (long) v2,
							(long) v3);
            }

            if (what.equals("randomnumber")) {
                return "" + faker.number().randomNumber();
            }

            if (what.equals("cellphone")) {
                return faker.phoneNumber().cellPhone();
            }

            if (what.equals("phonenumber")) {
                return faker.phoneNumber().phoneNumber();
            }

            if (what.equals("diseasename")) {
                return faker.medical().diseaseName();
            }

            if (what.equals("hospitalname")) {
                return faker.medical().hospitalName();
            }

            if (what.equals("medicinename")) {
                return faker.medical().medicineName();
            }

            if (what.equals("symptoms")) {
                return faker.medical().symptoms();
            }

            throw new IllegalArgumentException(
					       "Unknown anonymization:" + what + " needs to be one of:"
					       + "firstname|fullname|lastname|name|namewithmiddle|prefix|suffix|title|username|address|city|country|state|stateabbr|streetname|timezone|zipcode|latitude|longitude|countrycode|boolean|asin|ean13|ean8|gtin13|gtin8|imei|isbn10|isbn13|isbngroup|isbngs1|isbnregistrant|color|department|material|price|productname|promotioncode|demonym|educationalattainment|maritalstatus|race|sex|bic|creditcard|iban|ssn|digit|digits:number_of_digits|numberbetween:first:last|randomdigit|randomdigitnotzero|randomDouble:maxNumberOfDecimals:min:max|randomnumber|cellphone|phonenumber|diseasename|hospitalname|medicinename|symptoms");
        }
    }

    public static class ColumnFirst extends Converter {

        private HashSet<Integer> firstSeen;

        public ColumnFirst(TextReader ctx, List<String> cols) {
            super(cols);
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
	    //	    debug = rowCnt++==0;
            List<Integer> indices = getIndices(ctx);
            if (indices.size() == 0) {
                return row;
            }
            List<String> result = new ArrayList<String>();
            if (firstSeen == null) {
                firstSeen = new HashSet<Integer>();
                for (Integer idx : indices) {
                    firstSeen.add(idx);
                }
            }
            for (Integer idx : indices) {
		if(!row.indexOk(idx)) continue;
                String s = row.getString(idx);
                result.add(s);
            }
            for (int i = 0; i < row.size(); i++) {
                if ( !firstSeen.contains(i)) {
                    result.add(row.getString(i));
                }
            }

            return new Row(result);
        }
    }

    public static class ColumnLast extends Converter {

        private HashSet<Integer> firstSeen;

        public ColumnLast(TextReader ctx, List<String> cols) {
            super(cols);
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
	    //	    debug = rowCnt++==0;
            List<Integer> indices = getIndices(ctx);
            if (indices.size() == 0) {
                return row;
            }
            List<String> result = new ArrayList<String>();
            if (firstSeen == null) {
                firstSeen = new HashSet<Integer>();
                for (Integer idx : indices) {
                    firstSeen.add(idx);
                }
            }
            for (int i = 0; i < row.size(); i++) {
                if (!firstSeen.contains(i)) {
                    result.add(row.getString(i));
                }
            }
            for (Integer idx : indices) {
		if(!row.indexOk(idx)) continue;
                String s = row.getString(idx);
                result.add(s);
            }

            return new Row(result);
        }
    }

    public static class ColumnsBefore extends Converter {

        private HashSet<Integer> set;

        private String col;

        private int colIdx = -1;

        public ColumnsBefore(TextReader ctx, String col, List<String> cols) {
            super(cols);
            this.col = col;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (colIdx == -1) {
                colIdx = getColumnIndex(ctx, col);
            }
            List<Integer> indices = getIndices(ctx);
            if (set == null) {
                set = new HashSet<Integer>();
                for (int i : indices) {
                    set.add(i);
                }
            }
            if (indices.size() == 0) {
                return row;
            }

            List values  = row.getValues();
            List newList = new ArrayList();
            for (int i = 0; i < colIdx; i++) {
                if ( !set.contains(i)) {
                    newList.add(values.get(i));
                }
            }
            for (int i : indices) {
                newList.add(values.get(i));
            }
            for (int i = colIdx; i < values.size(); i++) {
                if (set.contains(i)) {
                    continue;
                }
                newList.add(values.get(i));
            }

            return new Row(newList);
        }
    }

    public static class ColumnsAfter extends Converter {

        private HashSet<Integer> set;

        private String col;

        private int colIdx = -1;

        public ColumnsAfter(TextReader ctx, String col, List<String> cols) {
            super(cols);
            this.col = col;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (colIdx == -1) {
                Integer i = getColumnIndex(ctx, col);
                if (i == null) {
                    fatal(ctx, "Could not find index:" + col);
                }
                colIdx = i;
            }

            List<Integer> indices = getIndices(ctx);
            if (set == null) {
                set = new HashSet<Integer>();
                for (int i : indices) {
                    set.add(i);
                }
            }
            if (indices.size() == 0) {
                return row;
            }

            List values  = row.getValues();
            List newList = new ArrayList();
            for (int i = colIdx + 1; i < values.size(); i++) {
                if ( !set.contains(i)) {
                    newList.add(values.get(i));
                }
            }
            newList.add(0, values.get(colIdx));
            for (int i = indices.size() - 1; i >= 0; i--) {
                newList.add(1, values.get(indices.get(i)));
            }
            for (int i = colIdx - 1; i >= 0; i--) {
                if (set.contains(i)) {
                    continue;
                }
                newList.add(0, values.get(i));
            }

            return new Row(newList);
        }
    }

    public static class ColumnNotSelector extends Converter {

        public ColumnNotSelector(TextReader ctx, List<String> cols) {
            super(cols);
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            List<Integer> indices = getIndices(ctx);
            return removeColumns(indices, row);
        }

    }

    /* */

    private static Hashtable<String, String> imageMap = new Hashtable<String,
	String>();

    public static class ImageSearch extends Converter {

        /* */

        private String suffix;

        private String imageColumn;

        private int imageColumnIndex = -1;

        public ImageSearch(TextReader ctx, List<String> cols, String suffix) {
            super(cols);
            this.suffix = suffix;
        }

        public ImageSearch(TextReader ctx, List<String> cols, String suffix,
                           String imageColumn) {
            super(cols);
            this.suffix      = suffix;
            this.imageColumn = imageColumn;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if ((imageColumn != null) && (imageColumnIndex == -1)) {
                imageColumnIndex = getIndex(ctx, imageColumn);
            }
            if (rowCnt++ == 0) {
                if (imageColumnIndex == -1) {
                    add(ctx, row, "image");
                }

                return row;
            }

            List<Integer> indices = getIndices(ctx);
            String        s       = "";
            for (Integer idx : indices) {
                s += row.getString(idx) + " ";
            }
            s += suffix;
            //hack, hack
            String script = "/Users/jeffmc/bin/imagesearch.sh";
            for (int attempt = 0; attempt < 3; attempt++) {
                if (imageColumnIndex >= 0) {
                    String existing = (String) row.get(imageColumnIndex);
                    if (Utils.stringDefined(existing)) {
                        return row;
                    }
                }
                String result = "";
                try {
                    s = s.replace(" ", "%s");
                    String image = imageMap.get(s);
                    if (image == null) {
                        Process p = Runtime.getRuntime().exec(new String[] {
				"sh",
				script, s });
                        result =
                            IO.readInputStream(p.getInputStream()).trim();
                        JSONObject obj    = new JSONObject(result);
                        JSONArray  values = obj.getJSONArray("value");
                        if (values.length() == 0) {
                            System.err.println(s + " failed. sleeping");
                            if (attempt == 0) {
                                System.err.println("response:" + result);
                            }
                            Misc.sleepSeconds(1 + attempt);

                            continue;
                        }
                        JSONObject value = values.getJSONObject(0);
                        image = value.optString("thumbnailUrl", "");
                        System.err.println("found image:" + s + " image:"
                                           + image);
                        ctx.printDebug("-image",
                                       "value:" + s + " found:" + image);
                        imageMap.put(s, image);
                    } else {
                        ctx.printDebug("-image",
                                       "value:" + s + " in cache:" + image);
                    }
                    if (imageColumnIndex >= 0) {
                        row.set(imageColumnIndex, image);
                    } else {
                        add(ctx, row, image);
                    }

                    return row;
                } catch (Exception exc) {
                    System.err.println("JSON:" + result);

                    throw new RuntimeException(exc);
                }
            }
            if (imageColumnIndex == -1) {
                add(ctx, row, "");
            }

            return row;
        }

    }

    public static class Embed extends Converter {

        private String imageColumn;

        private int imageColumnIndex = -1;

        public Embed(TextReader ctx, String col) {
            super();
            imageColumn = col;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                imageColumnIndex = getIndex(ctx, imageColumn);
                row.add("Image Data");

                return row;
            }

            String url = row.getString(imageColumnIndex);
            if ( !Utils.stringDefined(url)) {
                row.add("");

                return row;
            }
            url = url.trim();
            if (url.toLowerCase().startsWith("file")) {
                fatal(ctx, "Bad url:" + url);
            }
            String type = IOUtil.getFileExtension(url);
            if ( !Utils.stringDefined(type)) {
                type = "png";
            }
            try {
                URL         _url  = new URL(url);
                InputStream is    = IO.getInputStream(_url);
                byte[]      bytes = IOUtil.readBytes(is);
                String b = "data:image/" + type + ";base64, "
		    + Utils.encodeBase64Bytes(bytes);
                row.add(b);
            } catch (Exception exc) {
                System.err.println("Error reading url:" + url);
                exc.printStackTrace();
                row.add("");
                //              fatal(ctx, "Reading url:" + url, exc);
            }

            return row;
        }

    }

    public static class Fetch extends Converter {

        private Row headerRow;

        private String name;

        private String urlTemplate;

	private boolean ignoreErrors;

        public Fetch(TextReader ctx, String name, boolean ignoreErrors, String urlTemplate) {
            super();
            this.name        = name;
	    this.ignoreErrors = ignoreErrors;
            this.urlTemplate = urlTemplate;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                headerRow = row;
                row.add(name);

                return row;
            }
            if (rowCnt > 5) {
                //              row.add("FILLER");
                //              return row;
            }

            String url = replaceMacros(urlTemplate, headerRow,row);
            System.err.println("URL:" + url);
            if (url.toLowerCase().startsWith("file")) {
                fatal(ctx, "Bad url:" + url);
            }
            try {
                URL         _url     = new URL(url);
                InputStream is       = IO.getInputStream(_url);
                String      contents = IO.readInputStream(is);
                is.close();
                row.add(contents);
            } catch (Exception exc) {
		if(!ignoreErrors)
		    fatal(ctx, "Reading url:" + url, exc);
		row.add("");
            }

            return row;
        }

    }

    public static class WikiDescSearch extends Converter {

        /* */

        private String suffix;

        public WikiDescSearch(List<String> cols, String suffix) {
            super(cols);
            this.suffix = suffix;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                add(ctx, row, "description");

                return row;
            }

            try {
                List<Integer> indices = getIndices(ctx);
                String        s       = "";
                for (Integer idx : indices) {
                    s += row.getString(idx) + " ";
                }
                s += suffix;
                s = s.replaceAll(" ", "%20");
                String baseUrl = "https://en.wikipedia.org/w/api.php";
                String url =
                    baseUrl
                    + "?action=query&srlimit=1&list=search&format=json&srsearch="
                    + s;
                String     result = IO.readUrl(url);
                JSONObject obj    = new JSONObject(result);
                JSONArray  values = JsonUtil.readArray(obj, "query.search");
                if (values.length() == 0) {
                    System.err.println("No results for query:" + s);
                    add(ctx, row, "");

                    return row;
                }
                JSONObject value   = values.getJSONObject(0);
                String     snippet = value.optString("snippet", "");
                String     title   = value.optString("title", "");
                title = title.replaceAll(" ", "%20");
                url = baseUrl + "?action=parse&prop=text&page=" + title
		    + "&format=json";
                result = IO.readUrl(url);
                obj    = new JSONObject(result);
                obj    = JsonUtil.readObject(obj, "parse.text");
                String contents = obj.optString("*", "");
                String p = StringUtil.findPattern(contents,
						  "(?s)/table>(.*?)<div id=\"toc\"");
                if (p != null) {
                    String p2 = StringUtil.findPattern(contents,
						       "(?s).*?<p>(.*?)</p>");
                    if (p2 != null) {
                        p = p2;
                    } else {
                        p = snippet;
                    }
                }
                if (p == null) {
                    p = snippet;
                }
                add(ctx, row, p);

                return row;
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

    }

    public static class ColumnWidth extends Converter {

        /* */

        int size;

        public ColumnWidth(List<String> cols, int size) {
            super(cols);
            this.size = size;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            List<Integer> indices = getIndices(ctx);
            for (int i : indices) {
                if ((i < 0) || (i >= row.size())) {
                    continue;
                }
                String s = row.get(i).toString();
                if (s.length() > size) {
                    s = s.substring(0, size - 1);
                }
                row.set(i, s);
            }

            return row;
        }

    }

    public static class ColumnFormatter extends Converter {

        /* */

        private DecimalFormat format;

        public ColumnFormatter(List<String> cols, String fmt) {
            super(cols);
            format = new DecimalFormat(fmt);
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            List<Integer> indices = getIndices(ctx);
            for (int i : indices) {
                if ((i < 0) || (i >= row.size())) {
                    continue;
                }
                try {
                    double value = Double.parseDouble(row.get(i).toString());
                    row.set(i, format.format(value));
                } catch (NumberFormatException nfe) {}
            }

            return row;
        }
    }

    public static class Padder extends Converter {

        /* */

        private int count;

        /* */

        private String pad;

        public Padder(int count, String s) {
            this.count = count;
            this.pad   = s;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            while (row.getValues().size() < count) {
                row.getValues().add(pad);
            }
            while (row.getValues().size() > count) {
                row.getValues().remove(row.getValues().size() - 1);
            }

            return row;
        }

    }

    public static class Prefixer extends Converter {

        /* */

        private String pad;

        public Prefixer(List<String> cols, String s) {
            super(cols);
            this.pad = s;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                if ( !ctx.getAllData()) {
                    return row;
                }
            }
            List<Integer> indices = getIndices(ctx);
            for (int i : indices) {
		if(!row.indexOk(i)) continue;
                row.set(i, pad + row.get(i));
            }

            return row;
        }
    }

    public static class Suffixer extends Converter {

        /* */

        private String pad;

        public Suffixer(List<String> cols, String s) {
            super(cols);
            this.pad = s;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                if ( !ctx.getAllData()) {
                    return row;
                }
            }
            List<Integer> indices = getIndices(ctx);
            for (int i : indices) {
		if(!row.indexOk(i)) continue;
                row.set(i, row.get(i) + pad);
            }

            return row;
        }
    }

    public static class PrintHeader extends Converter {

        /* */

        private boolean asPoint = false;

        public PrintHeader() {}

        public PrintHeader(boolean asPoint) {
            this.asPoint = asPoint;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            PrintWriter writer    = ctx.getWriter();
            String      delimiter = ctx.getDelimiter();
            if (delimiter == null) {
                delimiter = ",";
            }
            if (asPoint) {
                writer.println("skiplines=1");
                writer.print("fields=");
            }

            for (int i = 0; i < row.size(); i++) {
                String col = row.getString(i);
                col = col.replaceAll("\n", " ");
                if (asPoint) {
                    if (i > 0) {
                        writer.print(", ");
                    }
                    String label =
                        Utils.makeLabel(col.replaceAll("\\([^\\)]+\\)", ""));
                    String unit = StringUtil.findPattern(col,
							 ".*?\\(([^\\)]+)\\).*");
                    StringBuffer attrs = new StringBuffer();
                    attrs.append("label=\"" + label + "\" ");
                    if (unit != null) {
                        attrs.append("unit=\"" + unit + "\" ");

                    }
                    String id = label.replaceAll(
						 "\\([^\\)]+\\)", "").replaceAll(
										 "-", "_").trim().toLowerCase().replaceAll(
															   " ", "_").replaceAll(":", "_");
                    id = id.replaceAll("/+", "_");
                    id = id.replaceAll("\\.", "_");
                    id = id.replaceAll("_+_", "_");
                    id = id.replaceAll("_+$", "");
                    id = id.replaceAll("^_+", "");
                    if (id.indexOf("date") >= 0) {
                        attrs.append("type=\"date\" format=\"\" ");
                    }
                    writer.print(id + "[" + attrs + "] ");
                } else {
		    String id = makeID(col);
                    writer.println("#" + i + " " + col + " id:" + id );
                }
            }
            if (asPoint) {
                writer.println("");
            }
            writer.flush();
            writer.close();

            ctx.stopRunning();

            return null;
        }

    }

    public static class HeaderNames extends Converter {

        public HeaderNames() {}

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ > 0) {
                return row;
            }
            for (int i = 0; i < row.size(); i++) {
                String s = row.getString(i);
                s = s.replaceAll("([A-Z])", "xdelimiter$1");
                s = Utils.makeID(s);
                s = s.replace("_", "xdelimiter");
                List<String> toks = Utils.split(s, "xdelimiter", true, true);
                String       tmp  = "";
                for (String tok : toks) {
                    if (tmp.length() > 0) {
                        tmp += " ";
                    }
                    tok = Utils.upperCaseFirst(tok);
                    tmp += tok;
                }
                tmp = tmp.replaceAll("[^\\p{ASCII}]", " ");
                row.set(i, tmp);
            }

            return row;
        }

    }

    public static class HeaderIds extends Converter {

        public HeaderIds() {}

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ > 0) {
                return row;
            }
            for (int i = 0; i < row.size(); i++) {
                String s = row.getString(i);
                s = Utils.makeID(s);
                row.set(i, s);
            }

            return row;
        }

    }

    public static class Ids extends Converter {

        public Ids() {}

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ > 0) {
                return row;
            }
            for (int i = 0; i < row.size(); i++) {
                String s = row.getString(i);
                s = Utils.makeID(s);
		if(s.length()==0) s="column" + (i+1);
                row.set(i, s);
            }

            return row;
        }

    }

    public static class HeaderMaker extends Converter {
        private int count = 0;
        PatternProps props;
        String defaultType = "string";
        String defaultTypeFromProperties;
        boolean defaultChartable = true;
        boolean makeLabel = true;
        boolean toStdOut = false;
        Row firstRow;
	List<String[]> typePatterns;

	org.ramadda.util.Propper  propper;

        public HeaderMaker(Seesv seesv, Dictionary<String, String> props) {
            this.props = new PatternProps(props);
	    try {
		for (Enumeration keys = props.keys(); keys.hasMoreElements(); ) {
		    String key =(String) keys.nextElement(); 
		    String value = props.get(key);
		    if(key.equals("file") || key.equals("file1") || key.equals("file2") || key.equals("file3")) {
			String c = seesv.readFile(value);
			Hashtable<String,String> h = Utils.getProperties(c);
			this.props.putAll(h,true);
		    }
		}
	    } catch(Exception exc) {
		throw new RuntimeException(exc);
	    }

            defaultType = Seesv.getDbProp(props, "default", "type",
					  defaultType);
	    
            defaultTypeFromProperties = Seesv.getDbProp(props, "default",
							"type", null);
            defaultChartable = Seesv.getDbProp(props, "default",
					       "chartable", true);
	    String namesFile = Seesv.getDbProp(props, null, "namesfile", null);
	    try {
		if(namesFile!=null) {
		    String names = seesv.readFile(namesFile);
		    propper = org.ramadda.util.Propper.create(true,namesFile,(InputStream)new ByteArrayInputStream(names.getBytes()));
		}
	    } catch(Exception exc) {
		System.err.println("Could not read names file:" + namesFile);
		throw new RuntimeException(exc);
	    }
            makeLabel = Seesv.getDbProp(props, null, "makeLabel", true);
            toStdOut  = Seesv.getDbProp(props, null, "stdout", false);
	    Enumeration<String> keys = props.keys();
	    typePatterns = new ArrayList<String[]>();
	    while (keys.hasMoreElements()) {
		String key = keys.nextElement();
		if(key.endsWith(".type")) {
		    String pattern = key.replace(".type","");
		    if(StringUtil.containsRegExp(pattern)) {
			String v  =props.get(key);
			pattern = pattern.replace("_space_"," ");
			//			System.err.println("PATTERN:" + pattern +  " value:" + v);
			typePatterns.add(new String[]{pattern,v});
		    }
		}
	    }

        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            boolean debug = Misc.equals(props.get("debug"), "true");
            rowCnt++;
            if (rowCnt > 2) {
                if (debug) {
		    //                    System.err.println("addHeader data row:" + row);
                }

                return row;
            }
            if (firstRow == null) {
                firstRow = row;
                if (debug) {
                    System.err.println("got first row:" + row);
                }

                return null;
            }
            boolean justFields  = Misc.equals(props.get("justFields"),
					      "true");
            PrintWriter  writer = ctx.getWriter();
            StringBuffer sb     = new StringBuffer();
            if (toStdOut) {
                sb.append("fields=");
            } else {
                sb.append("#fields=");
            }
            List values = new ArrayList<String>();
            String dfltFormat = Seesv.getDbProp(props, "default", "format",
						null);
            String dfltUnit = Seesv.getDbProp(props, "default", "unit",
					      null);
            boolean dfltSearchable = Seesv.getDbProp(props, "default", "searchable",false);

	    Row unitRow = firstRow.getUnitRow();
            for (int i = 0; i < firstRow.getValues().size(); i++) {
                String   col = (String) firstRow.getValues().get(i);
                String[] toks;
                String   desc  = null;
                String   label = null;

                try {
                    toks = Utils.findPatterns(col, "<desc>(.*)</desc>");
                    if ((toks != null) && (toks.length == 1)) {
                        desc = toks[0];
                        desc = desc.replaceAll(",", "_comma_");
                        desc = desc.replaceAll("\"", "").replaceAll("\n",
								    " ");
                        col = col.replaceAll("<desc>.*</desc>", "");
                    }
                    toks = Utils.findPatterns(col, "<label>(.*)</label>");
                    if ((toks != null) && (toks.length == 1)) {
                        label = toks[0];
                        col   = col.replaceAll("<label>.*</label>", "");
                    }
                } catch (Exception exc) {
                    throw new IllegalArgumentException(exc);
                }

                Object osample = row.indexOk(i)?row.getValues().get(i):null;
                if (osample == null) {
		    osample = "";
                }
                String sample  = (String) osample.toString();
                String _sample = sample.toLowerCase();
                col = Utils.replaceAll(col,
				       "\u00B5", "u",
				       "\u00B3", "^3",
				       "\n", " ");
                String id = Utils.replaceAll(col,"_","thedelimiter","°"," ",
					     "\\([^\\)]+\\)", "",
					     "\\?", "",
					     "\\$", "",
					     ",", "_",
					     "-", "_").trim().toLowerCase().replaceAll(" ", "_").replaceAll(":", "_");

                id = Utils.replaceAll(id,"<", "_",
				      ">", "_",
				      "\\+", "_",
				      "\"", "_",
				      "\\$", "_",
				      "&", "_",
				      "%", "_",
				      "\'", "_",
				      "/+", "_",
				      "\\.", "_",
				      "_+_", "_",
				      "_+$", "",
				      "^_+", "",
				      "\\^", "_");

		id = Utils.replaceAll(id,"thedelimiter","_");
                id = Seesv.getDbProp(props, id, i, "id", id);

		List<String> proppers = null;
		if(propper!=null) {
		    proppers = (List<String>)propper.get(col,id);
		}
                StringBuffer attrs = new StringBuffer();
                String group = Seesv.getDbProp(props, id, i, "group",
					       (String) null);
                if (group != null) {
                    attrs.append(" group=\"" + group + "\" ");
                }

                if (label == null) {
                    label = Seesv.getDbProp(props, id, i, "label",
					    (String) null);
                }
		if(label==null && proppers!=null) label  = proppers.get(0);
		if(desc==null && proppers!=null && proppers.size()>1) desc  = proppers.get(1);		
		//		System.err.println(col+":" + label+":" + desc);

                if (makeLabel && (label == null)) {
                    label = Utils.makeLabel(col.replaceAll("\\([^\\)]+\\)",
							   ""));
		    label = label.replaceAll("°"," ");
                }
		if(label!=null) label = label.replaceAll("[\"']+","");

                String unit = StringUtil.findPattern(col,
						     ".*?\\(([^\\)]+)\\).*");
		if(unitRow!=null && unitRow.indexOk(i)) unit=(String)unitRow.get(i);
                if (label != null) {
                    label = label.replaceAll(",", "%2C").replaceAll("<br>",
								    " ").replaceAll("<p>", " ");
                    label = label.replaceAll("  +", " ").replace("_space_",
								 " ");
                    attrs.append("label=\"" + label + "\" ");
                }

                if (desc != null) {
                    attrs.append(" description=\"" + desc + "\" ");
                }
                if (unit == null) {
                    unit = Seesv.getDbProp(props, id, i, "unit", dfltUnit);
                }
                if (Utils.stringDefined(unit)) {
                    attrs.append("unit=\"" + unit + "\" ");

                }
                String format = dfltFormat;
                String type   = (defaultTypeFromProperties != null)
		    ? defaultTypeFromProperties
		    : defaultType;
		//		System.err.println("id:"  + id  + " default:" + type);
                boolean isGeo = false;
                boolean chartable = Seesv.getDbProp(props, id, "chartable", defaultChartable);
		boolean searchable =  Seesv.getDbProp(props, id, "searchable", dfltSearchable);
                if (id.equals("date")) {
                    type = "date";
                } else if (id.equals("year")) {
                    type   = "date";
                    format = "yyyy";
                } else if (id.startsWith("date_")) {
                    type   = "date";
                    format = "yyyy-MM-dd";
                } else if (id.equals("url") || id.endsWith("_url")
                           || id.equals("website")) {
                    if (id.indexOf("photo") >= 0) {
                        type = "image";
                    } else {
                        type = "url";
                    }
                } else if (id.equals("state") || id.startsWith("state_")
                           || id.equals("country") || id.equals("category")
                           || id.equals("party") || id.equals("class")) {
                    type = "enumeration";
                } else if (id.equals("city") || id.endsWith("_city")) {
                    type = "enumeration";
                } else if (id.equals("gender") || id.equals("ethnicity")
                           || id.equals("religion")) {
                    type = "enumeration";
                } else if (id.equals("latitude") || id.equals("longitude")) {
                    type      = "double";
                    isGeo     = true;
                    chartable = false;
                } else if (defaultTypeFromProperties == null) {
                    try {
                        if (_sample.equals("true")
			    || _sample.equals("false")) {
                            type = "enumeration";
                        } else if (_sample.equals("nan")
                                   || _sample.equals("na")) {
                            type = "double";
                        } else if (sample.startsWith("http")) {
                            if (Utils.isImage(sample)) {
                                type = "image";
                            } else {
                                type = "url";
                            }
                        } else if (sample.matches("^(\\+|-)?\\d+$")) {
                            type = "integer";
                            //                      System.err.println("\tinteger:" + sample);
                        } else if (sample.matches(
						  "^[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?$")) {
                            //                      System.err.println("\tdouble:" + sample);
                            type = "double";
                        } else if (sample.matches(
						  "\\d\\d\\d\\d-\\d\\d-\\d\\d")) {
                            type   = "date";
                            format = "yyyy-MM-dd";
                        } else {}
                    } catch (Exception exc) {}
                }

                //              System.err.println("\tfinal type:" + type);

		for(String[]tuple:typePatterns) {
		    if(label.matches(tuple[0]) || id.matches(tuple[0])) {
			type = tuple[1];
			break;
		    }
		}
                type = Seesv.getDbProp(props, id, i, "type", type);

		//		System.err.println("ID:"  + id  + " type:" + type +" format:" + format);

                if (Misc.equals(type, "enum")) {
                    type = "enumeration";
                }
                format = Seesv.getDbProp(props, id, i, "format", format);
                if (format != null) {
                    format = Utils.convertDateFormat(format.replaceAll("_space_", " "));
                }

                attrs.append(" type=\"" + type + "\"");

		

                String enumeratedValues = Seesv.getDbProp(props, id, i,
							  "enumeratedValues", null);
                if (enumeratedValues != null) {
                    attrs.append(" enumeratedValues=\"" + enumeratedValues
                                 + "\"");
                }

                if (Misc.equals(type, "date")) {
                    if (format == null) {
                        format = "yyyy-MM-dd";
                    }
                    attrs.append(" format=\"" + format + "\" ");
                }

		if(searchable)
		    attrs.append(" searchable=\"" + "true" + "\" ");
		else
		    attrs.append(" searchable=\"" + "false" + "\" ");		    
                if (type.equals("double") || type.equals("integer")) {
                    if (chartable) {
                        attrs.append(" chartable=\"" + "true" + "\" ");
                    } else {
                        attrs.append(" chartable=\"" + "false" + "\" ");
                    }
                }
                if (debug) {
                    writer.println(StringUtil.padLeft(id, 20) + " " + attrs
                                   + "  sample:" + sample);
                }
                String field;

                if (justFields) {
                    field = id;
                } else {
                    field = id + "[" + attrs + "] ";
                }
                if (i == 0) {
                    if (toStdOut) {
                        field = "fields=" + field;
                    } else {
                        field = "#fields=" + field;
                    }
                }
                values.add(field);
            }

            if (debug) {
                System.err.println("header values:" + values);
            }
            Processor nextProcessor = getNextProcessor();
            firstRow.setValues(values);
            if (nextProcessor != null) {
                try {
                    if (debug) {
                        System.err.println(
					   "addheader: telling nextProcessor to handle row:"
					   + firstRow);
                    }
                    nextProcessor.handleRow(ctx, firstRow);
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }
            }
            firstRow = null;

            if (debug) {
                System.err.println("addheader: returning row:" + row);
            }

            return row;

        }
    }

    public static class ColumnPercenter extends Converter {

        public ColumnPercenter(List<String> cols) {
            super(cols);
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            List          values  = row.getValues();
            double        total   = 0;
            int           cnt     = 0;
            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                String s = values.get(idx).toString().trim();
                double v = (s.length() == 0)
		    ? 0
		    : Double.parseDouble(s);
                total += v;
                cnt++;
            }
            for (Integer idx : indices) {
                String s = values.get(idx).toString().trim();
                double v = (s.length() == 0)
		    ? 0
		    : Double.parseDouble(s);
                if (total != 0) {
                    values.set(idx, v / total);
                } else {
                    values.set(idx, Double.NaN);
                }
            }

            return row;
        }

    }

    public static class ColumnIncrease extends Converter {

        int step;

        /* */

        List<Double> values = new ArrayList<Double>();

        public ColumnIncrease(String col, int step) {
            super(col);
            this.step = step;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            int col = getIndex(ctx);
            if (rowCnt++ == 0) {
                add(ctx, row, row.get(col) + " increase");

                return row;
            }
            double v = Double.parseDouble(row.get(col).toString());
            if (values.size() < step) {
                add(ctx, row,  Double.valueOf(Double.NaN));
            } else {
                double pastValue = values.get(0);
                values.remove(0);
                double increase = 0;
                // 20 30
                if (pastValue == 0) {
                    add(ctx, row, Double.valueOf(Double.NaN));
                } else {
                    double diff = v - pastValue;
                    increase = diff / pastValue;
                    //              System.out.println("x:" + v +" " + pastValue +"  diff:" + diff +" i:" + increase);
                    add(ctx, row, Double.valueOf(increase));
                }
            }
            values.add(v);

            return row;
        }

    }

    public static class ColumnDiff extends Converter {

        int step;

        /* */

        List<Double> values = new ArrayList<Double>();

        public ColumnDiff(String col, int step) {
            super(col);
            this.step = step;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            int col = getIndex(ctx);
            if (rowCnt++ == 0) {
                add(ctx, row, row.get(col) + " difference");
                return row;
            }
            double v = Double.parseDouble(row.get(col).toString());
            if (values.size() < step) {
                add(ctx, row,  Double.valueOf(Double.NaN));
            } else {
                double pastValue = values.get(0);
                values.remove(0);
                double increase = 0;
		double diff = v - pastValue;
		add(ctx, row, Double.valueOf(diff));
            }
            values.add(v);

            return row;
        }

    }

    public static class Ranges extends Converter {

        String name;

        double start;

        double size;

        public Ranges(TextReader ctx, String col, String name, double start,
                      double size) {
            super(col);
            this.name  = name;
            this.start = start;
            this.size  = size;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            int col = getIndex(ctx);
            if (rowCnt++ == 0) {
                add(ctx, row, name);

                return row;
            }
            double v = Double.parseDouble(row.get(col).toString());
            if (Double.isNaN(v)) {
                add(ctx, row, "");
            } else if (v < start) {
                add(ctx, row, "<" + start);
            } else {
                double offset  = v - start;
                int    buckets = (int) (offset / size);
                double s       = start + buckets * size;
                double e       = s + size;
                String ss      = "" + (((int) s) == s
                                       ? (int) s
                                       : s);
                String se      = "" + (((int) e) == e
                                       ? (int) e
                                       : e);
                add(ctx, row, Utils.format(s) + "-" + Utils.format(e));
            }

            return row;
        }

    }

    public static class And extends Converter {

        private String name;

        public And(String name, List<String> cols) {
            super(cols);
            this.name = name;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                row.add(name);

                return row;
            }
            List<Integer> indices = getIndices(ctx);
            boolean       r       = true;
            for (int index : indices) {
                if ((index >= 0) && (index < row.size())) {
                    if ( !getBoolean(row.getString(index))) {
                        r = false;

                        break;
                    }
                }
            }
            row.add("" + r);

            return row;
        }

    }

    public static class Or extends Converter {

        private String name;

        public Or(String name, List<String> cols) {
            super(cols);
            this.name = name;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                row.add(name);

                return row;
            }
            List<Integer> indices = getIndices(ctx);
            boolean       r       = false;
            for (int index : indices) {
                if ((index >= 0) && (index < row.size())) {
                    String v = row.getString(index);
                    if (v == null) {
                        v = "false";
                    }
                    if (getBoolean(row.getString(index))) {
                        r = true;

                        break;
                    }
                }
            }
            row.add("" + r);

            return row;
        }

    }

    public static class Not extends Converter {

        private String name;

        public Not(String name, String col) {
            super(col);
            this.name = name;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                row.add(name);

                return row;
            }
            boolean b = getBoolean(row.getString(getIndex(ctx)));
            row.add("" + !b);

            return row;
        }

    }

    public static class ColumnAverage extends Converter {

        /* */

        public static final int MA = 0;

        /* */

        int what;

        /* */

        int period;

        /* */

        List<List<Double>> values = new ArrayList<List<Double>>();

        /* */

        String label;

        public ColumnAverage(int what, List<String> cols, int period,
                             String label) {
            super(cols);
            this.what   = what;
            this.period = period;
            this.label  = label;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            List<Integer> indices = getIndices(ctx);
            if (rowCnt++ == 0) {
                for (int i = 0; i < indices.size(); i++) {
                    values.add(new ArrayList<Double>());
                    add(ctx, row, row.get(indices.get(i)) + " " + label);
                }

                return row;
            }

            for (int i = 0; i < indices.size(); i++) {
                int          index = indices.get(i);
                List<Double> nums  = values.get(i);
                double       v =
                    Double.parseDouble(row.get(index).toString());
                nums.add(v);
                if (nums.size() > period) {
                    nums.remove(0);
                }
                double total = 0;
                int    cnt   = 0;
                for (int j = 0; j < nums.size(); j++) {
                    if ( !Double.isNaN(nums.get(j))) {
                        cnt++;
                        total += nums.get(j);
                    }
                }
                double average = (cnt == 0)
		    ? Double.NaN
		    : total / cnt;
                add(ctx, row, average);
            }

            return row;
        }

    }

    public static class Checker extends Converter {

	String strict;

        public Checker(List<String> cols, String strict) {
            super(cols);
	    this.strict =strict;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            List<Integer> indices = getIndices(ctx);
            if (rowCnt++ == 0) {
		return row;
            }
	    boolean anyBad = false;
            for (int i:indices) {
		if(i<0 || i>= row.size()) continue;
		String v = row.getString(i);
		try {
		    if(strict.equals("ramadda")) {
			if(Utils.isStandardMissingValue(v)) {
			    anyBad =true;
			}
			continue;
		    }
		    double d = Double.parseDouble(v);
		} catch(Exception exc) {
		    anyBad = true;
		    row.set(i,"bad:" +v);
		}
	    }
	    if(!anyBad) return null;
	    return row;
	}
    }

    public static class ColumnFunc extends Converter {

        String code;

        List<String> names;

        Row headerRow;

        org.mozilla.javascript.Context cx =
            org.mozilla.javascript.Context.enter();

        org.mozilla.javascript.Scriptable scope =
            cx.initSafeStandardObjects();

        org.mozilla.javascript.Script script;

        public ColumnFunc(TextReader ctx, String js, String names,
                          String code) {
            this.names = Utils.split(names, ",", true, true);
            if (code.indexOf("return ") >= 0) {
                code = "function _tmp() {" + code + "}\n_tmp()";
            }
            this.code = code;
            try {
                cx    = org.mozilla.javascript.Context.enter();
                scope = cx.initSafeStandardObjects();
                eval("var globalCache = {};");
                if (js.trim().length() > 0) {
                    eval(js);
                }
                String testScript =
                    "print(java.lang.System.getProperty(\"java.home\"));"
                    + "print(\"Create file variable\");"
                    + "var File = Java.type(\"java.io.File\");";
                //                eval(testScript);
                script = cx.compileString(code, "code", 0, null);
            } catch (Exception exc) {
                fatal(ctx, "Error evaluation function:" + code, exc);
                //                throw new RuntimeException(exc);
            }
        }

        private Object eval(String s) throws Exception {
            if (s == null) {
                return script.exec(cx, scope);
            }

            return cx.evaluateString(scope, s, "<cmd>", 1, null);
        }

        private void put(String name, Object value) throws Exception {
            scope.put(name, scope, value);
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            rowCnt++;
            debug("processRow");
            if (rowCnt == 1) {
                headerRow = new Row(row);
                for (String s : names) {
                    row.add(s);
                }

                return row;
            }
            List<String> vars = null;
            try {
                List hdr = headerRow.getValues();
                if (ctx.getDebug()) {
                    ctx.printDebug("function:" + code);
                    vars = new ArrayList<String>();
                }
                for (int i = 0; i < hdr.size(); i++) {
                    if (i >= row.size()) {
                        continue;
                    }
                    Object o   = row.get(i);
                    String var = hdr.get(i).toString();
                    var = Utils.makeID(var.toLowerCase());
                    if (ctx.getDebug()) {
                        ctx.printDebug("\tvar:" + var);
                    }
                    put("_" + var, o);
                    put("_" + var + "_idx", i);
                    put("_col" + i, o);
                    if (vars != null) {
                        vars.add("_" + var);
                    }
                }
                put("_header", hdr);
                put("_values", row.getValues());
                put("_rowidx", rowCnt - 1);
                Object o = eval(null);
                if (names.size() == 0) {
                    if (o == null) {
                        return row;
		    }
                    String s = o.toString();
                    if (s.equals("false")) {
                        return null;
                    }

                    return row;
                }
                if (o == null) {
                    return null;
                }

                row.add(org.mozilla.javascript.Context.toString(o));

                //              System.err.println("func row:" + row);
                return row;
            } catch (Exception exc) {
                fatal(ctx,
                      "Error evaluating function:" + code + "\n\theader:"
                      + headerRow + ((vars != null)
                                     ? "\n\tvars:" + Utils.join(vars, ",")
                                     : ""), exc);

                //                throw new RuntimeException(exc);
                return row;
            }
        }

    }

    public static class ColumnOperator extends Converter {

        public ColumnOperator() {}

        @Override
        public Row processRow(TextReader ctx, Row row) {
            List   values    = row.getValues();
            List   newValues = new ArrayList();
            double total     = 0;
            int    cnt       = 0;
            for (int i = 0; i < values.size(); i++) {
                String s = values.get(i).toString();
                if (s.trim().length() == 0) {
                    continue;
                }
                if (s.matches("(-|\\+)?[\\d\\.]+")) {
                    double d = Double.parseDouble(s);
                    total += d;
                } else {}
            }

            for (int i = 0; i < values.size(); i++) {
                String s = values.get(i).toString();
                if (s.trim().length() == 0) {
                    continue;
                }
                if (s.matches("(-|\\+)?[\\d\\.]+")) {}
                else {
                    newValues.add(s);
                }
            }
            newValues.add(Double.valueOf(total));
            row.setValues(newValues);

            return row;
        }

    }

    public static class Changer extends Converter {

        boolean isRegex;

        List<String[]> patterns = new ArrayList<String[]>();

        public Changer(TextReader ctx, List<String> cols, String pattern,
                       String value) {
            super(cols);
            if (pattern.startsWith("file:")) {
                String file = pattern.substring("file:".length());
                if ( !IO.okToReadFrom(file)) {
                    fatal(ctx, "Cannot read file:" + file);
                }
                try {
                    init(file);
                } catch (Exception exc) {
                    fatal(ctx, "Reading file:" + file, exc);
                }
                this.isRegex = true;
            } else {
		String convertedPattern = Utils.convertPattern(pattern);
                this.isRegex = StringUtil.containsRegExp(convertedPattern);
		//		System.err.println("orig:" + pattern +" converted:" + convertedPattern+" isReg:" + this.isRegex );
                patterns.add(new String[] { convertedPattern,
					    value });
            }
        }

        private void init(String file) throws Exception {
            String       contents = IO.readContents(file);
            List<String> lines    = Utils.split(contents, "\n");
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if ((line.length() == 0) || line.startsWith("#")) {
                    continue;
                }
                List<String> toks;
                if (line.indexOf("::") >= 0) {
                    toks = Utils.splitUpTo(line, "::", 2);
                } else {
                    toks = Utils.splitUpTo(line, ",", 2);
                }
                String pattern = toks.get(0);
                patterns.add(new String[] { Utils.convertPattern(pattern),
					    (toks.size() > 1)
					    ? toks.get(1)
					    : "" });
                //              System.out.println(Utils.convertPattern(pattern));
            }

            Collections.sort(patterns, new Comparator() {
		    public int compare(Object o1, Object o2) {
			String[] t1 = (String[]) o1;
			String[] t2 = (String[]) o2;

			return t2[0].length() - t1[0].length();
		    }
		});
            //      for(String[]t:patterns)System.err.println(t[0]);
        }

        String change(TextReader ctx, Row row, String s) {
            String os = s;
            //      System.out.println("change:" + s);
            for (String[] tuple : patterns) {
                String pattern = tuple[0];
                String value   = tuple[1];
                if (pattern.startsWith("one:")) {
                    pattern = pattern.substring(4);
                    s       = s.replaceFirst(pattern, value);
                } else {
		    String tmp;
		    if(isRegex) {
			tmp = s.replaceAll(pattern, value);
		    } else {
			tmp = s.replace(pattern, value);
		    }
		    //		    System.out.println(rowCnt + " V:" + value +" string:" + s +" to:" + tmp);
		    s = tmp;
                }
                //              System.out.println("\tchange:" + s);
                //              if(!os.equals(s)) break;
            }

            //      if(os.equals(s))System.out.println("\tno changes:" + os +" s:" + s);
            //      else System.out.println("\tchanged:" + os +" s:" + s);          
            return s;
        }

    }

    public static class ColumnChanger extends Changer {

        public ColumnChanger(TextReader ctx, List<String> cols,
                             String pattern, String value) {
            super(ctx, cols, pattern, value);
        }

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
                int index = idx.intValue();
                if ((index >= 0) && (index < row.size())) {
                    String s = row.getString(index).trim();
                    s = change(ctx, row, s);
                    row.set(index, s);
                }
            }

            return row;
        }

    }

    public static class CleanWhitespace extends Converter {

        public CleanWhitespace(List<String> cols) {
            super(cols);
        }

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
                int index = idx.intValue();
		if(!row.indexOk(index)) continue;
		String s = row.getString(index).trim();
		s = s.replaceAll("\n"," ").replaceAll("\r"," ").replaceAll("\\s\\s+"," ");
		row.set(index, s);
            }

            return row;
        }

    }

    public static class ColumnReplacer extends Converter {

        String with;

        public ColumnReplacer(TextReader ctx, List<String> cols,
                              String with) {
            super(cols);
            this.with = with;
        }

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
                int index = idx.intValue();
                if ((index >= 0) && (index < row.size())) {
                    String s = row.getString(index).trim();
                    s = with.replace("{value}", s);
                    row.set(index, s);
                }
            }

            return row;
        }

    }

    public static class Ascii extends Converter {

        private String value;

        public Ascii(List<String> cols, String value) {
            super(cols);
            this.value = value;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index >= 0) && (index < row.size())) {
                    String s = row.getString(index).trim();
                    s = s.replaceAll("[^\\x00-\\x7F]", value);
                    row.set(index, s);
                }
            }

            return row;
        }

    }

    public static class Cropper extends Converter {

        private List<String> patterns;

        public Cropper(List<String> cols, List<String> patterns) {
            super(cols);
            this.patterns = new ArrayList<String>();
            for (String s : patterns) {
                this.patterns.add(s.replaceAll("_comma_", ","));
            }
        }

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
                if ((idx >= 0) && (idx < row.size())) {
                    String s = row.getString(idx).trim();
                    for (String pattern : patterns) {
                        s = Utils.prune(s, pattern);
                    }
                    row.set(idx, s);
                } else {
                    //              System.out.println("bad idx:" + idx +" " + row.size());
                }
            }

            return row;
        }

    }

    public static class ColumnEndsWith extends Converter {

        private String value;

        public ColumnEndsWith(List<String> cols, String value) {
            super(cols);
            this.value = value;
        }

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
                int index = idx.intValue();
                if ((index >= 0) && (index < row.size())) {
                    String s = row.getString(index).trim();
                    if ( !s.endsWith(value)) {
                        s = s + value;
                    }
                    row.set(index, s);
                }
            }

            return row;
        }

    }

    public static class ColumnExtracter extends Converter {

        /* */

        private int col = -1;

        private String scol;

        /* */

        private String pattern;

        /* */

        private String replace;

        /* */

        private String name;

        public ColumnExtracter(String col, String pattern, String replace,
                               String name) {
            this.scol    = col;
            this.pattern = pattern;
            this.replace = replace;
            this.name    = name;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            //Don't process the first row
            if (rowCnt++ == 0) {
                this.col = getColumnIndex(ctx, scol);
                add(ctx, row, name);

                return row;
            }
            String value    = row.getString(col);
            String newValue = StringUtil.findPattern(value, pattern);
            //      System.err.println("value:" + value);
            //      System.err.println("pattern:" + pattern);
            //      System.err.println("NV:" + newValue);
            if (newValue == null) {
                newValue = "";
            }
            add(ctx, row, newValue);
            if ( !replace.equals("none")) {
                value = value.replaceAll(pattern, replace);
            }
            row.set(col, value);

            return row;
        }

    }

    public static class HtmlExtracter extends Converter {
	private String scol;

	private int col;

        private List<String> names;

        private Pattern pattern;

        public HtmlExtracter(String col, List<String> names, String pattern) {
            this.scol    = col;
	    this.pattern  = Pattern.compile(pattern, Pattern.DOTALL | Pattern.MULTILINE);
            //this.pattern  = Pattern.compile(pattern);
            this.names    = names;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                this.col = getColumnIndex(ctx, scol);
		for(String name: names)
		    add(ctx, row, name);
                return row;
            }
            String url    = row.getString(col);
	    try {
		String html = IO.readContents(url,(String)null);
		if(html==null) {
		    for(String name: names)
			add(ctx, row, "");
		    return row;
		}
		//		html = html.replace("\n"," ").replace("\r"," ");
		Matcher matcher = pattern.matcher(html);
		if (!matcher.find()) {
		    for(String name: names)
			add(ctx, row, "");
		    return row;
		}

		for(int i=0;i<names.size();i++) {
		    add(ctx, row, matcher.group(i+1));
		}
	    } catch(Exception exc) {
		System.err.println("error extracting html:" + url);
		throw new RuntimeException(exc);
	    }

	    //            add(ctx, row, newValue);
            return row;
        }

    }

    public static class HtmlInfo extends Converter {
	private String scol;

	private int col;

        public HtmlInfo(String col) {
            this.scol    = col;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                this.col = getColumnIndex(ctx, scol);
		row.add("title");
		row.add("description");
		row.add("keywords");
		row.add("icon");
                return row;
            }
	    if(!row.indexOk(this.col)) {
		return row;
	    }
            String url    = row.getString(col);
	    try {
		String html=null;
		File cacheFile = null;
		if(Seesv.getTmpCacheDir()!=null) {
		    cacheFile = new File(Seesv.getTmpCacheDir(),"cached_" + Utils.makeID(url));
		    if(!cacheFile.exists()) {
			cacheFile = new File(Seesv.getTmpCacheDir(),"cached_" + Utils.makeID(url.replace("http:","https:")));
		    }
		    if(cacheFile.exists()) {
			html = IO.readContents(cacheFile);
			if(!url.startsWith("https:") && cacheFile.getName().startsWith("cached_https_")) {
			    url = url.replace("http:","https:");
			}
		    }
		}
		if(html==null) {
		    try {
			if(url.startsWith("http:")) {
			    //Try https first
			    try {
				html = IO.readUrl(url.replace("http:","https:"));
			    } catch(Exception exc) {
			    }
			}
			if(html==null)
			    html = IO.readUrl(url);
		    } catch(Exception exc) {
			System.err.println("error reading url:" + url +"\n" + exc);
			html = "";
		    }
		    if(cacheFile!=null && html!=null) {
			try(OutputStream fos = new FileOutputStream(cacheFile)) {
			    IOUtil.writeTo(new ByteArrayInputStream(html.getBytes()),fos);
			}

		    }
		}
		if(html==null) {
		    row.add("","","","");
		    return row;
		}
		html  = html.replace("\r\n","\n");
		/*
		  int idx1 = html.indexOf("<head");
		  int idx2= html.indexOf("</head");		
		  if(idx1>=0 && idx2>=idx1) {
		  html = html.substring(idx1,idx2);
		  } else {
		  int idx = html.indexOf("<body");
		  if(idx>=0) html = html.substring(0,idx);
		  }
		*/
		Pattern titlePattern  = Pattern.compile("<title[^>]*>(.*?)</title",
							Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);

		Matcher titleMatcher  = titlePattern.matcher(html);
		if(titleMatcher.find()) {
		    String title = titleMatcher.group(1).replace("[\n\r]+"," ").trim();		    
		    String tmp = title;
		    title = title.replaceAll("(?i)(homepage|home page|home)[\\s\\-\\|]*","").trim();
		    title = title.replaceAll("^[\\|-]+","");
		    title = title.replaceAll("[\\|-]+$","");
		    title = title.replace("&amp;","&").replace("&#8211;","-");
		    title  = title.trim();
		    if(title.equalsIgnoreCase("page")) title = tmp;
		    add(ctx, row, title);
		    //		    add(ctx, row, title);
		} else {
		    add(ctx, row, "");		    
		}

		Pattern metaPattern  = Pattern.compile("<meta([^>]+>)",
						       Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
		Pattern metaPattern2  = Pattern.compile("name=\"description\"[^>]+content=\"([^\"]+)\"",
							Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
		Pattern contentPattern  = Pattern.compile("content=\"([^\"]+)\"",
							  Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);		
		Matcher metaMatcher = metaPattern.matcher(html);
		String desc = "";
		String keywords = "";		
		List<String> metas = new ArrayList<String>();
		while(metaMatcher.find()) {
		    metas.add(metaMatcher.group(1));
		}

		for(String meta:metas) {
		    if(meta.toLowerCase().indexOf("keywords")>=0) {
			Matcher contentMatcher = contentPattern.matcher(meta);		    
			if(contentMatcher.find()) {
			    keywords=contentMatcher.group(1);
			    keywords= keywords.replaceAll("[\n\r]+",",").replaceAll(",[,\\s]+",",");
			}
		    }
		    Matcher matcher = metaPattern2.matcher(meta);		    
		    if(matcher.find()) {
			desc = matcher.group(1);
			break;
		    }
		}
		add(ctx, row, desc);
		add(ctx, row, keywords);

		Pattern linkPattern  = Pattern.compile("<link([^>]+>)",
						       Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
		Pattern relPattern  = Pattern.compile("rel *= *(?:\"|')([^\"']+)(\"|')");
		Pattern hrefPattern  = Pattern.compile("href *= *(?:\"|')([^\"']+)(\"|')");
		Pattern sizesPattern  = Pattern.compile("sizes *= *(?:\"|')([^\"']+)(\"|')");				
		Pattern linkPattern2  = Pattern.compile("name=\"description\"[^>]+content=\"([^\"]+)\"",
							Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
		Matcher linkMatcher = linkPattern.matcher(html);
		String image32 = null;
		String image = null;
		while(linkMatcher.find()) {
		    String link =  linkMatcher.group(1);
		    Matcher relMatcher = relPattern.matcher(link);		    
		    if(!relMatcher.find()) continue;
		    String rel = relMatcher.group(1);
		    if(!rel.equals("icon") && !rel.equals("shortcut icon") &&  !rel.equals("apple-touch-icon")) continue;
		    Matcher hrefMatcher = hrefPattern.matcher(link);		    
		    if(!hrefMatcher.find()) continue;
		    image = hrefMatcher.group(1);
		    Matcher sizesMatcher = sizesPattern.matcher(link);		    
		    if(sizesMatcher.find()) {
			String sizes = hrefMatcher.group(1);
			if(sizes.indexOf("32x32")>=0) {
			    image32 = image;
			    break;
			}
		    }
		}

		if(image32==null) image32 = image;
		if(!Utils.stringDefined(image32)) {
		    Pattern metaPattern3  = Pattern.compile("content=\"([^\"]+)\"",
							    Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
		    for(String meta: metas) {
			if(meta.indexOf("og:image")<0) continue;
			Matcher imageMatcher = metaPattern3.matcher(meta);
			if(imageMatcher.find()) {
			    image32 = imageMatcher.group(1);
			    break;
			}
		    }
		}

		if(Utils.stringDefined(image32)) {
		    URL tmp = new URL(new URL(url),image32);
		    image32 = tmp.toString();
		    //		    System.err.println("IMAGE:" + image32);
		} else {
		    image32 = "";
		    //		    System.err.println("NULL IMAGE:" + url +" " + cacheFile);
		}
		add(ctx, row, image32);		
		//		add(ctx, row, matcher.group(0+2));		
	    } catch(Exception exc) {
		throw new RuntimeException(exc);
	    }
            return row;
        }

    }

    public static class CheckMissing extends Converter {
	private String scol;

	private int col;
	private String replace;

        public CheckMissing(String col, String replace) {
            this.scol    = col;
	    this.replace =replace;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                this.col = getColumnIndex(ctx, scol);
                return row;
            }
	    if(!row.indexOk(this.col)) {
		return row;
	    }
            String url    = row.getString(col);
	    if(!Utils.stringDefined(url)) {
		row.set(col,replace);
		return row;
	    }

	    try {
		URL _url = new URL(url);
		HttpURLConnection connection  = (HttpURLConnection)_url.openConnection();
		int               response = connection.getResponseCode();
		//Check for redirect. this only does one level of redirect
		if ((response == HttpURLConnection
		     .HTTP_MOVED_TEMP) || (response == HttpURLConnection
					   .HTTP_MOVED_PERM) || (response == HttpURLConnection
								 .HTTP_SEE_OTHER)) {
		    row.set(col,  connection.getHeaderField("Location"));
		} else if(response!=HttpURLConnection.HTTP_OK) {
		    row.set(col,  connection.getHeaderField("Location"));
		}
		connection.getInputStream().close();
	    } catch(Exception exc) {
		row.set(col,replace);
	    }
            return row;
        }

    }

    public static class UrlArg extends Converter {

        private String name;

        private int index = -1;

        public UrlArg(String col, String name) {
            super(col);
            this.name = name;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            //Don't process the first row
            if (rowCnt++ == 0) {
                row.add(name);

                return row;
            }
            if (index == -1) {
                index = getIndex(ctx);
            }
            try {
                String arg = Utils.getUrlArg(row.getString(index), name);
                if (arg == null) {
                    arg = "";
                }
                row.add(arg);
            } catch (Exception exc) {
                row.add("");
            }

            return row;
        }

    }

    public static class UrlEncode extends Converter {

        public UrlEncode(List<String> cols) {
            super(cols);
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
		for(int idx:getIndices(ctx)) {
		    row.add(row.getString(idx)+" encoded");
		}
                return row;
            }
	    for(int idx:getIndices(ctx)) {
		try {
		    row.add(java.net.URLEncoder.encode(row.getString(idx), "UTF-8"));
		} catch(Exception exc) {
		    throw new RuntimeException(exc);
		}
	    }
            return row;
        }
    }

    public static class XmlEncode extends Converter {

        public XmlEncode(List<String> cols) {
            super(cols);
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
		return row;
            }
	    for(int idx:getIndices(ctx)) {
		try {
		    if(!row.indexOk(idx)) continue;
		    String v  =row.getString(idx);
		    row.set(idx,XmlUtil.encodeString(v).replace("\n","\\n"));
		} catch(Exception exc) {
		    throw new RuntimeException(exc);
		}
	    }
            return row;
        }
    }

    public static class UrlDecode extends Converter {

        public UrlDecode(List<String> cols) {
            super(cols);
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
		for(int idx:getIndices(ctx)) {
		    row.add(row.getString(idx)+" decoded");
		}
                return row;
            }
	    for(int idx:getIndices(ctx)) {
		try {
		    row.add(java.net.URLDecoder.decode(row.getString(idx), "UTF-8"));
		} catch(Exception exc) {
		    throw new RuntimeException(exc);
		}
	    }
            return row;
        }
    }

    public static class Truncater extends Converter {

        private int col;

        private int length;

        private String suffix;

        public Truncater(int col, int length, String suffix) {
            this.col    = col;
            this.length = length;
            this.suffix = suffix;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            String value = row.get(col).toString();
            if (value.length() > length) {
                value = value.substring(0, length - 1) + suffix;
            }
            row.set(col, value);

            return row;
        }

    }

    public static class RowChanger extends Changer {

        private HashSet<Integer> rows;

        public RowChanger(TextReader ctx, List<Integer> rowList,
                          List<String> cols, String pattern, String value) {

            super(ctx, cols, pattern, value);
            rows = new HashSet<Integer>();
            for (int row : rowList) {
                rows.add(row);
            }
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if ( !rows.contains(rowCnt++)) {
                if ( !ctx.getAllData()) {
                    return row;
                }
            }
            List<Integer> indices = getIndices(ctx);
            for (int index : indices) {
		if(!row.indexOk(index)) continue;
                String s     = row.getString(index);
                String changed = change(ctx, row, s);
                row.set(index, changed);
            }
	    //	    System.err.println(row);
            return row;
        }

    }

    public static class Shifter extends Converter {

        private HashSet<Integer> rows;

        private int column;

        private int count;

        public Shifter(List<Integer> rowList, int column, int count) {
            rows = new HashSet<Integer>();
            for (int row : rowList) {
                rows.add(row);
            }
            this.column = column;
            this.count  = count;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if ( !rows.contains(rowCnt++)) {
                return row;
            }
            //a,b,c,d,e
            //a,b,c,d,e,X
            //a,b,c,d,e,X

            List values    = row.getValues();
            List newValues = new ArrayList();
            for (int i = 0; i < values.size(); i++) {
                newValues.add(values.get(i));
            }
            for (int i = 0; i < count; i++) {
                newValues.add(column, "");
            }

            return new Row(newValues);
        }

    }

    public static class RowMerger extends Converter {

        /* */

        HashSet<Integer> rows = new HashSet<Integer>();

        /* */

        private String delimiter;

        /* */

        private String close;

        /* */

        private Row firstRow;

        public RowMerger(List<Integer> rows, String delimiter, String close) {
            this.delimiter = delimiter;
            this.close     = close;
            for (int i = 0; i < rows.size(); i++) {
                this.rows.add(rows.get(i));
            }
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rows.size() == 0) {
                return row;
            }
            int rowNumber = rowCnt++;
            if ( !rows.contains(rowNumber)) {
                return row;
            }
            rows.remove(rowNumber);
            if (firstRow == null) {
                firstRow = row;
                return null;
            }
            for (int i = 0; i < row.size(); i++) {
                String s  = (i < row.size())
		    ? row.getString(i)
		    : "";
                String ss = (i < firstRow.size())
		    ? firstRow.getString(i)
		    : "";
		if(delimiter.equals("+")) {
		    try {
			double d1 = Double.parseDouble(s);
			double d2 = Double.parseDouble(ss);		    
			firstRow.set(i, ""+(d1+d2));
		    } catch(NumberFormatException nfe) {
		    }
		} else {
		    ss = ss + delimiter + s;
		    firstRow.set(i, ss);
		}
            }
            if (rows.size() == 0) {
                for (int i = 0; i < firstRow.size(); i++) {
                    firstRow.set(i, firstRow.getString(i) + close);
                }
                row      = firstRow;
                firstRow = null;

                return row;
            }

            return null;
        }

    }

    public static class ColumnDebugger extends Converter {

        /* */

        private String pattern;

        public ColumnDebugger(List<String> cols, String pattern) {
            super(cols);
            this.pattern = pattern;
        }

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
                int index = idx.intValue();
                if ((index >= 0) && (index < row.size())) {
                    String s = row.getString(index);
                    if ((pattern.length() > 0) && (s.indexOf(pattern) < 0)) {
                        continue;
                    }
                    System.err.println("column: " + idx + "=" + s);
                }
            }

            return row;
        }

    }

    public static class ColumnMapper extends Converter {

        private String name;

	private boolean replace;

        private Hashtable<String, String> map = new Hashtable();
	private List<String> toks;

        public ColumnMapper(List<String> cols, String name,
			    boolean replace,
                            List<String> toks) {
            super(cols);
            this.name = name;
	    this.replace = replace;
	    this.toks = toks;
            for (int i = 0; i < toks.size(); i += 2) {
                map.put(toks.get(i), toks.get(i + 1));
            }
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                if (name.length() > 0) {
                    row.getValues().add(name);
                }

                return row;
            }
            List<Integer> indices = getIndices(ctx);
            String        na      = map.get("_na_");
            if (na == null) {
                na = "";
            }
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index >= 0) && (index < row.size())) {
                    String s        = row.getString(index);
		    String newValue=null;
		    if(replace) {
			newValue  = s;
			for(int i=0;i<toks.size();i+=2) {
			    newValue = newValue.replace(toks.get(i),toks.get(i+1));
			}
		    } else {
			newValue = map.get(s);
		    }

                    if (name.length() > 0) {
                        if (newValue != null) {
                            row.getValues().add(newValue);
                        } else {
                            row.getValues().add(na);
                        }
                    } else {
                        if (newValue != null) {
                            row.set(index, newValue);
                        }
                    }
                }
            }

            return row;
        }

    }

    public static class ColumnSplitter extends Converter {

        /* */

        private String delimiter;

        /* */

        private List<String> names;

        public ColumnSplitter(String col, String delimiter,
                              List<String> names) {
            super(col);
            this.delimiter = delimiter;
            this.names     = names;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            int index = getIndex(ctx);
            int cnt   = 0;
            if (rowCnt++ == 0) {
                for (String name : names) {
                    row.insert(index + 1 + (cnt++), name);
                }

                return row;
            }
            if ((index < 0) || (index >= row.size())) {
                return row;
            }
            //            row.remove(index);
            int          colOffset = 0;
            List<String> toks      = Utils.split(row.get(index), delimiter);
            while (toks.size() < names.size()) {
                toks.add("");
            }
            for (String tok : toks) {
                row.insert(index + 1 + (colOffset++), tok);
            }

            return row;
        }

    }

    public static class ColumnConcatter extends Converter {

        /* */

        private String delimiter;

        /* */

        private String name;

        /* */

        private boolean inPlace;

        public ColumnConcatter(List<String> indices, String delimiter,
                               String name, boolean inPlace) {
            super(indices);
            this.delimiter = delimiter;
            this.name      = name;
            this.inPlace   = inPlace;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            List<Integer> indices = getIndices(ctx);
            if (rowCnt++ == 0) {
                if (name.length() > 0) {
                    if (inPlace) {
                        row = filterValues(ctx, row);
                        row.insert(indices.get(0), name);
                    } else {
                        row.getValues().add(name);
                    }

                    return row;
                }

                return null;
            }

            StringBuilder sb  = new StringBuilder();
            int           cnt = 0;
            for (Integer idx : indices) {
                int i = idx.intValue();
                if (cnt++ > 0) {
                    sb.append(delimiter);
                }
                if (i >= row.size()) {
                    sb.append("");
                } else {
                    sb.append(row.getString(i));
                }
            }

            //            if(rowCnt<5)
            //                System.err.println("combine:" + sb);
            if (inPlace) {
                row = filterValues(ctx, row);
                row.insert(indices.get(0), sb.toString());
            } else {
                row.getValues().add(sb.toString());
            }

            return row;
        }

    }

    public static class ColumnMerger extends Converter {

        private String name;

        private List<String> what;

        private boolean inPlace;

        public ColumnMerger(List<String> indices, String name, String what) {
            super(indices);
            this.what = Utils.split(what, ",", true, true);
            this.name = name;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            List<Integer> indices = getIndices(ctx);
            if (rowCnt++ == 0) {
                for (String what : this.what) {
                    row.getValues().add(name + " " + what);
                }

                return row;
            }

            double min   = Double.POSITIVE_INFINITY;
            double max   = Double.NEGATIVE_INFINITY;

            double total = 0;
            int    count = 0;
            for (Integer idx : indices) {
                int    i = idx.intValue();
                double v = Double.parseDouble(row.getString(i));
                if (Double.isNaN(v)) {
                    continue;
                }
                total += v;
                count++;
                min = Math.min(min, v);
                max = Math.max(max, v);
            }

            for (String op : what) {
                if (op.equals(OPERAND_SUM)) {
                    row.add(total);
                } else if (op.equals(OPERAND_MIN)) {
                    row.add((min == Double.POSITIVE_INFINITY)
                            ? "NaN"
                            : Double.toString(min));
                } else if (op.equals(OPERAND_MAX)) {
                    row.add((max == Double.NEGATIVE_INFINITY)
                            ? "NaN"
                            : Double.toString(max));
                } else if (op.equals(OPERAND_AVERAGE)) {
                    if (count == 0) {
                        row.add("NaN");
                    } else {
                        row.add(total / count);
                    }
                } else {
                    fatal(ctx, "Unknown histogram operator:" + op);
                }
            }

            return row;
        }

    }

    /* */

    private static Properties genderProperties;

    public static class Genderizer extends Converter {

        /* */

        private boolean doneHeader = false;

        public Genderizer(String col) {
            super(col);
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            int  column = getIndex(ctx);
            List values = row.getValues();
            if ( !doneHeader) {
                values.add("Gender");
                doneHeader = true;

                return row;
            }
            String value = values.get(column).toString().toLowerCase().trim();
            int    index = value.indexOf(",");
            if (index >= 0) {
                value = value.substring(index + 1).trim();
            }
            index = value.indexOf(" ");
            if (index >= 0) {
                value = value.substring(0, index).trim();
            }
            if (genderProperties == null) {
                genderProperties = new Properties();
                try {
                    InputStream inputStream =
                        Utils.getInputStream(
					     "/org/ramadda/util/seesvgender.properties",
					     getClass());
                    genderProperties.load(inputStream);
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }
            }
            String gender = (String) genderProperties.get(value);
            if (gender == null) {
                gender = "na";
            } else if ( !(gender.equals("m") || gender.equals("f"))) {
                gender = "na";
            }
            values.add(gender);

            return row;
        }

    }

    public static class Denormalizer extends Converter {

        /* */

        private Hashtable map = new Hashtable();

        /* */

        int destCol;

        /* */

        String newColName;

        /* */

        String mode;

        /* */

        boolean doDelete;

        public Denormalizer(String mapFile, int col1, int col2, String col,
                            String newName, String mode) {
	    super(col);
            try {
                makeMap(mapFile, col1, col2);
                this.newColName = newName;
                this.mode       = mode;
                this.doDelete   = mode.endsWith("delete");
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

        private void makeMap(String filename, int col1, int col2)
	    throws Exception {
            BufferedReader br = new BufferedReader(
						   new InputStreamReader(
									 getInputStream(filename)));

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }
                if (line.startsWith("#")) {
                    continue;
                }
                List<String> toks = Utils.tokenizeColumns(line, ",");
		if(col1<toks.size() && col2<toks.size()) {
		    map.put(toks.get(col1), toks.get(col2).replaceAll("\"", ""));
		}
            }
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            List   values   = row.getValues();
            String newValue = null;
            if (rowCnt++ == 0) {
		destCol = getIndex(ctx);
                if (newColName.length() > 1) {
                    newValue = newColName;
                }
            } else {
                Object key = values.get(destCol);
                newValue = (String) map.get(key);
                if (doDelete && (newValue == null)) {
                    return null;
                }
            }

            if ((newValue == null) && mode.startsWith("add")) {
                newValue = "";
            }

            if (newValue != null) {
                //                if(newValue.indexOf(",")>=0) newValue = "\"" + newValue+"\"";
                if (mode.startsWith("replace")) {
                    values.set(destCol, newValue);
                } else {
                    values.add(destCol + 1, newValue);
                }
            }

            return row;
        }

    }

    public static class ColumnDeleter extends Converter {

        public ColumnDeleter(List<String> indices) {
            super(indices);
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            row = filterValues(ctx, row);

            return row;
        }
    }

    public static class ColumnScaler extends Converter {

        /* */

        private double delta1;

        /* */

        private double delta2;

        /* */

        private double scale;

        public ColumnScaler(List<String> cols, double delta1, double scale,
                            double delta2) {
            super(cols);
            this.delta1 = delta1;
            this.delta2 = delta2;
            this.scale  = scale;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
	    if(rowCnt++==0) return row;
            List<Integer> indices = getIndices(ctx);
            for (int index : indices) {
                if (!row.indexOk(index)) {
                    continue;
                }
		String s = row.get(index).toString();
                try {
                    double value =   Seesv.parseDouble(s);
                    row.set(index,
                            Double.valueOf((value + delta1) * scale
					   + delta2).toString());
                } catch (NumberFormatException nfe) {
		    System.err.println("Error converting:" +s + " in row:" + row);
		}
            }

            return row;
        }

    }

    public static class MakeNumber extends Converter {

        public MakeNumber(List<String> cols) {
            super(cols);
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                return row;
            }
            List<Integer> indices = getIndices(ctx);
            for (int index : indices) {
		if(row.indexOk(index)) {
		    String s = row.getString(index);
		    s = s.replaceAll(",","").replaceAll("\\s","");
		    try {
			double d = Double.parseDouble(s);
			if(d == (int)d) {
			    row.set(index,Integer.toString((int)d));
			} else {
			    row.set(index,Double.toString(d));
			}
		    } catch (NumberFormatException nfe) {
			row.set(index,"NaN");
		    }
		}
	    }		

            return row;
        }

    }

    public static class Decimals extends Converter {

        /* */

        private int tens;

        private int decimals;

        public Decimals(List<String> cols, int decimals) {
            super(cols);
            this.decimals = decimals;
            this.tens     = (int) Math.pow(10, decimals);
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            List<Integer> indices = getIndices(ctx);
            for (int i = 0; i < indices.size(); i++) {
                try {
                    int index = indices.get(i);
                    if ((index < 0) || (index >= row.size())) {
                        continue;
                    }
                    double value =
                        Double.parseDouble(row.get(index).toString());
                    if (decimals == 0) {
                        row.set(index, Integer.valueOf((int) value));
                    } else {
                        value = (double) Math.round(value * tens) / tens;
                        row.set(index, Double.valueOf(value));
                    }
                } catch (NumberFormatException nfe) {}
            }

            return row;
        }

    }

    public static class Fuzzer extends Converter {

        private int places;

        private int numRandomDigits;

        private int tens;

        public Fuzzer(List<String> cols, int places, int numRandomDigits) {
            super(cols);
            this.places          = places;
            this.numRandomDigits = numRandomDigits;
            this.tens            = (int) Math.pow(10, numRandomDigits);
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            List<Integer> indices = getIndices(ctx);
            for (int i = 0; i < indices.size(); i++) {
                try {
                    int index = indices.get(i);
                    if ((index < 0) || (index >= row.size())) {
                        continue;
                    }
                    double value =
                        Double.parseDouble(row.get(index).toString());
                    if (Double.isNaN(value)) {
                        continue;
                    }
                    int    digits  = (int) (Math.random() * tens);
                    String sdigits = "" + digits;
                    if (sdigits.length() < numRandomDigits) {
                        sdigits = StringUtil.padRight(sdigits,
						      numRandomDigits, "0");
                        digits = Integer.parseInt(sdigits);
                    }

                    if (places <= 0) {
                        String       svalue = "" + value;
                        List<String> toks   = Utils.splitUpTo(svalue, ".", 2);
                        String       v      = toks.get(0);
                        String       d      = ((toks.size() > 1)
					       ? toks.get(1)
					       : "");
                        d = StringUtil.padRight(d, -places, "0");
                        if (d.length() > -places) {
                            d = d.substring(0, -places);
                        } else {
                            d = StringUtil.padRight("", -places, "0");
                        }
                        String newValue = v + "." + d + digits;
                        row.set(index, newValue);
                        //                      System.err.println("value:" + value+" left:" +v +" right:" + d+ " result:" +newValue);
                    } else {
                        int    ivalue = (int) value;
                        double d      = Math.pow(10, places);
                        ivalue = (int) (d * ((int) (ivalue / d)));

                        if (ivalue == 0) {
                            row.set(index, ivalue + "." + digits);
                        } else {
                            row.set(index, "" + (ivalue + digits));
                        }
                        //                      System.err.println("v:" + value+" I:" +ivalue +" result:" + row.get(index));
                    }
                } catch (NumberFormatException nfe) {}
            }

            return row;
        }

    }

    public static class Ceil extends Converter {

        private double value;

        public Ceil(List<String> cols, double value) {
            super(cols);
            this.value = value;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                return row;
            }
            List<Integer> indices = getIndices(ctx);
            for (int index : indices) {
                try {
                    if ((index < 0) || (index >= row.size())) {
                        continue;
                    }
                    double value =
                        Double.parseDouble(row.get(index).toString());
                    value = (double) Math.min(value, this.value);
                    row.set(index, Double.valueOf(value));
                } catch (NumberFormatException nfe) {}
            }

            return row;
        }
    }

    public static class Floor extends Converter {

        private double value;

        public Floor(List<String> cols, double value) {
            super(cols);
            this.value = value;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                return row;
            }
            List<Integer> indices = getIndices(ctx);
            for (int index : indices) {
                try {
                    if ((index < 0) || (index >= row.size())) {
                        continue;
                    }
                    double value =
                        Double.parseDouble(row.get(index).toString());
                    value = (double) Math.max(value, this.value);
                    row.set(index, Double.valueOf(value));
                } catch (NumberFormatException nfe) {}
            }

            return row;
        }
    }

    public static class ColumnCopier extends Converter {

        /* */

        private String name;

        public ColumnCopier(String col, String name) {
            super(col);
            this.name = name;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            int index = getIndex(ctx);
            if ((index < 0) || (index >= row.size())) {
                return row;
            }
            if (rowCnt++ == 0) {
                row.insert(index + 1, name);
            } else {
                row.insert(index + 1, row.getValues().get(index));
            }

            return row;
        }

    }

    public static class ColumnNewer extends Converter {

        /* */

        private String delimiter;

        private String name;

        public ColumnNewer(List<String> indices, String delimiter,
                           String name) {
            super(indices);
            this.delimiter = delimiter.replaceAll("_nl_","\n");
            this.name      = name;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                row.add(name);

                return row;
            }
            List<Integer> indices = getIndices(ctx);
            StringBuilder sb      = new StringBuilder();
            int           cnt     = 0;
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                String s = (String) row.getValues().get(index);
                if (cnt++ > 0) {
                    sb.append(delimiter);
                }
                sb.append(s);
            }
            row.getValues().add(sb.toString());

            return row;
        }
    }

    public static class RowConcat extends Converter {

        private int num;
	private List<Row> rows = new ArrayList<Row>();

        public RowConcat(int num) {
	    this.num = num;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
		Row newRow = new Row();
		for(int i=0;i<num;i++) {
		    for(Object o: row.getValues()) {
			newRow.add(o);
		    }
		}
		return newRow;
            }
	    rows.add(row);
	    if(rows.size()>=num) {
		Row newRow = makeRow();
		rows = new ArrayList<Row>();
		return newRow;
	    }
            return null;
	}

        @Override
        public void finish(TextReader ctx) throws Exception {
	    if(rows.size()>0) {
		Row newRow = makeRow();
		Processor nextProcessor = getNextProcessor();
		if (nextProcessor != null) {
		    nextProcessor.handleRow(ctx,newRow);
		}
	    }
            super.finish(ctx);
	}

	private Row makeRow() {
	    Row newRow = new Row();
	    for(Row row: rows) {
		for(Object o: row.getValues())
		    newRow.add(o);
	    }
	    return newRow;
	}

    }

    public static class ColumnMathOperator extends Converter {

        /* */

        private String name;

        /* */

        private String op;

	private String replaceCol;

	private int replaceIdx=-1;

        String foo;

        public ColumnMathOperator(List<String> indices, String name,
                                  String op) {
            super(indices);
            foo       = "" + indices;
            this.name = name;
            this.op   = op;
	    if(name.startsWith("replace:")) {
		replaceCol = name.substring("replace:".length());
	    }

        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            List<Integer> indices = getIndices(ctx);
            if (rowCnt++ == 0) {
		if(replaceCol!=null) {
		    replaceIdx = getIndex(ctx,replaceCol);
		} else {
		    row.getValues().add(name);
		}
                return row;
            }
            double value = 0;
            int    cnt   = 0;
            double total = 0;
            //      System.err.println("op:" + op +" " + foo +" indices:" + indices);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if (!row.indexOk(index)) {
                    continue;
                }
                String s = row.getString(index);
                double v = parse(row,s);
                //              System.err.println("\tindex::" + index +" value:" + value);
                if ( !Double.isNaN(v)) {
                    total += v;
                }
                if (op.equals("+")) {
                    value += v;
                } else {
                    if (cnt == 0) {
                        value = v;
                    } else {
                        if (op.equals("-")) {
                            value = value - v;
                        } else if (op.equals("*")) {
                            value = value * v;
                        } else if (op.equals("%")) {
                            value = value % v;			    
                        } else if (op.equals("/")) {
                            value = value / v;
                        }
                    }
                }
                cnt++;
            }
            if (op.equals(OPERAND_AVERAGE)) {
                if (cnt == 0) {
                    value = Double.NaN;
                } else {
                    value = total / cnt;
                }
            }
            //      System.err.println("\tfinal value:" + value);
	    if(replaceIdx>=0) {
		row.set(replaceIdx,value + "");
	    } else {
		row.getValues().add(value + "");
	    }
            return row;
        }

    }

    public static class CompareNumber extends Converter {

        private String op;

	private String scol1;
	private String scol2;
	private int col1;
	private int col2;	

        public CompareNumber(String col1, String col2, String op) {
	    this.scol1=  col1;
	    this.scol2=  col2;	    
            this.op   = op;
        }

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
            double value1 = row.getDouble(col1);
            double value2 = row.getDouble(col2);	    
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

    public static class Delta extends Converter {

        List<String> keys;

        private Hashtable<String, Row> prevRows = new Hashtable<String,
	    Row>();

        List<Integer> indices;

        List<Integer> keyindices;

        public Delta(List<String> keys, List<String> indices) {
            super(indices);
            this.keys = keys;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (indices == null) {
                indices    = getIndices(ctx);
                keyindices = getIndices(ctx, keys);
            }
            if (rowCnt++ == 0) {
                for (Integer idx : indices) {
                    int index = idx.intValue();
                    row.getValues().add("Difference " + row.get(index));
                }

                return row;
            }
            String key = "";
            for (Integer idx : keyindices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                key += row.get(index).toString() + "_";
            }
            Row prevRow = prevRows.get(key);
            if (prevRow == null) {
                prevRow = row;
                for (int i : indices) {
                    prevRow.add("0");
                }
                prevRows.put(key, prevRow);

                return row;
            }
            prevRows.put(key, row);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                double v1 = parse(prevRow,prevRow.get(index).toString());
                double v2 = parse(row,row.get(index).toString());
                row.add((v2 - v1) + "");
            }

            return row;
        }

    }

    public static class RunningSum extends Converter {
	String nameTemplate;
	Row prevRow;
	Hashtable<Integer,Double> values = new Hashtable<Integer,Double>();
	int lastDate=-1;
	String sresetDateIndex;
	int resetDateIndex=-1;
	int calendarField=-1;
	GregorianCalendar cal;

        public RunningSum(TextReader ctx, List<String> indices,String nameTemplate) {
            super(indices);
	    this.nameTemplate = nameTemplate;
	    String reset=null;
	    reset = (String)ctx.getProperty("runningsum.reset." + nameTemplate);
	    if(reset==null && indices.size()>0)
		reset = (String)ctx.getProperty("runningsum.reset." + indices.get(0));
	    if(reset==null)
		reset = (String)ctx.getProperty("runningsum.reset");
	    if(reset!=null) {
		List<String> toks = Utils.split(reset,":");
		if(toks.size()!=2) {
		    throw new IllegalArgumentException("Incorrect runninsum.reset value:" + reset +" needs to be date_column:<year|month|day|hour|minute>");
		}
		sresetDateIndex = toks.get(0);
		calendarField=Utils.getCalendarField(toks.get(1));
		if(calendarField==-1) {
		    throw new IllegalArgumentException("Incorrect calendar field" + toks.get(1) +" needs to be one of year|month|day|hour|minute|second");
		}
		cal = new GregorianCalendar();
	    }

        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
	    List<Integer>indices    = getIndices(ctx);
            if (rowCnt++ == 0) {
		if(sresetDateIndex!=null) {
		    resetDateIndex = getIndex(ctx,sresetDateIndex);
		}

                for (Integer idx : indices) {
                    int index = idx.intValue();
		    if(row.indexOk(index)) {
			String name = row.getString(index);
			if(Utils.stringDefined(nameTemplate)) {
			    name = nameTemplate.replace("${column_name}",name).replace("${name}",name);
			} else {
			    name = "Sum " + name;
			}
			row.getValues().add(name);
		    }
                }
                return row;
            }
	    if(resetDateIndex>=0){
		String sdttm = row.getString(resetDateIndex,"");
		Date dttm = Utils.parseDate(sdttm);
		cal.setTime(dttm);
		if(lastDate==-1) {
		    lastDate = cal.get(calendarField);
		} else {
		    int date = cal.get(calendarField);
		    if(date!=lastDate) {
			lastDate = date;
			values = new Hashtable<Integer,Double>();
		    }

		}
	    }
            if (prevRow == null) {
                prevRow = row;
                for (int i : indices) {
                    prevRow.add("0");
                }
                return row;
            }
            for (Integer idx : indices) {
                int index = idx.intValue();
                if (!row.indexOk(index)) {
                    continue;
                }
                Double total = values.get(index);
		if(total==null) {
		    total=new Double(0);
		}
                double v2 = parse(row,row.get(index).toString());
		v2 = v2+total;
		values.put(index,v2);
                row.add(v2+"");
            }
	    prevRow=row;
            return row;
        }

    }

    public static class Integrate extends Converter {
	String timeUnit;
	String newName;
	String dateSIndex;
	int dateIndex;
	Date lastDate;

        public Integrate(String index,String dateSIndex,String timeUnit,String newName) {
            super(index);
	    this.dateSIndex = dateSIndex;
	    this.timeUnit = timeUnit;
	    this.newName = newName;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
	    //true->missing ok
	    int index =getIndex(ctx,true);
	    if(index==UNDEFINED_INDEX) {
		return row;
	    }

            if (rowCnt++ == 0) {
		dateIndex = getIndex(ctx,dateSIndex);
		row.add(newName);
		return row;
            }

	    if (!row.indexOk(dateIndex)) {
		return row;
	    }

	    String sdate = row.getString(dateIndex,"");
	    Date  date = ctx.parseDate(sdate);
	    if(date==null) {
		row.add(Double.NaN);
		return row;
	    }
	    if(lastDate==null) {
		lastDate = date;
		row.add(0);
		return row;
	    }

	    if (!row.indexOk(index)) {
		return row;
	    }

	    double rate = row.getDouble(index);
	    if(Double.isNaN(rate)) {
		row.add("0");
	    } else {
		long ms = date.getTime()-lastDate.getTime();
		//cf/s
		long currentMillis = Utils.toMillis(1,timeUnit);
		double converted = rate/currentMillis;
		double volume = converted*ms;
		row.add(volume);

	    }
	    lastDate = date;
            return row;
        }

    }

    public static class TrendCounter extends Converter {
	String name;
	int counter=0;
	double lastValue=Double.NaN;

        public TrendCounter(String col, String name) {
            super(col);
	    this.name = name;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
		row.add(name);
                return row;
            }
	    int index = getIndex(ctx);
	    if(!row.indexOk(index)) return row;
	    double v = row.getDouble(index);
	    if(Double.isNaN(lastValue) || v<lastValue) {
		counter++;
	    }
	    lastValue = v;
	    row.add(""+(counter));
            return row;
        }

    }

    public static class ChangeCounter extends Converter {
	String name;
	int counter=0;
	double lastValue=Double.NaN;

        public ChangeCounter(String col, String name) {
            super(col);
	    this.name = name;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
		row.add(name);
                return row;
            }
	    int index = getIndex(ctx);
	    if(!row.indexOk(index)) return row;
	    double v = row.getDouble(index);
	    if(Double.isNaN(lastValue) || v!=lastValue) {
		counter++;
	    }
	    lastValue = v;
	    row.add(""+(counter));
            return row;
        }

    }

    public static class Mercator extends Converter {

        public Mercator(List<String> indices) {
            super(indices);

        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                row.getValues().add("Latitude");
                row.getValues().add("Longitude");

                return row;
            }
            List<Integer> indices = getIndices(ctx);
            double x =
                Double.parseDouble(row.getValues().get(indices.get(0)).toString());
            double y =
                Double.parseDouble(row.getValues().get(indices.get(1)).toString());
            double rMajor = 6378137;  //Equatorial Radius, WGS84
            double shift  = Math.PI * rMajor;
            double lon    = x / shift * 180.0;
            double lat    = y / shift * 180.0;
            lat = 180 / Math.PI
		* (2 * Math.atan(Math.exp(lat * Math.PI / 180.0))
		   - Math.PI / 2.0);

            row.getValues().add(lat);
            row.getValues().add(lon);

            return row;
        }

    }

    public static class ColumnRounder extends Converter {

        public ColumnRounder(List<String> indices) {
            super(indices);
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                if ( !ctx.getAllData()) {
                    return row;
                }
            }
            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                String s = row.getString(index);
                double v = (s.length() == 0)
		    ? 0
		    : Double.parseDouble(s.replaceAll(",", ""));
                row.set(index, "" + ((int) Math.round(v)));
            }

            return row;
        }

    }

    public static class Bytes extends Converter {

        private String unit;

        public Bytes(String unit, List<String> indices) {
            super(indices);
            this.unit = unit;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            List<Integer> indices = getIndices(ctx);
            if (rowCnt++ == 0) {
                for (int i : indices) {
                    row.add("Size " + row.get(i));
                }

                return row;
            }
            for (Integer idx : indices) {
                int    index = idx.intValue();
                String s     = (String) row.getValues().get(index);
                double v     = 0;

                s = s.replaceAll(" ", "").toLowerCase();
                String u = StringUtil.findPattern(s, "([^0-9\\.\\-]+)");
                if (u == null) {
                    v = Double.parseDouble(s);
                } else {
                    v = Double.parseDouble(s.replaceAll(u, ""));
                    v = v * getMultiplier(u);
                }
                row.add(v);

            }

            return row;
        }

        private double getMultiplier(String u) {
            if (unit.equals("binary")) {
                return getMultiplier(u, 1024);
            }

            return getMultiplier(u, 1000);
        }

        private double getMultiplier(String u, double base) {
            if (u.equals("kb")) {
                return base;
            }
            if (u.equals("mb")) {
                return base * base;
            }
            if (u.equals("gb")) {
                return base * base * base;
            }
            if (u.equals("tb")) {
                return base * base * base * base;
            }
            if (u.equals("pb")) {
                return base * base * base * base * base;
            }

            return 1;
        }

    }

    public static class ColumnAbs extends Converter {

        public ColumnAbs(List<String> indices) {
            super(indices);
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                return row;
            }
            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                String s = (String) row.getValues().get(index);
                double v = (s.length() == 0)
		    ? 0
		    : Double.parseDouble(s.replaceAll(",", ""));
                row.set(index, "" + Math.abs(v));
            }

            return row;
        }

    }

    public static class ColumnRand extends Converter {

        String name;

        double min;

        double max;

        public ColumnRand(String name, double min, double max) {
            this.name = name;
            this.min  = min;
            this.max  = max;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                row.add(name);

                return row;
            }
            double r = Math.random();
            r = r * (max - min) + min;
            row.add("" + r);

            return row;
        }

    }

    public static class Case extends Converter {

        /* */

        String action;

        public Case(List<String> indices, String action) {
            super(indices);
            this.action = action;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                return row;
            }
            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                String s  = (String) row.getValues().get(index);
                String os = s;
                if (s == null) {
                    return row;
                }
		s= Utils.applyCase(action, s);
                row.getValues().set(index, s);
            }

            return row;
        }

    }

    public static class ToId extends Converter {

        public ToId(List<String> indices) {
            super(indices);
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                return row;
            }
            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                int index = idx.intValue();
		if(!row.indexOk(index)) continue;
                String s  = (String) row.getValues().get(index);
                if (s == null) {
                    return row;
                }
                row.getValues().set(index, makeID(s));
            }

            return row;
        }

    }

    public static class NumColumns extends Converter {

        int number;

        public NumColumns(int number) {
            this.number = number;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
	    if(number<0) {
		number =row.size();
		return row;
	    }
            List values = row.getValues();
            while (values.size() < number) {
                values.add("");
            }
            while (values.size() > number) {
                values.remove(values.size() - 1);
            }

            return row;
        }

    }

    public static class Trim extends Converter {

        public Trim(List<String> indices) {
            super(indices);
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                Object o = row.getValues().get(index);
                if (o == null) {
                    continue;
                }
                String s = o.toString().trim();
                row.set(index, s);
            }

            return row;
        }

    }

    public static class PadLeftRight extends Converter {

        private int length;

        private String c;

        private boolean left;

        public PadLeftRight(boolean left, List<String> indices, String c,
                            int length) {
            super(indices);
            this.left   = left;
            this.c      = c;
            this.length = length;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                return row;
            }
            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                String s = (String) row.getValues().get(index);
                if (s == null) {
                    continue;
                }
                while (s.length() < length) {
                    if (left) {
                        s = c + s;
                    } else {
                        s = s + c;
                    }
                }
                row.set(index, s);
            }

            return row;
        }

    }

    public static class TrimQuotes extends Converter {

        public TrimQuotes(List<String> indices) {
            super(indices);
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                String s = (String) row.getValues().get(index);
                if (s == null) {
                    continue;
                }
                if (s.startsWith("\"")) {
                    s = s.substring(1);
                }
                if (s.endsWith("\"")) {
                    s = s.substring(0, s.length() - 2);
                }
                row.set(index, s);
            }

            return row;
        }

    }

    public static class StripTags extends Converter {

        public StripTags(List<String> indices) {
            super(indices);
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                return row;
            }
            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                String s = (String) row.getValues().get(index);
                s = Utils.stripTags(s);
                row.set(index, s);
            }

            return row;
        }

    }

    public static class Decoder extends Converter {

        public Decoder(List<String> indices) {
            super(indices);
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                return row;
            }
            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                String s = (String) row.getValues().get(index);
                row.set(index, HtmlUtils.entityDecode(s));
            }

            return row;
        }

    }

    public static class MD extends Converter {

        private MessageDigest md;

        public MD(TextReader ctx, List<String> indices, String type) {
            super(indices);
            try {
                type = type.trim();
                if (type.length() == 0) {
                    type = "MD5";
                }
                md = MessageDigest.getInstance(type);
            } catch (Exception exc) {
                fatal(ctx, "Creating message digest:" + type, exc);
            }
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                row.add("MD");

                return row;
            }
            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                String s = (String) row.getValues().get(index);
                if (s == null) {
                    s = "";
                }
                md.update(s.getBytes());
            }
            try {
                row.add(Utils.encodeMD(md.digest()));
            } catch (Exception exc) {
                fatal(ctx, "Error making message digest", exc);
            }

            return row;
        }

    }

    public static class Subst extends Converter {
	private List<String>header;
	private List<String>_header;
	private String name;
	private String template;

        public Subst(String name, String template) {
	    this.name = name;
	    this.template = template;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
		header = new ArrayList<String>();
		_header = new ArrayList<String>();
		for(int i=0;i<row.size();i++) {
		    header.add(row.getString(i));
		    _header.add(row.getString(i).toLowerCase());
		}
                row.add(name);
                return row;
            }
	    String v = template;
	    for(int i=0;i<row.size();i++) {	    
		v = Utils.replace(v,"${" + header.get(i) +"}", row.getString(i),
				  "${" + _header.get(i) +"}", row.getString(i),
				  "${" + i +"}", row.getString(i));
	    }
	    row.add(v);
            return row;
        }
    }

    public static class SoundexMaker extends Converter {

        private Soundex soundex;

        public SoundexMaker(List<String> indices) {
            super(indices);
            soundex = new Soundex();
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                row.add("Soundex");

                return row;
            }
            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                String s = (String) row.getValues().get(index);
                if (s == null) {
                    s = "";
                }
                row.add(soundex.soundex(s));
            }

            return row;
        }

    }

    public static class Even extends Converter {

        public Even(List<String> indices) {
            super(indices);
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                row.add("Even");

                return row;
            }
            String        v       = "";
            List<Integer> indices = getIndices(ctx);
            for (Integer idx : indices) {
                int index = idx.intValue();
                if ((index < 0) || (index >= row.size())) {
                    continue;
                }
                String s = (String) row.getValues().get(index);
                if (s == null) {
                    s = "";
                }
                v += s;
            }

            String  snum = StringUtil.findPattern(v, "^(\\d+)");
            boolean even = true;
            if (snum != null) {
                int num = (int) Double.parseDouble(snum);
                even = ((num % 2) == 0);
            }
            row.add(even
                    ? "even"
                    : "odd");

            return row;
        }

    }

    public static class ColumnInserter extends Converter {

        private String name;

        private List<String> values;

	private String value;

        public ColumnInserter(String col, String name, String value) {
            super(col);
            this.values = Utils.split(value, ",", false, false);
	    this.value = value;
            this.name   = name;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            int    col = hasColumns()
		? getIndex(ctx)
		: -1;
            String v   = "";
            if (rowCnt == 0) {
		value = ctx.applyMacros(value);
                v = name;
            } else if (rowCnt < values.size()) {
                //v = values.get(rowCnt);
		v = this.value;
            } else {
		//                v = values.get(values.size() - 1);
		v = this.value;
            }
	    v =v.replace("${row}",rowCnt+"");
            rowCnt++;
            if ((col < 0) || (col > row.getValues().size())) {
                row.getValues().add(v);
            } else {
                row.getValues().add(col, v);
            }

            return row;
        }
    }

    public static class ColumnAdder extends Converter {

        private List<String> names;

        private List<String> values;

        public ColumnAdder(String names, String values) {
            this.names  = Utils.split(names, ",", false, false);
            this.values = Utils.split(values, ",", false, false);
            while (this.values.size() < this.names.size()) {
                this.values.add("");
            }
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                row.addAll(names);
            } else {
                row.addAll(values);
            }

            return row;
        }
    }

    public static class Generator extends Converter {

        private String label;

        private double start;

        private double step;

        private double value;

        public Generator(String label, double start, double step) {
            this.label = label;
            this.start = start;
            this.step  = step;
            value      = start;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                add(ctx, row, label);

                return row;
            }
            if (value == (int) value) {
                add(ctx, row, "" + ((int) value));
            } else {
                add(ctx, row, "" + value);
            }
            value += step;

            return row;
        }
    }

    public static class ColumnMacro extends Converter {

	private  String spattern;

        private Pattern pattern;

        private String template;

        private String label;

        private String value;

        public ColumnMacro(String pattern, String template, String label) {
	    spattern = pattern;
            this.pattern  = Pattern.compile(pattern, Pattern.MULTILINE);
            this.template = template;
            this.label    = label;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
	    boolean debug =false;
            if ((rowCnt++ == 0) && !label.equals("none")) {
                row.getValues().add(label);
                return row;
            }
            if (value == null) {
		if(debug)
		    System.err.println("checking value with pattern:" + spattern +" template:" + template);
                for (String hline : ctx.getHeaderLines()) {
		    if(debug)
			System.err.println("header line:" + hline);
                    Matcher matcher = pattern.matcher(hline);
                    if (matcher.find()) {
			if(debug)
			    System.err.println("\tmatched");
                        String v = template;
                        for (int i = 0; i < matcher.groupCount(); i++) {
			    String m = matcher.group(i + 1);
			    if(debug)
				System.err.println("\tpattern group:" + (i+1)+"="+ m);
                            v = v.replace("{" + (i + 1) + "}",m);
                        }
                        value = v;
			if(debug) System.err.println("\tvalue:" + value);
                        break;
                    } else {
			if(debug) System.err.println("\tno match");
		    }
                }
            }
            if (value == null) {
		if(debug)
		    System.err.println("\tno value found");
                value = "";
            }
            row.getValues().add(value);
            return row;
        }
    }

    public static class CopyIf extends Converter {

        String pattern;

        String scol1;

        String scol2;

        int col1 = -1;

        int col2 = -1;

        public CopyIf(List<String> cols, String pattern, String col1,
                      String col2) {
            super(cols);
            this.pattern = pattern;
            scol1        = col1;
            scol2        = col2;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                col1 = getIndex(ctx, scol1);
                col2 = getIndex(ctx, scol2);

                return row;
            }

            //      System.err.println("Row:");
            for (int i : getIndices(ctx)) {
                String v = row.getString(i);
                //              System.err.println("\ti:" + i +" v:" + v +" p:" + pattern);
                if ( !v.matches(pattern)) {
                    //              System.err.println("\tno match:" +row);
                    return row;
                }
            }

            row.set(col2, row.getString(col1));

            return row;
        }
    }

    public static class CopyColumns extends Converter {

        List<String> toCols;

        List<Integer> toIndices;

        public CopyColumns(List<String> cols, List<String> cols2) {
            super(cols);
            if (cols.size() != cols2.size()) {
                throw new IllegalArgumentException(
						   "Mismatched columns in -copycolumns:" + cols + " "
						   + cols2);
            }
            this.toCols = cols2;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                toIndices = getIndices(ctx, toCols);

                return row;
            }

            List<Integer> from = getIndices(ctx);
            //      System.err.println("copy");
            for (int i = 0; i < from.size(); i++) {
                String v = row.getString(from.get(i));
                //              System.err.println("\tsetting:" + toIndices.get(i) +" to:" + v);
                row.set(toIndices.get(i), v);
            }

            return row;
        }
    }

    public static class ColumnNudger extends Converter {

        /* */

        private int col;

        /* */

        private int rowIdx;

        /* */

        private String value;

        public ColumnNudger(int row, int col, String value) {
            this.rowIdx = row;
            this.col    = col;
            this.value  = value;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if ((rowIdx < 0) || (rowIdx == rowCnt)) {
                if (col < 0) {
                    row.getValues().add(value);
                } else {
                    row.getValues().add(col, value);
                }
            }
            rowCnt++;

            return row;

        }

    }

    public static class ColumnUnNudger extends Converter {

        /* */

        private List<Integer> cols;

        /* */

        private int rowIdx;

        public ColumnUnNudger(int row, List<String> cols) {
            this.cols   = Utils.toInt(cols);
            this.rowIdx = row;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if ((rowIdx < 0) || (rowIdx == rowCnt)) {
                List newValues = new ArrayList();
                List values    = row.getValues();
                for (int i = 0; i < values.size(); i++) {
                    if ( !cols.contains(i)) {
                        newValues.add(values.get(i));
                    }
                }
                row.setValues(newValues);
            }
            rowCnt++;

            return row;

        }

    }

    public static class ColumnSetter extends Converter {

        /* */

        private List<Integer> cols;

        /* */

        private List<Integer> rows;

        /* */

        private String value;

        public ColumnSetter(List<String> cols, List<String> rows,
                            String value) {
            super(cols);
            this.rows  = Utils.toInt(rows);
            this.value = value;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (cols == null) {
                cols = getIndices(ctx);
            }

            boolean gotRow = false;
            for (int rowIdx : rows) {
                if (rowCnt == rowIdx) {
                    gotRow = true;

                    break;
                }
            }
            rowCnt++;
            if ( !gotRow) {
                return row;
            }

            List values = row.getValues();
            for (int col : cols) {
                if (col < values.size()) {
                    values.set(col, value);
                }
            }

            return row;
        }
    }

    public static class FillDown extends Converter {

        private Hashtable<Integer, String> lastValue = new Hashtable<Integer,
	    String>();

        public FillDown(List<String> cols) {
            super(cols);
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                return row;
            }
            List<Integer> indices = getIndices(ctx);
            for (int col : indices) {
                String v = row.getString(col);
                v = v.trim();
                if (v.length() == 0) {
                    v = lastValue.get(col);
                    if (v != null) {
                        row.set(col, v);
                    }
                } else {
                    lastValue.put(col, v);
                }
            }

            return row;
        }
    }

    public static class FillAcross extends Converter {

        private Hashtable<Integer, String> lastValue = new Hashtable<Integer,
	    String>();

	private HashSet<Integer> rows;

        public FillAcross(List<String> cols,List<Integer> rows) {
            super(cols);
	    this.rows = new HashSet<Integer>();
	    this.rows.addAll(rows);
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
	    if(!rows.contains(rowCnt++)) return row;
            List<Integer> indices = getIndices(ctx);
	    String lastValue="";
            for (int col : indices) {
		if(!row.indexOk(col)) continue;
                String v = row.getString(col);
                v = v.trim();
                if (v.length() == 0) {
		    row.set(col,lastValue);
		} else {
		    lastValue = v;
                }
            }

            return row;
        }
    }

    public static class Unfill extends Converter {

        private Hashtable<Integer, String> lastValue = new Hashtable<Integer,
	    String>();

        public Unfill(List<String> cols) {
            super(cols);
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                return row;
            }
            List<Integer> indices = getIndices(ctx);
            for (int col : indices) {
                String v = row.getString(col);
                v = v.trim();
		String lastv = lastValue.get(col);
		if(lastv!=null && v.equals(lastv)) {
		    row.set(col,"");
		} else {
		    lastValue.put(col,v);
		}
            }

            return row;
        }
    }

    public static class MakeIds extends Converter {

        public MakeIds() {}

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                Row newRow = new Row();
                for (int i = 0; i < row.size(); i++) {
		    String id = Utils.makeID(row.getString(i));
		    System.err.println(row.getString(i) +"=" +id);
                    newRow.add(id);
                }

                return newRow;
            }

            return row;
        }
    }

    public static class PriorPrefixer extends Converter {

        private int col;

        private String pattern;

        private String delim;

        private String prefix;

        public PriorPrefixer(int col, String pattern, String delim) {
            this.col     = col;
            this.pattern = pattern;
            this.delim   = delim;
        }

	@Override
        public void reset(boolean force) {
            super.reset(force);
            prefix = null;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            String val = row.get(col).toString();
            if ((prefix != null)
		&& (val.matches(pattern)
		    || (val.indexOf(pattern) >= 0))) {
                row.set(col, prefix + delim + val);
            } else {
                prefix = val;
            }

            return row;
        }

    }

    public static class Letter extends Converter {

        /* */

        int cnt = 0;

        public Letter(TextReader ctx) {}

        @Override
        public Row processRow(TextReader ctx, Row row) {
            cnt++;
            if (cnt == 1) {
                add(ctx, row, "label");
                return row;
            }
            String letter = getLabel(cnt - 2);
            add(ctx, row, letter);
            return row;
        }
    }

    public static class Number extends Converter {

        /* */

        int cnt = 0;

        public Number(TextReader ctx) {}

        @Override
        public Row processRow(TextReader ctx, Row row) {
            cnt++;
            if (cnt == 1) {
                row.insert(0, "number");

                return row;
            }
            row.insert(0, "" + (cnt - 1));

            return row;
        }

    }

    public static class UUID extends Converter {

        public UUID(TextReader ctx) {}

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                row.add("UUID");
            } else {
                row.add(Utils.getGuid());
            }

            return row;
        }
    }

    public static class B64Encode extends Converter {

        public B64Encode(TextReader ctx, List<String> cols) {
            super(cols);
        }

        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                for (int i : getIndices(ctx)) {
                    row.add(row.getString(i) + " base64");
                }
            } else {
                for (int i : getIndices(ctx)) {
                    row.add(new String(Utils.encodeBase64(row.getString(i))));
                }
            }

            return row;
        }

    }

    public static class ParseEmail extends Converter {

        public ParseEmail(TextReader ctx, List<String> cols) {
            super(cols);
        }

        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                for (int i : getIndices(ctx)) {
                    row.add("Name");
		    row.add("Email");
                }
            } else {
                for (int i : getIndices(ctx)) {
		    String s = row.getString(i);
		    String email=StringUtil.findPattern(s,"<(.*)>");
		    if(email==null) email= StringUtil.findPattern(s,"([^\\s]+@[^\\s]+)");
		    if(email==null) email=s;
		    String name=StringUtil.findPattern(s,"(.*)<(.*)>");
		    if(name==null) {
			name = StringUtil.findPattern(email,"(.*)@");
			if(name!=null && name.indexOf(".")>=0) {
			    String tmp="";
			    for(String tok:Utils.split(name,".",true,true)) {
				tmp+=Utils.applyCase(Utils.CASE_PROPER,tok);
				tmp+=" ";
			    }
			    name=tmp.trim(); 
			} else name=null;
		    }
		    if(name==null) name=s;
		    name = name.replaceAll("\\([^\\)]*\\)","");
                    row.add(name.trim());
		    row.add(email.trim());
                }
            }

            return row;
        }

    }

    public static class B64Decode extends Converter {

        public B64Decode(TextReader ctx, List<String> cols) {
            super(cols);
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                for (int i : getIndices(ctx)) {
                    row.add(row.getString(i) + " value");
                }
            } else {
                for (int i : getIndices(ctx)) {
                    row.add(
			    new String(
				       Utils.decodeBase64(row.getString(i).getBytes())));
                }
            }

            return row;
        }
    }

    public static class Rot13 extends Converter {

        public Rot13(TextReader ctx, List<String> cols) {
            super(cols);
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                for (int i : getIndices(ctx)) {
                    row.add(row.getString(i) + " rot13");
                }
            } else {
                for (int i : getIndices(ctx)) {
                    row.add(new String(Utils.rot13(row.getString(i))));
                }
            }

            return row;
        }
    }

    public static class Crypt extends Converter {

        protected Cipher cipher;

        public Crypt(boolean encrypt, TextReader ctx, List<String> cols,
                     String password)
	    throws Exception {
            super(cols);
	    MessageDigest digester = MessageDigest.getInstance("SHA-256");
	    digester.update(password.getBytes("UTF-8"));
	    byte[] key = digester.digest();
	    SecretKeySpec spec = new SecretKeySpec(key, "AES");
	    cipher = Cipher.getInstance("AES");
	    cipher.init(encrypt
			? Cipher.ENCRYPT_MODE
			: Cipher.DECRYPT_MODE, spec);
	}
    }

    public static class EncryptDecrypt extends Crypt {
	boolean mode;

        public EncryptDecrypt(TextReader ctx, boolean encrypt, List<String> cols, 
			      String key)
	    throws Exception {
            super(encrypt, ctx, cols, key);
	    this.mode = encrypt;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                for (int i : getIndices(ctx)) {
                    row.add(row.getString(i) + (mode?" encrypted":" encrypted"));
                }
		return row;
            }
	    try {
		for (int i : getIndices(ctx)) {
		    if(mode) {
			byte[]bytes = cipher.doFinal(row.getBytes(i));
			row.add(Utils.encodeBase64Bytes(bytes));
		    } else {
			byte[]bytes = Utils.decodeBase64(row.getBytes(i));
			row.add(new String(cipher.doFinal(bytes)));
		    }
		}
	    } catch (Exception exc) {
		fatal(ctx, "Error encrypting", exc);
            }

            return row;
        }
    }

    public static class ColumnPatternSetter extends Converter {

        /* */

        private int patternCol = -1;

        private String spatternCol;

        private String pattern;

        /* */

        private int writeCol = -1;

        private String swriteCol;

        private String what;

        public ColumnPatternSetter(String col1, String pattern, String col2,
                                   String what) {
            this.spatternCol = col1;
            this.pattern     = pattern;
            this.swriteCol   = col2;
            this.what        = what;
        }

        @Override
        public Row processRow(TextReader ctx, Row row) {
            if (rowCnt++ == 0) {
                patternCol = getIndex(ctx, spatternCol);
                writeCol   = getIndex(ctx, swriteCol);

                return row;
            }
            if (patternCol == -1) {
                row.set(writeCol, what);
            } else {
                String v = row.get(patternCol).toString();
                if (v.matches(pattern) || (v.indexOf(pattern) >= 0)) {
                    row.set(writeCol, what);
                }
            }

            return row;

        }

    }

}
