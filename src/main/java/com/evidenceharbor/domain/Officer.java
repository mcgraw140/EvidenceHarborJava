package com.evidenceharbor.domain;

public class Officer {
    private int id;
    private String name;
    private String badge;
    private String username;
    private String passwordHash;
    private String role;
    private String status;
    private boolean external;
    private String permissions;

    public Officer() {
        this.role = "officer";
        this.status = "Active";
    }
    public Officer(int id, String name, String badge) {
        this.id = id; this.name = name; this.badge = badge;
        this.role = "officer"; this.status = "Active";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isExternal() { return external; }
    public void setExternal(boolean external) { this.external = external; }
    public String getPermissions() { return permissions; }
    public void setPermissions(String permissions) { this.permissions = permissions; }

    @Override public String toString() { return name; }
}
