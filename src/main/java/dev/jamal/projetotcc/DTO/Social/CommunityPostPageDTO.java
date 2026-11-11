package dev.jamal.projetotcc.DTO.Social;
import java.util.List;
public record CommunityPostPageDTO(List<CommunityPostDTO> publicacoes, boolean temMais, Long proximoCursor) {}
