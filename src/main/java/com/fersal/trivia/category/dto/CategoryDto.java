package com.fersal.trivia.category.dto;

import com.fersal.trivia.category.Category;

public record CategoryDto(
        Long id,
        String slug,
        String name,
        String icon,
        String description,
        boolean active,
        long activeQuestionCount
) {
    public static CategoryDto from(Category c, long count) {
        return new CategoryDto(c.getId(), c.getSlug(), c.getName(), c.getIcon(), c.getDescription(), c.isActive(), count);
    }
}