package com.marchepro.attribution.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.marchepro.attribution.dto.AttributionDto;
import com.marchepro.attribution.dto.AttributionRequest;
import com.marchepro.attribution.entity.Attribution;
import com.marchepro.attribution.entity.StatutAttribution;
import com.marchepro.attribution.repository.AttributionRepository;
import com.marchepro.common.audit.AuditService;
import com.marchepro.common.dto.DecisionRequest;
import com.marchepro.common.exception.BusinessException;
import com.marchepro.common.exception.ConflictException;
import com.marchepro.common.exception.ResourceNotFoundException;
import com.marchepro.evaluation.repository.EvaluationRepository;
import com.marchepro.marche.entity.Marche;
import com.marchepro.marche.service.MarcheService;
import com.marchepro.notification.service.NotificationService;
import com.marchepro.offre.entity.Offre;
import com.marchepro.offre.entity.StatutOffre;
import com.marchepro.offre.repository.OffreRepository;
import com.marchepro.security.SecurityUtils;
import com.marchepro.utilisateur.entity.Utilisateur;
import com.marchepro.utilisateur.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AttributionService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Africa/Dakar"));

    private final AttributionRepository attributionRepository;
    private final OffreRepository offreRepository;
    private final EvaluationRepository evaluationRepository;
    private final MarcheService marcheService;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public AttributionService(AttributionRepository attributionRepository,
                              OffreRepository offreRepository,
                              EvaluationRepository evaluationRepository,
                              MarcheService marcheService,
                              UtilisateurRepository utilisateurRepository,
                              NotificationService notificationService,
                              AuditService auditService) {
        this.attributionRepository = attributionRepository;
        this.offreRepository = offreRepository;
        this.evaluationRepository = evaluationRepository;
        this.marcheService = marcheService;
        this.utilisateurRepository = utilisateurRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Transactional
    public AttributionDto proposer(AttributionRequest request) {
        Offre offre = offreRepository.findById(request.offreId())
                .orElseThrow(() -> new ResourceNotFoundException("Offre introuvable"));
        if (offre.getStatut() == StatutOffre.BROUILLON || offre.getStatut() == StatutOffre.DISQUALIFIEE) {
            throw new BusinessException("Cette offre ne peut pas être proposée à l'attribution");
        }
        if (evaluationRepository.findByOffreId(offre.getId()).isEmpty()) {
            throw new BusinessException("L'offre doit avoir été évaluée avant toute proposition d'attribution");
        }
        Marche marche = offre.getAppelOffre().getMarche();
        if (attributionRepository.findByMarcheId(marche.getId()).isPresent()) {
            throw new ConflictException("Une attribution existe déjà pour ce marché");
        }
        Utilisateur proposePar = utilisateurRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        Attribution attribution = new Attribution();
        attribution.setMarche(marche);
        attribution.setOffre(offre);
        attribution.setFournisseur(offre.getFournisseur());
        attribution.setMontant(offre.getMontant());
        attribution.setMotif(request.motif());
        attribution.setObservations(request.observations());
        attribution.setStatut(StatutAttribution.PROPOSEE);
        attribution.setProposePar(proposePar);
        attributionRepository.save(attribution);

        notificationService.envoyerAuRole(com.marchepro.utilisateur.entity.Role.DIRECTEUR,
                "Proposition d'attribution à examiner",
                "La commission propose d'attribuer " + marche.getReference() + " à "
                        + offre.getFournisseur().getRaisonSociale() + " pour "
                        + offre.getMontant() + " FCFA.",
                "ATTRIBUTION", "/directeur/attributions/" + attribution.getId());
        auditService.enregistrer("PROPOSITION_ATTRIBUTION", "ATTRIBUTION", attribution.getId(),
                marche.getReference() + " -> " + offre.getFournisseur().getRaisonSociale());
        return AttributionDto.from(attribution);
    }

    @Transactional
    public AttributionDto valider(Long id, DecisionRequest request) {
        Attribution attribution = charger(id);
        if (attribution.getStatut() != StatutAttribution.PROPOSEE) {
            throw new BusinessException("Cette attribution a déjà fait l'objet d'une décision");
        }
        Utilisateur directeur = utilisateurRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        attribution.setStatut(StatutAttribution.VALIDEE);
        attribution.setDateDecision(Instant.now());
        attribution.setValidePar(directeur);
        if (request != null && request.motif() != null) {
            attribution.setMotif(request.motif());
        }
        attributionRepository.save(attribution);

        Offre offreRetenue = attribution.getOffre();
        offreRetenue.setStatut(StatutOffre.RETENUE);
        offreRepository.save(offreRetenue);

        List<Offre> autres = offreRepository.findByAppelOffreIdAndStatutNot(
                offreRetenue.getAppelOffre().getId(), StatutOffre.BROUILLON);
        for (Offre autre : autres) {
            if (!autre.getId().equals(offreRetenue.getId())
                    && autre.getStatut() != StatutOffre.DISQUALIFIEE) {
                autre.setStatut(StatutOffre.NON_RETENUE);
                offreRepository.save(autre);
                notificationService.envoyer(autre.getFournisseur().getUtilisateur(),
                        "Résultat de l'appel d'offres",
                        "Votre offre pour " + attribution.getMarche().getReference()
                                + " n'a pas été retenue. Le marché a été attribué à "
                                + attribution.getFournisseur().getRaisonSociale() + ".",
                        "ATTRIBUTION", "/fournisseur/offres");
            }
        }

        marcheService.marquerAttribue(attribution.getMarche());
        notificationService.envoyer(attribution.getFournisseur().getUtilisateur(),
                "Félicitations, votre offre est retenue",
                "Le marché " + attribution.getMarche().getReference() + " - "
                        + attribution.getMarche().getObjet() + " vous est attribué pour "
                        + attribution.getMontant() + " FCFA.",
                "ATTRIBUTION", "/fournisseur/marches");
        auditService.enregistrer("VALIDATION_ATTRIBUTION", "ATTRIBUTION", id,
                "Attribution validée par " + directeur.getNomComplet());
        return AttributionDto.from(attribution);
    }

    @Transactional
    public AttributionDto rejeter(Long id, DecisionRequest request) {
        if (request.motif() == null || request.motif().isBlank()) {
            throw new BusinessException("Le motif de rejet est obligatoire");
        }
        Attribution attribution = charger(id);
        if (attribution.getStatut() != StatutAttribution.PROPOSEE) {
            throw new BusinessException("Cette attribution a déjà fait l'objet d'une décision");
        }
        Utilisateur directeur = utilisateurRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        attribution.setStatut(StatutAttribution.REJETEE);
        attribution.setDateDecision(Instant.now());
        attribution.setValidePar(directeur);
        attribution.setMotif(request.motif());
        attributionRepository.save(attribution);
        notificationService.envoyerAuRole(com.marchepro.utilisateur.entity.Role.MEMBRE_COMMISSION,
                "Proposition d'attribution rejetée",
                "La proposition d'attribution du marché " + attribution.getMarche().getReference()
                        + " a été rejetée. Motif : " + request.motif(),
                "ATTRIBUTION", "/commission/evaluations");
        auditService.enregistrer("REJET_ATTRIBUTION", "ATTRIBUTION", id, request.motif());
        return AttributionDto.from(attribution);
    }

    @Transactional(readOnly = true)
    public List<AttributionDto> lister(StatutAttribution statut) {
        List<Attribution> attributions = statut == null
                ? attributionRepository.findAll()
                : attributionRepository.findByStatutOrderByDatePropositionDesc(statut);
        return attributions.stream().map(AttributionDto::from).toList();
    }

    @Transactional(readOnly = true)
    public AttributionDto parId(Long id) {
        return AttributionDto.from(charger(id));
    }

    @Transactional(readOnly = true)
    public byte[] genererPv(Long id) {
        Attribution attribution = charger(id);
        if (attribution.getStatut() != StatutAttribution.VALIDEE) {
            throw new BusinessException("Le procès-verbal n'est disponible qu'après validation de l'attribution");
        }
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        Document pdf = new Document(PageSize.A4, 50, 50, 60, 50);
        try {
            PdfWriter.getInstance(pdf, sortie);
            pdf.open();
            Font titre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new java.awt.Color(21, 128, 61));
            Font sousTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 11);

            Paragraph entete = new Paragraph("MARCHÉPRO", titre);
            entete.setAlignment(Element.ALIGN_CENTER);
            pdf.add(entete);
            Paragraph org = new Paragraph("Organisme National de Démonstration", normal);
            org.setAlignment(Element.ALIGN_CENTER);
            pdf.add(org);
            pdf.add(Chunk.NEWLINE);
            Paragraph titrePv = new Paragraph("PROCÈS-VERBAL D'ATTRIBUTION", sousTitre);
            titrePv.setAlignment(Element.ALIGN_CENTER);
            pdf.add(titrePv);
            pdf.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{35, 65});
            ajouterLigne(table, "Référence du marché", attribution.getMarche().getReference(), normal);
            ajouterLigne(table, "Objet", attribution.getMarche().getObjet(), normal);
            ajouterLigne(table, "Fournisseur retenu", attribution.getFournisseur().getRaisonSociale(), normal);
            ajouterLigne(table, "NINEA", attribution.getFournisseur().getNinea(), normal);
            ajouterLigne(table, "Montant attribué", attribution.getMontant() + " FCFA", normal);
            ajouterLigne(table, "Offre retenue", attribution.getOffre().getReference(), normal);
            ajouterLigne(table, "Date de décision", DATE_FORMAT.format(attribution.getDateDecision()), normal);
            ajouterLigne(table, "Décideur", attribution.getValidePar() != null
                    ? attribution.getValidePar().getNomComplet() : "-", normal);
            ajouterLigne(table, "Motivation", attribution.getMotif() != null ? attribution.getMotif() : "-", normal);
            pdf.add(table);
            pdf.add(Chunk.NEWLINE);
            pdf.add(new Paragraph("Le présent procès-verbal atteste de l'attribution du marché au fournisseur "
                    + "désigné ci-dessus, conformément aux résultats de l'évaluation de la commission.",
                    normal));
            pdf.add(Chunk.NEWLINE);
            pdf.add(Chunk.NEWLINE);
            Paragraph signature = new Paragraph("Fait à Dakar, le "
                    + DATE_FORMAT.format(attribution.getDateDecision()), normal);
            signature.setAlignment(Element.ALIGN_RIGHT);
            pdf.add(signature);
            pdf.close();
        } catch (DocumentException ex) {
            throw new BusinessException("Erreur lors de la génération du procès-verbal");
        }
        return sortie.toByteArray();
    }

    private void ajouterLigne(PdfPTable table, String label, String valeur, Font font) {
        PdfPCell celluleLabel = new PdfPCell(new Phrase(label, font));
        celluleLabel.setBackgroundColor(new java.awt.Color(220, 252, 231));
        celluleLabel.setPadding(6);
        table.addCell(celluleLabel);
        PdfPCell celluleValeur = new PdfPCell(new Phrase(valeur == null ? "-" : valeur, font));
        celluleValeur.setPadding(6);
        table.addCell(celluleValeur);
    }

    @Transactional(readOnly = true)
    public Attribution charger(Long id) {
        return attributionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attribution introuvable"));
    }
}
