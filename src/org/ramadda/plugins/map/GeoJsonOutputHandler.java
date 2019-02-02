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

package org.ramadda.plugins.map;


import org.ramadda.repository.*;
import org.ramadda.repository.output.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.text.CsvUtil;

import org.w3c.dom.*;

import ucar.unidata.gis.shapefile.EsriShapefile;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;

import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 *
 *
 */
public class GeoJsonOutputHandler extends OutputHandler {


    /** Map output type */
    public static final OutputType OUTPUT_GEOJSONTABLE =
        new OutputType("Map Table", "geojsontable", OutputType.TYPE_VIEW, "",
                       ICON_TABLE);

    /** Map output type */
    public static final OutputType OUTPUT_GEOJSONCSV =
        new OutputType("GeoJson CSV", "geojsoncsv", OutputType.TYPE_FILE, "",
                       ICON_CSV);

    /**
     * Create a MapOutputHandler
     *
     *
     * @param repository  the repository
     * @param element     the Element
     * @throws Exception  problem generating handler
     */
    public GeoJsonOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_GEOJSONTABLE);
        addType(OUTPUT_GEOJSONCSV);
    }



    /**
     * Get the entry links
     *
     * @param request  the Request
     * @param state    the repository State
     * @param links    the links
     *
     * @throws Exception  problem creating links
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
        if ((state.getEntry() != null)
                && state.getEntry().getTypeHandler().isType("geo_geojson")) {
            links.add(makeLink(request, state.getEntry(),
                               OUTPUT_GEOJSONTABLE));
            links.add(makeLink(request, state.getEntry(), OUTPUT_GEOJSONCSV));

        }
    }



    /**
     * Output the entry
     *
     * @param request      the Request
     * @param outputType   the type of output
     * @param entry        the Entry to output
     *
     * @return  the Result
     *
     * @throws Exception  problem outputting entry
     */
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream           pw  = new PrintStream(bos);
        Json.toCsv(entry.getResource().getPath(), pw, null);
        pw.close();
        StringBuilder sb = new StringBuilder(new String(bos.toByteArray()));

        if (outputType.equals(OUTPUT_GEOJSONCSV)) {
            Result result = new Result("", sb, "text/csv");
            result.setReturnFilename(IOUtil.stripExtension(entry.getName())
                                     + ".csv");

            return result;
        }

        ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
        String[]              args = new String[] { "-table" };
        CsvUtil csvUtil = new CsvUtil(args, new BufferedOutputStream(bos2),
                                      null);

        csvUtil.setInputStream(
            new ByteArrayInputStream(sb.toString().getBytes()));
        csvUtil.run(null);
        sb = new StringBuilder();
        getPageHandler().entrySectionOpen(request, entry, sb, "Map Table");
        sb.append("\n");
        sb.append(new String(bos2.toByteArray()));
        sb.append("\n");
        getPageHandler().entrySectionClose(request, entry, sb);
        sb.append("\n");

        return new Result("", sb);
    }

}
