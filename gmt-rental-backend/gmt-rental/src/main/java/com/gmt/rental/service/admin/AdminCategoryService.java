package com.gmt.rental.service.admin;

import com.gmt.rental.dto.admin.CategoryRequest;
import com.gmt.rental.entity.Category;
import com.gmt.rental.repository.CategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AdminCategoryService {
    private final CategoryRepository categories;

    public AdminCategoryService(CategoryRepository categories) { this.categories = categories; }
    public List<Category> findAll() { return categories.findAllByOrderByIdDesc().stream().filter(c -> !Boolean.FALSE.equals(c.getActif())).toList(); }
    public Category findById(Long id) { return categories.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Catégorie introuvable")); }

    public Category create(CategoryRequest request) {
        Category category = new Category();
        apply(category, request);
        return categories.save(category);
    }

    public Category update(Long id, CategoryRequest request) {
        Category category = findById(id);
        apply(category, request);
        return categories.save(category);
    }

    public void deactivate(Long id) {
        Category category = findById(id);
        category.setActif(false);
        categories.save(category);
    }

    private void apply(Category category, CategoryRequest request) {
        if (request == null || request.nom() == null || request.nom().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom de la catégorie est obligatoire");
        }
        category.setNom(request.nom().trim());
        category.setDescription(request.description());
        category.setActif(request.actif() == null ? true : request.actif());
    }
}
