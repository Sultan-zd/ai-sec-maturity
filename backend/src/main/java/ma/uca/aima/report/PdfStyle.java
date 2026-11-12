package ma.uca.aima.report;

import java.awt.Color;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.Charset;
import java.util.Map;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Font;
import org.openpdf.text.pdf.BaseFont;

/** Charte graphique du rapport (polices, couleurs) et nettoyage du texte. */
final class PdfStyle {

    static final Color PRIMARY = new Color(0x1F, 0x3A, 0x5F);
    static final Color ACCENT = new Color(0x2E, 0x86, 0xAB);
    static final Color TARGET = new Color(0xE6, 0x7E, 0x22);
    static final Color LIGHT = new Color(0xEE, 0xF3, 0xF8);
    static final Color GRID = new Color(0xC8, 0xD0, 0xD8);
    static final Color TEXT_MUTED = new Color(0x5D, 0x6D, 0x7E);

    /** Couleur associée à chaque niveau de maturité (1 = rouge … 5 = vert foncé). */
    static final Color[] LEVEL_COLORS = {
            new Color(0xC0, 0x39, 0x2B), new Color(0xE6, 0x7E, 0x22), new Color(0xB7, 0x95, 0x0B),
            new Color(0x27, 0xAE, 0x60), new Color(0x1E, 0x84, 0x49)};

    static final BaseFont REGULAR = font(BaseFont.HELVETICA);
    static final BaseFont BOLD = font(BaseFont.HELVETICA_BOLD);

    static final Font TITLE = new Font(BOLD, 20, Font.NORMAL, PRIMARY);
    static final Font H1 = new Font(BOLD, 14, Font.NORMAL, PRIMARY);
    static final Font H2 = new Font(BOLD, 11, Font.NORMAL, PRIMARY);
    static final Font BODY = new Font(REGULAR, 9.5f, Font.NORMAL, Color.BLACK);
    static final Font SMALL = new Font(REGULAR, 8, Font.NORMAL, Color.BLACK);
    static final Font SMALL_MUTED = new Font(REGULAR, 8, Font.NORMAL, TEXT_MUTED);
    static final Font TABLE_HEADER = new Font(BOLD, 8.5f, Font.NORMAL, Color.WHITE);
    static final Font BIG_SCORE = new Font(BOLD, 30, Font.NORMAL, PRIMARY);

    private static final Charset CP1252 = Charset.forName("windows-1252");
    private static final Map<Character, String> REPLACEMENTS = Map.of(
            '↔', "<->", '→', "->", '≥', ">=", '≤', "<=", ' ', " ", '‑', "-", '✔', "x", '✘', "-",
            '−', "-");

    private PdfStyle() {
    }

    static Color levelColor(int level) {
        return LEVEL_COLORS[Math.max(1, Math.min(5, level)) - 1];
    }

    /**
     * Les polices standard PDF (Helvetica) utilisent l'encodage Windows-1252 : il couvre le français
     * (é, è, à, ç, œ, « »…) mais pas certains symboles (↔, →, ≥). On les remplace par un équivalent.
     */
    static String clean(String text) {
        if (text == null) {
            return "";
        }
        CharsetEncoder encoder = CP1252.newEncoder();
        StringBuilder out = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            if (encoder.canEncode(c)) {
                out.append(c);
            } else {
                out.append(REPLACEMENTS.getOrDefault(c, "?"));
            }
        }
        return out.toString();
    }

    private static BaseFont font(String name) {
        try {
            return BaseFont.createFont(name, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (DocumentException e) {
            throw new IllegalStateException(e);
        }
    }
}
