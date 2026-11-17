package com.migros.caseProject.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "courier_tracking_state")
public class CourierTrackingState {

    @Id
    @Column(name = "courier_id", length = 100)
    private String courierId;

    @Column(name = "last_latitude")
    private Double lastLatitude;

    @Column(name = "last_longitude")
    private Double lastLongitude;

    @Column(name = "last_timestamp")
    private Long lastTimestamp;

    @Column(name = "total_distance_meters", nullable = false)
    private double totalDistanceMeters;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "courier_inside_stores", joinColumns = @JoinColumn(name = "courier_id"))
    @MapKeyColumn(name = "store_name", length = 255)
    @Column(name = "is_inside", nullable = false)
    private Map<String, Boolean> insideStores = new HashMap<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "courier_store_entry_times", joinColumns = @JoinColumn(name = "courier_id"))
    @MapKeyColumn(name = "store_name", length = 255)
    @Column(name = "last_counted_entry_timestamp", nullable = false)
    private Map<String, Long> lastCountedEntryTimestamps = new HashMap<>();

    protected CourierTrackingState() {
    }

    public CourierTrackingState(String courierId) {
        this.courierId = courierId;
    }

    public String getCourierId() {
        return courierId;
    }

    public Double getLastLatitude() {
        return lastLatitude;
    }

    public void setLastLatitude(Double lastLatitude) {
        this.lastLatitude = lastLatitude;
    }

    public Double getLastLongitude() {
        return lastLongitude;
    }

    public void setLastLongitude(Double lastLongitude) {
        this.lastLongitude = lastLongitude;
    }

    public Long getLastTimestamp() {
        return lastTimestamp;
    }

    public void setLastTimestamp(Long lastTimestamp) {
        this.lastTimestamp = lastTimestamp;
    }

    public double getTotalDistanceMeters() {
        return totalDistanceMeters;
    }

    public void setTotalDistanceMeters(double totalDistanceMeters) {
        this.totalDistanceMeters = totalDistanceMeters;
    }

    public Map<String, Boolean> getInsideStores() {
        return insideStores;
    }

    public Map<String, Long> getLastCountedEntryTimestamps() {
        return lastCountedEntryTimestamps;
    }
}
