
package dev.jamal.projetotcc.Controllers;

import dev.jamal.projetotcc.DTO.Location.HobbyLocationRuleRequestDTO;
import dev.jamal.projetotcc.DTO.Location.HobbyLocationRuleResponseDTO;
import dev.jamal.projetotcc.Service.Location.HobbyLocationRuleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/hobbies/{hobbyId}/location-rules")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class HobbyLocationRuleAdminController {

    private final HobbyLocationRuleService ruleService;

    @GetMapping
    public ResponseEntity<List<HobbyLocationRuleResponseDTO>> listar(
            @PathVariable Long hobbyId
    ) {
        return ResponseEntity.ok(
                ruleService.listar(hobbyId)
        );
    }

    @PostMapping
    public ResponseEntity<HobbyLocationRuleResponseDTO> criar(
            @PathVariable Long hobbyId,
            @Valid @RequestBody HobbyLocationRuleRequestDTO request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ruleService.criar(hobbyId, request));
    }

    @PutMapping("/{ruleId}")
    public ResponseEntity<HobbyLocationRuleResponseDTO> atualizar(
            @PathVariable Long hobbyId,
            @PathVariable Long ruleId,
            @Valid @RequestBody HobbyLocationRuleRequestDTO request
    ) {
        return ResponseEntity.ok(
                ruleService.atualizar(hobbyId, ruleId, request)
        );
    }

    @PatchMapping("/{ruleId}/status")
    public ResponseEntity<HobbyLocationRuleResponseDTO> alterarStatus(
            @PathVariable Long hobbyId,
            @PathVariable Long ruleId,
            @Valid @RequestBody AlterarStatusRequest request
    ) {
        return ResponseEntity.ok(
                ruleService.alterarStatus(
                        hobbyId,
                        ruleId,
                        request.ativo()
                )
        );
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long hobbyId,
            @PathVariable Long ruleId
    ) {
        ruleService.excluir(hobbyId, ruleId);
        return ResponseEntity.noContent().build();
    }

    public record AlterarStatusRequest(
            @jakarta.validation.constraints.NotNull
            Boolean ativo
    ) {}
}
