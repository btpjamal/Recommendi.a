package dev.jamal.projetotcc.DTO.Social;
import java.util.List;
public record PrivateUnreadDTO(long total, List<ContactCount> porContato) {
    public record ContactCount(Long contatoId, long quantidade) {}
}
