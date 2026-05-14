/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.util;

import org.ramadda.repository.RepositoryBase;
import org.ramadda.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class RequestArgument {
    private List<String> args;

    private String argsProperty;

    public RequestArgument(String argsProperty) {
        this.argsProperty = argsProperty;
    }

    public RequestArgument(String[] args) {
        this.args = new ArrayList<String>();
        for (String arg : args) {
            this.args.add(arg);
        }
    }

    public List<String> getArgs(RepositoryBase repository) {
        if (args == null) {
            args = Utils.split(repository.getProperty(argsProperty, ""));
        }

        return args;

    }

}
