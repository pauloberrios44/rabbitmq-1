package com.example.publisher;
public class InvalidEventException extends RuntimeException {
    public InvalidEventException() { super("Invalid order event"); }
}
