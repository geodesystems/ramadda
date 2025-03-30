/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.seesv;

import org.ramadda.util.PropertyProvider;

import java.io.File;

import java.util.List;

/**
 */

public interface SeesvContext extends PropertyProvider {

    /**
     *  @return _more_
     */
    public List<Class> getClasses();

    /**
     *
     * @param name _more_
     *
     * @return _more_
     */
    public File getTmpFile(String name);

}
