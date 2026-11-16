package com.migros.caseProject.service;

public class InvalidLocationTimestampException extends RuntimeException {

    public InvalidLocationTimestampException(String courierId) {
        super("Location timestamp must be newer than the courier's last recorded location: " + courierId);
    }
}
