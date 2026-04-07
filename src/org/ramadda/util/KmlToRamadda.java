/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;

import org.w3c.dom.*;

import ucar.unidata.util.StringUtil;

import org.ramadda.util.MyXmlUtil;

import java.util.List;

/**
 * Class description
 *
 *
 * @version        $version$, Wed, Aug 31, '11
 * @author         Enter your name here...
 */
public class KmlToRamadda implements org.ramadda.repository.Constants {
    static int counter = 0;
    static int count = 0;

    public static String getId() {
        return "id" + (counter++);
    }

    public static void process(Element element, String parentId,
                               String category)
            throws Exception {
        NodeList elements = MyXmlUtil.getElements(element);
        for (int i = 0; i < elements.getLength(); i++) {
            Element child = (Element) elements.item(i);
            String  tag   = child.getTagName();
            if (tag.equals("Folder")) {
                String id   = getId();
                String name = MyXmlUtil.getGrandChildText(child, "name",
                                  "name");
                /*
                if (parentId != null) {
                    System.out.println(MyXmlUtil.tag("entry",
                            MyXmlUtil.attrs(new String[] {
                        "id", id, "name", name, "parent", parentId, "type",
                        "group"
                    })));
                } else {
                    System.out.println(MyXmlUtil.tag("entry",
                            MyXmlUtil.attrs(new String[] {
                        "id", id, "name", name, "type", "group"
                    })));
                    }*/
                //                process(child, id, name);
                process(child, parentId, name);
            } else if (tag.equals("Placemark")) {
                StringBuffer extra = new StringBuffer();
                Element      point = MyXmlUtil.findChild(child, "Point");
                if (point != null) {
                    String coords = MyXmlUtil.getGrandChildText(point,
                                        "coordinates", null);
                    if (coords != null) {
                        List<String> toks = StringUtil.split(coords, ",",
                                                true, true);
                        extra.append(MyXmlUtil.attrs(new String[] {
                            "north", toks.get(1), "south", toks.get(1),
                            "west", toks.get(0), "east", toks.get(0),
                        }));
                    }
                }

                String id   = getId();
                String name = MyXmlUtil.getGrandChildText(child, "name",
                                  "name");
                String desc = MyXmlUtil.getGrandChildText(child, "description",
                                  "");
                String descNode = MyXmlUtil.tag("wikitext", "",
                                      MyXmlUtil.getCdata(desc));

                String attrs = MyXmlUtil.attrs(new String[] {
                    ATTR_ID, id, ATTR_NAME, name, ATTR_TYPE, "wikipage"
                });
                if (parentId != null) {
                    attrs += MyXmlUtil.attrs(new String[] { ATTR_PARENT,
                            parentId });
                }
                if (category != null) {
                    descNode = descNode
                               + MyXmlUtil.tag("category", "",
                                             MyXmlUtil.getCdata(category));
                }
                //                if(count++>50) return;
                System.out.println(MyXmlUtil.tag(TAG_ENTRY, attrs + extra,
                        descNode));
            } else if (tag.equals("Document")) {
                process(child, parentId, category);
            } else {
                //                System.err.println(tag);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println(MyXmlUtil.XML_HEADER);
        System.out.println("<entries>");
        for (String arg : args) {
            process(MyXmlUtil.getRoot(arg, KmlToRamadda.class), null, null);
        }
        System.out.println("</entries>");
    }

}
