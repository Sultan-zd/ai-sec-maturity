package ma.uca.aima.assessment.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import ma.uca.aima.assessment.api.AssessmentDtos.AnswerDto;
import ma.uca.aima.assessment.api.AssessmentDtos.AnswerRequest;
import ma.uca.aima.assessment.api.AssessmentDtos.AssessmentResults;
import ma.uca.aima.assessment.api.AssessmentDtos.AssessmentSummary;
import ma.uca.aima.assessment.api.AssessmentDtos.CreateAssessmentRequest;
import ma.uca.aima.assessment.api.AssessmentDtos.Roadmap;
import ma.uca.aima.assessment.api.AssessmentDtos.RoadmapPhase;
import ma.uca.aima.assessment.model.Answer;
import ma.uca.aima.assessment.model.Assessment;
import ma.uca.aima.assessment.repository.AnswerRepository;
import ma.uca.aima.assessment.repository.AssessmentRepository;
import ma.uca.aima.common.BusinessRuleException;
import ma.uca.aima.common.ResourceNotFoundException;
import ma.uca.aima.organization.model.Organization;
import ma.uca.aima.organization.service.OrganizationService;
import ma.uca.aima.referential.model.Question;
import ma.uca.aima.referential.model.Referential;
import ma.uca.aima.referential.repository.QuestionRepository;
import ma.uca.aima.referential.service.ReferentialQueryService;
import ma.uca.aima.scoring.AnswerInput;
import ma.uca.aima.scoring.Horizon;
import ma.uca.aima.scoring.ScoringEngine;
import ma.uca.aima.scoring.ScoringModel;
import ma.uca.aima.scoring.ScoringModel.DomainSpec;
import ma.uca.aima.scoring.ScoringModelFactory;
import ma.uca.aima.scoring.ScoringResult;
import ma.uca.aima.scoring.ScoringResult.RecommendationItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cas d'usage des évaluations : création, saisie des réponses, soumission, résultats et feuille de route.
 */
