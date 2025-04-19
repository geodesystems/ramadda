/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.harvester;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;

import org.ramadda.util.sql.SqlUtil;
import org.w3c.dom.*;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.io.File;
import java.lang.reflect.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.regex.*;

public class WebHarvester extends Harvester {
    public static final String TAG_URLS = "urls";
    public static final String ATTR_URLS = "urls";
    private List<String> patternNames = new ArrayList<String>();
    private List<HarvesterEntry> urlEntries = new ArrayList<HarvesterEntry>();
    User user;
    List<String> statusMessages = new ArrayList<String>();
    private int entryCnt = 0;
    private int newEntryCnt = 0;

    public WebHarvester(Repository repository, String id) throws Exception {
        super(repository, id);
    }

    public WebHarvester(Repository repository, Element element)
            throws Exception {
        super(repository, element);
    }

    protected void init(Element element) throws Exception {
        super.init(element);
        List children = XmlUtil.findChildren(element,
                                             HarvesterEntry.TAG_URLENTRY);
        urlEntries = new ArrayList<HarvesterEntry>();
        for (int i = 0; i < children.size(); i++) {
            Element node = (Element) children.get(i);
            urlEntries.add(new HarvesterEntry(node));
        }
    }

    public String getDescription() {
        return "URL";
    }

    protected User getUser() throws Exception {
        if (user == null) {
            user = repository.getUserManager().getDefaultUser();
        }

        return user;
    }

    public void applyState(Element element) throws Exception {
        super.applyState(element);
        for (HarvesterEntry urlEntry : urlEntries) {
            urlEntry.toXml(element);
        }
    }

    public void applyEditForm(Request request) throws Exception {
        super.applyEditForm(request);
        StringBuffer sb  = new StringBuffer();
        int          cnt = 1;
        urlEntries = new ArrayList<HarvesterEntry>();
        HarvesterEntry lastEntry = null;
        while (true) {
            String urlArg = ATTR_URL + cnt;
            if ( !request.exists(urlArg)) {
                break;
            }
            if ( !request.defined(urlArg)) {
                cnt++;
                continue;
            }
            String baseGroupId = request.getUnsafeString(ATTR_BASEGROUP + cnt
                                     + "_hidden", "");

            String groupName = request.getUnsafeString(ATTR_GROUP + cnt, "");
            groupName = groupName.replace(" > ", "/");
            groupName = groupName.replace(">", "/");
	    String url = request.getUnsafeString(urlArg,"");
	    String name = request.getUnsafeString(ATTR_NAME + cnt, "");
	    if(!stringDefined(name)) {
		String path = new URL(url).getPath();
		name = path.substring(path.lastIndexOf('/') + 1);
	    }
            lastEntry = new HarvesterEntry(url, name,
                         request.getUnsafeString(ATTR_DESCRIPTION + cnt, ""),
                         groupName, baseGroupId);
            urlEntries.add(lastEntry);
            cnt++;
        }

    }

    public void addToEditForm(Request request, StringBuffer superSB)
            throws Exception {}

