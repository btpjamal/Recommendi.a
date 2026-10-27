package dev.jamal.projetotcc.Controllers;

import dev.jamal.projetotcc.DTO.Hobby.HobbyCreateRequestDTO;
import dev.jamal.projetotcc.DTO.Hobby.HobbyResponseDTO;
import dev.jamal.projetotcc.Service.HobbyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import dev.jamal.projetotcc.DTO.Hobby.HobbyEstatisticasDTO;

import java.util.List;

import jakarta.validation.Valid;

@Tag(
        name = "Hobbies",
        description = "Gerenciamento dos hobbies disponíveis para recomendação"
)
@RestController
@RequestMapping("/api/v1/hobbies")
@RequiredArgsConstructor
public class HobbyController {

    private final HobbyService hobbyService;

    @Operation(
            summary = "Estatísticas do catálogo",
            description = "Retorna a quantidade total de hobbies ativos e inativos."
    )
    @GetMapping("/estatisticas")
    public ResponseEntity<HobbyEstatisticasDTO> obterEstatisticas() {
        return ResponseEntity.ok(
                hobbyService.obterEstatisticas()
        );
    }

    @Operation(
            summary = "Listar hobbies",
            description = "Retorna todos os hobbies cadastrados no sistema."
    )
    @ApiResponse(responseCode = "200", description = "Hobbies encontrados")
    @GetMapping
    public ResponseEntity<List<HobbyResponseDTO>> listarTodos() {
        return ResponseEntity.ok(
                hobbyService.listarTodos()
        );
    }

    @Operation(
            summary = "Listar hobbies com paginação",
            description = "Retorna hobbies paginados, com busca, filtros e ordenação."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Página de hobbies retornada com sucesso"
    )
    @GetMapping("/paginados")
    public ResponseEntity<Page<HobbyResponseDTO>> listarPaginados(
            @RequestParam(required = false) String nome,
            @RequestParam(defaultValue = "TODOS") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "RECENTES") String ordenacao
    ) {
        return ResponseEntity.ok(
                hobbyService.listarPaginado(
                        nome,
                        status,
                        page,
                        size,
                        ordenacao
                )
        );
    }

    @Operation(
            summary = "Buscar hobby por ID",
            description = "Retorna os dados de um hobby específico."
    )
    @ApiResponse(responseCode = "200", description = "Hobby encontrado")
    @ApiResponse(responseCode = "404", description = "Hobby não encontrado")
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<HobbyResponseDTO> buscarPorId(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                hobbyService.buscarPorId(id)
        );
    }

    @Operation(
            summary = "Cadastrar hobby",
            description = "Cadastra um novo hobby no sistema."
    )
    @ApiResponse(responseCode = "201", description = "Hobby criado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "404", description = "Categoria não encontrada")
    @PostMapping
    public ResponseEntity<HobbyResponseDTO> cadastrar(
            @Valid @RequestBody HobbyCreateRequestDTO dto
    ) {
        HobbyResponseDTO response = hobbyService.salvar(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(
            summary = "Atualizar hobby",
            description = "Atualiza os dados de um hobby existente."
    )
    @ApiResponse(responseCode = "200", description = "Hobby atualizado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "404", description = "Hobby ou categoria não encontrado")
    @PutMapping("/{id}")
    public ResponseEntity<HobbyResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody HobbyCreateRequestDTO dto
    ) {
        return ResponseEntity.ok(
                hobbyService.atualizar(id, dto)
        );
    }

    @Operation(
            summary = "Deletar hobby",
            description = "Remove um hobby pelo ID."
    )
    @ApiResponse(responseCode = "204", description = "Hobby deletado com sucesso")
    @ApiResponse(responseCode = "404", description = "Hobby não encontrado")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id
    ) {
        hobbyService.deletar(id);

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Reativar hobby",
            description = "Reativa um hobby anteriormente desativado."
    )
    @ApiResponse(
            responseCode = "204",
            description = "Hobby reativado com sucesso"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Hobby não encontrado"
    )
    @PatchMapping("/{id}/reativar")
    public ResponseEntity<Void> reativar(
            @PathVariable Long id
    ) {
        hobbyService.reativar(id);

        return ResponseEntity.noContent().build();
    }
}
