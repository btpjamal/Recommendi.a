package dev.jamal.projetotcc.Service.Social;

import dev.jamal.projetotcc.DTO.Social.SocialProfileEditDTO;
import dev.jamal.projetotcc.Entities.SocialProfile;
import dev.jamal.projetotcc.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.LocalDateTime;
import java.util.Set;
import javax.imageio.IIOImage;
import javax.imageio.ImageWriteParam;
import javax.imageio.stream.MemoryCacheImageOutputStream;

@Service
@RequiredArgsConstructor
public class SocialProfileEditorService {
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;
    private final SocialProfileRepository profiles;
    private final UserRepository users;
    private final SocialProfileService participation;
    private final SocialHubService hub;
    private final SocialUserBlockRepository blocks;
    private final SocialProfilePhotoRepository photos;

    private static final int FOTO_MAX_DIMENSAO = 768;
    private static final int MINIATURA_MAX_DIMENSAO = 128;

    private static final float FOTO_QUALIDADE = 0.85f;
    private static final float MINIATURA_QUALIDADE = 0.75f;

    public void validar(Long id) {
        if (!participation.elegivel(id) || !participation.participa(id))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ative sua participação social para acessar o perfil.");
    }

    private SocialProfile perfil(Long id) {
        return profiles.findByUser_Id(id).filter(SocialProfile::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil indisponível."));
    }

    private ResponseStatusException perfilIndisponivel() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil indisponível.");
    }

