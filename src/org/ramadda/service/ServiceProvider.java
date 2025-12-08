/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/


package org.ramadda.service;


import java.util.List;



/**
 */
public interface ServiceProvider {

    /**
     * Get the DataProcesses that this supports
     *
     * @return the list of DataProcesses
     */
    public List<Service> getServices();
}
