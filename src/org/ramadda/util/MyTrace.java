/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;

import ucar.unidata.util.*;

import java.awt.*;
import java.awt.event.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import java.io.PrintStream;

import java.lang.reflect.InvocationTargetException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import javax.swing.*;

/**
 * Provides for application level call tracing, timing and memory
 * tracing.
 *
 */
@SuppressWarnings("unchecked")
public class MyTrace {
    private static Hashtable counters = new Hashtable();
    private static List counterList = new ArrayList();
    private static final Object MUTEX = new Object();
    public static boolean displayMsg = false;
    public static boolean showThreadLabel = false;
    private static boolean showLineNumber = false;
    private static Hashtable accumTable = new Hashtable();
    private static Hashtable accumCntTable = new Hashtable();
    private static Hashtable accum1Table = new Hashtable();
    private static List accumList = new ArrayList();
    private static Hashtable ticks = new Hashtable();
    private static Hashtable mems = new Hashtable();
    private static Hashtable tabs = new Hashtable();
    private static Hashtable traceMsgs = new Hashtable();
    private static String lastThreadName = "";
    private static long initMemory = 0;
    public static long lastMemory = 0;
    public static long lastTime = 0;
    static StringBuffer ts = new StringBuffer();
    static StringBuffer ms = new StringBuffer();
    static StringBuffer tms = new StringBuffer();
    static StringBuffer prefix = new StringBuffer();
    public static StringBuffer buff = new StringBuffer();
    private static List notThese = new ArrayList();
    private static List onlyThese = new ArrayList();

    public static List getNotThese() {
	return notThese;
    }

    public static List getOnlyThese() {
        return onlyThese;
    }

    public static void addNot(String pattern) {
        notThese.add(pattern);
    }

    public static void addOnly(String pattern) {
        onlyThese.add(pattern);
    }

    public static void removeOnly(String pattern) {
        onlyThese.remove(pattern);
    }

    /**
     * Clear out any of the patterns previously added by the addOnly call
     */
    public static void clearOnly() {
        onlyThese = new ArrayList();
    }

    static StringBuffer getBuffer() {
        Thread       t  = Thread.currentThread();
        StringBuffer sb = (StringBuffer) traceMsgs.get(t);
        if (sb == null) {
            sb = new StringBuffer();
            traceMsgs.put(t, sb);
        }

        return sb;
    }

    static Integer getTab() {
        Thread  t   = Thread.currentThread();
        Integer tab = (Integer) tabs.get(t);
        if (tab == null) {
            tab =  Integer.valueOf(0);
            tabs.put(t, tab);
        }

        return tab;
    }

    static int getCurrentTab() {
        return getTab().intValue();
    }

    public static void setShowLineNumbers(boolean v) {
        showLineNumber = v;
    }

    public static void startTrace() {
        displayMsg = true;
        initMemory = (Runtime.getRuntime().totalMemory()
                      - Runtime.getRuntime().freeMemory());
    }

    public static void stopTrace() {
        displayMsg = false;
    }

    public static boolean traceActive() {
        return displayMsg;
    }

    public static void deltaCurrentTab(int delta) {
        if ( !displayMsg) {
            return;
        }
        int v = getCurrentTab();
        tabs.put(Thread.currentThread(),  Integer.valueOf(v + delta));
    }

    public static void setFilters(String notTheseText, String onlyTheseText) {
        if (notTheseText != null) {
            notThese = StringUtil.split(notTheseText, "\n", true, true);
        }
        if (onlyTheseText != null) {
            onlyThese = StringUtil.split(onlyTheseText, "\n", true, true);
        }
    }

    private static boolean ok(String msg) {

        if (notThese.size() > 0) {
            if (StringUtil.findMatch(msg, notThese, null) != null) {
                return false;
            }
        }

        if (onlyThese.size() > 0) {
            if (StringUtil.findMatch(msg, onlyThese, null) == null) {
                return false;
            }
        }

        return true;
    }

