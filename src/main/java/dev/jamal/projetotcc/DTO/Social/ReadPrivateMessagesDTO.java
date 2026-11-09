package dev.jamal.projetotcc.DTO.Social;
import jakarta.validation.constraints.*;
public record ReadPrivateMessagesDTO(@NotNull @Positive Long ateId) {}
