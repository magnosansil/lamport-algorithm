package br.edu.lamport.model;

public enum MessageType {
    MULTICAST,
    ACK,
    HEARTBEAT;

    public static MessageType from(String raw) {
        return MessageType.valueOf(raw.trim().toUpperCase());
    }
}
