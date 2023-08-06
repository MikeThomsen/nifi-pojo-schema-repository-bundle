package org.apache.nifi.pojo.complex;

public class PaymentService {
    private String name;
    private Location serviceLocation;

    public PaymentService() {}

    public PaymentService(String name, Location serviceLocation) {
        this.name = name;
        this.serviceLocation = serviceLocation;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Location getServiceLocation() {
        return serviceLocation;
    }

    public void setServiceLocation(Location serviceLocation) {
        this.serviceLocation = serviceLocation;
    }
}
