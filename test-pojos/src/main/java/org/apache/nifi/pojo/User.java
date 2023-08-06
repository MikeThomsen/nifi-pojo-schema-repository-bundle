package org.apache.nifi.pojo;

public class User {
    private String username;
    private String password;
    private Integer failedLoginCount;

    public User(String username, String password, Integer failedLoginCount) {
        this.username = username;
        this.password = password;
        this.failedLoginCount = failedLoginCount;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getFailedLoginCount() {
        return failedLoginCount;
    }

    public void setFailedLoginCount(Integer failedLoginCount) {
        this.failedLoginCount = failedLoginCount;
    }
}
