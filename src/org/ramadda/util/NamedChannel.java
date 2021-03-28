/*
* Copyright (c) 2008-2021 Geode Systems LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*     http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
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
     * @param inputStream _more_
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
