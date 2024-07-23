/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.archive;



import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;


import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;




import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;




import java.io.File;


import java.sql.Statement;


import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class ArchiveMetadataHandler extends MetadataHandler {




    public ArchiveMetadataHandler(Repository repository) throws Exception {
        super(repository);
    }

    public ArchiveMetadataHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }


    @Override
    public String[] getHtml(Request request, Entry entry, Metadata metadata)
            throws Exception {
        if (!metadata.getType().equals("archive_media_resource")) {
	    return super.getHtml(request, entry, metadata);
	}

	String file    = metadata.getAttr1();
	String altUrl = metadata.getAttr2();

	if(!Utils.stringDefined(file) && !Utils.stringDefined(altUrl)) {
	    return null;
	}


	String caption = metadata.getAttr3();



        MetadataType type = getType(metadata.getType());
        if (type == null) {
            return null;
        }


	StringBuilder sb = new StringBuilder();

	if(!Utils.stringDefined(file) && Utils.stringDefined(altUrl)) {
	    sb.append("@(");
	    sb.append(altUrl);
	    sb.append(")");
	    String html = getWikiManager().wikifyEntry(request, entry, sb.toString());
	    return new String[] { "Archive Media",HU.makeShowHideBlock(caption, html,false)};
	}


        String[] nameUrl = type.getFileUrl(request, entry, metadata);
        if (nameUrl == null) {
	    return null;
        }


	String _file = getRepository().getStorageManager().getOriginalFilename(file);
	String label = Utils.stringDefined(caption)?caption:_file;


	if(HU.isAudio(file)) {
	    sb.append(HU.makeShowHideBlock(label,HU.getAudioEmbed(nameUrl[1]),false));
	} else if(HU.isPdf(file)) {
	    //	    sb.append("PDF FILE");
	    sb.append(HU.makeShowHideBlock(label,HU.getPdfEmbed(nameUrl[1],null),false));
	} else if(Utils.isImage(file)) {
	    sb.append(HU.makeShowHideBlock(label,HU.image(nameUrl[1],"width","100%"),false));
	} else {
	    sb.append(HU.href(nameUrl[1],label));
	}
	return new String[]{"Archive Media",sb.toString()};


    }



}
