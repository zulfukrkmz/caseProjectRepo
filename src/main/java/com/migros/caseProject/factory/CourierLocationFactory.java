package com.migros.caseProject.factory;

import com.migros.caseProject.model.CourierLocation;
import com.migros.caseProject.model.data.LocationDTO;

public class CourierLocationFactory {

    public static CourierLocation fromDTO(LocationDTO dto) {
        return new CourierLocation(dto.getCourierId(), dto.getLat(), dto.getLng(), dto.getTimestamp());
    }
}