package com.marchepro.common.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoriqueActionRepository extends JpaRepository<HistoriqueAction, Long> {

    Page<HistoriqueAction> findByEntiteAndEntiteIdOrderByDateActionDesc(String entite, Long entiteId, Pageable pageable);

    Page<HistoriqueAction> findAllByOrderByDateActionDesc(Pageable pageable);
}
