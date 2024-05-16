/**
Copyright (c) 2008-2023 Geode Systems LLC
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

import java.io.File;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


/**
 * Class TypeHandler _more_
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class ActionManager extends RepositoryManager {


    /** _more_ */
    public RequestUrl URL_STATUS = new RequestUrl(this, "/status");


    /** _more_ */
    private Hashtable<Object, ActionInfo> actions = new Hashtable<Object,
                                                        ActionInfo>();


    /**
     * _more_
     *
     * @param repository _more_
     */
    public ActionManager(Repository repository) {
        super(repository);
    }



    /**
     * _more_
     *
     * @param actionId _more_
     * @param html _more_
     */
    public void setContinueHtml(Object actionId, String html) {
        if (actionId == null) {
            return;
        }
        ActionInfo action = getAction(actionId);
        if (action != null) {
            action.setContinueHtml(html);
        }
    }


    /**
     *
     * @param request _more_
     * @param title _more_
     * @param status _more_
     * @param sb _more_
     * @param json _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
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

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
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
            sb.append("Action canceled");
            status = "canceled";
            JobManager.getManager().stopLoad(id);
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






    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    protected ActionInfo getAction(Object id) {
        if (id == null) {
            return null;
        }

        return actions.get(id);
    }


    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     * @param id _more_
     * @param msg _more_
     */
    public void setActionMessage(Object id, String msg) {
        ActionInfo action = getAction(id);
        if (action == null) {
            return;
        }
        action.setMessage(msg);
    }


    /**
     * _more_
     *
     * @param id _more_
     */
    public void actionComplete(Object id) {
        ActionInfo action = getAction(id);
        if (action == null) {
            return;
        }
        action.setRunning(false);
    }

    /**
     * _more_
     *
     * @param actionId _more_
     * @param exc _more_
     */
    public void handleError(Object actionId, Exception exc) {
        ActionInfo action = getAction(actionId);
        if (action == null) {
            return;
        }
        action.setError("An error has occurred:" + exc);
    }

    /**
     * _more_
     *
     * @param msg _more_
     * @param continueHtml _more_
     * @param entry _more_
     *
     * @return _more_
     */
    private Object addAction(String msg, String continueHtml, Entry entry) {
        String id = getRepository().getGUID();
        actions.put(id, new ActionInfo(msg, continueHtml, entry));

        return id;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param runnable _more_
     * @param name _more_
     * @param continueHtml _more_
     *
     * @return _more_
     */
    public Result doAction(Request request, final Action runnable,
                           String name, String continueHtml) {
        return doAction(request, runnable, name, continueHtml, null);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param runnable _more_
     * @param name _more_
     * @param continueHtml _more_
     * @param entry _more_
     *
     * @return _more_
     */
    public Result doAction(Request request, final Action runnable,
                           String name, String continueHtml, Entry entry) {
        Object actionId = runAction(runnable, name, continueHtml, entry);

        return new Result(request.makeUrl(URL_STATUS, ARG_ACTION_ID,
                                          "" + actionId));
    }


    /**
     *
     * @param request _more_
     * @param runnable _more_
     * @param name _more_
     * @param continueHtml _more_
     * @param entry _more_
     *  @return _more_
     */
    public Result doJsonAction(Request request, final Action runnable,
                               String name, String continueHtml,
                               Entry entry) {
        Object actionId = runAction(runnable, name, continueHtml, entry);
        String json = JsonUtil.map(Utils.makeListFromValues("actionid",
                          JsonUtil.quote(actionId.toString())));

        return new Result(json, Result.TYPE_JSON);
    }


    /**
     * _more_
     *
     * @param runnable _more_
     * @param name _more_
     * @param continueHtml _more_
     *
     * @return the action id
     */
    public Object runAction(final Action runnable, String name,
                            String continueHtml) {
        return runAction(runnable, name, continueHtml, null);
    }

    /**
     * _more_
     *
     * @param runnable _more_
     * @param name _more_
     * @param continueHtml _more_
     * @param entry _more_
     *
     * @return the action id
     */
    public Object runAction(final Action runnable, String name,
                            String continueHtml, Entry entry) {
        final Object actionId = addAction(name, continueHtml, entry);
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


    /**
     * Action _more_
     *
     *
     * @author RAMADDA Development Team
     * @version $Revision: 1.3 $
     */
    public abstract static class Action {

        /**  */
        boolean returnJson = false;


        /**
         *
         */
        public Action() {}

        /**
         *
         *
         * @param returnJson _more_
         */
        public Action(boolean returnJson) {
            this.returnJson = returnJson;
        }


        /**
         * _more_
         *
         * @param actionId _more_
         *
         * @throws Exception _more_
         */
        public abstract void run(Object actionId) throws Exception;
    }


    /**
     * Class ActionInfo _more_
     *
     *
     * @author RAMADDA Development Team
     * @version $Revision: 1.3 $
     */
    public class ActionInfo {

        /** _more_ */
        private String id;

        /** _more_ */
        private String name;

        /** _more_ */
        private boolean running = true;

        /** _more_ */
        private String message = "";

        /** _more_ */
        private String continueHtml;

        /** _more_ */
        private String error = null;


        /** _more_ */
        private String extraHtml;

        /** _more_ */
        private Entry entry;


        /**
         * _more_
         *
         * @param name _more_
         * @param continueHtml _more_
         * @param entry _more_
         */
        public ActionInfo(String name, String continueHtml, Entry entry) {
            this.name         = name;
            this.continueHtml = continueHtml;
            this.id           = getRepository().getGUID();
            this.entry        = entry;
        }


        /**
         *  Set the Id property.
         *
         *  @param value The new value for Id
         */
        public void setId(String value) {
            id = value;
        }

        /**
         *  Get the Id property.
         *
         *  @return The Id
         */
        public String getId() {
            return id;
        }



        /**
         *  Set the Name property.
         *
         *  @param value The new value for Name
         */
        public void setName(String value) {
            name = value;
        }

        /**
         *  Get the Name property.
         *
         *  @return The Name
         */
        public String getName() {
            return name;
        }

        /**
         *  Set the Running property.
         *
         *  @param value The new value for Running
         */
        public void setRunning(boolean value) {
            running = value;
        }

        /**
         *  Get the Running property.
         *
         *  @return The Running
         */
        public boolean getRunning() {
            return running;
        }



        /**
         * Set the Message property.
         *
         * @param value The new value for Message
         */
        public void setMessage(String value) {
            message = value;
        }

        /**
         * Get the Message property.
         *
         * @return The Message
         */
        public String getMessage() {
            return message;
        }


        /**
         *  Set the ExtraHtml property.
         *
         *  @param value The new value for ExtraHtml
         */
        public void setExtraHtml(String value) {
            extraHtml = value;
        }

        /**
         *  Get the ExtraHtml property.
         *
         *  @return The ExtraHtml
         */
        public String getExtraHtml() {
            return extraHtml;
        }


        /**
         * Set the ContinueHtml property.
         *
         * @param value The new value for ContinueHtml
         */
        public void setContinueHtml(String value) {
            continueHtml = value;
        }

        /**
         * Get the ContinueHtml property.
         *
         * @return The ContinueHtml
         */
        public String getContinueHtml() {
            return continueHtml;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public Entry getEntry() {
            return entry;
        }

        /**
         * Set the Error property.
         *
         * @param value The new value for Error
         */
        public void setError(String value) {
            error = value;
        }

        /**
         * Get the Error property.
         *
         * @return The Error
         */
        public String getError() {
            return error;
        }




    }



}
