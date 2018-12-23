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

package org.ramadda.geodata.cdmdata;


import org.ramadda.repository.*;
import org.ramadda.repository.admin.*;

import org.ramadda.util.HtmlUtils;

import ucar.unidata.xml.XmlUtil;

import java.io.File;

import java.util.List;


/**
 * And example class to add ldap configuration options to the admin screen
 *
 */
public class LdmAdminHandler extends AdminHandlerImpl {


    /**
     * ctor.
     *
     * @param repository _more_
     */
    public LdmAdminHandler(Repository repository) {
        super(repository);
    }


    /**
     * Used to uniquely identify this admin handler
     *
     * @return unique id for this admin handler
     */
    public String getId() {
        return "ldm";
    }

    /**
     * This adds the fields into the admin Settings->Access form section
     *
     * @param blockId which section
     * @param sb form buffer to append to
     */
    @Override
    public void addToAdminSettingsForm(String blockId, StringBuffer sb) {
        //Are we in the access section
        if ( !blockId.equals(Admin.BLOCK_ACCESS)) {
            return;
        }
        sb.append(
            HtmlUtils.colspan(
                msgHeader("Enable Unidata Local Data Manager (LDM) Access"),
                2));
        String pqinsertPath =
            getRepository().getProperty(LdmAction.PROP_LDM_PQINSERT, "");
        String ldmExtra1 = "";
        if ((pqinsertPath.length() > 0) && !new File(pqinsertPath).exists()) {
            ldmExtra1 =
                HtmlUtils.space(2)
                + HtmlUtils.span("File does not exist!",
                                 HtmlUtils.cssClass(CSS_CLASS_ERROR_LABEL));
        }

        sb.append(
            HtmlUtils.formEntry(
                "Path to pqinsert:",
                HtmlUtils.input(
                    LdmAction.PROP_LDM_PQINSERT, pqinsertPath,
                    HtmlUtils.SIZE_60) + ldmExtra1));
        String ldmQueue =
            getRepository().getProperty(LdmAction.PROP_LDM_QUEUE, "");
        String ldmExtra2 = "";
        if ((ldmQueue.length() > 0) && !new File(ldmQueue).exists()) {
            ldmExtra2 =
                HtmlUtils.space(2)
                + HtmlUtils.span("File does not exist!",
                                 HtmlUtils.cssClass(CSS_CLASS_ERROR_LABEL));
        }

        sb.append(
            HtmlUtils.formEntry(
                "Queue Location:",
                HtmlUtils.input(
                    LdmAction.PROP_LDM_QUEUE, ldmQueue,
                    HtmlUtils.SIZE_60) + ldmExtra2));




    }




    /**
     * apply the form submit
     *
     * @param request the request
     *
     * @throws Exception On badness
     */
    @Override
    public void applyAdminSettingsForm(Request request) throws Exception {
        getRepository().writeGlobal(request, LdmAction.PROP_LDM_PQINSERT,
                                    true);
        getRepository().writeGlobal(request, LdmAction.PROP_LDM_QUEUE, true);
    }


}
