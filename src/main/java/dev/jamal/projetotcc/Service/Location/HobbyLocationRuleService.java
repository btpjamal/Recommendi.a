
package dev.jamal.projetotcc.Service.Location;

import dev.jamal.projetotcc.DTO.Location.*;
import dev.jamal.projetotcc.Entities.Hobby;
import dev.jamal.projetotcc.Entities.HobbyLocationCondition;
import dev.jamal.projetotcc.Entities.HobbyLocationRule;
import dev.jamal.projetotcc.Enum.OsmMatchType;
import dev.jamal.projetotcc.Repository.HobbyLocationRuleRepository;
import dev.jamal.projetotcc.Repository.HobbyRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HobbyLocationRuleService {

    private static final Pattern OSM_TOKEN =
            Pattern.compile("[a-zA-Z0-9_:.-]+");

    private final HobbyLocationRuleRepository ruleRepository;
    private final HobbyRepository hobbyRepository;
    private final CacheManager cacheManager;

    @Transactional(readOnly = true)
    public List<HobbyLocationRuleResponseDTO> listar(Long hobbyId) {
        buscarHobby(hobbyId);

        return ruleRepository.findAllWithConditionsByHobbyId(hobbyId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public HobbyLocationRuleResponseDTO criar(
            Long hobbyId,
            HobbyLocationRuleRequestDTO request
    ) {
        Hobby hobby = buscarHobby(hobbyId);
        validar(request);
        verificarDuplicidade(hobbyId, request, null);

        HobbyLocationRule regra = new HobbyLocationRule();
        regra.setHobby(hobby);

        aplicarDados(regra, request);

        HobbyLocationRule salva = ruleRepository.saveAndFlush(regra);

        invalidarCacheAposCommit();

        return toResponse(salva);
    }

    @Transactional
    public HobbyLocationRuleResponseDTO atualizar(
            Long hobbyId,
            Long ruleId,
            HobbyLocationRuleRequestDTO request
    ) {
        buscarHobby(hobbyId);

        HobbyLocationRule regra = buscarRegra(hobbyId, ruleId);

        validar(request);
        verificarDuplicidade(hobbyId, request, ruleId);

        regra.getConditions().clear();
        aplicarDados(regra, request);

        ruleRepository.flush();

        invalidarCacheAposCommit();

        return toResponse(regra);
    }

    @Transactional
    public HobbyLocationRuleResponseDTO alterarStatus(
            Long hobbyId,
            Long ruleId,
            boolean ativo
    ) {
        HobbyLocationRule regra = buscarRegra(hobbyId, ruleId);

        regra.setAtivo(ativo);

        invalidarCacheAposCommit();

        return toResponse(regra);
    }

    @Transactional
    public void excluir(Long hobbyId, Long ruleId) {
        HobbyLocationRule regra = buscarRegra(hobbyId, ruleId);

        ruleRepository.delete(regra);

        invalidarCacheAposCommit();
    }

    private Hobby buscarHobby(Long hobbyId) {
        return hobbyRepository.findById(hobbyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Hobby não encontrado."
                ));
    }

    private HobbyLocationRule buscarRegra(
            Long hobbyId,
            Long ruleId
    ) {
        HobbyLocationRule regra = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Regra de localização não encontrada."
                ));

        if (!regra.getHobby().getId().equals(hobbyId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Regra não pertence ao hobby informado."
            );
        }

        // Inicializa as condições dentro da transação.
        regra.getConditions().size();

        return regra;
    }

    private void validar(HobbyLocationRuleRequestDTO request) {

        if (request == null
                || request.relevancia() == null
                || request.conditions() == null
                || request.conditions().isEmpty()) {
            throw erro("Informe a relevância e as condições OSM.");
        }

        Set<String> assinaturas = new HashSet<>();

        for (HobbyLocationConditionRequestDTO condicao :
                request.conditions()) {

            if (condicao == null
                    || condicao.matchType() == null) {
                throw erro("Condição OSM inválida.");
            }

            String chave = condicao.osmKey();
            String valor = condicao.osmValue();

            if (!tokenValido(chave)) {
                throw erro("Chave OSM inválida.");
            }

            if (condicao.matchType() == OsmMatchType.IGUAL
                    && !tokenValido(valor)) {
                throw erro("Valor OSM inválido.");
            }

            if (condicao.matchType() == OsmMatchType.EXISTE
                    && valor != null && !valor.isBlank()) {
                throw erro(
                        "Condições EXISTE não devem possuir osmValue."
                );
            }

            String assinatura = assinaturaCondicao(
                    chave,
                    condicao.matchType() == OsmMatchType.EXISTE
                            ? null : valor,
                    condicao.matchType()
            );

            if (!assinaturas.add(assinatura)) {
                throw erro("Existem condições OSM duplicadas.");
            }
        }
    }

    private void verificarDuplicidade(
            Long hobbyId,
            HobbyLocationRuleRequestDTO request,
            Long regraAtualId
    ) {
        Set<String> assinaturaNova = request.conditions()
                .stream()
                .map(c -> assinaturaCondicao(
                        c.osmKey(),
                        c.matchType() == OsmMatchType.EXISTE
                                ? null : c.osmValue(),
                        c.matchType()
                ))
                .collect(Collectors.toSet());

        List<HobbyLocationRule> existentes =
                ruleRepository.findAllWithConditionsByHobbyId(hobbyId);

        for (HobbyLocationRule existente : existentes) {

            if (existente.getId().equals(regraAtualId)) {
                continue;
            }

            Set<String> assinaturaExistente =
                    existente.getConditions()
                            .stream()
                            .map(c -> assinaturaCondicao(
                                    c.getOsmKey(),
                                    c.getOsmValue(),
                                    c.getMatchType()
                            ))
                            .collect(Collectors.toSet());

            if (assinaturaNova.equals(assinaturaExistente)) {
                throw erro(
                        "Já existe uma regra com essas condições para o hobby."
                );
            }
        }
    }

    private void aplicarDados(
            HobbyLocationRule regra,
            HobbyLocationRuleRequestDTO request
    ) {
        regra.setRelevancia(request.relevancia());
        regra.setAtivo(request.ativo());

        for (HobbyLocationConditionRequestDTO dto :
                request.conditions()) {

            HobbyLocationCondition condicao =
                    new HobbyLocationCondition();

            condicao.setOsmKey(dto.osmKey());
            condicao.setOsmValue(
                    dto.matchType() == OsmMatchType.EXISTE
                            ? null : dto.osmValue()
            );
            condicao.setMatchType(dto.matchType());

            regra.adicionarCondicao(condicao);
        }
    }

    private String assinaturaCondicao(
            String chave,
            String valor,
            OsmMatchType tipo
    ) {
        return tipo.name() + "|" + chave + "|"
                + (valor == null ? "" : valor);
    }

    private boolean tokenValido(String valor) {
        return valor != null
                && OSM_TOKEN.matcher(valor).matches();
    }

    private ResponseStatusException erro(String mensagem) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                mensagem
        );
    }

    private HobbyLocationRuleResponseDTO toResponse(
            HobbyLocationRule regra
    ) {
        List<HobbyLocationConditionResponseDTO> condicoes =
                regra.getConditions()
                        .stream()
                        .map(c -> new HobbyLocationConditionResponseDTO(
                                c.getId(),
                                c.getOsmKey(),
                                c.getOsmValue(),
                                c.getMatchType()
                        ))
                        .toList();

        return new HobbyLocationRuleResponseDTO(
                regra.getId(),
                regra.getHobby().getId(),
                regra.getHobby().getNome(),
                regra.getRelevancia(),
                regra.isAtivo(),
                condicoes
        );
    }

    private void invalidarCacheAposCommit() {

        if (!TransactionSynchronizationManager
                .isSynchronizationActive()) {
            throw new IllegalStateException(
                    "Transação não disponível para invalidar o cache."
            );
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        Cache cache = cacheManager.getCache(
                                "hobbyLocations"
                        );

                        if (cache != null) {
                            cache.clear();
                        }
                    }
                }
        );
    }
}
