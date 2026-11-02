package com.marchepro.livraison.repository;

import com.marchepro.livraison.entity.Livraison;
import com.marchepro.livraison.entity.StatutLivraison;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LivraisonRepository extends JpaRepository<Livraison, Long> {

    List<Livraison> findByMarcheIdOrderByDatePrevueAsc(Long marcheId);

    List<Livraison> findByFournisseurIdOrderByDatePrevueDesc(Long fournisseurId);

    List<Livraison> findAllByOrderByDatePrevueDesc();

    long countByStatut(StatutLivraison statut);

    long countByFournisseurId(Long fournisseurId);

    long countByFournisseurIdAndStatut(Long fournisseurId, StatutLivraison statut);

    @Query("""
            SELECT l FROM Livraison l
            WHERE l.datePrevue < :aujourdhui
              AND l.statut NOT IN (com.marchepro.livraison.entity.StatutLivraison.RECEPTIONNEE,
                                   com.marchepro.livraison.entity.StatutLivraison.LIVREE)
            ORDER BY l.datePrevue ASC
            """)
    List<Livraison> findEnRetard(@Param("aujourdhui") LocalDate aujourdhui);

    @Query("""
            SELECT COUNT(l) FROM Livraison l
            WHERE l.statut = com.marchepro.livraison.entity.StatutLivraison.RECEPTIONNEE
              AND l.dateEffective IS NOT NULL
              AND l.dateEffective <= l.datePrevue
            """)
    long countLivraisonsDansDelais();

    long countByStatutIn(List<StatutLivraison> statuts);
}
