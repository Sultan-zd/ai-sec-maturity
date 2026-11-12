package ma.uca.aima.report;

import java.awt.Color;
import java.util.List;
import org.openpdf.text.Element;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfTemplate;

/**
 * Dessine un graphique radar vectoriel (net à tous les zooms) : un axe par domaine,
 * le polygone bleu = niveau actuel, le polygone orange pointillé = niveau cible.
 */
final class RadarChart {

    /** @param value score du domaine, ou {@code null} si le domaine n'est pas encore évalué */
    record Axis(String label, Double value, double target) {
        double plotted() {
            return value == null ? 0 : value;
        }
    }

    private static final double MAX = 5.0;

    private RadarChart() {
    }

    static PdfTemplate draw(PdfContentByte canvas, List<Axis> axes, float width, float height) {
        PdfTemplate t = canvas.createTemplate(width, height);
        float cx = width / 2f;
        float cy = height / 2f - 4;
        float radius = Math.min(width, height) / 2f - 34;
        int n = axes.size();

        // 1. Polygone « actuel » rempli
        t.setColorFill(new Color(0xD6, 0xE6, 0xF2));
        polygon(t, axes, cx, cy, radius, false);
        t.fill();

        // 2. Grille : un anneau par niveau (1 à 5) et un axe par domaine
        t.setLineWidth(0.4f);
        t.setColorStroke(PdfStyle.GRID);
        for (int level = 1; level <= 5; level++) {
            float r = (float) (radius * level / MAX);
            for (int i = 0; i <= n; i++) {
                float[] p = point(cx, cy, r, i % n, n);
                if (i == 0) {
                    t.moveTo(p[0], p[1]);
                } else {
                    t.lineTo(p[0], p[1]);
                }
            }
            t.stroke();
        }
        for (int i = 0; i < n; i++) {
            float[] p = point(cx, cy, radius, i, n);
            t.moveTo(cx, cy);
            t.lineTo(p[0], p[1]);
        }
        t.stroke();

        // 3. Contour « actuel » et cible en pointillés
        t.setLineWidth(1.6f);
        t.setColorStroke(PdfStyle.ACCENT);
        polygon(t, axes, cx, cy, radius, false);
        t.closePathStroke();

        t.setLineWidth(1.2f);
        t.setColorStroke(PdfStyle.TARGET);
        t.setLineDash(4f, 3f, 0f);
        polygon(t, axes, cx, cy, radius, true);
        t.closePathStroke();
        t.setLineDash(0f);

        // 4. Libellés : graduations et codes des domaines
        t.beginText();
        t.setColorFill(PdfStyle.TEXT_MUTED);
        t.setFontAndSize(PdfStyle.REGULAR, 6.5f);
        for (int level = 1; level <= 5; level++) {
            t.showTextAligned(Element.ALIGN_LEFT, String.valueOf(level),
                    cx + 2, cy + (float) (radius * level / MAX) - 7, 0);
        }
        t.setColorFill(PdfStyle.PRIMARY);
        t.setFontAndSize(PdfStyle.BOLD, 8);
        for (int i = 0; i < n; i++) {
            float[] p = point(cx, cy, radius + 14, i, n);
            String label = axes.get(i).label() + " " + format(axes.get(i).value());
            t.showTextAligned(Element.ALIGN_CENTER, PdfStyle.clean(label), p[0], p[1] - 3, 0);
        }
        // Légende
        t.setFontAndSize(PdfStyle.REGULAR, 7.5f);
        t.setColorFill(PdfStyle.ACCENT);
        t.showTextAligned(Element.ALIGN_LEFT, "— Niveau actuel", 4, 6, 0);
        t.setColorFill(PdfStyle.TARGET);
        t.showTextAligned(Element.ALIGN_LEFT, "- - Niveau cible", 90, 6, 0);
        t.endText();
        return t;
    }

    private static void polygon(PdfTemplate t, List<Axis> axes, float cx, float cy, float radius, boolean target) {
        int n = axes.size();
        for (int i = 0; i < n; i++) {
            double v = target ? axes.get(i).target() : axes.get(i).plotted();
            float[] p = point(cx, cy, (float) (radius * Math.max(0, Math.min(MAX, v)) / MAX), i, n);
            if (i == 0) {
                t.moveTo(p[0], p[1]);
            } else {
                t.lineTo(p[0], p[1]);
            }
        }
    }

    /** Point sur l'axe i (le premier axe pointe vers le haut, sens horaire). */
    private static float[] point(float cx, float cy, float r, int i, int n) {
        double angle = Math.PI / 2 - 2 * Math.PI * i / n;
        return new float[] {(float) (cx + r * Math.cos(angle)), (float) (cy + r * Math.sin(angle))};
    }

    private static String format(Double value) {
        return value == null ? "N/A" : String.format(java.util.Locale.FRANCE, "%.1f", value);
    }
}
