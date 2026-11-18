package br.com.fiap.rotaleve.service;

/** Publicado dentro da transação; o outbox persiste o evento antes do commit. */
public record PontoAlteradoEvent(Long pontoId, String mensagem) { }
