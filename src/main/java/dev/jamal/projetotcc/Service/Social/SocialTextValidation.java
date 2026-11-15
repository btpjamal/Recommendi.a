package dev.jamal.projetotcc.Service.Social;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class SocialTextValidation {
    private SocialTextValidation() {}

    static void rejectNul(String text) {
        if (text != null && text.indexOf('\0') >= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O texto contém um caractere inválido.");
        }
    }
}
