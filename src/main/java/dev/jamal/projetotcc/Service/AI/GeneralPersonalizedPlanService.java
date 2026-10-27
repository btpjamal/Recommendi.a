package dev.jamal.projetotcc.Service.AI;

import dev.jamal.projetotcc.DTO.AI.GeneralPersonalizedPlanResponseDTO;
import dev.jamal.projetotcc.Entities.GeneralPersonalizedPlan;
import dev.jamal.projetotcc.Exception.BusinessException;
import dev.jamal.projetotcc.Repository.GeneralPersonalizedPlanRepository;
import dev.jamal.projetotcc.Repository.UserRepository;
import dev.jamal.projetotcc.Service.AI.provider.AIProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated() and #p0 == authentication.principal.id")
public class GeneralPersonalizedPlanService {
    private final GeneralPersonalizedPlanRepository repository;
    private final UserRepository userRepository;
    private final AIGeneralPlanContextService contextService;
    private final AIGeneralPlanContextHashService hashService;
    private final PromptBuilderService promptBuilderService;
    private final AIProvider aiProvider;
    private final UserAIConfigurationService userAIConfigurationService;
    private final PlanGenerationGuard generationGuard;
    private final TransactionTemplate planTransactions;

    private record Input(String prompt, String hash, long tokenVersion) {}

    public GeneralPersonalizedPlanResponseDTO gerarPlano(Long userId) { return generate(userId, false); }
    public GeneralPersonalizedPlanResponseDTO regenerarPlano(Long userId) { return generate(userId, true); }

    private GeneralPersonalizedPlanResponseDTO generate(Long userId, boolean regenerate) {
        return generationGuard.run(userId, () -> {
            Input input = planTransactions.execute(status -> {
                boolean exists = repository.existsByUser_Id(userId);
                if (exists != regenerate) throw new BusinessException(exists
                        ? "O usuário já possui um plano geral." : "Plano geral não encontrado.");
                var context = contextService.construir(userId);
                return new Input(promptBuilderService.construirPromptPlanoGeral(context),
                        hashService.calcular(context), SessionVersionGuard.snapshot(userRepository, userId));
            });
            String content = aiProvider.generate(input.prompt(), userAIConfigurationService.obterApiKey(userId));
            return planTransactions.execute(status -> {
                SessionVersionGuard.lockAndValidate(userRepository, userId, input.tokenVersion());
                var user = userRepository.findById(userId).orElseThrow();
                GeneralPersonalizedPlan plan;
                if (regenerate) plan = repository.findByUser_Id(userId).orElseThrow();
                else {
                    if (repository.existsByUser_Id(userId)) throw new BusinessException("Já existe um plano geral.");
                    plan = new GeneralPersonalizedPlan();
                    plan.setUser(user);
                }
                plan.setConteudo(content);
                plan.setContextHash(input.hash());
                return toResponseDTO(repository.saveAndFlush(plan), false);
            });
        });
    }

    @Transactional(readOnly = true)
    public GeneralPersonalizedPlanResponseDTO buscarPlano(Long userId) {
        var plan = repository.findByUser_Id(userId)
                .orElseThrow(() -> new BusinessException("Plano geral não encontrado."));
        String hash = hashService.calcular(contextService.construir(userId));
        return toResponseDTO(plan, !plan.getContextHash().equals(hash));
    }

    private GeneralPersonalizedPlanResponseDTO toResponseDTO(GeneralPersonalizedPlan plan, boolean stale) {
        return new GeneralPersonalizedPlanResponseDTO(plan.getId(), plan.getConteudo(),
                plan.getDataCriacao(), plan.getDataAtualizacao(), stale);
    }
}
