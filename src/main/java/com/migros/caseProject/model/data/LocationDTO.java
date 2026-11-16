package com.migros.caseProject.model.data;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class LocationDTO {
    @NotBlank
    @Size(max = 100)
    private String courierId;

    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double lat;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double lng;

    @NotNull
    @PositiveOrZero
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

    public void setCourierId(String courierId) {
        this.courierId = courierId;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
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