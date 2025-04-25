package com.migros.caseProject.model.data;

public class LocationDTO {
    private String courierId;
    private Double lat;
    private Double lng;
    private Long timestamp;

    public LocationDTO() {
    }

    public LocationDTO(String courierId, Double lat, Double lng, Long timestamp) {
        this.courierId = courierId;
        this.lat = lat;
        this.lng = lng;
        this.timestamp = timestamp;
    }

    public String getCourierId() {
        return courierId;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "LocationDTO{" +
                "courierId='" + courierId + '\'' +
                ", lat=" + lat +
                ", lng=" + lng +
                ", timestamp=" + timestamp +
                '}';
    }
}