package com.mygarage.repository;

import com.mygarage.model.User;
import com.mygarage.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole(Role role);
    List<User> findByActiveTrue();
    long countByRole(Role role);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.role = 'NORMAL_USER' AND " +
           "(LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<User> searchUsers(@org.springframework.data.repository.query.Param("query") String query);
}
