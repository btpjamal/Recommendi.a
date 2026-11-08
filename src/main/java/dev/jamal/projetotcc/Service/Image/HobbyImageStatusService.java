package dev.jamal.projetotcc.Service.Image;

import dev.jamal.projetotcc.Entities.HobbyImageUpload;
import dev.jamal.projetotcc.Enum.HobbyImageStatus;
import dev.jamal.projetotcc.Repository.HobbyImageUploadRepository;
import dev.jamal.projetotcc.Repository.HobbyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HobbyImageStatusService {

    private final HobbyImageUploadRepository uploadRepository;
    private final HobbyRepository hobbyRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarComoSubstituida(String publicId, String token) {

        HobbyImageUpload imagem = uploadRepository
                .findByPublicIdForUpdate(publicId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Imagem anterior não encontrada."
                        )
                );

        if (imagem.getStatus() != HobbyImageStatus.EXCLUINDO
                || token == null
                || !token.equals(imagem.getExclusaoToken())) {

            throw new IllegalStateException(
                    "A reserva de exclusão não pertence a esta tentativa."
            );
        }

        if (hobbyRepository.existsByImagemUrl(imagem.getImagemUrl())) {
            throw new IllegalStateException(
                    "A imagem ainda está sendo utilizada oficialmente por um hobby."
            );
        }

        imagem.setStatus(HobbyImageStatus.SUBSTITUIDA);
        imagem.setExclusaoIniciadaEm(null);
        imagem.setExclusaoToken(null);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public boolean imagemEstaEmUso(String publicId) {

        HobbyImageUpload imagem = uploadRepository
                .findByPublicId(publicId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Imagem não encontrada."
                        )
                );

        return hobbyRepository.existsByImagemUrl(
                imagem.getImagemUrl()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String reservarParaExclusao(String publicId) {

        HobbyImageUpload imagem = uploadRepository
                .findByPublicIdForUpdate(publicId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Imagem não encontrada."
                        )
                );

        if (imagem.getStatus() != HobbyImageStatus.AGUARDANDO_EXCLUSAO) {
            return null;
        }

        if (hobbyRepository.existsByImagemUrl(imagem.getImagemUrl())) {
            return null;
        }

        String token = UUID.randomUUID().toString();

        imagem.setStatus(HobbyImageStatus.EXCLUINDO);
        imagem.setExclusaoIniciadaEm(Instant.now());
        imagem.setExclusaoToken(token);

        return token;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void liberarReservaParaNovaTentativa(String publicId, String token) {

        HobbyImageUpload imagem = uploadRepository
                .findByPublicIdForUpdate(publicId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Imagem não encontrada."
                        )
                );

        if (imagem.getStatus() != HobbyImageStatus.EXCLUINDO
                || token == null
                || !token.equals(imagem.getExclusaoToken())) {
            return;
        }

        imagem.setStatus(HobbyImageStatus.AGUARDANDO_EXCLUSAO);
        imagem.setExclusaoIniciadaEm(null);
        imagem.setExclusaoToken(null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean recuperarReservaExpirada(
            String publicId,
            Instant limite
    ) {

        if (limite == null) {
            throw new IllegalArgumentException(
                    "Informe o limite para recuperação."
            );
        }

        HobbyImageUpload imagem = uploadRepository
                .findByPublicIdForUpdate(publicId)
                .orElse(null);

        if (imagem == null
                || imagem.getStatus() != HobbyImageStatus.EXCLUINDO) {
            return false;
        }

        Instant inicio = imagem.getExclusaoIniciadaEm();

        if (inicio != null && !inicio.isBefore(limite)) {
            return false;
        }

        imagem.setStatus(HobbyImageStatus.AGUARDANDO_EXCLUSAO);
        imagem.setExclusaoIniciadaEm(null);
        imagem.setExclusaoToken(null);

        return true;
    }
}