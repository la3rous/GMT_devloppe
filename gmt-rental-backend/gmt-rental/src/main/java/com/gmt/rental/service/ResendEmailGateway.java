package com.gmt.rental.service;

import com.resend.core.exception.ResendException;

public interface ResendEmailGateway {
    String send(String apiKey, String from, String recipient, String subject, String html, String text)
            throws ResendException;
}
