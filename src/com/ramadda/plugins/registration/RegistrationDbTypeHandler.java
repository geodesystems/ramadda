/**
 * Copyright (c) 2008-2015 Geode Systems LLC
 * This Software is licensed under the Geode Systems RAMADDA License available in the source distribution in the file
 * ramadda_license.txt. The above copyright notice shall be included in all copies or substantial portions of the Software.
 */

package com.ramadda.plugins.registration;


import org.ramadda.plugins.db.*;


import org.ramadda.repository.*;

import org.ramadda.repository.output.*;


import org.ramadda.repository.type.*;
import org.ramadda.util.FormInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JQuery;
import org.ramadda.util.Site;



import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;


import org.w3c.dom.*;


import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;



import java.io.File;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;



/**
 *
 */

public class RegistrationDbTypeHandler extends DbTypeHandler {


    //10 50 100 1000
    //60 40 20 10
    //10  50    100
    //600 1500 3000


    


    private static final int [] USERS   = {0,10,25,50,100,250,500,1000};
    private static final int [] PPU  =    {0, 60,48,36,24,12,8,6};


    /** _more_ */
    public static final String STATE_PENDING = "pending";

    /** _more_ */
    public static final String STATE_PURCHASED = "purchased";




    /** _more_ */
    private static int IDX = IDX_MAX_INTERNAL;

    /** _more_ */
    public static final int IDX_DATE = ++IDX;

    /** _more_ */
    public static final int IDX_TYPE = ++IDX;


    /** _more_ */
    public static final int IDX_NAME = ++IDX;

    /** _more_ */
    public static final int IDX_EMAIL = ++IDX;

    /** _more_ */
    public static final int IDX_ORGANIZATION = ++IDX;

    /** _more_ */
    public static final int IDX_COUNTRY = ++IDX;

    /** _more_ */
    public static final int IDX_STATE = ++IDX;

    /** _more_ */
    public static final int IDX_CODE = ++IDX;

    /** _more_ */
    public static final int IDX_URL = ++IDX;


    /** _more_ */
    public static final int IDX_USERS = ++IDX;

    public static final int IDX_ORGANIZATION_TYPE = ++IDX;

    /** _more_ */
    public static final int IDX_SUPPORT_TIER = ++IDX;


    /** _more_ */
    public static final int IDX_LICENSE_AMOUNT = ++IDX;


    /** _more_ */
    public static final int IDX_SUPPORT_AMOUNT = ++IDX;

    /** _more_ */
    public static final int IDX_TOTAL_AMOUNT = ++IDX;


    /** _more_ */
    public static final int IDX_KEY = ++IDX;

    /** _more_ */
    public static final int IDX_DETAILS = ++IDX;

    /** _more_ */
    public static final int IDX_COMMENTS = ++IDX;



    /** _more_ */
    public static final String ARG_ACTION = "action";

    public static final String ARG_ACADEMIC = "academic";



    /** _more_ */
    public static final String ARG_COMMENTS = "comments";

    /** _more_ */
    public static final String ARG_REGISTER = "register";

    /** _more_ */
    public static final String ARG_USERS = "users";

    /** _more_ */
    public static final String ARG_NAME = "name";


    public static final String ARG_SUPPORT_TIER = "support_tier";

    /** _more_ */
    public static final String ARG_ORGANIZATION = "organization";

    /** _more_ */
    public static final String ARG_EMAIL = "email";

    /** _more_ */
    public static final String ARG_URL = "url";

    /** _more_ */
    public static final String ACTION_BUY = "buy";


    /** _more_ */
    public static final String ACTION_PAID = "paid";

    /** _more_ */
    public static final String ACTION_CANCEL = "cancel";


    /** _more_ */
    public static final String PROP_PAYPAL_BUSINESS = "paypal_business";

    /** _more_ */
    public static final String ARG_PAYPAL_INVOICE = "invoice";

    /** _more_ */
    public static final String PROP_PRICE_PER_USER = "price_per_user";

    /** _more_ */
    public static final String PROP_DO_PAY = "do_pay";


    /** _more_ */
    public static final String PROP_HTML_REGISTER = "register_html";

    /** _more_ */
    public static final String PROP_HTML_DONE = "done_html";

    /** _more_ */
    private static final String PAYPAL_URL =
        "https://www.paypal.com/cgi-bin/webscr?business=${paypal_business}&cmd=_xclick&item_name=${name}&amount=${amount}&currency_code=USD&rm=2&no_shipping=1&cbt=Return to geodesystems.com to retrieve registration key";

