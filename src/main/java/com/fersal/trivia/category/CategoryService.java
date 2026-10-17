package com.fersal.trivia.category;

import com.fersal.trivia.category.dto.CategoryDto;
import com.fersal.trivia.category.dto.SaveCategoryRequest;
import com.fersal.trivia.common.BusinessException;
import com.fersal.trivia.question.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final QuestionRepository questionRepository;

    public CategoryService(CategoryRepository categoryRepository, QuestionRepository questionRepository) {
        this.categoryRepository = categoryRepository;
        this.questionRepository = questionRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> listActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByIdAsc().stream()
                .map(c -> CategoryDto.from(c, questionRepository.countByCategoryIdAndActiveTrue(c.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> listAllCategoriesForAdmin() {
        return categoryRepository.findAllByOrderByIdAsc().stream()
                .map(c -> CategoryDto.from(c, questionRepository.countByCategoryIdAndActiveTrue(c.getId())))
                .toList();
    }

    @Transactional
    public CategoryDto createCategory(SaveCategoryRequest req) {
        String normalizedSlug = req.slug().trim().toLowerCase(Locale.ROOT);
        if (categoryRepository.findBySlug(normalizedSlug).isPresent()) {
            throw new BusinessException("DUPLICATE_CATEGORY_SLUG", "Ya existe una categoría con ese código/slug.");
        }
        Category c = new Category();
        c.setSlug(normalizedSlug);
        c.setName(req.name().trim());
        c.setIcon(req.icon() != null && !req.icon().isBlank() ? req.icon().trim() : "🚦");
        c.setDescription(req.description());
        c.setActive(req.active());
        c = categoryRepository.save(c);
        return CategoryDto.from(c, 0L);
    }

    @Transactional
    public CategoryDto updateCategory(Long id, SaveCategoryRequest req) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("CATEGORY_NOT_FOUND", "Categoría no encontrada."));
        c.setName(req.name().trim());
        c.setIcon(req.icon() != null && !req.icon().isBlank() ? req.icon().trim() : "🚦");
        c.setDescription(req.description());
        c.setActive(req.active());
        c = categoryRepository.save(c);
        long count = questionRepository.countByCategoryIdAndActiveTrue(c.getId());
        return CategoryDto.from(c, count);
    }
}