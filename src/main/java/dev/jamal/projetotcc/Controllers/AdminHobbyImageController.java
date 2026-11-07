package dev.jamal.projetotcc.Controllers;

import dev.jamal.projetotcc.Service.Image.HobbyImageUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import dev.jamal.projetotcc.DTO.Image.HobbyImageUploadResponseDTO;
import dev.jamal.projetotcc.DTO.Image.HobbyImageConfirmRequestDTO;
import jakarta.validation.Valid;

import java.io.IOException;
import java.util.List;


@RestController
@RequestMapping("/api/v1/admin/hobbies/images")
@RequiredArgsConstructor
public class AdminHobbyImageController {

    private final HobbyImageUploadService imageUploadService;

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<HobbyImageUploadResponseDTO> upload(
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        HobbyImageUploadResponseDTO resultado =
                imageUploadService.enviarImagem(file);

        return ResponseEntity.ok(resultado);
    }

    @PostMapping("/confirm")
    public ResponseEntity<Void> confirmarImagem(
            @Valid @RequestBody HobbyImageConfirmRequestDTO request
    ) {

        imageUploadService.confirmarImagem(
                request.publicId(),
                request.hobbyId()
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/discard")
    public ResponseEntity<Void> descartarImagem(
            @RequestParam("publicId") String publicId
    ) throws IOException {

        imageUploadService.descartarImagem(publicId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pending")
    public ResponseEntity<List<HobbyImageUploadResponseDTO>> listarPendentes() {

        return ResponseEntity.ok(
                imageUploadService.listarPendentes()
        );
    }
}