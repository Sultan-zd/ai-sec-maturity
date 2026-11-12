package ma.uca.aima.referential.service;

import java.util.List;
import ma.uca.aima.common.ResourceNotFoundException;
import ma.uca.aima.referential.api.ReferentialDtos.QuestionDto;
import ma.uca.aima.referential.api.ReferentialDtos.ReferentialDetail;
import ma.uca.aima.referential.api.ReferentialDtos.ReferentialSummary;
import ma.uca.aima.referential.api.ReferentialMapper;
import ma.uca.aima.referential.model.Referential;
import ma.uca.aima.referential.repository.QuestionRepository;
import ma.uca.aima.referential.repository.ReferentialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lecture du référentiel (aucune modification possible via l'API : le référentiel est immuable). */
@Service
@Transactional(readOnly = true)
public class ReferentialQueryService {

    private final ReferentialRepository referentialRepository;
    private final QuestionRepository questionRepository;

    public ReferentialQueryService(ReferentialRepository referentialRepository,
                                   QuestionRepository questionRepository) {
        this.referentialRepository = referentialRepository;
        this.questionRepository = questionRepository;
    }

    public List<ReferentialSummary> listReferentials() {
        return referentialRepository.findAllByOrderByImportedAtDesc().stream()
                .map(r -> new ReferentialSummary(r.getId(), r.getCode(), r.getVersion(), r.getName(), r.isActive(),
                        r.getImportedAt(), referentialRepository.countDomains(r.getId()),
                        referentialRepository.countQuestions(r.getId())))
                .toList();
    }

    public ReferentialDetail getActive() {
        return ReferentialMapper.toDetail(findActive());
    }

    public ReferentialDetail getById(Long id) {
        return ReferentialMapper.toDetail(findById(id));
    }

    public QuestionDto getQuestion(Long referentialId, String code) {
        findById(referentialId);
        return questionRepository.findByReferentialAndCode(referentialId, code)
                .map(ReferentialMapper::toQuestion)
                .orElseThrow(() -> new ResourceNotFoundException("Question " + code + " introuvable"));
    }

    public Referential findActive() {
        return referentialRepository.findByActiveTrue()
                .orElseThrow(() -> new ResourceNotFoundException("Aucun référentiel actif"));
    }

    public Referential findById(Long id) {
        return referentialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Référentiel " + id + " introuvable"));
    }
}
