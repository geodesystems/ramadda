/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.admin;


import org.ramadda.repository.*;

import org.ramadda.repository.auth.*;
import org.ramadda.repository.database.*;

import org.ramadda.repository.harvester.*;

import org.ramadda.repository.output.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import org.w3c.dom.*;



import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;

import java.io.*;


import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;


import javax.mail.*;
import javax.mail.internet.*;


/**
 * Class Admin
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public class MailManager extends RepositoryManager {

    /** _more_ */
    public static final String PROP_SMTP_USER = "ramadda.admin.smtp.user";

    /** _more_ */
    public static final String PROP_SMTP_PASSWORD =
        "ramadda.admin.smtp.password";

    /** _more_ */
    public static final String PROP_SMTP_STARTTLS =
        "ramadda.admin.smtp.starttls";

    /**
     * _more_
     *
     * @param repository _more_
     */
    public MailManager(Repository repository) {
        super(repository);

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void addAdminSettings(Request request, StringBuffer sb)
            throws Exception {
        sb.append(HU.row(HU.colspan(msgHeader("Email"), 2)));
	HU.formEntry(sb,msg("For sending password reset messages, etc. Define the SMTP user and password in a RAMADDA .properties file on your server:")+HU.pre("ramadda.admin.smtp.user=\nramadda.admin.smtp.password="));
        sb.append(
            HU.formEntry(
                msgLabel("Administrator Email"),
                HU.input(
                    PROP_ADMIN_EMAIL,
                    formPropValue(request, PROP_ADMIN_EMAIL, ""),
                    HU.SIZE_40)));
        sb.append(
            HU.formEntry(
                msgLabel("Mail Server"), HU.input(
                    PROP_ADMIN_SMTP,
		    formPropValue(request, PROP_ADMIN_SMTP, ""), HU.SIZE_40+HU.attr("placeholder","smtp.example.com")
)));

	HU.formEntry(sb,msgLabel("Test Message"),
		     HU.b("To: ")+
		     HU.input("mailtest_to",request.getString("mailtest_to","")) +
		     HU.space(1) +
		     HU.b("Subject: ")+
		     HU.input("mailtest_subject",
			      request.getString("mailtest_subject","This is a test")) +
		     HU.space(1) +
		     HU.b("Message: ")+
		     HU.input("mailtest_message",
			      request.getString("mailtest_message","Test message")));
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void applyAdminSettings(Request request) throws Exception {
        getRepository().writeGlobal(request, PROP_ADMIN_SMTP, true);
        getRepository().writeGlobal(request, PROP_ADMIN_EMAIL, true);
	if(!isEmailEnabled()) {
	    getSessionManager().addSessionMessage(request,"Email is not enabled");
	    return;
	}

        String smtpUser = getSmtpUser();
        String smtpPassword = getSmtpPassword();
	if(!stringDefined(smtpUser)) {
	    getSessionManager().addSessionMessage(request,"No ramadda.admin.smtp.user property defined");
	}
	if(!stringDefined(smtpPassword)) {
	    getSessionManager().addSessionMessage(request,"No ramadda.admin.smtp.password property defined");
	}	



	if(request.defined("mailtest_to")) {
	    String to = request.getString("mailtest_to","").trim();
	    String subject = request.getString("mailtest_subject","");
	    String msg = request.getString("mailtest_message","");	    	    
	    try {
		sendEmail(to, subject, msg,false);
		getSessionManager().addSessionMessage(request,"Test email sent");
	    } catch(Exception exc) {
		getSessionManager().addSessionMessage(request,"Error sending test email: " + exc.getMessage());
	    }
	}

    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getSmtpServer() {
        return getPropertyFromTree(PROP_ADMIN_SMTP, "");
    }

    public String getSmtpUser() {
        return getPropertyFromTree(PROP_SMTP_USER, (String) null);
    }

    public String getSmtpPassword() {
        return  getPropertyFromTree(PROP_SMTP_PASSWORD,
				    (String) null);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getAdminEmail() {
        return getPropertyFromTree(PROP_ADMIN_EMAIL, "");
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isEmailEnabled() {
        if (getRepository().getParentRepository() != null) {
            return getRepository().getParentRepository().getMailManager()
                .isEmailEnabled();
        }

        String smtpServer  = getSmtpServer();
        String serverAdmin = getAdminEmail();
        if ((serverAdmin.length() == 0) || (smtpServer.length() == 0)) {
            return false;
        }

        return true;
    }

    /**
     * _more_
     *
     * @param to _more_
     * @param subject _more_
     * @param contents _more_
     * @param asHtml _more_
     *
     * @throws Exception _more_
     */
    public void sendEmail(String to, String subject, String contents,
                          boolean asHtml)
	throws Exception {
        sendEmail(to, getAdminEmail(), subject, contents, asHtml);
    }

    public int sendEmail(List<String> to, String subject, String contents,
                          boolean asHtml)
            throws Exception {
	
        List<Address> addresses = new ArrayList<Address>();
	for(String address: to) {
	    address = address.trim();
	    if(address.startsWith("#")) continue;
	    addresses.add(new InternetAddress(address));
	}
	if(addresses.size()==0) return 0;

        sendEmail(addresses,
                  new InternetAddress( getAdminEmail()), subject, contents, false,
                  false,null);
	return addresses.size();
    }    


    /**
     * _more_
     *
     * @param to _more_
     * @param from _more_
     * @param subject _more_
     * @param contents _more_
     * @param asHtml _more_
     *
     * @throws Exception _more_
     */
    public void sendEmail(String to, String from, String subject,
                          String contents, boolean asHtml)
            throws Exception {

        sendEmail(to, from, subject, contents, asHtml, null);
    }

    /**
     * _more_
     *
     * @param to _more_
     * @param from _more_
     * @param subject _more_
     * @param contents _more_
     * @param asHtml _more_
     * @param file _more_
     *
     * @throws Exception _more_
     */
    public void sendEmail(String to, String from, String subject,
                          String contents, boolean asHtml, File file)
            throws Exception {
        sendEmail((List<Address>) Misc.newList(new InternetAddress(to)),
                  new InternetAddress(from), subject, contents, false,
                  asHtml, file);
    }


    /**
     * _more_
     *
     * @param to _more_
     * @param from _more_
     * @param subject _more_
     * @param contents _more_
     * @param bcc _more_
     * @param asHtml _more_
     * @param file _more_
     *
     * @throws Exception _more_
     */
    public void sendEmail(List<Address> to, InternetAddress from,
                          String subject, String contents, boolean bcc,
                          boolean asHtml, File file)
            throws Exception {

        //Defer to the parent
        if (getRepository().getParentRepository() != null) {
            getRepository().getParentRepository().getMailManager().sendEmail(
                to, from, subject, contents, bcc, asHtml, null);

            //Make sure we return!!!!
            return;
        }


        if ( !isEmailEnabled()) {
            throw new IllegalStateException(
                "This RAMADDA server has not been configured to send email");
        }


        String smtpServer = getSmtpServer();
        String smtpUser = getSmtpUser();
        String smtpPassword = getSmtpPassword();
        boolean startTls = getPropertyFromTree(PROP_SMTP_STARTTLS,
                               "false").equals("true");


        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.host", smtpServer);
        props.put("mail.from", from.getAddress());
        javax.mail.Session session = javax.mail.Session.getInstance(props,
                                         null);
        if (startTls) {
            // Port we will connect to on the Amazon SES SMTPendpoint. We are choosing port 25 because we will use
            // STARTTLS to encrypt the connection.
            props.put("mail.smtp.port", 25);


            // Set properties indicating that we want to use STARTTLS to encrypt the connection.
            // The SMTP session will begin on an unencrypted connection, and then the client
            // will issue a STARTTLS command to upgrade to an encrypted connection.
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }


        if (smtpUser != null) {
            props.put("mail.smtp.user", smtpUser);
        }


        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(from);
        Address[] array = new Address[to.size()];
        for (int i = 0; i < to.size(); i++) {
            array[i] = to.get(i);
        }
        msg.setRecipients((bcc
                           ? Message.RecipientType.BCC
                           : Message.RecipientType.TO), array);
        msg.setSubject(subject);
        msg.setSentDate(new Date());
        if (file == null) {
            msg.setContent(contents, (asHtml
                                      ? "text/html"
                                      : "text/plain"));

        } else {
            Multipart    multipart       = new MimeMultipart();
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(contents);
            multipart.addBodyPart(messageBodyPart);
            messageBodyPart = new MimeBodyPart();
            String filename =
                getStorageManager().getFileTail(file.toString());
            DataSource source = new FileDataSource(file.toString());
            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName(filename);
            multipart.addBodyPart(messageBodyPart);
            msg.setContent(multipart);
        }



        // Create a transport.        
        Transport transport = session.getTransport();

        // Send the message.
        try {
            if (smtpUser != null) {
                transport.connect(smtpServer, smtpUser, smtpPassword);
                Address[] recipients = msg.getAllRecipients();
                transport.sendMessage(msg, recipients);
            } else {
                Transport.send(msg);
            }
        } finally {
            // Close and terminate the connection.
            transport.close();
        }

        /*

        if(smtpPassword!=null) {
            System.err.println("password:" + smtpPassword);
            Transport tr = session.getTransport();
            tr.connect(null, smtpPassword);
            tr.send(msg);
        } else {
            Transport.send(msg);
        }
        */
    }



    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        List<Address> to =
            (List<Address>) Misc.newList(new InternetAddress(args[0]));
        InternetAddress from = new InternetAddress(args[0]);
        sendEmailNew(to, from, "test", "message", false, true);
    }


    /**
     *  Do all this reflection so we don't have a dependency into the plugins
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean isSmsEnabled() throws Exception {
        Object twilio =
            getRepository().getApiManager().getApiHandler("twilio");
        if (twilio == null) {
            return false;
        }
        java.lang.reflect.Method send = Misc.findMethod(twilio.getClass(),
                                            "isEnabled", new Class[] {});
        if (send == null) {
            return false;
        }

        return (boolean) send.invoke(twilio);
    }

    /**
     *  Do all this reflection so we don't have a dependency into the plugins
     *
     * @param fromPhone _more_
     * @param toPhone _more_
     * @param message _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */

    public boolean sendTextMessage(String fromPhone, String toPhone,
                                   String message)
            throws Exception {
        Object twilio =
            getRepository().getApiManager().getApiHandler("twilio");
        if (twilio == null) {
            return false;
        }
        Class[] paramTypes = new Class[] { String.class, String.class,
                                           String.class };
        java.lang.reflect.Method send = Misc.findMethod(twilio.getClass(),
                                            "sendTextMessage", paramTypes);
        if (send == null) {
            return false;
        }
        boolean result = (boolean) send.invoke(twilio, fromPhone, toPhone,
                             message);

        return result;
    }



    /**
     * _more_
     *
     * @param to _more_
     * @param from _more_
     * @param subject _more_
     * @param body _more_
     * @param bcc _more_
     * @param asHtml _more_
     *
     * @throws Exception _more_
     */
    public static void sendEmailNew(List<Address> to, InternetAddress from,
                                    String subject, String body, boolean bcc,
                                    boolean asHtml)
            throws Exception {

        Properties props = System.getProperties();
        props.put("mail.transport.protocol", "smtp");



        boolean startTls     = true;
        String  smtpServer   = "email-smtp.us-east-1.amazonaws.com";
        String  smtpUser     = "";
        String  smtpPassword = "";



        if (startTls) {
            // Port we will connect to on the Amazon SES SMTPendpoint. We are choosing port 25 because we will use
            // STARTTLS to encrypt the connection.
            props.put("mail.smtp.port", 25);


            // Set properties indicating that we want to use STARTTLS to encrypt the connection.
            // The SMTP session will begin on an unencrypted connection, and then the client
            // will issue a STARTTLS command to upgrade to an encrypted connection.
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }

        // Create a Session object to represent a mail session with the specified properties. 
        //        Session session = Session.getDefaultInstance(props);
        //        props.put("mail.smtp.host", smtpServer);
        props.put("mail.from", from.getAddress());
        javax.mail.Session session = Session.getInstance(props, null);

        // Create a message with the specified information. 
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress("info@ramadda.org"));
        Address[] array = new Address[to.size()];
        for (int i = 0; i < to.size(); i++) {
            array[i] = to.get(i);
        }
        msg.setRecipients((bcc
                           ? Message.RecipientType.BCC
                           : Message.RecipientType.TO), array);

        msg.setSubject(subject);
        msg.setSentDate(new Date());
        msg.setContent(body, (asHtml
                              ? "text/html"
                              : "text/plain"));


        // Create a transport.        
        Transport transport = session.getTransport();

        // Send the message.
        try {
            if (smtpUser != null) {
                transport.connect(smtpServer, smtpUser, smtpPassword);
            }
            // Send the email.
            transport.sendMessage(msg, msg.getAllRecipients());
        } finally {
            // Close and terminate the connection.
            transport.close();
        }
    }




}
