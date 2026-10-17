package com.fersal.trivia.participant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    Optional<Participant> findByNormalizedName(String normalizedName);

    Page<Participant> findAllByOrderByLastPlayedAtDesc(Pageable pageable);
}