/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.aws;


import org.ramadda.repository.*;
import org.ramadda.util.FileWrapper;
import org.ramadda.util.S3File;
import org.ramadda.util.HtmlUtils;
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
	StringBuilder sb = new StringBuilder();
        String base = getRepository().getUrlBase();
	getPageHandler().sectionOpen(request, sb, "S3 List",true);
        sb.append(HU.formTable());
        sb.append(HU.form(base + "/aws/s3/list"));
	sb.append(HU.formEntry("S3 Bucket:",HU.input("bucket",request.getString("bucket"),HU.SIZE_80)));
	sb.append(HU.formEntry("Level:",HU.select("level",Utils.makeListFromValues("1","2","3","4","5"),
						  request.getString("level","2"))));

        sb.append(HU.formEntry("", HU.submit("List Bucket", "")));
        sb.append(HU.formClose());
        sb.append(HU.formTableClose());

	if(request.defined("bucket")) {
	    String bucket = request.getString("bucket");
	    listBucket(request, sb, bucket);
	}

	getPageHandler().sectionClose(request, sb);
	return new Result("RAMADDA S3",sb);
    }

    private void listBucket(Request request, Appendable sb, String bucket) throws Exception {
	StringBuilder s = new StringBuilder();
	int level = request.get("level",2);
	FileWrapper.walkDirectory(new S3File(bucket), new S3File.MyFileViewer(s,Math.min(level,5)));
	String ss = s.toString().replace(Utils.ANSI_RED,"").replace(Utils.ANSI_RESET,"").replace(Utils.ANSI_GREEN,"");
	sb.append("<pre style='max-height:400px;overflow-y:auto;>");
	sb.append(ss);
	sb.append("</pre>");
    }


}
