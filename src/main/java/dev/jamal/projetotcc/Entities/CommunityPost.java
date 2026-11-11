package dev.jamal.projetotcc.Entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="community_posts") @Getter @Setter
public class CommunityPost {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="community_id",nullable=false)
    private Community community;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id",nullable=false)
    private User user;
    @Column(nullable=false) private UUID clientId;
    @Column(nullable=false,length=64) private String requestHash;
    @Column(nullable=false,length=4000) private String conteudo;
    @Column(nullable=false) private boolean possuiImagem;
    @Column(nullable=false) private Instant createdAt;
    private Instant editedAt;
    private Instant deletedAt;
    private Instant hiddenAt;
    @Version private Long version;
}
