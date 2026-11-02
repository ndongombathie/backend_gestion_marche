package com.marchepro.materiel.repository;

import com.marchepro.materiel.entity.Materiel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MaterielRepository extends JpaRepository<Materiel, Long> {

    @Query("""
            SELECT m FROM Materiel m
            WHERE m.actif = true
              AND (CAST(:categorie AS string) IS NULL OR m.categorie = :categorie)
              AND (CAST(:q AS string) IS NULL OR LOWER(m.designation) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                   OR LOWER(m.caracteristiques) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
            """)
    Page<Materiel> rechercher(@Param("categorie") String categorie,
                              @Param("q") String q,
                              Pageable pageable);

    List<Materiel> findByDesignationContainingIgnoreCaseAndActifTrue(String designation);

    @Query("SELECT DISTINCT m.categorie FROM Materiel m WHERE m.actif = true ORDER BY m.categorie")
    List<String> listerCategories();
}
