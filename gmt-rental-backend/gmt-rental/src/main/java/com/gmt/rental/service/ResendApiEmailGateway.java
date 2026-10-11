package com.gmt.rental.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.springframework.stereotype.Component;

@Component
public class ResendApiEmailGateway implements ResendEmailGateway {
    @Override
    public String send(String apiKey, String from, String recipient, String subject, String html, String text)
            throws ResendException {
        Resend resend = new Resend(apiKey);
        CreateEmailOptions options = CreateEmailOptions.builder()
                .from(from)
                .to(recipient)
                .subject(subject)
                .html(html)
                .text(text)
                .build();
        CreateEmailResponse response = resend.emails().send(options);
        return response == null ? null : response.getId();
    }
}
