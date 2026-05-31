package br.edu.lamport;

import br.edu.lamport.core.LamportNode;
import br.edu.lamport.network.PeerInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Ponto de entrada do sistema de sincronizacao Lamport.
 *
 * Uso:
 *   java -jar lamport-algorithm.jar --id 1 --port 5001 --peers 2:192.168.1.10:5002,3:192.168.1.11:5003
 *
 * Comandos interativos:
 *   send <texto>  - envia mensagem com multicast totalmente ordenado
 *   event <texto> - executa evento local (incrementa relogio)
 *   state         - exibe relogio, fila e ACKs recebidos
 *   help          - lista comandos
 *   quit          - encerra o processo
 */
public class LamportApp {

    public static void main(String[] args) {
        try {
            Config config = Config.parse(args);
            run(config);
        } catch (Exception e) {
            System.err.println("Erro: " + e.getMessage());
            printUsage();
            System.exit(1);
        }
    }

    private static void run(Config config) throws IOException {
        LamportNode node = new LamportNode(config.processId(), config.port(), config.peers());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                node.close();
            } catch (IOException ignored) {
            }
        }));

        printBanner(config);
        readCommands(node);
    }

    private static void readCommands(LamportNode node) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Digite 'help' para ver os comandos disponiveis.");

        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
                node.close();
                break;
            }
            if (line.equalsIgnoreCase("help")) {
                printHelp();
                continue;
            }
            if (line.equalsIgnoreCase("state")) {
                node.printState();
                continue;
            }
            if (line.startsWith("send ")) {
                node.send(line.substring(5).trim());
                continue;
            }
            if (line.startsWith("event ")) {
                node.localEvent(line.substring(6).trim());
                continue;
            }

            System.out.println("Comando desconhecido. Digite 'help'.");
        }
    }

    private static void printBanner(Config config) {
        System.out.println("========================================");
        System.out.println("  Algoritmo de Lamport - SD");
        System.out.println("  Processo P" + config.processId() + " | porta " + config.port());
        System.out.println("========================================");
    }

    private static void printHelp() {
        System.out.println("Comandos:");
        System.out.println("  send <texto>   Envia mensagem (multicast totalmente ordenado)");
        System.out.println("  event <texto>  Evento interno local (Regra 1 do relogio)");
        System.out.println("  state          Mostra relogio, fila e timestamps recebidos");
        System.out.println("  help           Exibe esta ajuda");
        System.out.println("  quit           Encerra o processo");
    }

    private static void printUsage() {
        System.out.println();
        System.out.println("Uso:");
        System.out.println("  java -jar lamport-algorithm.jar --id <id> --port <porta> --peers <lista>");
        System.out.println();
        System.out.println("Exemplo (3 processos):");
        System.out.println("  Maquina 1: --id 1 --port 5001 --peers 2:192.168.0.2:5002,3:192.168.0.3:5003");
        System.out.println("  Maquina 2: --id 2 --port 5002 --peers 1:192.168.0.1:5001,3:192.168.0.3:5003");
        System.out.println("  Maquina 3: --id 3 --port 5003 --peers 1:192.168.0.1:5001,2:192.168.0.2:5002");
        System.out.println();
        System.out.println("Formato de peer: id:host:porta (separados por virgula)");
    }

    private record Config(int processId, int port, Map<Integer, PeerInfo> peers) {

        static Config parse(String[] args) {
            Integer id = null;
            Integer port = null;
            String peersRaw = null;

            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--id" -> id = Integer.parseInt(requireValue(args, ++i, "--id"));
                    case "--port" -> port = Integer.parseInt(requireValue(args, ++i, "--port"));
                    case "--peers" -> peersRaw = requireValue(args, ++i, "--peers");
                    default -> throw new IllegalArgumentException("Argumento desconhecido: " + args[i]);
                }
            }

            if (id == null || port == null || peersRaw == null || peersRaw.isBlank()) {
                throw new IllegalArgumentException("Parametros --id, --port e --peers sao obrigatorios.");
            }

            Map<Integer, PeerInfo> peers = new HashMap<>();
            for (String peerToken : peersRaw.split(",")) {
                PeerInfo peer = PeerInfo.parse(peerToken.trim());
                if (peer.processId() == id) {
                    throw new IllegalArgumentException("Peer nao pode ser o proprio processo: " + peer);
                }
                peers.put(peer.processId(), peer);
            }

            if (peers.isEmpty()) {
                throw new IllegalArgumentException("Informe ao menos um peer em --peers.");
            }

            return new Config(id, port, peers);
        }

        private static String requireValue(String[] args, int index, String flag) {
            if (index >= args.length) {
                throw new IllegalArgumentException("Valor ausente para " + flag);
            }
            return args[index];
        }
    }
}
