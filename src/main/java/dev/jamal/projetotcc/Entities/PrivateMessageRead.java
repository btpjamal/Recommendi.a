package dev.jamal.projetotcc.Entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "social_private_reads") @Getter @Setter
public class PrivateMessageRead {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long contactId;
    @Column(nullable = false) private Long userId;
    @Column(nullable = false) private Long lastMessageId;
}
