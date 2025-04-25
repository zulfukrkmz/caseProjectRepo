package com.migros.caseProject.model;

public class CourierLocation {

    private String courierId; // Kuryenin kimliği
    private Double lat;       // Enlem
    private Double lng;       // Boylam
    private Long timestamp;   // Konumun kaydedildiği zaman (epoch time)

    // Constructor
    public CourierLocation(String courierId, Double lat, Double lng, Long timestamp) {
        this.courierId = courierId;
        this.lat = lat;
        this.lng = lng;
        this.timestamp = timestamp;
    }

    // Getter ve Setter'lar
    public String getCourierId() {
        return courierId;
    }

    public void setCourierId(String courierId) {
        this.courierId = courierId;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "CourierLocation{" +
                "courierId='" + courierId + '\'' +
                ", lat=" + lat +
                ", lng=" + lng +
                ", timestamp=" + timestamp +
                '}';
    }
}