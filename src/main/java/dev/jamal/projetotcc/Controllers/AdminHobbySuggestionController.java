
package dev.jamal.projetotcc.Controllers;

import dev.jamal.projetotcc.DTO.HobbySuggestion.HobbySuggestionRejectRequestDTO;
import dev.jamal.projetotcc.DTO.HobbySuggestion.HobbySuggestionResponseDTO;
import dev.jamal.projetotcc.Entities.User;
import dev.jamal.projetotcc.Service.HobbySuggestionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import dev.jamal.projetotcc.DTO.HobbySuggestion.HobbySuggestionApproveRequestDTO;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/hobby-suggestions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminHobbySuggestionController {

    private final HobbySuggestionService suggestionService;

    @GetMapping("/pending")
    public ResponseEntity<List<HobbySuggestionResponseDTO>>
    listarPendentes() {

        return ResponseEntity.ok(
                suggestionService.listarPendentes()
        );
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<HobbySuggestionResponseDTO> rejeitar(
            @PathVariable Long id,
            @RequestBody @Valid HobbySuggestionRejectRequestDTO dto,
            @AuthenticationPrincipal User admin
    ) {

        HobbySuggestionResponseDTO response =
                suggestionService.rejeitar(
                        id,
                        dto.getJustificativa(),
                        admin.getId()
                );

        return ResponseEntity.ok(response);
    }


    @PatchMapping("/{id}/approve")
    public ResponseEntity<HobbySuggestionResponseDTO> aprovar(
            @PathVariable Long id,
            @RequestBody @Valid HobbySuggestionApproveRequestDTO dto,
            @AuthenticationPrincipal User admin
    ) {
        HobbySuggestionResponseDTO response =
                suggestionService.aprovar(
                        id,
                        dto,
                        admin.getId()
                );

        return ResponseEntity.ok(response);
    }

}
