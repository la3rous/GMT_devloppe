package com.gmt.rental.repository;

import com.gmt.rental.entity.VerificationCode;
import com.gmt.rental.entity.VerificationCodeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    Optional<VerificationCode> findTopByEmailIgnoreCaseAndTypeAndUsedFalseOrderByCreatedAtDesc(
            String email,
            VerificationCodeType type
    );

    List<VerificationCode> findByEmailIgnoreCaseAndTypeAndUsedFalse(
            String email,
            VerificationCodeType type
    );

    long countByEmailIgnoreCaseAndTypeAndCreatedAtAfter(
            String email,
            VerificationCodeType type,
            Instant after
    );
}

