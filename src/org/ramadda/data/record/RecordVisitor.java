/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.record;

import java.util.ArrayList;
import java.util.List;
import java.io.*;

public abstract class RecordVisitor {
    public RecordVisitor() {
    }

    public abstract boolean visitRecord(RecordFile file, VisitInfo visitInfo,
                                        BaseRecord record)
     throws Exception;

    /**
     * This gets called when the visitor is done visiting the given record file
     *
     * @param file file we just visited
     * @param visitInfo visit info
     *
     * @throws Exception _more_
     */
    public void finished(RecordFile file, VisitInfo visitInfo)
            throws Exception {}

    public void close(VisitInfo visitInfo) {}

}
