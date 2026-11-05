package dev.jamal.projetotcc.DTO.Recommendation;
import lombok.*; import java.util.List;
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class HobbyRecommendationDTO {
 private Long hobbyId;
 private String nome;
 private String descricao;
 private String categoria;
 private Double score;
 private List<String> motivos;
 private List<String> alertas;
 private List<CriterionScoreDTO> criterios;
 private String imagemUrl;


}
