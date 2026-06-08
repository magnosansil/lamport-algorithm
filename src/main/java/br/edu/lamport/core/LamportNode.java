package br.edu.lamport.core;

import br.edu.lamport.clock.LamportClock;
import br.edu.lamport.model.LamportTimestamp;
import br.edu.lamport.model.NetworkMessage;
import br.edu.lamport.model.PendingMessage;
import br.edu.lamport.network.MessageSender;
import br.edu.lamport.network.MessageServer;
import br.edu.lamport.network.PeerInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Processo distribuido com relogio de Lamport e multicast totalmente ordenado.
 */
public class LamportNode implements AutoCloseable {

    private final int processId;
    private final Set<Integer> allProcessIds;
    private final LamportClock clock;
    private final MessageSender sender;
    private final MessageServer server;

    private final List<PendingMessage> pendingQueue = new ArrayList<>();
    private final Map<Integer, Long> lastSeenClockFromProcess = new HashMap<>();

    public LamportNode(int processId, int port, Map<Integer, PeerInfo> peers) throws IOException {
        this.processId = processId;
        this.allProcessIds = new TreeSet<>();
        this.allProcessIds.add(processId);
        this.allProcessIds.addAll(peers.keySet());
        this.clock = new LamportClock();
        this.sender = new MessageSender(peers);

        for (Integer id : allProcessIds) {
            lastSeenClockFromProcess.put(id, 0L);
        }

        this.server = new MessageServer(port, this::handleIncoming);
        this.server.start();

        log("Servidor escutando na porta " + port);
        log("Processos no grupo: " + allProcessIds);
        log("Conectando ao(s) peer(s) em segundo plano...");

        this.sender.connectAllAsync();
    }

    public void printConnectionStatus() {
        for (Integer peerId : allProcessIds) {
            if (peerId == processId) {
                continue;
            }
            String status = sender.isConnected(peerId) ? "conectado" : "aguardando/desconectado";
            log("P" + peerId + ": " + status);
        }
    }

    /** Envia mensagem de aplicacao com multicast totalmente ordenado. */
    public synchronized void send(String content) {
        long localClock = clock.tick();
        LamportTimestamp timestamp = new LamportTimestamp(localClock, processId);

        PendingMessage pending = new PendingMessage(timestamp, content, processId);
        insertPending(pending);

        NetworkMessage message = NetworkMessage.multicast(processId, localClock, timestamp, content);
        sender.broadcast(message, processId);

        log("ENVIOU " + timestamp + " -> \"" + content + "\" (relogio local=" + clock.get() + ")");
        tryDeliver();
    }

    /** Evento interno local (demonstra Regra 1). */
    public synchronized void localEvent(String description) {
        long localClock = clock.tick();
        log("EVENTO LOCAL [" + description + "] relogio=" + localClock);
    }

    public synchronized void printState() {
        System.out.println("[P" + processId + "] Relogio Lamport: " + clock.get());
        System.out.println("[P" + processId + "] Ultimos relogios vistos: " + lastSeenClockFromProcess);
        System.out.println("[P" + processId + "] Fila pendente (" + pendingQueue.size() + "):");
        for (PendingMessage pending : pendingQueue) {
            System.out.println("  - " + pending.timestamp() + " de P" + pending.senderId()
                    + ": \"" + pending.content() + "\"");
        }
    }

    private void handleIncoming(NetworkMessage message) {
        synchronized (this) {
            switch (message.type()) {
                case MULTICAST -> onMulticast(message);
                case ACK -> onAck(message);
                case HEARTBEAT -> onHeartbeat(message);
            }
        }
    }

    private void onMulticast(NetworkMessage message) {
        LamportTimestamp msgTimestamp = message.messageTimestamp();
        long updatedClock = clock.updateOnReceive(msgTimestamp.clock());

        updateLastSeen(message.senderId(), message.senderClock());

        PendingMessage pending = new PendingMessage(msgTimestamp, message.content(), message.senderId());
        insertPending(pending);

        log("RECEBEU MULTICAST " + msgTimestamp + " de P" + message.senderId()
                + " (relogio local=" + updatedClock + ")");

        NetworkMessage ack = NetworkMessage.ack(processId, clock.get(), msgTimestamp);
        sender.broadcast(ack, processId);

        tryDeliver();
    }

    private void onAck(NetworkMessage message) {
        long updatedClock = clock.updateOnReceive(message.senderClock());
        updateLastSeen(message.senderId(), message.senderClock());

        log("RECEBEU ACK de P" + message.senderId() + " ref=" + message.messageTimestamp()
                + " (relogio local=" + updatedClock + ")");

        tryDeliver();
    }

    private void onHeartbeat(NetworkMessage message) {
        clock.updateOnReceive(message.senderClock());
        updateLastSeen(message.senderId(), message.senderClock());
    }

    private void updateLastSeen(int fromProcess, long seenClock) {
        lastSeenClockFromProcess.merge(fromProcess, seenClock, Math::max);
    }

    private void insertPending(PendingMessage pending) {
        pendingQueue.removeIf(p -> p.timestamp().equals(pending.timestamp()));
        pendingQueue.add(pending);
        pendingQueue.sort(Comparator.comparing(PendingMessage::timestamp));
    }

    /**
     * Entrega mensagens quando:
     * 1. Estao no topo da fila (menor timestamp total).
     * 2. Todos os OUTROS processos enviaram msg/ACK com relogio > timestamp da mensagem.
     */
    private void tryDeliver() {
        while (!pendingQueue.isEmpty()) {
            PendingMessage top = pendingQueue.get(0);
            if (!canDeliver(top)) {
                break;
            }
            pendingQueue.remove(0);
            deliver(top);
        }
    }

    private boolean canDeliver(PendingMessage message) {
        long messageClock = message.timestamp().clock();

        for (Integer otherId : allProcessIds) {
            if (otherId == processId) {
                continue;
            }
            long lastSeen = lastSeenClockFromProcess.getOrDefault(otherId, 0L);
            if (lastSeen <= messageClock) {
                return false;
            }
        }
        return true;
    }

    private void deliver(PendingMessage message) {
        System.out.println();
        System.out.println(">>> [P" + processId + "] ENTREGA ORDENADA " + message.timestamp()
                + " | remetente=P" + message.senderId()
                + " | conteudo=\"" + message.content() + "\""
                + " | relogio atual=" + clock.get());
        System.out.println();
    }

    private void log(String text) {
        System.out.println("[P" + processId + "] " + text);
        System.out.flush();
    }

    @Override
    public void close() throws IOException {
        server.close();
        sender.close();
    }
}
