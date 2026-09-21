package hr.algebra.workforce.dto;

public record LoginResponse(String token,
                            String tokenType,
                            long expiresInMinutes,
                            String fullName,
                            String role) {
}
