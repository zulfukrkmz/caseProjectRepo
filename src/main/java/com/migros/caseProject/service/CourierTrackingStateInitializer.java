package com.migros.caseProject.service;

import org.springframework.stereotype.Component;

@Component
public class CourierTrackingStateInitializer {

    private static final Object[] INITIALIZATION_LOCKS = new Object[64];

    static {
        for (int i = 0; i < INITIALIZATION_LOCKS.length; i++) {
            INITIALIZATION_LOCKS[i] = new Object();
        }
    }

    private final CourierTrackingStateCreator stateCreator;

    public CourierTrackingStateInitializer(CourierTrackingStateCreator stateCreator) {
        this.stateCreator = stateCreator;
    }

    public void ensureExists(String courierId) {
        Object initializationLock = INITIALIZATION_LOCKS[Math.floorMod(courierId.hashCode(),
                INITIALIZATION_LOCKS.length)];
        synchronized (initializationLock) {
            stateCreator.createIfMissing(courierId);
        }
    }
}
