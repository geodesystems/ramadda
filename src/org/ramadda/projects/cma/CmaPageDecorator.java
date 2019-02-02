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

package org.ramadda.projects.cma;


import org.ramadda.repository.*;
import org.ramadda.repository.output.*;

import ucar.unidata.util.Misc;

import java.util.List;


/**
 * @author Jeff McWhirter
 */
public class CmaPageDecorator extends PageDecorator {

    //Make this an object so we don't have linker problems when loading the plugins

    /** _more_ */
    private Object cdmManager;

    /** _more_ */
    private boolean cdmError = false;

    /**
     * ctor
     */
    public CmaPageDecorator() {}


    /**
     * _more_
     *
     * @param repository _more_
     */
    public CmaPageDecorator(Repository repository) {
        super(repository);
    }

    /**
     *  This is called when no ARG_OUTPUT is specified. It can return the OUTPUT_TYPE to use for the given entry
     *
     * @param repository _more_
     * @param request _more_
     * @param html _more_
     * @param entry _more_
     * @param subFolders _more_
     * @param subEntries _more_
     *
     * @return _more_
     */
    /*
    @Override
    public String getDefaultOutputType(Repository repository,
                                       Request request, Entry entry,
                                       List<Entry> subFolders,
                                       List<Entry> subEntries) {
        if (cdmError) {
            return null;
        }

        if (entry.isGroup() && (subEntries != null)) {
            //Look at each child entry
            for (Entry child : subEntries) {
                //If there are any images then use the image player
                if (child.getResource().isImage()) {
                    return ImageOutputHandler.OUTPUT_PLAYER.getId();
                }
            }

            return null;
        }

        if ( !entry.isFile()) {
            return null;
        }

        if ( !entry.getResource().getPath().endsWith(".nc")) {
            return null;
        }

        //Here we use the CdmManager from the cdmdata plugin to determine what kind of entry we have
        try {
            if (cdmManager == null) {
                cdmManager = repository.getRepositoryManager(
                    Misc.findClass("org.ramadda.geodata.cdmdata.CdmManager"));
                if (cdmManager == null) {
                    cdmError = true;
                    System.err.println(
                        "CmaPageDecorator: could  not find CdmManager");

                    return null;
                }
            }
            if ( !((org.ramadda.geodata.cdmdata.CdmManager) cdmManager)
                    .canLoadAsCdm(entry)) {
                return null;
            }
            if (((org.ramadda.geodata.cdmdata.CdmManager) cdmManager)
                    .canLoadAsGrid(entry)) {
                return "data.gridsubset.form";
            }
            if (((org.ramadda.geodata.cdmdata.CdmManager) cdmManager)
                    .canLoadAsPoint(entry)) {
                return "data.point.map";
            }

            return "data.cdl";
        } catch (Exception exc) {
            cdmError = true;
            repository.getLogManager().logError(
                "ERROR: CmaPageDecorator entry=" + entry, exc);
        }

        return null;
    }

    */

    /**
     * Decorate the html. This allows you to do anything with the HTML for the given entry
     *
     * @param repository the repository
     * @param request the request
     * @param html The html page template
     * @param entry This is the last entry the user has seen. Note: this may be null.
     *
     * @return The html
     */
    @Override
    public String decoratePage(Repository repository, Request request,
                               String html, Entry entry) {
        //Do nothing
        return super.decoratePage(repository, request, html, entry);
    }

}
