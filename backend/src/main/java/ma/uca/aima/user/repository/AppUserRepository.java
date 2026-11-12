package ma.uca.aima.user.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ma.uca.aima.user.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    @Query("select u from AppUser u left join fetch u.organization where lower(u.email) = lower(:email)")
    Optional<AppUser> findByEmail(@Param("email") String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("select u from AppUser u left join fetch u.organization order by u.email")
    List<AppUser> findAllWithOrganization();
}
