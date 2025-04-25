package com.migros.caseProject.model;

public class Store {
    private String name;
    private Double lat;
    private Double lng;

    public Store() {
    }

    public Store(String name, Double lat, Double lng) {
        this.name = name;
        this.lat = lat;
        this.lng = lng;
    }

    public String getName() {
        return name;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLng() {
        return lng;
    }
}