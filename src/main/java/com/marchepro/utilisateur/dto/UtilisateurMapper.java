package com.marchepro.utilisateur.dto;

import com.marchepro.fournisseur.entity.Fournisseur;
import com.marchepro.utilisateur.entity.Utilisateur;
import org.springframework.stereotype.Component;

@Component
public class UtilisateurMapper {

    public UtilisateurDto toDto(Utilisateur utilisateur) {
        return toDto(utilisateur, null);
    }

    public UtilisateurDto toDto(Utilisateur utilisateur, Fournisseur fournisseur) {
        return new UtilisateurDto(
                utilisateur.getId(),
                utilisateur.getNom(),
                utilisateur.getPrenom(),
                utilisateur.getNomComplet(),
                utilisateur.getEmail(),
                utilisateur.getTelephone(),
                utilisateur.getRole(),
                utilisateur.getStatut(),
                utilisateur.getDateCreation(),
                utilisateur.getDerniereConnexion(),
                fournisseur != null ? fournisseur.getId() : null,
                fournisseur != null ? fournisseur.getRaisonSociale() : null,
                fournisseur != null ? fournisseur.getStatutValidation() : null
        );
    }
}
