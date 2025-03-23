/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import org.apache.tika.config.TikaConfig;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.Parser;


import org.apache.tika.sax.BodyContentHandler;

import org.ramadda.util.ProcessRunner;

import ucar.unidata.util.IOUtil;

import java.io.*;

import java.nio.*;
import java.nio.file.*;


import java.util.List;
import java.util.concurrent.*;


/**
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public class TikaUtil {

    /**  */
    public static final int LUCENE_MAX_LENGTH = 25000000;

    private static TikaConfig tikaConfig;
    private static TikaConfig tikaConfigNoImage;    


    /**
     *
     * @param f _more_
      * @return _more_
     */
    public static File getTextCorpusCacheFile(File f) {
        return new File(f.getParentFile(), "." + f.getName() + ".corpus.txt");
    }



    private  static File configFile;

    public static void setConfigFile(File file) {
	configFile = file;
    }

    public static TikaConfig getConfigNoImage() throws Exception {
	if(tikaConfigNoImage == null) {
	    InputStream inputStream = TikaUtil.class.getResourceAsStream("/org/ramadda/util/resources/tika-config-no-image.xml");
	    tikaConfigNoImage = new TikaConfig(inputStream);
	}
	return tikaConfigNoImage;
    }

    public static TikaConfig getConfig() throws Exception {
	if(tikaConfig == null) {
	    InputStream inputStream;
	    if(configFile!=null) {
		//		System.err.println("TikaConfig:" +configFile);
		inputStream  = new FileInputStream(configFile);
	    }   else {
		//		System.err.println("TikaConfig:" +"/org/ramadda/util/resources/tika-config.xml");
		inputStream = TikaUtil.class.getResourceAsStream("/org/ramadda/util/resources/tika-config.xml");
	    }
	    tikaConfig =new TikaConfig(inputStream);
	}
	return tikaConfig;
    }

    /**
     *
     * @param f _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    public static String extractTextCorpus(File f) throws Exception {
        InputStream stream = new FileInputStream(f);
        org.apache.tika.metadata.Metadata metadata =
            new org.apache.tika.metadata.Metadata();
        AutoDetectParser parser = new AutoDetectParser(getConfig());
        BodyContentHandler handler =
            new BodyContentHandler(LUCENE_MAX_LENGTH /*100000000*/);
        parser.parse(stream, handler, metadata,
                     new org.apache.tika.parser.ParseContext());
        String corpus = handler.toString();
        corpus = corpus.replace("\n", " ").replaceAll("\\s\\s+", " ");

        return corpus;
    }

    /**
     *
     * @param f _more_
     * @param force _more_
     *
     * @throws Exception _more_
     */
    public static void writeTextCorpus(File f, boolean force)
            throws Exception {
        File corpusFile = getTextCorpusCacheFile(f);
        if ( !force && corpusFile.exists()) {
            System.err.println("exists:" + corpusFile);
            return;
        }
        long   t1     = System.currentTimeMillis();
        String corpus = extractTextCorpus(f);
        long   t2     = System.currentTimeMillis();
        System.err.println("corpus:" + corpusFile.getName() + " time:"
                           + (t2 - t1));
        IOUtil.writeFile(corpusFile, corpus);
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        final boolean[] force   = { false };
        final String[]  pattern = { null };
        for (int i = 0; i < args.length; i++) {
            String sf = args[i];
            if (sf.equals("-force")) {
                force[0] = true;
                continue;
            }
            if (sf.equals("-pattern")) {
                pattern[0] = args[++i];
                continue;
            }
            File f = new File(sf);
            if (f.isDirectory()) {
                FileWrapper.FileViewer viewer = new FileWrapper.FileViewer() {
                    public int viewFile(int level, FileWrapper f,
                                        FileWrapper[] children)
                            throws Exception {
                        if (f.getName().startsWith(".")) {
                            return DO_CONTINUE;
                        }
                        if ( !f.isDirectory()) {
                            if (pattern[0] != null) {
                                if ( !f.getName().matches(pattern[0])) {
                                    return DO_CONTINUE;
                                }
                            }
                            writeTextCorpus(new File(f.toString()), force[0]);
                        }

                        return DO_CONTINUE;
                    }
                };

                FileWrapper.walkDirectory(f, viewer);
            } else {
                long t1 = System.currentTimeMillis();
                writeTextCorpus(f, force[0]);
                //          System.out.println("contents: " + contents.replace("\n"," ").replaceAll("\\s\\s+"," "));
                long t2 = System.currentTimeMillis();
                System.err.println("file:" + f.getName() + " time:"
                                   + (t2 - t1));
            }
        }
        Utils.exitTest(0);
    }


}
