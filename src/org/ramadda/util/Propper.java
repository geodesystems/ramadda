/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import ucar.unidata.util.IOUtil;

import java.io.*;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


/**
 *  A wrapper around either a hashtable for direct look ups or a list of key,values for
 *  pattern based lookups
 */
@SuppressWarnings("unchecked")
public class Propper {

    /**  */
    public static boolean debug = false;

    /**  */
    private boolean isProperties = false;

    /**  */
    private boolean isText = false;

    /**  */
    private boolean isCsv = false;

    /**  */
    private boolean exact = true;

    /**  */
    private Hashtable props;

    /**  */
    private List<Value> values;

    /**  */
    private List<String> header;

    /**
     *
     */
    public Propper() {}

    /**
     *
     *
     * @param exact _more_
     */
    public Propper(boolean exact) {
        this.exact = exact;
    }

    /**
     
     *
     * @param exact _more_
     * @param pattern _more_
     * @param contents _more_
     */
    public Propper(boolean exact, String pattern, Object contents) {
        this(exact);
        if ( !exact) {
            values = new ArrayList<Value>();
            values.add(new Value(pattern, contents));
        } else {
            props = new Hashtable();
            props.put(pattern, contents);
        }

    }

    /**
     *
     * @param exact _more_
     * @param filename _more_
     * @param is _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    public static Propper create(boolean exact, String filename,
                                 InputStream is)
            throws Exception {
        Propper propper;
        if (filename.endsWith(".properties")) {
            Properties properties = new Properties();
            properties.load(is);
            propper              = new Propper(exact, properties);
            propper.isProperties = true;
        } else if (filename.endsWith(".txt")) {
            String       contents = IOUtil.readContents(is);
            List<String> toks     = Utils.split(contents, "\n");
	    String firstLine = null;
	    int idx;
            for (idx = 0; idx < toks.size(); idx++) {
		String line = toks.get(idx);
		if(line.startsWith("#")) continue;
		firstLine = line;
		break;
	    }
            StringBuffer sb       = new StringBuffer();
            for (int i = idx+1; i < toks.size(); i++) {
                sb.append(toks.get(i));
                sb.append("\n");
            }
            propper        = new Propper(exact, firstLine, sb.toString());
            propper.isText = true;
        } else {  //csv
            propper       = new Propper(exact);
            propper.isCsv = true;
            for (String line :
                    Utils.split(IOUtil.readContents(is), "\n", true, true)) {
		if(line.startsWith("#")) continue;
                List<String> cols = Utils.tokenizeColumns(line, ",");
                if (propper.header == null) {
                    cols.remove(0);
                    propper.header = cols;
                    continue;
                }
                String key = cols.get(0);
                cols.remove(0);
                propper.add(key, cols);
            }
        }

        return propper;
    }




    /**
     *
     *
     * @param exact _more_
     * @param props _more_
     */
    public Propper(boolean exact, Hashtable props) {
        this(exact);
        this.props = props;
        if ( !exact) {
            values = new ArrayList<Value>();
            List tmp = Utils.makeListFromDictionary(props);
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
     * @param names _more_
     * @param o _more_
      * @return _more_
     */
    public String getValue(String[] names, Object o) {
        if (o instanceof String) {
            return (String) o;
        }
        List<String> values = (List<String>) o;
        for (int i = 0; i < header.size(); i++) {
            String h = header.get(i);
            for (String name : names) {
                //              System.err.print("H:" + h + " name:" + name +" v:" + values);
                if (h.equalsIgnoreCase(name)) {
                    return values.get(i);
                }
            }
        }

        return null;
    }

    /**
     *
     * @param names _more_
     * @param keys _more_
      * @return _more_
     */
    public String getNamedValue(String[] names, String... keys) {
        Object o = get(keys);
        if (o == null) {
            return null;
        }
        if (o instanceof String) {
            return (String) o;
        }
        List<String> l = (List<String>) o;
        if (header == null) {
            return null;
        }

        return getValue(names, l);
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
	if(debug) System.err.println("Propper.get");
        if (values == null) {
            return null;
        }
        for (Object key : keys) {
            for (Value v : values) {
                if (exact) {
                    if (debug) {
                        System.err.println("\tkey:" + key + " value:" + v.key);
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
         *  @return _more_
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
