/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.geo;
import org.ramadda.repository.*;
import org.ramadda.repository.output.*;
import org.ramadda.util.Utils;
import org.ramadda.util.seesv.Seesv;
import org.w3c.dom.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class XpodOutputHandler extends OutputHandler {
    public static final OutputType OUTPUT_XPOD_MERGE =
        new OutputType("Merge XPOD Files", "xpodmerge", -1, "", "/lucide/wind.png");

    public XpodOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_XPOD_MERGE);
    }

    @Override
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> children)
            throws Exception {
	if(children.size()==0) {
	    StringBuilder sb = new StringBuilder();
	    getPageHandler().sectionOpen(request,sb,"XPOD Process",false);
	    sb.append(getPageHandler().showDialogWarning("No entries selected"));
	    getPageHandler().sectionClose(request,sb);
	    return new Result("XPOD Process",sb);
	}

	File dir =getStorageManager().createProcessDir();
	List <File> processedFiles = new ArrayList<File>();
	String header="Date,InputVoltage,Fig2600,Fig2602,Fig3,Fig3_Heat,Fig4,Fig4_Heat,PID,e2vO3,CO_Aux,CO_Main,CO2,temperature,pressure,humidity,QSCO_Aux,QSCO_Main,NO_Aux,NO_Main,NO2_Aux,NO2_Main,O3_Aux,O3_Main,Wind_Spd,Wind_Dir,MQ,PT_PM10ENV,PT_PM25ENV,PT_PM100ENV,PT_PM03um,PT_PM05um,PT_PM10um,PT_PM25um,PT_PM50um,PT_100um,OPC_SampPer,OPC_FlowRate,OPC_PM10,OPC_PM25,OPC_PM10";
	List<String> cmds = new ArrayList<String>();
	Utils.add(cmds,    "-header", header,    "-columns","0-35",  "-decimate","0","250", "-print");
	for(Entry child: children) {
	    if(!child.getTypeHandler().isType("type_xpod_csv")) continue;
	    File file = child.getResource().getTheFile();
	    cmds.add(file.toString());
	}
	File merged  = Seesv.applySeesv(dir,Utils.toStringArray(cmds));
	cmds = new ArrayList<String>();
	Utils.add(cmds,"-sortby","date","up","string","-print",merged.toString());
	File sorted  = Seesv.applySeesv(dir,Utils.toStringArray(cmds));
	cmds = new ArrayList<String>();
	Utils.add(cmds,"-skiplines","1","-print",sorted.toString());
	File finalFile  = Seesv.applySeesv(dir,Utils.toStringArray(cmds));
	request.setReturnFilename("xpod_subset.csv");
	return new Result(new FileInputStream(finalFile),"text/csv");
    }
}
