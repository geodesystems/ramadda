/*
* Copyright (c) 2008-2018 Geode Systems LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*     http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.ramadda.util;


import org.apache.log4j.Logger;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.io.*;


/**
 * Orignally from JD Evora
 */


public class Log4jPrintWriter extends PrintWriter {

    /** _more_ */
    private Logger log;

    /** _more_ */
    StringBuffer text = new StringBuffer("");

    /**
     * _more_
     *
     * @param log _more_
     */
    public Log4jPrintWriter(Logger log) {
        super(System.err);  // PrintWriter doesn't have default constructor.
        this.log = log;
    }

    /**
     * _more_
     */
    public void log() {
        if (log != null) {
            log.info(text.toString());
        }
        text.setLength(0);
    }


    // overrides all the print and println methods for 'print' it to the constructor's Category

    /**
     * _more_
     */
    public void close() {
        flush();
    }

    /**
     * _more_
     */
    public void flush() {
        if ( !text.toString().equals("")) {
            log();
        }
    }



    /**
     * _more_
     *
     * @param b _more_
     */
    public void print(boolean b) {
        text.append(b);
    }

    /**
     * _more_
     *
     * @param c _more_
     */
    public void print(char c) {
        text.append(c);
    }

    /**
     * _more_
     *
     * @param s _more_
     */
    public void print(char[] s) {
        text.append(s);
    }

    /**
     * _more_
     *
     * @param d _more_
     */
    public void print(double d) {
        text.append(d);
    }

    /**
     * _more_
     *
     * @param f _more_
     */
    public void print(float f) {
        text.append(f);
    }

    /**
     * _more_
     *
     * @param i _more_
     */
    public void print(int i) {
        text.append(i);
    }

    /**
     * _more_
     *
     * @param l _more_
     */
    public void print(long l) {
        text.append(l);
    }

    /**
     * _more_
     *
     * @param obj _more_
     */
    public void print(Object obj) {
        text.append(obj);
    }

    /**
     * _more_
     *
     * @param s _more_
     */
    public void print(String s) {
        text.append(s);
    }

    /**
     * _more_
     */
    public void println() {
        if ( !text.toString().equals("")) {
            log();
        }
    }

    /**
     * _more_
     *
     * @param x _more_
     */
    public void println(boolean x) {
        text.append(x);
        log();
    }

    /**
     * _more_
     *
     * @param x _more_
     */
    public void println(char x) {
        text.append(x);
        log();
    }

    /**
     * _more_
     *
     * @param x _more_
     */
    public void println(char[] x) {
        text.append(x);
        log();
    }

    /**
     * _more_
     *
     * @param x _more_
     */
    public void println(double x) {
        text.append(x);
        log();
    }

    /**
     * _more_
     *
     * @param x _more_
     */
    public void println(float x) {
        text.append(x);
        log();
    }

    /**
     * _more_
     *
     * @param x _more_
     */
    public void println(int x) {
        text.append(x);
        log();
    }

    /**
     * _more_
     *
     * @param x _more_
     */
    public void println(long x) {
        text.append(x);
        log();
    }

    /**
     * _more_
     *
     * @param x _more_
     */
    public void println(Object x) {
        text.append(x);
        log();
    }

    /**
     * _more_
     *
     * @param x _more_
     */
    public void println(String x) {
        text.append(x);
        log();
    }
}
