/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.media;

import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;

import org.ramadda.service.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import java.io.*;

import java.util.List;

/**
 */
public class FfmpegService extends Service {

    public FfmpegService(Repository repository, Element element)
            throws Exception {
        super(repository, element);
    }

    @Override
    public void addExtraArgs(Request request, ServiceInput input,
                             List<String> args, boolean start)
            throws Exception {
        if (start) {
            return;
        }
        File f =
            request.getRepository().getStorageManager().getTmpFile(request,
                "files.txt");

        StringBuilder sb = new StringBuilder();
        for (Entry entry : input.getEntries()) {
            sb.append("file '" + entry.getResource().getPath() + "'\n");
        }
        IOUtil.writeFile(f, sb.toString());
        args.add(f.toString());
        args.add("movie.mp4");

    }

}
