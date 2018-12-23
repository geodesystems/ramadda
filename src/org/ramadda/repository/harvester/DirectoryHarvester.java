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

package org.ramadda.repository.harvester;


import org.ramadda.repository.*;
import org.ramadda.repository.type.*;

import org.ramadda.sql.SqlUtil;
import org.ramadda.util.HtmlUtils;


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


import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



import java.util.regex.*;


/**
 *
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class DirectoryHarvester extends Harvester {


    /**
     * _more_
     *
     * @param repository _more_
     */
    public DirectoryHarvester(Repository repository) {
        super(repository);
    }

    /**
     * _more_
     *
     * @param repository _more_
     * @param id _more_
     *
     * @throws Exception _more_
     */
    public DirectoryHarvester(Repository repository, String id)
            throws Exception {
        super(repository, id);
    }


    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     *
     * @throws Exception _more_
     */
    public DirectoryHarvester(Repository repository, Element element)
            throws Exception {
        super(repository, element);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getDescription() {
        return "Make folders from directory tree";
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void createEditForm(Request request, StringBuffer sb)
            throws Exception {
        sb.append(HtmlUtils.formEntry(msgLabel("Harvester name"),
                                      HtmlUtils.input(ARG_NAME, getName(),
                                          HtmlUtils.SIZE_40)));
        sb.append(HtmlUtils
            .formEntry(msgLabel("Run"), HtmlUtils
                .checkbox(ATTR_ACTIVEONSTART, "true", getActiveOnStart()) + HtmlUtils
                .space(1) + msg("Active on startup") + HtmlUtils.space(3)
                    + HtmlUtils.checkbox(ATTR_MONITOR, "true", getMonitor())
                        + HtmlUtils.space(1) + msg("Monitor")
                            + HtmlUtils.space(3) + msgLabel("Sleep")
                                + HtmlUtils.space(1)
                                    + HtmlUtils
                                        .input(ATTR_SLEEP, ""
                                            + getSleepMinutes(), HtmlUtils
                                                .SIZE_5) + HtmlUtils.space(1)
                                                    + "(" + msg("minutes")
                                                        + ")"));



        List<File>   rootDirs   = getRootDirs();

        String       extraLabel = "";
        StringBuffer inputText  = new StringBuffer();
        for (File rootDir : rootDirs) {
            String path = rootDir.toString();
            path = path.replace("\\", "/");
            inputText.append(path);
            inputText.append("\n");
            if ( !rootDir.exists()) {
                extraLabel = HtmlUtils.space(2)
                             + HtmlUtils.bold("Directory does not exist");
            }
        }


        sb.append(
            RepositoryManager.tableSubHeader("Walk the directory tree"));
        sb.append(HtmlUtils.formEntry(msgLabel("Under directory"),
                                      HtmlUtils.input(ATTR_ROOTDIR,
                                          inputText,
                                          HtmlUtils.SIZE_60) + extraLabel));
        sb.append(
            RepositoryManager.tableSubHeader("Create new folders under"));

        addBaseGroupSelect(ATTR_BASEGROUP, sb);
    }


    /**
     * _more_
     *
     *
     * @param timestamp _more_
     * @throws Exception _more_
     */
    protected void runInner(int timestamp) throws Exception {
        if ( !canContinueRunning(timestamp)) {
            return;
        }
        Entry baseGroup = getBaseGroup();
        if (baseGroup == null) {
            baseGroup = getEntryManager().getRootEntry();
        }
        for (File rootDir : getRootDirs()) {
            walkTree(rootDir, baseGroup);
        }
    }


    /**
     * _more_
     *
     * @param dir _more_
     * @param parentGroup _more_
     *
     * @throws Exception _more_
     */
    protected void walkTree(File dir, Entry parentGroup) throws Exception {
        String name = dir.getName();
        File xmlFile = new File(IOUtil.joinDir(dir.getParentFile(),
                           "." + name + ".ramadda"));
        Entry fileInfoEntry = getEntryManager().getTemplateEntry(dir);
        Entry group = getEntryManager().findGroupFromName(getRequest(),
                          parentGroup.getFullName() + "/" + name, getUser(),
                          false);
        if (group == null) {
            group = getEntryManager().makeNewGroup(parentGroup, name,
                    getUser(), fileInfoEntry);
        }
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                walkTree(files[i], group);
            }
        }
    }

}
