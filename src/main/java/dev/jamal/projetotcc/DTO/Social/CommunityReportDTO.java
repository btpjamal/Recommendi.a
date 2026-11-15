package dev.jamal.projetotcc.DTO.Social;
public record CommunityReportDTO(Long id, Long communityId, String comunidade, Long postId, Long commentId, dev.jamal.projetotcc.Enum.CommunityReportReason motivo, String detalhes, dev.jamal.projetotcc.Enum.CommunityReportStatus status, dev.jamal.projetotcc.Enum.CommunityModerationDecision decisao, String notas, java.time.Instant criadoEm, java.time.Instant analisadoEm, Long versaoDenuncia, Long versaoDenunciada, Long versaoConteudo, String publicacao, String comentario, boolean excluido, boolean oculto, boolean possuiImagem, Evidence evidencia) {
    public record Evidence(String publicacao, String comentario, String sha256, boolean possuiImagem) {}
}
