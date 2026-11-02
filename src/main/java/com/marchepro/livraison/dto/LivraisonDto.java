package com.marchepro.livraison.dto;

import com.marchepro.common.document.DocumentDto;
import com.marchepro.livraison.entity.Livraison;
import com.marchepro.livraison.entity.StatutLivraison;
import com.marchepro.livraison.entity.StatutReception;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record LivraisonDto(
        Long id,
        LocalDate datePrevue,
        LocalDate dateEffective,
        StatutLivraison statut,
        String observations,
        Instant dateCreation,
        Long marcheId,
        String marcheReference,
        String marcheObjet,
        Long fournisseurId,
        String raisonSociale,
        ReceptionDto reception,
        List<LigneLivraisonDto> lignes,
        List<DocumentDto> documents
) {
    public record LigneLivraisonDto(Long id, Long ligneMarcheId, String designation,
                                    int quantiteAttendue, int quantiteLivree) {
    }

    public record ReceptionDto(Long id, LocalDate dateReception, String observations, String reserves,
                               StatutReception statut, String receptionnaire) {
    }

    public static LivraisonDto from(Livraison l, List<DocumentDto> documents) {
        ReceptionDto reception = null;
        if (l.getReception() != null) {
            reception = new ReceptionDto(
                    l.getReception().getId(), l.getReception().getDateReception(),
                    l.getReception().getObservations(), l.getReception().getReserves(),
                    l.getReception().getStatut(),
                    l.getReception().getReceptionnaire() != null
                            ? l.getReception().getReceptionnaire().getNomComplet() : null);
        }
        return new LivraisonDto(
                l.getId(), l.getDatePrevue(), l.getDateEffective(), l.getStatut(), l.getObservations(),
                l.getDateCreation(),
                l.getMarche().getId(), l.getMarche().getReference(), l.getMarche().getObjet(),
                l.getFournisseur().getId(), l.getFournisseur().getRaisonSociale(),
                reception,
                l.getLignes().stream().map(ligne -> new LigneLivraisonDto(
                        ligne.getId(), ligne.getLigneMarche().getId(), ligne.getLigneMarche().getDesignation(),
                        ligne.getLigneMarche().getQuantite(), ligne.getQuantiteLivree())).toList(),
                documents
        );
    }
}
