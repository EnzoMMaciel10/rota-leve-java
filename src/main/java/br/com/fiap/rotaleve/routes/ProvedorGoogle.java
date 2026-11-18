package br.com.fiap.rotaleve.routes;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
@ConditionalOnProperty(name="rotaleve.routes.provider", havingValue="google")
public class ProvedorGoogle implements ProvedorRotas {
    private final RestClient client;
    private final String apiKey;
    @Autowired
    public ProvedorGoogle(@Value("${rotaleve.google.api-key:}") String apiKey) {
        this(criarCliente(), apiKey);
    }
    ProvedorGoogle(RestClient client, String apiKey) {
        this.client = client;
        this.apiKey = apiKey;
    }
    private static RestClient criarCliente() {
        var http = java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(15));
        return RestClient.builder().baseUrl("https://routes.googleapis.com")
            .requestFactory(factory).build();
    }
    @Override public String nome() { return "GOOGLE_ROUTES"; }
    @Override public List<RotasDtos.Candidata> buscar(String origem, String destino) {
        if (apiKey.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Chave Google não configurada");
        try {
            JsonNode response = client.post().uri("/directions/v2:computeRoutes")
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", "routes.duration,routes.legs.steps.travelMode,routes.legs.steps.distanceMeters,routes.polyline.encodedPolyline")
                .body(Map.of("origin", Map.of("address", origem), "destination", Map.of("address", destino),
                    "travelMode", "TRANSIT", "computeAlternativeRoutes", true, "languageCode", "pt-BR", "regionCode", "BR"))
                .retrieve().body(JsonNode.class);
            List<RotasDtos.Candidata> result = new ArrayList<>();
            if (response == null) throw new IllegalArgumentException("Resposta vazia");
            for (JsonNode route : response.path("routes")) {
                String duration = route.path("duration").asText();
                if (duration.isBlank() || !route.path("legs").isArray() || route.path("legs").isEmpty())
                    throw new IllegalArgumentException("Rota incompleta");
                int seconds = (int) Math.ceil(Double.parseDouble(duration.replace("s", "")));
                int walking = 0;
                for (JsonNode leg : route.path("legs")) {
                    if (!leg.path("steps").isArray() || leg.path("steps").isEmpty()) throw new IllegalArgumentException("Trechos ausentes");
                    for (JsonNode step : leg.path("steps")) {
                        if (step.path("travelMode").asText().equals("WALK")) {
                            if (!step.has("distanceMeters")) throw new IllegalArgumentException("Distância desconhecida");
                            walking += step.get("distanceMeters").asInt();
                        }
                    }
                }
                // Google fornece candidatos e caminhada, sem comprovar escadas, elevadores, assentos ou embarque.
                // Não associamos um equipamento apenas por proximidade: seria uma garantia sem evidência.
                result.add(new RotasDtos.Candidata(UUID.randomUUID().toString(), origem, destino,
                    "Transporte público · alternativa " + (result.size()+1), nome(), seconds, walking,
                    null, null, null, null, List.of(), route.path("polyline").path("encodedPolyline").asText(null)));
            }
            return List.copyOf(result);
        } catch (ResponseStatusException ex) { throw ex; }
        catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível consultar rotas no Google; tente novamente");
        }
    }
}
