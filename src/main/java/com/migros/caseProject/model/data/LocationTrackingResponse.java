package com.migros.caseProject.model.data;

import java.util.List;

public record LocationTrackingResponse(
        String courierId,
        double segmentDistanceMeters,
        double totalDistanceMeters,
        List<StoreEntrance> storeEntrances) {
}
