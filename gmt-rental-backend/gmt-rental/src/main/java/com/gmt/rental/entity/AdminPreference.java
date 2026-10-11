package com.gmt.rental.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "admin_preferences", uniqueConstraints = @UniqueConstraint(name = "uk_admin_preferences_user", columnNames = "user_id"))
public class AdminPreference {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 16)
    private String theme = "dark";

    @Column(name = "quote_notifications", nullable = false)
    private boolean quoteNotifications = true;

    @Column(name = "notification_email", length = 254)
    private String notificationEmail;

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public boolean isQuoteNotifications() { return quoteNotifications; }
    public void setQuoteNotifications(boolean quoteNotifications) { this.quoteNotifications = quoteNotifications; }
    public String getNotificationEmail() { return notificationEmail; }
    public void setNotificationEmail(String notificationEmail) { this.notificationEmail = notificationEmail; }
}
