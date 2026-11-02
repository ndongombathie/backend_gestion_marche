package com.marchepro.rapport.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.marchepro.attribution.entity.StatutAttribution;
import com.marchepro.attribution.repository.AttributionRepository;
import com.marchepro.livraison.repository.LivraisonRepository;
import com.marchepro.marche.entity.Marche;
import com.marchepro.marche.repository.MarcheRepository;
import com.marchepro.rapport.dto.DashboardDto;
import com.marchepro.rapport.dto.RapportDto;
import com.marchepro.rapport.entity.Rapport;
import com.marchepro.rapport.repository.RapportRepository;
import com.marchepro.security.SecurityUtils;
import com.marchepro.utilisateur.repository.UtilisateurRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class RapportService {

    private static final ZoneId DAKAR = ZoneId.of("Africa/Dakar");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRANCE);

    private final DashboardService dashboardService;
    private final MarcheRepository marcheRepository;
    private final AttributionRepository attributionRepository;
    private final LivraisonRepository livraisonRepository;
    private final RapportRepository rapportRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Value("${app.organisation.nom:Organisme}")
    private String organisation;

    public RapportService(DashboardService dashboardService,
                          MarcheRepository marcheRepository,
                          AttributionRepository attributionRepository,
                          LivraisonRepository livraisonRepository,
                          RapportRepository rapportRepository,
                          UtilisateurRepository utilisateurRepository) {
        this.dashboardService = dashboardService;
        this.marcheRepository = marcheRepository;
        this.attributionRepository = attributionRepository;
        this.livraisonRepository = livraisonRepository;
        this.rapportRepository = rapportRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Transactional
    public byte[] exporterPdf(LocalDate debut, LocalDate fin) {
        DashboardDto d = dashboardService.tableauDeBord(debut, fin);
        List<Marche> marches = marcheRepository.findAll(Sort.by(Sort.Direction.DESC, "dateCreation"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 32, 32, 32, 32);
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(20, 83, 45));
            Font sousTitre = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.DARK_GRAY);
            Font section = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(22, 101, 52));
            Font cellule = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font entete = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);

            Paragraph p = new Paragraph("Rapport de pilotage des marchés publics", titre);
            p.setAlignment(Element.ALIGN_CENTER);
            document.add(p);
            Paragraph org = new Paragraph(organisation, sousTitre);
            org.setAlignment(Element.ALIGN_CENTER);
            document.add(org);
            Paragraph date = new Paragraph("Généré le " + DATE.format(Instant.now().atZone(DAKAR)), sousTitre);
            date.setAlignment(Element.ALIGN_CENTER);
            date.setSpacingAfter(18);
            document.add(date);

            document.add(new Paragraph("Indicateurs clés", section));
            PdfPTable indicateurs = new PdfPTable(3);
            indicateurs.setWidthPercentage(100);
            indicateurs.setSpacingBefore(8);
            indicateurs.setSpacingAfter(18);
            ajouterIndicateur(indicateurs, "Marchés", String.valueOf(d.totalMarches()), entete, cellule);
            ajouterIndicateur(indicateurs, "Appels d'offres publiés",
                    String.valueOf(d.appelsOffresPublies()), entete, cellule);
            ajouterIndicateur(indicateurs, "Fournisseurs validés",
                    String.valueOf(d.fournisseursActifs()), entete, cellule);
            ajouterIndicateur(indicateurs, "Montant prévisionnel",
                    formater(d.montantPrevisionnel()) + " FCFA", entete, cellule);
            ajouterIndicateur(indicateurs, "Montant attribué",
                    formater(d.montantAttribue()) + " FCFA", entete, cellule);
            ajouterIndicateur(indicateurs, "Taux de livraison dans les délais",
                    String.format(Locale.FRANCE, "%.1f %%", d.tauxLivraisonDelais()), entete, cellule);
            document.add(indicateurs);

            document.add(new Paragraph("Liste des marchés", section));
            PdfPTable table = new PdfPTable(new float[]{2.2f, 4.5f, 2.2f, 2.2f, 2.2f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(8);
            String[] colonnes = {"Référence", "Objet", "Statut", "Budget (FCFA)", "Mode"};
            for (String colonne : colonnes) {
                PdfPCell cell = new PdfPCell(new Phrase(colonne, entete));
                cell.setBackgroundColor(new Color(22, 101, 52));
                cell.setPadding(6);
                table.addCell(cell);
            }
            for (Marche m : marches) {
                table.addCell(new PdfPCell(new Phrase(m.getReference(), cellule)));
                table.addCell(new PdfPCell(new Phrase(m.getObjet(), cellule)));
                table.addCell(new PdfPCell(new Phrase(m.getStatut().name(), cellule)));
                table.addCell(new PdfPCell(new Phrase(formater(m.getBudgetPrevisionnel()), cellule)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(m.getModePassation()), cellule)));
            }
            document.add(table);
            document.close();

            enregistrer("Rapport de pilotage (PDF)", "PDF",
                    "du " + (debut != null ? debut : "origine") + " au " + (fin != null ? fin : "aujourd'hui"));
            return out.toByteArray();
        } catch (Exception e) {
            if (document.isOpen()) {
                document.close();
            }
            throw new IllegalStateException("Impossible de générer le rapport PDF", e);
        }
    }

    @Transactional
    public byte[] exporterExcel(LocalDate debut, LocalDate fin) {
        List<Marche> marches = marcheRepository.findAll(Sort.by(Sort.Direction.DESC, "dateCreation"));
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle entete = enteteExcel(workbook);
            CellStyle monnaie = workbook.createCellStyle();
            monnaie.setDataFormat(workbook.createDataFormat().getFormat("# ##0"));

            Sheet sheet = workbook.createSheet("Marchés");
            String[] colonnes = {"Référence", "Objet", "Statut", "Budget prévisionnel", "Mode de passation",
                    "Date de création", "Date de publication"};
            ecrireEntete(sheet, colonnes, entete);
            int ligne = 1;
            for (Marche m : marches) {
                Row row = sheet.createRow(ligne++);
                row.createCell(0).setCellValue(m.getReference());
                row.createCell(1).setCellValue(m.getObjet());
                row.createCell(2).setCellValue(m.getStatut().name());
                Cell budget = row.createCell(3);
                budget.setCellValue(m.getBudgetPrevisionnel() != null ? m.getBudgetPrevisionnel().doubleValue() : 0);
                budget.setCellStyle(monnaie);
                row.createCell(4).setCellValue(String.valueOf(m.getModePassation()));
                row.createCell(5).setCellValue(m.getDateCreation() != null
                        ? DATE.format(m.getDateCreation().atZone(DAKAR)) : "");
                row.createCell(6).setCellValue(m.getDatePublication() != null
                        ? DATE.format(m.getDatePublication().atZone(DAKAR)) : "");
            }
            for (int i = 0; i < colonnes.length; i++) {
                sheet.setColumnWidth(i, 24 * 256);
            }

            Sheet sheetFournisseurs = workbook.createSheet("Fournisseurs");
            String[] colonnesF = {"Fournisseur", "Marchés attribués", "Montant total (FCFA)"};
            ecrireEntete(sheetFournisseurs, colonnesF, entete);
            int ligneF = 1;
            for (Object[] row : attributionRepository.classementFournisseurs()) {
                Row r = sheetFournisseurs.createRow(ligneF++);
                r.createCell(0).setCellValue(String.valueOf(row[0]));
                r.createCell(1).setCellValue(((Number) row[1]).doubleValue());
                r.createCell(2).setCellValue(row[2] != null ? ((BigDecimal) row[2]).doubleValue() : 0);
            }
            sheetFournisseurs.setColumnWidth(0, 32 * 256);
            sheetFournisseurs.setColumnWidth(1, 18 * 256);
            sheetFournisseurs.setColumnWidth(2, 22 * 256);

            Sheet sheetLivraisons = workbook.createSheet("Livraisons en retard");
            String[] colonnesL = {"Marché", "Fournisseur", "Date prévue", "Statut"};
            ecrireEntete(sheetLivraisons, colonnesL, entete);
            int ligneL = 1;
            for (var livraison : livraisonRepository.findEnRetard(LocalDate.now(DAKAR))) {
                Row r = sheetLivraisons.createRow(ligneL++);
                r.createCell(0).setCellValue(livraison.getMarche().getReference());
                r.createCell(1).setCellValue(livraison.getFournisseur().getRaisonSociale());
                r.createCell(2).setCellValue(livraison.getDatePrevue().toString());
                r.createCell(3).setCellValue(livraison.getStatut().name());
            }
            for (int i = 0; i < colonnesL.length; i++) {
                sheetLivraisons.setColumnWidth(i, 24 * 256);
            }

            workbook.write(out);
            enregistrer("Export Excel des marchés", "EXCEL",
                    "du " + (debut != null ? debut : "origine") + " au " + (fin != null ? fin : "aujourd'hui"));
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de générer l'export Excel", e);
        }
    }

    @Transactional(readOnly = true)
    public List<RapportDto> historique() {
        return rapportRepository.findAll(Sort.by(Sort.Direction.DESC, "dateGeneration")).stream()
                .map(r -> new RapportDto(r.getId(), r.getTitre(), r.getFormat(), r.getParametres(),
                        r.getDateGeneration(),
                        r.getGenerePar() != null ? r.getGenerePar().getNomComplet() : null))
                .toList();
    }

    private void enregistrer(String titre, String format, String parametres) {
        Rapport rapport = new Rapport();
        rapport.setTitre(titre);
        rapport.setFormat(format);
        rapport.setParametres(parametres);
        utilisateurRepository.findById(SecurityUtils.currentUserId()).ifPresent(rapport::setGenerePar);
        rapportRepository.save(rapport);
    }

    private static void ajouterIndicateur(PdfPTable table, String libelle, String valeur,
                                          Font entete, Font cellule) {
        PdfPCell titreCell = new PdfPCell(new Phrase(libelle, entete));
        titreCell.setBackgroundColor(new Color(22, 101, 52));
        titreCell.setPadding(6);
        table.addCell(titreCell);
        PdfPCell valeurCell = new PdfPCell(new Phrase(valeur, cellule));
        valeurCell.setPadding(6);
        table.addCell(valeurCell);
        PdfPCell vide = new PdfPCell(new Phrase("", cellule));
        vide.setBorder(0);
        table.addCell(vide);
    }

    private static CellStyle enteteExcel(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private static void ecrireEntete(Sheet sheet, String[] colonnes, CellStyle style) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < colonnes.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(colonnes[i]);
            cell.setCellStyle(style);
        }
    }

    private static String formater(BigDecimal montant) {
        return String.format(Locale.FRANCE, "%,.0f", montant != null ? montant : BigDecimal.ZERO);
    }
}
