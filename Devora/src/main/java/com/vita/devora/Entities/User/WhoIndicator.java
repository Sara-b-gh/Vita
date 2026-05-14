package com.vita.devora.Entities;

public class WhoIndicator {

    private String name;
    private String value;
    private String code;

    public WhoIndicator(String name, String value, String code) {
        this.name = name;
        this.value = value;
        this.code = code;
    }

    public String getName() { return name; }
    public String getValue() { return value; }
    public String getCode() { return code; }
}