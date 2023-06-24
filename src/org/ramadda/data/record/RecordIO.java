/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.record;

import org.ramadda.util.text.TextReader;

import java.io.*;


/**
 * This class  is  a holder for various IO capabilities. It needs a core InputStream or  OutputStream.
 * This holds wrappers around those streams - e.g., DataInputStream, DataOutputStream, PrintWriter, etc.
 * This is used in conjunction with the record file reading and writing.
 *
 * @author  Jeff McWhirter
 */
public class RecordIO {

    /** _more_ */
    static int cnt = 0;

    /** _more_ */
    public String myid = "RecordIO-" + (cnt++);

    /** the input stream */
    private InputStream inputStream;

    /** the output stream */
    private OutputStream outputStream;

    /** reader */
    private BufferedReader bufferedReader;

    /** data input stream */
    private DataInputStream dataInputStream;

    /** data output stream */
    private DataOutputStream dataOutputStream;

    /** print writer */
    private PrintWriter printWriter;

    /** _more_ */
    private String putBackLine;



    /**
     * ctor
     *
     * @param reader initialize with a buffered reader
     */
    public RecordIO(BufferedReader reader) {
        bufferedReader = reader;
    }

    /**
     * ctor
     *
     * @param inputStream input stream
     */
    public RecordIO(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * ctor
     *
     * @param outputStream output stream
     */
    public RecordIO(OutputStream outputStream) {
        this.outputStream = outputStream;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isOk() {
        return (inputStream != null) || (bufferedReader != null);
    }



    /**
     * Copy ctor
     *
     * @param that what to copy
     */
    public void reset(RecordIO that) {
        close();
        this.inputStream      = that.inputStream;
        this.outputStream     = that.outputStream;
        this.bufferedReader   = that.bufferedReader;
        this.dataInputStream  = that.dataInputStream;
        this.dataOutputStream = that.dataOutputStream;
        this.printWriter      = that.printWriter;
    }

    /**
     * Close all of the  streams
     *
     */
    public void close() {
        try {
            if (dataInputStream != null) {
                dataInputStream.close();
            }
        } catch (Exception ignore) {}

        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (Exception ignore) {}


        try {
            if (printWriter != null) {
                printWriter.close();
            }
        } catch (Exception ignore) {}

        try {
            if (dataOutputStream != null) {
                dataOutputStream.close();
            }
        } catch (Exception ignore) {}

        try {
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (Exception ignore) {}
    }

    /**
     * Return the input stream
     *
     * @return the input stream
     *
     * @throws IOException On badness
     */
    public InputStream getInputStream() throws IOException {
        return inputStream;
    }

    /**
     * Return the output stream
     *
     * @return the output stream
     *
     * @throws IOException On badness
     */
    public OutputStream getOutputStream() throws IOException {
        return outputStream;
    }


    /**
     * Create if needed and return the DataInputStream
     *
     * @return The DataInputStream
     *
     * @throws IOException On badness
     */
    public DataInputStream getDataInputStream() throws IOException {
        if (dataInputStream == null) {
            dataInputStream = new DataInputStream(getInputStream());
        }

        return dataInputStream;
    }


    /**
     * Create if needed and return the DataOutputStream
     *
     * @return The DataOutputStream
     *
     * @throws IOException On badness
     */
    public DataOutputStream getDataOutputStream() throws IOException {
        if (dataOutputStream == null) {
            dataOutputStream = new DataOutputStream(outputStream);
        }

        return dataOutputStream;
    }



    /**
     * Create if needed and return the BufferedReader
     *
     * @return The BufferedReader
     *
     * @throws IOException On badness
     */
    public BufferedReader getBufferedReader() throws IOException {
        if (bufferedReader == null) {
            bufferedReader =
                new BufferedReader(new InputStreamReader(getInputStream()));
        }

        return bufferedReader;
    }

    private TextReader textReader;
    public TextReader getTextReader() throws Exception {
	if(textReader==null)
	    textReader = new TextReader();
	textReader.setReader(getBufferedReader());
	return textReader;
    }

    /**
     * Read a line from the buffered reader
     *
     * @return The read line
     *
     * @throws IOException On badness
     */
    public String readLine() throws Exception {
        if (putBackLine != null) {
            String s = putBackLine;
            putBackLine = null;

            return s;
        }
	return getTextReader().readLine();
    }

    /**
     * _more_
     *
     * @param line _more_
     */
    public void putBackLine(String line) {
        putBackLine = line;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getAndClearPutback() {
        String s = putBackLine;
        putBackLine = null;

        return s;

    }

    /**
     * Create if needed and return the PrintWriter
     *
     * @return The PrintWriter
     *
     * @throws IOException On badness
     */
    public PrintWriter getPrintWriter() throws IOException {
        if (printWriter == null) {
            printWriter = new PrintWriter(getOutputStream());
        }

        return printWriter;
    }

}
