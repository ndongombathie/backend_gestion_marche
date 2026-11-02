package com.marchepro.config;

import com.marchepro.appeloffre.entity.AppelOffre;
import com.marchepro.appeloffre.entity.DocumentObligatoire;
import com.marchepro.appeloffre.repository.AppelOffreRepository;
import com.marchepro.appeloffre.repository.DocumentObligatoireRepository;
import com.marchepro.common.document.DocumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "app.documents-obligatoires.initialiser",
        havingValue = "true", matchIfMissing = true)
public class DocumentObligatoireInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DocumentObligatoireInitializer.class);

    private final AppelOffreRepository appelOffreRepository;
    private final DocumentObligatoireRepository documentObligatoireRepository;

    public DocumentObligatoireInitializer(AppelOffreRepository appelOffreRepository,
                                          DocumentObligatoireRepository documentObligatoireRepository) {
        this.appelOffreRepository = appelOffreRepository;
        this.documentObligatoireRepository = documentObligatoireRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int initialises = 0;
        for (AppelOffre appelOffre : appelOffreRepository.findAll()) {
            if (documentObligatoireRepository.countByAppelOffreId(appelOffre.getId()) > 0) {
                continue;
            }
            List<String> libelles = libelles(appelOffre.getDocumentsObligatoires());
            int ordre = 1;
            for (String libelle : libelles) {
                DocumentObligatoire document = new DocumentObligatoire();
                document.setAppelOffre(appelOffre);
                document.setLibelle(libelle);
                document.setType(type(libelle));
                document.setObligatoire(true);
                document.setOrdre(ordre++);
                documentObligatoireRepository.save(document);
            }
            initialises++;
        }
        if (initialises > 0) {
            log.info("Documents obligatoires initialisés pour {} appel(s) d'offres existant(s).", initialises);
        }
    }

    private List<String> libelles(String documentsObligatoires) {
        Set<String> libelles = new LinkedHashSet<>();
        if (documentsObligatoires != null && !documentsObligatoires.isBlank()) {
            for (String partie : documentsObligatoires.split("[,;\\n]")) {
                String libelle = partie.trim().replaceAll("\\.$", "");
                if (!libelle.isBlank()) {
                    libelles.add(libelle);
                }
            }
        }
        if (libelles.isEmpty()) {
            libelles.addAll(List.of("Offre technique", "Offre financière", "Documents administratifs"));
        }
        return new ArrayList<>(libelles);
    }

    private DocumentType type(String libelle) {
        String valeur = libelle.toLowerCase(Locale.ROOT);
        if (valeur.contains("technique")) {
            return DocumentType.OFFRE_TECHNIQUE;
        }
        if (valeur.contains("financ")) {
            return DocumentType.OFFRE_FINANCIERE;
        }
        if (valeur.contains("administrat")) {
            return DocumentType.DOCUMENT_ADMINISTRATIF;
        }
        return DocumentType.AUTRE;
    }
}
