package br.edu.lamport.network;

public record PeerInfo(int processId, String host, int port) {

    @Override
    public String toString() {
        return "P" + processId + "@" + host + ":" + port;
    }

    public static PeerInfo parse(String raw) {
        String[] parts = raw.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException(
                    "Formato de peer invalido: " + raw + " (esperado id:host:porta)");
        }
        return new PeerInfo(
                Integer.parseInt(parts[0].trim()),
                parts[1].trim(),
                Integer.parseInt(parts[2].trim())
        );
    }
}
