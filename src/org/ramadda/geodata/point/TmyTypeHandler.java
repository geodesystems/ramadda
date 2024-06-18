/**
   Copyright (c) 2008-2024 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.point;


import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;
import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.data.services.RecordTypeHandler;
import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.seesv.Seesv;

import org.w3c.dom.*;

import ucar.unidata.util.StringUtil;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;


public class TmyTypeHandler extends PointTypeHandler {
    private SimpleDateFormat dateSDF;
    private static int IDX = PointTypeHandler.IDX_LAST + 1;
    private static int IDX_STATE = IDX++;

    public TmyTypeHandler(Repository repository, Element node)
	throws Exception {
        super(repository, node);
    }
    @Override
    public String getContextNamespace() {
        return "tmy";
    }

    @Override
    public RecordFile doMakeRecordFile(Request request, Entry entry,
                                       Hashtable properties,
                                       Hashtable requestProperties)
	throws Exception {
        return new TmyRecordFile(getRepository(), entry,
                                 new IO.Path(entry.getResource().getPath()), this);
    }

    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
	throws Exception {
	if(!isNew(newType)) return;
        initializeRecordEntry(entry, entry.getFile(), true);

        FileInputStream fis =
            new FileInputStream(entry.getResource().getPath());
        BufferedReader br   = new BufferedReader(new InputStreamReader(fis));
        String         line = br.readLine();
        //690150,"TWENTYNINE PALMS",CA,-8.0,34.300,-116.167,626
        List<String> toks = StringUtil.split(line, ",");
        entry.setName(toks.get(1).replaceAll("\"", ""));
        entry.setValue(IDX_STATE, toks.get(2));
        entry.setLocation(Double.parseDouble(toks.get(4)),
                          Double.parseDouble(toks.get(5)));
        fis.close();
    }

    public static class TmyRecordFile extends CsvFile {
        Repository repository;

        Entry entry;

        public TmyRecordFile(Repository repository, Entry entry,
                             IO.Path path, RecordFileContext context)
	    throws IOException {
            super(path, context, null);
            this.repository = repository;
            this.entry      = entry;
        }

    }
}
