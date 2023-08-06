package org.apache.nifi.pojo.complex;

public class Location {
    private String streetAddress;
    private String city;
    private String province;
    private String country;

    private String zipCode;

    public Location() {}

    public Location(String streetAddress, String city, String province, String country, String zipCode) {
        this.streetAddress = streetAddress;
        this.city = city;
        this.province = province;
        this.country = country;
        this.zipCode = zipCode;
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

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    @Override
    public String toString() {
        return "Location{" +
                "streetAddress='" + streetAddress + '\'' +
                ", city='" + city + '\'' +
                ", province='" + province + '\'' +
                ", country='" + country + '\'' +
                ", zipCode='" + zipCode + '\'' +
                '}';
    }
}
