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

package org.ramadda.plugins.awhere;


import org.json.*;

import org.ramadda.data.record.*;
import org.ramadda.data.services.PointTypeHandler;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.Json;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import java.net.URL;

import java.sql.Statement;

import java.util.ArrayList;
import java.util.List;


/**
 */
public class AwhereFieldTypeHandler extends PointTypeHandler {


    /** _more_ */
    private static int IDX =
        org.ramadda.data.services.RecordTypeHandler.IDX_LAST + 1;


    /** _more_ */
    public static final int IDX_FIELD_ID = IDX++;

    /** _more_ */
    public static final int IDX_ACRES = IDX++;


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public AwhereFieldTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }





    /**
     * This gets called when a user directly creates a field
     *
     * @param request The request
     * @param entry New entry
     * @param parent _more_
     * @param newEntry _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeEntryFromForm(Request request, Entry entry,
                                        Entry parent, boolean newEntry)
            throws Exception {
        super.initializeEntryFromForm(request, entry, parent, newEntry);

        Object[] values  = entry.getTypeHandler().getEntryValues(entry);
        String   fieldId = (String) values[IDX_FIELD_ID];
        if ( !Utils.stringDefined(fieldId)) {
            values[IDX_FIELD_ID] = fieldId = entry.getId();
        }

        //Delete  the old one if this is an edit
        if ( !newEntry) {
            /*
            System.err.println("not new entry  - deleting old one:"
                               + Awhere.doDelete(parent,
                                   new URL(Awhere.URL_FIELDS + "/"
                                           + fieldId)));
            */
        }

        Entry  farmEntry = Awhere.getFarmEntry(entry);
        String farmId    = ((farmEntry == null)
                            ? ""
                            : farmEntry.getValue(
                                AwhereFarmTypeHandler.IDX_FARM_ID, ""));
        if ( !Utils.stringDefined(farmId)) {
            farmId = "farm_id";
        }

        String acres = entry.getValue(IDX_ACRES, null);
        String json  = Json.map(new String[] {
            "id", Json.quote(entry.getId()), "name",
            Json.quote(entry.getName()), "farmId",
            Json.quote(Awhere.getId(farmId)), "acres",
            Utils.stringDefined(acres)
            ? acres
            : "0", "centerPoint",
            Json.map("latitude", "" + entry.getCenter()[0], "longitude",
                     "" + entry.getCenter()[1])
        });

        //For now don't make the field at awhere
        if (true) {
            return;
        }

        System.err.println("creating field:" + json);
        System.err.println("create results: "
                           + Awhere.doPost(entry, Awhere.URL_FIELDS, json));

    }



    /**
     * _more_
     *
     * @param request _more_
     * @param statement _more_
     * @param id _more_
     * @param parentEntry _more_
     * @param values _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void deleteEntry(Request request, Statement statement, String id,
                            Entry parentEntry, Object[] values)
            throws Exception {
        super.deleteEntry(request, statement, id, parentEntry, values);
        if ((values == null) || (parentEntry == null)) {
            return;
        }
        try {
            String fieldId = (String) values[IDX_FIELD_ID];
            if ( !Utils.stringDefined(fieldId)) {
                return;
            }
            System.err.println("delete:"
                               + Awhere.doDelete(parentEntry,
                                   new URL(Awhere.URL_FIELDS + "/"
                                           + fieldId)));
        } catch (Exception exc) {
            getLogManager().logError("Deleting entry:" + id, exc);
        }
    }



}
