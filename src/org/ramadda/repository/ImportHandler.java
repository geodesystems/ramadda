/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository;

import org.w3c.dom.*;

import ucar.unidata.util.TwoFacedObject;

import java.io.InputStream;

import java.util.List;

public abstract class ImportHandler extends RepositoryManager {

    public ImportHandler() {
        super(null);
    }

    public ImportHandler(Repository repository) {
        super(repository);
    }

    public void addImportTypes(List<TwoFacedObject> importTypes,
                               Appendable formBuffer) {}

    public Result handleRequest(Request request, Repository repository,
                                String uploadedFile, Entry parentEntry)
            throws Exception {
        return null;
    }

    public Result handleUrlRequest(Request request, Repository repository,
                                   String url, Entry parentEntry)
            throws Exception {
        return null;
    }

    public InputStream getStream(Request request, Entry parent,
                                 String fileName, InputStream stream)
            throws Exception {
        return null;
    }

    public Element getDOM(Request request, Element root) throws Exception {
        return null;
    }

}
