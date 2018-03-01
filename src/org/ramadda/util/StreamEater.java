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
