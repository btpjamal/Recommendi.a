package dev.jamal.projetotcc.Service;
import dev.jamal.projetotcc.Enum.TipoSocializacao;

import dev.jamal.projetotcc.DTO.Hobby.HobbyCreateRequestDTO;
import dev.jamal.projetotcc.DTO.Hobby.HobbyResponseDTO;
import dev.jamal.projetotcc.Entities.Hobby;
import dev.jamal.projetotcc.Entities.HobbyCategory;
import dev.jamal.projetotcc.Exception.ResourceNotFoundException;
import dev.jamal.projetotcc.Mapper.HobbyMapper;
import dev.jamal.projetotcc.Repository.HobbyCategoryRepository;
import dev.jamal.projetotcc.Repository.HobbyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import dev.jamal.projetotcc.DTO.Hobby.HobbyEstatisticasDTO;
import dev.jamal.projetotcc.DTO.Hobby.HobbyInterestAssociationDTO;
import dev.jamal.projetotcc.DTO.Hobby.HobbyObjectiveAssociationDTO;
import dev.jamal.projetotcc.Entities.*;
import dev.jamal.projetotcc.Repository.*;

import java.util.HashSet;
import java.util.Set;

import java.util.List;

import dev.jamal.projetotcc.Exception.BusinessException;

@Service
@RequiredArgsConstructor
@Transactional
public class HobbyService {

    private final HobbyRepository hobbyRepository;
    private final HobbyCategoryRepository categoryRepository;
    private final HobbyMapper hobbyMapper;
    private final HobbyInterestRepository hobbyInterestRepository;
    private final HobbyObjectiveRepository hobbyObjectiveRepository;
    private final InterestRepository interestRepository;
    private final ObjectiveRepository objectiveRepository;

    public HobbyEstatisticasDTO obterEstatisticas() {
        long total = hobbyRepository.count();
        long ativos = hobbyRepository.countByAtivoTrue();
        long inativos = hobbyRepository.countByAtivoFalse();

        return new HobbyEstatisticasDTO(
                total,
                ativos,
                inativos
        );
    }

    @Transactional
    public List<HobbyResponseDTO> listarTodos() {
        return hobbyRepository.findAllWithCategory()
                .stream()
                .map(hobbyMapper::toResponseDTO)
                .toList();
    }

    @Transactional
    public Page<HobbyResponseDTO> listarPaginado(
            String nome,
            String status,
            int page,
            int size,
            String ordenacao
    ) {

        Sort sort = switch (ordenacao.toUpperCase()) {
            case "NOME_ASC" -> Sort.by(
                    Sort.Order.asc("nome").ignoreCase(),
                    Sort.Order.asc("id")
            );

            case "NOME_DESC" -> Sort.by(
                    Sort.Order.desc("nome").ignoreCase(),
                    Sort.Order.asc("id")
            );

            case "ANTIGOS" -> Sort.by("id").ascending();

            default -> Sort.by("id").descending();
        };

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.max(1, Math.min(size, 100)),
                sort
        );

        String nomeNormalizado =
                nome == null ? "" : nome.trim();

        String statusNormalizado =
                status == null ? "TODOS" : status.toUpperCase();

