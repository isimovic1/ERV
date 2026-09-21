package hr.algebra.workforce.controller.rest;

import hr.algebra.workforce.AbstractIntegrationTest;
import hr.algebra.workforce.model.Role;
import hr.algebra.workforce.model.User;
import hr.algebra.workforce.security.AppUserDetails;
import hr.algebra.workforce.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ApiSecurityTest extends AbstractIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private String employeeToken;
    private String managerToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        User manager = createUser("voditelj@test.hr", Role.MANAGER, null);
        User employee = createUser("zaposlenik@test.hr", Role.EMPLOYEE, manager);
        managerToken = jwtService.generateToken(new AppUserDetails(manager));
        employeeToken = jwtService.generateToken(new AppUserDetails(employee));
    }

    @Test
    void loginReturnsTokenForValidCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"zaposlenik@test.hr\",\"password\":\"Algebra1!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"zaposlenik@test.hr\",\"password\":\"pogresna\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Neispravna e-pošta ili lozinka."));
    }

    @Test
    void requestWithoutTokenReturnsUnauthorizedJson() throws Exception {
        mockMvc.perform(get("/api/leaves/balance"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void malformedTokenReturnsUnauthorizedInsteadOfServerError() throws Exception {
        mockMvc.perform(get("/api/leaves/balance").header("Authorization", "Bearer neispravan.token.abc"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenGrantsAccessToOwnData() throws Exception {
        mockMvc.perform(get("/api/leaves/balance").header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.annualEntitledDays").value(20))
                .andExpect(jsonPath("$.annualRemainingDays").value(20))
                .andExpect(jsonPath("$.paidRemainingDays").value(7));
    }

    @Test
    void employeeIsForbiddenFromTeamReports() throws Exception {
        mockMvc.perform(get("/api/reports/monthly").header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void managerCanReadTeamReports() throws Exception {
        mockMvc.perform(get("/api/reports/monthly").header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows").isArray());
    }

    @Test
    void invalidPayloadReturnsListOfMessages() throws Exception {
        mockMvc.perform(post("/api/work-entries")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workDate\":\"2026-09-17\",\"hours\":20.0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]").value("Najveći unos je 16 sati."));
    }

    @Test
    void deletingForeignEntryReturnsNotFound() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/work-entries/999")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isNotFound());
    }

    private User createUser(String email, Role role, User manager) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName(role.name());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("Algebra1!"));
        user.setRole(role);
        user.setHireDate(LocalDate.of(2025, 1, 1));
        user.setManager(manager);
        return userRepository.save(user);
    }
}
