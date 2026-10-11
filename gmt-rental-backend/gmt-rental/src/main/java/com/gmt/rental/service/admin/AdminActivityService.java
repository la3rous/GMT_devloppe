package com.gmt.rental.service.admin;

import com.gmt.rental.entity.AdminActivity;
import com.gmt.rental.repository.AdminActivityRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminActivityService {
    private final AdminActivityRepository repository;
    public AdminActivityService(AdminActivityRepository repository) { this.repository = repository; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String actor, String target, String action, String module, String description, boolean success) {
        AdminActivity item = new AdminActivity();
        item.setActorEmail(clean(actor, 320));
        item.setTargetLabel(clean(target, 320));
        item.setAction(clean(action, 64));
        item.setModule(clean(module, 64));
        item.setDescription(clean(description, 2000));
        item.setResult(success ? "SUCCESS" : "FAILURE");
        repository.save(item);
    }

    public Page<AdminActivity> search(String keyword, String module, String action, LocalDate from, LocalDate to, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(100, size));
        Specification<AdminActivity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String term = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("actorEmail")), term),
                        cb.like(cb.lower(root.get("targetLabel")), term),
                        cb.like(cb.lower(root.get("action")), term),
                        cb.like(cb.lower(root.get("module")), term),
                        cb.like(cb.lower(root.get("description")), term),
                        cb.like(cb.lower(root.get("result")), term)));
            }
            if (module != null && !module.isBlank()) predicates.add(cb.equal(cb.lower(root.get("module")), module.trim().toLowerCase()));
            if (action != null && !action.isBlank()) predicates.add(cb.equal(cb.lower(root.get("action")), action.trim().toLowerCase()));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from.atStartOfDay()));
            if (to != null) predicates.add(cb.lessThan(root.get("occurredAt"), to.plusDays(1).atStartOfDay()));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return repository.findAll(spec, PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "occurredAt")));
    }

    public static String csv(Page<AdminActivity> page) {
        return csvRows(page.getContent());
    }

    public String exportCsv(String keyword, String module, String action, LocalDate from, LocalDate to) {
        Page<AdminActivity> first = search(keyword, module, action, from, to, 0, 100);
        List<AdminActivity> rows = new ArrayList<>(first.getContent());
        for (int page = 1; page < first.getTotalPages(); page++) {
            rows.addAll(search(keyword, module, action, from, to, page, 100).getContent());
        }
        return csvRows(rows);
    }

    private static String csvRows(List<AdminActivity> rows) {
        StringBuilder csv = new StringBuilder("Date et heure;Administrateur;Utilisateur concerné;Action;Module;Description;Résultat\r\n");
        for (AdminActivity item : rows) {
            csv.append(cell(item.getOccurredAt())).append(';').append(cell(item.getActorEmail())).append(';')
                    .append(cell(item.getTargetLabel())).append(';').append(cell(item.getAction())).append(';')
                    .append(cell(item.getModule())).append(';').append(cell(item.getDescription())).append(';')
                    .append(cell(item.getResult())).append("\r\n");
        }
        return "\uFEFF" + csv;
    }

    public Page<AdminActivity> search(String keyword, String module, LocalDate from, LocalDate to, int page, int size) {
        return search(keyword, module, null, from, to, page, size);
    }

    private static String clean(String value, int max) {
        if (value == null) return "";
        String cleaned = value.replaceAll("[\\r\\n\\t]", " ").trim();
        return cleaned.length() > max ? cleaned.substring(0, max) : cleaned;
    }
    private static String cell(Object value) {
        String raw = value == null ? "" : value.toString();
        if (!raw.isEmpty() && "=+-@".indexOf(raw.charAt(0)) >= 0) raw = "'" + raw;
        return "\"" + raw.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ") + "\"";
    }
}
