package dev.jamal.projetotcc.DTO.Social;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

public record CommunityPostDTO(Long id, Long communityId, Long autorId, String autorNome,
    String conteudo, boolean possuiImagem, Instant criadoEm, Instant editadoEm, Long versao, boolean propria,
    @JsonInclude(JsonInclude.Include.NON_NULL) CommunityInteractionDTO interacoes) {

    public CommunityPostDTO(Long id, Long communityId, Long autorId, String autorNome,
            String conteudo, boolean possuiImagem, Instant criadoEm, Instant editadoEm, Long versao, boolean propria) {
        this(id, communityId, autorId, autorNome, conteudo, possuiImagem, criadoEm, editadoEm, versao, propria, null);
    }

    public CommunityPostDTO comInteracoes(CommunityInteractionDTO resumo) {
        return new CommunityPostDTO(id, communityId, autorId, autorNome, conteudo, possuiImagem,
                criadoEm, editadoEm, versao, propria, resumo);
    }
}
