package br.com.fiap.rotaleve.routes;

import java.util.List;
import br.com.fiap.rotaleve.routes.RotasDtos.Candidata;

public interface ProvedorRotas {
    List<Candidata> buscar(String origem, String destino);
    String nome();
}
