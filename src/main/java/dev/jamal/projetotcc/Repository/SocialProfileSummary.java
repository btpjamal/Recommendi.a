package dev.jamal.projetotcc.Repository;

import dev.jamal.projetotcc.Enum.SocialVisibility;

import java.time.LocalDate;

public record SocialProfileSummary(Long userId, String nome, LocalDate nascimento, boolean compartilharRegiao,
                                   String biografia, SocialVisibility fotoVisibilidade,
                                   SocialVisibility biografiaVisibilidade, SocialVisibility interessesVisibilidade,
                                   SocialVisibility hobbiesVisibilidade, boolean possuiFoto) {}
