package br.com.fiap.rotaleve.service;

import br.com.fiap.rotaleve.exception.RegraNegocioException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Fotos comunitárias: nome aleatório, formato verificado e sem metadados da câmera. */
@Service
public class FotoService {
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private final Path diretorio;
    public FotoService(@Value("${rotaleve.fotos.diretorio}") String diretorio) {
        this.diretorio = Path.of(diretorio).toAbsolutePath().normalize();
    }
    public String salvar(MultipartFile arquivo) throws IOException {
        if (arquivo.isEmpty()) throw erro(HttpStatus.BAD_REQUEST, "Escolha uma foto JPEG ou PNG.");
        if (arquivo.getSize() > MAX_BYTES) throw erro(HttpStatus.PAYLOAD_TOO_LARGE, "A foto deve ter no máximo 5 MB.");
        String formato;
        BufferedImage imagem;
        try (var entrada = ImageIO.createImageInputStream(arquivo.getInputStream())) {
            var leitores = ImageIO.getImageReaders(entrada);
            if (!leitores.hasNext()) throw erro(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "O arquivo deve ser uma imagem JPEG ou PNG válida.");
            ImageReader leitor = leitores.next();
            try {
                formato = leitor.getFormatName().toLowerCase(Locale.ROOT);
                if (!formato.equals("jpeg") && !formato.equals("png"))
                    throw erro(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Use uma foto JPEG ou PNG.");
                leitor.setInput(entrada, true, true);
                int largura = leitor.getWidth(0), altura = leitor.getHeight(0);
                if (largura > 8192 || altura > 8192 || (long) largura * altura > 24_000_000)
                    throw erro(HttpStatus.BAD_REQUEST, "Reduza a foto para no máximo 24 megapixels e 8192 pixels por lado.");
                imagem = leitor.read(0);
            } finally { leitor.dispose(); }
        } catch (javax.imageio.IIOException | IllegalArgumentException exception) {
            throw erro(HttpStatus.BAD_REQUEST, "A foto está corrompida. Escolha outra imagem JPEG ou PNG.");
        }
        var normalizada = new ByteArrayOutputStream();
        if (!ImageIO.write(imagem, formato, normalizada)) throw new IOException("Formato de imagem indisponível.");
        if (normalizada.size() > MAX_BYTES) throw erro(HttpStatus.PAYLOAD_TOO_LARGE, "Reduza a resolução da foto para que ela ocupe até 5 MB.");
        Files.createDirectories(diretorio);
        String nome = UUID.randomUUID() + "." + formato;
        Files.write(diretorio.resolve(nome), normalizada.toByteArray(), StandardOpenOption.CREATE_NEW);
        return nome;
    }
    public byte[] ler(String nome) throws IOException {
        if (!nome.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpeg|png)"))
            throw erro(HttpStatus.NOT_FOUND, "Foto não encontrada.");
        Path arquivo = diretorio.resolve(nome).normalize();
        if (!arquivo.getParent().equals(diretorio) || !Files.isRegularFile(arquivo, LinkOption.NOFOLLOW_LINKS))
            throw erro(HttpStatus.NOT_FOUND, "Foto não encontrada.");
        return Files.readAllBytes(arquivo);
    }
    private RegraNegocioException erro(HttpStatus status, String mensagem) { return new RegraNegocioException(status, mensagem); }
}
