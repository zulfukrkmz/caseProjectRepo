package com.migros.caseProject.service;

public class InvalidLocationTimestampException extends RuntimeException {

    public InvalidLocationTimestampException(String courierId) {
        super("Location timestamp must not be older than the courier's last recorded location: " + courierId);
    }
}