    /** _more_          */
    private static final String[] PAYPAL_URLARGS = new String[] {
        "first_name", "last_name", "payer_email", "payment_gross",
        "payment_fee", "residence_country", "txn_id", "auth", "payer_id"
    };


    /** _more_ */
    private List<Column> userColumns;


    /**
     * _more_
     *
     *
     * @param dbAdmin _more_
     * @param repository _more_
     * @param tableName _more_
     * @param tableNode _more_
     * @param desc _more_
     *
     * @throws Exception _more_
     */
    public RegistrationDbTypeHandler(Repository repository, String tableName,
                                     Element tableNode, String desc)
            throws Exception {
        super(repository, tableName, tableNode, desc);
    }



    /**
     * _more_
     *
     * @return _more_
     */
    private List<Column> getUserColumns() {
        if (userColumns == null) {
            List<Column> tmp = new ArrayList<Column>();
            for (String name : new String[] {
                    "name", "email", "organization", "country", "state_province", "postal_code",
                "url",
            }) {
                tmp.add(getColumn(name));
            }
            userColumns = tmp;
        }

        return userColumns;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Result getHtmlDisplay(Request request, Entry entry)
            throws Exception {

        if (request.getUser().getAdmin() && !request.get("force", false)) {
            return super.getHtmlDisplay(request, entry);
        }

        return processRegistration(request, entry);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processRegistration(Request request, Entry entry)
            throws Exception {

        Hashtable props = getProperties(entry);

        StringBuilder message          = new StringBuilder();

        StringBuilder required         = new StringBuilder();
        String        templateProperty = PROP_HTML_REGISTER;
        String templatePath =
            "/com/ramadda/plugins/registration/register.html";
        String noBuyMessage =
            " RAMADDA is great but I am unable to pay the registration fee at this time";


        StringBuffer html   = new StringBuffer();
        StringBuffer sb     = new StringBuffer();
        String       action = request.getString(ARG_ACTION, "");

        int pricePerUser =  Utils.getProperty(props,
                               PROP_PRICE_PER_USER, 60);

        int users = request.get(ARG_USERS, 0);
        int ppu = 0;
        for(int i=0;i<USERS.length;i++) {
            if(users == USERS[i]) {
                ppu = PPU[i];
                break;
            }
        }
        
        double licenseAmount = ppu * users;

        int supportTier = request.get(ARG_SUPPORT_TIER,0);
        double supportAmount =0;
        if(supportTier == 1) supportAmount = 600; 
        else if(supportTier == 2) supportAmount = 3000; 

        double totalAmount = licenseAmount + supportAmount ;
        Object[] values = getValues(entry, (String)null);
        initializeValueArray(request, null, values);
        for (Column column : getUserColumns()) {
            column.setValue(request, entry, values);
        }

        values[IDX_DATE]     = new Date();
        values[IDX_COMMENTS] = "";
        values[IDX_TYPE]     = "";
        values[IDX_LICENSE_AMOUNT]   = Double.valueOf(0);
        values[IDX_SUPPORT_AMOUNT]   = Double.valueOf(0);
        values[IDX_TOTAL_AMOUNT]   = Double.valueOf(0);
        values[IDX_USERS]    =  Integer.valueOf(1);
        values[IDX_SUPPORT_TIER]    = "1";
        values[IDX_ORGANIZATION_TYPE] = "normal";


        String name         = (String) values[IDX_NAME];
        String email        = (String) values[IDX_EMAIL];
        String organization = (String) values[IDX_ORGANIZATION];
        String userUrl      = (String) values[IDX_URL];
        String comments = request.getAnonymousEncodedString(ARG_COMMENTS, "");

        if (action.equals(ACTION_CANCEL)) {
            //TODO:
        }

        //This is the return request from paypal
        if (action.equals(ACTION_PAID)) {
            String invoiceId =
                request.getAnonymousEncodedString(ARG_PAYPAL_INVOICE, "");
            if ( !Utils.stringDefined(invoiceId)) {
                return new Result(
                    entry.getName(),
                    new StringBuffer(
                        getRepository().getPageHandler().showDialogError(
                            "No invoice ID given")));
            }
            values = getValues(entry, invoiceId);
            if (values == null) {
                return new Result(
                    entry.getName(),
                    new StringBuffer(
                        getRepository().getPageHandler().showDialogError(
                            "Could not find invoice")));
            }

            if (Misc.equals(values[IDX_TYPE], STATE_PENDING)) {
                values[IDX_TYPE] = STATE_PURCHASED;
                StringBuffer extra = new StringBuffer();
                for (String arg : PAYPAL_URLARGS) {
                    if (request.defined(arg)) {
                        extra.append(arg);
                        extra.append("=");
                        extra.append(request.getAnonymousEncodedString(arg,
                                ""));
                        extra.append("\n");
                    }
                }
                values[IDX_DETAILS] = extra.toString();
                addRegistration(request, entry, values, STATE_PURCHASED,
                                false);
            }
            return processDone(request, entry, values, "");
        }

        boolean academic = request.get(ARG_ACADEMIC,false);
        boolean buying = action.equals(ACTION_BUY) || request.exists(ARG_REGISTER);
        boolean ok = false;
        if (request.exists(ARG_REGISTER) ||  buying) {
            ok = true;
            for (Column column : userColumns) {
                if ( !column.isRequired()) {
                    continue;
                }
                if ( !request.defined(column.getEditArg())) {
                    required.append(column.getLabel() + " is required"
                                    + HtmlUtils.br());
                    ok = false;
                }
            }

            if ( !request.get(ARG_AGREE, false)) {
                required.append("You need to agree to the license"
                                + HtmlUtils.br());
                ok = false;
            }

            if (ok) {
                ok  =   getRepository().getUserManager().isHuman(request, required);
            }

            if (ok && buying) {
                if (totalAmount <= 0 || totalAmount!=totalAmount) {
                    required.append("Please enter number of users or select a support tier"  + HtmlUtils.br());
                    ok = false;
                }
            }
        }

        if (required.length() > 0) {
            message.append(
                getRepository().getPageHandler().showDialogError(
                    required.toString()));
        }




        //purchase -  save the state and forward them to paypal
        if (ok && buying) {
            values[IDX_TYPE]   = STATE_PENDING;
            values[IDX_SUPPORT_TIER] = ""+supportTier;
            values[IDX_LICENSE_AMOUNT] = Double.valueOf(licenseAmount);
            values[IDX_SUPPORT_AMOUNT] = Double.valueOf(supportAmount);
            values[IDX_TOTAL_AMOUNT] = Double.valueOf(totalAmount);
            values[IDX_USERS]  =  Integer.valueOf(users);
            values[IDX_ORGANIZATION_TYPE] = academic ?"academic":"other";
            //Store
            doStore(entry, values, true);


            String returnUrl = request.getAbsoluteUrl(
                                   request.entryUrl(
                                       getRepository().URL_ENTRY_SHOW,
                                       entry));
            String id       = values[IDX_DBID].toString();
            String itemName = "Ramadda registration for " + users + " users";
            String url = PAYPAL_URL.replace("${amount}",
                                            totalAmount + "").replace("${name}",
                                                itemName);
            String business = Misc.getProperty(props, PROP_PAYPAL_BUSINESS,
                                  "");
            if ( !Utils.stringDefined(business)) {
                business = "info@ramadda.org";
            }
            url = url.replace("${paypal_business}", business);
            //<Company>_<Service>_<Product>_<Country>
            url += "&" + HtmlUtils.arg(ARG_PAYPAL_INVOICE, id, true);
            url += "&"
                   + HtmlUtils.arg("bn", "GeodeSystems_BuyNow_WPS_US", true);
            url += "&"
                   + HtmlUtils.arg("return",
                                   returnUrl + "&"
                                   + HtmlUtils.arg(ARG_ACTION,
                                       ACTION_PAID), true);
            url += "&"
                   + HtmlUtils.arg("cancel_return",
                                   returnUrl + "?"
                                   + HtmlUtils.arg(ARG_ACTION,
                                       ACTION_CANCEL), true);
            url += "&"
                   + HtmlUtils.arg(
                       "image_url",
                       "http://geodesystems.com/repository/images/geodesystems.png",
                       true);

            return new Result(url);
        }



        String template = Misc.getProperty(props, templateProperty,
                                           (String) null);

        if ( !Utils.stringDefined(template)) {
            template = getRepository().getResource(templatePath);
        }

        html.append(getWikiManager().wikifyEntry(request, entry,
                entry.getDescription()));

        String formId = HtmlUtils.getUniqueId("regform_");
        html.append(request.form(getRepository().URL_ENTRY_SHOW,
                               HtmlUtils.id(formId)));
        if (request.get("force", false)) {
            html.append(HtmlUtils.hidden("force", "true"));
        }
        html.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));





        StringBuilder infoSB = new StringBuilder();
        infoSB.append(HtmlUtils.formTable(HtmlUtils.id(formId)+HtmlUtils.cssClass("formtable")));

        FormInfo  formInfo = new FormInfo(formId);
        Hashtable state    = new Hashtable();
        for (Column column : getUserColumns()) {
            getTableTypeHandler().addColumnToEntryForm(request, column,
                                                       infoSB, entry, null /* values*/, state, formInfo, this);

            if(column.getName().equals("organization")) {
                infoSB.append(HtmlUtils.formEntry("",  HtmlUtils.checkbox(ARG_ACADEMIC,"true",false) + HtmlUtils.space(1) + "This is an academic institution"));
            }
        }

        StringBuilder validateJavascript = new StringBuilder("");
        formInfo.addJavascriptValidation(validateJavascript);
        String script = JQuery.ready(JQuery.submit(JQuery.id(formId),
                            validateJavascript.toString()));
        infoSB.append(HtmlUtils.script(script));
        infoSB.append(HtmlUtils.formTableClose());

        StringBuilder regSB = new StringBuilder();

        List<TwoFacedObject> sizes = new ArrayList<TwoFacedObject>();
        for(int i=0;i<USERS.length;i++)  {
            int price = USERS[i] * PPU[i];
            String label = USERS[i] +" Users ";
            label += " - ";
            sizes.add(new TwoFacedObject(label + "$" + price + "",""+USERS[i]));
        }

        String usersHelp = "<a href=\"#users\">Details</a>";
        regSB.append("\n:heading How many users?\n");
        regSB.append(HtmlUtils.select(ARG_USERS, sizes, request.getString(ARG_USERS,""+USERS[1]))
                     +HtmlUtils.space(2) + usersHelp);


        List<TwoFacedObject> tiers = new ArrayList<TwoFacedObject>();
        tiers.add(new TwoFacedObject("No support","0"));
        tiers.add(new TwoFacedObject("Tier 1 support - $600/year","1"));
        tiers.add(new TwoFacedObject("Tier 2 support - $1200/year","2"));
        tiers.add(new TwoFacedObject("Tier 3 support - $6000/year","3"));

        regSB.append("\n<p>\n:heading Support level\n");
        String supportHelp = "<a href=\"#support\">Details</a>";
        regSB.append(HtmlUtils.select(ARG_SUPPORT_TIER, tiers, request.getString(ARG_SUPPORT_TIER,"1")) +
                     HtmlUtils.space(2) + supportHelp);


        StringBuilder licenseSB =
            new StringBuilder(getRepository().getAdmin().getLicenseForm());
        licenseSB.append(HtmlUtils.p());
        getRepository().getUserManager().makeHumanForm(request, licenseSB, formInfo);
        licenseSB.append(HtmlUtils.p());
        licenseSB.append(HtmlUtils.submit("Register", ARG_REGISTER));
        sb.append(HtmlUtils.formClose());
        sb.append(HtmlUtils.script("var pricePerUser = " + pricePerUser
                                   + ";\n"));
        sb.append(
            HtmlUtils.importJS(
                getRepository().getHtdocsUrl("/registration/registration.js")));



        template = template.replace("${banner}","banner");
        template = template.replace("${form1}", infoSB.toString());
        template = template.replace("${reg}", regSB.toString());
        template = template.replace("${license}", licenseSB.toString());
        template = template.replace("${form2}", sb.toString());
        template = template.replace("${price_per_user}", "" + pricePerUser);
        template = template.replace("${message}", message.toString());




        template = getWikiManager().wikifyEntry(request, entry, template);
        html.append(template);

        return new Result(entry.getName(), html);

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param formBuffer _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void addToEditForm(Request request, Entry entry,
                              Appendable formBuffer)
            throws Exception {
        super.addToEditForm(request, entry, formBuffer);
        Hashtable props = getProperties(entry);
        formBuffer.append(
            HtmlUtils.row(
                HtmlUtils.colspan(
                    HtmlUtils.div(
                        "Registration Settings",
                        HtmlUtils.cssClass("formgroupheader")), 2)));
        formBuffer.append(HtmlUtils.formEntry("",
                HtmlUtils.checkbox(PROP_DO_PAY, "true",
                                   Misc.getProperty(props, PROP_DO_PAY,
                                       false)) + " Allow Pay"));
        formBuffer.append(
            HtmlUtils.formEntry(
                msgLabel("Price/user"),
                HtmlUtils.input(
                    PROP_PRICE_PER_USER,
                    "" + Utils.getProperty(props, PROP_PRICE_PER_USER, 60),
                    HtmlUtils.SIZE_5)));
        formBuffer.append(HtmlUtils.formEntry(msgLabel("Paypal Account"),
                HtmlUtils.input(PROP_PAYPAL_BUSINESS,
                                Misc.getProperty(props, PROP_PAYPAL_BUSINESS,
                                    ""), HtmlUtils.SIZE_60)));

        String textarea = getRepository().getWikiManager().makeWikiEditBar(
                              request, entry,
                              PROP_HTML_REGISTER) + HtmlUtils.br()
                                  + HtmlUtils.textArea(
                                      PROP_HTML_REGISTER,
                                      Misc.getProperty(
                                          props, PROP_HTML_REGISTER, ""), 25,
                                              100,
                                              HtmlUtils.id(
                                                  PROP_HTML_REGISTER));

        formBuffer.append(HtmlUtils.formEntry(msgLabel("Registration Page"),
                textarea));

        textarea =
            getRepository().getWikiManager().makeWikiEditBar(request, entry,
                PROP_HTML_DONE) + HtmlUtils.br()
                                + HtmlUtils.textArea(PROP_HTML_DONE,
                                    Misc.getProperty(props, PROP_HTML_DONE,
                                        ""), 25, 100,
                                             HtmlUtils.id(PROP_HTML_DONE));
        formBuffer.append(HtmlUtils.formEntry(msgLabel("Done Page"),
                textarea));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param parent _more_
     * @param newEntry _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeEntryFromForm(Request request, Entry entry,
                                        Entry parent, boolean newEntry)
            throws Exception {
        super.initializeEntryFromForm(request, entry, parent, newEntry);
        Hashtable props = getProperties(entry);
        props.put(PROP_PRICE_PER_USER,
                  request.getString(PROP_PRICE_PER_USER, ""));
        props.put(PROP_PAYPAL_BUSINESS,
                  request.getString(PROP_PAYPAL_BUSINESS, ""));
        props.put(PROP_DO_PAY, "" + request.get(PROP_DO_PAY, false));
        props.put(PROP_HTML_REGISTER,
                  request.getString(PROP_HTML_REGISTER, ""));
        props.put(PROP_HTML_DONE, request.getString(PROP_HTML_DONE, ""));
        //        props.put(PROP_ANONFORM_MESSAGE,request.getString(PROP_ANONFORM_MESSAGE,""));
        setProperties(entry, props);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param values _more_
     * @param type _more_
     * @param newReg _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private void addRegistration(Request request, Entry entry,
                                 Object[] values, String type, boolean newReg)
            throws Exception {
        int    users = ((Integer) values[IDX_USERS]).intValue();
        String id    = (String) values[IDX_DBID];
        String reg   = makeRegistration(id, users, (String)values[IDX_ORGANIZATION_TYPE]);
        values[IDX_KEY]  = reg;
        doStore(entry, values, newReg);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param values _more_
     * @param message _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result processDone(Request request, Entry entry, Object[] values,
                               String message)
            throws Exception {
        Hashtable props = getProperties(entry);
        String template = Misc.getProperty(props, PROP_HTML_DONE,
                                           (String) null);
        if (Utils.stringUndefined(template)) {
            template = getRepository().getResource(
                "/com/ramadda/plugins/registration/done.html");
        }
        template = template.replace("${message}", message);
        template = getWikiManager().wikifyEntry(request, entry, template);


        template = template.replace("${key}", "" + values[IDX_KEY]);

        return new Result(entry.getName(), new StringBuilder(template));
    }

    /**
     * _more_
     *
     *
     * @param id _more_
     * @param email _more_
     * @param users _more_
     *
     * @return _more_
     */
    private static String makeRegistration(String id, int users, String orgType) {
        String dateString =
            new SimpleDateFormat("yyyyMMdd").format(new Date());
        String delim = ":";
        double version = 2.2;
        //This should follow  RepositoryUtil.MAJOR_VERSION
        //id:keyword:version:date:orgtype:users:
        return id + delim + Utils.obfuscate("buenobueno", true) + delim
            + Utils.obfuscate(Double.toString(version), true) + delim
               + Utils.obfuscate(dateString, true) + delim
               + Utils.obfuscate(orgType, true) + delim
               + Utils.obfuscate("" + users, true);
    }

    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        System.err.println(makeRegistration(Utils.getGuid(), 10,"academic"));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processCancel(Request request, Entry entry)
            throws Exception {
        System.err.println("Registration Cancel:" + request);
        StringBuffer sb = new StringBuffer();

        return new Result(entry.getName(), sb);
    }







}
