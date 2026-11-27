package dev.jamal.projetotcc.Service;

import dev.jamal.projetotcc.DTO.Profile.UserProfileCreateRequestDTO;
import dev.jamal.projetotcc.DTO.Profile.UserProfileResponseDTO;
import dev.jamal.projetotcc.Entities.RecommendationProfile;
import dev.jamal.projetotcc.Entities.User;
import dev.jamal.projetotcc.Exception.ResourceNotFoundException;
import dev.jamal.projetotcc.Mapper.UserProfileMapper;
import dev.jamal.projetotcc.Repository.RecommendationProfileRepository;
import dev.jamal.projetotcc.Repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@PreAuthorize("isAuthenticated() and #p0 == authentication.principal.id")
@Service
@RequiredArgsConstructor
@Transactional
public class UserProfileService {

    private final RecommendationProfileRepository recommendationProfileRepository;
    private final UserRepository userRepository;
    private final UserProfileMapper mapper;
    private final QuestionarioConclusao questionarioConclusao;

    public UserProfileResponseDTO buscarPorUsuario(Long userId) {
        return mapper.toResponseDTO(recommendationProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil do usuário não encontrado.")));
    }

    public UserProfileResponseDTO criarOuAtualizar(Long userId, UserProfileCreateRequestDTO dto) {
        User user = userRepository.findByIdForProfileUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        RecommendationProfile perfil = recommendationProfileRepository.findByUserId(userId)
                .orElseGet(RecommendationProfile::new);

        perfil.setUser(user);
        perfil.setTempoDisponivelSemanal(dto.getTempoDisponivelSemanal());
        perfil.setOrcamentoInicial(dto.getOrcamentoInicial());
        perfil.setTipoSocializacao(dto.getTipoSocializacao());
        perfil.setNivelAtividadeFisicaDesejada(dto.getNivelAtividadeFisicaDesejada());
        perfil.setAmbientePreferido(dto.getAmbientePreferido());
        questionarioConclusao.atualizar(perfil);

        return mapper.toResponseDTO(recommendationProfileRepository.save(perfil));
    }
}
