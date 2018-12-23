/*
* Copyright (c) 2008-2019 Geode Systems LLC
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


import ucar.unidata.util.DateUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import java.io.*;

import java.net.URL;
import java.net.URLConnection;


import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;

import java.util.GregorianCalendar;

import java.util.List;




import java.util.TimeZone;
import java.util.regex.*;


/**
 * Class LdmListener _more_
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class LdmListener {

    /** timezone */
    public static final TimeZone TIMEZONE_DEFAULT =
        TimeZone.getTimeZone("UTC");


    /** _more_ */
    private boolean debug = false;

    /** _more_ */
    private BufferedReader br;

    /** _more_ */
    private SimpleDateFormat yearSdf;

    /** _more_ */
    private SimpleDateFormat monthSdf;

    /** _more_ */
    private List files = new ArrayList();

    /** _more_ */
    private Pattern pattern;

    /** _more_ */
    private String type = "any";

    /** _more_ */
    private String bufferFile;

    /** _more_ */
    private FileOutputStream bufferOS;

    /** _more_ */
    int bufferCnt = 0;

    /** _more_ */
    long startTime;

    /** _more_ */
    int cnt = 0;

    /** _more_ */
    private Object FILES_MUTEX = new Object();

    /** _more_ */
    private Object PROCESS_MUTEX = new Object();

    // = "SDUS[2357]. .... ([0-3][0-9])([0-2][0-9])([0-6][0-9]).*/p(...)(...)";

    /** _more_ */
    private String patternString;
    //"/data/ldm/gempak/nexrad/NIDS/\\5/\\4/\\4_(\\1:yyyy)(\\1:mm)\\1_\\2\\3";

    /** _more_ */
    private String fileTemplate;


    /** _more_ */
    private String getFileUrlTemplate =
        "http://localhost:8080/repository/harvester/processfile?file=${file}&type=${type}";

    /** _more_ */
    private String bufferUrlTemplate =
        "http://localhost:8080/repository/harvester/processfile?tocfile=${file}";

    /**
     * _more_
     *
     * @param msg _more_
     */
    private void usage(String msg) {
        System.err.println(msg);
        System.err.println(
            "usage: LdmListener -pattern <product pattern> -template <file template> -debug -type <repository type>");
        System.exit(1);
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public LdmListener(String[] args) throws Exception {
        processArgs(args);
        System.err.println("pattern:" + patternString);
        pattern = Pattern.compile(patternString);
        InputStreamReader sr = new InputStreamReader(System.in);
        br      = new BufferedReader(sr);
        yearSdf = new SimpleDateFormat();
        yearSdf.setTimeZone(TIMEZONE_DEFAULT);
        yearSdf.applyPattern("yyyy");

        monthSdf = new SimpleDateFormat();
        monthSdf.setTimeZone(TIMEZONE_DEFAULT);
        monthSdf.applyPattern("MM");
        startTime = System.currentTimeMillis();
        Misc.run(new Runnable() {
            public void run() {
                checkFiles();
            }
        });
        processIncoming();
        System.exit(0);
    }



    /**
     * _more_
     *
     * @param args _more_
     */
    private void processArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-pattern")) {
                if (i == args.length - 1) {
                    usage("Incorrect input");
                }
                patternString = args[++i];
            } else if (args[i].equals("-debug")) {
                debug = true;
            } else if (args[i].equals("-template")) {
                if (i == args.length - 1) {
                    usage("Incorrect input");
                }
                fileTemplate = args[++i];
            } else if (args[i].equals("-bufferfile")) {
                if (i == args.length - 1) {
                    usage("Incorrect input");
                }
                bufferFile = args[++i];
            } else if (args[i].equals("-type")) {
                if (i == args.length - 1) {
                    usage("Incorrect input");
                }
                type = args[++i];
            } else {
                usage("Unknown argument:" + args[i]);
            }
        }
        if (patternString == null) {
            usage("No -pattern given");
        }
        if (fileTemplate == null) {
            usage("No -template given");
        }
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void processIncoming() throws Exception {
        while (true) {
            try {
                String line = br.readLine();
                if (line != null) {
                    processLine(line);
                } else {
                    Misc.sleep(100);
                }
            } catch (IOException e) {
                break;
            }
        }
    }


    /**
     * _more_
     *
     * @param line _more_
     */
    private void processLine(String line) {

        Matcher matcher = pattern.matcher(line);
        if ( !matcher.find()) {
            if (debug) {
                System.err.println("no match:" + line);
            }

            return;
        }


        if (debug) {
            if ((cnt++) % 50 == 0) {
                double minutes = (System.currentTimeMillis() - startTime)
                                 / 1000.0 / 60.0;
                if (minutes > 0) {
                    System.err.println("#" + cnt + " rate: "
                                       + ((int) (cnt / (double) minutes))
                                       + "/minute");
                }
            }
        }
        String filename = fileTemplate;

        Date   now      = new Date();
        String year     = yearSdf.format(now);
        String month    = monthSdf.format(now);

        int    count    = matcher.groupCount();
        for (int groupIdx = 1; groupIdx <= count; groupIdx++) {
            String match = matcher.group(groupIdx);
            filename = filename.replace("(\\" + groupIdx + ":yyyy)", year);
            filename = filename.replace("(\\" + groupIdx + ":mm)", month);
            filename = filename.replace("\\" + groupIdx + "", match);
        }

        File f   = new File(filename);
        int  cnt = 0;
        if (debug) {
            System.err.println("file:" + filename + " exists:" + f.exists());
        }
        addFile(f);
    }


    /**
     * _more_
     */
    private void checkFiles() {
        while (true) {
            if (bufferCnt > 500) {}
            if (files.size() > 0) {
                List tmp;
                synchronized (FILES_MUTEX) {
                    tmp   = new ArrayList(files);
                    files = new ArrayList();
                }
                for (int i = 0; i < tmp.size(); i++) {
                    File f = (File) tmp.get(i);
                    if ( !f.exists()) {
                        addFile(f);
                    } else {
                        processFile(f);
                    }
                }
            }
            Misc.sleep(1000);
        }
    }

    /**
     * _more_
     *
     * @param f _more_
     */
    private void addFile(File f) {
        synchronized (FILES_MUTEX) {
            files.add(f);
        }
    }


    /**
     * _more_
     *
     * @param f _more_
     *
     * @throws Exception _more_
     */
    private void writeToBuffer(File f) throws Exception {
        if (bufferOS == null) {
            bufferOS = new FileOutputStream(bufferFile, true);
        }
        String s = type + ":" + f + "\n";
        bufferOS.write(s.getBytes());
        bufferOS.flush();
        bufferCnt++;
    }

    /**
     * _more_
     *
     * @param f _more_
     *
     * @return _more_
     */
    private boolean processFile(File f) {
        synchronized (PROCESS_MUTEX) {
            if (bufferFile != null) {
                try {
                    writeToBuffer(f);
                } catch (Exception exc) {
                    bufferOS = null;
                    System.out.println("error:" + exc);
                    addFile(f);

                    return false;
                }
            }

            String urlString = getFileUrlTemplate.replace("${file}",
                                   f.toString());
            urlString = urlString.replace("${type}", type);
            try {
                URL           url        = new URL(urlString);
                URLConnection connection = url.openConnection();
                InputStream   s          = connection.getInputStream();
                String        results    = IOUtil.readInputStream(s);
                if ( !results.equals("OK")) {
                    addFile(f);
                    if (debug) {
                        System.out.println("connection not successful:"
                                           + results);
                    }

                    return false;
                }
            } catch (Exception exc) {
                System.out.println("error:" + exc);
                addFile(f);

                return false;
            }

            return true;
        }
    }




    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        LdmListener listener = new LdmListener(args);
    }


}
