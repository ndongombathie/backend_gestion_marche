package com.marchepro.rapport.dto;

import com.marchepro.livraison.dto.LivraisonDto;
import com.marchepro.marche.dto.MarcheDto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardDto(
        long totalMarches,
        long appelsOffresPublies,
        BigDecimal montantPrevisionnel,
        BigDecimal montantAttribue,
        long fournisseursActifs,
        long offresRecues,
        long livraisonsEnRetard,
        double tauxLivraisonDelais,
        List<Repartition> marchesParStatut,
        List<Repartition> marchesParCategorie,
        List<PointMois> evolutionMensuelle,
        List<ClassementFournisseur> classementFournisseurs,
        List<MarcheDto> marchesRecents,
        List<LivraisonDto> livraisonsEnRetardListe
) {
    public record Repartition(String label, long valeur) {
    }

    public record PointMois(String mois, long total) {
    }

    public record ClassementFournisseur(String fournisseur, long marches, BigDecimal montant) {
    }
}
