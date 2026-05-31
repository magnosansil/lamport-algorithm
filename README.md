# Algoritmo de Lamport — Sistemas Distribuídos

Implementação em Java do **Algoritmo de Lamport** (Relógios Lógicos) com **multicast totalmente ordenado**, permitindo sincronização lógica entre processos em computadores diferentes via TCP.

## Requisitos

- **Java 17** ou superior
- **Maven 3.6+** (para compilar)
- Dois computadores na **mesma rede** (Wi-Fi ou cabo), ou teste local na mesma máquina

Verifique o Java:

```powershell
java -version
mvn -version
```

## Compilação

Na pasta do projeto:

```powershell
mvn package
```

O executável será gerado em:

```
target/lamport-algorithm-1.0.0.jar
```

Copie esse arquivo `.jar` para os dois computadores (ou compile em cada um).

---

## Rodando com 2 computadores

Este projeto foi pensado para funcionar com **2 processos** (`P1` e `P2`), um em cada máquina.

### Passo 1 — Descobrir os IPs

Em **cada computador**, abra o terminal e execute:

**Windows:**
```powershell
ipconfig
```

**Linux/macOS:**
```bash
ip addr
# ou
ifconfig
```

Anote o endereço IPv4 de cada máquina na rede local. Exemplo:

| Máquina      | IP            | Processo | Porta |
|--------------|---------------|----------|-------|
| Computador A | `192.168.0.10` | P1       | 5001  |
| Computador B | `192.168.0.15` | P2       | 5002  |

> Use o IP da interface conectada à rede (Wi-Fi ou Ethernet), não `127.0.0.1`.

### Passo 2 — Liberar o firewall

Cada computador precisa **aceitar conexões de entrada** na porta que vai usar:

- Computador A: porta **5001**
- Computador B: porta **5002**

**Windows (PowerShell como administrador):**

```powershell
# No Computador A (P1)
New-NetFirewallRule -DisplayName "Lamport P1" -Direction Inbound -Protocol TCP -LocalPort 5001 -Action Allow

# No Computador B (P2)
New-NetFirewallRule -DisplayName "Lamport P2" -Direction Inbound -Protocol TCP -LocalPort 5002 -Action Allow
```

### Passo 3 — Iniciar os processos

**Ordem recomendada:** inicie primeiro o processo que os outros vão contatar. Na prática, pode iniciar A e depois B (aguardando ~2 segundos entre eles).

**No Computador A (P1):**

```powershell
java -jar lamport-algorithm-1.0.0.jar --id 1 --port 5001 --peers 2:192.168.0.15:5002
```

**No Computador B (P2):**

```powershell
java -jar lamport-algorithm-1.0.0.jar --id 2 --port 5002 --peers 1:192.168.0.10:5001
```

Substitua os IPs pelos valores reais das suas máquinas.

Se tudo estiver correto, você verá mensagens como:

```
[REDE] Conectado a P2@192.168.0.15:5002
[P1] Iniciado na porta 5001
[P1] Processos no grupo: [1, 2]
```

### Passo 4 — Testar a sincronização

Em cada terminal, use os comandos abaixo.

**No P1:**
```
send Mensagem do computador A
```

**No P2:**
```
send Mensagem do computador B
```

**Em ambos:**
```
state
```

Quando a entrega ordenada ocorrer, ambos exibirão as mensagens **na mesma ordem**, determinada pelos relógios lógicos de Lamport:

```
>>> [P1] ENTREGA ORDENADA (1, P1) | remetente=P1 | conteudo="Mensagem do computador A" | relogio atual=4
>>> [P2] ENTREGA ORDENADA (2, P2) | remetente=P2 | conteudo="Mensagem do computador B" | relogio atual=5
```

A ordem depende dos timestamps `(relógio, id do processo)`, não do horário real do relógio da máquina.

---

## Parâmetros de linha de comando

```
java -jar lamport-algorithm-1.0.0.jar --id <id> --port <porta> --peers <lista>
```

| Parâmetro  | Descrição |
|------------|-----------|
| `--id`     | Identificador deste processo (`1` ou `2`) |
| `--port`   | Porta TCP que **este** computador vai escutar |
| `--peers`  | Lista de outros processos no formato `id:ip:porta`, separados por vírgula |

**Formato de peer:** `id:host:porta`

Exemplo com 2 máquinas:
```
--peers 2:192.168.0.15:5002
```

---

## Comandos interativos

| Comando          | Descrição |
|------------------|-----------|
| `send <texto>`   | Envia mensagem com multicast totalmente ordenado |
| `event <texto>`  | Executa evento local (incrementa o relógio — Regra 1) |
| `state`          | Exibe relógio Lamport, fila pendente e timestamps recebidos |
| `help`           | Lista os comandos |
| `quit`           | Encerra o processo |

---

## Teste local (mesma máquina, 2 processos)

Para testar antes de usar dois computadores físicos:

```powershell
scripts\run-local-2.bat
```

Isso abre duas janelas de terminal: P1 na porta 5001 e P2 na porta 5002, ambos em `127.0.0.1`.

---

## Como funciona (resumo)

### Relógio lógico de Lamport

Cada processo mantém um contador `L` iniciando em 0:

1. **Evento local ou envio:** `L = L + 1`
2. **Envio de mensagem:** anexa o timestamp `(L, id do processo)` na mensagem
3. **Recebimento:** `L = max(L, timestamp_recebido) + 1`

### Ordem total

Dois eventos com o mesmo relógio lógico são desempatados pelo ID do processo:

```
(L, P_a) < (L, P_b)  se  P_a < P_b
```

### Multicast totalmente ordenado

- Toda mensagem enviada entra em uma **fila local** ordenada por `(relógio, processo)`
- Ao receber uma mensagem, o processo envia **ACK** para o outro
- Uma mensagem só é **entregue** quando o outro processo já enviou msg/ACK com relógio **maior** que o dela — garantindo que nenhuma mensagem "atrasada" ainda vai chegar

Isso garante que **P1 e P2 processam todas as mensagens exatamente na mesma ordem**, sem depender de relógios físicos sincronizados.

---

## Solução de problemas

| Problema | Possível causa | Solução |
|----------|----------------|---------|
| `Falha ao conectar` | IP ou porta errados | Confira `ipconfig` e os parâmetros `--peers` |
| `Falha ao conectar` | Firewall bloqueando | Libere a porta TCP no Windows Defender |
| Processos não se conectam | Redes diferentes | Ambos devem estar na mesma rede Wi-Fi/LAN |
| Mensagens não entregam | Só um processo rodando | Os dois (`P1` e `P2`) precisam estar ativos |
| `Parametros obrigatorios` | Argumentos faltando | Passe `--id`, `--port` e `--peers` |

**Teste de conectividade** (do Computador A para o B):

```powershell
Test-NetConnection -ComputerName 192.168.0.15 -Port 5002
```

Se `TcpTestSucceeded` for `False`, o problema é rede ou firewall.

---

## Estrutura do projeto

```
lamport-algorithm/
├── pom.xml
├── README.md
├── scripts/
│   ├── run-local-2.bat      # Teste local com 2 processos
│   └── run-local-3.bat      # Teste local com 3 processos (opcional)
└── src/main/java/br/edu/lamport/
    ├── LamportApp.java           # Entrada principal (CLI)
    ├── clock/LamportClock.java   # Relógio lógico
    ├── core/LamportNode.java     # Lógica do processo e entrega ordenada
    ├── model/                    # Timestamps e mensagens
    └── network/                  # Comunicação TCP
```

