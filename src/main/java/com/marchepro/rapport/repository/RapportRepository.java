package com.marchepro.rapport.repository;

import com.marchepro.rapport.entity.Rapport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RapportRepository extends JpaRepository<Rapport, Long> {
}
