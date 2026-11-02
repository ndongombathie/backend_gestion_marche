package com.marchepro.evaluation.entity;

import com.marchepro.marche.entity.CritereEvaluation;
import com.marchepro.offre.entity.Offre;
import com.marchepro.utilisateur.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evaluations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_evaluation_offre_membre", columnNames = {"offre_id", "membre_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offre_id", nullable = false)
    private Offre offre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Utilisateur membre;

    @Column(name = "note_technique", precision = 6, scale = 2)
    private BigDecimal noteTechnique;

    @Column(name = "note_financiere", precision = 6, scale = 2)
    private BigDecimal noteFinanciere;

    @Column(name = "note_globale", precision = 6, scale = 2)
    private BigDecimal noteGlobale;

    @Column(length = 2000)
    private String observations;

    @Column(name = "date_evaluation", nullable = false, updatable = false)
    private Instant dateEvaluation;

    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<NoteEvaluation> notes = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (dateEvaluation == null) {
            dateEvaluation = Instant.now();
        }
    }

    public void ajouterNote(NoteEvaluation note) {
        note.setEvaluation(this);
        notes.add(note);
    }
}
