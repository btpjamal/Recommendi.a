package dev.jamal.projetotcc.Service.Social;

import dev.jamal.projetotcc.DTO.Social.SocialFollowDTO;
import dev.jamal.projetotcc.Repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SocialFollowService {
    private final SocialFollowStore follows;
    private final SocialContactLockRepository locks;
    private final UserRepository users;
    private final SocialProfileRepository profiles;
    private final SocialUserBlockRepository blocks;
    @PersistenceContext private EntityManager entityManager;

    void validarParticipante(Long id, boolean refresh) {
        var user = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Participação indisponível."));
        var profile = profiles.findByUser_Id(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Ative sua participação social."));
        if (refresh) {
            entityManager.refresh(user);
            entityManager.refresh(profile);
        }
        if (!profile.isEnabled() || user.getDataNascimento() == null ||
                user.getDataNascimento().isAfter(LocalDate.now().minusYears(18))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Participação social indisponível.");
        }
    }

    private void validarDestino(Long actor, Long target) {
        if (target == null || target <= 0 || actor.equals(target)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha outro participante.");
        }
    }

    @Transactional(readOnly = true)
    public SocialFollowDTO consultar(Long actor, Long target) {
        validarParticipante(actor, false);
        validarDestino(actor, target);
        exigirDestinoDisponivel(actor, target, false);
        return new SocialFollowDTO(target, follows.seguindo(actor, target));
    }

    @Transactional
    public SocialFollowDTO alterar(Long actor, Long target, boolean seguir) {
        validarDestino(actor, target);
        if (locks.lockUser(Math.min(actor, target)).isEmpty() ||
                locks.lockUser(Math.max(actor, target)).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Participante indisponível.");
        }
        validarParticipante(actor, true);
        if (seguir) {
            exigirDestinoDisponivel(actor, target, true);
            follows.seguir(actor, target);
        } else {
            follows.deixarDeSeguir(actor, target);
        }
        return new SocialFollowDTO(target, seguir);
    }

    private void exigirDestinoDisponivel(Long actor, Long target, boolean refresh) {
        try {
            validarParticipante(target, refresh);
        } catch (ResponseStatusException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Participante indisponível.");
        }
        if (blocks.bloqueados(actor, target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Participante indisponível.");
        }
    }
}
