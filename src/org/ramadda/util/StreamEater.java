/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import java.io.*;


/**
 * Class to eat a stream
 */
public class StreamEater extends Thread {

    /** the input stream */
    private InputStream in;

    /** The place to write the lines to */
    private PrintWriter pw;

    /** The type name (for debugging) */
    private String type;

    /** _more_ */
    private boolean running = false;


    /**
     * A class for reading lines from an input stream in a thread
     *
     * @param in  InputStream
     * @param pw  the writer for the output
     */
    public StreamEater(InputStream in, PrintWriter pw) {
        this(in, pw, "StreamEater");
    }

    /**
     * A class for reading lines from an input stream in a thread
     *
     * @param in  InputStream
     * @param pw  the writer for the output
     * @param type a string for debugging
     */
    public StreamEater(InputStream in, PrintWriter pw, String type) {
        this.in   = in;
        this.pw   = pw;
        this.type = type;
    }

    /**
     * Run the eater
     */
    @Override
    public void run() {
        running = true;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(in));
            String line = null;
            while ((line = br.readLine()) != null) {
                pw.println(line);
                //System.out.println(line);
            }
            //System.out.println("Done reading " + type);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            running = false;
            try {
                pw.close();
                br.close();
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getRunning() {
        return running;
    }

}
