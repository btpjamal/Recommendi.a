package dev.jamal.projetotcc.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "community_messages",
        indexes = {
                @Index(
                        name = "idx_community_message_community_date",
                        columnList = "community_id, data_envio"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommunityMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "community_id",
            nullable = false
    )
    private Community community;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;


    @Column(
            nullable = false,
            length = 1000
    )
    private String conteudo;


    @Column(
            name = "data_envio",
            nullable = false,
            updatable = false
    )
    private LocalDateTime dataEnvio;


    @Column(name = "client_message_id", updatable = false)
    private java.util.UUID clientMessageId;

    @PrePersist
    public void prePersist() {
        dataEnvio = LocalDateTime.now();
    }
}