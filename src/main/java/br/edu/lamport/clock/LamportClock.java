package br.edu.lamport.clock;

/**
 * Relogio logico de Lamport (Li) conforme as regras do algoritmo.
 */
public class LamportClock {

    private long value;

    public LamportClock() {
        this.value = 0;
    }

    public synchronized long get() {
        return value;
    }

    /** Regra 1: incremento antes de evento local ou envio. */
    public synchronized long tick() {
        return ++value;
    }

    /** Regra 3: atualizacao ao receber mensagem com timestamp t. */
    public synchronized long updateOnReceive(long receivedTimestamp) {
        value = Math.max(value, receivedTimestamp) + 1;
        return value;
    }

    @Override
    public synchronized String toString() {
        return Long.toString(value);
    }
}
