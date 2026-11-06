package dev.jamal.projetotcc.Repository;

import dev.jamal.projetotcc.Entities.HobbyImageUpload;
import dev.jamal.projetotcc.Enum.HobbyImageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HobbyImageUploadRepository
        extends JpaRepository<HobbyImageUpload, Long> {

    Optional<HobbyImageUpload> findByPublicId(String publicId);

    boolean existsByPublicId(String publicId);

    List<HobbyImageUpload> findByStatus(HobbyImageStatus status);

    List<HobbyImageUpload> findByHobbyId(Long hobbyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT u
        FROM HobbyImageUpload u
        WHERE u.publicId = :publicId
        """)
    Optional<HobbyImageUpload> findByPublicIdForUpdate(
            @Param("publicId") String publicId
    );
}