    public void createEditForm(Request request, StringBuffer formSB)
            throws Exception {

        StringBuffer sb      = new StringBuffer();
        StringBuffer superSB = new StringBuffer();

        formSB.append(HU.formTableClose());
        superSB.append(HU.formTable());
        super.createEditForm(request, superSB);
        superSB.append(
            HU.formEntry(
                msgLabel("Entry type"),
                repository.makeTypeSelect(
                    request, false, getTypeHandler().getType(), false,
                    null)));

        superSB.append(HU.formEntry(msgLabel("User"),
                                           HU.input(ATTR_USER,
                                               (getUserName() != null)
                ? getUserName().trim()
                : "", HU.SIZE_30)));

        addToEditForm(request, superSB);

        superSB.append(HU.formTableClose());
        formSB.append(HU.makeShowHideBlock("Basic Information",
                superSB.toString(), true));

        sb.append(HU.hr());
        sb.append("Enter urls and the folders to add them to.");

        int          cnt = 1;

        StringBuffer entrySB;
        for (HarvesterEntry urlEntry : urlEntries) {
            entrySB = new StringBuffer();
            entrySB.append(HU.formTable());
            String link = "";
            if ((urlEntry.url != null) && (urlEntry.url.length() > 0)) {
                link = HU.space(1) + HU.href(
                    urlEntry.url,
                    HU.img(getRepository().getIconUrl(ICON_LINK),"",HU.attrs("width","16px")),
                    HU.attr("target", "_linkpage"));
            }
            String urlInput = HU.input(ATTR_URL + cnt, urlEntry.url,
                                  HU.SIZE_80) + link;

            entrySB.append(HU.formEntry(msgLabel("Fetch URL"),
                    urlInput));
            addEntryToForm(request, entrySB, urlEntry, cnt);
            entrySB.append(HU.formTableClose());
            sb.append(HU.makeShowHideBlock("URL #" + cnt,
                    entrySB.toString(), true));
            sb.append(HU.hr());
            cnt++;
        }

        entrySB = new StringBuffer();
        entrySB.append(HU.formTable());
        entrySB.append(HU.formEntry(msgLabel("Fetch URL"),
                                           HU.input(ATTR_URL + cnt,
                                               "", HU.SIZE_80)));
        /*
        entrySB.append(
            RepositoryManager.tableSubHeader("Then create an entry with"));
        entrySB.append(
            HU.formEntry(
                msgLabel("Name"),
                HU.input(
                    ATTR_NAME + cnt, "",
                    HU.SIZE_80 + HU.title(templateHelp))));
        entrySB.append(
            HU.formEntry(
                msgLabel("Description"),
                HU.input(
                    ATTR_DESCRIPTION + cnt, "",
                    HU.SIZE_80 + HU.title(templateHelp))));
        entrySB.append(
            HU.formEntry(
                msgLabel("Sub-Folder"),
                HU.input(
                    ATTR_GROUP + cnt, "",
                    HU.SIZE_80 + HU.title(templateHelp))));
        */

        entrySB.append(HU.formTableClose());

        sb.append(HU.makeShowHideBlock("New URL", entrySB.toString(),
                true));

        sb.append(HU.p());

        formSB.append(sb);
        formSB.append(HU.formTable());

    }

    public static final String templateHelp =
        "Use macros: ${filename}, ${fromdate}, ${todate}, etc.";

    protected void addEntryToForm(Request request, StringBuffer entrySB,
                                  HarvesterEntry urlEntry, int cnt)
            throws Exception {
        entrySB.append(
            RepositoryManager.tableSubHeader("Then create an entry with"));

        entrySB.append(
            HU.formEntry(
                msgLabel("Name"),
                HU.input(
                    ATTR_NAME + cnt, urlEntry.name,
                    HU.SIZE_80) + HU.space(1) +
		templateHelp));
        entrySB.append(
            HU.formEntry(
                msgLabel("Description"),
                HU.input(
                    ATTR_DESCRIPTION + cnt, urlEntry.description,
                    HU.SIZE_80 + HU.title(templateHelp))));

        addBaseFolderToForm(request, entrySB, urlEntry, cnt);

        String fieldId = ATTR_GROUP + cnt;
        entrySB.append(
            HU.formEntry(
                msgLabel("Sub-Folder Template"),
                HU.input(
                    fieldId, urlEntry.group,
                    HU.SIZE_80 + HU.id(fieldId)
                    + HU.title(templateHelp))));

    }

    protected void addBaseFolderToForm(Request request, StringBuffer entrySB,
                                       HarvesterEntry urlEntry, int cnt)
            throws Exception {
        String baseGroupFieldId = ATTR_BASEGROUP + cnt;
        Entry  baseGroup        = ((urlEntry.baseGroupId.length() == 0)
                                   ? null
                                   : getEntryManager().findGroup(request,
                                       urlEntry.baseGroupId));
        String baseSelect = OutputHandler.getGroupSelect(request,
                                baseGroupFieldId);
        entrySB.append(HU.hidden(baseGroupFieldId + "_hidden",
                                        urlEntry.baseGroupId,
                                        HU.id(baseGroupFieldId
                                            + "_hidden")));
        entrySB.append(
            HU.formEntry(
                msgLabel("Base Folder"),
                HU.disabledInput(baseGroupFieldId, ((baseGroup != null)
                ? baseGroup.getFullName()
                : ""), HU.id(baseGroupFieldId)
                       + HU.SIZE_60) + baseSelect));

    }

    public String getExtraInfo() throws Exception {
        String messages = StringUtil.join("", statusMessages);

        return status.toString() + ((messages.length() == 0)
                                    ? ""
                                    : HU.makeShowHideBlock("Entries",
                                    messages, false));
    }

