package com.marchepro.appeloffre.dto;

import com.marchepro.appeloffre.entity.AppelOffre;
import com.marchepro.appeloffre.entity.StatutAppelOffre;
import com.marchepro.common.document.DocumentDto;
import com.marchepro.marche.dto.LigneMarcheDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record AppelOffreDto(
        Long id,
        String reference,
        String objet,
        String description,
        String categorie,
        String organisme,
        BigDecimal budgetEstimatif,
        boolean budgetPublic,
        StatutAppelOffre statut,
        Instant dateCreation,
        Instant datePublication,
        LocalDateTime dateLimite,
        String conditionsParticipation,
        String documentsObligatoires,
        String modalitesSoumission,
        String motifAnnulation,
        Long marcheId,
        String marcheReference,
        String marcheObjet,
        String publiePar,
        long nombreOffres,
        boolean ouvert,
        List<DocumentDto> documents,
        List<DocumentObligatoireDto> documentsObligatoiresDetails,
        List<LigneMarcheDto> materiels
) {
    public static AppelOffreDto from(AppelOffre a, long nombreOffres, List<DocumentDto> documents,
                                     List<DocumentObligatoireDto> documentsObligatoiresDetails) {
        BigDecimal budget = a.isBudgetPublic() && a.getMarche() != null
                ? a.getMarche().getBudgetPrevisionnel() : null;
        return new AppelOffreDto(
                a.getId(), a.getReference(), a.getObjet(), a.getDescription(), a.getCategorie(),
                a.getOrganisme(), budget, a.isBudgetPublic(), a.getStatut(), a.getDateCreation(),
                a.getDatePublication(), a.getDateLimite(), a.getConditionsParticipation(),
                a.getDocumentsObligatoires(), a.getModalitesSoumission(), a.getMotifAnnulation(),
                a.getMarche() != null ? a.getMarche().getId() : null,
                a.getMarche() != null ? a.getMarche().getReference() : null,
                a.getMarche() != null ? a.getMarche().getObjet() : null,
                a.getPubliePar() != null ? a.getPubliePar().getNomComplet() : null,
                nombreOffres,
                a.estOuvert(),
                documents,
                documentsObligatoiresDetails,
                a.getMarche() != null
                        ? a.getMarche().getLignes().stream().map(LigneMarcheDto::from).toList()
                        : List.of()
        );
    }
}
