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

package org.ramadda.repository.util;


import ucar.unidata.util.IOUtil;

import java.io.*;

import java.util.zip.*;



/**
 * Holds information for generating entries in entries.xml
 */
public class FileWriter {

    /** _more_ */
    private ZipOutputStream zos;

    /** _more_ */
    private File directory;

    /**
     * _more_
     *
     * @param directory _more_
     */
    public FileWriter(File directory) {
        this.directory = directory;
    }

    /**
     * _more_
     *
     * @param zos _more_
     */
    public FileWriter(ZipOutputStream zos) {
        this.zos = zos;
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void close() throws Exception {
        if (zos != null) {
            IOUtil.close(zos);
        }
    }

    /**
     * _more_
     */
    public void setCompressionOn() {
        if (zos != null) {
            zos.setLevel(0);
        }
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param fis _more_
     *
     * @throws Exception _more_
     */
    public void writeFile(String name, InputStream fis) throws Exception {
        if (zos != null) {
            ZipEntry zipEntry = new ZipEntry(name);
            zos.putNextEntry(zipEntry);
            try {
                IOUtil.writeTo(fis, zos);
                zos.closeEntry();
            } finally {
                IOUtil.close(fis);
                zos.closeEntry();
            }
        } else {
            FileOutputStream fos =
                new FileOutputStream(IOUtil.joinDir(directory, name));
            IOUtil.writeTo(fis, fos);
            IOUtil.close(fos);
        }
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param bytes _more_
     *
     * @throws Exception _more_
     */
    public void writeFile(String name, byte[] bytes) throws Exception {
        if (zos != null) {
            zos.putNextEntry(new ZipEntry(name));
            zos.write(bytes, 0, bytes.length);
            zos.closeEntry();
        } else {
            writeFile(name, new ByteArrayInputStream(bytes));
            //TODO
        }
    }




}
