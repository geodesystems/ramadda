/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;

import java.io.*;

import java.nio.*;
import java.nio.channels.*;


public class NamedChannel {
    String name;
    ReadableByteChannel channel;

    public NamedChannel(String name, ReadableByteChannel channel) {
        this.name    = name;
        this.channel = channel;
    }

    public String getName() {
        return name;
    }

    public ReadableByteChannel getChannel() {
        return channel;
    }

    public void close() throws Exception {
        if ( !name.equals("stdin")) {
            channel.close();
        }
    }

}
