/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository;

import org.ramadda.repository.util.SelectInfo;

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


    private SelectInfo selectInfo;

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
	selectInfo = new SelectInfo(request);
	selectInfo.setSyntheticOk(false);
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
     */
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

    public int getProcessedCount() {
	return processedCnt;
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
        if (//entry.getTypeHandler().isSynthType()|| 
	    getRepository().getEntryManager().isSynthEntry(
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
                    + processedCnt + "<br>" + sb);
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

    /**
     */
    public void finished() {}


    /**
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private boolean walkInner(Entry entry) throws Exception {

        //        System.err.println("Walk: " + entry);
        if ( !isRunning()) {
            System.err.println("EntryVisitor: not running");

            return true;
        }
        if ( !entryOk(entry)) {
            System.err.println("EntryVisitor: Entry not ok:" + entry +" " + entry.getId());
            return true;
        }
        totalCnt++;
        updateMessage();
        List<Entry> children =
            getRepository().getEntryManager().getChildren(request, entry,selectInfo);
        if (children == null) {
            System.err.println("EntryVisitor:No children");
            return true;
        }
        if ( !processEntry(entry, children)) {
            return false;
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