@Service
@Transactional
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final OrganizationService organizationService;
    private final ReferentialQueryService referentialService;
    private final ScoringModelFactory modelFactory;
    private final ScoringEngine engine;

    public AssessmentService(AssessmentRepository assessmentRepository, AnswerRepository answerRepository,
                             QuestionRepository questionRepository, OrganizationService organizationService,
                             ReferentialQueryService referentialService, ScoringModelFactory modelFactory,
                             ScoringEngine engine) {
        this.assessmentRepository = assessmentRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.organizationService = organizationService;
        this.referentialService = referentialService;
        this.modelFactory = modelFactory;
        this.engine = engine;
    }

    // ------------------------------------------------------------------ cycle de vie

    public AssessmentSummary create(CreateAssessmentRequest request) {
        Organization organization = organizationService.find(request.organizationId());
        Referential referential = referentialService.findActive();
        ScoringModel model = modelFactory.forReferential(referential.getId());

        Map<String, Integer> targets = new LinkedHashMap<>();
        for (DomainSpec domain : model.domains()) {
            targets.put(domain.code(), model.defaultTargetLevel());
        }
        targets.putAll(checkedTargets(model, request.targets()));

        Assessment assessment = new Assessment(organization, referential, request.title().trim(),
                request.scope(), targets);
        return summary(assessmentRepository.saveAndFlush(assessment));
    }

    public AssessmentSummary updateTargets(UUID id, Map<String, Integer> targets) {
        Assessment assessment = find(id);
        assessment.updateTargets(checkedTargets(model(assessment), targets));
        return summary(assessmentRepository.saveAndFlush(assessment));
    }

    public AssessmentSummary submit(UUID id) {
        Assessment assessment = find(id);
        assessment.assertEditable();
        ScoringResult result = compute(assessment);
        int missing = result.total() - result.answered() - result.notApplicable();
        if (missing > 0) {
            throw BusinessRuleException.conflict(missing + " question(s) sans réponse : complétez l'évaluation "
                    + "avant de la soumettre");
        }
        if (result.global() == null) {
            throw BusinessRuleException.conflict("Toutes les questions sont « Non applicable » : aucun score calculable");
        }
        assessment.submit(result.global().value(), result.global().level());
        return summary(assessmentRepository.saveAndFlush(assessment));
    }

    public AssessmentSummary reopen(UUID id) {
        Assessment assessment = find(id);
        assessment.reopen();
        return summary(assessmentRepository.saveAndFlush(assessment));
    }

    public void delete(UUID id) {
        Assessment assessment = find(id);
        assessment.assertEditable();
        assessmentRepository.delete(assessment);
    }

    @Transactional(readOnly = true)
    public List<AssessmentSummary> list(UUID organizationId) {
        List<Assessment> assessments = organizationId == null
                ? assessmentRepository.findAllByOrderByCreatedAtDesc()
                : assessmentRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
        return assessments.stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public AssessmentSummary get(UUID id) {
        return summary(find(id));
    }

    // ------------------------------------------------------------------ réponses

    public AnswerDto saveAnswer(UUID id, String questionCode, AnswerRequest request) {
        Assessment assessment = find(id);
        assessment.assertEditable();
        validate(request);
        Question question = findQuestion(assessment, questionCode);

        Answer answer = answerRepository.findByAssessmentIdAndQuestionId(id, question.getId())
                .orElseGet(() -> new Answer(assessment, question));
        answer.update(request.level(), request.isNotApplicable(), request.justification(), request.comment(),
                request.evidence());
        return toDto(answerRepository.saveAndFlush(answer));
    }

    public void deleteAnswer(UUID id, String questionCode) {
        Assessment assessment = find(id);
        assessment.assertEditable();
        Question question = findQuestion(assessment, questionCode);
        answerRepository.findByAssessmentIdAndQuestionId(id, question.getId())
                .ifPresent(answerRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<AnswerDto> answers(UUID id) {
        find(id);
        return answerRepository.findByAssessmentWithQuestion(id).stream().map(AssessmentService::toDto).toList();
    }

    private static void validate(AnswerRequest request) {
        if (request.isNotApplicable()) {
            if (request.level() != null) {
                throw BusinessRuleException.invalid("Une question « Non applicable » ne doit pas avoir de niveau");
            }
            if (request.justification() == null || request.justification().isBlank()) {
                throw BusinessRuleException.invalid("La justification est obligatoire pour « Non applicable »");
            }
        } else if (request.level() == null) {
            throw BusinessRuleException.invalid("Le niveau (1 à 5) est obligatoire");
        }
    }

    // ------------------------------------------------------------------ résultats

    @Transactional(readOnly = true)
    public AssessmentResults results(UUID id) {
        Assessment assessment = find(id);
        List<Answer> answers = answerRepository.findByAssessmentWithQuestion(id);
        long evaluated = answers.stream().filter(a -> !a.isNotApplicable()).count();
        long proven = answers.stream().filter(a -> !a.isNotApplicable() && a.hasEvidence()).count();
        BigDecimal evidenceRate = evaluated == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(100.0 * proven / evaluated).setScale(2, RoundingMode.HALF_UP);
        return new AssessmentResults(id, assessment.getStatus(), evidenceRate, compute(assessment, answers));
    }

    @Transactional(readOnly = true)
    public Roadmap roadmap(UUID id) {
        Assessment assessment = find(id);
        List<RecommendationItem> items = compute(assessment).recommendations();
        List<RoadmapPhase> phases = Arrays.stream(Horizon.values())
                .map(h -> new RoadmapPhase(h, h.label(), h.period(),
                        items.stream().filter(i -> i.horizon() == h).toList()))
                .toList();
        return new Roadmap(id, items.size(), phases);
    }

    private ScoringResult compute(Assessment assessment) {
        return compute(assessment, answerRepository.findByAssessmentWithQuestion(assessment.getId()));
    }

    private ScoringResult compute(Assessment assessment, List<Answer> answers) {
        Map<String, AnswerInput> inputs = new HashMap<>();
        for (Answer a : answers) {
            inputs.put(a.getQuestion().getCode(), new AnswerInput(a.getLevel(), a.isNotApplicable()));
        }
        return engine.compute(model(assessment), inputs, assessment.getTargets());
    }

    // ------------------------------------------------------------------ utilitaires

    private Assessment find(UUID id) {
        return assessmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Évaluation " + id + " introuvable"));
    }

    private ScoringModel model(Assessment assessment) {
        return modelFactory.forReferential(assessment.getReferential().getId());
    }

    private Question findQuestion(Assessment assessment, String code) {
        return questionRepository.findByReferentialAndCode(assessment.getReferential().getId(), code)
                .orElseThrow(() -> new ResourceNotFoundException("Question " + code + " introuvable"));
    }

    private static Map<String, Integer> checkedTargets(ScoringModel model, Map<String, Integer> targets) {
        if (targets == null) {
            return Map.of();
        }
        List<String> known = model.domains().stream().map(DomainSpec::code).toList();
        targets.keySet().stream()
                .filter(code -> !known.contains(code))
                .findFirst()
                .ifPresent(code -> {
                    throw BusinessRuleException.invalid("Domaine inconnu : " + code + " (attendus : " + known + ")");
                });
        return targets;
    }

    private AssessmentSummary summary(Assessment a) {
        ScoringModel model = model(a);
        long answered = answerRepository.countByAssessmentId(a.getId());
        int total = model.totalQuestions();
        BigDecimal completion = total == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(100.0 * answered / total).setScale(2, RoundingMode.HALF_UP);
        Map<String, Integer> orderedTargets = new LinkedHashMap<>();
        model.domains().forEach(d -> orderedTargets.put(d.code(), a.getTargets().get(d.code())));
        return new AssessmentSummary(a.getId(), a.getTitle(), a.getScope(), a.getStatus(),
                a.getOrganization().getId(), a.getOrganization().getName(),
                a.getReferential().getId(), a.getReferential().getVersion(), orderedTargets,
                answered, total, completion, a.getGlobalScore(), a.getGlobalLevel(),
                a.getCreatedAt(), a.getUpdatedAt(), a.getSubmittedAt());
    }

    private static AnswerDto toDto(Answer a) {
        return new AnswerDto(a.getQuestion().getCode(), a.getLevel(), a.isNotApplicable(), a.getJustification(),
                a.getComment(), a.getEvidence(), a.getUpdatedAt());
    }
}
