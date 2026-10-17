package com.fersal.trivia.category;

import com.fersal.trivia.category.dto.CategoryDto;
import com.fersal.trivia.category.dto.SaveCategoryRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/api/v1/categories")
    public ResponseEntity<List<CategoryDto>> getPublicActiveCategories() {
        return ResponseEntity.ok(categoryService.listActiveCategories());
    }

    @GetMapping("/api/v1/admin/categories")
    public ResponseEntity<List<CategoryDto>> getAllCategoriesAdmin() {
        return ResponseEntity.ok(categoryService.listAllCategoriesForAdmin());
    }

    @PostMapping("/api/v1/admin/categories")
    public ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody SaveCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(request));
    }

    @PutMapping("/api/v1/admin/categories/{id}")
    public ResponseEntity<CategoryDto> updateCategory(@PathVariable Long id,
                                                      @Valid @RequestBody SaveCategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }
}