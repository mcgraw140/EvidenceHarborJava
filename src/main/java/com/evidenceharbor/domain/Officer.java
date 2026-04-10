package com.evidenceharbor.domain;

public class Officer {
    private int id;
    private String name;
    private String badge;

    public Officer() {}
    public Officer(int id, String name, String badge) { this.id=id; this.name=name; this.badge=badge; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }

    @Override public String toString() { return name; }
}
