package com.marchepro.utilisateur.repository;

import com.marchepro.utilisateur.entity.Role;
import com.marchepro.utilisateur.entity.StatutUtilisateur;
import com.marchepro.utilisateur.entity.Utilisateur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByRoleAndStatut(Role role, StatutUtilisateur statut);

    long countByRole(Role role);

    Optional<Utilisateur> findByResetToken(String resetToken);

    List<Utilisateur> findByRoleAndStatut(Role role, StatutUtilisateur statut);

    List<Utilisateur> findByRole(Role role);

    @Query("""
            SELECT u FROM Utilisateur u
            WHERE (:role IS NULL OR u.role = :role)
              AND (:statut IS NULL OR u.statut = :statut)
              AND (CAST(:q AS string) IS NULL OR LOWER(CONCAT(u.prenom, ' ', u.nom)) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
            """)
    Page<Utilisateur> rechercher(@Param("role") Role role,
                                 @Param("statut") StatutUtilisateur statut,
                                 @Param("q") String q,
                                 Pageable pageable);
}
