/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.geo;


import org.ramadda.util.Utils;

import java.util.List;

/**
 *
 * @author  Jeff McWhirter
 */
public class Address {

    /**  */
    private String address;

    /**  */
    private String city;

    /**  */
    private String county;

    /**  */
    private String state;

    /**  */
    private String postalCode;

    /**  */
    private String country;

    public Address() {
    }

    /**
     *
     *
     * @param address _more_
     * @param city _more_
     * @param county _more_
     * @param state _more_
     * @param postalCode _more_
     * @param country _more_
     */
    public Address(String address, String city, String county, String state,
                   String postalCode, String country) {
        this.address    = address;
        this.city       = city;
        this.state      = state;
        this.postalCode = postalCode;
        this.country    = country;
    }


    /**
       return if all of address/city/state/etc have been set
    */
    public boolean isComplete() {
	return address!=null && city!=null && county!=null && state!=null && postalCode!=null && country!=null;
    }

    public boolean isPartialComplete() {
	return city!=null && county!=null && state!=null && postalCode!=null && country!=null;
    }    


    /**
     *  @return _more_
     */
    public String toString() {
        return address + ", " + city + " " + county+ " " + state + " "+postalCode +" "+ country;
    }

    private static final String DELIM = "<addr_delim>";

    public String encode() {
        return address + DELIM+ city + DELIM + postalCode + DELIM +county+DELIM+ state + DELIM   + country;
    }

    public void decode(String s) {
	List<String> toks = Utils.split(s,DELIM);
	int i=0;
	if(toks.size()>0)
	    address=toks.get(i++);
	if(toks.size()>1)
	    city=toks.get(i++);
	if(toks.size()>2)
	    postalCode=toks.get(i++);
	if(toks.size()>3)
	    county=toks.get(i++);	
	if(toks.size()>4)
	    state=toks.get(i++);
	if(toks.size()>5)
	    country=toks.get(i++);
    }    


    /**
     *  Set the Address property.
     *
     *  @param value The new value for Address
     */
    public void setAddress(String value) {
        address = value;
    }

    /**
     *  Get the Address property.
     *
     *  @return The Address
     */
    public String getAddress() {
        return address;
    }

    /**
     *  Set the City property.
     *
     *  @param value The new value for City
     */
    public void setCity(String value) {
        city = value;
    }

    /**
     *  Get the City property.
     *
     *  @return The City
     */
    public String getCity() {
        return city;
    }

    /**
     *  Set the County property.
     *
     *  @param value The new value for County
     */
    public void setCounty(String value) {
        county = value;
    }

    /**
     *  Get the County property.
     *
     *  @return The County
     */
    public String getCounty() {
        return county;
    }

    /**
     *  Set the State property.
     *
     *  @param value The new value for State
     */
    public void setState(String value) {
        state = value;
    }

    /**
     *  Get the State property.
     *
     *  @return The State
     */
    public String getState() {
        return state;
    }

    /**
     *  Set the PostalCode property.
     *
     *  @param value The new value for PostalCode
     */
    public void setPostalCode(String value) {
        postalCode = value;
    }

    /**
     *  Get the PostalCode property.
     *
     *  @return The PostalCode
     */
    public String getPostalCode() {
        return postalCode;
    }

    /**
     *  Set the Country property.
     *
     *  @param value The new value for Country
     */
    public void setCountry(String value) {
        country = value;
    }

    /**
     *  Get the Country property.
     *
     *  @return The Country
     */
    public String getCountry() {
        return country;
    }






}
