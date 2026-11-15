package dev.jamal.projetotcc.Service.Social;

import dev.jamal.projetotcc.Entities.SocialProfile;
import dev.jamal.projetotcc.Entities.User;
import dev.jamal.projetotcc.Repository.SocialProfileRepository;
import dev.jamal.projetotcc.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;

@Service
@RequiredArgsConstructor
public class SocialProfileService {

    private static final int IDADE_MINIMA = 18;

    private final UserRepository userRepository;
    private final SocialProfileRepository socialProfileRepository;


    public boolean elegivel(Long userId) {

        User user = buscarUsuario(userId);

        return maiorDeIdade(user);
    }


    public boolean participa(Long userId) {

        return socialProfileRepository
                .findByUser_Id(userId)
                .map(SocialProfile::isEnabled)
                .orElse(false);
    }


    @Transactional
    public SocialProfile ativar(Long userId) {

        userRepository.lockTokenVersion(userId).orElseThrow(() -> new IllegalStateException("Usuário não encontrado."));

        User user = buscarUsuario(userId);

        if (!maiorDeIdade(user)) {
            throw new IllegalStateException(
                    "A comunidade está disponível apenas para usuários maiores de 18 anos."
            );
        }

        SocialProfile socialProfile =
                socialProfileRepository
                        .findByUser_Id(userId)
                        .orElseGet(() -> {

                            SocialProfile novo =
                                    new SocialProfile();

                            novo.setUser(user);

                            return novo;
                        });

        socialProfile.setEnabled(true);

        return socialProfileRepository.save(socialProfile);
    }


    @Transactional
    public void desativar(Long userId) {

        userRepository.lockTokenVersion(userId).orElseThrow(() -> new IllegalStateException("Usuário não encontrado."));

        socialProfileRepository
                .findByUser_Id(userId)
                .ifPresent(socialProfile -> {

                    socialProfile.setEnabled(false);

                    socialProfileRepository.save(
                            socialProfile
                    );
                });
    }


    private User buscarUsuario(Long userId) {

        return userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Usuário não encontrado"
                        )
                );
    }


    static boolean maiorDeIdade(User user) {

        if (user.getDataNascimento() == null) {
            return false;
        }

        int idade = Period.between(
                user.getDataNascimento(),
                LocalDate.now()
        ).getYears();

        return idade >= IDADE_MINIMA;
    }
}
