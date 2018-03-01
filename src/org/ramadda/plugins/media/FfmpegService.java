/*
* Copyright (c) 2008-2018 Geode Systems LLC
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

    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     *
     * @throws Exception _more_
     */
    public FfmpegService(Repository repository, Element element)
            throws Exception {
        super(repository, element);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param input _more_
     * @param args _more_
     * @param start _more_
     *
     * @throws Exception _more_
     */
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
