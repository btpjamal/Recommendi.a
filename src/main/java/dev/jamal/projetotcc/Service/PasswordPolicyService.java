package dev.jamal.projetotcc.Service;

import dev.jamal.projetotcc.Exception.BusinessException;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

@Service
public class PasswordPolicyService {
    private static final Set<String> COMMON = Set.of("123456789012", "1234567890123456", "password1234", "password123456", "qwerty123456", "senhasenha123", "administrador", "admin12345678");
    public void validar(String senha) {
        if (senha == null || senha.codePointCount(0, senha.length()) < 12
                || senha.getBytes(StandardCharsets.UTF_8).length > 72
                || COMMON.contains(senha.toLowerCase(Locale.ROOT))
                || senha.codePoints().distinct().count() < 4) {
            throw new BusinessException("Use uma senha de pelo menos 12 caracteres, até 72 bytes, que não seja comum ou repetitiva.");
        }
    }
}
