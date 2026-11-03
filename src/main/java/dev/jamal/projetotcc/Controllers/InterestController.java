package dev.jamal.projetotcc.Controllers;

import dev.jamal.projetotcc.DTO.Interest.InterestResponseDTO;
import dev.jamal.projetotcc.Service.InterestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/interests")
@RequiredArgsConstructor
public class InterestController {

    private final InterestService interestService;

    @GetMapping
    public ResponseEntity<List<InterestResponseDTO>> listarTodos() {

        System.out.println(">>> ENTROU NO GET /interests");

        List<InterestResponseDTO> interesses =
                interestService.listarTodos();

        System.out.println(
                ">>> QUANTIDADE DE INTERESSES: " + interesses.size()
        );

        System.out.println(
                ">>> INTERESSES: " + interesses
        );

        return ResponseEntity.ok(interesses);
    }
}
