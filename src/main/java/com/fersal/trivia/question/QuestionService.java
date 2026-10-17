package com.fersal.trivia.question;

import com.fersal.trivia.category.Category;
import com.fersal.trivia.category.CategoryRepository;
import com.fersal.trivia.common.BusinessException;
import com.fersal.trivia.question.dto.AdminQuestionDto;
import com.fersal.trivia.question.dto.SaveQuestionRequest;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final CategoryRepository categoryRepository;
    private final ImageStorageService imageStorageService;
    private final EntityManager entityManager;

    public QuestionService(QuestionRepository questionRepository,
                           CategoryRepository categoryRepository,
                           ImageStorageService imageStorageService,
                           EntityManager entityManager) {
        this.questionRepository = questionRepository;
        this.categoryRepository = categoryRepository;
        this.imageStorageService = imageStorageService;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public Page<AdminQuestionDto> listQuestions(Long categoryId, Boolean active, Pageable pageable) {
        return questionRepository.findFiltered(categoryId, active, pageable)
                .map(AdminQuestionDto::fromEntity);
    }

    @Transactional
    public AdminQuestionDto createQuestion(SaveQuestionRequest request) {
        validateFiveOptionsAndSingleCorrect(request.options());

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new BusinessException("CATEGORY_NOT_FOUND", "Categoría no encontrada."));

        Question q = new Question();
        q.setCategory(category);
        q.setStatement(request.statement().trim());
        q.setExplanation(request.explanation().trim());
        q.setIcon(request.icon() != null && !request.icon().isBlank() ? request.icon().trim() : "🚦");
        q.setActive(request.active());
        q.replaceOptions(mapOptions(request.options(), q));

        Question saved = questionRepository.save(q);
        return AdminQuestionDto.fromEntity(saved);
    }

    @Transactional
    public AdminQuestionDto updateQuestion(Long id, SaveQuestionRequest request) {
        validateFiveOptionsAndSingleCorrect(request.options());

        Question q = questionRepository.findWithOptionsById(id)
                .orElseThrow(() -> new BusinessException("QUESTION_NOT_FOUND", "Pregunta no encontrada."));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new BusinessException("CATEGORY_NOT_FOUND", "Categoría no encontrada."));

        q.setCategory(category);
        q.setStatement(request.statement().trim());
        q.setExplanation(request.explanation().trim());
        q.setIcon(request.icon() != null && !request.icon().isBlank() ? request.icon().trim() : "🚦");
        q.setActive(request.active());

        // Limpiamos opciones previas y sincronizamos para respetar el índice único parcial en PostgreSQL
        q.getOptions().clear();
        entityManager.flush();

        q.replaceOptions(mapOptions(request.options(), q));
        Question saved = questionRepository.saveAndFlush(q);
        return AdminQuestionDto.fromEntity(saved);
    }

    @Transactional
    public AdminQuestionDto uploadQuestionImage(Long questionId, MultipartFile file) {
        Question q = questionRepository.findWithOptionsById(questionId)
                .orElseThrow(() -> new BusinessException("QUESTION_NOT_FOUND", "Pregunta no encontrada."));

        String oldImage = q.getImageFilename();
        String newFilename = imageStorageService.storeImage(file);
        q.setImageFilename(newFilename);
        Question saved = questionRepository.save(q);

        if (oldImage != null) {
            imageStorageService.deleteImageIfExists(oldImage);
        }
        return AdminQuestionDto.fromEntity(saved);
    }

    @Transactional
    public AdminQuestionDto removeQuestionImage(Long questionId) {
        Question q = questionRepository.findWithOptionsById(questionId)
                .orElseThrow(() -> new BusinessException("QUESTION_NOT_FOUND", "Pregunta no encontrada."));

        if (q.getImageFilename() != null) {
            imageStorageService.deleteImageIfExists(q.getImageFilename());
            q.setImageFilename(null);
            q = questionRepository.save(q);
        }
        return AdminQuestionDto.fromEntity(q);
    }

    private void validateFiveOptionsAndSingleCorrect(List<SaveQuestionRequest.OptionInput> options) {
        if (options == null || options.size() != 5) {
            throw new BusinessException("INVALID_OPTIONS_COUNT",
                    "Cada pregunta debe tener exactamente 5 opciones de respuesta.");
        }
        long correctCount = options.stream().filter(SaveQuestionRequest.OptionInput::correct).count();
        if (correctCount != 1) {
            throw new BusinessException("INVALID_CORRECT_OPTIONS_COUNT",
                    "Debe marcar exactamente 1 opción como respuesta correcta.");
        }
        boolean anyBlank = options.stream().anyMatch(o -> o.text() == null || o.text().trim().isEmpty());
        if (anyBlank) {
            throw new BusinessException("EMPTY_OPTION_TEXT", "Ninguna de las 5 opciones puede quedar en blanco.");
        }
    }

    private List<AnswerOption> mapOptions(List<SaveQuestionRequest.OptionInput> inputs, Question q) {
        List<AnswerOption> list = new ArrayList<>(5);
        short order = 1;
        for (SaveQuestionRequest.OptionInput in : inputs) {
            AnswerOption opt = new AnswerOption();
            opt.setQuestion(q);
            opt.setOptionOrder(order++);
            opt.setOptionText(in.text().trim());
            opt.setCorrect(in.correct());
            list.add(opt);
        }
        return list;
    }
}