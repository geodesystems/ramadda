/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.type;


import org.apache.commons.net.ftp.*;

import org.python.core.*;
import org.python.util.*;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Pool;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 * Class TypeHandler _more_
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class JythonTypeHandler extends GenericTypeHandler {

    /** _more_ */
    public static final String ARG_SCRIPT_PASSWORD = "script.password";

    /** _more_ */
    private Pool<String, PythonInterpreter> interpPool =
        new Pool<String, PythonInterpreter>(10) {
        protected PythonInterpreter createValue(String path) {
            try {
                getStorageManager().initPython();
                PythonInterpreter interp = new PythonInterpreter();
                for (String f : getRepository().getPythonLibs()) {
                    interp.execfile(IOUtil.getInputStream(f, getClass()), f);
                }
                interp.exec(
                    getRepository().getResource(
                        "/org/ramadda/repository/resources/init.py"));

                return interp;
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

    };


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public JythonTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public boolean canBeCreatedBy(Request request) {
        return request.getUser().getAdmin();
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
    public Result getHtmlDisplay(Request request, Entry entry)
            throws Exception {
        PythonInterpreter interp = interpPool.get("interp");
        Result            result = getHtmlDisplay(request, entry, interp);
        interpPool.put("interp", interp);

        return result;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param interp _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result getHtmlDisplay(Request request, Entry entry,  PythonInterpreter interp)
            throws Exception {


        String       init     =  entry.getStringValue(request, 1,"");
        StringBuffer sb       = new StringBuffer();
        FormInfo     formInfo = new FormInfo(this, entry, request, sb);
        boolean      makeForm = !request.exists(ARG_SUBMIT);


        interp.set("formInfo", formInfo);
        interp.set("request", request);
        interp.set("typeHandler", this);
        interp.set("repository", getRepository());

        interp.set("makeForm", (makeForm
                                ? Integer.valueOf(1)
                                : Integer.valueOf(0)));

        List<Metadata> metadataList =
            getMetadataManager().findMetadata(request, entry,
                ContentMetadataHandler.TYPE_JYTHON, true);
        if (metadataList != null) {
            for (Metadata metadata : metadataList) {
                File jythonLib =
                    new File(
                        IOUtil.joinDir(
                            getRepository().getStorageManager().getEntryDir(
                                metadata.getEntryId(),
                                false), metadata.getAttr1()));
                interp.execfile(new java.io.FileInputStream(jythonLib),
                                jythonLib.toString());

            }
        }




        if ((init != null) && (init.trim().length() > 0)) {
            try {
                interp.exec(init);
            } catch (Exception exc) {
                return new Result(entry.getName(),
                                  new StringBuffer("Error:" + exc));
            }
        }






        String password =  entry.getStringValue(request,0,"");
        if ((password != null) && (password.trim().length() > 0)) {
            if ( !Misc.equals(password.trim(),
                              request.getString(ARG_SCRIPT_PASSWORD,
                                  "").trim())) {
                return new Result((formInfo.title != null)
                                  ? formInfo.title
                                  : entry.getName(), new StringBuffer(
                                      repository.getPageHandler()
                                          .showDialogError("Bad password")));
            }
        }

        if (makeForm) {
            return makeForm(request, entry, interp, formInfo);
        }


        return processForm(request, entry, interp, formInfo);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param interp _more_
     * @param formInfo _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result makeForm(Request request, Entry entry,
                              PythonInterpreter interp, FormInfo formInfo)
            throws Exception {

        String       password = entry.getStringValue(request, 0,"");

        StringBuffer formSB   = new StringBuffer();
        formSB.append(formInfo.prefix);

        String formUrl = getEntryManager().getFullEntryShowUrl(request);
        interp.set("formUrl", formUrl);



        if (formInfo.cnt > 0) {
            if (formInfo.resultFileName != null) {
                formUrl = formUrl + "/" + formInfo.resultFileName;
            }
            formSB.append(HtmlUtils.uploadForm(formUrl, ""));
            formSB.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
            formSB.append(HtmlUtils.formTable());
            if ((password != null) && (password.trim().length() > 0)) {
                formSB.append(HtmlUtils.formEntry(msgLabel("Password"),
                        HtmlUtils.password(ARG_SCRIPT_PASSWORD)));
            }
            formSB.append(formInfo.sb);
            formSB.append(HtmlUtils.formTableClose());
            formSB.append(HtmlUtils.submit("Submit", ARG_SUBMIT));
            formSB.append(HtmlUtils.formClose());
        }
        Result result = new Result((formInfo.title != null)
                                   ? formInfo.title
                                   : entry.getName(), formSB);

        return result;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param interp _more_
     * @param formInfo _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result processForm(Request request, Entry entry,
                                 PythonInterpreter interp, FormInfo formInfo)
            throws Exception {

        ProcessInfo processInfo = doMakeProcessInfo();

        try {
            for (InputInfo info : formInfo.inputs) {
                if (info.type == InputInfo.TYPE_FILE) {
                    String file = request.getUploadedFile(info.id);
                    if ((file != null) && (file.length() > 0)
                            && new File(file).exists()) {
                        processInfo.files.add(new File(file));
                        interp.set(info.id, file);
                    } else {
                        return new Result((formInfo.title != null)
                                          ? formInfo.title
                                          : entry.getName(), new StringBuffer(
                                          repository.getPageHandler()
                                              .showDialogError(
                                                  "No file uploaded")));
                    }
                } else if (info.type == InputInfo.TYPE_ENTRY) {
                    String entryName = request.getString(info.id, "");

                    String entryId = request.getUnsafeString(info.id
                                         + "_hidden", "");
                    Entry theEntry = getEntryManager().getEntry(request,
                                         entryId);
                    if (theEntry == null) {
                        return new Result((formInfo.title != null)
                                          ? formInfo.title
                                          : entry.getName(), new StringBuffer(
                                          repository.getPageHandler()
                                              .showDialogError(
                                                  "No entry selected")));
                    }

                    interp.set(info.id, theEntry);
                    if (theEntry.isFile()) {
                        interp.set(info.id + "_file",
                                   theEntry.getResource().getPath());
                        processInfo.variables.add(info.id + "_file");
                    } else {
                        interp.set(info.id + "_file", null);
                    }
                    processEntry(request, interp, info, processInfo,
                                 theEntry);
                } else if (info.type == InputInfo.TYPE_NUMBER) {
                    interp.set(info.id,
                               Double.parseDouble(request.getString(info.id,
                                   "").trim()));
                } else {
                    interp.set(info.id, request.getString(info.id, ""));
                }
                processInfo.variables.add(info.id);
            }
            try {
                String exec = entry.getStringValue(request,2,"");
                interp.exec(exec);
            } catch (Exception exc) {
                return new Result(entry.getName(),
                                  new StringBuffer("Error:" + exc));
            }
        } finally {
            cleanup(request, entry, interp, processInfo);
        }

        if (formInfo.errorMessage != null) {
            formInfo.resultHtml =
                getPageHandler().showDialogError(formInfo.errorMessage);
        }

        if (formInfo.inputStream != null) {
            return new Result((formInfo.title != null)
                              ? formInfo.title
                              : entry.getName(), formInfo.inputStream,
                              formInfo.mimeType);
        }

        if (formInfo.resultHtml == null) {
            formInfo.resultHtml = "No result provided";
        }
        Result result = new Result((formInfo.title != null)
                                   ? formInfo.title
                                   : entry.getName(), new StringBuffer(
                                       formInfo.resultHtml), formInfo
                                           .mimeType);

        return result;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param interp _more_
     * @param info _more_
     * @param processInfo _more_
     * @param theEntry _more_
     *
     * @throws Exception _more_
     */
    protected void processEntry(Request request, PythonInterpreter interp,
                                InputInfo info, ProcessInfo processInfo,
                                Entry theEntry)
            throws Exception {}

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param interp _more_
     * @param processInfo _more_
     *
     * @throws Exception _more_
     */
    protected void cleanup(Request request, Entry entry,
                           PythonInterpreter interp, ProcessInfo processInfo)
            throws Exception {
        for (File f : processInfo.files) {
            f.delete();
        }
        for (String var : processInfo.variables) {
            interp.set(var, null);
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public ProcessInfo doMakeProcessInfo() {
        return new ProcessInfo();
    }


    /**
     * Class description
     *
     *
     * @version        Enter version here..., Mon, May 3, '10
     * @author         Enter your name here...
     */
    public static class ProcessInfo {

        /**
         * _more_
         */
        public ProcessInfo() {}

        /** _more_ */
        public List<File> files = new ArrayList<File>();

        /** _more_ */
        public List<String> variables = new ArrayList<String>();

    }



    /**
     * Class InputInfo _more_
     *
     *
     * @author RAMADDA Development Team
     */
    public static class InputInfo {

        /** _more_ */
        private static final int TYPE_FILE = 0;

        /** _more_ */
        private static final int TYPE_ENTRY = 1;

        /** _more_ */
        private static final int TYPE_TEXT = 2;

        /** _more_ */
        private static final int TYPE_NUMBER = 3;


        /** _more_ */
        int type;

        /** _more_ */
        public String id;


        /**
         * _more_
         *
         * @param type _more_
         * @param id _more_
         */
        public InputInfo(int type, String id) {
            this.type = type;
            this.id   = id;
        }
    }

    /**
     * Class FormInfo _more_
     *
     *
     * @author RAMADDA Development Team
     */
    public static class FormInfo {

        /** _more_ */
        List<InputInfo> inputs = new ArrayList<InputInfo>();



        /** _more_ */
        JythonTypeHandler typeHandler;

        /** _more_ */
        Entry entry;


        /** _more_ */
        StringBuffer sb;

        /** _more_ */
        int cnt = 0;

        /** _more_ */
        String title;

        /** _more_ */
        String prefix = "";

        /** _more_ */
        Request request;

        /** _more_ */
        String resultHtml;

        /** _more_ */
        String mimeType = "text/html";

        /** _more_ */
        InputStream inputStream;

        /** _more_ */
        String errorMessage;

        /** _more_ */
        String resultFileName = null;


        /**
         * _more_
         *
         * @param typeHandler _more_
         * @param entry _more_
         * @param request _more_
         * @param sb _more_
         */
        public FormInfo(JythonTypeHandler typeHandler, Entry entry,
                        Request request, StringBuffer sb) {
            this.sb          = sb;
            this.request     = request;
            this.typeHandler = typeHandler;
            this.entry       = entry;
        }

        /**
         * _more_
         *
         * @param value _more_
         */
        public void setErrorMessage(String value) {
            errorMessage = value;
        }

        /**
         * _more_
         *
         * @param f _more_
         */
        public void setResultFileName(String f) {
            resultFileName = f;
        }

        /**
         *  Set the MimeType property.
         *
         *  @param value The new value for MimeType
         */
        public void setMimeType(String value) {
            this.mimeType = value;
        }

        /**
         *  Get the MimeType property.
         *
         *  @return The MimeType
         */
        public String getMimeType() {
            return this.mimeType;
        }

        /**
         *  Set the InputStream property.
         *
         *  @param value The new value for InputStream
         * @param mimeType _more_
         */
        public void setInputStream(InputStream value, String mimeType) {
            this.mimeType    = mimeType;
            this.inputStream = value;
        }

        /**
         *  Get the InputStream property.
         *
         *  @return The InputStream
         */
        public InputStream getInputStream() {
            return this.inputStream;
        }


        /**
         * _more_
         *
         * @param s _more_
         */
        public void append(String s) {
            sb.append(s);
        }

        /**
         * _more_
         *
         * @param html _more_
         */
        public void setResult(String html) {
            resultHtml = html;
        }

        /**
         * _more_
         *
         * @param title _more_
         */
        public void setTitle(String title) {
            this.title = title;
        }


        /**
         * _more_
         *
         * @param prefix _more_
         */
        public void setFormPrefix(String prefix) {
            this.prefix = prefix;
        }


        /**
         * _more_
         *
         * @param id _more_
         * @param label _more_
         */
        public void addFormFileUpload(String id, String label) {
            cnt++;
            inputs.add(new InputInfo(InputInfo.TYPE_FILE, id));
            sb.append(
                HtmlUtils.formEntry(
                    typeHandler.msgLabel(label),
                    HtmlUtils.fileInput(
                        id, HtmlUtils.attr(HtmlUtils.ATTR_SIZE, "80"))));
        }


        /**
         * _more_
         *
         * @param id _more_
         * @param label _more_
         *
         * @throws Exception _more_
         */
        public void addFormEntry(String id, String label) throws Exception {
            inputs.add(new InputInfo(InputInfo.TYPE_ENTRY, id));

            sb.append(HtmlUtils.hidden(id + "_hidden", "",
                                       HtmlUtils.id(id + "_hidden")));
            String select = OutputHandler.getSelect(request, id, "Select",
                                true, null, entry);
            sb.append(
                HtmlUtils.formEntry(
                    label,
                    HtmlUtils.disabledInput(
                        id, "",
                        HtmlUtils.id(id) + HtmlUtils.SIZE_60) + select));
            cnt++;
        }


        /**
         * _more_
         *
         * @param label _more_
         *
         * @throws Exception _more_
         */
        public void addFormLabel(String label) throws Exception {
            sb.append(HtmlUtils.formEntry("", label));
        }


        /**
         * _more_
         *
         * @param id _more_
         * @param label _more_
         * @param dflt _more_
         * @param columns _more_
         * @param rows _more_
         */
        public void addFormText(String id, String label, String dflt,
                                int columns, int rows) {
            cnt++;
            inputs.add(new InputInfo(InputInfo.TYPE_TEXT, id));
            if (rows == 1) {
                sb.append(HtmlUtils.formEntry(typeHandler.msgLabel(label),
                        HtmlUtils.input(id, dflt,
                                        HtmlUtils.attr(HtmlUtils.ATTR_SIZE,
                                            "" + columns))));
            } else {
                sb.append(HtmlUtils.formEntryTop(typeHandler.msgLabel(label),
                        HtmlUtils.textArea(id, dflt, rows, columns)));
            }
        }

        /**
         * _more_
         *
         * @param id _more_
         * @param label _more_
         * @param dflt _more_
         * @param items _more_
         */
        public void addFormSelect(String id, String label, String dflt,
                                  List items) {
            cnt++;
            inputs.add(new InputInfo(InputInfo.TYPE_TEXT, id));
            sb.append(HtmlUtils.formEntry(label,
                                          HtmlUtils.select(id, items, dflt)));
        }


        /**
         * _more_
         *
         * @param id _more_
         * @param label _more_
         * @param dflt _more_
         */
        public void addFormNumber(String id, String label, double dflt) {
            inputs.add(new InputInfo(InputInfo.TYPE_NUMBER, id));
            cnt++;
            sb.append(
                HtmlUtils.formEntry(
                    typeHandler.msgLabel(label),
                    HtmlUtils.input(
                        id, "" + dflt,
                        HtmlUtils.attr(HtmlUtils.ATTR_SIZE, "" + 5))));
        }


    }



}
