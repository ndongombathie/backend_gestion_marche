package com.marchepro.config;

import com.marchepro.common.document.DocumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class DocumentTypeConstraintInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DocumentTypeConstraintInitializer.class);
    private static final String CONTRAINTE = "documents_type_check";

    private final JdbcTemplate jdbcTemplate;

    public DocumentTypeConstraintInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            String definition = jdbcTemplate.query(
                    "select pg_get_constraintdef(oid) from pg_constraint "
                            + "where conrelid = 'documents'::regclass and conname = ?",
                    rs -> rs.next() ? rs.getString(1) : null,
                    CONTRAINTE);
            if (definition == null || Arrays.stream(DocumentType.values())
                    .allMatch(type -> definition.contains(type.name()))) {
                return;
            }
            String valeurs = Arrays.stream(DocumentType.values())
                    .map(type -> "'" + type.name() + "'")
                    .collect(Collectors.joining(", "));
            jdbcTemplate.execute("alter table documents drop constraint if exists " + CONTRAINTE);
            jdbcTemplate.execute("alter table documents add constraint " + CONTRAINTE
                    + " check (type in (" + valeurs + "))");
            log.info("Contrainte {} mise à jour avec les nouveaux types de documents.", CONTRAINTE);
        } catch (Exception ex) {
            log.warn("Impossible de mettre à jour la contrainte {} : {}", CONTRAINTE, ex.getMessage());
        }
    }
}
