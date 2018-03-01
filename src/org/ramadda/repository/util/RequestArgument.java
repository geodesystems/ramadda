/*
* Copyright (c) 2008-2018 Geode Systems LLC
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

package org.ramadda.repository.util;


import org.ramadda.repository.Request;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.output.*;

import org.ramadda.sql.SqlUtil;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.util.WrapperException;
import ucar.unidata.xml.XmlUtil;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import java.lang.reflect.*;



import java.net.*;




import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.regex.*;



import java.util.regex.*;

import javax.servlet.*;
import javax.servlet.http.*;


/**
 *
 *
 * @author RAMADDA Development Team
 */
public class RequestArgument {

    /** _more_ */
    private String argsProperty;


    /** _more_ */
    private List<String> args;


    /**
     * _more_
     *
     * @param argsProperty _more_
     */
    public RequestArgument(String argsProperty) {
        this.argsProperty = argsProperty;
    }


    /**
     * _more_
     *
     * @param args _more_
     */
    public RequestArgument(String[] args) {
        this.args = new ArrayList<String>();
        for (String arg : args) {
            this.args.add(arg);
        }
    }

    /**
     * _more_
     *
     *
     * @param request _more_
     * @return _more_
     */
    public List<String> getArgs(Request request) {
        if (args == null) {
            args = StringUtil.split(
                request.getRepository().getProperty(argsProperty, ""));
        }

        return args;
    }



}
