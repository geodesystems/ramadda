/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;

import org.ramadda.util.Utils;
import org.ramadda.util.sql.SqlUtil;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.JobManager;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.util.concurrent.Future;


import java.io.File;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

public class ActionManager extends RepositoryManager {

    public RequestUrl URL_STATUS = new RequestUrl(this, "/status");

    private Hashtable<Object, ActionInfo> actions = new Hashtable<Object,
                                                        ActionInfo>();

    public ActionManager(Repository repository) {
        super(repository);
    }

    public void setContinueHtml(Object actionId, String html) {
        if (actionId == null) {
            return;
        }
        ActionInfo action = getAction(actionId);
        if (action != null) {
            action.setContinueHtml(html);
        }
    }

    public Result makeResult(Request request, String title, String status,
                             StringBuffer sb, boolean json)
            throws Exception {
        if (json) {
            String result = JsonUtil.map(Utils.makeListFromValues("status",
                                JsonUtil.quote(status), "message",
                                JsonUtil.quote(sb.toString())));

            return new Result(result, Result.TYPE_JSON);
        }

        return new Result(title, sb);
    }

    public Result processStatus(Request request) throws Exception {
        boolean      json   = request.getString("output", "").equals("json");
        String       status = "";
        String       id     = request.getString(ARG_ACTION_ID, "");
        ActionInfo   action = getAction(id);
        StringBuffer sb     = new StringBuffer();
        if (action == null) {
            sb.append("No action found");

            return makeResult(request, "Status", "noaction", sb, json);
        }

        if ( !json) {
            if (action.getEntry() != null) {
                getPageHandler().entrySectionOpen(request, action.getEntry(),
                        sb, "Action: " + action.getName());
            } else {
                getPageHandler().sectionOpen(request, sb, "Action: "
					     + action.getName(),false);
            }
        }

        if (request.exists(ARG_CANCEL)) {
            action.setRunning(false);
            actions.remove(id);
            sb.append("Action cancelled");
            status = "canceled";
            JobManager.getManager().stopLoad(id);
	    String url = action.getRedirectUrl();
	    if(url!=null) return new Result(url);
        } else {
            if (action.getError() != null) {
                if ( !json) {
                    sb.append(getPageHandler().showDialogError("Error"
                            + "<p>" + action.getError()));
                } else {
                    status = "error";
                    sb.append(action.getError());
                }
                actions.remove(id);
            } else if ( !action.getRunning()) {
                StringBuilder message = new StringBuilder();
                status = "complete";
                if (json) {
                    sb.append(action.getContinueHtml());
                } else {
                    message.append("Completed");
                    sb.append(
                        getPageHandler().showDialogNote(
                            message.toString(), action.getContinueHtml()));
                }
                actions.remove(id);
            } else {
                status = "running";
                String msg = JobManager.getManager().getDialogLabel2(id);
                if ( !json) {
                    sb.append("<meta http-equiv=\"refresh\" content=\"2\">");
                    sb.append(getRepository().progress("In progress"));
                    sb.append(HtmlUtils.href(request.makeUrl(URL_STATUS,
                            ARG_ACTION_ID, id), "Reload"));
                    sb.append("<p>");
                }
                if (action.getExtraHtml() != null) {
                    sb.append(action.getExtraHtml());
                }
                if (action.getExtraHtml() != null) {
                    sb.append(action.getExtraHtml());
                }
                if (msg != null) {
                    sb.append(msg);
                } else {
                    sb.append(action.getMessage());
                }
                if ( !json) {
                    sb.append("<p>");
                    sb.append(request.form(URL_STATUS));
                    sb.append(HtmlUtils.submit("Cancel Action", ARG_CANCEL));
                    sb.append(HtmlUtils.hidden(ARG_ACTION_ID, id));
                    sb.append(HtmlUtils.formClose());
                }
            }
        }
        if ( !json) {
            if (action.getEntry() != null) {
                getPageHandler().entrySectionClose(request,
                        action.getEntry(), sb);
            } else {
                getPageHandler().sectionClose(request, sb);
            }
        }
        Result result = makeResult(request, "Status", status, sb, json);
        if ( !json) {
            if (action.entry != null) {
                return getEntryManager().addEntryHeader(request,
                        action.entry, result);
            }
        }

        return result;
    }

    protected ActionInfo getAction(Object id) {
        if (id == null) {
            return null;
        }

        return actions.get(id);
    }

    public boolean getActionOk(Object id) {
        if (id == null) {
            return true;
        }
        ActionInfo action = getAction(id);
        if (action == null) {
            return false;
        }

        return action.getRunning();
    }

