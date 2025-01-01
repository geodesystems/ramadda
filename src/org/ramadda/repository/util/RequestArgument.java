/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.util;


import org.ramadda.repository.RepositoryBase;
import org.ramadda.util.Utils;

import java.util.ArrayList;
import java.util.List;


/**
 *
 *
 * @author RAMADDA Development Team
 */
public class RequestArgument {


    /** _more_ */
    private List<String> args;

    /**  */
    private String argsProperty;


    /**
     * _more_
     *
     * @param argsProperty _more_
     */
    public RequestArgument(String argsProperty) {
        this.argsProperty = argsProperty;
    }


    /**
     * _more_
     *
     * @param args _more_
     */
    public RequestArgument(String[] args) {
        this.args = new ArrayList<String>();
        for (String arg : args) {
            this.args.add(arg);
        }
    }

    /**
     *
     *
     * @param repository _more_
     * @return _more_
     */
    public List<String> getArgs(RepositoryBase repository) {
        if (args == null) {
            args = Utils.split(repository.getProperty(argsProperty, ""));
        }

        return args;

    }


}
