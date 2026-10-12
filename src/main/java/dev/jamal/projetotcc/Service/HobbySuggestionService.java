
package dev.jamal.projetotcc.Service;

import dev.jamal.projetotcc.DTO.HobbySuggestion.*;
import dev.jamal.projetotcc.Entities.*;
import dev.jamal.projetotcc.Enum.HobbySuggestionStatus;
import dev.jamal.projetotcc.Exception.BusinessException;
import dev.jamal.projetotcc.Exception.ResourceNotFoundException;
import dev.jamal.projetotcc.DTO.Hobby.HobbyCreateRequestDTO;
import dev.jamal.projetotcc.DTO.HobbySuggestion.HobbySuggestionApproveRequestDTO;
import dev.jamal.projetotcc.Mapper.HobbyMapper;
import dev.jamal.projetotcc.Repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class HobbySuggestionService {

    private final HobbySuggestionRepository suggestionRepository;
    private final HobbyRepository hobbyRepository;
    private final HobbyCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final HobbyMapper hobbyMapper;

    public HobbySuggestionResponseDTO criar(
            HobbySuggestionCreateRequestDTO dto,
            Long userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Usuário não encontrado."
                        )
                );

        HobbyCategory category = categoryRepository
                .findById(dto.getCategoryId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Categoria não encontrada."
                        )
                );

        String nome = dto.getNome().trim()
                .replaceAll("\\s+", " ");

        String nomeNormalizado = normalizarNome(nome);

        boolean hobbyExistente = hobbyRepository.findAll()
                .stream()
                .anyMatch(h ->
                        normalizarNome(h.getNome())
                                .equals(nomeNormalizado)
                );

        if (hobbyExistente) {
            throw new BusinessException(
                    "Este hobby já existe no catálogo."
            );
        }

        boolean sugestaoPendente = suggestionRepository
                .findByStatusOrderByCreatedAtAsc(
                        HobbySuggestionStatus.PENDENTE
                )
                .stream()
                .anyMatch(s ->
                        normalizarNome(s.getNome())
                                .equals(nomeNormalizado)
                );

        if (sugestaoPendente) {
            throw new BusinessException(
                    "Já existe uma sugestão pendente para este hobby."
            );
        }

        HobbySuggestion suggestion = new HobbySuggestion();

        suggestion.setNome(nome);
        suggestion.setDescricao(dto.getDescricao().trim());
        suggestion.setCustoEstimado(dto.getCustoEstimado());
        suggestion.setCustoRecorrenteEstimado(
                dto.getCustoRecorrenteEstimado()
        );
        suggestion.setNivelDificuldade(dto.getNivelDificuldade());
        suggestion.setTempoNecessario(dto.getTempoNecessario());
        suggestion.setTipoSocializacao(dto.getTipoSocializacao());
        suggestion.setNivelAtividadeFisica(
                dto.getNivelAtividadeFisica()
        );
        suggestion.setAmbiente(dto.getAmbiente());
        suggestion.setRequerEquipamento(dto.isRequerEquipamento());

        suggestion.setCategory(category);
        suggestion.setCreatedBy(user);
        suggestion.setStatus(HobbySuggestionStatus.PENDENTE);

        HobbySuggestion salvo = suggestionRepository.save(suggestion);

        return toResponseDTO(salvo);
    }

    @Transactional
    public List<HobbySuggestionResponseDTO> listarMinhas(
            Long userId
    ) {
        return suggestionRepository
                .findByCreatedByIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    private String normalizarNome(String nome) {
        String texto = Normalizer.normalize(
                nome.trim().toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD
        );

        return texto
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", " ");
    }

    private HobbySuggestionResponseDTO toResponseDTO(
            HobbySuggestion suggestion
    ) {
        HobbySuggestionResponseDTO dto =
                new HobbySuggestionResponseDTO();

        dto.setId(suggestion.getId());
        dto.setNome(suggestion.getNome());
        dto.setDescricao(suggestion.getDescricao());

        dto.setCustoEstimado(suggestion.getCustoEstimado());
        dto.setCustoRecorrenteEstimado(
                suggestion.getCustoRecorrenteEstimado()
        );
        dto.setNivelDificuldade(suggestion.getNivelDificuldade());
        dto.setTempoNecessario(suggestion.getTempoNecessario());

        dto.setTipoSocializacao(suggestion.getTipoSocializacao());
        dto.setNivelAtividadeFisica(
                suggestion.getNivelAtividadeFisica()
        );
        dto.setAmbiente(suggestion.getAmbiente());
        dto.setRequerEquipamento(
                suggestion.isRequerEquipamento()
        );

        dto.setCategoryId(suggestion.getCategory().getId());
        dto.setCategoryNome(suggestion.getCategory().getNome());

        dto.setCreatedById(suggestion.getCreatedBy().getId());
        dto.setCreatedByNome(suggestion.getCreatedBy().getNome());

        dto.setStatus(suggestion.getStatus());
        dto.setCreatedAt(suggestion.getCreatedAt());
        dto.setReviewedAt(suggestion.getReviewedAt());
        dto.setReviewNotes(suggestion.getReviewNotes());

        if (suggestion.getApprovedHobby() != null) {
            dto.setApprovedHobbyId(
                    suggestion.getApprovedHobby().getId()
            );
        }

        return dto;
    }

    @Transactional
    public List<HobbySuggestionResponseDTO> listarPendentes() {

        return suggestionRepository
                .findByStatusOrderByCreatedAtAsc(
                        HobbySuggestionStatus.PENDENTE
                )
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional
    public List<HobbySuggestionResponseDTO> listarTodas(
            HobbySuggestionStatus status
    ) {
        List<HobbySuggestion> sugestoes;

        if (status == null) {
            sugestoes = suggestionRepository
                    .findAllByOrderByCreatedAtDesc();
        } else {
            sugestoes = suggestionRepository
                    .findByStatusOrderByCreatedAtAsc(status);
        }

        return sugestoes.stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public HobbySuggestionResponseDTO rejeitar(
            Long suggestionId,
            String justificativa,
            Long adminId
    ) {

        HobbySuggestion suggestion = suggestionRepository
                .findByIdForUpdate(suggestionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Sugestão não encontrada."
                        )
                );

        if (suggestion.getStatus() != HobbySuggestionStatus.PENDENTE) {
            throw new BusinessException(
                    "Somente sugestões pendentes podem ser rejeitadas."
            );
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Administrador não encontrado."
                        )
                );

        if (admin.getRole() != dev.jamal.projetotcc.Enum.Role.ADMIN) {
            throw new BusinessException(
                    "O usuário não possui permissão administrativa."
            );
        }

        suggestion.setStatus(HobbySuggestionStatus.REJEITADA);
        suggestion.setReviewNotes(justificativa.trim());
        suggestion.setReviewedAt(LocalDateTime.now());
        suggestion.setReviewedBy(admin);

        HobbySuggestion salvo = suggestionRepository.save(suggestion);

        return toResponseDTO(salvo);
    }

    private HobbyCreateRequestDTO prepararHobbyDTO(
            HobbySuggestionApproveRequestDTO dto
    ) {
        HobbyCreateRequestDTO hobbyDTO =
                new HobbyCreateRequestDTO();

        hobbyDTO.setNome(dto.getNome().trim().replaceAll("\\s+", " "));
        hobbyDTO.setDescricao(dto.getDescricao().trim());
        hobbyDTO.setImagemUrl(dto.getImagemUrl());

        hobbyDTO.setCustoEstimado(dto.getCustoEstimado());
        hobbyDTO.setCustoRecorrenteEstimado(
                dto.getCustoRecorrenteEstimado()
        );

        hobbyDTO.setNivelDificuldade(dto.getNivelDificuldade());
        hobbyDTO.setTempoNecessario(dto.getTempoNecessario());

        hobbyDTO.setTipoSocializacao(dto.getTipoSocializacao());
        hobbyDTO.setNivelAtividadeFisica(
                dto.getNivelAtividadeFisica()
        );
        hobbyDTO.setAmbiente(dto.getAmbiente());
        hobbyDTO.setRequerEquipamento(dto.isRequerEquipamento());

        hobbyDTO.setCategoryId(dto.getCategoryId());

        return hobbyDTO;
    }


    public HobbySuggestionResponseDTO aprovar(
            Long suggestionId,
            HobbySuggestionApproveRequestDTO dto,
            Long adminId
    ) {
        HobbySuggestion suggestion = suggestionRepository
                .findByIdForUpdate(suggestionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Sugestão não encontrada."
                        )
                );

        if (suggestion.getStatus() != HobbySuggestionStatus.PENDENTE) {
            throw new BusinessException(
                    "Somente sugestões pendentes podem ser aprovadas."
            );
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Administrador não encontrado."
                        )
                );

        if (admin.getRole() != dev.jamal.projetotcc.Enum.Role.ADMIN) {
            throw new BusinessException(
                    "O usuário não possui permissão administrativa."
            );
        }

        String nomeNormalizado = normalizarNome(dto.getNome());

        boolean hobbyExistente = hobbyRepository.findAll()
                .stream()
                .anyMatch(h ->
                        normalizarNome(h.getNome())
                                .equals(nomeNormalizado)
                );

        if (hobbyExistente) {
            throw new BusinessException(
                    "Já existe um hobby com esse nome no catálogo."
            );
        }

        HobbyCategory category = categoryRepository
                .findById(dto.getCategoryId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Categoria não encontrada."
                        )
                );

        HobbyCreateRequestDTO hobbyDTO = prepararHobbyDTO(dto);

        Hobby hobby = hobbyMapper.toEntity(hobbyDTO, category);

        Hobby hobbySalvo = hobbyRepository.saveAndFlush(hobby);

        suggestion.setStatus(HobbySuggestionStatus.APROVADA);
        suggestion.setReviewedBy(admin);
        suggestion.setReviewedAt(java.time.LocalDateTime.now());
        suggestion.setReviewNotes(dto.getObservacoes());
        suggestion.setApprovedHobby(hobbySalvo);

        HobbySuggestion salva = suggestionRepository.save(suggestion);

        return toResponseDTO(salva);
    }

}