    public void setActionMessage(Object id, String msg) {
        ActionInfo action = getAction(id);
        if (action == null) {
            return;
        }
        action.setMessage(msg);
    }

    public void actionComplete(Object id) {
        ActionInfo action = getAction(id);
        if (action == null) {
            return;
        }
        action.setRunning(false);
    }

    public void handleError(Object actionId, Exception exc) {
        ActionInfo action = getAction(actionId);
        if (action == null) {
            return;
        }
        action.setError("An error has occurred:" + exc);
    }

    public void removeAction(Object actionId) {
	if(actionId==null) return;
	actions.remove(actionId);
    }

    public Object addAction(String msg, String continueHtml, Entry entry,Action action) {
        String id = getRepository().getGUID();
        actions.put(id, new ActionInfo(msg, continueHtml, entry,action));
        return id;
    }

    public Result doAction(Request request, final Action runnable,
                           String name, String continueHtml) {
        return doAction(request, runnable, name, continueHtml, null);
    }

    public Result doAction(Request request, final Action runnable,
                           String name, String continueHtml, Entry entry) {
        Object actionId = runAction(runnable, name, continueHtml, entry);

        return new Result(request.makeUrl(URL_STATUS, ARG_ACTION_ID,
                                          "" + actionId));
    }


    public String getCancelUrl(Request request, Object actionId) {
	return request.makeUrl(URL_STATUS, ARG_ACTION_ID,actionId.toString(),ARG_CANCEL,"true");
    }


    public Result doJsonAction(Request request, final Action runnable,
                               String name, String continueHtml,
                               Entry entry) {
        Object actionId = runAction(runnable, name, continueHtml, entry);
        String json = JsonUtil.map(Utils.makeListFromValues("actionid",
                          JsonUtil.quote(actionId.toString())));

        return new Result(json, Result.TYPE_JSON);
    }

    public Object runAction(final Action runnable, String name,
                            String continueHtml) {
        return runAction(runnable, name, continueHtml, null);
    }

    public Object runAction(final Action runnable, String name,
                            String continueHtml, Entry entry) {
        final Object actionId = addAction(name, continueHtml, entry,runnable);
        Misc.run(new Runnable() {
            public void run() {
                try {
                    runnable.run(actionId);
                } catch (Exception exc) {
                    //TODO: handle the error better
                    exc.printStackTrace();
                    handleError(actionId, exc);

                    return;
                }
                actionComplete(actionId);
            }
        });

        return actionId;
    }

    public  static class Action {
        boolean returnJson = false;
	protected Entry entry;
	protected Future future;
        public Action() {}

        public Action(Entry entry) {
	    this.entry = entry;
	}

        public Action(boolean returnJson) {
            this.returnJson = returnJson;
        }

        public  void run(Object actionId) throws Exception {
	}
	public void setRunning(boolean running) {
	    if(!running) {
		if(future!=null) {
		    try {
			future.cancel(true);
			future = null;
		    } catch (Exception exc) {
			System.err.println("ActionManager: error cancelling future " + exc);
		    }
		}
	    }
	}
	public void setFuture(Future future) {
	    this.future = future;
	}

	public String getRedirectUrl() {
	    return null;
	}
    }

    public class ActionInfo {

	private Action action;

        private String id;

        private String name;

        private boolean running = true;

        private String message = "";

        private String continueHtml;

        private String error = null;

        private String extraHtml;

        private Entry entry;

        public ActionInfo(String name, String continueHtml, Entry entry,Action action) {
            this.name         = name;
            this.continueHtml = continueHtml;
            this.id           = getRepository().getGUID();
            this.entry        = entry;
	    this.action = action;
        }

        public ActionInfo(String name, String continueHtml, Entry entry) {
	    this(name, continueHtml, entry,null);
	}

        public void setId(String value) {
            id = value;
        }

        public String getId() {
            return id;
        }

        public void setName(String value) {
            name = value;
        }

        public String getName() {
            return name;
        }

	public String getRedirectUrl() {
	    if(action!=null) return action.getRedirectUrl();
	    return null;
	}

        public void setRunning(boolean value) {
            running = value;
	    if(action!=null) {
		action.setRunning(value);
	    }
        }

        public boolean getRunning() {
            return running;
        }

        public void setMessage(String value) {
            message = value;
        }

        public String getMessage() {
            return message;
        }

        public void setExtraHtml(String value) {
            extraHtml = value;
        }

        public String getExtraHtml() {
            return extraHtml;
        }

        public void setContinueHtml(String value) {
            continueHtml = value;
        }

        public String getContinueHtml() {
            return continueHtml;
        }

        public Entry getEntry() {
            return entry;
        }

        public void setError(String value) {
            error = value;
        }

        public String getError() {
            return error;
        }

    }

}
