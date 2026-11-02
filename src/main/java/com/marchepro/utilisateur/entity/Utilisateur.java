package com.marchepro.utilisateur.entity;

import com.marchepro.fournisseur.entity.Fournisseur;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "utilisateurs", indexes = {
        @Index(name = "idx_utilisateurs_email", columnList = "email", unique = true),
        @Index(name = "idx_utilisateurs_role", columnList = "role")
})

@Getter
@Setter
@NoArgsConstructor
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nom;

    @Column(nullable = false, length = 80)
    private String prenom;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse;

    @Column(length = 30)
    private String telephone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutUtilisateur statut = StatutUtilisateur.ACTIF;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;

    @Column(name = "reset_token", length = 120)
    private String resetToken;

    @Column(name = "reset_token_expiry")
    private Instant resetTokenExpiry;

    @Column(name = "derniere_connexion")
    private Instant derniereConnexion;

    @OneToOne(mappedBy = "utilisateur", fetch = FetchType.LAZY)
    private Fournisseur fournisseur;

    @PrePersist
    void onCreate() {
        if (dateCreation == null) {
            dateCreation = Instant.now();
        }
        if (statut == null) {
            statut = StatutUtilisateur.ACTIF;
        }
    }

    public String getNomComplet() {
        return prenom + " " + nom;
    }
}
