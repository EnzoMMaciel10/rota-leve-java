package br.com.fiap.rotaleve.model;

/** Vocabulário compartilhado pelo domínio e pelos contratos da API. */
public final class Tipos {
    private Tipos() { }
    public enum Papel { CIDADAO, GESTOR }
    public enum TipoMobilidade { GESTANTE, CARRINHO, IDOSO, CADEIRANTE, TEMPORARIA }
    public enum TipoPonto { ELEVADOR, ESCADA, CALCADA, RAMPA, PONTO_ONIBUS }
    public enum EstadoPonto { OPERACIONAL, PROBLEMA, DESCONHECIDO }
    public enum TipoProblema { ELEVADOR_QUEBRADO, BURACO, DEGRAU, OBSTACULO, SEM_COBERTURA }
    public enum StatusRelato { ATIVO, RESOLVIDO, EXPIRADO }
}
