package dev.jamal.projetotcc.config.DataSeeder;

import dev.jamal.projetotcc.Entities.*;
import dev.jamal.projetotcc.Enum.AmbientePreferido;
import dev.jamal.projetotcc.Enum.NivelAtividadeFisica;
import dev.jamal.projetotcc.Enum.TipoSocializacao;
import dev.jamal.projetotcc.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SeederMethods{

    private final HobbyCategoryRepository categoryRepository;
    private final HobbyRepository hobbyRepository;
    private final ObjectiveRepository objectiveRepository;
    private final HobbyObjectiveRepository hobbyObjectiveRepository;
    private final InterestRepository interestRepository;
    private final HobbyInterestRepository hobbyInterestRepository;


    public Interest obterOuCriarInteresse(String nome) {
        return interestRepository
                .findByNomeIgnoreCase(nome)
                .orElseGet(() -> {
                    Interest interest = new Interest();
                    interest.setNome(nome);

                    return interestRepository.save(interest);
                });
    }

    public void adicionarInteresseAoHobby(
            Hobby hobby,
            Interest interest,
            Integer peso
    ) {
        HobbyInterestId id = new HobbyInterestId();

        id.setHobbyId(hobby.getId());
        id.setInterestId(interest.getId());

        HobbyInterest hobbyInterest =
                hobbyInterestRepository
                        .findById(id)
                        .orElseGet(HobbyInterest::new);

        hobbyInterest.setId(id);
        hobbyInterest.setHobby(hobby);
        hobbyInterest.setInterest(interest);
        hobbyInterest.setPeso(peso);

        hobbyInterestRepository.save(hobbyInterest);
    }

    public HobbyCategory criarCategoriaSeNaoExistir(String nome) {
        return categoryRepository.findByNomeIgnoreCase(nome)
                .orElseGet(() -> {
                    HobbyCategory category = new HobbyCategory();
                    category.setNome(nome);
                    return categoryRepository.save(category);
                });
    }


    public Hobby criarOuAtualizarHobby(String nome, String descricao, Double custoEstimado, Integer nivelDificuldade,
                                        Double tempoNecessario, TipoSocializacao tipoSocializacao, HobbyCategory category,
                                        NivelAtividadeFisica atividadeFisica, AmbientePreferido ambiente
    ) {
        Hobby hobby = hobbyRepository.findByNomeIgnoreCase(nome)
                .orElseGet(Hobby::new);

        hobby.setNome(nome);
        hobby.setDescricao(descricao);
        hobby.setCustoEstimado(custoEstimado);
        hobby.setNivelDificuldade(nivelDificuldade);
        hobby.setTempoNecessario(tempoNecessario);
        hobby.setTipoSocializacao(tipoSocializacao);
        hobby.setCategory(category);
        hobby.setNivelAtividadeFisica(atividadeFisica);
        hobby.setAmbiente(ambiente);
        hobby.setRequerEquipamento(hobby.getCustoEstimado() != null && hobby.getCustoEstimado() > 0);

        return hobbyRepository.save(hobby);
    }

    public Objective obterOuCriarObjetivo(
            String nome,
            String descricao
    ){
        return objectiveRepository
                .findByNomeIgnoreCase(nome)
                .orElseGet(() -> {
                    Objective objective = new Objective();
                    objective.setNome(nome);
                    objective.setDescricao(descricao);

                    return objectiveRepository.save(objective);
                });
    }

    public void adicionarObjetivoAoHobby(
            Hobby hobby,
            Objective objective,
            Integer peso
    ){
        HobbyObjectiveId id = new HobbyObjectiveId();
        id.setHobbyId(hobby.getId());
        id.setObjectiveId(objective.getId());

        HobbyObjective hobbyObjective =
                hobbyObjectiveRepository
                        .findById(id)
                        .orElseGet(HobbyObjective::new);

        hobbyObjective.setId(id);
        hobbyObjective.setHobby(hobby);
        hobbyObjective.setObjective(objective);
        hobbyObjective.setPeso(peso);

        hobbyObjectiveRepository.save(hobbyObjective);
    }
}
