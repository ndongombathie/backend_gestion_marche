package com.marchepro.appeloffre.repository;

import com.marchepro.appeloffre.entity.AppelOffre;
import com.marchepro.appeloffre.entity.StatutAppelOffre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppelOffreRepository extends JpaRepository<AppelOffre, Long> {

    Optional<AppelOffre> findByReference(String reference);

    Optional<AppelOffre> findByMarcheId(Long marcheId);

    long countByStatut(StatutAppelOffre statut);

    java.util.List<AppelOffre> findByStatutAndDateLimiteBefore(StatutAppelOffre statut, LocalDateTime date);

    java.util.List<AppelOffre> findByStatutInOrderByDateLimiteDesc(java.util.List<StatutAppelOffre> statuts);

    @Query("""
            SELECT a FROM AppelOffre a
            WHERE (:statut IS NULL OR a.statut = :statut)
              AND (CAST(:q AS string) IS NULL OR LOWER(a.objet) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                   OR LOWER(a.reference) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
            ORDER BY a.dateCreation DESC
            """)
    Page<AppelOffre> rechercherTous(@Param("statut") StatutAppelOffre statut,
                                    @Param("q") String q,
                                    Pageable pageable);

    @Query("""
            SELECT a FROM AppelOffre a
            WHERE a.statut = com.marchepro.appeloffre.entity.StatutAppelOffre.PUBLIE
              AND (CAST(:q AS string) IS NULL OR LOWER(a.objet) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                   OR LOWER(a.reference) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                   OR LOWER(a.description) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
              AND (CAST(:categorie AS string) IS NULL OR a.categorie = :categorie)
              AND (CAST(:organisme AS string) IS NULL OR a.organisme = :organisme)
              AND (CAST(:dateLimiteApres AS timestamp) IS NULL OR a.dateLimite >= :dateLimiteApres)
              AND (CAST(:dateLimiteAvant AS timestamp) IS NULL OR a.dateLimite <= :dateLimiteAvant)
            """)
    Page<AppelOffre> rechercherPublics(@Param("q") String q,
                                       @Param("categorie") String categorie,
                                       @Param("organisme") String organisme,
                                       @Param("dateLimiteApres") LocalDateTime dateLimiteApres,
                                       @Param("dateLimiteAvant") LocalDateTime dateLimiteAvant,
                                       Pageable pageable);

    @Query("""
            SELECT a FROM AppelOffre a
            WHERE a.statut = com.marchepro.appeloffre.entity.StatutAppelOffre.PUBLIE
              AND a.dateLimite > :maintenant
            ORDER BY a.datePublication DESC
            """)
    Page<AppelOffre> findRecents(@Param("maintenant") LocalDateTime maintenant, Pageable pageable);

    @Query("""
            SELECT a FROM AppelOffre a
            WHERE a.statut = com.marchepro.appeloffre.entity.StatutAppelOffre.PUBLIE
              AND a.dateLimite > :maintenant
              AND a.dateLimite <= :limite
            ORDER BY a.dateLimite ASC
            """)
    Page<AppelOffre> findBientotExpires(@Param("maintenant") LocalDateTime maintenant,
                                        @Param("limite") LocalDateTime limite,
                                        Pageable pageable);

    @Query("SELECT DISTINCT a.organisme FROM AppelOffre a WHERE a.statut = com.marchepro.appeloffre.entity.StatutAppelOffre.PUBLIE AND a.organisme IS NOT NULL ORDER BY a.organisme")
    List<String> listerOrganismes();

    @Query("SELECT DISTINCT a.categorie FROM AppelOffre a WHERE a.statut = com.marchepro.appeloffre.entity.StatutAppelOffre.PUBLIE AND a.categorie IS NOT NULL ORDER BY a.categorie")
    List<String> listerCategories();

    @Query("""
            SELECT a FROM AppelOffre a
            WHERE a.statut = com.marchepro.appeloffre.entity.StatutAppelOffre.PUBLIE
              AND (CAST(:q AS string) IS NULL OR LOWER(a.objet) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
            ORDER BY a.dateLimite ASC
            """)
    List<AppelOffre> rechercherPourChatbot(@Param("q") String q, Pageable pageable);
}
