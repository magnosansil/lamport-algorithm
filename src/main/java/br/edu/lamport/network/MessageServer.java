package br.edu.lamport.network;

import br.edu.lamport.model.NetworkMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Servidor TCP que aceita conexoes de outros processos.
 */
public class MessageServer implements AutoCloseable {

    private final int port;
    private final Consumer<NetworkMessage> messageHandler;
    private ServerSocket serverSocket;
    private Thread acceptThread;
    private volatile boolean running;

    public MessageServer(int port, Consumer<NetworkMessage> messageHandler) {
        this.port = port;
        this.messageHandler = messageHandler;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        acceptThread = new Thread(this::acceptLoop, "server-" + port);
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket client = serverSocket.accept();
                Thread handler = new Thread(() -> handleClient(client), "client-handler");
                handler.setDaemon(true);
                handler.start();
            } catch (IOException e) {
                if (running) {
                    System.err.println("[REDE] Erro ao aceitar conexao: " + e.getMessage());
                }
            }
        }
    }

    private void handleClient(Socket socket) {
        try (socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    NetworkMessage message = NetworkMessage.parse(line);
                    messageHandler.accept(message);
                } catch (RuntimeException e) {
                    System.err.println("[REDE] Mensagem invalida: " + line + " -> " + e.getMessage());
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("[REDE] Conexao encerrada: " + e.getMessage());
            }
        }
    }

    @Override
    public void close() throws IOException {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
    }
}
