package org.apache.nifi.pojo.complex;

import java.time.Instant;

public class Person {
    private String firstName;
    private String middleName;
    private String lastName;
    private Instant dateOfBirth;

    private Location mailingAddress;

    public Person() {}

    public Person(String firstName, String middleName,
                  String lastName, Instant dateOfBirth,
                  Location mailingAddress) {
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.mailingAddress = mailingAddress;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Instant getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(Instant dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Location getMailingAddress() {
        return mailingAddress;
    }

    public void setMailingAddress(Location mailingAddress) {
        this.mailingAddress = mailingAddress;
    }

    @Override
    public String toString() {
        return "Person{" +
                "firstName='" + firstName + '\'' +
                ", middleName='" + middleName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", mailingAddress=" + mailingAddress +
                '}';
    }
}
