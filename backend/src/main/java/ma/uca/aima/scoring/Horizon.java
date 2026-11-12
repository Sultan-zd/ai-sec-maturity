package ma.uca.aima.scoring;

/** Horizons de la feuille de route (voir docs/02-modele-maturite.md §6.3). */
public enum Horizon {
    QUICK_WIN("Quick wins", "0-3 mois"),
    SHORT_TERM("Court terme", "3-6 mois"),
    MEDIUM_TERM("Moyen terme", "6-12 mois"),
    LONG_TERM("Long terme", "12-18 mois");

    private final String label;
    private final String period;

    Horizon(String label, String period) {
        this.label = label;
        this.period = period;
    }

    public String label() {
        return label;
    }

    public String period() {
        return period;
    }
}
