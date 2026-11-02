package com.marchepro.common.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByEntiteTypeAndEntiteIdOrderByDateUploadDesc(String entiteType, Long entiteId);

    boolean existsByEntiteTypeAndEntiteIdAndType(String entiteType, Long entiteId, DocumentType type);

    long countByEntiteTypeAndEntiteIdAndType(String entiteType, Long entiteId, DocumentType type);

    void deleteByEntiteTypeAndEntiteId(String entiteType, Long entiteId);
}
