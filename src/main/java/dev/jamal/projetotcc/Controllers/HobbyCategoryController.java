
package dev.jamal.projetotcc.Controllers;

import dev.jamal.projetotcc.Repository.HobbyCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hobby-categories")
@RequiredArgsConstructor
public class HobbyCategoryController {

    private final HobbyCategoryRepository categoryRepository;

    public record CategoryResponseDTO(
            Long id,
            String nome
    ) {}

    @GetMapping
    public ResponseEntity<List<CategoryResponseDTO>> listarTodas() {

        List<CategoryResponseDTO> categorias =
                categoryRepository.findAll()
                        .stream()
                        .map(category -> new CategoryResponseDTO(
                                category.getId(),
                                category.getNome()
                        ))
                        .sorted((a, b) ->
                                a.nome().compareToIgnoreCase(b.nome())
                        )
                        .toList();

        return ResponseEntity.ok(categorias);
    }
}
