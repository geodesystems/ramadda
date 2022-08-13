/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.aws;


import org.ramadda.repository.*;
import org.ramadda.util.S3File;
import org.ramadda.util.Utils;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 */
public class S3ApiHandler extends RepositoryManager implements RequestHandler {
    public S3ApiHandler(Repository repository) throws Exception {
        super(repository);
    }

    public Result processList(Request request) throws Exception {
	return null;
    }

}
