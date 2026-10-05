package com.example.structuredoutput.model;

@FunctionalInterface
public interface ModelClient {

    String generate(String prompt);
}
