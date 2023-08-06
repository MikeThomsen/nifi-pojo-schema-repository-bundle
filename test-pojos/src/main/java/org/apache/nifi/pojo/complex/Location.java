package org.apache.nifi.pojo.complex;

public class Location {
    private String streetAddress;
    private String city;
    private String province;
    private String country;

    public Location() {}

    public Location(String streetAddress, String city, String province, String country) {
        this.streetAddress = streetAddress;
        this.city = city;
        this.province = province;
        this.country = country;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
