/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.db;

import org.ramadda.util.NamedBuffer;

public class DbNamedBuffer extends NamedBuffer {
    String anchor;

    DbNamedBuffer(String name) {
        super(name);
    }

    DbNamedBuffer(String name, String b) {
        super(name, b);
    }

    DbNamedBuffer(String name, String b, String anchor) {
        super(name, b);
        this.anchor = anchor;
    }
}
