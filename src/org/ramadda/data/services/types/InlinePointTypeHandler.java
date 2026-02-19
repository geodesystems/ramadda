/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.services.types;

import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;
import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.data.services.RecordTypeHandler;
import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;

public class InlinePointTypeHandler extends PointTypeHandler {
    private static String COL_DATA = "data";

    public InlinePointTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }

    @Override
    public RecordFile doMakeRecordFile(Request request, Entry entry, Hashtable properties,  Hashtable requestProperties)
            throws Exception {
        return new InlinePointRecordFile(request,getRepository(), entry);
    }


    public static class InlinePointRecordFile extends CsvFile {
	Request request;
        Repository repository;
        Entry entry;

        public InlinePointRecordFile(Request request, Repository repository, Entry entry)
                throws IOException {
	    this.request = request;
            this.repository = repository;
            this.entry      = entry;
        }

        @Override
        public InputStream doMakeInputStream(boolean buffered)
                throws Exception {
            String data  = (String) entry.getValue(request,COL_DATA);
            byte[] bytes = data.getBytes();
            return new BufferedInputStream(new ByteArrayInputStream(bytes));
        }
    }
}
