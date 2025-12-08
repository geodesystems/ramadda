/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;

import org.apache.logging.log4j.Logger;

import java.io.*;

/**
 * Orignally from JD Evora
 */
public class Log4jPrintWriter extends PrintWriter {
    private Logger log;
    StringBuffer text = new StringBuffer("");

    public Log4jPrintWriter(Logger log) {
        super(System.err);  // PrintWriter doesn't have default constructor.
        this.log = log;
    }

    public void log() {
        if (log != null) {
            log.info(text.toString());
        }
        text.setLength(0);
    }

    // overrides all the print and println methods for 'print' it to the constructor's Category

    public void close() {
        flush();
    }

    public void flush() {
        if ( !text.toString().equals("")) {
            log();
        }
    }

    public void print(boolean b) {
        text.append(b);
    }

    public void print(char c) {
        text.append(c);
    }

    public void print(char[] s) {
        text.append(s);
    }

    public void print(double d) {
        text.append(d);
    }

    public void print(float f) {
        text.append(f);
    }

    public void print(int i) {
        text.append(i);
    }

    public void print(long l) {
        text.append(l);
    }

    public void print(Object obj) {
        text.append(obj);
    }

    public void print(String s) {
        text.append(s);
    }

    public void println() {
        if ( !text.toString().equals("")) {
            log();
        }
    }

    public void println(boolean x) {
        text.append(x);
        log();
    }

    public void println(char x) {
        text.append(x);
        log();
    }

    public void println(char[] x) {
        text.append(x);
        log();
    }

    public void println(double x) {
        text.append(x);
        log();
    }

    public void println(float x) {
        text.append(x);
        log();
    }

    public void println(int x) {
        text.append(x);
        log();
    }

    public void println(long x) {
        text.append(x);
        log();
    }

    public void println(Object x) {
        text.append(x);
        log();
    }

    public void println(String x) {
        text.append(x);
        log();
    }
}
