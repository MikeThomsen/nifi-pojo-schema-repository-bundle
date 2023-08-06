package org.apache.nifi.pojo.complex;

public enum Country {
    UNITED_STATES("us"),
    UNITED_KINGDOM("uk"),
    CANADA("ca");

    private String value;

    Country(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
