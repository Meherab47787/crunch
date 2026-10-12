package com.crustcrunch.repository;

import com.crustcrunch.entity.User;
import com.crustcrunch.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            SELECT u FROM User u
            WHERE u.role IN :roles
              AND (:q IS NULL OR lower(u.email) LIKE lower(concat('%', CAST(:q AS string), '%'))
                   OR lower(u.fullName) LIKE lower(concat('%', CAST(:q AS string), '%')))
            """)
    Page<User> search(@Param("roles") Collection<Role> roles, @Param("q") String q, Pageable pageable);

    long countByRole(Role role);
}
