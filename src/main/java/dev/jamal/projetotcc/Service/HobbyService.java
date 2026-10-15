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

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class HobbyService {

    private final HobbyRepository hobbyRepository;
    private final HobbyCategoryRepository categoryRepository;
    private final HobbyMapper hobbyMapper;

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

        HobbyCategory category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada."));

        Hobby hobby = hobbyMapper.toEntity(dto, category);

        Hobby salvo = hobbyRepository.save(hobby);

        return hobbyMapper.toResponseDTO(salvo);
    }


    public HobbyResponseDTO atualizar(Long id, HobbyCreateRequestDTO dto) {

        Hobby hobby = hobbyRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Hobby não encontrado.")
                );

        HobbyCategory category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Categoria não encontrada.")
                );

        hobby.setNome(dto.getNome());
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
                        new ResourceNotFoundException("Hobby não encontrado.")
                );

        hobby.setAtivo(true);
        hobbyRepository.save(hobby);
    }

    // para uso interno da aplicação, não deve ser consumido por controllers
    @Transactional
    public Hobby buscarEntidadePorId(Long id) {
        return hobbyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hobby não encontrado."));
    }
}
