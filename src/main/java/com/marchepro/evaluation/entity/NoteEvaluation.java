package com.marchepro.evaluation.entity;

import com.marchepro.marche.entity.CritereEvaluation;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "notes_evaluation")
@Getter
@Setter
@NoArgsConstructor
public class NoteEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private Evaluation evaluation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "critere_id", nullable = false)
    private CritereEvaluation critere;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal note;

    @Column(length = 1000)
    private String commentaire;
}
