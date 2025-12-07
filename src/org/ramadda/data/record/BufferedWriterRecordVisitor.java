/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.record;

import java.io.*;

public abstract class BufferedWriterRecordVisitor {
    protected StringBuffer buffer = new StringBuffer();
    protected PrintWriter pw;

    public BufferedWriterRecordVisitor(PrintWriter pw) {}

    public void finished(RecordFile file, VisitInfo visitInfo) {
        if (buffer.length() > 0) {
            synchronized (pw) {
                pw.append(buffer);
            }
        }
    }

}
