package ma.uca.aima.report;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import ma.uca.aima.assessment.api.AssessmentDtos.AnswerDto;
import ma.uca.aima.assessment.api.AssessmentDtos.AssessmentResults;
import ma.uca.aima.assessment.api.AssessmentDtos.AssessmentSummary;
import ma.uca.aima.assessment.api.AssessmentDtos.Roadmap;
import ma.uca.aima.assessment.api.AssessmentDtos.RoadmapPhase;
import ma.uca.aima.assessment.model.ReviewStatus;
import ma.uca.aima.assessment.service.AssessmentService;
import ma.uca.aima.organization.api.OrganizationDtos.OrganizationDto;
import ma.uca.aima.organization.model.Sector;
import ma.uca.aima.organization.service.OrganizationService;
import ma.uca.aima.referential.api.ReferentialDtos.ReferentialDetail;
import ma.uca.aima.referential.model.Effort;
import ma.uca.aima.referential.service.ReferentialQueryService;
import ma.uca.aima.scoring.ScoringResult;
import ma.uca.aima.scoring.ScoringResult.DomainResult;
import ma.uca.aima.scoring.ScoringResult.RecommendationItem;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Génère le rapport PDF d'une évaluation. Les données passent par {@link AssessmentService} :
 * les règles d'accès (un ORG_USER ne voit que son organisation) s'appliquent donc aussi au rapport.
 */
@Service
public class ReportService {

