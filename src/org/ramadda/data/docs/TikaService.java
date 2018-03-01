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

package org.ramadda.data.docs;


import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;

import org.ramadda.repository.*;
import org.ramadda.service.*;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import java.io.*;

import java.util.List;




/**
 *
 * @author Jeff McWhirter/geodesystems.com
 */
public class TikaService extends Service {


    /**
     * ctor
     *
     * @param repository _more_
     * @throws Exception _more_
     */
    public TikaService(Repository repository) throws Exception {
        super(repository, (Element) null);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param service _more_
     * @param input _more_
     * @param args _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean extractText(Request request, Service service,
                               ServiceInput input, List args)
            throws Exception {
        Entry entry = null;
        for (Entry e : input.getEntries()) {
            if (e.isFile()) {
                entry = e;

                break;
            }
        }
        if (entry == null) {
            throw new IllegalArgumentException("No file entry found");
        }

        String name = getStorageManager().getFileTail(entry);
        if ( !Utils.stringDefined(name)) {
            name = entry.getName();
        }
        name = IOUtil.stripExtension(name);

        //        System.out.println("TikaService.extractText:" + entry.getFile());
        Parser parser = new AutoDetectParser();
        //Set the max char length to be 5 meg
        BodyContentHandler handler = new BodyContentHandler(5 * 1000 * 1000);
        Metadata           metadata    = new Metadata();
        FileInputStream    inputstream = new FileInputStream(entry.getFile());
        ParseContext       context     = new ParseContext();
        parser.parse(inputstream, handler, metadata, context);
        String fileContent = handler.toString();

        //getting the list of all meta data elements 
        String[] metadataNames = metadata.names();

        for (String metadataName : metadataNames) {
            //            System.out.println(metadataName + ": " + metadata.get(metadataName));
            entry.putTransientProperty(metadataName,
                                       metadata.get(metadataName));
        }

        /*
        Tika tika = new Tika();
        String fileContent = tika.parseToString(entry.getFile());
        */

        File newFile = new File(IOUtil.joinDir(input.getProcessDir(),
                           name + ".txt"));

        FileOutputStream fileOut = new FileOutputStream(newFile);
        IOUtil.writeFile(newFile, fileContent);
        fileOut.close();

        return true;

    }



}
