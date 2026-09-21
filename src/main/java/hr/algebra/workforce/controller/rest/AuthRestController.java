package hr.algebra.workforce.controller.rest;

import hr.algebra.workforce.dto.LoginRequest;
import hr.algebra.workforce.dto.LoginResponse;
import hr.algebra.workforce.security.AppUserDetails;
import hr.algebra.workforce.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthRestController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
        return new LoginResponse(jwtService.generateToken(user), "Bearer", jwtService.getValidityMinutes(),
                user.getFullName(), user.getRole().name());
    }
}
