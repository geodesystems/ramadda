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

package org.ramadda.repository;


import org.ramadda.repository.auth.*;

import org.ramadda.repository.database.*;
import org.ramadda.repository.harvester.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;

import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlTemplate;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.TTLCache;
import org.ramadda.util.TTLObject;

import org.ramadda.util.TempDir;
import org.ramadda.util.Utils;


import org.ramadda.util.sql.Clause;
import org.ramadda.util.sql.SqlUtil;

import org.w3c.dom.*;



import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;


import java.io.*;

import java.io.File;
import java.io.InputStream;



import java.net.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import java.text.SimpleDateFormat;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import java.util.regex.*;


/**
 * Class description
 *
 *
 * @version        $version$, Fri, Aug 23, '13
 * @author         Enter your name here...
 */
public abstract class EntryVisitor implements Constants {

    /** _more_ */
    private Repository repository;

    /** _more_ */
    private Request request;

    /** _more_ */
    private boolean recurse;

    /** _more_ */
    private int totalCnt = 0;

    /** _more_ */
    private int processedCnt = 0;

    /** _more_ */
    private Object actionId;

    /** _more_ */
    private StringBuffer sb = new StringBuffer();


    /**
     * _more_
     *
     * @param request _more_
     * @param repository _more_
     * @param actionId _more_
     * @param recurse _more_
     */
    public EntryVisitor(Request request, Repository repository,
                        Object actionId, boolean recurse) {
        this.repository = repository;
        this.request    = request;
        this.actionId   = actionId;
        this.recurse    = recurse;

    }

    /**
     * _more_
     *
     * @return _more_
     */
    public StringBuffer getMessageBuffer() {
        return sb;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Repository getRepository() {
        return repository;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Request getRequest() {
        return request;
    }

    /**
     * _more_
     *
     * @param object _more_
     */
    public void append(Object object) {
        sb.append(object);
    }

    /**
     * _more_
     *
     * @param by _more_
     */
    public void incrementProcessedCnt(int by) {
        processedCnt += by;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isRunning() {
        if (actionId == null) {
            return true;
        }

        return getRepository().getActionManager().getActionOk(actionId);
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public boolean entryOk(Entry entry) {
        if ( !entry.isGroup()) {
            //TODO: do we recurse
            //            return false;
        }
        if (entry.getTypeHandler().isSynthType()
                || getRepository().getEntryManager().isSynthEntry(
                    entry.getId())) {
            return false;
        }

        return true;
    }

    /**
     * _more_
     */
    public void updateMessage() {
        if (actionId != null) {
            getRepository().getActionManager().setActionMessage(actionId,
                    "# entries:" + totalCnt + "<br># changed entries:"
                    + processedCnt);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Object getActionId() {
        return actionId;
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean walk(Entry entry) throws Exception {
        //        System.err.println("Walk: " + entry);
        if ( !isRunning()) {
            System.err.println("\t- not running");

            return true;
        }
        if ( !entryOk(entry)) {
            System.err.println("\tEntry not ok");

            return true;
        }
        totalCnt++;
        updateMessage();
        List<Entry> children =
            getRepository().getEntryManager().getChildren(request, entry);
        if (children == null) {
            System.err.println("\tNo children");

            return true;
        }
        if (recurse) {
            for (Entry child : children) {
                if ((actionId != null)
                        && !getRepository().getActionManager().getActionOk(
                            actionId)) {
                    return false;
                }
                if ( !walk(child)) {
                    return false;
                }
            }
        }

        if ( !processEntry(entry, children)) {
            return false;
        }

        return true;
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param children _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public abstract boolean processEntry(Entry entry, List<Entry> children)
     throws Exception;

}
