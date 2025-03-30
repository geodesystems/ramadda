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
 * This is an example of the extended command.
 * Write a class like this and seet the environment variable:
 * export SEESV_CLASSES=org.ramadda.util.seesv.TestCommand1
 *
 * @author Jeff McWhirter
 */
public class TestCommand1 extends ExtCommand {

    int rowIndex = 0;

    String arg;

    List<Row> rows;

    /**
     * _more_
     */
    public TestCommand1() {
	//If this command collects the rows then finishes them we use this to hold the rows
	rows = new ArrayList<Row>();
    }

    /**
     * 
     * @param seesv The seesv
     * @param arg The argument
     *
     * @return Does this command handle the argument
     */
    public boolean canHandle(Seesv seesv, String arg) {
	return arg.equals("-command1");
    }	

    /**
     *
     * @param seesv The seesv
     * @param args array of args
     * @param index current index
     *
     * @return the next index
     */
    public int processArgs(Seesv seesv, List<String> args, int index) {
	//This example command takes 1 argument
	arg = args.get(++index);
        return index;
    }

    /**
     * Process the row
     *
     * @param ctx context
     * @param row row
     *
     * @return the processed row
     *
     * @throws Exception on badness
     */
    public Row processRow(TextReader ctx, Row row) throws Exception {
	//If this command collects the rows then add the row to the rows list
	//and return null:
	//rows.add(row);
	//return null;
	//e.g.: remove the first column and add a new column
	row.remove(0);
	if(rowIndex++==0) row.add(arg);
	else row.add(""+ (rowIndex-1));
        return row;
    }

    /**
     * Finish processing
     *
     * @param ctx The context
     * @throws Exception On badness
     */
    public void finish(TextReader ctx) throws Exception {
	//If this command collects rows then process the rows and then call
	//Processor.finishRows to continue processing the rows
	/*
	List<Row> newRows  = new ArrayList<Row>();
	for(Row row: rows) {
	    //Do something
	    Row newRow = new Row();
	    newRows.add(newRow);
	}	    
	finishRows(ctx,newRows);
	*/
    }

}
