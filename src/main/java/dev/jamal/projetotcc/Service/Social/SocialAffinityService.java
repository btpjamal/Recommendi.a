package dev.jamal.projetotcc.Service.Social;

import dev.jamal.projetotcc.DTO.Social.SocialAffinityDTO;
import dev.jamal.projetotcc.DTO.Social.SocialHobbyDTO;
import dev.jamal.projetotcc.DTO.Social.SocialProfileDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SocialAffinityService {

    static final int PESO_PRATICANDO = 3;
    static final int PESO_HOBBY_INTERESSE = 2;
    static final int PESO_INTERESSE = 1;


    public SocialAffinityDTO calcular(
            SocialProfileDTO usuarioAtual,
            SocialProfileDTO outroUsuario
    ) {

        List<String> interessesEmComum =
                usuarioAtual.interesses()
                        .stream()
                        .filter(outroUsuario.interesses()::contains)
                        .sorted()
                        .toList();


        List<String> praticandoEmComum =
                nomesEmComum(
                        usuarioAtual.praticando(),
                        outroUsuario.praticando()
                );


        List<String> hobbiesDeInteresseEmComum =
                nomesEmComum(
                        usuarioAtual.interessados(),
                        outroUsuario.interessados()
                );


        int score =
                praticandoEmComum.size() * PESO_PRATICANDO
                        + hobbiesDeInteresseEmComum.size() * PESO_HOBBY_INTERESSE
                        + interessesEmComum.size() * PESO_INTERESSE;


        return new SocialAffinityDTO(
                score,
                interessesEmComum,
                praticandoEmComum,
                hobbiesDeInteresseEmComum
        );
    }


    private List<String> nomesEmComum(
            List<SocialHobbyDTO> primeiraLista,
            List<SocialHobbyDTO> segundaLista
    ) {

        return primeiraLista
                .stream()

                .filter(primeiro ->
                        segundaLista
                                .stream()
                                .anyMatch(segundo ->
                                        segundo.hobbyId()
                                                .equals(primeiro.hobbyId())
                                )
                )

                .map(SocialHobbyDTO::nome)
                .sorted()
                .toList();
    }
}