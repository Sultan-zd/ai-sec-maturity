package ma.uca.aima.scoring;

/** Réponse à une question, vue par le moteur : un niveau (1 à 5) ou « Non applicable ». */
public record AnswerInput(Integer level, boolean notApplicable) {

    public static AnswerInput ofLevel(int level) {
        return new AnswerInput(level, false);
    }

    public static AnswerInput ofNotApplicable() {
        return new AnswerInput(null, true);
    }
}
