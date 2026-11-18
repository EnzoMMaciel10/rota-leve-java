package br.com.fiap.rotaleve.controller;

import br.com.fiap.rotaleve.service.FotoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController @RequestMapping("/api/v1/arquivos/fotos") @RequiredArgsConstructor
public class FotoController {
    private final FotoService fotos;
    public record FotoResponse(String url) { }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Enviar foto comunitária JPEG/PNG de até 5 MB; metadados são removidos")
    public ResponseEntity<FotoResponse> enviar(@RequestPart("arquivo") MultipartFile arquivo) throws IOException {
        String nome = fotos.salvar(arquivo);
        URI url = ServletUriComponentsBuilder.fromCurrentRequest().pathSegment(nome).build().toUri();
        return ResponseEntity.created(url).body(new FotoResponse(url.toString()));
    }
    @GetMapping("/{nome}")
    @Operation(summary = "Visualizar foto comunitária pelo identificador aleatório")
    public ResponseEntity<byte[]> visualizar(@PathVariable String nome) throws IOException {
        byte[] dados = fotos.ler(nome);
        MediaType tipo = nome.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(tipo).contentLength(dados.length)
            .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nome + "\"").body(dados);
    }
}
