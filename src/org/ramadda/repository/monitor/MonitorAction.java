/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.monitor;


import org.ramadda.util.HtmlUtils;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;


/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.30 $
 */
public abstract class MonitorAction implements Constants, Cloneable {

    public static final HtmlUtils HU = null;


    /** _more_ */
    public static final String macroTooltip =
        "macros: ${entryid} ${resourcepath} ${resourcename} ${fileextension} ${from_day}  ${from_month} ${from_year} ${from_monthname}  <br>"
        + "${to_day}  ${to_month} ${to_year} ${to_monthname}";



    /** _more_ */
    private String id;


    /** _more_ */
    protected String parentGroupId;

    private String pathTemplate;


    /** _more_ */
    protected Entry group;


    /**
     * _more_
     */
    public MonitorAction() {}


    /**
     * _more_
     *
     * @param id _more_
     */
    public MonitorAction(String id) {
        this.id = id;
    }


    public MonitorAction cloneMe() throws CloneNotSupportedException {
        return (MonitorAction) super.clone();
    }



    public boolean doSearch() {
	return true;
    }

    public boolean isLive(EntryMonitor monitor) {
	return false;
    }    

    public void checkLiveAction(EntryMonitor monitor) throws Throwable {
    }
    
    /**
     * _more_
     *
     * @param repository _more_
     *
     * @return _more_
     */
    public boolean enabled(Repository repository) {
        return true;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean adminOnly() {
        return false;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public abstract String getActionLabel();


    /**
     * _more_
     *
     * @return _more_
     */
    public abstract String getActionName();


    /**
     * _more_
     *
     *
     * @param entryMonitor _more_
     * @return _more_
     */
    public String getSummary(EntryMonitor entryMonitor) {
        return getActionName();
    }

    /**
     * _more_
     *
     * @param prefix _more_
     *
     * @return _more_
     */
    protected String getArgId(String prefix) {
        return prefix + "_" + id;
    }

    public void addStatusLine(Request request, EntryMonitor monitor,Appendable sb) throws Exception {
    }
    public void addButtons(Request request, EntryMonitor monitor,Appendable sb) throws Exception {
    }


    /**
     * _more_
     *
     * @param monitor _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void addToEditForm(Request request, EntryMonitor monitor, Appendable sb)
	throws Exception {}

    /**
     * _more_
     *
     * @param request _more_
     * @param monitor _more_
     */
    public void applyEditForm(Request request, EntryMonitor monitor) {
        pathTemplate= request.getString(getArgId(ARG_PATHTEMPLATE),
					pathTemplate);
    }


    /**
     * _more_
     *
     *
     * @param monitor _more_
     * @param entry _more_
     * @param isNew _more_
     */
    public void entryMatched(EntryMonitor monitor, Entry entry,
                             boolean isNew) {}



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
     *  Set the ParentGroupId property.
     *
     *  @param value The new value for ParentGroupId
     */
    public void setParentGroupId(String value) {
        this.parentGroupId = value;
    }

    /**
     *  Get the ParentGroupId property.
     *
     *  @return The ParentGroupId
     */
    public String getParentGroupId() {
        return this.parentGroupId;
    }

    /**
     * _more_
     *
     * @param entryMonitor _more_
     *
     * @return _more_
     */
    public Entry getGroup(EntryMonitor entryMonitor) {
        try {
            if (group == null) {
                group =
                    (Entry) entryMonitor.getRepository().getEntryManager()
		    .findGroup(entryMonitor.getRepository().getAdminRequest(), parentGroupId);
		//		if(group==null)    System.err.println("MonitorAction.getGroup: null group:" + parentGroupId);
            }
            return group;
        } catch (Exception exc) {
	    entryMonitor.getRepository().getLogManager().logError("Monitor.getGroup:" + parentGroupId,exc);
            return null;
        }
    }

    public void addGroupToEditForm(EntryMonitor monitor, Appendable sb) throws Exception {
	Entry  group      = getGroup(monitor);
	String errorLabel = "";
	if ((group != null) && !monitor.okToAddNew(group)) {
	    errorLabel = HU.span(
					monitor.getRepository().msg(
								    "You cannot add to the folder"), HU.cssClass(
															HU.CLASS_ERRORLABEL));
	}
	String groupName = ((group != null)
			    ? group.getFullName()
			    : "");
	String inputId   = getArgId(ARG_GROUP);
	String select =
	    monitor.getRepository().getHtmlOutputHandler().getSelect(
								     null, inputId,
								     HU.img(
										   monitor.getRepository().getIconUrl(
														      ICON_FOLDER_OPEN)) + HU.space(1)
								     + monitor.getRepository().msg(
												   "Select"), false, "");
	sb.append(HU.hidden(inputId + "_hidden", parentGroupId,
				   HU.id(inputId + "_hidden")));
	sb.append(
		  HU.formEntry(
				      "Folder:",
				      HU.disabledInput(
							      inputId, groupName,
							      HU.SIZE_60 + HU.id(inputId)) + select));


    }


    public void addPathTemplateEditForm(Request request, EntryMonitor monitor, Appendable sb) throws Exception {
        sb.append(HU.formEntry("Path Template:",
				      HU.input(getArgId(ARG_PATHTEMPLATE), pathTemplate,
						      HU.SIZE_60) + " "
				      + "<br>Path under the destination parent to create. " +
				      HU.href(monitor.getRepository().getUrlBase()+"/userguide/monitors.html#pathtemplate","(Help)","target=_help") +
				      "<br>e.g.:<br>" +
				      "<i>Collection/Sub Collection</i><br>" +
				      "Add in a macro for the date<br>"  +
				      "<i>Collection ${create_monthname} ${create_year}</i><br>" +
				      "Or specify a type:<br>" +
				      "<i>Collection ${create_monthname} ${create_year}:type=type_point_collection</i><br>" +
				      "Or specify a nested collection of point data:<br>" +
				      "<i>Collection:type=type_point_collection_collection/${create_monthname} ${create_year}:type=type_point_collection</i>"));
    }
	

    public void applyGroupEditForm(Request request, EntryMonitor monitor) {
        this.parentGroupId = request.getString(getArgId(ARG_GROUP)
					       + "_hidden", "");
	System.err.println("parent group:" + parentGroupId);
        this.group    = null;
    }



    /**
       Set the PathTemplate property.

       @param value The new value for PathTemplate
    **/
    public void setPathTemplate (String value) {
	pathTemplate = value;
    }

    /**
       Get the PathTemplate property.

       @return The PathTemplate
    **/
    public String getPathTemplate () {
	return pathTemplate;
    }



}
