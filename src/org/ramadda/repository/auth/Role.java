/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.auth;

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
    public static final Role ROLE_ANONYMOUS = new Role("anonymous");

    /** _more_ */
    public static final Role ROLE_GUEST = new Role("guest");

    /** _more_ */
    public static final Role ROLE_INHERIT = new Role("inherit");


    boolean negated=false;
    boolean isIp=false;    
    String role;
    String baseRole;
    
    public Role(String role) {
	this.role = role;
	negated= role.startsWith("!");
	if(negated) {
	    baseRole    = role.substring(1);
	} else {
	    baseRole = role;
	}
	isIp = baseRole.startsWith("ip:");
	if(isIp) {
	    baseRole = baseRole.substring(3);
	}
    }

    public static List<Role> makeRoles(List<String> sroles) {
	List<Role> roles = new ArrayList<Role>();
	for(String role: sroles) roles.add(new Role(role));
	return roles;
    }


    public boolean isRole(String role) {
	boolean r = this.baseRole.equals(role) || this.role.equals(role);
	System.err.println("\t\tISROLE(String):" + this.role +" " + role +" R:" + r);
	return r;
    }


    public boolean isRole(Role role) {
	boolean r = this.role.equals(role.role) || this.baseRole.equals(role.baseRole);
	System.err.println("\t\tISROLE:" + this.role +" " + role.role +" R:" + r);
	return r;
    }

    public boolean getNegated() {
	return negated;
    }

    public boolean getIsIp() {
	return isIp;
    }    

    public String getBaseRole() {
	return baseRole;
    }

    public boolean isComment() {
	return baseRole.startsWith("#");
    }

    public String getRole() {
	return role;
    }

    @Override
    public String toString() {
	return role;
    }

    @Override
    public boolean equals(Object o) {
	if(o instanceof Role) {
	    return isRole((Role)o);
	}
	return false;
    }

}
