/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;

import org.w3c.dom.*;

import ucar.unidata.xml.XmlUtil;

import java.util.HashSet;
import java.util.List;


public class OpenSearchUtil {

    public static final String XMLNS = "http://a9.com/-/spec/opensearch/1.1/";

    public static final String XMLNS_GEO =
        "http://a9.com/-/opensearch/extensions/geo/1.0/";

    public static final String XMLNS_TIME =
        "http://a9.com/-/opensearch/extensions/time/1.0/";

    public static final String MIMETYPE =
        "application/opensearchdescription+xml";

    public static final String TAG_OPENSEARCHDESCRIPTION =
        "OpenSearchDescription";

    public static final String TAG_SHORTNAME = "ShortName";

    public static final String TAG_DESCRIPTION = "Description";

    public static final String TAG_TAGS = "Tags";

    public static final String TAG_CONTACT = "Contact";

    public static final String TAG_URL = "Url";

    public static final String TAG_LONGNAME = "LongName";

    public static final String TAG_IMAGE = "Image";

    public static final String TAG_QUERY = "Query";

    public static final String TAG_DEVELOPER = "Developer";

    public static final String TAG_ATTRIBUTION = "Attribution";

    public static final String TAG_SYNDICATIONRIGHT = "SyndicationRight";

    public static final String TAG_ADULTCONTENT = "AdultContent";

    public static final String TAG_LANGUAGE = "Language";

    public static final String TAG_OUTPUTENCODING = "OutputEncoding";

    public static final String TAG_INPUTENCODING = "InputEncoding";

    public static final String ATTR_XMLNS = "xmlns";

    public static final String ATTR_XMLNS_GEO = "xmlns:geo";

    public static final String ATTR_XMLNS_TIME = "xmlns:time";

    public static final String ATTR_BBOX = "bbox";

    public static final String ATTR_TEMPLATE = "template";

    public static final String ATTR_TYPE = "type";

    public static final String ATTR_HEIGHT = "height";

    public static final String ATTR_WIDTH = "width";

    public static final String ATTR_ROLE = "role";

    public static final String ATTR_SEARCHTERMS = "searchTerms";

    public static final String MACRO_TEXT = "{searchTerms}";

    public static final String MACRO_BBOX = "{geo:box?}";

    public static final String MACRO_TIME_START = "{time:start?}";

    public static final String MACRO_TIME_END = "{time:end?}";

    public static Element getRoot() throws Exception {
        Document doc = XmlUtil.makeDocument();
        Element root = XmlUtil.create(doc, TAG_OPENSEARCHDESCRIPTION, null,
                                      new String[] {
            ATTR_XMLNS, XMLNS, ATTR_XMLNS_GEO, XMLNS_GEO, ATTR_XMLNS_TIME,
            XMLNS_TIME,
        });

        return root;
    }

    /*
<ShortName>Web Search</ShortName>
<Description>Use Example.com to search the Web.</Description>
<Tags>example web</Tags>
<Contact>admin@example.com</Contact>
<SyndicationRight>open</SyndicationRight>
<AdultContent>false</AdultContent>
<Language>en-us</Language>
<OutputEncoding>UTF-8</OutputEncoding>
<InputEncoding>UTF-8</InputEncoding>

    */

    public static void addBasicTags(Element root, String name,
                                    String description, String email)
            throws Exception {
        if (name.length() > 16) {
            name = name.substring(0, 15);
        }
        String[] tags = new String[] {
            TAG_SHORTNAME, name, TAG_DESCRIPTION, description, TAG_CONTACT,
            email, TAG_SYNDICATIONRIGHT, "open", TAG_ADULTCONTENT, "false",
            TAG_LANGUAGE, "en-us", TAG_OUTPUTENCODING, "UTF-8",
            TAG_INPUTENCODING, "UTF-8"
        };
        for (int i = 0; i < tags.length; i += 2) {
            ((Element) XmlUtil.create(tags[i], root)).appendChild(
                XmlUtil.makeCDataNode(
                    root.getOwnerDocument(), tags[i + 1], false));
        }
    }

}
