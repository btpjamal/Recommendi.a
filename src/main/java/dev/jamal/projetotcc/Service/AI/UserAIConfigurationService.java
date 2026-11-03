package dev.jamal.projetotcc.Service.AI;

import dev.jamal.projetotcc.Entities.User;
import dev.jamal.projetotcc.Entities.UserAIConfiguration;
import dev.jamal.projetotcc.Exception.BusinessException;
import dev.jamal.projetotcc.Repository.UserAIConfigurationRepository;
import dev.jamal.projetotcc.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.security.access.prepost.PreAuthorize("isAuthenticated() and #p0 == authentication.principal.id")
@Service
@RequiredArgsConstructor
public class UserAIConfigurationService {

    private final UserAIConfigurationRepository repository;
    private final UserRepository userRepository;
    private final APIKeyEncryptionService encryptionService;


    @Transactional(readOnly = true)
    public boolean configurada(Long userId) {

        return repository.existsByUser_Id(userId);
    }


    @Transactional
    public void salvar(
            Long userId,
            String apiKey
    ) {

        String chave = validar(apiKey);

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Usuário não encontrado."
                                )
                        );


        UserAIConfiguration configuration =
                repository.findByUser_Id(userId)
                        .orElseGet(() -> {

                            UserAIConfiguration nova =
                                    new UserAIConfiguration();

                            nova.setUser(user);

                            return nova;
                        });


        configuration.setApiKeyEncrypted(
                encryptionService.encrypt(chave)
        );

        repository.save(configuration);
    }


    @Transactional
    public void remover(Long userId) {

        repository.findByUser_Id(userId)
                .ifPresent(repository::delete);
    }


    @Transactional(readOnly = true)
    public String obterApiKey(Long userId) {

        UserAIConfiguration configuration =
                repository.findByUser_Id(userId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Configure sua chave de API para utilizar os recursos de inteligência artificial."
                                )
                        );


        return encryptionService.decrypt(
                configuration.getApiKeyEncrypted()
        );
    }


    private String validar(String apiKey) {

        if (
                apiKey == null ||
                        apiKey.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Informe uma chave de API."
            );
        }


        String chave = apiKey.trim();


        if (chave.length() > 500 || chave.chars().anyMatch(c -> c <= 32 || c >= 127)) {
            throw new IllegalArgumentException(
                    "Chave de API inválida."
            );
        }


        return chave;
    }
}