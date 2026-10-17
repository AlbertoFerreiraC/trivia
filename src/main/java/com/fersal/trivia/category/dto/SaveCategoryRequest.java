package com.fersal.trivia.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveCategoryRequest(
        @NotBlank(message = "El identificador (slug) es obligatorio")
        @Size(max = 50)
        String slug,

        @NotBlank(message = "El nombre de la categoría es obligatorio")
        @Size(max = 100)
        String name,

        @Size(max = 30)
        String icon,

        @Size(max = 255)
        String description,

        boolean active
) {
}