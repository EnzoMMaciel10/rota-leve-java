package br.com.fiap.rotaleve.messaging;

public interface EntregaEvento {
    void entregar(EventoPonto evento) throws Exception;
}
