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


import java.util.List;


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

    public void resetMessageBuffer() {
	sb = new StringBuffer();
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
                    + processedCnt+"<br>"+ sb);
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
	try {
	    boolean ok = walkInner(entry);
	    return ok;
	} finally {
	    finished();
	}
    }

    public void finished() {}

    private boolean walkInner(Entry entry) throws Exception {	

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
                if ( !walkInner(child)) {
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
