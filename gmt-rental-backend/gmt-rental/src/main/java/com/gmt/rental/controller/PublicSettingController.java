package com.gmt.rental.controller;

import com.gmt.rental.repository.SettingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/parametres")
public class PublicSettingController {
    private final SettingRepository settings;
    private final BigDecimal defaultRate;

    public PublicSettingController(
            SettingRepository settings,
            @Value("${gmt.currency.eur-to-dt:3.4}") BigDecimal defaultRate
    ) {
        this.settings = settings;
        this.defaultRate = defaultRate;
    }

    @GetMapping
    public Map<String, BigDecimal> getCurrencyRate() {
        BigDecimal rate = settings.findAll().stream()
                .map(setting -> setting.getTauxEurDt())
                .findFirst()
                .orElse(defaultRate);
        return Map.of("tauxEurDt", rate);
    }
}
