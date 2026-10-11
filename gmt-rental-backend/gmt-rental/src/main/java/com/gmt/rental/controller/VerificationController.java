package com.gmt.rental.controller;

import com.gmt.rental.dto.verification.SendVerificationCodeRequest;
import com.gmt.rental.dto.verification.VerificationResponse;
import com.gmt.rental.dto.verification.VerifyCodeRequest;
import com.gmt.rental.service.VerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/verification")
public class VerificationController {

    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping("/send")
    public ResponseEntity<VerificationResponse> send(@RequestBody SendVerificationCodeRequest request) {
        verificationService.sendEmailVerificationCode(request != null ? request.email() : null);
        return ResponseEntity.ok(new VerificationResponse(
                true,
                "Si cette adresse e-mail correspond à un compte en attente, un code de vérification a été envoyé."
        ));
    }

    @PostMapping("/verify")
    public ResponseEntity<VerificationResponse> verify(@RequestBody VerifyCodeRequest request) {
        VerificationResponse response = verificationService.verifyEmailCode(
                request != null ? request.email() : null,
                request != null ? request.code() : null
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend")
    public ResponseEntity<VerificationResponse> resend(@RequestBody SendVerificationCodeRequest request) {
        verificationService.resendEmailVerificationCode(request != null ? request.email() : null);
        return ResponseEntity.ok(new VerificationResponse(
                true,
                "Un nouveau code de vérification a été envoyé si l'adresse est valide."
        ));
    }
}

