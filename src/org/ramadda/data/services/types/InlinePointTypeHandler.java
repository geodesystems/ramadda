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

/**
 */
public class InlinePointTypeHandler extends PointTypeHandler {

    /** _more_ */
    private static int IDX = PointTypeHandler.IDX_LAST + 1;

    /** _more_ */
    private static int IDX_DATA = IDX++;


    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public InlinePointTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public RecordFile doMakeRecordFile(Request request, Entry entry, Hashtable properties,  Hashtable requestProperties)
            throws Exception {
        return new InlinePointRecordFile(request,getRepository(), entry);
    }




    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Dec 8, '18
     * @author         Enter your name here...
     */
    public static class InlinePointRecordFile extends CsvFile {

	Request request;

        /** _more_ */
        Repository repository;

        /** _more_ */
        Entry entry;

        /**
         * _more_
         *
         *
         * @param repository _more_
         * @param entry _more_
         * @param filename _more_
         *
         * @throws IOException _more_
         */
        public InlinePointRecordFile(Request request, Repository repository, Entry entry)
                throws IOException {
	    this.request = request;
            this.repository = repository;
            this.entry      = entry;
        }


        /**
         * _more_
         *
         * @param buffered _more_
         *
         * @return _more_
         *
         *
         * @throws Exception _more_
         */
        @Override
        public InputStream doMakeInputStream(boolean buffered)
                throws Exception {
            String data  = (String) entry.getValue(request,IDX_DATA);
            byte[] bytes = data.getBytes();

            return new BufferedInputStream(new ByteArrayInputStream(bytes));
        }
    }
}
