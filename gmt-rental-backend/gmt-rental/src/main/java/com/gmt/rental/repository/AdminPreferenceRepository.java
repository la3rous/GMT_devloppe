package com.gmt.rental.repository;

import com.gmt.rental.entity.AdminPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface AdminPreferenceRepository extends JpaRepository<AdminPreference, Long> {
    Optional<AdminPreference> findByUserId(Long userId);
    List<AdminPreference> findAllByQuoteNotificationsTrue();
}
