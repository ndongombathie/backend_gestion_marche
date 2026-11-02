package com.marchepro.chatbot.entity;

import com.marchepro.utilisateur.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "recherches_chatbot")
@Getter
@Setter
@NoArgsConstructor
public class RechercheChatbot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String question;

    @Column(length = 4000)
    private String reponse;

    @Column(length = 60)
    private String intention;

    @Column(name = "date_recherche", nullable = false, updatable = false)
    private Instant dateRecherche;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @PrePersist
    void onCreate() {
        if (dateRecherche == null) {
            dateRecherche = Instant.now();
        }
    }
}