        return hobbyRepository
                .buscarPaginado(
                        nomeNormalizado,
                        statusNormalizado,
                        pageable
                )
                .map(hobbyMapper::toResponseDTO);
    }

    @Transactional
    public HobbyResponseDTO buscarPorId(Long id) {
        Hobby hobby = hobbyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hobby não encontrado."));

        return hobbyMapper.toResponseDTO(hobby);
    }

    public HobbyResponseDTO salvar(HobbyCreateRequestDTO dto) {

        String nome = dto.getNome().trim().replaceAll("\\s+", " ");

        if (hobbyRepository.existsByNomeNormalizado(nome)) {
            throw new BusinessException(
                    "Já existe um hobby com esse nome no catálogo."
            );
        }

        HobbyCategory category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Categoria não encontrada.")
                );

        Hobby hobby = hobbyMapper.toEntity(dto, category);
        hobby.setNome(nome);

        Hobby salvo = hobbyRepository.saveAndFlush(hobby);

        salvarInteresses(salvo, dto.getInterestAssociations());
        salvarObjetivos(salvo, dto.getObjectiveAssociations());

        return hobbyMapper.toResponseDTO(salvo);
    }


    public HobbyResponseDTO atualizar(Long id, HobbyCreateRequestDTO dto) {

        Hobby hobby = hobbyRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Hobby não encontrado.")
                );

        String nome = dto.getNome().trim().replaceAll("\\s+", " ");

        if (hobbyRepository.existsByNomeNormalizadoAndIdNot(nome, id)) {
            throw new BusinessException(
                    "Já existe outro hobby com esse nome no catálogo."
            );
        }

        HobbyCategory category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Categoria não encontrada.")
                );

        hobby.setNome(nome);
        hobby.setDescricao(dto.getDescricao());
        hobby.setImagemUrl(dto.getImagemUrl());

        hobby.setCustoEstimado(dto.getCustoEstimado());
        hobby.setCustoRecorrenteEstimado(dto.getCustoRecorrenteEstimado());

        hobby.setNivelDificuldade(dto.getNivelDificuldade());
        hobby.setTempoNecessario(dto.getTempoNecessario());

        hobby.setTipoSocializacao(dto.getTipoSocializacao());
        hobby.setNivelAtividadeFisica(dto.getNivelAtividadeFisica());
        hobby.setAmbiente(dto.getAmbiente());

        hobby.setRequerEquipamento(dto.isRequerEquipamento());
        hobby.setCategory(category);

        Hobby atualizado = hobbyRepository.save(hobby);

        if (dto.getInterestAssociations() != null) {
            hobbyInterestRepository.deleteByHobbyId(atualizado.getId());
            hobbyInterestRepository.flush();

            salvarInteresses(
                    atualizado,
                    dto.getInterestAssociations()
            );
        }

        if (dto.getObjectiveAssociations() != null) {
            hobbyObjectiveRepository.deleteByHobbyId(atualizado.getId());
            hobbyObjectiveRepository.flush();

            salvarObjetivos(
                    atualizado,
                    dto.getObjectiveAssociations()
            );
        }

        return hobbyMapper.toResponseDTO(atualizado);
    }


    public void deletar(Long id) {
        Hobby hobby = hobbyRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Hobby não encontrado.")
                );

        hobby.setAtivo(false);
        hobbyRepository.save(hobby);
    }


    public void reativar(Long id) {

        Hobby hobby = hobbyRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Hobby não encontrado."
                        )
                );

        boolean possuiInteresseValido =
                hobbyInterestRepository.existsByHobbyIdAndPesoBetween(
                        id, 1, 3
                );

        boolean possuiObjetivoValido =
                hobbyObjectiveRepository.existsByHobbyIdAndPesoBetween(
                        id, 1, 3
                );

        if (!possuiInteresseValido || !possuiObjetivoValido) {
            throw new BusinessException(
                    "Não é possível reativar o hobby. " +
                            "É necessário possuir pelo menos um interesse " +
                            "e um objetivo associados com pesos válidos."
            );
        }

        hobby.setAtivo(true);
        hobbyRepository.save(hobby);
    }


    // para uso interno da aplicação, não deve ser consumido por controllers
    @Transactional
    public Hobby buscarEntidadePorId(Long id) {
        return hobbyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hobby não encontrado."));
    }


    private void salvarInteresses(
            Hobby hobby,
            List<HobbyInterestAssociationDTO> associacoes
    ) {
        if (associacoes == null) {
            return;
        }

        Set<Long> ids = new HashSet<>();

        for (HobbyInterestAssociationDTO associacao : associacoes) {

            if (associacao == null
                    || associacao.interestId() == null
                    || associacao.peso() == null
                    || associacao.peso() < 1
                    || associacao.peso() > 3) {
                throw new BusinessException(
                        "Associação de interesse inválida."
                );
            }

            if (!ids.add(associacao.interestId())) {
                throw new BusinessException(
                        "Interesse duplicado: " + associacao.interestId()
                );
            }

            Interest interest = interestRepository
                    .findById(associacao.interestId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Interesse não encontrado: "
                                            + associacao.interestId()
                            )
                    );

            HobbyInterestId id = new HobbyInterestId();
            id.setHobbyId(hobby.getId());
            id.setInterestId(interest.getId());

            HobbyInterest hobbyInterest = new HobbyInterest();
            hobbyInterest.setId(id);
            hobbyInterest.setHobby(hobby);
            hobbyInterest.setInterest(interest);
            hobbyInterest.setPeso(associacao.peso());

            hobbyInterestRepository.save(hobbyInterest);
        }
    }


    private void salvarObjetivos(
            Hobby hobby,
            List<HobbyObjectiveAssociationDTO> associacoes
    ) {
        if (associacoes == null) {
            return;
        }

        Set<Long> ids = new HashSet<>();

        for (HobbyObjectiveAssociationDTO associacao : associacoes) {

            if (associacao == null
                    || associacao.objectiveId() == null
                    || associacao.peso() == null
                    || associacao.peso() < 1
                    || associacao.peso() > 3) {
                throw new BusinessException(
                        "Associação de objetivo inválida."
                );
            }

            if (!ids.add(associacao.objectiveId())) {
                throw new BusinessException(
                        "Objetivo duplicado: " + associacao.objectiveId()
                );
            }

            Objective objective = objectiveRepository
                    .findById(associacao.objectiveId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Objetivo não encontrado: "
                                            + associacao.objectiveId()
                            )
                    );

            HobbyObjectiveId id = new HobbyObjectiveId();
            id.setHobbyId(hobby.getId());
            id.setObjectiveId(objective.getId());

            HobbyObjective hobbyObjective = new HobbyObjective();
            hobbyObjective.setId(id);
            hobbyObjective.setHobby(hobby);
            hobbyObjective.setObjective(objective);
            hobbyObjective.setPeso(associacao.peso());

            hobbyObjectiveRepository.save(hobbyObjective);
        }
    }


}