    public record Report(String fileName, byte[] content) {
    }

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRANCE);
    private static final ZoneId ZONE = ZoneId.of("Africa/Casablanca");

    private final AssessmentService assessmentService;
    private final OrganizationService organizationService;
    private final ReferentialQueryService referentialService;

    public ReportService(AssessmentService assessmentService, OrganizationService organizationService,
                         ReferentialQueryService referentialService) {
        this.assessmentService = assessmentService;
        this.organizationService = organizationService;
        this.referentialService = referentialService;
    }

    @Transactional(readOnly = true)
    public Report generate(UUID assessmentId) {
        AssessmentSummary summary = assessmentService.get(assessmentId);
        AssessmentResults results = assessmentService.results(assessmentId);
        Roadmap roadmap = assessmentService.roadmap(assessmentId);
        List<AnswerDto> answers = assessmentService.answers(assessmentId);
        OrganizationDto organization = organizationService.get(summary.organizationId());
        ReferentialDetail referential = referentialService.getById(summary.referentialId());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 50, 50);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new Footer(organization.name()));
            document.addTitle("Rapport AIMA — " + organization.name());
            document.addCreator("AIMA — AI Security Maturity Assessment");
            document.open();

            coverAndSynthesis(document, writer, summary, results, organization, referential);
            document.newPage();
            domainTable(document, results.scoring());
            roadmapSection(document, roadmap);
            document.newPage();
            answersAnnex(document, answers, questionTexts(referential));
            document.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Erreur de génération du PDF", e);
        }
        return new Report(fileName(organization.name()), out.toByteArray());
    }

    // ------------------------------------------------------------------ page 1 : synthèse

    private void coverAndSynthesis(Document doc, PdfWriter writer, AssessmentSummary s, AssessmentResults r,
                                   OrganizationDto org, ReferentialDetail ref) throws DocumentException {
        doc.add(paragraph("Rapport d'évaluation de maturité", PdfStyle.TITLE, 0, 2));
        doc.add(paragraph("Cybersécurité et gouvernance de l'intelligence artificielle", PdfStyle.H2, 0, 14));

        PdfPTable info = new PdfPTable(new float[] {1.2f, 2.8f});
        info.setWidthPercentage(100);
        infoRow(info, "Organisation", org.name() + " — " + sectorLabel(org.sector()) + ", " + org.size()
                + (org.country() == null ? "" : ", " + org.country()));
        infoRow(info, "Évaluation", s.title());
        if (s.scope() != null) {
            infoRow(info, "Périmètre", s.scope());
        }
        infoRow(info, "Statut", statusLabel(s));
        infoRow(info, "Référentiel", ref.name() + " v" + ref.version());
        infoRow(info, "Complétion", fmt(r.scoring().completionPercent()) + " % (" + s.answeredCount() + "/"
                + s.totalQuestions() + " questions)");
        infoRow(info, "Indice de confiance", fmt(r.evidenceRatePercent()) + " % des réponses appuyées par une preuve");
        var c = r.comparison();
        if (c.official() != null) {
            infoRow(info, "Déclaré / officiel / prouvé",
                    fmt(c.declared().value()) + " / " + fmt(c.official().value()) + " / " + fmt(c.proven().value())
                            + "   (prouvé : " + c.provenAnswers() + " réponse(s) démontrée(s) sur "
                            + c.evaluatedAnswers() + " ; une réponse sans preuve compte au niveau 1)");
        }
        infoRow(info, "Généré le", ZonedDateTime.now(ZONE).format(DATE));
        doc.add(info);

        // Score global
        ScoringResult.Score global = r.scoring().global();
        PdfPTable scoreBox = new PdfPTable(new float[] {1.3f, 2.7f});
        scoreBox.setWidthPercentage(100);
        scoreBox.setSpacingBefore(16);
        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.setBackgroundColor(PdfStyle.LIGHT);
        left.setPadding(10);
        boolean reliable = r.scoring().globalReliable();
        left.addElement(paragraph(global == null || !reliable ? "—" : fmt(global.value()) + " / 5",
                PdfStyle.BIG_SCORE, 0, 0));
        left.addElement(paragraph("Score global de maturité", PdfStyle.SMALL_MUTED, 0, 0));
        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setBackgroundColor(PdfStyle.LIGHT);
        right.setPadding(10);
        if (global != null && !reliable) {
            // Moins de 80 % de réponses : on n'affiche pas de score comme s'il était définitif.
            right.addElement(paragraph("Score provisoire", new Font(PdfStyle.BOLD, 16, Font.NORMAL,
                    PdfStyle.levelColor(1)), 0, 4));
            right.addElement(paragraph("Seulement " + fmt(r.scoring().completionPercent())
                    + " % des questions ont une réponse. Le score global n'est considéré comme fiable qu'à partir de "
                    + fmt(r.scoring().minCompletionPercent()) + " %. Estimation actuelle, à titre indicatif : "
                    + fmt(global.value()) + " / 5.", PdfStyle.BODY, 0, 0));
        } else if (global != null) {
            right.addElement(paragraph("Niveau " + global.level() + " — " + global.levelName(),
                    new Font(PdfStyle.BOLD, 16, Font.NORMAL, PdfStyle.levelColor(global.level())), 0, 4));
            right.addElement(paragraph(fmt(global.percent()) + " % de maturité. "
                    + levelDescription(ref, global.level()), PdfStyle.BODY, 0, 0));
        } else {
            right.addElement(paragraph("Aucune réponse évaluable pour le moment.", PdfStyle.BODY, 0, 0));
        }
        scoreBox.addCell(left);
        scoreBox.addCell(right);
        doc.add(scoreBox);

        // Radar
        doc.add(paragraph("Profil de maturité par domaine", PdfStyle.H1, 16, 4));
        List<RadarChart.Axis> axes = r.scoring().domains().stream()
                .map(d -> new RadarChart.Axis(d.code(),
                        d.score() == null ? null : d.score().value().doubleValue(), d.targetLevel()))
                .toList();
        Image radar = Image.getInstance(RadarChart.draw(writer.getDirectContent(), axes, 330, 280));
        radar.setAlignment(Element.ALIGN_CENTER);
        doc.add(radar);

        long critical = r.scoring().domains().stream().filter(d -> d.cappedBy() != null).count();
        if (critical > 0) {
            doc.add(paragraph("Attention : " + critical + " domaine(s) plafonné(s) par une faille critique "
                    + "(règle du maillon faible) — voir le tableau détaillé.",
                    new Font(PdfStyle.BOLD, 9, Font.NORMAL, PdfStyle.LEVEL_COLORS[0]), 6, 0));
        }
    }

    // ------------------------------------------------------------------ page 2 : domaines + feuille de route

    private void domainTable(Document doc, ScoringResult scoring) throws DocumentException {
        doc.add(paragraph("Résultats par domaine", PdfStyle.H1, 0, 6));
        PdfPTable table = new PdfPTable(new float[] {3.4f, 0.8f, 0.9f, 1.6f, 0.8f, 0.8f});
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        header(table, "Domaine", "Poids", "Score", "Niveau", "Cible", "Écart");
        for (DomainResult d : scoring.domains()) {
            String name = d.code() + " — " + d.name();
            if (d.cappedBy() != null) {
                name += "\nPlafonné à " + fmt(d.cap()) + " par " + d.cappedBy() + " (score brut " + fmt(d.rawScore()) + ")";
            }
            table.addCell(cell(name, PdfStyle.SMALL, Element.ALIGN_LEFT, null));
            table.addCell(cell(fmt(d.weight().multiply(BigDecimal.valueOf(100))) + " %", PdfStyle.SMALL,
                    Element.ALIGN_CENTER, null));
            if (d.score() == null) {
                table.addCell(cell("N/A", PdfStyle.SMALL, Element.ALIGN_CENTER, null));
                table.addCell(cell("—", PdfStyle.SMALL, Element.ALIGN_CENTER, null));
            } else {
                table.addCell(cell(fmt(d.score().value()), PdfStyle.SMALL, Element.ALIGN_CENTER, null));
                Color c = PdfStyle.levelColor(d.score().level());
                table.addCell(cell(d.score().level() + " — " + d.score().levelName(),
                        new Font(PdfStyle.BOLD, 8, Font.NORMAL, Color.WHITE), Element.ALIGN_CENTER, c));
            }
            table.addCell(cell(String.valueOf(d.targetLevel()), PdfStyle.SMALL, Element.ALIGN_CENTER, null));
            boolean gap = d.gap() != null && d.gap().signum() > 0;
            table.addCell(cell(d.gap() == null ? "—" : fmt(d.gap()),
                    gap ? new Font(PdfStyle.BOLD, 8, Font.NORMAL, PdfStyle.LEVEL_COLORS[0]) : PdfStyle.SMALL,
                    Element.ALIGN_CENTER, null));
        }
        doc.add(table);
    }

    private void roadmapSection(Document doc, Roadmap roadmap) throws DocumentException {
        doc.add(paragraph("Feuille de route (" + roadmap.totalActions() + " actions)", PdfStyle.H1, 18, 2));
        doc.add(paragraph("Actions triées par horizon puis par priorité "
                + "[(cible − niveau) × criticité × poids du domaine × 100].", PdfStyle.SMALL_MUTED, 0, 6));
        if (roadmap.totalActions() == 0) {
            doc.add(paragraph("Tous les domaines évalués atteignent leur niveau cible.", PdfStyle.BODY, 0, 0));
            return;
        }
        for (RoadmapPhase phase : roadmap.phases()) {
            if (phase.actions().isEmpty()) {
                continue;
            }
            doc.add(paragraph(phase.label() + " — " + phase.period() + " (" + phase.actions().size() + ")",
                    PdfStyle.H2, 8, 4));
            PdfPTable table = new PdfPTable(new float[] {0.9f, 4.6f, 0.7f, 0.8f, 0.7f});
            table.setWidthPercentage(100);
            table.setHeaderRows(1);
            header(table, "Question", "Action recommandée", "Niveau", "Priorité", "Effort");
            for (RecommendationItem item : phase.actions()) {
                table.addCell(cell(item.questionCode(), PdfStyle.SMALL, Element.ALIGN_LEFT, null));
                String action = item.action();
                if (!item.followUpActions().isEmpty()) {
                    action += "\nEnsuite : " + String.join(" / ", item.followUpActions());
                }
                table.addCell(cell(action, PdfStyle.SMALL, Element.ALIGN_LEFT, null));
                table.addCell(cell(item.currentLevel() + " → " + item.targetLevel(), PdfStyle.SMALL,
                        Element.ALIGN_CENTER, null));
                table.addCell(cell(fmt(item.priority()), PdfStyle.SMALL, Element.ALIGN_CENTER, null));
                table.addCell(cell(effortLabel(item.effort()), PdfStyle.SMALL, Element.ALIGN_CENTER, null));
            }
            doc.add(table);
        }
    }

    // ------------------------------------------------------------------ annexe

    private void answersAnnex(Document doc, List<AnswerDto> answers, Map<String, String> texts)
            throws DocumentException {
        doc.add(paragraph("Annexe — Détail des réponses", PdfStyle.H1, 0, 6));
        PdfPTable table = new PdfPTable(new float[] {0.9f, 3.6f, 0.8f, 0.9f, 1.8f});
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        header(table, "Code", "Question", "Déclaré", "Retenu", "Preuve / commentaire");
        for (AnswerDto a : answers) {
            table.addCell(cell(a.questionCode(), PdfStyle.SMALL, Element.ALIGN_LEFT, null));
            table.addCell(cell(texts.getOrDefault(a.questionCode(), ""), PdfStyle.SMALL, Element.ALIGN_LEFT, null));
            table.addCell(cell(a.notApplicable() ? "N/A" : String.valueOf(a.level()), PdfStyle.SMALL,
                    Element.ALIGN_CENTER, null));
            String retained = a.reviewStatus() == ReviewStatus.PENDING ? "—"
                    : (a.effectiveLevel() == null ? "N/A" : String.valueOf(a.effectiveLevel()))
                    + (a.reviewStatus() == ReviewStatus.ADJUSTED ? " (corrigé)" : " (confirmé)");
            table.addCell(cell(retained, PdfStyle.SMALL, Element.ALIGN_CENTER, null));
            StringBuilder note = new StringBuilder();
            append(note, a.evidence() == null ? null : "Preuve : " + a.evidence());
            append(note, a.notApplicable() ? "Justification N/A : " + a.justification() : null);
            append(note, a.comment());
            append(note, a.auditorComment() == null ? null : "Auditeur : " + a.auditorComment());
            table.addCell(cell(note.toString(), PdfStyle.SMALL_MUTED, Element.ALIGN_LEFT, null));
        }
        doc.add(table);
    }

    // ------------------------------------------------------------------ utilitaires de mise en page

    /** Pied de page sur chaque page : organisation, mention de confidentialité, numéro de page. */
    private static final class Footer extends PdfPageEventHelper {
        private final String organization;

        Footer(String organization) {
            this.organization = organization;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Phrase left = new Phrase(PdfStyle.clean("AIMA — " + organization + " — Document confidentiel"),
                    PdfStyle.SMALL_MUTED);
            Phrase right = new Phrase("Page " + writer.getPageNumber(), PdfStyle.SMALL_MUTED);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT, left,
                    document.left(), document.bottom() - 25, 0);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT, right,
                    document.right(), document.bottom() - 25, 0);
        }
    }

    private static Paragraph paragraph(String text, Font font, float before, float after) {
        Paragraph p = new Paragraph(PdfStyle.clean(text), font);
        p.setSpacingBefore(before);
        p.setSpacingAfter(after);
        return p;
    }

    private static void infoRow(PdfPTable table, String label, String value) {
        PdfPCell l = cell(label, new Font(PdfStyle.BOLD, 9, Font.NORMAL, PdfStyle.PRIMARY), Element.ALIGN_LEFT, null);
        PdfPCell v = cell(value, PdfStyle.BODY, Element.ALIGN_LEFT, null);
        l.setBorder(Rectangle.BOTTOM);
        v.setBorder(Rectangle.BOTTOM);
        l.setBorderColor(PdfStyle.GRID);
        v.setBorderColor(PdfStyle.GRID);
        table.addCell(l);
        table.addCell(v);
    }

    private static void header(PdfPTable table, String... titles) {
        for (String title : titles) {
            table.addCell(cell(title, PdfStyle.TABLE_HEADER, Element.ALIGN_CENTER, PdfStyle.PRIMARY));
        }
    }

    private static PdfPCell cell(String text, Font font, int align, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(PdfStyle.clean(text), font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);
        cell.setBorderColor(PdfStyle.GRID);
        if (background != null) {
            cell.setBackgroundColor(background);
        }
        return cell;
    }

    private static void append(StringBuilder sb, String text) {
        if (text != null && !text.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append('\n');
            }
            sb.append(text);
        }
    }

    private static Map<String, String> questionTexts(ReferentialDetail referential) {
        Map<String, String> texts = new HashMap<>();
        referential.domains().forEach(d -> d.subdomains().forEach(sd -> sd.questions()
                .forEach(q -> texts.put(q.code(), q.text()))));
        return texts;
    }

    private static String levelDescription(ReferentialDetail ref, int level) {
        return ref.maturityLevels().stream().filter(l -> l.level() == level).findFirst()
                .map(l -> l.description()).orElse("");
    }

    private static String statusLabel(AssessmentSummary s) {
        return switch (s.status()) {
            case DRAFT -> "Brouillon (résultats provisoires)";
            case SUBMITTED -> "Soumise — auto-évaluation déclarative (" + s.reviewedCount() + "/" + s.answeredCount()
                    + " réponses revues par l'auditeur)";
            case VALIDATED -> "Validée par un auditeur le " + s.validatedAt().atZone(ZONE).format(DATE);
        };
    }

    private static String sectorLabel(Sector sector) {
        return switch (sector) {
            case BANKING_FINANCE -> "Banque et finance";
            case INSURANCE -> "Assurance";
            case HEALTHCARE -> "Santé";
            case PUBLIC_SECTOR -> "Secteur public";
            case TELECOM -> "Télécommunications";
            case INDUSTRY -> "Industrie";
            case ENERGY -> "Énergie";
            case RETAIL -> "Commerce";
            case TECHNOLOGY -> "Technologie";
            case EDUCATION -> "Éducation";
            case OTHER -> "Autre secteur";
        };
    }

    private static String effortLabel(Effort effort) {
        return switch (effort) {
            case LOW -> "Faible";
            case MEDIUM -> "Moyen";
            case HIGH -> "Élevé";
        };
    }

    private static String fmt(BigDecimal value) {
        return value == null ? "—" : String.format(Locale.FRANCE, "%.2f", value);
    }

    /** Nom de fichier sûr : « rapport-aima-banque-atlas-2026-11-13.pdf ». */
    private static String fileName(String organization) {
        String slug = Normalizer.normalize(organization, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return "rapport-aima-" + slug + "-" + ZonedDateTime.now(ZONE).toLocalDate() + ".pdf";
    }
}
