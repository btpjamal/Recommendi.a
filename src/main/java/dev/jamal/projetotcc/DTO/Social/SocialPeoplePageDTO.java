package dev.jamal.projetotcc.DTO.Social;

import java.util.List;

public record SocialPeoplePageDTO(List<Card> pessoas, boolean temMais) {
    public record Card(SocialProfileDTO perfil, boolean seguindo) {}
}