    public static void call1(String m) {
        call1(m, "", true);
    }

    public static void call1(String m, boolean print) {
        call1(m, "", print);
    }

    public static void call1(String m, String extra) {
        call1(m, extra, true);
    }

    public static void call1(String m, String extra, boolean print) {
        if ( !displayMsg) {
            return;
        }

        if ( !ok(m)) {
            return;
        }

        synchronized (MUTEX) {
            if (print) {
                writeTrace(">" + m + " " + extra);
            }
            deltaCurrentTab(1);
            ticks.put(m, Long.valueOf(System.currentTimeMillis()));
            mems.put(m, Long.valueOf(Misc.usedMemory()));
        }
    }

    public static void call2(String m) {
        call2(m, "");
    }

    public static void call2(String m, String extra) {
        if ( !displayMsg) {
            return;
        }
        if ( !ok(m)) {
            return;
        }
        synchronized (MUTEX) {
            deltaCurrentTab(-1);
            long now        = System.currentTimeMillis();
            Long lastTime   = (Long) ticks.get(m);
            Long lastMemory = (Long) mems.get(m);
            if ((lastTime != null) && (lastMemory != null)) {
                long memDiff = Misc.usedMemory() - lastMemory.longValue();
                long then    = lastTime.longValue();
                writeTrace("<" + m + " ms: " + (now - then) + " " + extra);
                ticks.remove(m);
                mems.remove(m);
            } else {
                writeTrace(m + " NO LAST TIME");
            }
        }
    }

    public static void clearMsgs() {
        tabs      = new Hashtable();
        traceMsgs = new Hashtable();
    }

    public static void printMsgs() {
        for (java.util.Enumeration keys = traceMsgs.keys();
                keys.hasMoreElements(); ) {
            Object key = keys.nextElement();
            System.out.println(key);
            System.out.println(traceMsgs.get(key));
        }
        clearMsgs();
    }

    public static void before(String m) {
        if ( !displayMsg) {
            return;
        }
        synchronized (MUTEX) {
            writeTrace(m);
            deltaCurrentTab(1);
        }
    }

    public static void after(String m) {
        if ( !displayMsg) {
            return;
        }
        synchronized (MUTEX) {
            deltaCurrentTab(-1);
            writeTrace(m);
        }
    }

    public static void msg(String m) {
        if ( !displayMsg) {
            return;
        }
        if ( !ok(m)) {
            return;
        }
        synchronized (MUTEX) {
            writeTrace(m);
        }
    }

    private static void writeTrace(String msg) {
        String suff = "";

        if (showLineNumber) {
            List trace = StringUtil.split(LogUtil.getStackTrace(), "\n",
                                          true, true);
            for (int i = 0; i < trace.size(); i++) {
                //            System.err.println("line:" + trace.get(i));
                String line = (String) trace.get(i);
                if ((line.indexOf("(") >= 0)
                        && (line.indexOf("LogUtil.getStackTrace") < 0)
                        && (line.indexOf("Trace.java") < 0)
                        && (line.indexOf("Method") < 0)) {
                    line = line.substring(line.indexOf("(") + 1,
                                          line.length() - 1);
                    suff = "   " + line;

                    break;
                }
            }
        }

        Thread t              = Thread.currentThread();
        String crntThreadName = t.getName();
        if ( !crntThreadName.equals(lastThreadName)) {
            if (showThreadLabel) {
                System.out.println("Thread:" + crntThreadName);
            }
            lastThreadName = crntThreadName;
        }
        //      StringBuffer sb = getBuffer ();
        //      printTabs (sb);
        printTabs(null);
        System.out.print(msg + suff + "\n");
        LogUtil.consoleMessage(msg);
        //      sb.append (msg+"\n");
    }

