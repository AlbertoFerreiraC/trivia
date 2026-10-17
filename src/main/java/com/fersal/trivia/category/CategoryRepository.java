package com.fersal.trivia.category;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByActiveTrueOrderByIdAsc();
    List<Category> findAllByOrderByIdAsc();
    Optional<Category> findBySlug(String slug);
}