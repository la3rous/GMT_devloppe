package com.gmt.rental.repository;

import com.gmt.rental.entity.SupportRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface SupportRequestRepository extends JpaRepository<SupportRequest, Long> {
    long countBySenderEmailAndCreatedAtAfter(String senderEmail, LocalDateTime after);
    List<SupportRequest> findTop20BySenderEmailOrderByCreatedAtDesc(String senderEmail);
}
