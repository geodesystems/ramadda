/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.projects.asdi;
import  org.ramadda.repository.metadata.*;


import org.ramadda.repository.*;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.util.HtmlTemplate;
import org.ramadda.util.HtmlUtils;

import ucar.unidata.util.StringUtil;

import org.w3c.dom.*;


import ucar.unidata.util.Misc;


import java.awt.Image;


import java.io.*;

import java.net.URL;
import java.net.URLConnection;



import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class AsdiMetadataHandler extends MetadataHandler {

    /**
     * _more_
     *
     * @param repository _more_
     *
     * @throws Exception _more_
     */
    public AsdiMetadataHandler(Repository repository) throws Exception {
        super(repository);
    }

    private static String[] TAGS= {
	"No Poverty",
	"Zero Hunger",
	"Good Health and Well-being",
	"Quality Education",
	"Gender Equality",
	"Clean Water and Sanitation",
	"Affordable and Clean Energy",
	"Decent Work and Economic Growth",
	"Industry, Innovation and Infrastructure",
	"Reduced Inequalities",
	"Sustainable Cities and Communities",
	"Responsible Consumption and Production",
	"Climate Action",
	"Life Below Water",
	"Life on Land",
	"Peace, Justice and Strong Institutions",
	"Partnerships for the Goals"};

    private static String[] COLORS= {
	"#E5243B",
	"#DDA73A",
	"#4B9F37",
	"#C5182C",
	"#FE3920",
	"#25BDE2",
	"#FCC30B",
	"#A11842",
	"#FD6925",
	"#DD1366",
	"#FD9C22",
	"#BE8A2D",
	"#407D44",
	"#0997D9",
	"#55BF2B",
	"#00689D",
	"#194869"};



    @Override
    public String getTag(Request request, Metadata metadata) {
	String mtd = metadata.getAttr(1);
	String image = null;
	String color = null;
	String text="";
	for(int i=0;i<TAGS.length;i++) {
	    if(mtd.equals(TAGS[i])) {
		color = COLORS[i];
		text = TAGS[i];
		image = getPageHandler().makeHtdocsUrl("/asdi/E-WEB-Goal-" + StringUtil.padLeft(""+(i+1),2,"0"))+".png";
		break;
	    }
	}

	if(image!=null) {
	    return  HU.image(image,HU.attrs("title",text,"class","metadata-tag","metadata-tag",mtd,"metadata-tag-type","image","data-image-url",image));
	} else {
	    String extra = "";
	    if(color!=null) extra = HU.style("background:" + color+";") + HU.attr("data-background",color);
	    String contents=  mtd;
	    return HU.div(mtd,extra + HU.cssClass("metadata-tag")+HU.attr("metadata-tag",contents));
	}
    }

}
