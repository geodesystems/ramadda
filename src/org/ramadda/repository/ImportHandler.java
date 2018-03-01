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
