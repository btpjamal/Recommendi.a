package dev.jamal.projetotcc.Service.Social;

import dev.jamal.projetotcc.Repository.SocialProfilePhotoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.social.thumbnail-backfill.enabled",
        havingValue = "true"
)
public class SocialThumbnailBackfillRunner
        implements ApplicationRunner {

    private static final int TAMANHO_LOTE = 20;

    private final SocialProfileEditorService editor;
    private final SocialProfilePhotoRepository photos;

    @Override
    public void run(ApplicationArguments args) {
        preencher();
    }

    Resultado preencher() {
        log.info("Iniciando preenchimento de miniaturas.");

        Long ultimoId = photos.ultimoIdPendente();
        Long cursor = null;
        long processadas = 0;
        long ignoradas = 0;
        long falhas = 0;

        while (ultimoId != null) {
            var lote = photos.buscarMiniaturasPendentes(cursor, ultimoId, TAMANHO_LOTE);
            if (lote.isEmpty()) {
                break;
            }

            for (long profileId : lote) {
                var foto = photos.buscarFotoPendente(profileId);
                if (foto.isEmpty()) {
                    ignoradas++;
                    log.info("Miniatura ignorada. profileId={} motivo=foto_ausente_ou_ja_preenchida", profileId);
                    continue;
                }

                byte[] miniatura;
                try {
                    miniatura = editor.gerarMiniatura(foto.get().getFoto());
                } catch (SocialProfileEditorService.ImagemInvalida ex) {
                    falhas++;
                    log.warn("Miniatura ignorada por imagem inválida. profileId={} motivo={}",
                            profileId, ex.getMessage());
                    continue;
                }

                if (photos.preencherMiniatura(profileId, miniatura, foto.get().getRevisao()) == 1) {
                    processadas++;
                } else {
                    ignoradas++;
                    log.info("Miniatura ignorada. profileId={} motivo=escrita_concorrente", profileId);
                }
            }

            cursor = lote.get(lote.size() - 1);
        }

        long pendentes = photos.contarMiniaturasPendentes();
        log.info("Preenchimento finalizado. processadas={} ignoradas={} falhas={} pendentes={}",
                processadas, ignoradas, falhas, pendentes);
        return new Resultado(processadas, ignoradas, falhas, pendentes);
    }

    record Resultado(long processadas, long ignoradas, long falhas, long pendentes) {}
}
