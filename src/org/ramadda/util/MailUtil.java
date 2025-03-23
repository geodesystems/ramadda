/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;
import java.io.File;

import java.util.List;
import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.*;


/**
 *
 *
 * @author Jeff McWhirter
 */
public class MailUtil {

    /**
     * _more_
     *
     * @param content _more_
     * @param desc _more_
     *
     * @throws Exception _more_
     */
    public static void extractText(Object content, StringBuffer desc)
            throws Exception {
        extractText(content, desc, "");
    }

    /**
     * _more_
     *
     * @param content _more_
     * @param desc _more_
     * @param tab _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static boolean extractText(Object content, StringBuffer desc,
                                      String tab)
            throws Exception {
        //System.err.println (tab + "Extract text");
        tab = tab + "\t";
        if (content instanceof MimeMultipart) {
            //System.err.println (tab + "Is Multipart");
            MimeMultipart multipart = (MimeMultipart) content;
            for (int i = 0; i < multipart.getCount(); i++) {
                MimeBodyPart part = (MimeBodyPart) multipart.getBodyPart(i);
                String       disposition = part.getDisposition();
                String       contentType =
                    part.getContentType().toLowerCase();
                //System.err.println(tab+"part:" + part + " Type:" + contentType);
                Object partContent = part.getContent();
                if (disposition == null) {
                    //System.err.println(tab+"disposition is null");
                    if (partContent instanceof MimeMultipart) {
                        //System.err.println(tab+"part is mulitpart");
                        if (extractText(partContent, desc, tab + "\t")) {
                            //System.err.println(tab+"got text");
                            return true;
                        }
                    } else {
                        //Only ingest the text
                        if (contentType.indexOf("text/plain") >= 0) {
                            desc.append(partContent);
                            desc.append("\n");

                            return true;
                        }
                        //System.err.println(tab+"content type:" + contentType);
                    }

                    continue;
                }
                if (disposition.equalsIgnoreCase(Part.INLINE)
                        && (contentType.indexOf("text/plain") >= 0)) {
                    //System.err.println(tab+"inline text");
                    desc.append(partContent);

                    return true;
                }

                //System.err.println(tab+"disposition:" + disposition + " Type:" + contentType +" part:" + partContent.getClass().getName());
                /*
                if (disposition.equalsIgnoreCase(Part.ATTACHMENT)
                        || disposition.equalsIgnoreCase(Part.INLINE)) {
                    if (part.getFileName() != null) {
                        InputStream inputStream = part.getInputStream();
                    }
                    }*/
            }
        } else if (content instanceof Part) {
            //TODO
            Part part = (Part) content;
            //System.err.println(tab+"Part:" + part);
        } else {
            //System.err.println (tab + "content:" +  content.getClass().getName());
            //            //System.err.println ("xxx content:" +
            //            //System.err.println("Content");
            String contents = content.toString();
            desc.append(contents);
            desc.append("\n");

            return true;
        }

        return false;
    }



}
