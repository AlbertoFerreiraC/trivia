package com.fersal.trivia.question;

import com.fersal.trivia.question.dto.AdminQuestionDto;
import com.fersal.trivia.question.dto.SaveQuestionRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/questions")
public class AdminQuestionController {

    private final QuestionService questionService;

    public AdminQuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping
    public ResponseEntity<Page<AdminQuestionDto>> listQuestions(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(questionService.listQuestions(categoryId, active, PageRequest.of(page, size)));
    }

    @PostMapping
    public ResponseEntity<AdminQuestionDto> createQuestion(@Valid @RequestBody SaveQuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(questionService.createQuestion(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminQuestionDto> updateQuestion(@PathVariable Long id,
                                                           @Valid @RequestBody SaveQuestionRequest request) {
        return ResponseEntity.ok(questionService.updateQuestion(id, request));
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdminQuestionDto> uploadImage(@PathVariable Long id,
                                                        @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(questionService.uploadQuestionImage(id, file));
    }

    @DeleteMapping("/{id}/image")
    public ResponseEntity<AdminQuestionDto> deleteImage(@PathVariable Long id) {
        return ResponseEntity.ok(questionService.removeQuestionImage(id));
    }
}