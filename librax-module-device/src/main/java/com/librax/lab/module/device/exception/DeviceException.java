package com.librax.lab.module.device.exception;


import lombok.Getter;

@Getter
public class DeviceException extends RuntimeException {

    private final String errorCode;

    public DeviceException(String message) {
        super(message);
        this.errorCode = "DEVICE_ERROR";
    }

    public DeviceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "DEVICE_ERROR";
    }

    public DeviceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public DeviceException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

}

