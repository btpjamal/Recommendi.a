package dev.jamal.projetotcc.Mapper;

import dev.jamal.projetotcc.DTO.Hobby.*;
import dev.jamal.projetotcc.Entities.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HobbyMapper {
    private final HobbyCategoryMapper categoryMapper;

    public Hobby toEntity(HobbyCreateRequestDTO d, HobbyCategory c) {
        Hobby h = new Hobby();
        h.setNome(d.getNome());
        h.setDescricao(d.getDescricao());
        h.setImagemUrl(d.getImagemUrl());
        h.setCustoEstimado(d.getCustoEstimado());
        h.setCustoRecorrenteEstimado(d.getCustoRecorrenteEstimado());
        h.setNivelDificuldade(d.getNivelDificuldade());
        h.setTempoNecessario(d.getTempoNecessario());
        h.setTipoSocializacao(d.getTipoSocializacao());
        h.setNivelAtividadeFisica(d.getNivelAtividadeFisica());
        h.setAmbiente(d.getAmbiente());
        h.setRequerEquipamento(d.isRequerEquipamento());
        h.setCategory(c);
        return h;
    }

    public HobbyResponseDTO toResponseDTO(Hobby h) {
        return new HobbyResponseDTO(h.getId(), h.getNome(), h.getDescricao(), h.getImagemUrl(), h.getCustoEstimado(), h.getCustoRecorrenteEstimado(), h.getNivelDificuldade(), h.getTempoNecessario(), h.getTipoSocializacao(), h.getNivelAtividadeFisica(), h.getAmbiente(), h.isRequerEquipamento(), categoryMapper.toResponseDTO(h.getCategory()));
    }

    public HobbySummaryDTO toSummaryDTO(Hobby h) {
        return new HobbySummaryDTO(h.getId(), h.getNome(), h.getCategory().getNome());
    }
}
