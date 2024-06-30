/**
Copyright (c) 2008-2024 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository;
import org.ramadda.repository.util.SelectInfo;
import java.util.List;

public abstract class EntryVisitor implements Constants {
    private Repository repository;
    private Request request;
    private boolean recurse;
    private int totalCnt = 0;
    private int processedCnt = 0;
    private Object actionId;
    private StringBuffer sb = new StringBuffer();
    private SelectInfo selectInfo;
    public EntryVisitor(Request request, Repository repository,
                        Object actionId, boolean recurse) {
        this.repository = repository;
        this.request    = request;
        this.actionId   = actionId;
        this.recurse    = recurse;
	selectInfo = new SelectInfo(request);
	selectInfo.setSyntheticOk(false);
    }

    public StringBuffer getMessageBuffer() {
        return sb;
    }

    public void resetMessageBuffer() {
        sb = new StringBuffer();
    }

    public Repository getRepository() {
        return repository;
    }

    public Request getRequest() {
        return request;
    }

    public void append(Object object) {
        sb.append(object);
    }

    
    public void incrementProcessedCnt(int by) {
        processedCnt += by;
    }

    public int getProcessedCount() {
	return processedCnt;
    }

    
    public boolean isRunning() {
        if (actionId == null) {
            return true;
        }

        return getRepository().getActionManager().getActionOk(actionId);
    }

    
    public boolean entryOk(Entry entry) {
        if (//entry.getTypeHandler().isSynthType()|| 
	    getRepository().getEntryManager().isSynthEntry(
                    entry.getId())) {
            return false;
        }

        return true;
    }

    
    
    public void updateMessage() {
        if (actionId != null) {
            getRepository().getActionManager().setActionMessage(actionId,
                    "# entries:" + totalCnt + "<br># changed entries:"
                    + processedCnt + "<br>" + sb);
        }
    }

    
    public Object getActionId() {
        return actionId;
    }

    

    public boolean walk(List<Entry> entries) throws Exception {
	boolean ok = true;
	for(Entry entry:entries) {
	    ok = walk(entry);
	    if(!ok) break;
	}
	return ok;
    }

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

    
    public abstract boolean processEntry(Entry entry, List<Entry> children)
     throws Exception;

}
