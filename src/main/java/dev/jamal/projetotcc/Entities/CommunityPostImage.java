package dev.jamal.projetotcc.Entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="community_post_images") @Getter @Setter
public class CommunityPostImage {
    @Id private Long postId;
    @Column(nullable=false,columnDefinition="bytea") private byte[] dados;
}
