/**
Copyright (c) 2008-2025 Geode Systems LLC
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

    public static final String ARG_SCRIPT_PASSWORD = "script.password";

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

    public JythonTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    public boolean canBeCreatedBy(Request request) {
        return request.getUser().getAdmin();
    }

    @Override
    public Result getHtmlDisplay(Request request, Entry entry)
            throws Exception {
        PythonInterpreter interp = interpPool.get("interp");
        Result            result = getHtmlDisplay(request, entry, interp);
        interpPool.put("interp", interp);

        return result;
    }

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

    protected void processEntry(Request request, PythonInterpreter interp,
                                InputInfo info, ProcessInfo processInfo,
                                Entry theEntry)
            throws Exception {}

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

        public ProcessInfo() {}

        public List<File> files = new ArrayList<File>();

        public List<String> variables = new ArrayList<String>();

    }

    /**
     * Class InputInfo _more_
     *
     *
     * @author RAMADDA Development Team
     */
    public static class InputInfo {

        private static final int TYPE_FILE = 0;

        private static final int TYPE_ENTRY = 1;

        private static final int TYPE_TEXT = 2;

        private static final int TYPE_NUMBER = 3;

        int type;

        public String id;

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

        List<InputInfo> inputs = new ArrayList<InputInfo>();

        JythonTypeHandler typeHandler;

        Entry entry;

        StringBuffer sb;

        int cnt = 0;

        String title;

        String prefix = "";

        Request request;

        String resultHtml;

        String mimeType = "text/html";

        InputStream inputStream;

        String errorMessage;

        String resultFileName = null;

        public FormInfo(JythonTypeHandler typeHandler, Entry entry,
                        Request request, StringBuffer sb) {
            this.sb          = sb;
            this.request     = request;
            this.typeHandler = typeHandler;
            this.entry       = entry;
        }

        public void setErrorMessage(String value) {
            errorMessage = value;
        }

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

        public void append(String s) {
            sb.append(s);
        }

        public void setResult(String html) {
            resultHtml = html;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public void setFormPrefix(String prefix) {
            this.prefix = prefix;
        }

        public void addFormFileUpload(String id, String label) {
            cnt++;
            inputs.add(new InputInfo(InputInfo.TYPE_FILE, id));
            sb.append(
                HtmlUtils.formEntry(
                    typeHandler.msgLabel(label),
                    HtmlUtils.fileInput(
                        id, HtmlUtils.attr(HtmlUtils.ATTR_SIZE, "80"))));
        }

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

        public void addFormLabel(String label) throws Exception {
            sb.append(HtmlUtils.formEntry("", label));
        }

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

        public void addFormSelect(String id, String label, String dflt,
                                  List items) {
            cnt++;
            inputs.add(new InputInfo(InputInfo.TYPE_TEXT, id));
            sb.append(HtmlUtils.formEntry(label,
                                          HtmlUtils.select(id, items, dflt)));
        }

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
