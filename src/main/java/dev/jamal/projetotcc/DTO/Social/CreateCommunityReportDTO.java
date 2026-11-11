package dev.jamal.projetotcc.DTO.Social;
public record CreateCommunityReportDTO(@jakarta.validation.constraints.NotNull dev.jamal.projetotcc.Enum.CommunityReportReason motivo, @jakarta.validation.constraints.Size(max=500) String detalhes, @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.PositiveOrZero Long versao) {}
