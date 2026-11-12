package ma.uca.aima.referential.repository;

import java.util.Optional;
import ma.uca.aima.referential.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query("""
            select q from Question q
            join fetch q.subdomain s
            join fetch s.domain d
            where d.referential.id = :referentialId and q.code = :code
            """)
    Optional<Question> findByReferentialAndCode(@Param("referentialId") Long referentialId,
                                                @Param("code") String code);
}
