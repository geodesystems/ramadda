/*
* Copyright (c) 2008-2018 Geode Systems LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*     http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.ramadda.repository.auth;


import org.ramadda.repository.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;



import java.util.Date;
import java.util.Hashtable;
import java.util.List;


/**
 * Class FilesInfo _more_
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class User {

    /** _more_ */
    public static final String PROP_CAN_CHANGE_PASSWORD =
        "prop.changepassword";

    /** _more_ */
    private String id = "";

    /** _more_ */
    private String name = "";

    /** _more_ */
    private String email = "";

    /** _more_ */
    private String question = "";

    /** _more_ */
    private String answer = "";

    /** _more_ */
    private String description = "";

    /** _more_ */
    private String hashedPassword = "";

    /** _more_ */
    private boolean admin = false;

    /** _more_ */
    private boolean anonymous = false;

    /** _more_ */
    private List<String> roles;

    /** _more_ */
    private List<FavoriteEntry> favorites;


    /** _more_ */
    private String language = "";


    /** _more_ */
    private String template;

    /** _more_ */
    private boolean isLocal = true;

    /** _more_ */
    private boolean isGuest = false;

    /** _more_ */
    private Hashtable properties = new Hashtable();

    /**
     * _more_
     */
    public User() {
        this.anonymous = true;
        this.name      = UserManager.USER_ANONYMOUS;
    }


    /**
     * _more_
     *
     * @param id _more_
     */
    public User(String id) {
        this(id, id);
    }

    /**
     * _more_
     *
     * @param id _more_
     * @param name _more_
     */
    public User(String id, String name) {
        this(id, name, false);
    }

    /**
     * _more_
     *
     * @param id _more_
     * @param admin _more_
     */
    public User(String id, boolean admin) {
        this.id    = id;
        this.admin = admin;
    }


    /**
     * _more_
     *
     * @param id _more_
     * @param name _more_
     * @param admin _more_
     */
    public User(String id, String name, boolean admin) {
        this(id, admin);
        this.name = name;
    }


    /**
     * _more_
     *
     * @param id _more_
     * @param name _more_
     * @param email _more_
     * @param question _more_
     * @param answer _more_
     * @param hashedPassword _more_
     * @param description _more_
     * @param admin _more_
     * @param language _more_
     * @param template _more_
     * @param isGuest _more_
     * @param propertiesBlob _more_
     */
    public User(String id, String name, String email, String question,
                String answer, String hashedPassword, String description,
                boolean admin, String language, String template,
                boolean isGuest, String propertiesBlob) {
        this.id             = id;
        this.name           = name;
        this.email          = email;
        this.question       = question;
        this.answer         = answer;
        this.hashedPassword = hashedPassword;
        this.description    = description;
        this.admin          = admin;
        if (language == null) {
            language = "";
        }
        this.language = language;
        this.template = template;
        this.isGuest  = isGuest;
        if ((propertiesBlob != null) && (propertiesBlob.length() > 0)) {
            try {
                properties =
                    (Hashtable) Repository.decodeObject(propertiesBlob);
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getPropertiesBlob() {
        if (properties != null) {
            return Repository.encodeObject(properties);
        }

        return null;
    }


    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public Object getProperty(String key) {
        return properties.get(key);
    }

    /**
     * _more_
     *
     * @param key _more_
     * @param value _more_
     */
    public void putProperty(String key, Object value) {
        properties.put(key, value);
    }





    /**
     * _more_
     *
     * @return _more_
     */
    public int hashCode() {
        return Misc.hashcode(id) ^ Misc.hashcode(name) ^ (admin
                ? 1
                : 2) ^ (anonymous
                        ? 1
                        : 2);
    }

    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    public boolean equals(Object o) {
        if ( !o.getClass().equals(getClass())) {
            return false;
        }
        User that = (User) o;

        return Misc.equals(this.id, that.id);
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
     * _more_
     *
     * @return _more_
     */
    public String getLabel() {
        if (name.trim().length() == 0) {
            return id;
        }

        return name;

    }


    /**
     * Set the Name property.
     *
     * @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
        if (name == null) {
            name = "";
        }
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
     * Set the Admin property.
     *
     * @param value The new value for Admin
     */
    public void setAdmin(boolean value) {
        admin = value;
    }

    /**
     * Get the Admin property.
     *
     * @return The Admin
     */
    public boolean getAdmin() {
        return admin;
    }

    /**
     * Set the Anonymous property.
     *
     * @param value The new value for Anonymous
     */
    public void setAnonymous(boolean value) {
        anonymous = value;
    }


    /**
     * Get the Anonymous property.
     *
     * @return The Anonymous
     */
    public boolean getAnonymous() {
        if (Misc.equals(UserManager.USER_ANONYMOUS, id)) {
            return true;
        }

        return anonymous;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return "" + id;
    }


    /**
     * Set the Email property.
     *
     * @param value The new value for Email
     */
    public void setEmail(String value) {
        email = value;
        if (email == null) {
            email = "";
        }
    }

    /**
     * Get the Email property.
     *
     * @return The Email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Set the Question property.
     *
     * @param value The new value for Question
     */
    public void setQuestion(String value) {
        question = value;
        if (question == null) {
            question = "";
        }
    }

    /**
     * Get the Question property.
     *
     * @return The Question
     */
    public String getQuestion() {
        return question;
    }

    /**
     * Set the Answer property.
     *
     * @param value The new value for Answer
     */
    public void setAnswer(String value) {
        answer = value;
        if (answer == null) {
            answer = "";
        }
    }

    /**
     * Get the Answer property.
     *
     * @return The Answer
     */
    public String getAnswer() {
        return answer;
    }

    /**
     * Set the Description property.
     *
     * @param value The new value for Description
     */
    public void setDescription(String value) {
        description = value;
    }

    /**
     * Get the Description property.
     *
     * @return The Description
     */
    public String getDescription() {
        if (description == null) {
            description = "";
        }

        return description;
    }





    /**
     * Set the Password property.
     * @deprecated use setHashedPassword
     *
     * @param value The new value for Password
     */
    public void setPassword(String value) {
        setHashedPassword(value);
    }

    /**
     * Get the Password property.
     *
     * @deprecated use getHashedPassword
     * @return The Password
     */
    public String getPassword() {
        return getHashedPassword();
    }


    /**
     * Set the HashedPassword property.
     *
     * @param value The new value for HashedPassword
     */
    public void setHashedPassword(String value) {
        hashedPassword = value;
        if (hashedPassword == null) {
            hashedPassword = "";
        }
    }

    /**
     * Get the HashedPassword property.
     *
     * @return The HashedPassword
     */
    public String getHashedPassword() {
        return hashedPassword;
    }

    /**
     *  Set the Roles property.
     *
     *  @param value The new value for Roles
     */
    public void setRoles(List<String> value) {
        roles = value;
    }

    /**
     *  Get the Roles property.
     *
     *  @return The Roles
     */
    public List<String> getRoles() {
        return roles;
    }

    /**
     * _more_
     *
     * @param role _more_
     *
     * @return _more_
     */
    public boolean isRole(String role) {
        if (role.equals(UserManager.ROLE_ANY)) {
            return true;
        }
        if (role.equals(UserManager.ROLE_USER)) {
            return !getAnonymous();
        }
        if (role.equals(UserManager.ROLE_ANONYMOUS)) {
            return getAnonymous();
        }
        if (role.equals(UserManager.ROLE_GUEST)) {
            return getIsGuest();
        }
        if (role.equals("user:" + getId())) {
            return true;
        }
        if (roles == null) {
            return false;
        }

        return roles.contains(role);
    }

    /**
     * _more_
     *
     * @param delimiter _more_
     *
     * @return _more_
     */
    public String getRolesAsString(String delimiter) {
        if (roles == null) {
            return "";
        }

        return StringUtil.join(delimiter, roles);
    }

    /**
     * Set the Language property.
     *
     * @param value The new value for Language
     */
    public void setLanguage(String value) {
        language = value;
    }

    /**
     * Get the Language property.
     *
     * @return The Language
     */
    public String getLanguage() {
        return language;
    }


    /**
     *  Set the Template property.
     *
     *  @param value The new value for Template
     */
    public void setTemplate(String value) {
        template = value;
    }

    /**
     *  Get the Template property.
     *
     *  @return The Template
     */
    public String getTemplate() {
        if (template == null) {
            return "";
        }

        return template;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean canEditSettings() {
        return !(getAnonymous() || getIsGuest() || !getIsLocal());
        //        return !(getAnonymous() || getIsGuest());
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean canEditFavorites() {
        return !(getAnonymous() || getIsGuest());
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean canChangePassword() {
        return getIsLocal() && canEditSettings() && getCanChangePassword();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean canChangeNameAndEmail() {
        return getIsLocal() && canEditSettings();
    }

    /**
     *  Set the Favorites property.
     *
     *  @param value The new value for Favorites
     */
    public void setUserFavorites(List<FavoriteEntry> value) {
        favorites = value;
    }

    /**
     *  Get the Favorites property.
     *
     *  @return The Favorites
     */
    public List<FavoriteEntry> getFavorites() {
        return favorites;
    }

    /**
     *  Set the IsLocal property.
     *
     *  @param value The new value for IsLocal
     */
    public void setIsLocal(boolean value) {
        this.isLocal = value;
    }

    /**
     *  Get the IsLocal property.This is true if the user is from the ramadda user database.
     *  Else, for example, if it was created by a userauthenticator plugin, then this is false.
     *
     *  @return The IsLocal
     */
    public boolean getIsLocal() {
        return this.isLocal;
    }

    /**
     *  Set the IsGues property.
     *
     *  @param value The new value for IsGues
     */
    public void setIsGuest(boolean value) {
        isGuest = value;
    }

    /**
     *  Get the IsGues property.
     *
     *  @return The IsGues
     */
    public boolean getIsGuest() {
        return isGuest;
    }

    /**
     *  Set the CanChangePassword property.
     *
     *  @param value The new value for CanChangePassword
     */
    public void setCanChangePassword(boolean value) {
        putProperty(PROP_CAN_CHANGE_PASSWORD, "" + value);
    }

    /**
     *  Get the CanChangePassword property.
     *
     *  @return The CanChangePassword
     */
    public boolean getCanChangePassword() {
        String v = (String) getProperty(PROP_CAN_CHANGE_PASSWORD);
        if ((v == null) || Misc.equals(v, "true")) {
            return true;
        }

        return false;
    }

}
