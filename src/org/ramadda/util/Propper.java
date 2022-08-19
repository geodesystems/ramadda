/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


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
