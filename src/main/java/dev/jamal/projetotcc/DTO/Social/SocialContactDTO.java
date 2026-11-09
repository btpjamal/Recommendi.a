package dev.jamal.projetotcc.DTO.Social;

import dev.jamal.projetotcc.Enum.SocialContactStatus;
import java.time.Instant;

public record SocialContactDTO(
        Long id, SocialContactStatus status, String direcao,
        Instant solicitadaEm, Instant atualizadaEm, Instant novaSolicitacaoApos,
        String apresentacao, Long outroUsuarioId, SocialProfileDTO outraPessoa, boolean disponivel
) {}
