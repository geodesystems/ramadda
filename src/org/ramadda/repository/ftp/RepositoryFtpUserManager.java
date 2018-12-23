/*
* Copyright (c) 2008-2019 Geode Systems LLC
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

package org.ramadda.repository.ftp;


import org.apache.ftpserver.*;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.listener.*;
import org.apache.ftpserver.usermanager.*;
import org.apache.ftpserver.usermanager.impl.*;




import org.ramadda.repository.*;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;




/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */

public class RepositoryFtpUserManager implements org.ramadda.repository
    .Constants, org.apache.ftpserver.ftplet.UserManager {

    /** _more_ */
    FtpManager ftpManager;

    /** _more_ */
    BaseUser user;

    /**
     * _more_
     *
     * @param ftpManager _more_
     */
    public RepositoryFtpUserManager(FtpManager ftpManager) {
        this.ftpManager = ftpManager;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    private Repository getRepository() {
        return ftpManager.getRepository();
    }

    /**
     * _more_
     *
     * @param auth _more_
     *
     * @return _more_
     */
    public org.apache.ftpserver.ftplet.User authenticate(
            Authentication auth) {

        String name     = "anonymous";
        String password = "";

        try {
            if (auth instanceof UsernamePasswordAuthentication) {
                UsernamePasswordAuthentication upa =
                    (UsernamePasswordAuthentication) auth;
                name     = upa.getUsername();
                password = upa.getPassword();
                //                ftpManager.logInfo("name:" + name + " password:" + password);
                if ( !ftpManager.getRepository().getUserManager()
                        .isPasswordValid(name, password)) {
                    ftpManager.getRepository().getLogManager()
                        .logInfoAndPrint("FTP: incorrect password for user:"
                                         + name);

                    return null;
                }
            } else if (auth instanceof AnonymousAuthentication) {
                if (getRepository().getRequireLogin()) {
                    return null;
                }
                name = org.ramadda.repository.auth.UserManager.USER_ANONYMOUS;
                ftpManager.logInfo("Logging in user as anonymous");
            } else {
                return null;
            }

            org.ramadda.repository.auth.User repositoryUser =
                getRepository().getUserManager().findUser(name);
            if (repositoryUser == null) {
                ftpManager.logInfo("Could not find user:" + name);

                return null;
            }

            if (getRepository().getAdminOnly()) {
                if ( !repositoryUser.getAdmin()) {
                    ftpManager.logInfo(
                        "Only site administrators can access this server");

                    return null;
                }
            }

            BaseUser user = new BaseUser();
            user.setName(name);
            String tmpDir =
                getRepository().getStorageManager().getTmpDir().toString()
                + "/dummy";
            user.setHomeDirectory(tmpDir);
            ftpManager.logInfo("Setting dir to: " + tmpDir);
            List<Authority> auths = new ArrayList<Authority>();
            auths.add(new ConcurrentLoginPermission(10, 10));
            user.setAuthorities(auths);

            //      System.err.println(" returning user:"+ user);
            return user;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     * _more_
     *
     * @param username _more_
     */
    public void delete(java.lang.String username) {}

    /**
     * _more_
     *
     * @param username _more_
     *
     * @return _more_
     */
    public boolean doesExist(java.lang.String username) {
        return true;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getAdminName() {
        return "foo";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String[] getAllUserNames() {
        return new String[] { "jeffmc" };
    }


    /**
     * _more_
     *
     * @param username _more_
     *
     * @return _more_
     */
    public org.apache.ftpserver.ftplet.User getUserByName(
            java.lang.String username) {
        return user;
    }

    /**
     * _more_
     *
     * @param username _more_
     *
     * @return _more_
     */
    public boolean isAdmin(java.lang.String username) {
        return false;
    }

    /**
     * _more_
     *
     * @param user _more_
     */
    public void save(org.apache.ftpserver.ftplet.User user) {}

}
