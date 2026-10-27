package dev.jamal.projetotcc.Service.AI;

import dev.jamal.projetotcc.DTO.AI.AIUserContext;
import dev.jamal.projetotcc.DTO.AI.PersonalizedPlanResponseDTO;
import dev.jamal.projetotcc.Entities.PersonalizedPlan;
import dev.jamal.projetotcc.Exception.BusinessException;
import dev.jamal.projetotcc.Repository.*;
import dev.jamal.projetotcc.Service.AI.provider.AIProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated() and #p0 == authentication.principal.id")
public class PersonalizedPlanService {
    private final AIContextService aiContextService;
    private final PromptBuilderService promptBuilderService;
    private final AIProvider aiProvider;
    private final PersonalizedPlanRepository personalizedPlanRepository;
    private final UserRepository userRepository;
    private final HobbyRepository hobbyRepository;
    private final AIContextHashService aiContextHashService;
    private final UserAIConfigurationService userAIConfigurationService;
    private final PlanGenerationGuard generationGuard;
    private final TransactionTemplate planTransactions;

    private record Input(String prompt, String hash, long tokenVersion) {}

    public String gerarPlano(Long userId, Long hobbyId) {
        return generate(userId, hobbyId, false);
    }

    public String regenerarPlano(Long userId, Long hobbyId) {
        return generate(userId, hobbyId, true);
    }

    private String generate(Long userId, Long hobbyId, boolean regenerate) {
        return generationGuard.run(userId, () -> {
            Input input = planTransactions.execute(status -> {
                boolean exists = personalizedPlanRepository.existsByUser_IdAndHobby_Id(userId, hobbyId);
                if (exists != regenerate) throw new BusinessException(exists
                        ? "Já existe um plano personalizado para este hobby."
                        : "Não existe plano personalizado para atualizar.");
                AIUserContext context = aiContextService.construirContexto(userId, hobbyId);
                long version = SessionVersionGuard.snapshot(userRepository, userId);
                return new Input(promptBuilderService.construirPromptPlanoInicial(context),
                        aiContextHashService.calcular(context), version);
            });
            String key = userAIConfigurationService.obterApiKey(userId);
            String content = aiProvider.generate(input.prompt(), key);
            planTransactions.executeWithoutResult(status -> {
                SessionVersionGuard.lockAndValidate(userRepository, userId, input.tokenVersion());
                var user = userRepository.findById(userId).orElseThrow();
                PersonalizedPlan plan;
                if (regenerate) plan = personalizedPlanRepository.findByUser_IdAndHobby_Id(userId, hobbyId)
                        .orElseThrow(() -> new BusinessException("Plano não encontrado."));
                else {
                    if (personalizedPlanRepository.existsByUser_IdAndHobby_Id(userId, hobbyId))
                        throw new BusinessException("Já existe um plano para este hobby.");
                    plan = new PersonalizedPlan();
                    plan.setUser(user);
                    plan.setHobby(hobbyRepository.findById(hobbyId).orElseThrow());
                }
                plan.setConteudo(content);
                plan.setContextHash(input.hash());
                personalizedPlanRepository.save(plan);
            });
            return content;
        });
    }

    @Transactional(readOnly = true)
    public PersonalizedPlanResponseDTO buscarPlano(Long userId, Long hobbyId) {
        PersonalizedPlan plan = personalizedPlanRepository.findByUser_IdAndHobby_Id(userId, hobbyId)
                .orElseThrow(() -> new BusinessException("Plano personalizado ainda não foi gerado."));
        String hash = aiContextHashService.calcular(aiContextService.construirContexto(userId, hobbyId));
        return new PersonalizedPlanResponseDTO(plan.getId(), plan.getHobby().getId(),
                plan.getHobby().getNome(), plan.getConteudo(), plan.getDataCriacao(),
                plan.getDataAtualizacao(), !plan.getContextHash().equals(hash));
    }
}
