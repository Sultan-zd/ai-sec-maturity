package ma.uca.aima.user.service;

import ma.uca.aima.security.SecurityProperties;
import ma.uca.aima.user.model.AppUser;
import ma.uca.aima.user.model.Role;
import ma.uca.aima.user.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Au tout premier démarrage (aucun utilisateur en base), crée le compte administrateur
 * défini par la configuration. Sans cela, personne ne pourrait se connecter pour créer les autres comptes.
 */
@Component
@Order(0)
public class BootstrapAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties properties;

    public BootstrapAdminInitializer(AppUserRepository repository, PasswordEncoder passwordEncoder,
                                     SecurityProperties properties) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        var admin = properties.bootstrapAdmin();
        repository.save(new AppUser(admin.email(), admin.fullName(), passwordEncoder.encode(admin.password()),
                Role.ADMIN, null));
        log.warn("Compte administrateur initial créé : {} — changez son mot de passe (AIMA_ADMIN_PASSWORD) "
                + "avant toute mise en production", admin.email());
    }
}
