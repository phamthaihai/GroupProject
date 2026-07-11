package com.example.groupproject.exception;

public class AdminLockedException extends RuntimeException {
    public AdminLockedException(String message) {
        super(message);
    }
}