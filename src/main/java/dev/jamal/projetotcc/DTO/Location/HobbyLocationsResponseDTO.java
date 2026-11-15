
package dev.jamal.projetotcc.DTO.Location;

import dev.jamal.projetotcc.Enum.HobbyLocationSearchStatus;

import java.util.List;

public record HobbyLocationsResponseDTO(

        String cidade,
        String estado,
        Double latitude,
        Double longitude,
        List<HobbyLocationDTO> locais,
        HobbyLocationSearchStatus status,
        String mensagem,
        String consultadoEm,
        boolean dadosDesatualizados

) {

    public HobbyLocationsResponseDTO(
            String cidade,
            String estado,
            Double latitude,
            Double longitude,
            List<HobbyLocationDTO> locais,
            String consultadoEm,
            boolean dadosDesatualizados
    ) {
        this(
                cidade,
                estado,
                latitude,
                longitude,
                locais,
                locais == null || locais.isEmpty()
                        ? HobbyLocationSearchStatus.SEM_RESULTADOS
                        : HobbyLocationSearchStatus.SUCESSO,
                locais == null || locais.isEmpty()
                        ? "Nenhum local encontrado para este hobby na região."
                        : "Locais encontrados com sucesso.",
                consultadoEm,
                dadosDesatualizados
        );
    }

    public HobbyLocationsResponseDTO(String cidade, String estado, Double latitude, Double longitude,
                                     List<HobbyLocationDTO> locais, HobbyLocationSearchStatus status, String mensagem) {
        this(cidade, estado, latitude, longitude, locais, status, mensagem, null, false);
    }
}
