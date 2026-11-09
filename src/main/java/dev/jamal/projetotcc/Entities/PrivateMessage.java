package dev.jamal.projetotcc.Entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "social_private_messages") @Getter @Setter
public class PrivateMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long contactId;
    @Column(nullable = false) private Long senderId;
    @Column(nullable = false, length = 1000) private String conteudo;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private UUID clientMessageId;
}
