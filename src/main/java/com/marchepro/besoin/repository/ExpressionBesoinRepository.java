package com.marchepro.besoin.repository;

import com.marchepro.besoin.entity.ExpressionBesoin;
import com.marchepro.besoin.entity.StatutBesoin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ExpressionBesoinRepository extends JpaRepository<ExpressionBesoin, Long> {

    Optional<ExpressionBesoin> findByReference(String reference);

    long countByStatut(StatutBesoin statut);

    long countByCreateurId(Long createurId);

    @Query("""
            SELECT e FROM ExpressionBesoin e
            WHERE (:createurId IS NULL OR e.createur.id = :createurId)
              AND (:statut IS NULL OR e.statut = :statut)
              AND (CAST(:q AS string) IS NULL OR LOWER(e.objet) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                   OR LOWER(e.reference) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
            """)
    Page<ExpressionBesoin> rechercher(@Param("createurId") Long createurId,
                                      @Param("statut") StatutBesoin statut,
                                      @Param("q") String q,
                                      Pageable pageable);

    @Query("SELECT e FROM ExpressionBesoin e WHERE e.statut = com.marchepro.besoin.entity.StatutBesoin.VALIDEE ORDER BY e.dateValidation DESC")
    Page<ExpressionBesoin> findValidees(Pageable pageable);
}
