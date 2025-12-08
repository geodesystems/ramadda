/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

public class BufferMapList<T> extends MapList<T, Appendable> {

    public Appendable get(T key) {
        Appendable sb = super.get(key);
        if (sb == null) {
            sb = new StringBuilder();
            initNewBuffer(sb);
            super.put(key, sb);
        }

        return sb;
    }

    public Appendable peek(T key) {
        return super.get(key);
    }

    public void initNewBuffer(Appendable sb) {}

}
