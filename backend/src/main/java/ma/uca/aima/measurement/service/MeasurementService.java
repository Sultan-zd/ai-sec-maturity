package ma.uca.aima.measurement.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import ma.uca.aima.assessment.model.Answer;
import ma.uca.aima.assessment.model.Assessment;
import ma.uca.aima.assessment.model.AssessmentStatus;
import ma.uca.aima.assessment.model.ReviewStatus;
import ma.uca.aima.assessment.repository.AnswerRepository;
import ma.uca.aima.assessment.repository.AssessmentRepository;
import ma.uca.aima.common.BusinessRuleException;
import ma.uca.aima.common.ResourceNotFoundException;
import ma.uca.aima.measurement.api.MeasurementDtos.AttackFamilyDto;
import ma.uca.aima.measurement.api.MeasurementDtos.MeasurementBatchRequest;
import ma.uca.aima.measurement.api.MeasurementDtos.MeasurementBatchResult;
import ma.uca.aima.measurement.api.MeasurementDtos.MeasurementDto;
import ma.uca.aima.measurement.api.MeasurementDtos.MeasurementItem;
import ma.uca.aima.measurement.model.AttackFamily;
import ma.uca.aima.measurement.model.Measurement;
import ma.uca.aima.measurement.repository.MeasurementRepository;
import ma.uca.aima.referential.model.Question;
import ma.uca.aima.referential.repository.QuestionRepository;
import ma.uca.aima.security.AccessGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Import des résultats du labo red-team : « l'organisation déclare » devient « on mesure ».
 * Un lot est enregistré entièrement ou pas du tout (une seule transaction).
 */
@Service
@Transactional
public class MeasurementService {

    private final MeasurementRepository measurementRepository;
    private final AssessmentRepository assessmentRepository;
    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final AccessGuard accessGuard;

    public MeasurementService(MeasurementRepository measurementRepository, AssessmentRepository assessmentRepository,
                              AnswerRepository answerRepository, QuestionRepository questionRepository,
                              AccessGuard accessGuard) {
        this.measurementRepository = measurementRepository;
        this.assessmentRepository = assessmentRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.accessGuard = accessGuard;
    }

    public MeasurementBatchResult ingest(UUID assessmentId, MeasurementBatchRequest request) {
        Assessment assessment = findAccessible(assessmentId);
        if (assessment.getStatus() == AssessmentStatus.VALIDATED) {
            throw BusinessRuleException.conflict("L'évaluation est validée : elle n'accepte plus de mesures");
        }

        // 1. Toutes les erreurs du lot sont signalées en une fois (rien n'est enregistré s'il y en a)
        List<String> errors = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Map<String, Question> questions = new HashMap<>();
        for (MeasurementItem item : request.results()) {
            String code = item.questionCode().trim();
            if (!seen.add(code)) {
                errors.add(code + " : présente plusieurs fois dans le lot");
                continue;
            }
            questionRepository.findByReferentialAndCode(assessment.getReferential().getId(), code)
                    .ifPresentOrElse(q -> questions.put(code, q),
                            () -> errors.add(code + " : question inconnue dans le référentiel de l'évaluation"));
            if (!item.attackFamily().measures(code)) {
                errors.add(code + " : la famille " + item.attackFamily() + " ne mesure que "
                        + item.attackFamily().questionCodes() + " (utiliser OTHER pour une autre question)");
            }
        }
        if (!errors.isEmpty()) {
            throw BusinessRuleException.invalid("Lot refusé : " + String.join(" ; ", errors));
        }

        // 2. Enregistrement : une nouvelle mesure remplace la précédente pour la même question
        Instant measuredAt = request.measuredAt() == null ? Instant.now() : request.measuredAt();
        UUID recordedBy = accessGuard.current().id();
        for (MeasurementItem item : request.results()) {
            Question question = questions.get(item.questionCode().trim());
            Measurement measurement = measurementRepository
                    .findByAssessmentIdAndQuestionId(assessmentId, question.getId())
                    .orElseGet(() -> new Measurement(assessment, question));
            measurement.record(item.measuredLevel(), item.attackFamily(), request.tool().trim(),
                    request.runId().trim(), item.evidence().trim(), item.evidenceUrl(), measuredAt, recordedBy);
            measurementRepository.save(measurement);
        }
        measurementRepository.flush();
        List<MeasurementDto> all = list(assessmentId);
        Set<String> recorded = questions.keySet();
        return new MeasurementBatchResult(request.runId().trim(), recorded.size(),
                all.stream().filter(m -> recorded.contains(m.questionCode())).toList());
    }

    @Transactional(readOnly = true)
    public List<MeasurementDto> list(UUID assessmentId) {
        findAccessible(assessmentId);
        Map<String, Answer> answers = answerRepository.findByAssessmentWithQuestion(assessmentId).stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getCode(), Function.identity()));
        return measurementRepository.findByAssessmentWithQuestion(assessmentId).stream()
                .map(m -> toDto(m, answers.get(m.getQuestion().getCode())))
                .toList();
    }

    public void delete(UUID assessmentId, String questionCode) {
        Assessment assessment = findAccessible(assessmentId);
        if (assessment.getStatus() == AssessmentStatus.VALIDATED) {
            throw BusinessRuleException.conflict("L'évaluation est validée : ses mesures ne sont plus modifiables");
        }
        Question question = questionRepository.findByReferentialAndCode(assessment.getReferential().getId(), questionCode)
                .orElseThrow(() -> new ResourceNotFoundException("Question " + questionCode + " introuvable"));
        measurementRepository.findByAssessmentIdAndQuestionId(assessmentId, question.getId())
                .ifPresent(measurementRepository::delete);
    }

    /** Niveau mesuré par question (utilisé par le calcul des scores). */
    @Transactional(readOnly = true)
    public Map<String, Integer> measuredLevels(UUID assessmentId) {
        return measurementRepository.findByAssessmentWithQuestion(assessmentId).stream()
                .collect(Collectors.toMap(m -> m.getQuestion().getCode(), Measurement::getMeasuredLevel));
    }

    public static List<AttackFamilyDto> attackFamilies() {
        return Arrays.stream(AttackFamily.values())
                .map(f -> new AttackFamilyDto(f, f.label(), f.questionCodes()))
                .toList();
    }

    // ------------------------------------------------------------------ utilitaires

    private Assessment findAccessible(UUID id) {
        Assessment assessment = assessmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Évaluation " + id + " introuvable"));
        accessGuard.checkOrganization(assessment.getOrganization().getId());
        return assessment;
    }

    private static MeasurementDto toDto(Measurement m, Answer answer) {
        Integer declared = answer == null || answer.isNotApplicable() ? null : answer.getLevel();
        Integer audited = answer != null && answer.getReviewStatus() == ReviewStatus.ADJUSTED
                ? answer.getAuditedLevel() : null;
        Integer gap = declared == null ? null : declared - m.getMeasuredLevel();
        return new MeasurementDto(m.getQuestion().getCode(), m.getAttackFamily(), m.getMeasuredLevel(),
                declared, audited, gap, m.getTool(), m.getRunId(), m.getEvidence(), m.getEvidenceUrl(),
                m.getMeasuredAt(), m.getUpdatedAt());
    }
}
