/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import ucar.unidata.util.IOUtil;
import java.io.*;

/**
 *  A wrapper around either a hashtable for direct look ups or a list of key,values for
 *  pattern based lookups
 */
@SuppressWarnings("unchecked")
public class Propper {

    /**  */
    public static boolean debug = false;

    /**  */
    private boolean exact = true;

    /**  */
    private Hashtable props;

    /**  */
    private List<Value> values;

    /**
     
     */
    public Propper() {}

    /**
     
     *
     * @param exact _more_
     */
    public Propper(boolean exact) {
        this.exact = exact;
    }

    public Propper(boolean exact,String pattern, Object contents) {
	this(exact);
        if (!exact) {
            values = new ArrayList<Value>();
	    values.add(new Value(pattern, contents));
        } else {
	    props = new Hashtable();
	    props.put(pattern,contents);
	}

    }    

    public static Propper create(boolean exact, String  filename,InputStream is) throws Exception {
	if(filename.endsWith(".properties")) {
	    Properties properties = new Properties();
	    properties.load(is);
	    return  new Propper(exact,properties);
	} else if(filename.endsWith(".txt")) {
	    String contents = IOUtil.readContents(is);
	    List<String> toks = Utils.split(contents,"\n");
	    StringBuffer sb= new StringBuffer();
	    for(int i=1;i<toks.size();i++) {
		sb.append(toks.get(i));
		sb.append("\n");
	    }
	    return  new Propper(exact, toks.get(0),sb.toString());
	} else { //csv
	    Propper propper = new Propper(exact);
	    for(String line: Utils.split(IOUtil.readContents(is),"\n",true,true)) {
		List<String> cols = Utils.tokenizeColumns(line,",");
		String key = cols.get(0);
		cols.remove(0);
		propper.add(key,cols);
	    }
	    return propper;
	}
    }

	


    /**
     
     *
     * @param exact _more_
     * @param props _more_
     */
    public Propper(boolean exact, Hashtable props) {
        this(exact);
        this.props = props;
        if ( !exact) {
            values = new ArrayList<Value>();
            List tmp = Utils.makeList(props);
            for (int i = 0; i < tmp.size(); i += 2) {
                values.add(new Value(tmp.get(i).toString(), tmp.get(i + 1)));
            }
        }
    }

    /**
     *
     * @param props _more_
     */
    public void set(Hashtable props) {
        this.props = props;
    }


    /**
     *
     * @param key _more_
     * @param value _more_
     */
    public void add(String key, Object value) {
        if (exact) {
            if (props == null) {
                props = new Hashtable();
            }
            props.put(key, value);
        } else {
            if (values == null) {
                values = new ArrayList<Value>();
            }
            values.add(new Value(key, value));
        }

    }

    /**
     *
     * @param keys _more_
      * @return _more_
     */
    public Object get(String... keys) {
        if (props != null) {
            for (Object key : keys) {
                Object v = props.get(key);
                if (v != null) {
                    return v;
                }
            }
        }
        if (values == null) {
            return null;
        }
        for (Object key : keys) {
            for (Value v : values) {
                if (exact) {
                    if (debug) {
                        System.err.println("key:" + key + " value:" + v.key);
                    }
                    if (key.toString().equals(v.key)) {
                        return v.values;
                    }

                } else {
                    if (debug) {
                        System.err.println("V:" + key + " P:" + v.key + ":");
                    }
                    if (v.matches(key.toString())) {
                        return v.values;
                    }

                }
            }
        }

        return null;
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Aug 19, '22
     * @author         Enter your name here...    
     */
    private static class Value {

        /**  */
        String key;

        /**  */
        Object values;

        /**
         
         *
         * @param key _more_
         * @param values _more_
         */
        Value(String key, Object values) {
            this.key    = key;
            this.values = values;
        }

        /**
         *
         * @param v _more_
          * @return _more_
         */
        boolean matches(String v) {
            if (v.matches(key)) {
                if (debug) {
                    System.err.println("Value:" + v + " matches:" + key);
                }

                return true;
            }


            return false;
        }
    }

    /**
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        String v = "07";
        String p = "^\\d\\d$";
        System.err.println(v.matches(p));
    }



}
