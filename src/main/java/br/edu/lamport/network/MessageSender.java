package br.edu.lamport.network;

import br.edu.lamport.model.NetworkMessage;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Envia mensagens para os peers via TCP (uma conexao por destino).
 */
public class MessageSender implements AutoCloseable {

    private final Map<Integer, PrintWriter> writers = new ConcurrentHashMap<>();
    private final Map<Integer, PeerInfo> peers;

    public MessageSender(Map<Integer, PeerInfo> peers) {
        this.peers = peers;
    }

    public void connectAll() {
        for (PeerInfo peer : peers.values()) {
            connect(peer);
        }
    }

    private void connect(PeerInfo peer) {
        int attempts = 0;
        while (attempts < 30) {
            try {
                Socket socket = new Socket(peer.host(), peer.port());
                socket.setTcpNoDelay(true);
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                writers.put(peer.processId(), writer);
                System.out.println("[REDE] Conectado a " + peer);
                return;
            } catch (IOException e) {
                attempts++;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        System.err.println("[REDE] Falha ao conectar em " + peer);
    }

    public synchronized void sendTo(int processId, NetworkMessage message) {
        PrintWriter writer = writers.get(processId);
        if (writer == null) {
            PeerInfo peer = peers.get(processId);
            if (peer != null) {
                connect(peer);
                writer = writers.get(processId);
            }
        }
        if (writer != null) {
            writer.println(message.serialize());
        } else {
            System.err.println("[REDE] Sem conexao com P" + processId);
        }
    }

    public void broadcast(NetworkMessage message, int selfId) {
        for (Integer processId : peers.keySet()) {
            if (processId != selfId) {
                sendTo(processId, message);
            }
        }
    }

    @Override
    public void close() {
        for (PrintWriter writer : writers.values()) {
            writer.close();
        }
        writers.clear();
    }
}
