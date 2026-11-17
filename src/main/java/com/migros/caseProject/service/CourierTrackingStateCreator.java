package com.migros.caseProject.service;

import com.migros.caseProject.model.CourierTrackingState;
import com.migros.caseProject.repository.CourierTrackingStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourierTrackingStateCreator {

    private final CourierTrackingStateRepository stateRepository;

    public CourierTrackingStateCreator(CourierTrackingStateRepository stateRepository) {
        this.stateRepository = stateRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createIfMissing(String courierId) {
        if (!stateRepository.existsById(courierId)) {
            stateRepository.saveAndFlush(new CourierTrackingState(courierId));
        }
    }
}
