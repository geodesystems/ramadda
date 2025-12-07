/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.record;

import java.io.*;

public class RecordCountVisitor extends RecordVisitor {
    private int cnt = 0;
    public RecordCountVisitor() {}

    public boolean visitRecord(RecordFile file, VisitInfo visitInfo,
                               BaseRecord record) {
        cnt++;

        return true;
    }

    public int getCount() {
        return cnt;
    }

}
