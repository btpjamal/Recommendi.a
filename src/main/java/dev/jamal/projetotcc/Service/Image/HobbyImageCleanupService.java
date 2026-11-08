package dev.jamal.projetotcc.Service.Image;

import dev.jamal.projetotcc.Enum.HobbyImageStatus;
import dev.jamal.projetotcc.Repository.HobbyImageUploadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class HobbyImageCleanupService {

    private final HobbyImageUploadRepository uploadRepository;
    private final HobbyImageStatusService statusService;
    private final HobbyImageStorageService storageService;
    @Value("${app.imagens.exclusao.reserva-expiracao-minutos:30}")
    private long reservaExpiracaoMinutos;

    public void limparImagensPendentes() {

        recuperarReservasExpiradas();

        var imagens = uploadRepository.findByStatus(
                HobbyImageStatus.AGUARDANDO_EXCLUSAO
        );

        for (var imagem : imagens) {
            excluirImagemPendente(imagem.getPublicId());
        }
    }

    public void excluirImagemPendente(String publicId) {

        String token = null;

        try {
            token = statusService.reservarParaExclusao(publicId);

            if (token == null) {
                log.debug(
                        "Imagem não reservada para exclusão: {}",
                        publicId
                );
                return;
            }

            storageService.excluirImagem(publicId);

            statusService.marcarComoSubstituida(publicId, token);

            log.info(
                    "Imagem antiga removida com sucesso: {}",
                    publicId
            );

        } catch (Exception erro) {

            if (token != null) {
                try {
                    statusService.liberarReservaParaNovaTentativa(
                            publicId,
                            token
                    );
                } catch (Exception erroLiberacao) {
                    erro.addSuppressed(erroLiberacao);
                }
            }

            log.error(
                    "Falha ao excluir imagem: {}",
                    publicId,
                    erro
            );
        }
    }

    public void recuperarReservasExpiradas() {

        if (reservaExpiracaoMinutos <= 0) {
            throw new IllegalStateException(
                    "O prazo de expiração das reservas deve ser positivo."
            );
        }

        Instant limite = Instant.now()
                .minus(reservaExpiracaoMinutos, ChronoUnit.MINUTES);

        var publicIds = uploadRepository
                .buscarPublicIdsComReservaExpirada(
                        HobbyImageStatus.EXCLUINDO,
                        limite
                );

        for (String publicId : publicIds) {
            try {
                boolean recuperada =
                        statusService.recuperarReservaExpirada(
                                publicId,
                                limite
                        );

                if (recuperada) {
                    log.info(
                            "Reserva expirada liberada para nova tentativa: {}",
                            publicId
                    );
                }

            } catch (Exception erro) {
                log.error(
                        "Falha ao recuperar reserva expirada: {}",
                        publicId,
                        erro
                );
            }
        }
    }
}