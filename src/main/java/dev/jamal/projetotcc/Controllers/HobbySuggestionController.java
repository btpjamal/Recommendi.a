
package dev.jamal.projetotcc.Controllers;

import dev.jamal.projetotcc.DTO.HobbySuggestion.*;
import dev.jamal.projetotcc.Entities.User;
import dev.jamal.projetotcc.Service.HobbySuggestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hobby-suggestions")
@RequiredArgsConstructor
public class HobbySuggestionController {

    private final HobbySuggestionService suggestionService;

    @PostMapping
    public ResponseEntity<HobbySuggestionResponseDTO> criar(
            @RequestBody @Valid HobbySuggestionCreateRequestDTO dto,
            @AuthenticationPrincipal User user
    ) {
        HobbySuggestionResponseDTO response =
                suggestionService.criar(dto, user.getId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/mine")
    public ResponseEntity<List<HobbySuggestionResponseDTO>>
    listarMinhas(@AuthenticationPrincipal User user) {

        return ResponseEntity.ok(
                suggestionService.listarMinhas(user.getId())
        );
    }
}
