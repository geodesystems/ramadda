/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.metadata;


import org.json.*;

import org.ramadda.repository.Repository;
import org.ramadda.util.IO;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public class License implements Comparable {


    private String from;

    /**  */
    private String id;

    /**  */
    private String name;

    /**  */
    private String url;

    /**  */
    private String icon;

    /**  */
    private String text;

    /**  */
    private int priority;

    /**
     *
     *
     * @param id _more_
     */
    public License(String id) {
        this.id   = id;
        this.name = Utils.makeLabel(id);
    }

    /**
     *
     *
     * @param id _more_
     * @param name _more_
     * @param url _more_
     * @param icon _more_
     * @param text _more_
     */
    public License(String id, String name, String url, String icon,
                   String text) {
	this.from="";
        this.id   = id;
        this.name = name;
        this.url  = url;
        this.icon = icon;
        this.text = text;
    }


    /**
     *
     *
     * @param repository _more_
     * @param obj _more_
     * @param priority _more_
     *
     * @throws Exception _more_
     */
    public License(Repository repository, String from, String url, JSONObject obj, int priority)
            throws Exception {
        this.priority = obj.optInt("priority", priority);
	this.from = from;
        id            = obj.getString("id");
        name          = obj.optString("name", Utils.makeLabel(id));
        this.url =url;
        text          = obj.optString("text", null);
        icon          = obj.optString("icon", null);
        if (icon != null) {
            if (icon.startsWith("/")) {
                icon = repository.getUrlBase() + icon;
            }
        }
    }


    /**
     *
     * @param o _more_
     *  @return _more_
     */
    public int compareTo(Object o) {
        boolean ok = o instanceof License;
        if ( !ok) {
            return -1;
        }

        return this.priority - ((License) o).priority;
    }


    /**
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        String values = IO.readContents(args[0], License.class);
        String urls = IO.readContents(args[1], License.class);
        Hashtable<String, String> urlMap = new Hashtable<String, String>();
        for (String line : Utils.split(urls, "\n", true, true)) {
            if (line.startsWith("#")) {
                continue;
            }
            List<String> toks = Utils.splitUpTo(line, ":", 2);
            if (toks.size() != 2) {
                System.err.println("bad line:" + line);
                continue;
            }
            urlMap.put(toks.get(0), toks.get(1));
        }
        List<String> licenses = new ArrayList<String>();
        for (String line : Utils.split(values, "\n", true, true)) {
            if (line.startsWith("#")) {
                continue;
            }
            List<String> toks = Utils.splitUpTo(line, ":", 2);
            if (toks.size() != 2) {
                System.err.println("bad line:" + line);
                continue;
            }
            String id   = toks.get(0);
            String name = toks.get(1);
            String url  = urlMap.get(id);
            String icon = "null";
            if (id.startsWith("CC-")) {
                icon = id.toLowerCase();
                icon = icon.replace("cc-", "").replace("-4.0", "");
                icon = JsonUtil.quote("/licenses/cc/" + icon + ".png");
            }
            if (id.startsWith("localcontexts-")) {
                //localcontexts-tk-a
                icon = id.toLowerCase().replace("localcontexts-",
                        "/licenses/localcontexts/").replaceAll("-",
                            "_") + ".png";
                icon = JsonUtil.quote(icon);
            }


            List<String> attrs = Utils.add(null, "id", JsonUtil.quote(id),
                                           "name", JsonUtil.quote(name),
                                           "icon", icon);
            if (url != null) {
                Utils.add(attrs, "url", JsonUtil.quote(url));
            }
            licenses.add(JsonUtil.map(attrs));
        }
        System.out.println(JsonUtil.map(Utils.makeListFromValues("name",
                JsonUtil.quote("Licenses from..."), "priority", "100",
                "licenses", JsonUtil.list(licenses))));
    }


    /**
       Get the From property.

       @return The From
    **/
    public String getFrom () {
	return from;
    }

    public void setFrom (String s) {
	from =s;
    }
    

    /**
     *  @return _more_
     */
    public String toString() {
        return id + ":" + name;
    }

    /**
     * Set the Id property.
     *
     * @param value The new value for Id
     */
    public void setId(String value) {
        id = value;
    }

    /**
     * Get the Id property.
     *
     * @return The Id
     */
    public String getId() {
        return id;
    }

    /**
     * Set the Name property.
     *
     * @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * Get the Name property.
     *
     * @return The Name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the Url property.
     *
     * @param value The new value for Url
     */
    public void setUrl(String value) {
        url = value;
    }

    /**
     * Get the Url property.
     *
     * @return The Url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Set the Icon property.
     *
     * @param value The new value for Icon
     */
    public void setIcon(String value) {
        icon = value;
    }

    /**
     * Get the Icon property.
     *
     * @return The Icon
     */
    public String getIcon() {
        return icon;
    }

    /**
     * Set the Text property.
     *
     * @param value The new value for Text
     */
    public void setText(String value) {
        text = value;
    }

    /**
     * Get the Text property.
     *
     * @return The Text
     */
    public String getText() {
        return text;
    }

    /**
     *  Set the Priority property.
     *
     *  @param value The new value for Priority
     */
    public void setPriority(int value) {
        priority = value;
    }

    /**
     *  Get the Priority property.
     *
     *  @return The Priority
     */
    public int getPriority() {
        return priority;
    }



}
