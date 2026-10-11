package com.gmt.rental.controller.admin;

import com.gmt.rental.dto.admin.SettingRequest;
import com.gmt.rental.dto.admin.SettingResponse;
import com.gmt.rental.entity.Setting;
import com.gmt.rental.repository.SettingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/settings")
public class AdminSettingController {
    private final SettingRepository settings;
    private final BigDecimal defaultRate;
    public AdminSettingController(SettingRepository settings, @Value("${gmt.currency.eur-to-dt:3.4}") BigDecimal defaultRate) {
        this.settings = settings;
        this.defaultRate = defaultRate;
    }
    @GetMapping public List<SettingResponse> getAll() {
        return settings.findAll().stream()
                .findFirst()
                .map(setting -> List.of(new SettingResponse("TAUX_EUR_TND", setting.getTauxEurDt().toPlainString())))
                .orElseGet(() -> List.of(new SettingResponse("TAUX_EUR_TND", defaultRate.toPlainString())));
    }

    @PostMapping public SettingResponse save(@RequestBody SettingRequest request) {
        if (request == null || request.cle() == null || request.cle().isBlank() || request.valeur() == null || request.valeur().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La clé et la valeur sont obligatoires");
        }
        if (!"TAUX_EUR_TND".equals(request.cle().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paramètre inconnu");
        }

        final BigDecimal rate;
        try {
            rate = new BigDecimal(request.valeur().trim().replace(',', '.'));
            if (rate.signum() <= 0) throw new NumberFormatException();
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le taux doit être un nombre positif");
        }

        Setting setting = settings.findAll().stream().findFirst().orElseGet(Setting::new);
        setting.setTauxEurDt(rate);
        setting.setDateModification(LocalDateTime.now());
        Setting saved = settings.save(setting);
        return new SettingResponse("TAUX_EUR_TND", saved.getTauxEurDt().toPlainString());
    }
}
