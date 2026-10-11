package com.gmt.rental.repository;

import com.gmt.rental.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    long countByRoleIgnoreCase(String role);

    @Query("select count(user) from User user where upper(user.role) in ('ADMIN', 'ROLE_ADMIN') "
            + "and (user.actif is null or user.actif = true)")
    long countActiveAdministrators();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from User user where upper(user.role) in ('ADMIN', 'ROLE_ADMIN') order by user.id")
    java.util.List<User> lockAdministrators();

}
