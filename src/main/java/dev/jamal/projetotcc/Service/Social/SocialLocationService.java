package dev.jamal.projetotcc.Service.Social;

import dev.jamal.projetotcc.DTO.Social.SocialLocationSettingsDTO;
import dev.jamal.projetotcc.Entities.SocialProfile;
import dev.jamal.projetotcc.Enum.SocialLocationScope;
import dev.jamal.projetotcc.Repository.RecommendationProfileRepository;
import dev.jamal.projetotcc.Repository.SocialProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SocialLocationService {
    private static final Set<String> UFS = Set.of(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS",
            "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");

    private final RecommendationProfileRepository profileRepository;
    private final SocialProfileRepository socialRepository;

    public record Localizacao(String estado, String cidade) {
        public boolean temEstado() { return estado != null; }
        public boolean temCidade() { return temEstado() && cidade != null; }
    }

    public static String normalizarEstado(String valor) {
        if (valor == null) return null;
        String uf = valor.trim().toUpperCase(Locale.ROOT);
        return UFS.contains(uf) ? uf : null;
    }

    public static String normalizarCidade(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .strip().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public Localizacao buscar(Long userId) {
        return profileRepository.findByUserId(userId)
                .map(p -> new Localizacao(normalizarEstado(p.getEstado()), normalizarCidade(p.getCidade())))
                .orElse(new Localizacao(null, null));
    }

    public void validarEscopo(Localizacao local, SocialLocationScope escopo) {
        if (escopo == SocialLocationScope.ESTADUAL && !local.temEstado()) {
            throw new IllegalArgumentException("Preencha uma UF válida no seu perfil para filtrar por estado.");
        }
        if (escopo == SocialLocationScope.MUNICIPAL && !local.temCidade()) {
            throw new IllegalArgumentException("Preencha cidade e UF válidas no seu perfil para usar esta opção.");
        }
    }

    public static String comparar(Localizacao atual, Localizacao outra) {
        if (!atual.temEstado() || !outra.temEstado() || !atual.estado().equals(outra.estado())) return null;
        if (atual.temCidade() && outra.temCidade() && atual.cidade().equals(outra.cidade())) return "MESMA_CIDADE";
        return "MESMO_ESTADO";
    }

    @Transactional(readOnly = true)
    public String proximidade(Localizacao atual, SocialProfile outro) {
        if (!outro.isCompartilharRegiao()) return null;
        return comparar(atual, buscar(outro.getUser().getId()));
    }

    @Transactional(readOnly = true)
    public SocialLocationSettingsDTO configuracao(Long userId) {
        Localizacao local = buscar(userId);
        boolean visivel = socialRepository.findByUser_Id(userId)
                .filter(SocialProfile::isEnabled).map(SocialProfile::isCompartilharRegiao).orElse(false);
        return new SocialLocationSettingsDTO(local.temEstado(), local.temCidade(), visivel);
    }

    @Transactional
    public SocialLocationSettingsDTO atualizarVisibilidade(Long userId, boolean compartilhar) {
        SocialProfile perfil = socialRepository.findByUser_Id(userId)
                .filter(SocialProfile::isEnabled)
                .orElseThrow(() -> new IllegalStateException("Ative sua participação social primeiro."));
        perfil.setCompartilharRegiao(compartilhar);
        return configuracao(userId);
    }
}
