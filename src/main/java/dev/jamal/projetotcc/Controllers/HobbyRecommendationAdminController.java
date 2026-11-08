package dev.jamal.projetotcc.Controllers;

import dev.jamal.projetotcc.DTO.Hobby.HobbyRecommendationAssociationsDTO;
import dev.jamal.projetotcc.Service.HobbyRecommendationAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/hobbies")
public class HobbyRecommendationAdminController {

    private final HobbyRecommendationAdminService service;

    @GetMapping("/{hobbyId}/recommendation-associations")
    public HobbyRecommendationAssociationsDTO buscar(@PathVariable Long hobbyId) {
        return service.buscar(hobbyId);
    }
}