package com.gmt.rental.controller.admin;

import com.gmt.rental.dto.admin.CategoryRequest;
import com.gmt.rental.entity.Category;
import com.gmt.rental.service.admin.AdminCategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {
    private final AdminCategoryService service;
    public AdminCategoryController(AdminCategoryService service) { this.service = service; }
    @GetMapping public List<Category> getAll() { return service.findAll(); }
    @GetMapping("/{id}") public Category getById(@PathVariable Long id) { return service.findById(id); }
    @PostMapping public ResponseEntity<Category> create(@RequestBody CategoryRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request)); }
    @PutMapping("/{id}") public Category update(@PathVariable Long id, @RequestBody CategoryRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deactivate(@PathVariable Long id) { service.deactivate(id); }
}
