package br.com.fiap.rotaleve.routes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ProvedorGoogleTests {
    private MockRestServiceServer server;
    private ProvedorGoogle provedor;

    @BeforeEach
    void preparar() {
        var builder = RestClient.builder().baseUrl("https://routes.googleapis.com");
        server = MockRestServiceServer.bindTo(builder).build();
        provedor = new ProvedorGoogle(builder.build(), "chave-ficticia-de-teste");
    }

    @Test
    void consultaTransporteComAlternativasESomaSomenteTrechosAPe() {
        server.expect(requestTo("https://routes.googleapis.com/directions/v2:computeRoutes"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Goog-Api-Key", "chave-ficticia-de-teste"))
                .andExpect(jsonPath("$.travelMode").value("TRANSIT"))
                .andExpect(jsonPath("$.computeAlternativeRoutes").value(true))
                .andExpect(jsonPath("$.origin.address").value("Origem sintética"))
                .andExpect(jsonPath("$.destination.address").value("Destino sintético"))
                .andRespond(withSuccess("""
                    {"routes":[{"duration":"1800.9s","polyline":{"encodedPolyline":"polyline-teste"},
                    "legs":[{"steps":[{"travelMode":"WALK","distanceMeters":150},
                    {"travelMode":"TRANSIT","distanceMeters":4000},
                    {"travelMode":"WALK","distanceMeters":50}]}]}]}
                    """, MediaType.APPLICATION_JSON));

        var rotas = provedor.buscar("Origem sintética", "Destino sintético");

        assertThat(rotas).hasSize(1);
        var rota = rotas.get(0);
        assertThat(rota.caminhadaMetros()).isEqualTo(200);
        assertThat(rota.duracaoSegundos()).isEqualTo(1801);
        assertThat(rota.polyline()).isEqualTo("polyline-teste");
        assertThat(rota.fonte()).isEqualTo("GOOGLE_ROUTES");
        // Ausência de dados no provedor não equivale à confirmação de acessibilidade.
        assertThat(rota.temEscadas()).isNull();
        assertThat(rota.embarqueAcessivel()).isNull();
        assertThat(rota.temAssento()).isNull();
        assertThat(rota.temCobertura()).isNull();
        assertThat(rota.pontosCriticos()).isEmpty();
        server.verify();
    }

    @Test
    void provedorSemRotaRetornaListaVazia() {
        server.expect(anything()).andRespond(withSuccess("{\"routes\":[]}", MediaType.APPLICATION_JSON));
        assertThat(provedor.buscar("A", "B")).isEmpty();
        server.verify();
    }

    @Test
    void falhaExternaSeTornaBadGatewaySemExporCorpoDoProvedor() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.FORBIDDEN)
                .body("detalhe privado da conta Google").contentType(MediaType.TEXT_PLAIN));
        assertThatThrownBy(() -> provedor.buscar("A", "B"))
                .isInstanceOfSatisfying(ResponseStatusException.class, erro -> {
                    assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(erro.getReason()).doesNotContain("privado");
                });
        server.verify();
    }

    @Test
    void caminhadaSemDistanciaEhRejeitadaEmVezDeAssumirZero() {
        server.expect(anything()).andRespond(withSuccess("""
                {"routes":[{"duration":"300s","legs":[{"steps":[{"travelMode":"WALK"}]}]}]}
                """, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> provedor.buscar("A", "B"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        erro -> assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));
        server.verify();
    }

    @Test
    void chaveAusenteRetornaIndisponivelAntesDaRequisicao() {
        var semChave = new ProvedorGoogle(RestClient.create(), "");
        assertThatThrownBy(() -> semChave.buscar("A", "B"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        erro -> assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }
}