    private SocialProfile bloquear(Long id) {
        users.lockTokenVersion(id).orElseThrow();
        var p = profiles.findByUser_Id(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil indisponível."));
        // Re-read after acquiring the same lock used by social deactivation (including Open-in-View).
        entityManager.refresh(p);
        entityManager.refresh(p.getUser());
        validar(id);
        return p;
    }

    @Transactional(readOnly = true)
    public SocialProfileEditDTO buscar(Long id) {
        validar(id);
        return profiles.findEditableByUserId(id).orElseThrow(this::perfilIndisponivel);
    }

    @Transactional
    public SocialProfileEditDTO salvar(Long id, SocialProfileEditDTO dto) {
        // Serialize with deactivation, location settings and photo writes.
        users.lockTokenVersion(id).orElseThrow();
        if (!profiles.existsByUser_Id(id)) throw perfilIndisponivel();
        var atual = buscar(id);
        SocialTextValidation.rejectNul(dto.biografia());
        if (dto.biografia() != null && dto.biografia().length() > 500)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A biografia deve ter até 500 caracteres.");
        if (dto.fotoVisibilidade() == null || dto.biografiaVisibilidade() == null ||
                dto.interessesVisibilidade() == null || dto.hobbiesVisibilidade() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a visibilidade de cada campo.");
        var atualizado = new SocialProfileEditDTO(dto.biografia() == null ? null : dto.biografia().strip(),
                dto.fotoVisibilidade(), dto.biografiaVisibilidade(), dto.interessesVisibilidade(),
                dto.hobbiesVisibilidade());
        if (!atualizado.equals(atual) && profiles.updateTextSettings(id, atualizado.biografia(),
                atualizado.fotoVisibilidade(), atualizado.biografiaVisibilidade(), atualizado.interessesVisibilidade(),
                atualizado.hobbiesVisibilidade(), LocalDateTime.now()) != 1) {
            throw perfilIndisponivel();
        }
        return atualizado;
    }

    @Transactional
    public void salvarFoto(Long id, byte[] imagem) {

        var perfil = bloquear(id);

        String contentType = identificarFormato(imagem);
        if (contentType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de imagem inválido.");
        }

        byte[] miniatura;
        try {
            miniatura = gerarMiniatura(imagem);
        } catch (ImagemInvalida ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível gerar a miniatura.");
        }

        photos.salvarOuAtualizar(
                perfil.getId(),
                imagem,
                miniatura,
                contentType,
                LocalDateTime.now()
        );
    }

    @Transactional
    public void removerFoto(Long id) {

        var perfil = bloquear(id);

        photos.remover(perfil.getId());
    }

    private SocialProfile autorizarAcessoFoto(
            Long viewer,
            Long owner
    ) {

        validar(viewer);

        if (blocks.bloqueados(viewer, owner)) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND
            );
        }

        var p = perfil(owner);

        if (!SocialProfileService.maiorDeIdade(p.getUser())
                || !hub.podeVer(
                p.getFotoVisibilidade(),
                viewer,
                owner
        )) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND
            );
        }

        return p;
    }

    @Transactional(readOnly = true)
    public byte[] foto(Long viewer, Long owner) {

        var p = autorizarAcessoFoto(viewer, owner);

        return photos.findPhotoData(p.getId())
                .map(SocialProfilePhotoRepository.PhotoData::getFoto)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND
                        )
                );
    }

    @Transactional(readOnly = true)
    public byte[] miniatura(Long viewer, Long owner) {

        var p = autorizarAcessoFoto(viewer, owner);

        return photos.findThumbnail(p.getId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND
                        )
                );
    }

    // Decode only bounded raster images, then encode again to discard original metadata/content.
    public byte[] prepararFoto(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > 1024 * 1024)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envie uma imagem PNG ou JPEG de até 1 MB.");
        try (var stream = new MemoryCacheImageInputStream(file.getInputStream())) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new IOException();
            var reader = readers.next();
            try {
                if (!Set.of("png", "jpeg", "jpg").contains(reader.getFormatName().toLowerCase(java.util.Locale.ROOT)))
                    throw new IOException();
                reader.setInput(stream, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || (long) width * height > 16_000_000) throw new IOException();
                var source = reader.read(0);

                var target = redimensionar(
                        source,
                        FOTO_MAX_DIMENSAO
                );

                return comprimirJpeg(
                        target,
                        FOTO_QUALIDADE
                );
            } finally { reader.dispose(); }
        } catch (IOException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Imagem inválida. Use PNG ou JPEG de até 1 MB e 16 megapixels.");
        }
    }

    private byte[] comprimirJpeg(
            BufferedImage imagem,
            float qualidade
    ) throws IOException {

        var writers = ImageIO.getImageWritersByFormatName("jpeg");

        if (!writers.hasNext()) {
            throw new IOException("Codificador JPEG indisponível.");
        }

        var writer = writers.next();
        var output = new ByteArrayOutputStream();

        try (var stream = new MemoryCacheImageOutputStream(output)) {

            writer.setOutput(stream);

            var parametros = writer.getDefaultWriteParam();

            parametros.setCompressionMode(
                    ImageWriteParam.MODE_EXPLICIT
            );

            parametros.setCompressionQuality(qualidade);

            writer.write(
                    null,
                    new IIOImage(imagem, null, null),
                    parametros
            );

            stream.flush();

        } finally {
            writer.dispose();
        }

        return output.toByteArray();
    }

    private String identificarFormato(byte[] imagem) {

        if (imagem != null && imagem.length >= 3
                && (imagem[0] & 0xFF) == 0xFF
                && (imagem[1] & 0xFF) == 0xD8
                && (imagem[2] & 0xFF) == 0xFF) {

            return "image/jpeg";
        }

        if (imagem != null && imagem.length >= 8
                && java.util.HexFormat.of()
                .formatHex(imagem, 0, 8)
                .equals("89504e470d0a1a0a")) {

            return "image/png";
        }

        return null;
    }

    private BufferedImage redimensionar(
            BufferedImage source,
            int dimensaoMaxima
    ) {

        int width = source.getWidth();
        int height = source.getHeight();

        double scale = Math.min(
                1.0,
                (double) dimensaoMaxima / Math.max(width, height)
        );

        int novaLargura = Math.max(1, (int) (width * scale));
        int novaAltura = Math.max(1, (int) (height * scale));

        var target = new BufferedImage(
                novaLargura,
                novaAltura,
                BufferedImage.TYPE_INT_RGB
        );

        var g = target.createGraphics();

        try {

            g.setColor(Color.WHITE);

            g.fillRect(
                    0, 0,
                    novaLargura,
                    novaAltura
            );

            g.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );

            g.drawImage(
                    source,
                    0, 0,
                    novaLargura,
                    novaAltura,
                    null
            );

        } finally {
            g.dispose();
        }

        return target;
    }

    static final class ImagemInvalida extends RuntimeException {
        private ImagemInvalida(String motivo) {
            super(motivo);
        }
    }

    byte[] gerarMiniatura(byte[] imagem) {
        if (identificarFormato(imagem) == null) {
            throw new ImagemInvalida("assinatura_invalida");
        }

        var source = decodificarMiniatura(imagem);
        var miniatura = redimensionar(source, MINIATURA_MAX_DIMENSAO);
        try {
            return comprimirJpeg(miniatura, MINIATURA_QUALIDADE);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha no codificador JPEG de miniaturas.", ex);
        }
    }

    private BufferedImage decodificarMiniatura(byte[] imagem) {
        try (var stream = new MemoryCacheImageInputStream(new ByteArrayInputStream(imagem))) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                throw new ImagemInvalida("decodificacao_invalida");
            }

            var reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || (long) width * height > 16_000_000) {
                    throw new ImagemInvalida("dimensoes_invalidas");
                }

                var source = reader.read(0);
                if (source == null) {
                    throw new ImagemInvalida("decodificacao_invalida");
                }
                return source;
            } finally {
                reader.dispose();
            }
        } catch (IOException | IllegalArgumentException ex) {
            throw new ImagemInvalida("decodificacao_invalida");
        }
    }

    public record FotoCondicional(String versao, byte[] imagem) {}

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public FotoCondicional fotoCondicional(Long viewer, Long owner, String versaoCliente) {
        var perfil = autorizarAcessoFoto(viewer, owner);
        var dados = photos.findPhotoConditional(perfil.getId(), versaoCliente)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new FotoCondicional(dados.getVersao(), dados.getFoto());
    }

    public record MiniaturaCondicional(
            String versao,
            byte[] imagem
    ) {

        public boolean naoModificada() {
            return imagem == null;
        }
    }

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public MiniaturaCondicional miniaturaCondicional(
            Long viewer,
            Long owner,
            String versaoCliente
    ) {

        var perfil = autorizarAcessoFoto(
                viewer,
                owner
        );

        var dados = photos.findThumbnailConditional(
                perfil.getId(),
                versaoCliente
        ).orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND
                )
        );

        return new MiniaturaCondicional(
                dados.getVersao(),
                dados.getMiniatura()
        );
    }
}

