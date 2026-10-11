package com.gmt.rental.repository;

import com.gmt.rental.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop50ByOrderByDateCreationDescIdDesc();

    long countByLuFalse();

    @Modifying
    @Query("UPDATE Notification notification SET notification.lu = true WHERE notification.lu = false")
    int markAllAsRead();
}
