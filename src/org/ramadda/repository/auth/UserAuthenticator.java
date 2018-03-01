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


import java.util.List;


/**
 * UserAuthenticator _more_
 *
 *
 * @author RAMADDA Development Team
 */
public interface UserAuthenticator {

    /**
     * this gets called at startup
     *
     * @throws Exception _more_
     */
    public void initUsers() throws Exception;

    /**
     * this gets called when we want to just get a User object from the ID.
     * return null if user is unknown
     *
     * @param repository _more_
     * @param userId _more_
     *
     * @return _more_
     */
    public User findUser(Repository repository, String userId);


    /**
     * this gets called when we want to autheticate the given user/password
     * return null if user/password is unknown or incorrect
     *
     * @param repository _more_
     * @param request _more_
     * @param loginFormExtra _more_
     * @param userId _more_
     * @param password _more_
     *
     * @return _more_
     */
    public User authenticateUser(Repository repository, Request request,
                                 StringBuffer loginFormExtra, String userId,
                                 String password);



    /**
     * This is used to list out the roles for display in the access pages
     *
     * @return _more_
     */
    public List<String> getAllRoles();

    /**
     * this can be used to list out all of the users and display them
     * in RAMADDA
     * It is not used by RAMADDA right now
     *
     * @return _more_
     */
    public List<User> getAllUsers();



    /**
     * This will be used to allow this authenticator to add options
     * to the admin config form
     * Its not used right now
     *
     * @param repository _more_
     * @param sb _more_
     */
    public void addToConfigurationForm(Repository repository,
                                       StringBuffer sb);

    /**
     * This will be used to allow this authenticator to set the options from the config form
     * to the admin config form
     * Its not used right now
     *
     * @param repository _more_
     * @param request _more_
     */
    public void applyConfigurationForm(Repository repository,
                                       Request request);

}