    protected void runInner(int timestamp) throws Exception {
        if ( !canContinueRunning(timestamp)) {
            return;
        }
        entryCnt       = 0;
        newEntryCnt    = 0;
        statusMessages = new ArrayList<String>();
        status         = new StringBuffer("Fetching URLS<br>");
        int cnt = 0;

        if (getTestMode()) {
            collectEntries((cnt == 0));

            return;
        }

        while (canContinueRunning(timestamp)) {
            long t1 = System.currentTimeMillis();
            collectEntries((cnt == 0));
            long t2 = System.currentTimeMillis();
            cnt++;
            if ( !getMonitor()) {
                status = new StringBuffer("Done<br>");

                break;
            }

            status = new StringBuffer();
            doPause();
            if ( !canContinueRunning(timestamp)) {
                return;
            }
        }
    }

    private void collectEntries(boolean firstTime) throws Exception {
        List<Entry> entries = new ArrayList<Entry>();
        for (HarvesterEntry urlEntry : urlEntries) {
            if ( !getActive()) {
                return;
            }
            if ( !processEntry(urlEntry, entries)) {
                break;
            }
        }
        newEntryCnt += entries.size();
        if (entries.size() > 0) {
            getEntryManager().addNewEntries(getRequest(), entries);
        }
    }

    protected boolean processEntry(HarvesterEntry urlEntry,
                                   List<Entry> entries)
            throws Exception {

        Entry baseGroup = ((urlEntry.baseGroupId.length() == 0)
                           ? null
                           : getEntryManager().findGroup(null,
                               urlEntry.baseGroupId));
        Entry entry = processUrl(urlEntry.url, urlEntry.name,
                                 urlEntry.description, baseGroup,
                                 urlEntry.group);
        if (entry != null) {
            entries.add(entry);
            if (statusMessages.size() > 100) {
                statusMessages = new ArrayList<String>();
            }

            String crumbs = getPageHandler().getBreadCrumbs(getRequest(),
                                entry);
            crumbs = crumbs.replace("class=", "xclass=");
            statusMessages.add(crumbs);
            entryCnt++;
        }

        return true;
    }

    private Entry processUrl(String url, String name, String desc,
                             Entry baseGroup, String groupName)
            throws Exception {

        String fileName = url;
        String tail     = IO.getFileTail(url);
        File   tmpFile  = getStorageManager().getTmpFile(tail);
        //System.err.println ("WebHarvester: " + getName() +" fetching URL: " + url);

        try {
            IOUtil.writeTo(new URL(url), tmpFile, null);
        } catch (Exception exc) {
            statusMessages.add("Unable to fetch URL: " + url);

            return null;
        }

        File newFile = getStorageManager().moveToStorage(null, tmpFile);
        //                           getRepository().getGUID() + "_");
        //        System.err.println ("got it " + newFile);
        String tag        = tagTemplate;

        Date   createDate = new Date();
        Date   fromDate   = createDate;
        Date   toDate     = createDate;
        String ext        = IO.getFileExtension(url);
        tag = tag.replace("${extension}", ext);

        //        groupName = groupName.replace("${dirgroup}", dirGroup);

        groupName = applyMacros(groupName, createDate, fromDate, toDate,
                                fileName);

        Entry group = ((baseGroup != null)
                       ? getEntryManager().findGroupUnder(getRequest(),
                           baseGroup, groupName, getUser())
                       : getEntryManager().findEntryFromName(getRequest(),null,
							     groupName,  true,null,null,null));
        //        System.err.println("Group:" + group.getFullName());
        Entry entry = getTypeHandler().createEntry(repository.getGUID());
        Resource resource = new Resource(newFile.toString(),
                                         Resource.TYPE_STOREDFILE);

        //        System.err.println ("WebHarvester: " + getName() +" adding entry: " + name);
        entry.initEntry(name, desc, group, getUser(), resource, "",
                        Entry.DEFAULT_ORDER, createDate.getTime(),
                        createDate.getTime(), fromDate.getTime(),
                        toDate.getTime(), null);
	entry.setName(getEntryManager().replaceMacros(entry,name));
	entry.setDescription(getEntryManager().replaceMacros(entry,desc));	

        if (tag.length() > 0) {
            List tags = Utils.split(tag, ",", true, true);
            for (int i = 0; i < tags.size(); i++) {
                getMetadataManager().addMetadata(getRequest(), entry,
                        new Metadata(repository.getGUID(), entry.getId(),
                                     getMetadataManager().findType(EnumeratedMetadataHandler.TYPE_TAG),
                                     DFLT_INHERITED, (String) tags.get(i),
                                     Metadata.DFLT_ATTR, Metadata.DFLT_ATTR,
                                     Metadata.DFLT_ATTR,
                                     Metadata.DFLT_EXTRA));
            }
        }
        getTypeHandler().initializeEntryFromHarvester(getRequest(), entry, true);
	getEntryManager().parentageChanged(group,true);
        return entry;
    }

}
