package hr.algebra.workforce.dto;

import hr.algebra.workforce.model.Role;
import hr.algebra.workforce.model.User;

public record UserRow(Long id,
                      String fullName,
                      String email,
                      Role role,
                      String managerName,
                      boolean active,
                      int vacationDaysPerYear) {

    public static UserRow of(User user) {
        return new UserRow(user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                user.getManager() == null ? null : user.getManager().getFullName(),
                user.isActive(), user.getVacationDaysPerYear());
    }
}
