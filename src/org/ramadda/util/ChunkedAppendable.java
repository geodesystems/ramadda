/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import java.io.IOException;

import java.util.ArrayList;


import java.util.List;


/**
 * Class description
 *
 *
 * @version        $version$, Sat, Oct 17, '15
 * @author         Enter your name here...
 */
public class ChunkedAppendable implements Appendable {

    /** _more_ */
    private int size;

    /** _more_ */
    private List<StringBuilder> buffers = new ArrayList<StringBuilder>();

    /** _more_ */
    private boolean changed = false;

    /**
     * _more_
     *
     * @param chunkSize _more_
     */
    public ChunkedAppendable(int chunkSize) {
        this.size = chunkSize;
        buffers.add(new StringBuilder());
    }

    /**
     * _more_
     */
    private ChunkedAppendable() {}

    /**
     * _more_
     *
     * @return _more_
     */
    public List<StringBuilder> getBuffers() {
        return buffers;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getChanged() {
        return changed;
    }

    /**
     * _more_
     *
     *
     * @param nextSize _more_
     * @return _more_
     */
    private Appendable getAppendable(int nextSize) {
        changed = true;
        StringBuilder sb = buffers.get(buffers.size() - 1);
        if (sb.length() + nextSize > size) {
            sb = new StringBuilder();
            buffers.add(sb);
        }

        return sb;
    }

    /**
     * _more_
     *
     * @param c _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    @Override
    public Appendable append(char c) throws IOException {
        getAppendable(1).append(c);

        return this;
    }

    /**
     * _more_
     *
     * @param c _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    @Override
    public Appendable append(CharSequence c) throws IOException {
        getAppendable(c.length()).append(c);

        return this;
    }

    /**
     * _more_
     *
     * @param c _more_
     * @param start _more_
     * @param end _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    @Override
    public Appendable append(CharSequence c, int start, int end)
            throws IOException {
        getAppendable(end - start).append(c, start, end);

        return this;
    }
}

;
