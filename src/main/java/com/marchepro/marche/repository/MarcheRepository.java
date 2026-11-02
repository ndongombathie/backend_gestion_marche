package com.marchepro.marche.repository;

import com.marchepro.marche.entity.Marche;
import com.marchepro.marche.entity.StatutMarche;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public interface MarcheRepository extends JpaRepository<Marche, Long> {

    Optional<Marche> findByReference(String reference);

    Optional<Marche> findByExpressionBesoinId(Long expressionBesoinId);

    long countByStatut(StatutMarche statut);

    @Query("""
            SELECT m FROM Marche m
            WHERE (:statut IS NULL OR m.statut = :statut)
              AND (CAST(:q AS string) IS NULL OR LOWER(m.objet) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                   OR LOWER(m.reference) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
              AND (CAST(:debut AS timestamp) IS NULL OR m.dateCreation >= :debut)
              AND (CAST(:fin AS timestamp) IS NULL OR m.dateCreation <= :fin)
            """)
    Page<Marche> rechercher(@Param("statut") StatutMarche statut,
                            @Param("q") String q,
                            @Param("debut") Instant debut,
                            @Param("fin") Instant fin,
                            Pageable pageable);

    @Query("SELECT COALESCE(SUM(m.budgetPrevisionnel), 0) FROM Marche m")
    BigDecimal totalBudgetPrevisionnel();

    @Query("SELECT COALESCE(SUM(m.budgetPrevisionnel), 0) FROM Marche m WHERE m.dateCreation >= :debut AND m.dateCreation <= :fin")
    BigDecimal totalBudgetPrevisionnelPeriode(@Param("debut") Instant debut, @Param("fin") Instant fin);

    @Query("""
            SELECT m.statut, COUNT(m) FROM Marche m
            GROUP BY m.statut
            """)
    java.util.List<Object[]> repartitionParStatut();

    @Query(value = """
            SELECT TO_CHAR(date_creation, 'YYYY-MM') AS mois, COUNT(*) AS total
            FROM marches
            WHERE date_creation >= :debut
            GROUP BY TO_CHAR(date_creation, 'YYYY-MM')
            ORDER BY mois
            """, nativeQuery = true)
    java.util.List<Object[]> evolutionMensuelle(@Param("debut") Instant debut);

    @Query(value = """
            SELECT categorie, COUNT(*) AS total
            FROM appels_offres
            WHERE categorie IS NOT NULL
            GROUP BY categorie
            ORDER BY total DESC
            """, nativeQuery = true)
    java.util.List<Object[]> repartitionParCategorie();
}
