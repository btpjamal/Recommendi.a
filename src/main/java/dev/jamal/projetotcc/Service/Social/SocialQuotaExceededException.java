package dev.jamal.projetotcc.Service.Social;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class SocialQuotaExceededException extends ResponseStatusException {
    public SocialQuotaExceededException() {
        super(HttpStatus.TOO_MANY_REQUESTS,
                "Limite de novas ações atingido. Aguarde até 60 segundos e tente novamente.");
    }
}
