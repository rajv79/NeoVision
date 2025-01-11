package com.example.audioapp.utils;

public interface RecordStreamListener {
    void recordOfByte(byte[] data, int begin, int end);
}
