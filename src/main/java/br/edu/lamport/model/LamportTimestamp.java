package br.edu.lamport.model;

import java.util.Objects;

/**
 * Ordem total: (L(a), P_a) < (L(b), P_b).
 */
public record LamportTimestamp(long clock, int processId) implements Comparable<LamportTimestamp> {

    @Override
    public int compareTo(LamportTimestamp other) {
        int clockCompare = Long.compare(this.clock, other.clock);
        if (clockCompare != 0) {
            return clockCompare;
        }
        return Integer.compare(this.processId, other.processId);
    }

    @Override
    public String toString() {
        return "(" + clock + ", P" + processId + ")";
    }

    public static LamportTimestamp parse(String raw) {
        String trimmed = raw.trim();
        if (!trimmed.startsWith("(") || !trimmed.endsWith(")")) {
            throw new IllegalArgumentException("Timestamp invalido: " + raw);
        }

        String inner = trimmed.substring(1, trimmed.length() - 1);
        String[] parts = inner.split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Timestamp invalido: " + raw);
        }

        long clock = Long.parseLong(parts[0].trim());
        String processPart = parts[1].trim();
        int processId = processPart.startsWith("P")
                ? Integer.parseInt(processPart.substring(1))
                : Integer.parseInt(processPart);

        return new LamportTimestamp(clock, processId);
    }
}
