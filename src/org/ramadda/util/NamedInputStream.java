/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;

import java.io.*;

public class NamedInputStream {

    String name;

    InputStream inputStream;

    public NamedInputStream(String name, InputStream inputStream) {
        this.name        = name;
        this.inputStream = inputStream;
    }

    public String getName() {
        return name;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void close() throws Exception {
        if (inputStream != System.in) {
            IO.close(inputStream);
        }
    }

    public String toString() {
        return "NamedInputStream:" + name;
    }

}
