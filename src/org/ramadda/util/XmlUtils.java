/*
* Copyright (c) 2008-2019 Geode Systems LLC
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


import org.w3c.dom.*;

import org.xml.sax.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;



import ucar.unidata.xml.*;



import java.io.ByteArrayInputStream;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.security.SignatureException;



import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


import javax.xml.parsers.*;



/**
 * A collection of utilities for xml.
 *
 * @author IDV development team
 */

public abstract class XmlUtils {


    /**
     * _more_
     *
     * @param sb _more_
     * @param bytes _more_
     *
     * @throws Exception _more_
     */
    public static void appendCdataBytes(Appendable sb, byte[] bytes)
            throws Exception {
        sb.append("<![CDATA[");
        sb.append(XmlUtil.encodeBase64(bytes));
        sb.append("]]>");
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param s _more_
     *
     * @throws Exception _more_
     */
    public static void appendCdata(Appendable sb, String s) throws Exception {
        sb.append("<![CDATA[");
        sb.append(s);
        sb.append("]]>");
    }


}
