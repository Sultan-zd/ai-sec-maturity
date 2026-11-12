package ma.uca.aima.user.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.uca.aima.user.api.UserDtos.LoginRequest;
import ma.uca.aima.user.api.UserDtos.LoginResponse;
import ma.uca.aima.user.api.UserDtos.UserDto;
import ma.uca.aima.user.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentification", description = "Connexion et utilisateur courant")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    @SecurityRequirements      // endpoint public
    @Operation(summary = "Se connecter et obtenir un jeton JWT")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Utilisateur connecté")
    public UserDto me() {
        return userService.me();
    }
}
