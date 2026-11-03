package dev.jamal.projetotcc.Controllers;

import dev.jamal.projetotcc.DTO.AI.AIGeneralPlanContext;
import dev.jamal.projetotcc.DTO.AI.AIUserContext;
import dev.jamal.projetotcc.DTO.AI.GeneralPersonalizedPlanResponseDTO;
import dev.jamal.projetotcc.DTO.AI.PersonalizedPlanResponseDTO;
import dev.jamal.projetotcc.Entities.GeneralPersonalizedPlan;
import dev.jamal.projetotcc.Service.AI.*;
import dev.jamal.projetotcc.Service.AI.provider.AIProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import dev.jamal.projetotcc.Entities.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AIContextController {

    private final AIContextService aiContextService;
    private final PromptBuilderService promptBuilderService;
    private final PersonalizedPlanService personalizedPlanService;
    private final AIGeneralPlanContextService aiGeneralPlanContextService;
    private final AIProvider aiProvider;
    private final GeneralPersonalizedPlanService generalPersonalizedPlanService;

    @org.springframework.security.access.prepost.PreAuthorize("#p0 == authentication.principal.id")
    @GetMapping("/context/{userId}/{hobbyId}")
    public ResponseEntity<AIUserContext> obterContexto(
            @PathVariable Long userId,
            @PathVariable Long hobbyId
    ) {

        AIUserContext contexto =
                aiContextService.construirContexto(
                        userId,
                        hobbyId
                );

        return ResponseEntity.ok(contexto);
    }

    @org.springframework.security.access.prepost.PreAuthorize("#p0 == authentication.principal.id")
    @GetMapping("/prompt/{userId}/{hobbyId}")
    public ResponseEntity<String> obterPrompt(
            @PathVariable Long userId,
            @PathVariable Long hobbyId
    ) {

        AIUserContext contexto =
                aiContextService.construirContexto(
                        userId,
                        hobbyId
                );

        String prompt =
                promptBuilderService
                        .construirPromptPlanoInicial(contexto);

        return ResponseEntity.ok(prompt);
    }

    @PostMapping("/plan/{hobbyId}")
    public ResponseEntity<String> gerarPlano(
        @AuthenticationPrincipal User user,
        @PathVariable Long hobbyId
    ) {



        String plano =
                personalizedPlanService
                        .gerarPlano(
                                user.getId(),
                                hobbyId
                        );

        return ResponseEntity.ok(plano);
    }

    @GetMapping("/plan/{hobbyId}")
    public ResponseEntity<PersonalizedPlanResponseDTO> buscarPlano(
            @AuthenticationPrincipal User user,
            @PathVariable Long hobbyId
    ) {
        return ResponseEntity.ok(
                personalizedPlanService.buscarPlano(user.getId(), hobbyId)
        );
    }

    @PostMapping("/plan/{hobbyId}/regenerate")
    public ResponseEntity<String> regenerarPlano(
            @AuthenticationPrincipal User user,
            @PathVariable Long hobbyId
    ) {
        return ResponseEntity.ok(
                personalizedPlanService.regenerarPlano(
                        user.getId(),
                        hobbyId
                )
        );
    }

//    @GetMapping("/general/context")
//    public ResponseEntity<AIGeneralPlanContext> buscarContextoPlanoGeral(
//            @AuthenticationPrincipal User user
//    ) {
//        return ResponseEntity.ok(
//                aiGeneralPlanContextService.construir(user.getId())
//        );
//    }
//
//    @GetMapping("/general/prompt")
//    public ResponseEntity<String> buscarPromptPlanoGeral(
//            @AuthenticationPrincipal User user
//    ) {
//
//        AIGeneralPlanContext context =
//                aiGeneralPlanContextService.construir(
//                        user.getId()
//                );
//
//        return ResponseEntity.ok(
//                promptBuilderService
//                        .construirPromptPlanoGeral(context)
//        );
//    }
//
//    @PostMapping("/general/test")
//    public ResponseEntity<String> testarPlanoGeral(
//            @AuthenticationPrincipal User user
//    ) {
//
//        AIGeneralPlanContext context =
//                aiGeneralPlanContextService.construir(
//                        user.getId()
//                );
//
//        String prompt =
//                promptBuilderService
//                        .construirPromptPlanoGeral(context);
//
//        return ResponseEntity.ok(
//                aiProvider.generate(prompt)
//        );
//    }

    @GetMapping("/general-plan")
    public ResponseEntity<GeneralPersonalizedPlanResponseDTO> buscarPlanoGeral(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(
                generalPersonalizedPlanService.buscarPlano(user.getId())
        );
    }

    @PostMapping("/general-plan")
    public ResponseEntity<GeneralPersonalizedPlanResponseDTO> gerarPlanoGeral(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(
                generalPersonalizedPlanService.gerarPlano(user.getId())
        );
    }

    @PostMapping("/general-plan/regenerate")
    public ResponseEntity<GeneralPersonalizedPlanResponseDTO> regenerarPlanoGeral(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(
                generalPersonalizedPlanService.regenerarPlano(user.getId())
        );
    }
}