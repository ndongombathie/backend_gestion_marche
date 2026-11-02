package com.marchepro.common.audit;

import com.marchepro.utilisateur.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "historique_actions", indexes = {
        @Index(name = "idx_historique_entite", columnList = "entite, entite_id"),
        @Index(name = "idx_historique_date", columnList = "date_action")
})
@Getter
@Setter
@NoArgsConstructor
public class HistoriqueAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(nullable = false, length = 60)
    private String entite;

    @Column(name = "entite_id")
    private Long entiteId;

    @Column(length = 2000)
    private String details;

    @Column(name = "date_action", nullable = false, updatable = false)
    private Instant dateAction;

    @PrePersist
    void onCreate() {
        if (dateAction == null) {
            dateAction = Instant.now();
        }
    }
}
