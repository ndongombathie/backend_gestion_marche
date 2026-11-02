package com.marchepro.utilisateur.dto;

import com.marchepro.fournisseur.entity.StatutValidation;
import com.marchepro.utilisateur.entity.Role;
import com.marchepro.utilisateur.entity.StatutUtilisateur;

import java.time.Instant;

public record UtilisateurDto(
        Long id,
        String nom,
        String prenom,
        String nomComplet,
        String email,
        String telephone,
        Role role,
        StatutUtilisateur statut,
        Instant dateCreation,
        Instant derniereConnexion,
        Long fournisseurId,
        String raisonSociale,
        StatutValidation fournisseurStatut
) {
}
