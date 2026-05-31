package br.edu.lamport.model;

public record PendingMessage(LamportTimestamp timestamp, String content, int senderId) {
}
