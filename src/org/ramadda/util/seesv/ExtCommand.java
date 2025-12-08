/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.seesv;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


public abstract class ExtCommand extends Processor implements Cloneable,SeesvPlugin {
    public ExtCommand() {}

    public ExtCommand(Seesv seesv) {
        this.seesv = seesv;
    }


    public abstract boolean canHandle(Seesv seesv, String arg);

    public boolean isSink() {return false;}

    public int processArgs(Seesv seesv, List<String> args, int index) {
        return index;
    }

    public ExtCommand cloneMe() throws Exception {
        return (ExtCommand) this.clone();
    }

    public Row processRow(TextReader ctx, Row row) throws Exception {
        return row;
    }

    public void finish(TextReader ctx) throws Exception {}

}
