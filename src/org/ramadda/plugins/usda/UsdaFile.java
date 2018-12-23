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

package org.ramadda.plugins.usda;


import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;


import org.ramadda.repository.RepositoryUtil;

import org.ramadda.util.Utils;
import org.ramadda.util.text.*;

import org.w3c.dom.Element;

import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;

import java.util.Date;
import java.util.List;

import java.util.TimeZone;


/**
 */
public abstract class UsdaFile extends CsvFile {

    /** _more_ */
    public byte[] bytes;

    /**
     * ctor
     *
     * @param filename _more_
     *
     * @throws IOException _more_
     */
    public UsdaFile(String filename) throws IOException {
        super(filename);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public abstract Processor.Unfurler doMakeUnfurler();



    /**
     * Gets called when first reading the file. Parses the header
     *
     * @param visitInfo visit info
     *
     * @return the visit info
     *
     *
     * @throws Exception _more_
     */
    @Override
    public VisitInfo prepareToVisit(VisitInfo visitInfo) throws Exception {

        TextReader info = new TextReader();
        info.setDelimiter(",");
        info.setSkip(0);
        info.setInput(doMakeInputStream(true));

        if (bytes == null) {
            ByteArrayOutputStream bos    = new ByteArrayOutputStream();
            PrintWriter           writer = new PrintWriter(bos);
            info.setWriter(writer);
            Processor.Unfurler unfurler = doMakeUnfurler();
            info.getProcessor().addProcessor(unfurler);
            info.getProcessor().addProcessor(new Processor.Sorter(0));
            info.getProcessor().addProcessor(new Processor.Printer(true,
                    false));
            CsvUtil csvUtil = new CsvUtil(new ArrayList<String>());
            csvUtil.process(info);
            List<Row> rows = info.getProcessor().getRows();
            if (rows.size() <= 1) {
                return null;
            }
            info.getWriter().flush();
            bytes = bos.toByteArray();
            //            System.err.println(new String(bytes));
        }


        RecordIO recordIO = new RecordIO(new ByteArrayInputStream(bytes));
        visitInfo.setRecordIO(recordIO);
        putProperty(PROP_HEADER_STANDARD, "true");
        putProperty("picky", "false");
        super.prepareToVisit(visitInfo);

        return visitInfo;
    }

    /**
     * _more_
     */
    @Override
    public void doQuickVisit() {
        //noop

    }


    /**
     * _more_
     *
     * @param buffered _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    @Override
    public InputStream doMakeInputStream(boolean buffered)
            throws IOException {
        String path = getFilename();
        if ( !new File(path).exists()) {
            if (path.indexOf("format=csv") < 0) {
                path += "&format=csv";
            }
        }

        return Utils.doMakeInputStream(path, buffered);
    }




    /**
     * _more_
     *
     * @param record _more_
     * @param field _more_
     * @param tok _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public double parseValue(TextRecord record, RecordField field, String tok)
            throws Exception {
        tok = tok.replaceAll(",", "");
        //TODO: what does D mean?
        if (tok.equals("(D)") || tok.equals("(X)") || tok.equals("(NA)")) {
            return Double.NaN;
        }

        return Double.parseDouble(tok);
    }


}
