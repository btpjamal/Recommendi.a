package dev.jamal.projetotcc.DTO.Social;
import java.time.Instant;
public record CommunityPostDTO(Long id, Long communityId, Long autorId, String autorNome,
    String conteudo, boolean possuiImagem, Instant criadoEm, Instant editadoEm, Long versao, boolean propria) {}
