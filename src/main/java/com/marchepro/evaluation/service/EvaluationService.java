package com.marchepro.evaluation.service;

import com.marchepro.appeloffre.entity.AppelOffre;
import com.marchepro.appeloffre.entity.StatutAppelOffre;
import com.marchepro.appeloffre.repository.AppelOffreRepository;
import com.marchepro.common.audit.AuditService;
import com.marchepro.common.exception.BusinessException;
import com.marchepro.common.exception.ResourceNotFoundException;
import com.marchepro.evaluation.dto.ConsolidationDto;
import com.marchepro.evaluation.dto.EvaluationDto;
import com.marchepro.evaluation.dto.EvaluationRequest;
import com.marchepro.evaluation.dto.NoteEvaluationRequest;
import com.marchepro.evaluation.entity.Evaluation;
import com.marchepro.evaluation.entity.NoteEvaluation;
import com.marchepro.evaluation.repository.EvaluationRepository;
import com.marchepro.marche.entity.CritereEvaluation;
import com.marchepro.marche.entity.TypeCritere;
import com.marchepro.marche.service.MarcheService;
import com.marchepro.offre.entity.Offre;
import com.marchepro.offre.entity.StatutOffre;
import com.marchepro.offre.repository.OffreRepository;
import com.marchepro.security.SecurityUtils;
import com.marchepro.utilisateur.entity.Utilisateur;
import com.marchepro.utilisateur.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final OffreRepository offreRepository;
    private final AppelOffreRepository appelOffreRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final MarcheService marcheService;
    private final AuditService auditService;

    public EvaluationService(EvaluationRepository evaluationRepository,
                             OffreRepository offreRepository,
                             AppelOffreRepository appelOffreRepository,
                             UtilisateurRepository utilisateurRepository,
                             MarcheService marcheService,
                             AuditService auditService) {
        this.evaluationRepository = evaluationRepository;
        this.offreRepository = offreRepository;
        this.appelOffreRepository = appelOffreRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.marcheService = marcheService;
        this.auditService = auditService;
    }

    @Transactional
    public EvaluationDto enregistrer(EvaluationRequest request) {
        Offre offre = offreRepository.findById(request.offreId())
                .orElseThrow(() -> new ResourceNotFoundException("Offre introuvable"));
        if (offre.getStatut() == StatutOffre.BROUILLON
                || offre.getStatut() == StatutOffre.DISQUALIFIEE
                || offre.getStatut() == StatutOffre.RETENUE
                || offre.getStatut() == StatutOffre.NON_RETENUE) {
            throw new BusinessException("Cette offre n'est pas éligible à une évaluation");
        }
        AppelOffre appelOffre = offre.getAppelOffre();
        if (appelOffre.getStatut() == StatutAppelOffre.ANNULE) {
            throw new BusinessException("Cet appel d'offres est annulé");
        }
        Utilisateur membre = utilisateurRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        Map<Long, CritereEvaluation> criteres = appelOffre.getMarche().getCriteres().stream()
                .collect(Collectors.toMap(CritereEvaluation::getId, Function.identity()));

        Evaluation evaluation = evaluationRepository
                .findByOffreIdAndMembreId(offre.getId(), membre.getId())
                .orElseGet(Evaluation::new);
        evaluation.setOffre(offre);
        evaluation.setMembre(membre);
        evaluation.setObservations(request.observations());
        evaluation.getNotes().clear();

        BigDecimal totalTechnique = BigDecimal.ZERO;
        BigDecimal totalFinancier = BigDecimal.ZERO;
        for (NoteEvaluationRequest noteRequest : request.notes()) {
            CritereEvaluation critere = criteres.get(noteRequest.critereId());
            if (critere == null) {
                throw new BusinessException("Critère d'évaluation invalide : " + noteRequest.critereId());
            }
            NoteEvaluation note = new NoteEvaluation();
            note.setCritere(critere);
            note.setNote(noteRequest.note());
            note.setCommentaire(noteRequest.commentaire());
            evaluation.ajouterNote(note);

            BigDecimal pondere = noteRequest.note()
                    .multiply(BigDecimal.valueOf(critere.getPonderation()))
                    .divide(BigDecimal.valueOf(20), 4, RoundingMode.HALF_UP);
            if (critere.getType() == TypeCritere.TECHNIQUE) {
                totalTechnique = totalTechnique.add(pondere);
            } else {
                totalFinancier = totalFinancier.add(pondere);
            }
        }
        evaluation.setNoteTechnique(totalTechnique.setScale(2, RoundingMode.HALF_UP));
        evaluation.setNoteFinanciere(totalFinancier.setScale(2, RoundingMode.HALF_UP));
        evaluation.setNoteGlobale(totalTechnique.add(totalFinancier).setScale(2, RoundingMode.HALF_UP));
        evaluationRepository.save(evaluation);

        if (offre.getStatut() == StatutOffre.SOUMISE) {
            offre.setStatut(StatutOffre.EN_EVALUATION);
            offreRepository.save(offre);
        }
        marcheService.marquerEnEvaluation(appelOffre.getMarche());
        auditService.enregistrer("EVALUATION_OFFRE", "OFFRE", offre.getId(),
                "Note globale " + evaluation.getNoteGlobale() + " par " + membre.getNomComplet());
        return EvaluationDto.from(evaluation);
    }

    @Transactional(readOnly = true)
    public List<EvaluationDto> evaluationsParOffre(Long offreId) {
        return evaluationRepository.findByOffreId(offreId).stream()
                .map(EvaluationDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AppelOffre> appelsOffresAEvaluer() {
        return appelOffreRepository.findByStatutInOrderByDateLimiteDesc(List.of(
                StatutAppelOffre.PUBLIE, StatutAppelOffre.CLOTURE));
    }

    @Transactional(readOnly = true)
    public ConsolidationDto consolider(Long appelOffreId) {
        AppelOffre appelOffre = appelOffreRepository.findById(appelOffreId)
                .orElseThrow(() -> new ResourceNotFoundException("Appel d'offres introuvable"));
        List<Offre> offres = offreRepository.findRecevables(appelOffreId);
        List<ConsolidationDto.ClassementOffreDto> lignes = new ArrayList<>();

        for (Offre offre : offres) {
            List<Evaluation> evaluations = evaluationRepository.findByOffreId(offre.getId());
            BigDecimal noteTech = moyenne(evaluations, Evaluation::getNoteTechnique);
            BigDecimal noteFin = moyenne(evaluations, Evaluation::getNoteFinanciere);
            BigDecimal noteGlobale = moyenne(evaluations, Evaluation::getNoteGlobale);
            lignes.add(new ConsolidationDto.ClassementOffreDto(
                    0, offre.getId(), offre.getReference(), offre.getFournisseur().getRaisonSociale(),
                    offre.getMontant(), noteTech, noteFin, noteGlobale, evaluations.size(),
                    offre.getStatut() == StatutOffre.RETENUE));
        }

        lignes.sort(Comparator.comparing(ConsolidationDto.ClassementOffreDto::noteGlobale).reversed()
                .thenComparing(ConsolidationDto.ClassementOffreDto::montant));
        List<ConsolidationDto.ClassementOffreDto> classement = new ArrayList<>();
        for (int i = 0; i < lignes.size(); i++) {
            ConsolidationDto.ClassementOffreDto l = lignes.get(i);
            classement.add(new ConsolidationDto.ClassementOffreDto(
                    i + 1, l.offreId(), l.offreReference(), l.fournisseur(), l.montant(),
                    l.noteTechnique(), l.noteFinanciere(), l.noteGlobale(), l.nombreEvaluations(), l.retenue()));
        }
        return new ConsolidationDto(appelOffre.getId(), appelOffre.getReference(), appelOffre.getObjet(),
                appelOffre.getMarche().getStatut().name(), classement);
    }

    private BigDecimal moyenne(List<Evaluation> evaluations, Function<Evaluation, BigDecimal> extractor) {
        if (evaluations.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal somme = evaluations.stream()
                .map(extractor)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return somme.divide(BigDecimal.valueOf(evaluations.size()), 2, RoundingMode.HALF_UP);
    }
}
