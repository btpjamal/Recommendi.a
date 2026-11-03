package dev.jamal.projetotcc.Service;
import dev.jamal.projetotcc.DTO.Profile.*; import dev.jamal.projetotcc.Entities.*; import dev.jamal.projetotcc.Exception.ResourceNotFoundException; import dev.jamal.projetotcc.Mapper.UserProfileMapper; import dev.jamal.projetotcc.Repository.*;
import jakarta.transaction.Transactional; import lombok.RequiredArgsConstructor; import org.springframework.stereotype.Service;
@org.springframework.security.access.prepost.PreAuthorize("isAuthenticated() and #p0 == authentication.principal.id")
@Service @RequiredArgsConstructor @Transactional
public class UserProfileService {
 private final RecommendationProfileRepository recommendationProfileRepository; private final UserRepository userRepository; private final UserProfileMapper mapper;
 public UserProfileResponseDTO buscarPorUsuario(Long userId){ return mapper.toResponseDTO(recommendationProfileRepository.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException("Perfil do usuário não encontrado."))); }
 public UserProfileResponseDTO criarOuAtualizar(Long userId, UserProfileCreateRequestDTO d){ User u=userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado.")); RecommendationProfile p= recommendationProfileRepository.findByUserId(userId).orElseGet(RecommendationProfile::new); p.setUser(u); p.setTempoDisponivelSemanal(d.getTempoDisponivelSemanal()); p.setOrcamentoInicial(d.getOrcamentoInicial()); p.setTipoSocializacao(d.getTipoSocializacao()); p.setNivelAtividadeFisicaDesejada(d.getNivelAtividadeFisicaDesejada()); p.setAmbientePreferido(d.getAmbientePreferido()); p.setQuestionarioConcluido(true); return mapper.toResponseDTO(recommendationProfileRepository.save(p)); }
}
