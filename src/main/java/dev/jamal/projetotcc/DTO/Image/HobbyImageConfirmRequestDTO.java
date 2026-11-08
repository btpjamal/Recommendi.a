package dev.jamal.projetotcc.DTO.Image;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HobbyImageConfirmRequestDTO(

        @NotBlank
        String publicId,

        @NotNull
        Long hobbyId

) {
}