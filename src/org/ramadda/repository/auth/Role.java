/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.auth;
import org.ramadda.repository.Repository;


import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;


/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public class Role {

    /** role */
    public static final Role ROLE_ANY = new Role("any");

    /** role */
    public static final Role ROLE_NONE = new Role("none");

    /** _more_ */
    public static final Role ROLE_USER = new Role("user");

    /** _more_ */
    public static final Role ROLE_ADMIN = new Role("admin");    

    /** _more_ */
    public static final Role ROLE_ANONYMOUS = new Role("anonymous");

    /** _more_ */
    public static final Role ROLE_GUEST = new Role("guest");

    /** _more_ */
    public static final Role ROLE_INHERIT = new Role("inherit");


    
    /**  */
    boolean negated = false;

    /**  */
    boolean isIp = false;

    /**  */
    String role;

    /**  */
    String baseRole;

    Date date;


    private static final SimpleDateFormat sdf1;
    private static final SimpleDateFormat sdf2;
    private static final SimpleDateFormat sdf3;

    static {
	sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm Z");    
	sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm");    
	sdf3 = new SimpleDateFormat("yyyy-MM-dd");
        sdf1.setTimeZone(Utils.TIMEZONE_DEFAULT);
        sdf2.setTimeZone(Utils.TIMEZONE_DEFAULT);
        sdf3.setTimeZone(Utils.TIMEZONE_DEFAULT);		
    }

    /**
     *
     *
     * @param role _more_
     */
    public Role(String role) {
        this.role = role.trim();
        negated   = role.startsWith("!");
        if (negated) {
            baseRole = role.substring(1);
        } else {
            baseRole = role;
        }
        isIp = baseRole.startsWith("ip:");
        if (isIp) {
            baseRole = baseRole.substring(3);
        }
	if(baseRole.startsWith("date:")) {
	    String dttm  =baseRole.substring("date:".length()).trim();
	    try {
		synchronized(sdf1) {
		    date = sdf1.parse(dttm);
		    //System.err.println("SDF1:" + date);
		}
	    } catch(Exception exc1) {}
	    if(date==null) {
		synchronized(sdf2) {
		    try {
			date = sdf2.parse(dttm);
			//System.err.println("SDF2:" + date);
		    } catch(Exception exc1) {}
		}
	    }

	    if(date==null) {
		synchronized(sdf3) {
		    try {
			date = sdf3.parse(dttm);
			//			System.err.println("SDF3:" + date);
		    } catch(Exception exc1) {}
		}
	    }
	    if(date==null) {
		System.err.println("Error parsing role date:" + baseRole);
	    } 
	}
    }

    public boolean isDate() {

	return date!=null;
    }

    public boolean dateOk() {
	Date now=new Date();
	return now.getTime()>date.getTime();
    }    

    /**
     *
     * @param sroles _more_
     *  @return _more_
     */
    public static List<Role> makeRoles(List<String> sroles) {
        List<Role> roles = new ArrayList<Role>();
        for (String role : sroles) {
            roles.add(new Role(role));
        }

        return roles;
    }


    public boolean isAdmin() {
	return equals(ROLE_ADMIN);
    }


    /**
     *
     * @param role _more_
     *  @return _more_
     */
    public boolean isRole(String role) {
        boolean r = this.baseRole.equals(role) || this.role.equals(role);

        //      System.err.println("\t\tISROLE(String):" + this.role +" " + role +" R:" + r);
        return r;
    }


    /**
     *
     * @param role _more_
     *  @return _more_
     */
    public boolean isRole(Role role) {
        boolean r = this.role.equals(role.role)
                    || this.baseRole.equals(role.baseRole);

        //      System.err.println("\t\tISROLE:" + this.role +" " + role.role +" R:" + r);
        return r;
    }

    /**
     *  @return _more_
     */
    public boolean getNegated() {
        return negated;
    }

    /**
     *  @return _more_
     */
    public boolean getIsIp() {
        return isIp;
    }

    /**
     *  @return _more_
     */
    public String getBaseRole() {
        return baseRole;
    }

    /**
     *  @return _more_
     */
    public boolean isComment() {
        return baseRole.startsWith("#");
    }

    /**
     *  @return _more_
     */
    public String getCssClass() {
        if (isComment()) {
            return "ramadda-role-comment";
        }
        if (Role.ROLE_NONE.isRole(this)) {
            return "ramadda-role-none";
        }
        if (getNegated()) {
            return "ramadda-role-negated";
        }

        return "";
    }

    /**
     *  @return _more_
     */
    public String getRole() {
        return role;
    }

    /**
     *  @return _more_
     */
    @Override
    public String toString() {
        return role;
    }

    /**
     *
     * @param o _more_
     *  @return _more_
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Role) {
            return isRole((Role) o);
        }

        return false;
    }

}
