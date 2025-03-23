/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import java.io.*;

import java.nio.*;
import java.nio.channels.*;


/**
 * @author Jeff McWhirter
 */

public class NamedChannel {

    /** _more_ */
    String name;

    /** _more_ */
    ReadableByteChannel channel;

    /**
     * _more_
     *
     * @param name _more_
     * @param channel _more_
     */
    public NamedChannel(String name, ReadableByteChannel channel) {
        this.name    = name;
        this.channel = channel;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getName() {
        return name;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public ReadableByteChannel getChannel() {
        return channel;
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void close() throws Exception {
        if ( !name.equals("stdin")) {
            channel.close();
        }
    }

}
