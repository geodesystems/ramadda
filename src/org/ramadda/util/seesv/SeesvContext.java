/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.seesv;

import org.ramadda.util.PropertyProvider;

import java.io.File;

import java.util.List;


public interface SeesvContext extends PropertyProvider {
    public List<Class> getClasses();
    public File getTmpFile(String name);

}
