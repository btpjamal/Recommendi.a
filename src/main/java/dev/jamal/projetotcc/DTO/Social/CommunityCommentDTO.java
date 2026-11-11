package dev.jamal.projetotcc.DTO.Social;
public record CommunityCommentDTO(Long id, Long autorId, String autorNome, String conteudo, java.time.Instant criadoEm, java.time.Instant editadoEm, Long versao, boolean propria) {}
