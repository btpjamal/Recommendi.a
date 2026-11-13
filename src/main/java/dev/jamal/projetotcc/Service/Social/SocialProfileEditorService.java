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
import java.util.Set;

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

    public void validar(Long id) {
        if (!participation.elegivel(id) || !participation.participa(id))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ative sua participação social para acessar o perfil.");
    }

    private SocialProfile perfil(Long id) {
        return profiles.findByUser_Id(id).filter(SocialProfile::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil indisponível."));
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
        var p = perfil(id);
        return new SocialProfileEditDTO(p.getBiografia(), p.getFotoVisibilidade(), p.getBiografiaVisibilidade(),
                p.getInteressesVisibilidade(), p.getHobbiesVisibilidade());
    }

    @Transactional
    public SocialProfileEditDTO salvar(Long id, SocialProfileEditDTO dto) {
        var p = bloquear(id);
        SocialTextValidation.rejectNul(dto.biografia());
        if (dto.biografia() != null && dto.biografia().length() > 500)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A biografia deve ter até 500 caracteres.");
        if (dto.fotoVisibilidade() == null || dto.biografiaVisibilidade() == null ||
                dto.interessesVisibilidade() == null || dto.hobbiesVisibilidade() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a visibilidade de cada campo.");
        p.setBiografia(dto.biografia() == null ? null : dto.biografia().strip());
        p.setFotoVisibilidade(dto.fotoVisibilidade());
        p.setBiografiaVisibilidade(dto.biografiaVisibilidade());
        p.setInteressesVisibilidade(dto.interessesVisibilidade());
        p.setHobbiesVisibilidade(dto.hobbiesVisibilidade());
        profiles.save(p);
        return buscar(id);
    }

    @Transactional
    public void salvarFoto(Long id, byte[] png) {
        bloquear(id).setFoto(png);
    }

    @Transactional
    public void removerFoto(Long id) {
        bloquear(id).setFoto(null);
    }

    @Transactional(readOnly = true)
    public byte[] foto(Long viewer, Long owner) {
        validar(viewer);
        if (blocks.bloqueados(viewer, owner)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        var p = perfil(owner);
        if (!SocialProfileService.maiorDeIdade(p.getUser()) || p.getFoto() == null || !hub.podeVer(p.getFotoVisibilidade(), viewer, owner))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return p.getFoto();
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
                double scale = Math.min(1, 256.0 / Math.max(width, height));
                var target = new BufferedImage(Math.max(1, (int)(width * scale)), Math.max(1, (int)(height * scale)), BufferedImage.TYPE_INT_RGB);
                var g = target.createGraphics();
                try {
                    g.setColor(Color.WHITE); g.fillRect(0, 0, target.getWidth(), target.getHeight());
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g.drawImage(source, 0, 0, target.getWidth(), target.getHeight(), null);
                } finally { g.dispose(); }
                var output = new ByteArrayOutputStream();
                ImageIO.write(target, "png", output);
                return output.toByteArray();
            } finally { reader.dispose(); }
        } catch (IOException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Imagem inválida. Use PNG ou JPEG de até 1 MB e 16 megapixels.");
        }
    }
}

