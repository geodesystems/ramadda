/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChunkedAppendable implements Appendable {
    private int size;
    private List<StringBuilder> buffers = new ArrayList<StringBuilder>();
    private boolean changed = false;

    public ChunkedAppendable(int chunkSize) {
        this.size = chunkSize;
        buffers.add(new StringBuilder());
    }

    private ChunkedAppendable() {}

    public List<StringBuilder> getBuffers() {
        return buffers;
    }

    public boolean getChanged() {
        return changed;
    }

    private Appendable getAppendable(int nextSize) {
        changed = true;
        StringBuilder sb = buffers.get(buffers.size() - 1);
        if (sb.length() + nextSize > size) {
            sb = new StringBuilder();
            buffers.add(sb);
        }

        return sb;
    }

    @Override
    public Appendable append(char c) throws IOException {
        getAppendable(1).append(c);

        return this;
    }

    @Override
    public Appendable append(CharSequence c) throws IOException {
        getAppendable(c.length()).append(c);

        return this;
    }

    @Override
    public Appendable append(CharSequence c, int start, int end)
            throws IOException {
        getAppendable(end - start).append(c, start, end);

        return this;
    }
}

;
