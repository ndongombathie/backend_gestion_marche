package com.marchepro.fournisseur.dto;

import com.marchepro.fournisseur.entity.Fournisseur;
import com.marchepro.fournisseur.entity.StatutValidation;

import java.time.Instant;

public record FournisseurDto(
        Long id,
        String raisonSociale,
        String ninea,
        String rccm,
        String adresse,
        String telephone,
        String email,
        String representant,
        StatutValidation statutValidation,
        String motifRejet,
        Instant dateCreation,
        Long utilisateurId,
        String emailUtilisateur,
        String nomRepresentant
) {
    public static FournisseurDto from(Fournisseur f) {
        return new FournisseurDto(
                f.getId(), f.getRaisonSociale(), f.getNinea(), f.getRccm(), f.getAdresse(),
                f.getTelephone(), f.getEmail(), f.getRepresentant(), f.getStatutValidation(),
                f.getMotifRejet(), f.getDateCreation(),
                f.getUtilisateur() != null ? f.getUtilisateur().getId() : null,
                f.getUtilisateur() != null ? f.getUtilisateur().getEmail() : null,
                f.getUtilisateur() != null ? f.getUtilisateur().getNomComplet() : null
        );
    }
}
