/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.seesv;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author Jeff McWhirter
 */
public abstract class ExtCommand extends Processor implements Cloneable,SeesvPlugin {

    /**
     * _more_
     */
    public ExtCommand() {}

    /**
     *
     *
     * @param seesv _more_
     */
    public ExtCommand(Seesv seesv) {
        this.seesv = seesv;
    }

    /**
     *
     * @param seesv _more_
     * @param arg _more_
     *
     * @return _more_
     */
    public abstract boolean canHandle(Seesv seesv, String arg);

    public boolean isSink() {
	return false;
    }

    /**
     *
     * @param seesv _more_
     * @param args _more_
     * @param index _more_
     *
     * @return _more_
     */
    public int processArgs(Seesv seesv, List<String> args, int index) {
        return index;
    }

    /**
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public ExtCommand cloneMe() throws Exception {
        return (ExtCommand) this.clone();
    }

    /**
     * _more_
     *
     * @param ctx _more_
     * @param row _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Row processRow(TextReader ctx, Row row) throws Exception {
        return row;
    }

    /**
     * _more_
     *
     * @param ctx _more_
     * @param rows _more_
     *
     *
     * @return _more_
     * @throws Exception On badness
     */
    public void finish(TextReader ctx) throws Exception {}

}
