package com.gmt.rental.repository;

import com.gmt.rental.entity.EmailDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailDeliveryRepository extends JpaRepository<EmailDelivery, Long> {
    boolean existsByQuoteRequestIdAndRecipient(Long quoteRequestId, String recipient);
}
