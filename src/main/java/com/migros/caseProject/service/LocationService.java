package com.migros.caseProject.service;

import com.migros.caseProject.model.CourierLocation;
import com.migros.caseProject.model.Store;
import com.migros.caseProject.model.data.LocationDTO;
import com.migros.caseProject.util.GeoUtils;
import com.migros.caseProject.util.StoreLoader;
import org.springframework.stereotype.Service;
import com.migros.caseProject.factory.CourierLocationFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LocationService {

    private final StoreLoader storeLoader;
    private final Map<String, Long> lastEntryTimestamps = new HashMap<>();
    private final Map<String, Double> totalDistances = new HashMap<>();
    private final Map<String, CourierLocation> lastLocations = new HashMap<>();

    // Constructor Injection
    public LocationService(StoreLoader storeLoader) {
        this.storeLoader = storeLoader;
    }

    // Kuryenin belirli bir mağazaya girişini kontrol etme ve reentry kontrolü
    public void checkCourierStoreEntrance(CourierLocation courierLocation) {
        List<Store> stores = storeLoader.loadStores(); // Mağazaları yükle
        boolean enteredStore = false; // Giriş yapılan mağaza kontrolü

        for (Store store : stores) {
            Double distance = GeoUtils.calculateDistance(courierLocation.getLat(), courierLocation.getLng(),
                    store.getLat(), store.getLng());

            if (distance < 100 && !enteredStore) {  // 100 metre mesafede ve daha önce giriş yapılmamış
                Long lastEntryTime = lastEntryTimestamps.get(courierLocation.getCourierId() + store.getName());

                // Eğer 1 dakikadan daha kısa süre geçtiyse, yeniden giriş sayılmaz
                if (lastEntryTime != null && (courierLocation.getTimestamp() - lastEntryTime) < 60000) {
                    System.out.println("Kurye " + courierLocation.getCourierId() + " aynı mağazaya yeniden girmedi: " + store.getName());
                } else {
                    lastEntryTimestamps.put(courierLocation.getCourierId() + store.getName(), courierLocation.getTimestamp());
                    System.out.println("Kurye " + courierLocation.getCourierId() + " mağazaya girdi: " + store.getName());
                    enteredStore = true; // İlk mağaza girişi yapıldığında işaretle
                }
            }
        }
    }

    // Kuryenin hareketini takip etme ve mesafe hesaplama
    public void trackCourierMovement(CourierLocation courierLocation) {
        Double totalDistance = totalDistances.getOrDefault(courierLocation.getCourierId(), 0.0);
        CourierLocation previousLocation = lastLocations.get(courierLocation.getCourierId()); // En son konumu al

        if (previousLocation != null) {
            Double distance = GeoUtils.calculateDistance(previousLocation.getLat(), previousLocation.getLng(),
                    courierLocation.getLat(), courierLocation.getLng());
            totalDistance += distance;
        }

        totalDistances.put(courierLocation.getCourierId(), totalDistance); // Toplam mesafeyi güncelle
        lastLocations.put(courierLocation.getCourierId(), courierLocation); // Son konumu güncelle
    }

    // Toplam mesafe hesaplama
    public Double getTotalTravelDistance(String courierId) {
        return totalDistances.getOrDefault(courierId, 0.0);
    }

    // DTO'dan alınan veriyi kullanarak işlemleri tetikler
    public void processLocation(LocationDTO locationDTO) {
        // Factory kullanarak CourierLocation nesnesini oluşturuyoruz
        CourierLocation courierLocation = CourierLocationFactory.fromDTO(locationDTO);

        // Hem hareketi takip et hem de mağaza girişini kontrol et
        trackCourierMovement(courierLocation);
        checkCourierStoreEntrance(courierLocation);
    }
}