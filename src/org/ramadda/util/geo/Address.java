/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.geo;

import org.ramadda.util.Utils;
import java.util.List;

public class Address {
    private String address;
    private String city;
    private String county;
    private String state;
    private String postalCode;
    private String country;

    public Address() {
    }


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

    public void setAddress(String value) {
        address = value;
    }

    public String getAddress() {
        return address;
    }

    public void setCity(String value) {
        city = value;
    }


    public String getCity() {
        return city;
    }


    public void setCounty(String value) {
        county = value;
    }

    public String getCounty() {
        return county;
    }

    public void setState(String value) {
        state = value;
    }


    public String getState() {
        return state;
    }

    public void setPostalCode(String value) {
        postalCode = value;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setCountry(String value) {
        country = value;
    }

    public String getCountry() {
        return country;
    }

}
