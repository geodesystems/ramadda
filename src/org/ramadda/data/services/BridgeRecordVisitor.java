/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.services;

import org.ramadda.data.point.*;

import org.ramadda.data.record.*;
import org.ramadda.data.record.*;
import org.ramadda.repository.*;

import ucar.unidata.util.IOUtil;

import java.io.*;

/**
 * Is used when we are visiting point files. This counts the points that are visited
 * and provides some IO stuff
 *
 *
 * @author Jeff McWhirter
 */
public abstract class BridgeRecordVisitor extends RecordVisitor {

    /** Have we closed */
    private boolean closed = false;

    /** The output handler */
    RecordOutputHandler handler;

    /** Synchronizes access to the output streams */
    protected Object MUTEX = new Object();

    /** Keep track of the number of points */
    int numPoints = 0;

    /** Job processing ID */
    Object processId;

    /** User request */
    Request request;

    /** The entry we are dealing with */
    Entry mainEntry;

    /** File suffix when creating product files */
    String suffix;

    /** The output stream */
    private OutputStream os;

    /** For text output */
    PrintWriter pw;

    /** For binary output */
    private DataOutputStream dos;

    public BridgeRecordVisitor(RecordOutputHandler handler) {
        this.handler = handler;
    }

    public BridgeRecordVisitor(RecordOutputHandler handler,
                               Object processId) {
        this.handler   = handler;
        this.processId = processId;
    }

    public BridgeRecordVisitor(RecordOutputHandler handler, Request request,
                               Object processId, Entry mainEntry,
                               String suffix) {
        this.handler   = handler;
        this.request   = request;
        this.processId = processId;
        this.mainEntry = mainEntry;
        this.suffix    = suffix;
    }

    public BridgeRecordVisitor() {}

    public RecordOutputHandler getHandler() {
        return handler;
    }

    public Object getProcessId() {
        return processId;
    }

    /**
     * Creates if needed and returns  the output stream
     *
     * @return output stream
     *
     * @throws Exception On badness
     */
    public DataOutputStream getTheDataOutputStream() throws Exception {
        checkState();
        if (dos == null) {
            synchronized (MUTEX) {
                if (dos == null) {
                    dos = handler.getDataOutputStream(request, processId,
                            mainEntry, suffix);
                }
            }
        }

        return dos;
    }

    /**
     * Creates if needed and returns  the print writer
     *
     * @return print writer
     *
     * @throws Exception On badness
     */
    public PrintWriter getThePrintWriter() throws Exception {
        checkState();
        if (pw == null) {
            synchronized (MUTEX) {
                if (pw == null) {
                    pw = handler.getPrintWriter(request, processId,
                            mainEntry, suffix);
                }
            }
        }

        return pw;
    }

    /**
     * Create the output stream
     *
     * @return output stream
     *
     * @throws Exception On badness
     */
    public OutputStream getTheOutputStream() throws Exception {
        checkState();
        if (os == null) {
            synchronized (MUTEX) {
                if (os == null) {
                    os = handler.getOutputStream(request, processId,
                            mainEntry, suffix);
                }
            }
        }

        return os;
    }

    /**
     * Make sure we don't try to do anything  after we're close
     */
    private void checkState() {
        if (closed) {
            throw new IllegalStateException("RecordVisitor has been closed");
        }
    }

    /**
     * Called when we're done visiting the ldiar records
     *
     * @param visitInfo visit info
     */
    @Override
    public void close(VisitInfo visitInfo) {
        closed = true;
        super.close(visitInfo);
        if (pw != null) {
	    pw.flush();
            pw.close();
            pw = null;
        }
        if (os != null) {
            IOUtil.close(os);
            os = null;
        }
        if (dos != null) {
            IOUtil.close(dos);
            dos = null;
        }
    }

    /**
     * Gets called by the RecordFile when we are visiting a record file
     * This tracks and checks a few things then calls doVisitRecord which will be overwritten
     * to do the actual work.
     *
     * @param file The file we are visiting
     * @param visitInfo Tracks state
     * @param record The record we have just read
     *
     * @return Everything is cool
     */
    public final boolean visitRecord(RecordFile file, VisitInfo visitInfo,
                                     BaseRecord record) {
        numPoints++;
        if ((handler != null) && !handler.jobOK(processId)) {
            return false;
        }
        try {
            return doVisitRecord(file, visitInfo, record);
        } catch (Exception exc) {
            System.err.println("Error:" + exc);
            exc.printStackTrace();

            throw new RuntimeException(exc);
        }
    }

    /**
     * Visit the record
     *
     * @param file record file
     * @param visitInfo tracks state
     * @param record the record we just read
     *
     * @return everything is cool
     *
     * @throws Exception On badness
     */
    public abstract boolean doVisitRecord(RecordFile file,
                                          VisitInfo visitInfo,
                                          BaseRecord record)
     throws Exception;

    /**
     * We can keep a string buffer attached to the file to use.
     *
     * @param file record file
     *
     * @return buffer
     */
    public StringBuffer getBuffer(RecordFile file) {
        StringBuffer buffer = (StringBuffer) file.getProperty("buffer");
        if (buffer == null) {
            file.putProperty("buffer", buffer = new StringBuffer());
        }

        return buffer;
    }

    /**
     * getter
     *
     * @return num points
     */
    public int getNumPoints() {
        return numPoints;
    }

}
