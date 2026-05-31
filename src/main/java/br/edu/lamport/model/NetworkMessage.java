package br.edu.lamport.model;

import java.util.Objects;

/**
 * Formato de linha: TIPO|processoOrigem|relogio|timestampMsg|processoMsg|conteudo
 * Campos timestampMsg/processoMsg/conteudo variam conforme o tipo.
 */
public record NetworkMessage(
        MessageType type,
        int senderId,
        long senderClock,
        LamportTimestamp messageTimestamp,
        String content
) {

    public String serialize() {
        String timestampPart = messageTimestamp != null ? messageTimestamp.toString() : "-";
        String contentPart = content != null ? content : "";
        return type.name() + "|" + senderId + "|" + senderClock + "|" + timestampPart + "|" + contentPart;
    }

    public static NetworkMessage parse(String line) {
        String[] parts = line.split("\\|", 5);
        if (parts.length < 4) {
            throw new IllegalArgumentException("Linha invalida: " + line);
        }

        MessageType type = MessageType.from(parts[0]);
        int senderId = Integer.parseInt(parts[1].trim());
        long senderClock = Long.parseLong(parts[2].trim());

        LamportTimestamp messageTimestamp = "-".equals(parts[3].trim())
                ? null
                : LamportTimestamp.parse(parts[3].trim());

        String content = parts.length == 5 ? parts[4] : "";

        return new NetworkMessage(type, senderId, senderClock, messageTimestamp, content);
    }

    public static NetworkMessage multicast(int senderId, long senderClock, LamportTimestamp timestamp, String content) {
        return new NetworkMessage(MessageType.MULTICAST, senderId, senderClock, timestamp, content);
    }

    public static NetworkMessage ack(int senderId, long senderClock, LamportTimestamp originalTimestamp) {
        return new NetworkMessage(MessageType.ACK, senderId, senderClock, originalTimestamp, "");
    }

    public static NetworkMessage heartbeat(int senderId, long senderClock) {
        return new NetworkMessage(MessageType.HEARTBEAT, senderId, senderClock, null, "");
    }

    @Override
    public String toString() {
        return switch (type) {
            case MULTICAST -> "MULTICAST de P" + senderId + " " + messageTimestamp + " relogio=" + senderClock
                    + " conteudo='" + content + "'";
            case ACK -> "ACK de P" + senderId + " relogio=" + senderClock + " ref=" + messageTimestamp;
            case HEARTBEAT -> "HEARTBEAT de P" + senderId + " relogio=" + senderClock;
        };
    }
}
