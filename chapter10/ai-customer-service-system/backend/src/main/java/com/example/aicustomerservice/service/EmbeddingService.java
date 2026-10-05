package com.example.aicustomerservice.service;

public interface EmbeddingService {

    String modelName();

    int dimensions();

    float[] embed(String text);

    byte[] embedAsBytes(String text);
}
