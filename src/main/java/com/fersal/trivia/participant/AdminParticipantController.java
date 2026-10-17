package com.fersal.trivia.participant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/participants")
public class AdminParticipantController {

    private final ParticipantRepository participantRepository;

    public AdminParticipantController(ParticipantRepository participantRepository) {
        this.participantRepository = participantRepository;
    }

    @GetMapping
    public ResponseEntity<Page<Participant>> listParticipants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return ResponseEntity.ok(participantRepository.findAllByOrderByLastPlayedAtDesc(PageRequest.of(page, size)));
    }
}