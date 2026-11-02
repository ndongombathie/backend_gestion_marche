package com.marchepro.evaluation.repository;

import com.marchepro.evaluation.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    List<Evaluation> findByOffreId(Long offreId);

    Optional<Evaluation> findByOffreIdAndMembreId(Long offreId, Long membreId);

    List<Evaluation> findByOffreAppelOffreId(Long appelOffreId);

    boolean existsByOffreIdAndMembreId(Long offreId, Long membreId);

    long countByMembreId(Long membreId);
}
