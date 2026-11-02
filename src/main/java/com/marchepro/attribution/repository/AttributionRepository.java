package com.marchepro.attribution.repository;

import com.marchepro.attribution.entity.Attribution;
import com.marchepro.attribution.entity.StatutAttribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AttributionRepository extends JpaRepository<Attribution, Long> {

    Optional<Attribution> findByMarcheId(Long marcheId);

    List<Attribution> findByStatutOrderByDatePropositionDesc(StatutAttribution statut);

    List<Attribution> findByFournisseurIdAndStatut(Long fournisseurId, StatutAttribution statut);

    long countByStatut(StatutAttribution statut);

    long countByFournisseurIdAndStatut(Long fournisseurId, StatutAttribution statut);

    @Query("""
            SELECT COUNT(a) FROM Attribution a
            WHERE a.fournisseur.id = :fournisseurId
              AND a.statut = com.marchepro.attribution.entity.StatutAttribution.VALIDEE
              AND a.marche.statut = com.marchepro.marche.entity.StatutMarche.EN_EXECUTION
            """)
    long countMarchesEnExecutionFournisseur(@Param("fournisseurId") Long fournisseurId);

    @Query("SELECT COALESCE(SUM(a.montant), 0) FROM Attribution a WHERE a.statut = com.marchepro.attribution.entity.StatutAttribution.VALIDEE")
    BigDecimal totalMontantAttribue();

    @Query("""
            SELECT a.fournisseur.raisonSociale, COUNT(a), COALESCE(SUM(a.montant), 0)
            FROM Attribution a
            WHERE a.statut = com.marchepro.attribution.entity.StatutAttribution.VALIDEE
            GROUP BY a.fournisseur.raisonSociale
            ORDER BY COUNT(a) DESC
            """)
    List<Object[]> classementFournisseurs();
}
