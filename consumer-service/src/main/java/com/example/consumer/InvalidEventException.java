package com.example.consumer;
public class InvalidEventException extends RuntimeException {
    public InvalidEventException() { super("Invalid order event"); }
}
