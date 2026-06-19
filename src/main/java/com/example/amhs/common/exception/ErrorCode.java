package com.example.amhs.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    NODE_NOT_FOUND(HttpStatus.NOT_FOUND, "Node not found"),
    EDGE_NOT_FOUND(HttpStatus.NOT_FOUND, "Edge not found"),
    ALERT_NOT_FOUND(HttpStatus.NOT_FOUND, "Alert not found"),
    EQUIPMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Equipment not found"),
    NO_AVAILABLE_EQUIPMENT(HttpStatus.BAD_REQUEST, "No available equipment"),
    TRANSFER_JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "Transfer job not found"),
    ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "Route not found"),
    INVALID_JOB_STATUS(HttpStatus.BAD_REQUEST, "Invalid transfer job status"),
    INVALID_JOB_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "Invalid transfer job status transition"),
    DUPLICATED_NODE_CODE(HttpStatus.CONFLICT, "Duplicated node code"),
    DUPLICATED_EDGE(HttpStatus.CONFLICT, "Duplicated edge"),
    DUPLICATED_EQUIPMENT_CODE(HttpStatus.CONFLICT, "Duplicated equipment code"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
