package com.fersal.trivia.question.dto;

import com.fersal.trivia.question.Question;

import java.time.Instant;
import java.util.List;

public record AdminQuestionDto(
        Long id,
        Long categoryId,
        String categoryName,
        String statement,
        String explanation,
        String icon,
        String imageUrl,
        boolean active,
        List<AdminOptionDto> options,
        Instant createdAt,
        Instant updatedAt
) {
    public record AdminOptionDto(Long id, short optionOrder, String text, boolean correct) {
    }

    public static AdminQuestionDto fromEntity(Question q) {
        String imgUrl = q.getImageFilename() != null ? "/uploads/questions/" + q.getImageFilename() : null;
        List<AdminOptionDto> opts = q.getOptions().stream()
                .map(o -> new AdminOptionDto(o.getId(), o.getOptionOrder(), o.getOptionText(), o.isCorrect()))
                .toList();
        return new AdminQuestionDto(
                q.getId(),
                q.getCategory().getId(),
                q.getCategory().getName(),
                q.getStatement(),
                q.getExplanation(),
                q.getIcon(),
                imgUrl,
                q.isActive(),
                opts,
                q.getCreatedAt(),
                q.getUpdatedAt()
        );
    }
}