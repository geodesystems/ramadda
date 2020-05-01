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

package org.ramadda.projects.rdx;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.Json;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import java.util.ArrayList;

import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.*;



/**
 * Provides a top-level API
 *
 */
public class RdxApiHandler extends RepositoryManager implements RequestHandler {

    /**
     *     ctor
     *
     *     @param repository the repository
     *
     *     @throws Exception on badness
     */
    public RdxApiHandler(Repository repository) throws Exception {
        super(repository);
    }


    /** _more_          */
    private int passwordErrors = 0;

    /**
     * handle the request
     *
     * @param request request
     *
     * @return result
     *
     * @throws Exception on badness
     */
    public Result processUpdate(Request request) throws Exception {
        StringBuilder sb      = new StringBuilder();
        String        message = "";

        //If too many failed attempts then bail
        if (passwordErrors > 50) {
            message = "Too many failed requests";
            sb.append(Json.map("ok", "false", "message",
                               Json.quote(message)));

            return new Result(sb.toString(), "application/json");
        }

        String auth = getRepository().getProperty("rdx.update.password");
        if (auth == null) {
            message =
                "No rdx.update.password specified in repository properties";
            sb.append(Json.map("ok", "false", "message",
                               Json.quote(message)));

            return new Result(sb.toString(), "application/json");
        }

        String password = request.getString("password", (String) null);
        if (password == null) {
            message = "No password specified in request";
            sb.append(Json.map("ok", "false", "message",
                               Json.quote(message)));

            return new Result(sb.toString(), "application/json");
        }

        if ( !password.equals(auth)) {
            message = "Incorrect password specified in request";
            sb.append(Json.map("ok", "false", "message",
                               Json.quote(message)));
            //Sleep a second to prevent multiple guesses
            Misc.sleepSeconds(1);
            passwordErrors++;

            return new Result(sb.toString(), "application/json");
        }

        //Clear the error count
        passwordErrors = 0;

        String id = request.getString("instrument_id", (String) null);
        if (id == null) {
            message = "No instrument_id specified in request";
            sb.append(Json.map("ok", "false", "message",
                               Json.quote(message)));

            return new Result(sb.toString(), "application/json");
        }




        Request tmpRequest = getRepository().getTmpRequest();
        tmpRequest.put("type", "rdx_instrument");
        tmpRequest.put("search.rdx_instrument.instrument_id", id);
        List[]      result  = getEntryManager().getEntries(tmpRequest);
        List<Entry> entries = new ArrayList<Entry>();
        entries.addAll((List<Entry>) result[0]);
        entries.addAll((List<Entry>) result[1]);
        if (entries.size() == 0) {
            message = "Could not find instrument: " + id;
            sb.append(Json.map("ok", "false", "message",
                               Json.quote(message)));

            return new Result(sb.toString(), "application/json");
        }

        boolean network = request.get("network_status", true);
        int     data    = request.get("data_download", 0);
        for (Entry entry : entries) {
            //TODO: make these IDX-es
            entry.setValue(4, network);
            entry.setValue(5, data);
            System.err.println("rdx update: updated:" + entry.getName());
            message += "updated: " + entry.getName() + "\n";
            getEntryManager().updateEntry(request, entry);
        }

        sb.append(Json.map("ok", "true", "message", Json.quote(message)));

        return new Result(sb.toString(), "application/json");
    }

}
