/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository;


import org.w3c.dom.*;

import ucar.unidata.util.TwoFacedObject;


import java.io.InputStream;

import java.util.List;


/**
 */
public abstract class ImportHandler extends RepositoryManager {

    /**
     * _more_
     */
    public ImportHandler() {
        super(null);
    }

    /**
     * _more_
     *
     * @param repository _more_
     */
    public ImportHandler(Repository repository) {
        super(repository);
    }

    /**
     * _more_
     *
     * @param importTypes _more_
     * @param formBuffer _more_
     */
    public void addImportTypes(List<TwoFacedObject> importTypes,
                               Appendable formBuffer) {}


    /**
     * _more_
     *
     * @param request _more_
     * @param repository _more_
     * @param uploadedFile _more_
     * @param parentEntry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleRequest(Request request, Repository repository,
                                String uploadedFile, Entry parentEntry)
            throws Exception {
        return null;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param repository _more_
     * @param url _more_
     * @param parentEntry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleUrlRequest(Request request, Repository repository,
                                   String url, Entry parentEntry)
            throws Exception {
        return null;
    }



    /**
     * _more_
     *
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
    public InputStream getStream(Request request, Entry parent,
                                 String fileName, InputStream stream)
            throws Exception {
        return null;
    }

    /**
     * _more_
     *
     *
     * @param request _more_
     * @param root _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Element getDOM(Request request, Element root) throws Exception {
        return null;
    }

}
