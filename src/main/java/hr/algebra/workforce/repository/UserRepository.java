package hr.algebra.workforce.repository;

import hr.algebra.workforce.model.Role;
import hr.algebra.workforce.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "manager")
    List<User> findByActiveTrueOrderByLastNameAsc();

    @EntityGraph(attributePaths = "manager")
    List<User> findAllByOrderByLastNameAscFirstNameAsc();

    long countByRoleAndActiveTrue(Role role);

    List<User> findByManagerId(Long managerId);

    List<User> findByManagerIdAndActiveTrueOrderByLastNameAsc(Long managerId);

    List<User> findByRoleOrderByLastNameAsc(Role role);
}
