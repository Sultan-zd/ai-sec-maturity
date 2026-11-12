package ma.uca.aima.user.service;

import java.util.List;
import ma.uca.aima.common.BusinessRuleException;
import ma.uca.aima.organization.model.Organization;
import ma.uca.aima.organization.service.OrganizationService;
import ma.uca.aima.security.LoginAttemptService;
import ma.uca.aima.security.TokenService;
import ma.uca.aima.security.TokenService.IssuedToken;
import ma.uca.aima.user.api.UserDtos.CreateUserRequest;
import ma.uca.aima.user.api.UserDtos.LoginRequest;
import ma.uca.aima.user.api.UserDtos.LoginResponse;
import ma.uca.aima.user.api.UserDtos.UserDto;
import ma.uca.aima.user.model.AppUser;
import ma.uca.aima.user.model.Role;
import ma.uca.aima.user.repository.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponseException;

@Service
@Transactional
public class UserService {

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final OrganizationService organizationService;
    private final LoginAttemptService loginAttempts;
    /** Hash factice : vérifié quand l'e-mail est inconnu, pour que la réponse prenne le même temps. */
    private final String dummyHash;

    public UserService(AppUserRepository repository, PasswordEncoder passwordEncoder, TokenService tokenService,
                       OrganizationService organizationService, LoginAttemptService loginAttempts) {
        this.loginAttempts = loginAttempts;
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.organizationService = organizationService;
        this.dummyHash = passwordEncoder.encode("timing-attack-protection");
    }

    /**
     * Connexion. Le message d'erreur est identique que l'e-mail existe ou non, et le temps de réponse
     * aussi : un attaquant ne peut pas deviner quels comptes existent (énumération d'utilisateurs).
     */
    public LoginResponse login(LoginRequest request) {
        loginAttempts.checkAllowed(request.email());
        AppUser user = repository.findByEmail(request.email().trim()).orElse(null);
        boolean passwordOk = passwordEncoder.matches(request.password(),
                user == null ? dummyHash : user.getPasswordHash());
        if (user == null || !passwordOk || !user.isEnabled()) {
            loginAttempts.recordFailure(request.email());
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                    "E-mail ou mot de passe incorrect");
            problem.setTitle("Identifiants invalides");
            throw new ErrorResponseException(HttpStatus.UNAUTHORIZED, problem, null);
        }
        loginAttempts.recordSuccess(request.email());
        user.recordLogin();
        IssuedToken token = tokenService.issue(user);
        return new LoginResponse(token.value(), "Bearer", token.expiresAt(), UserDto.from(user));
    }

    public UserDto create(CreateUserRequest request) {
        if (repository.existsByEmailIgnoreCase(request.email().trim())) {
            throw BusinessRuleException.conflict("Un utilisateur existe déjà avec cet e-mail");
        }
        Organization organization = null;
        if (request.role() == Role.ORG_USER) {
            if (request.organizationId() == null) {
                throw BusinessRuleException.invalid("organizationId est obligatoire pour le rôle ORG_USER");
            }
            organization = organizationService.find(request.organizationId());
        } else if (request.organizationId() != null) {
            throw BusinessRuleException.invalid("Seul un ORG_USER est rattaché à une organisation");
        }
        AppUser user = new AppUser(request.email(), request.fullName().trim(),
                passwordEncoder.encode(request.password()), request.role(), organization);
        return UserDto.from(repository.saveAndFlush(user));
    }

    @Transactional(readOnly = true)
    public List<UserDto> list() {
        return repository.findAllWithOrganization().stream().map(UserDto::from).toList();
    }

    @Transactional(readOnly = true)
    public UserDto me() {
        return repository.findByEmail(TokenService.currentUser().email())
                .map(UserDto::from)
                .orElseThrow(() -> new IllegalStateException("Utilisateur du jeton introuvable"));
    }
}
