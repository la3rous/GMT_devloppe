package com.gmt.rental.service;

import com.gmt.rental.entity.Category;
import com.gmt.rental.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAllByOrderByIdDesc().stream()
                .filter(category -> !Boolean.FALSE.equals(category.getActif()))
                .toList();
    }

    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id).filter(category -> !Boolean.FALSE.equals(category.getActif()));
    }
}
