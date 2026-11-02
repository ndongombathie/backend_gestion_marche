package com.marchepro.offre.repository;

import com.marchepro.offre.entity.Offre;
import com.marchepro.offre.entity.StatutOffre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OffreRepository extends JpaRepository<Offre, Long> {

    List<Offre> findByFournisseurIdOrderByDateCreationDesc(Long fournisseurId);

    List<Offre> findByAppelOffreIdOrderByMontantAsc(Long appelOffreId);

    List<Offre> findByAppelOffreIdAndStatutNot(Long appelOffreId, StatutOffre statut);

    Optional<Offre> findByAppelOffreIdAndFournisseurId(Long appelOffreId, Long fournisseurId);

    boolean existsByAppelOffreIdAndFournisseurIdAndStatutNot(Long appelOffreId, Long fournisseurId, StatutOffre statut);

    boolean existsByReference(String reference);

    long countByFournisseurId(Long fournisseurId);

    long countByFournisseurIdAndStatutIn(Long fournisseurId, List<StatutOffre> statuts);

    long countByAppelOffreId(Long appelOffreId);

    long countByStatutIn(List<StatutOffre> statuts);

    @Query("SELECT COUNT(o) FROM Offre o WHERE o.appelOffre.marche.id = :marcheId")
    long countByMarcheId(@Param("marcheId") Long marcheId);

    @Query("""
            SELECT o FROM Offre o
            WHERE o.appelOffre.id = :appelOffreId
              AND o.statut NOT IN (com.marchepro.offre.entity.StatutOffre.BROUILLON,
                                   com.marchepro.offre.entity.StatutOffre.DISQUALIFIEE)
            ORDER BY o.montant ASC
            """)
    List<Offre> findRecevables(@Param("appelOffreId") Long appelOffreId);

    @Query("""
            SELECT o FROM Offre o
            WHERE (CAST(:q AS string) IS NULL OR LOWER(o.fournisseur.raisonSociale) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
            ORDER BY o.dateSoumission DESC
            """)
    List<Offre> rechercherPourChatbot(@Param("q") String q);
}
