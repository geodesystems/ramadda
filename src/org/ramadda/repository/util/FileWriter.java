/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.util;    

import org.ramadda.util.IO;
import ucar.unidata.util.IOUtil;
import java.io.*;
import java.util.zip.*;

/**
 * Holds information for generating entries in entries.xml
 */
public class FileWriter {
    private ZipOutputStream zos;
    private File directory;

    public FileWriter(File directory) {
        this.directory = directory;
    }

    public FileWriter(ZipOutputStream zos) {
        this.zos = zos;
    }

    public ZipOutputStream getZipOutputStream() {
        return zos;
    }

    public void close() throws Exception {
        if (zos != null) {
            IO.close(zos);
        }
    }

    public void setCompressionOn() {
        if (zos != null) {
            zos.setLevel(0);
        }
    }

    public void writeFile(String name, InputStream fis) throws Exception {
        if (zos != null) {
            ZipEntry zipEntry = new ZipEntry(name);
            zos.putNextEntry(zipEntry);
            try {
                IOUtil.writeTo(fis, zos);
                zos.closeEntry();
            } finally {
                IO.close(fis);
                zos.closeEntry();
            }
        } else {
            FileOutputStream fos =
                new FileOutputStream(IOUtil.joinDir(directory, name));
            IOUtil.writeTo(fis, fos);
            IO.close(fos);
        }
    }

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
