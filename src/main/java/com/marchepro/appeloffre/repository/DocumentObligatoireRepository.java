package com.marchepro.appeloffre.repository;

import com.marchepro.appeloffre.entity.DocumentObligatoire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentObligatoireRepository extends JpaRepository<DocumentObligatoire, Long> {

    List<DocumentObligatoire> findByAppelOffreIdOrderByOrdreAscIdAsc(Long appelOffreId);

    long countByAppelOffreId(Long appelOffreId);

    boolean existsByAppelOffreIdAndLibelleIgnoreCase(Long appelOffreId, String libelle);

    boolean existsByAppelOffreIdAndLibelleIgnoreCaseAndIdNot(Long appelOffreId, String libelle, Long id);

    void deleteByAppelOffreId(Long appelOffreId);
}
