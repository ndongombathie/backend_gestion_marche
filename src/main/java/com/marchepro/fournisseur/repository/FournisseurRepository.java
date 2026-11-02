package com.marchepro.fournisseur.repository;

import com.marchepro.fournisseur.entity.Fournisseur;
import com.marchepro.fournisseur.entity.StatutValidation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FournisseurRepository extends JpaRepository<Fournisseur, Long> {

    Optional<Fournisseur> findByUtilisateurId(Long utilisateurId);

    boolean existsByNineaIgnoreCase(String ninea);

    long countByStatutValidation(StatutValidation statut);

    java.util.List<Fournisseur> findByStatutValidation(StatutValidation statut);

    @Query("""
            SELECT f FROM Fournisseur f
            WHERE (:statut IS NULL OR f.statutValidation = :statut)
              AND (CAST(:q AS string) IS NULL OR LOWER(f.raisonSociale) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                   OR LOWER(f.ninea) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
            """)
    Page<Fournisseur> rechercher(@Param("statut") StatutValidation statut,
                                 @Param("q") String q,
                                 Pageable pageable);
}
