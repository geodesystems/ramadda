/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.docs;

import org.ramadda.repository.*;

import org.ramadda.service.*;

import org.w3c.dom.*;

import java.util.List;

public class TabularService extends Service {

    public TabularService(Repository repository) throws Exception {
        super(repository, (Element) null);
    }

    public TabularOutputHandler getTabularOutputHandler() throws Exception {
        return (TabularOutputHandler) getRepository().getOutputHandler(
            TabularOutputHandler.class);
    }

    public boolean csv(Request request, Service service, ServiceInput input,
                       List args)
            throws Exception {
        return getTabularOutputHandler().csv(request, service, input, args);
    }

}