    private static void printTabs(StringBuffer sb) {
        if ( !displayMsg) {
            return;
        }
        int tabs = getCurrentTab();
        long usedMemory2 = (Runtime.getRuntime().totalMemory()
                            - Runtime.getRuntime().freeMemory());
        if (initMemory == 0) {
            initMemory = usedMemory2;
        }

        long currentTime = System.currentTimeMillis();

        ts.setLength(0);
        ms.setLength(0);
        tms.setLength(0);
        prefix.setLength(0);
        ts.append((currentTime - lastTime));
        while (ts.length() < 4) {
            ts.append(" ");
        }

        ms.append((int) ((usedMemory2 - lastMemory) / 1000.0));
        while (ms.length() < 5) {
            ms.append(" ");
        }

        //      tms.append  ((int)((usedMemory2-initMemory)/1000.0));
        //      while (tms.length ()<5) {
        //          tms.append (" ");
        //      }

        if (lastTime == 0) {
            prefix.append("S   D     T");
        } else {
            //      prefix=ts+" "+ms+" "+tms+" ";
            prefix.append(ts.toString());
            prefix.append(" ");
            prefix.append(ms.toString());
            prefix.append(" ");
        }

        while (prefix.length() < 10) {
            prefix.append(" ");
        }

        System.out.print(prefix.toString());
        if (sb != null) {
            sb.append(prefix.toString());
        }

        for (int i = 0; i < tabs; i++) {
            if (sb != null) {
                sb.append("  ");
            }
            System.out.print("  ");
        }

        lastTime = currentTime;
        lastMemory = (Runtime.getRuntime().totalMemory()
                      - Runtime.getRuntime().freeMemory());
    }

    public static void accum1(String name) {
        if ( !displayMsg) {
            return;
        }
        //        Long l = Long.valueOf(System.currentTimeMillis());
        Long l = Long.valueOf(System.nanoTime());
        accum1Table.put(name, l);
    }

    public static void accum2(String name) {
        if ( !displayMsg) {
            return;
        }
        //        long time = System.currentTimeMillis();
        long time = System.nanoTime();
        Long l    = (Long) accum1Table.get(name);
        if (l == null) {
            msg("Cannot find accum:" + name);

            return;
        }
        long delta = time - l.longValue();
        Long total = (Long) accumTable.get(name);
        if (total == null) {
            total = Long.valueOf(delta);
            accumList.add(name);
        } else {
            total = Long.valueOf(total.longValue() + delta);
        }
        Integer cnt = (Integer) accumCntTable.get(name);
        if (cnt == null) {
            cnt = Integer.valueOf(1);
        } else {
            cnt = Integer.valueOf(cnt.intValue() + 1);
        }
        accumCntTable.put(name, cnt);
        accumTable.put(name, total);
    }

    public static void printAccum() {
        for (int i = 0; i < accumList.size(); i++) {
            String  name  = (String) accumList.get(i);
            Long    total = (Long) accumTable.get(name);
            Integer cnt   = (Integer) accumCntTable.get(name);
            long    nanos = total.longValue();
            msg(name + " Time:" + (nanos / 1000000) + " count:" + cnt);
        }
        accumList     = new ArrayList();
        accum1Table   = new Hashtable();
        accumCntTable = new Hashtable();
        accumTable    = new Hashtable();
    }

    public static void count(String name) {
        Integer i = (Integer) counters.get(name);
        if (i == null) {
            i = Integer.valueOf(0);
            counters.put(name, i);
            counterList.add(name);
        }
        i = Integer.valueOf(i.intValue() + 1);
        counters.put(name, i);
    }

    public static void printAndClearCount() {
        for (int i = 0; i < counterList.size(); i++) {
            String  name     = (String) counterList.get(i);
            Integer theCount = (Integer) counters.get(name);
            System.out.println("Count:" + name + "=" + theCount);
        }
        counterList = new ArrayList();
        counters    = new Hashtable();
    }

}
