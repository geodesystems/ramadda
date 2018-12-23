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

package org.ramadda.geodata.model;


import org.ramadda.repository.Entry;
import org.ramadda.repository.ImportHandler;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.ramadda.util.Utils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;


import java.io.ByteArrayInputStream;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 */
public class OpendapImporter extends ImportHandler {

    /** _more_ */
    public static final String TYPE_OPENDAP = "OPENDAP";


    /**
     * ctor
     *
     * @param repository _more_
     */
    public OpendapImporter(Repository repository) {
        super(repository);
    }


    /**
     * _more_
     *
     * @param importTypes _more_
     * @param formBuffer _more_
     */
    @Override
    public void addImportTypes(List<TwoFacedObject> importTypes,
                               Appendable formBuffer) {
        super.addImportTypes(importTypes, formBuffer);
        importTypes.add(new TwoFacedObject("Climate Model OPeNDAP Links",
                                           TYPE_OPENDAP));
    }


    /**
     *
     * @param request _more_
     * @param parent _more_
     * @param fileName _more_
     * @param stream _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public InputStream getStream(Request request, Entry parent,
                                 String fileName, InputStream stream)
            throws Exception {
        if ( !request.getString(ARG_IMPORT_TYPE, "").equals(TYPE_OPENDAP)) {
            return null;
        }

        StringBuffer sb = new StringBuffer("<entries>\n");
        String links = new String(
                           IOUtil.readBytes(
                               getStorageManager().getFileInputStream(
                                   fileName)));
        processLinks(request, parent, sb, links);
        sb.append("</entries>");

        System.err.println("entries xml: " + sb);

        return new ByteArrayInputStream(sb.toString().getBytes());

    }



    /**
     * _more_
     *
     *
     * @param request _more_
     * @param parent _more_
     * @param sb _more_
     * @param csv _more_
     *
     * @throws Exception _more_
     */
    private void processLinks(Request request, Entry parent, StringBuffer sb,
                              String csv)
            throws Exception {

        String       entryType    = "climate_modelfile";
        String       filepattern  = ClimateModelFileTypeHandler.FILE_REGEX;
        List<String> patternNames = new ArrayList<String>();
        Pattern      myPattern    = Pattern.compile(filepattern);
        for (String line : StringUtil.split(csv, "\n", true, true)) {
            if (line.startsWith("#")) {
                continue;
            }
            System.err.println("Line:" + line);
            // check for a pattern
            if (line.startsWith(
                    ClimateModelFileTypeHandler.PROP_FILE_PATTERN)) {
                String pattern =
                    line
                    .substring(ClimateModelFileTypeHandler.PROP_FILE_PATTERN
                        .length() + 1);
                if ( !pattern.isEmpty()) {
                    filepattern  = pattern;
                    patternNames = new ArrayList<String>();
                    String userPattern =
                        Utils.extractPatternNames(filepattern, patternNames);
                    myPattern = Pattern.compile(userPattern);
                }

                continue;
            }
            System.out.println("Using file pattern: " + filepattern);

            String       name     = line;
            String       desc     = "";

            StringBuffer innerXml = new StringBuffer();

            //TODO: extract the attributes from the url
            //String  file       = IOUtil.getFileTail(name);
            // if it comes from RAMADDA, strip off the /entry.das
            name = name.replaceAll("/entry.das", "");

            String  file       = IOUtil.getFileTail(name);
            String  model      = "";
            String  experiment = "";
            String  ensemble   = "mean";
            String  variable   = "";
            Matcher matcher    = myPattern.matcher(file);
            if ( !matcher.find()) {
                System.err.println("no match for: " + file);

                // TODO:  should we continue?
                continue;
            }

            for (int dataIdx = 0; dataIdx < patternNames.size(); dataIdx++) {
                String dataName = patternNames.get(dataIdx);
                if ( !Utils.stringDefined(dataName)) {
                    continue;
                }
                String value = (String) matcher.group(dataIdx + 1);
                if (dataName.equals("model")) {
                    model = value;
                } else if (dataName.equals("experiment")) {
                    experiment = value;
                } else if (dataName.equals("ensemble")) {
                    ensemble = value;
                } else if (dataName.equals("variable")) {
                    variable = value;
                }
            }

            innerXml.append(XmlUtil.tag("collection_id", "", parent.getId()));
            innerXml.append(XmlUtil.tag("model", "", model));
            innerXml.append(XmlUtil.tag("experiment", "", experiment));
            innerXml.append(XmlUtil.tag("ensemble", "", ensemble));
            innerXml.append(XmlUtil.tag("variable", "", variable));
            String attrs = XmlUtil.attrs(new String[] {
                ATTR_URL, line, ATTR_TYPE, entryType, ATTR_NAME, name,
                ATTR_DESCRIPTION, desc
            });
            sb.append(XmlUtil.tag("entry", attrs, innerXml.toString()));
        }

    }



}
